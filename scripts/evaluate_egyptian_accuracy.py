#!/usr/bin/env python3
"""
Egyptian Voice Command Model Evaluation Script

This script evaluates the fine-tuned FunctionGemma model on Egyptian Arabic
voice commands and generates comprehensive accuracy reports.

Author: EgyptianAgent Team
Date: 2026
"""

import os
import sys
import json
import argparse
import logging
from pathlib import Path
from typing import Dict, List, Tuple, Any, Optional
from collections import defaultdict
import time

import torch
import numpy as np
from transformers import AutoTokenizer, AutoModelForCausalLM
from sklearn.metrics import (
    accuracy_score,
    precision_recall_fscore_support,
    confusion_matrix,
    classification_report,
)
import matplotlib.pyplot as plt
import seaborn as sns

# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Set random seeds
SEED = 42
torch.manual_seed(SEED)
np.random.seed(SEED)


class EgyptianVoiceCommandEvaluator:
    """Evaluator for Egyptian voice command models."""
    
    # Function category mapping
    FUNCTION_CATEGORIES = {
        'call_contact': 'CALL_CONTACT',
        'send_whatsapp': 'SEND_WHATSAPP',
        'set_alarm': 'SET_ALARM',
        'emergency': 'EMERGENCY',
        'read_time': 'READ_TIME',
        'open_app': 'OPEN_APP',
        'device_control': 'DEVICE_CONTROL',
        'send_voice_message': 'SEND_VOICE_MESSAGE',
        'read_missed_calls': 'READ_MISSED_CALLS',
        'read_contacts': 'READ_CONTACTS',
    }
    
    def __init__(
        self,
        model_path: str,
        test_data_path: str,
        base_model_path: Optional[str] = None,
    ):
        """Initialize the evaluator."""
        self.model_path = model_path
        self.test_data_path = test_data_path
        self.base_model_path = base_model_path
        
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        logger.info(f"Using device: {self.device}")
        
        self.model = None
        self.tokenizer = None
        self.base_model = None
        
    def load_model(self):
        """Load the fine-tuned model."""
        logger.info(f"Loading model from {self.model_path}...")
        
        self.tokenizer = AutoTokenizer.from_pretrained(
            self.model_path,
            trust_remote_code=True,
            padding_side='left',
        )
        
        if self.tokenizer.pad_token is None:
            self.tokenizer.pad_token = self.tokenizer.eos_token
        
        self.model = AutoModelForCausalLM.from_pretrained(
            self.model_path,
            trust_remote_code=True,
            torch_dtype=torch.float32,
            device_map='auto' if torch.cuda.is_available() else None,
        )
        
        if not torch.cuda.is_available():
            self.model = self.model.to(self.device)
        
        self.model.eval()
        logger.info("Model loaded successfully")
        
    def load_base_model(self):
        """Load the base model for comparison."""
        if not self.base_model_path:
            logger.info("No base model path provided, skipping base model comparison")
            return
        
        logger.info(f"Loading base model from {self.base_model_path}...")
        
        base_tokenizer = AutoTokenizer.from_pretrained(
            self.base_model_path,
            trust_remote_code=True,
            padding_side='left',
        )
        
        if base_tokenizer.pad_token is None:
            base_tokenizer.pad_token = base_tokenizer.eos_token
        
        self.base_model = AutoModelForCausalLM.from_pretrained(
            self.base_model_path,
            trust_remote_code=True,
            torch_dtype=torch.float32,
            device_map='auto' if torch.cuda.is_available() else None,
        )
        
        if not torch.cuda.is_available():
            self.base_model = self.base_model.to(self.device)
        
        self.base_model.eval()
        logger.info("Base model loaded successfully")
        
    def load_test_data(self) -> List[Dict]:
        """Load test dataset."""
        logger.info(f"Loading test data from {self.test_data_path}...")
        
        test_data = []
        with open(self.test_data_path, 'r', encoding='utf-8') as f:
            for line in f:
                if line.strip():
                    test_data.append(json.loads(line))
        
        logger.info(f"Loaded {len(test_data)} test examples")
        return test_data
    
    def extract_function_from_response(self, response: str) -> Optional[str]:
        """Extract function name from model response."""
        try:
            # Try to parse as JSON
            response = response.strip()
            
            # Find JSON in response
            start_idx = response.find('{')
            end_idx = response.rfind('}') + 1
            
            if start_idx >= 0 and end_idx > start_idx:
                json_str = response[start_idx:end_idx]
                parsed = json.loads(json_str)
                return parsed.get('function')
        except (json.JSONDecodeError, Exception) as e:
            logger.debug(f"Failed to parse response: {e}")
        
        return None
    
    def extract_expected_function(self, example: Dict) -> Optional[str]:
        """Extract expected function from example."""
        messages = example.get('messages', [])
        
        for msg in messages:
            if msg.get('role') == 'assistant':
                content = msg.get('content', '')
                return self.extract_function_from_response(content)
        
        return None
    
    def predict(self, user_input: str, max_new_tokens: int = 128) -> str:
        """Generate prediction for user input."""
        # Format input
        prompt = f"<system>\nYou are a function calling assistant for Egyptian Arabic voice commands. Call functions based on user requests.\n</system>\n<user>\n{user_input}\n</user>\n<assistant>\n"
        
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
        
        # Decode output
        generated = self.tokenizer.decode(outputs[0], skip_special_tokens=True)
        
        # Extract assistant response
        if '<assistant>\n' in generated:
            response = generated.split('<assistant>\n')[-1].strip()
        else:
            response = generated
        
        return response
    
    def predict_base(self, user_input: str, max_new_tokens: int = 128) -> str:
        """Generate prediction using base model."""
        if self.base_model is None:
            return ""
        
        prompt = f"<system>\nYou are a function calling assistant for Egyptian Arabic voice commands. Call functions based on user requests.\n</system>\n<user>\n{user_input}\n</user>\n<assistant>\n"
        
        inputs = self.tokenizer(
            prompt,
            return_tensors='pt',
            truncation=True,
            max_length=512,
        ).to(self.device)
        
        with torch.no_grad():
            outputs = self.base_model.generate(
                **inputs,
                max_new_tokens=max_new_tokens,
                do_sample=False,
                temperature=0.0,
                pad_token_id=self.tokenizer.eos_token_id,
            )
        
        generated = self.tokenizer.decode(outputs[0], skip_special_tokens=True)
        
        if '<assistant>\n' in generated:
            response = generated.split('<assistant>\n')[-1].strip()
        else:
            response = generated
        
        return response
    
    def evaluate(self) -> Dict[str, Any]:
        """Run full evaluation."""
        logger.info("Starting evaluation...")
        
        test_data = self.load_test_data()
        
        predictions = []
        expected = []
        latencies = []
        examples_with_details = []
        
        # Evaluate each example
        for i, example in enumerate(test_data):
            messages = example.get('messages', [])
            
            # Find user input
            user_input = None
            for msg in messages:
                if msg.get('role') == 'user':
                    user_input = msg.get('content', '')
                    break
            
            if not user_input:
                continue
            
            # Get expected function
            expected_function = self.extract_expected_function(example)
            
            # Measure latency
            start_time = time.time()
            response = self.predict(user_input)
            latency = (time.time() - start_time) * 1000  # Convert to ms
            latencies.append(latency)
            
            # Extract predicted function
            predicted_function = self.extract_function_from_response(response)
            
            predictions.append(predicted_function)
            expected.append(expected_function)
            
            # Store details
            examples_with_details.append({
                'user_input': user_input,
                'expected_function': expected_function,
                'predicted_function': predicted_function,
                'response': response,
                'latency_ms': latency,
                'correct': predicted_function == expected_function,
            })
            
            if (i + 1) % 20 == 0:
                logger.info(f"Evaluated {i + 1}/{len(test_data)} examples")
        
        # Calculate metrics
        results = self.calculate_metrics(predictions, expected, latencies, examples_with_details)
        
        return results
    
    def evaluate_base_model(self) -> Dict[str, Any]:
        """Evaluate base model for comparison."""
        if self.base_model is None:
            return {}
        
        logger.info("Evaluating base model...")
        
        test_data = self.load_test_data()
        
        predictions = []
        expected = []
        
        for example in test_data:
            messages = example.get('messages', [])
            
            user_input = None
            for msg in messages:
                if msg.get('role') == 'user':
                    user_input = msg.get('content', '')
                    break
            
            if not user_input:
                continue
            
            expected_function = self.extract_expected_function(example)
            response = self.predict_base(user_input)
            predicted_function = self.extract_function_from_response(response)
            
            predictions.append(predicted_function)
            expected.append(expected_function)
        
        # Calculate accuracy
        correct = sum(1 for p, e in zip(predictions, expected) if p == e)
        accuracy = correct / len(predictions) if predictions else 0
        
        return {
            'accuracy': accuracy,
            'num_examples': len(predictions),
        }
    
    def calculate_metrics(
        self,
        predictions: List[str],
        expected: List[str],
        latencies: List[float],
        examples: List[Dict],
    ) -> Dict[str, Any]:
        """Calculate comprehensive evaluation metrics."""
        
        # Filter out None values
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
        
        # Precision, Recall, F1
        labels = list(set(valid_expected + valid_predictions))
        precision, recall, f1, support = precision_recall_fscore_support(
            valid_expected,
            valid_predictions,
            labels=labels,
            average=None,
            zero_division=0,
        )
        
        per_class_metrics = {}
        for i, label in enumerate(labels):
            per_class_metrics[label] = {
                'precision': float(precision[i]),
                'recall': float(recall[i]),
                'f1': float(f1[i]),
                'support': int(support[i]),
            }
        
        # Macro averages
        macro_precision = float(np.mean(precision))
        macro_recall = float(np.mean(recall))
        macro_f1 = float(np.mean(f1))
        
        # Confusion matrix
        label_to_idx = {label: i for i, label in enumerate(labels)}
        idx_to_label = {i: label for label, i in label_to_idx.items()}
        
        y_true_idx = [label_to_idx.get(e, -1) for e in valid_expected]
        y_pred_idx = [label_to_idx.get(p, -1) for p in valid_predictions]
        
        cm = confusion_matrix(y_true_idx, y_pred_idx, labels=list(range(len(labels))))
        
        # Latency statistics
        latency_stats = {
            'mean_ms': float(np.mean(latencies)),
            'median_ms': float(np.median(latencies)),
            'std_ms': float(np.std(latencies)),
            'min_ms': float(np.min(latencies)),
            'max_ms': float(np.max(latencies)),
            'p95_ms': float(np.percentile(latencies, 95)),
            'p99_ms': float(np.percentile(latencies, 99)),
        }
        
        # JSON validity check
        json_valid = sum(1 for ex in examples if ex['predicted_function'] is not None)
        json_validity_rate = json_valid / len(examples) if examples else 0
        
        # Error analysis
        errors = [ex for ex in examples if not ex['correct']]
        error_by_category = defaultdict(list)
        for error in errors:
            expected_cat = error['expected_function']
            if expected_cat:
                error_by_category[expected_cat].append(error)
        
        results = {
            'overall_accuracy': float(overall_accuracy),
            'num_examples': len(valid_expected),
            'per_class_accuracy': per_class_accuracy,
            'per_class_metrics': per_class_metrics,
            'macro_precision': macro_precision,
            'macro_recall': macro_recall,
            'macro_f1': macro_f1,
            'confusion_matrix': cm.tolist(),
            'labels': labels,
            'latency_stats': latency_stats,
            'json_validity_rate': float(json_validity_rate),
            'num_errors': len(errors),
            'error_by_category': {k: len(v) for k, v in error_by_category.items()},
            'sample_errors': errors[:10],  # First 10 errors for analysis
        }
        
        return results
    
    def generate_report(self, results: Dict[str, Any], output_dir: str):
        """Generate comprehensive evaluation report."""
        os.makedirs(output_dir, exist_ok=True)
        
        # Save JSON report
        report_path = os.path.join(output_dir, 'evaluation_report.json')
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        logger.info(f"JSON report saved to {report_path}")
        
        # Generate text summary
        summary_path = os.path.join(output_dir, 'evaluation_summary.txt')
        with open(summary_path, 'w', encoding='utf-8') as f:
            f.write("=" * 60 + "\n")
            f.write("Egyptian Voice Command Model Evaluation Report\n")
            f.write("=" * 60 + "\n\n")
            
            f.write(f"Overall Accuracy: {results['overall_accuracy']:.2%}\n")
            f.write(f"Number of Examples: {results['num_examples']}\n")
            f.write(f"JSON Validity Rate: {results['json_validity_rate']:.2%}\n")
            f.write(f"Number of Errors: {results['num_errors']}\n\n")
            
            f.write("Macro Averages:\n")
            f.write(f"  Precision: {results['macro_precision']:.4f}\n")
            f.write(f"  Recall: {results['macro_recall']:.4f}\n")
            f.write(f"  F1 Score: {results['macro_f1']:.4f}\n\n")
            
            f.write("Per-Class Accuracy:\n")
            for cls, acc in sorted(results['per_class_accuracy'].items()):
                f.write(f"  {cls}: {acc:.2%}\n")
            f.write("\n")
            
            f.write("Latency Statistics:\n")
            for stat, value in results['latency_stats'].items():
                f.write(f"  {stat}: {value:.2f} ms\n")
            f.write("\n")
            
            f.write("Error Distribution by Category:\n")
            for cat, count in sorted(results['error_by_category'].items()):
                f.write(f"  {cat}: {count} errors\n")
            f.write("\n")
            
            if results['sample_errors']:
                f.write("Sample Errors:\n")
                for i, error in enumerate(results['sample_errors'][:5], 1):
                    f.write(f"\n{i}. Input: {error['user_input']}\n")
                    f.write(f"   Expected: {error['expected_function']}\n")
                    f.write(f"   Predicted: {error['predicted_function']}\n")
        
        logger.info(f"Text summary saved to {summary_path}")
        
        # Generate confusion matrix plot
        self.plot_confusion_matrix(results, output_dir)
        
        # Generate per-class accuracy plot
        self.plot_per_class_accuracy(results, output_dir)
        
    def plot_confusion_matrix(self, results: Dict[str, Any], output_dir: str):
        """Plot confusion matrix."""
        cm = np.array(results['confusion_matrix'])
        labels = results['labels']
        
        plt.figure(figsize=(12, 10))
        sns.heatmap(
            cm,
            annot=True,
            fmt='d',
            cmap='Blues',
            xticklabels=labels,
            yticklabels=labels,
        )
        plt.title('Confusion Matrix - Egyptian Voice Commands')
        plt.xlabel('Predicted')
        plt.ylabel('True')
        plt.xticks(rotation=45, ha='right')
        plt.yticks(rotation=0)
        plt.tight_layout()
        
        plot_path = os.path.join(output_dir, 'confusion_matrix.png')
        plt.savefig(plot_path, dpi=150, bbox_inches='tight')
        plt.close()
        
        logger.info(f"Confusion matrix plot saved to {plot_path}")
    
    def plot_per_class_accuracy(self, results: Dict[str, Any], output_dir: str):
        """Plot per-class accuracy."""
        classes = list(results['per_class_accuracy'].keys())
        accuracies = list(results['per_class_accuracy'].values())
        
        plt.figure(figsize=(14, 6))
        bars = plt.bar(classes, accuracies, color='steelblue')
        
        # Add value labels on bars
        for bar, acc in zip(bars, accuracies):
            plt.text(
                bar.get_x() + bar.get_width() / 2,
                bar.get_height() + 0.01,
                f'{acc:.1%}',
                ha='center',
                va='bottom',
                rotation=45,
                fontsize=8,
            )
        
        plt.title('Per-Class Accuracy - Egyptian Voice Commands')
        plt.xlabel('Function Category')
        plt.ylabel('Accuracy')
        plt.ylim(0, 1.1)
        plt.xticks(rotation=45, ha='right')
        plt.axhline(y=results['overall_accuracy'], color='red', linestyle='--', 
                   label=f"Overall: {results['overall_accuracy']:.1%}")
        plt.legend()
        plt.tight_layout()
        
        plot_path = os.path.join(output_dir, 'per_class_accuracy.png')
        plt.savefig(plot_path, dpi=150, bbox_inches='tight')
        plt.close()
        
        logger.info(f"Per-class accuracy plot saved to {plot_path}")
    
    def run(self, output_dir: str = 'evaluation_results') -> Dict[str, Any]:
        """Run complete evaluation pipeline."""
        logger.info("=" * 60)
        logger.info("Egyptian Voice Command Model Evaluation")
        logger.info("=" * 60)
        
        # Load models
        self.load_model()
        self.load_base_model()
        
        # Evaluate fine-tuned model
        results = self.evaluate()
        
        # Evaluate base model for comparison
        if self.base_model:
            base_results = self.evaluate_base_model()
            results['base_model_accuracy'] = base_results.get('accuracy', 0)
            results['improvement'] = (
                results['overall_accuracy'] - results['base_model_accuracy']
            )
            
            logger.info(f"\nBase Model Accuracy: {results['base_model_accuracy']:.2%}")
            logger.info(f"Fine-tuned Model Accuracy: {results['overall_accuracy']:.2%}")
            logger.info(f"Improvement: {results['improvement']:.2%}")
        
        # Generate report
        self.generate_report(results, output_dir)
        
        # Print summary
        logger.info("\n" + "=" * 60)
        logger.info("Evaluation Summary")
        logger.info("=" * 60)
        logger.info(f"Overall Accuracy: {results['overall_accuracy']:.2%}")
        logger.info(f"Target Accuracy (95%): {'✓ PASSED' if results['overall_accuracy'] >= 0.95 else '✗ NEEDS IMPROVEMENT'}")
        logger.info(f"Mean Latency: {results['latency_stats']['mean_ms']:.2f} ms")
        logger.info(f"JSON Validity: {results['json_validity_rate']:.2%}")
        logger.info("=" * 60)
        
        return results


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description='Evaluate Egyptian voice command model'
    )
    parser.add_argument(
        '--model',
        type=str,
        required=True,
        help='Path to fine-tuned model'
    )
    parser.add_argument(
        '--test_data',
        type=str,
        default='datasets/egyptian_voice_commands/test.jsonl',
        help='Path to test dataset'
    )
    parser.add_argument(
        '--base_model',
        type=str,
        default=None,
        help='Path to base model for comparison'
    )
    parser.add_argument(
        '--output_dir',
        type=str,
        default='evaluation_results',
        help='Output directory for reports'
    )
    
    args = parser.parse_args()
    
    # Check if model exists
    if not os.path.exists(args.model):
        logger.error(f"Model not found: {args.model}")
        sys.exit(1)
    
    # Check if test data exists
    if not os.path.exists(args.test_data):
        logger.error(f"Test data not found: {args.test_data}")
        sys.exit(1)
    
    # Initialize evaluator
    evaluator = EgyptianVoiceCommandEvaluator(
        model_path=args.model,
        test_data_path=args.test_data,
        base_model_path=args.base_model,
    )
    
    # Run evaluation
    results = evaluator.run(output_dir=args.output_dir)
    
    # Exit with appropriate code
    if results['overall_accuracy'] >= 0.95:
        logger.info("\n✓ Model meets target accuracy of 95%!")
        sys.exit(0)
    else:
        logger.warning("\n✗ Model does not meet target accuracy of 95%")
        sys.exit(1)


if __name__ == '__main__':
    main()
