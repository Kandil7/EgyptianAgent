#!/usr/bin/env python3
"""
FunctionGemma Egyptian Dialect Fine-tuning Script

This script fine-tunes FunctionGemma-270M-IT for Egyptian Arabic voice commands
using LoRA (Low-Rank Adaptation) for efficient training.

Author: EgyptianAgent Team
Date: 2026
"""

import os
import sys
import json
import argparse
import logging
from pathlib import Path
from typing import Dict, List, Optional, Any

import torch
import yaml
from datasets import load_dataset, Dataset
from transformers import (
    AutoTokenizer,
    AutoModelForCausalLM,
    TrainingArguments,
    Trainer,
    DataCollatorForLanguageModeling,
    EarlyStoppingCallback,
)
from peft import (
    LoraConfig,
    get_peft_model,
    prepare_model_for_kbit_training,
    TaskType,
)
from trl import SFTTrainer
from sklearn.model_selection import train_test_split
import numpy as np

# Set up logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Set random seeds for reproducibility
SEED = 42
torch.manual_seed(SEED)
np.random.seed(SEED)


def load_config(config_path: str) -> Dict[str, Any]:
    """Load configuration from YAML file."""
    with open(config_path, 'r', encoding='utf-8') as f:
        return yaml.safe_load(f)


def load_dataset_from_jsonl(file_path: str) -> List[Dict]:
    """Load dataset from JSONL file."""
    data = []
    with open(file_path, 'r', encoding='utf-8') as f:
        for line in f:
            if line.strip():
                data.append(json.loads(line))
    return data


def format_example(example: Dict) -> str:
    """Format a single example for training."""
    messages = example.get('messages', [])
    formatted = ""
    
    for msg in messages:
        role = msg.get('role', '')
        content = msg.get('content', '')
        
        if role == 'system':
            formatted += f"<system>\n{content}\n</system>\n"
        elif role == 'user':
            formatted += f"<user>\n{content}\n</user>\n"
        elif role == 'assistant':
            formatted += f"<assistant>\n{content}\n</assistant>\n"
    
    return formatted


def prepare_dataset(
    data: List[Dict],
    tokenizer: AutoTokenizer,
    max_length: int = 512
) -> Dataset:
    """Prepare dataset for training."""
    formatted_texts = [format_example(example) for example in data]
    
    def tokenize_function(examples):
        tokenized = tokenizer(
            examples['text'],
            truncation=True,
            max_length=max_length,
            padding='max_length',
            return_tensors='pt'
        )
        tokenized['labels'] = tokenized['input_ids'].clone()
        return tokenized
    
    dataset = Dataset.from_dict({'text': formatted_texts})
    tokenized_dataset = dataset.map(
        tokenize_function,
        batched=True,
        remove_columns=['text']
    )
    
    return tokenized_dataset


def create_lora_config(config: Dict) -> LoraConfig:
    """Create LoRA configuration."""
    lora_config = config.get('lora', {})
    
    return LoraConfig(
        r=lora_config.get('r', 16),
        lora_alpha=lora_config.get('alpha', 32),
        lora_dropout=lora_config.get('dropout', 0.05),
        target_modules=lora_config.get(
            'target_modules',
            ['q_proj', 'v_proj', 'k_proj', 'o_proj']
        ),
        bias='none',
        task_type=TaskType.CAUSAL_LM,
    )


def compute_metrics(eval_pred):
    """Compute evaluation metrics."""
    predictions, labels = eval_pred
    
    # Decode predictions and labels
    pred_tokens = np.argmax(predictions, axis=-1)
    
    # Calculate accuracy
    accuracy = (pred_tokens == labels).mean()
    
    return {
        'accuracy': accuracy,
    }


class EgyptianVoiceCommandTrainer:
    """Main trainer class for Egyptian voice command fine-tuning."""
    
    def __init__(self, config_path: str):
        """Initialize the trainer with configuration."""
        self.config = load_config(config_path)
        self.model_config = self.config.get('model', {})
        self.training_config = self.config.get('training', {})
        self.data_config = self.config.get('data', {})
        self.output_config = self.config.get('output', {})
        
        # Set device
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        logger.info(f"Using device: {self.device}")
        
        # Initialize model and tokenizer
        self.model = None
        self.tokenizer = None
        self.trainer = None
        
    def load_model_and_tokenizer(self):
        """Load base model and tokenizer."""
        model_name = self.model_config.get('name', 'google/functiongemma-270m-it')
        trust_remote_code = self.model_config.get('trust_remote_code', True)
        
        logger.info(f"Loading model: {model_name}")
        
        # Load tokenizer
        self.tokenizer = AutoTokenizer.from_pretrained(
            model_name,
            trust_remote_code=trust_remote_code,
            padding_side='right',
        )
        
        # Set pad token if not set
        if self.tokenizer.pad_token is None:
            self.tokenizer.pad_token = self.tokenizer.eos_token
        
        # Load model
        self.model = AutoModelForCausalLM.from_pretrained(
            model_name,
            trust_remote_code=trust_remote_code,
            torch_dtype=torch.float32,
            device_map='auto' if torch.cuda.is_available() else None,
        )
        
        # Prepare model for k-bit training if using quantization
        if torch.cuda.is_available():
            self.model = prepare_model_for_kbit_training(self.model)
        
        logger.info("Model and tokenizer loaded successfully")
        
    def apply_lora(self):
        """Apply LoRA to the model."""
        logger.info("Applying LoRA configuration...")
        
        lora_config = create_lora_config(self.config)
        
        self.model = get_peft_model(self.model, lora_config)
        self.model.print_trainable_parameters()
        
        logger.info("LoRA applied successfully")
        
    def load_datasets(self):
        """Load and prepare training datasets."""
        train_path = self.data_config.get('train_path')
        eval_path = self.data_config.get('eval_path')
        test_path = self.data_config.get('test_path')
        
        logger.info("Loading datasets...")
        
        # Load training data
        train_data = load_dataset_from_jsonl(train_path)
        logger.info(f"Loaded {len(train_data)} training examples")
        
        # Load evaluation data if exists
        eval_data = []
        if eval_path and os.path.exists(eval_path):
            eval_data = load_dataset_from_jsonl(eval_path)
            logger.info(f"Loaded {len(eval_data)} evaluation examples")
        else:
            # Split training data for evaluation
            train_data, eval_data = train_test_split(
                train_data,
                test_size=0.1,
                random_state=SEED
            )
            logger.info(f"Split data: {len(train_data)} train, {len(eval_data)} eval")
        
        # Prepare datasets
        max_length = self.training_config.get('max_seq_length', 512)
        
        self.train_dataset = prepare_dataset(
            train_data,
            self.tokenizer,
            max_length
        )
        
        self.eval_dataset = prepare_dataset(
            eval_data,
            self.tokenizer,
            max_length
        )
        
        logger.info("Datasets prepared successfully")
        
    def create_trainer(self):
        """Create the trainer with training arguments."""
        output_dir = self.output_config.get('dir', './models/functiongemma-270m-egyptian')
        
        training_args = TrainingArguments(
            output_dir=output_dir,
            per_device_train_batch_size=self.training_config.get('batch_size', 4),
            per_device_eval_batch_size=self.training_config.get('batch_size', 4),
            gradient_accumulation_steps=self.training_config.get('gradient_accumulation_steps', 4),
            learning_rate=float(self.training_config.get('learning_rate', 2e-4)),
            num_train_epochs=self.training_config.get('epochs', 5),
            max_seq_length=self.training_config.get('max_seq_length', 512),
            warmup_ratio=self.training_config.get('warmup_ratio', 0.1),
            lr_scheduler_type=self.training_config.get('lr_scheduler_type', 'cosine'),
            logging_steps=self.training_config.get('logging_steps', 10),
            save_steps=self.training_config.get('save_steps', 100),
            evaluation_strategy='epoch' if self.training_config.get('evaluation_strategy') == 'epoch' else 'no',
            save_total_limit=self.training_config.get('save_total_limit', 2),
            fp16=torch.cuda.is_available(),
            load_best_model_at_end=True,
            metric_for_best_model='eval_loss',
            greater_is_better=False,
            report_to='none',
            seed=SEED,
        )
        
        # Create data collator
        data_collator = DataCollatorForLanguageModeling(
            tokenizer=self.tokenizer,
            mlm=False,
        )
        
        # Create trainer
        self.trainer = SFTTrainer(
            model=self.model,
            args=training_args,
            train_dataset=self.train_dataset,
            eval_dataset=self.eval_dataset,
            data_collator=data_collator,
            tokenizer=self.tokenizer,
            compute_metrics=compute_metrics,
            callbacks=[EarlyStoppingCallback(early_stopping_patience=3)],
        )
        
        logger.info("Trainer created successfully")
        
    def train(self):
        """Start training."""
        logger.info("Starting training...")
        
        train_result = self.trainer.train()
        
        # Log training metrics
        metrics = train_result.metrics
        logger.info(f"Training completed with metrics: {metrics}")
        
        return metrics
        
    def evaluate(self):
        """Evaluate the trained model."""
        logger.info("Evaluating model...")
        
        eval_results = self.trainer.evaluate()
        
        logger.info(f"Evaluation results: {eval_results}")
        
        return eval_results
        
    def save_model(self):
        """Save the trained model."""
        output_dir = self.output_config.get('dir', './models/functiongemma-270m-egyptian')
        
        logger.info(f"Saving model to {output_dir}...")
        
        # Save model
        self.trainer.save_model(output_dir)
        
        # Save tokenizer
        self.tokenizer.save_pretrained(output_dir)
        
        # Save training metrics
        metrics_path = os.path.join(output_dir, 'training_metrics.json')
        with open(metrics_path, 'w', encoding='utf-8') as f:
            json.dump({
                'training': self.trainer.state.log_history,
                'config': self.config,
            }, f, indent=2, ensure_ascii=False)
        
        logger.info(f"Model saved successfully to {output_dir}")
        
    def run(self):
        """Run the complete training pipeline."""
        logger.info("=" * 60)
        logger.info("FunctionGemma Egyptian Dialect Fine-tuning")
        logger.info("=" * 60)
        
        # Load model and tokenizer
        self.load_model_and_tokenizer()
        
        # Apply LoRA
        self.apply_lora()
        
        # Load datasets
        self.load_datasets()
        
        # Create trainer
        self.create_trainer()
        
        # Train
        train_metrics = self.train()
        
        # Evaluate
        eval_metrics = self.evaluate()
        
        # Save model
        self.save_model()
        
        # Print summary
        logger.info("=" * 60)
        logger.info("Training Summary")
        logger.info("=" * 60)
        logger.info(f"Training Loss: {train_metrics.get('train_loss', 'N/A')}")
        logger.info(f"Evaluation Loss: {eval_metrics.get('eval_loss', 'N/A')}")
        logger.info(f"Evaluation Accuracy: {eval_metrics.get('eval_accuracy', 'N/A')}")
        logger.info("=" * 60)
        
        return {
            'train_metrics': train_metrics,
            'eval_metrics': eval_metrics,
        }


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description='Fine-tune FunctionGemma for Egyptian Arabic voice commands'
    )
    parser.add_argument(
        '--config',
        type=str,
        default='configs/finetune_config.yaml',
        help='Path to configuration file'
    )
    parser.add_argument(
        '--output_dir',
        type=str,
        default=None,
        help='Override output directory'
    )
    parser.add_argument(
        '--epochs',
        type=int,
        default=None,
        help='Override number of training epochs'
    )
    parser.add_argument(
        '--batch_size',
        type=int,
        default=None,
        help='Override batch size'
    )
    parser.add_argument(
        '--learning_rate',
        type=float,
        default=None,
        help='Override learning rate'
    )
    
    args = parser.parse_args()
    
    # Check if config file exists
    if not os.path.exists(args.config):
        logger.error(f"Configuration file not found: {args.config}")
        sys.exit(1)
    
    # Initialize trainer
    trainer = EgyptianVoiceCommandTrainer(args.config)
    
    # Override config if specified
    if args.output_dir:
        trainer.output_config['dir'] = args.output_dir
    if args.epochs:
        trainer.training_config['epochs'] = args.epochs
    if args.batch_size:
        trainer.training_config['batch_size'] = args.batch_size
    if args.learning_rate:
        trainer.training_config['learning_rate'] = args.learning_rate
    
    # Run training
    results = trainer.run()
    
    logger.info("Training pipeline completed successfully!")
    
    return results


if __name__ == '__main__':
    main()
