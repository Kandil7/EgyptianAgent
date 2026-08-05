#!/usr/bin/env python3
"""
Egyptian Agent - Egyptian Voice Command Evaluation Script

Evaluates the fine-tuned FunctionGemma model on Egyptian Arabic
voice commands and generates comprehensive accuracy reports.

USAGE:
    python evaluate_egyptian_accuracy.py [OPTIONS]

OPTIONS:
    --model PATH        Path to fine-tuned model (required)
    --test-data PATH    Path to test dataset (JSONL)
    --base-model PATH   Path to base model for comparison
    --output-dir DIR    Output directory for reports
    --device DEVICE     Device (cuda/cpu)
    --batch-size N      Evaluation batch size
    --verbose           Enable verbose output

EXAMPLES:
    python evaluate_egyptian_accuracy.py --model models/egyptian-model
    python evaluate_egyptian_accuracy.py --model models/egyptian-model --base-model google/functiongemma-270m-it
    python evaluate_egyptian_accuracy.py --model models/egyptian-model --verbose

REQUIREMENTS:
    - Python 3.8+
    - PyTorch
    - transformers, sklearn

RETURN CODES:
    0   Success (accuracy >= 95%)
    1   General error
    2   Model not found
    3   Evaluation failed
    4   Accuracy below threshold
"""

import os
import sys
import json
import argparse
import logging
import time
from pathlib import Path
from typing import Dict, List, Tuple, Any, Optional
from collections import defaultdict

import numpy as np

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Try imports
try:
    import torch
    from transformers import AutoTokenizer, AutoModelForCausalLM
    from sklearn.metrics import accuracy_score, precision_recall_fscore_support, confusion_matrix
    EVAL_AVAILABLE = True
except ImportError as e:
    logger.warning(f"Evaluation libraries not available: {e}")
    EVAL_AVAILABLE = False


class EgyptianVoiceCommandEvaluator:
    """Evaluator for Egyptian voice command models."""

    FUNCTION_CATEGORIES = {
        'call_contact': 'CALL_CONTACT',
        'send_whatsapp': 'SEND_WHATSAPP',
        'set_alarm': 'SET_ALARM',
        'emergency': 'EMERGENCY',
        'read_time': 'READ_TIME',
        'open_app': 'OPEN_APP',
        'device_control': 'DEVICE_CONTROL',
    }

    def __init__(self, args):
        self.args = args
        self.device = torch.device(args.device if torch.cuda.is_available() else 'cpu')
        logger.info(f"Using device: {self.device}")
        
        self.model = None
        self.tokenizer = None
        self.base_model = None

    def load_model(self, path: str):
        """Load a model from path."""
        logger.info(f"Loading model from {path}...")
        
        self.tokenizer = AutoTokenizer.from_pretrained(
            path,
            trust_remote_code=True,
            padding_side='left',
        )
        
        if self.tokenizer.pad_token is None:
            self.tokenizer.pad_token = self.tokenizer.eos_token
        
        self.model = AutoModelForCausalLM.from_pretrained(
            path,
            trust_remote_code=True,
            torch_dtype=torch.float32,
        )

        self.model = self.model.to(self.device)
        self.model.eval()
        logger.info("Model loaded")

    def load_test_data(self) -> List[Dict]:
        """Load test dataset."""
        logger.info(f"Loading test data from {self.args.test_data}...")
        
        test_data = []
        with open(self.args.test_data, 'r', encoding='utf-8') as f:
            for line in f:
                if line.strip():
                    test_data.append(json.loads(line))
        
        logger.info(f"Loaded {len(test_data)} test examples")
        return test_data

    def extract_function_from_response(self, response: str) -> Optional[str]:
        """Extract function name from model response."""
        try:
            response = response.strip()
            start_idx = response.find('{')
            end_idx = response.rfind('}') + 1
            
            if start_idx >= 0 and end_idx > start_idx:
                json_str = response[start_idx:end_idx]
                parsed = json.loads(json_str)
                return parsed.get('function')
        except (json.JSONDecodeError, Exception):
            pass
        
        return None

    def predict(self, user_input: str, max_new_tokens: int = 128) -> str:
        """Generate prediction for user input."""
        prompt = f"<system>\nYou are a function calling assistant for Egyptian Arabic voice commands.\n</system>\n<user>\n{user_input}\n</user>\n<assistant>\n"
        
        inputs = self.tokenizer(
            prompt,
            return_tensors='pt',
            truncation=True,
            max_length=512,
        ).to(self.device)
        
        with torch.no_grad():
            outputs = self.model.generate(
                **inputs,
                max_new_tokens=max_new_tokens,
                do_sample=False,
                temperature=0.0,
                pad_token_id=self.tokenizer.eos_token_id,
            )
        
        generated = self.tokenizer.decode(outputs[0], skip_special_tokens=True)
        
        if '<assistant>\n' in generated:
            return generated.split('<assistant>\n')[-1].strip()
        return generated

    def evaluate(self) -> Dict[str, Any]:
        """Run full evaluation."""
        logger.info("Starting evaluation...")
        
        test_data = self.load_test_data()
        
        predictions = []
        expected = []
        latencies = []
        examples_with_details = []
        
        for i, example in enumerate(test_data):
            messages = example.get('messages', [])
            
            user_input = None
            for msg in messages:
                if msg.get('role') == 'user':
                    user_input = msg.get('content', '')
                    break
            
            if not user_input:
                continue
            
            # Get expected function
            expected_function = None
            for msg in messages:
                if msg.get('role') == 'assistant':
                    expected_function = self.extract_function_from_response(msg.get('content', ''))
                    break
            
            # Measure latency
            start_time = time.time()
            response = self.predict(user_input)
            latency = (time.time() - start_time) * 1000
            latencies.append(latency)
            
            # Extract predicted function
            predicted_function = self.extract_function_from_response(response)
            
            predictions.append(predicted_function)
            expected.append(expected_function)
            
            examples_with_details.append({
                'user_input': user_input,
                'expected_function': expected_function,
                'predicted_function': predicted_function,
                'correct': predicted_function == expected_function,
            })
            
            if (i + 1) % 20 == 0:
                logger.info(f"Evaluated {i + 1}/{len(test_data)} examples")
        
        return self.calculate_metrics(predictions, expected, latencies, examples_with_details)

    def calculate_metrics(
        self,
        predictions: List[str],
        expected: List[str],
        latencies: List[float],
        examples: List[Dict],
    ) -> Dict[str, Any]:
        """Calculate evaluation metrics."""
        
        valid_pairs = [(p, e) for p, e in zip(predictions, expected) if e is not None]
        valid_predictions = [p for p, e in valid_pairs]
        valid_expected = [e for p, e in valid_pairs]
        
        # Overall accuracy
        correct = sum(1 for p, e in zip(valid_predictions, valid_expected) if p == e)
        overall_accuracy = correct / len(valid_expected) if valid_expected else 0
        
        # Per-class accuracy
        class_correct = defaultdict(int)
        class_total = defaultdict(int)
        
        for pred, exp in zip(valid_predictions, valid_expected):
            class_total[exp] += 1
            if pred == exp:
                class_correct[exp] += 1
        
        per_class_accuracy = {
            cls: class_correct[cls] / class_total[cls] if class_total[cls] > 0 else 0
            for cls in class_total
        }
        
        # Latency statistics
        latency_stats = {
            'mean_ms': float(np.mean(latencies)),
            'median_ms': float(np.median(latencies)),
            'p95_ms': float(np.percentile(latencies, 95)),
        }
        
        # Error analysis
        errors = [ex for ex in examples if not ex['correct']]
        
        return {
            'overall_accuracy': float(overall_accuracy),
            'num_examples': len(valid_expected),
            'per_class_accuracy': dict(per_class_accuracy),
            'latency_stats': latency_stats,
            'num_errors': len(errors),
            'sample_errors': errors[:10],
        }

    def generate_report(self, results: Dict[str, Any]):
        """Generate evaluation report."""
        os.makedirs(self.args.output_dir, exist_ok=True)
        
        # JSON report
        report_path = os.path.join(self.args.output_dir, 'evaluation_report.json')
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        logger.info(f"JSON report: {report_path}")
        
        # Text summary
        summary_path = os.path.join(self.args.output_dir, 'evaluation_summary.txt')
        with open(summary_path, 'w', encoding='utf-8') as f:
            f.write("=" * 60 + "\n")
            f.write("Egyptian Voice Command Model Evaluation Report\n")
            f.write("=" * 60 + "\n\n")
            f.write(f"Overall Accuracy: {results['overall_accuracy']:.2%}\n")
            f.write(f"Number of Examples: {results['num_examples']}\n")
            f.write(f"Number of Errors: {results['num_errors']}\n\n")
            f.write("Per-Class Accuracy:\n")
            for cls, acc in sorted(results['per_class_accuracy'].items()):
                f.write(f"  {cls}: {acc:.2%}\n")
            f.write("\nLatency Statistics:\n")
            for stat, value in results['latency_stats'].items():
                f.write(f"  {stat}: {value:.2f} ms\n")
        logger.info(f"Text summary: {summary_path}")

    def run(self) -> int:
        """Run complete evaluation."""
        logger.info("=" * 60)
        logger.info("Egyptian Voice Command Model Evaluation")
        logger.info("=" * 60)
        
        self.load_model(self.args.model)
        results = self.evaluate()
        self.generate_report(results)
        
        # Print summary
        logger.info("\n" + "=" * 60)
        logger.info("Evaluation Summary")
        logger.info("=" * 60)
        logger.info(f"Overall Accuracy: {results['overall_accuracy']:.2%}")
        logger.info(f"Target (95%): {'PASSED' if results['overall_accuracy'] >= 0.95 else 'NEEDS IMPROVEMENT'}")
        logger.info(f"Mean Latency: {results['latency_stats']['mean_ms']:.2f} ms")
        logger.info("=" * 60)
        
        return 0 if results['overall_accuracy'] >= 0.95 else 4


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description='Evaluate Egyptian voice command model'
    )
    parser.add_argument('--model', type=str, required=True,
                       help='Path to fine-tuned model')
    parser.add_argument('--test-data', type=str,
                       default='datasets/egyptian_voice_commands/test.jsonl',
                       help='Path to test dataset')
    parser.add_argument('--base-model', type=str, default=None,
                       help='Path to base model for comparison')
    parser.add_argument('--output-dir', type=str, default='evaluation_results',
                       help='Output directory')
    parser.add_argument('--device', type=str, default='cuda',
                       help='Device (cuda/cpu)')
    parser.add_argument('--batch-size', type=int, default=4,
                       help='Batch size')
    parser.add_argument('--verbose', action='store_true',
                       help='Enable verbose output')
    
    args = parser.parse_args()
    
    if not EVAL_AVAILABLE:
        logger.error("Evaluation libraries not available. Install with:")
        logger.error("  pip install torch transformers scikit-learn")
        return 1
    
    if not os.path.exists(args.model):
        logger.error(f"Model not found: {args.model}")
        return 2
    
    if not os.path.exists(args.test_data):
        logger.error(f"Test data not found: {args.test_data}")
        return 2
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    evaluator = EgyptianVoiceCommandEvaluator(args)
    return evaluator.run()


if __name__ == '__main__':
    sys.exit(main())
