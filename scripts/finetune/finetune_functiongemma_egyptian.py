#!/usr/bin/env python3
"""
Egyptian Agent - FunctionGemma Fine-tuning Script

Fine-tunes FunctionGemma-270M-IT for Egyptian Arabic voice commands
using LoRA (Low-Rank Adaptation) for efficient training.

USAGE:
    python finetune_functiongemma_egyptian.py [OPTIONS]

OPTIONS:
    --config PATH       Configuration file (default: configs/finetune_config.yaml)
    --data PATH         Training data path (JSONL format)
    --eval-data PATH    Evaluation data path
    --output-dir DIR    Output directory for model
    --epochs N          Number of training epochs
    --batch-size N      Training batch size
    --lr RATE           Learning rate
    --lora-r N          LoRA rank
    --device DEVICE     Device (cuda/cpu)
    --resume PATH       Resume from checkpoint
    --dry-run           Validate configuration without training

EXAMPLES:
    python finetune_functiongemma_egyptian.py --data data/train.jsonl
    python finetune_functiongemma_egyptian.py --epochs 10 --batch-size 8
    python finetune_functiongemma_egyptian.py --resume checkpoints/epoch-5

REQUIREMENTS:
    - Python 3.8+
    - PyTorch with CUDA (optional)
    - transformers, peft, datasets, trl

RETURN CODES:
    0   Success
    1   General error
    2   Configuration error
    3   Training failed
"""

import os
import sys
import json
import argparse
import logging
from pathlib import Path
from typing import Dict, List, Optional, Any
from datetime import datetime

# Configure logging
logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s"
)
logger = logging.getLogger(__name__)

# Try imports
try:
    import torch
    from transformers import (
        AutoTokenizer,
        AutoModelForCausalLM,
        TrainingArguments,
        DataCollatorForLanguageModeling,
    )
    from peft import (
        LoraConfig,
        get_peft_model,
        prepare_model_for_kbit_training,
        TaskType,
    )
    from datasets import Dataset

    TRAINING_AVAILABLE = True
except ImportError as e:
    logger.warning(f"Training libraries not available: {e}")
    TRAINING_AVAILABLE = False


def load_config(config_path: str) -> Dict[str, Any]:
    """Load configuration from YAML file."""
    import yaml

    with open(config_path, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def load_dataset_from_jsonl(file_path: str) -> List[Dict]:
    """Load dataset from JSONL file."""
    data = []
    with open(file_path, "r", encoding="utf-8") as f:
        for line in f:
            if line.strip():
                data.append(json.loads(line))
    return data


def format_example(example: Dict) -> str:
    """Format a single example for training."""
    messages = example.get("messages", [])
    formatted = ""

    for msg in messages:
        role = msg.get("role", "")
        content = msg.get("content", "")

        if role == "system":
            formatted += f"<system>\n{content}\n</system>\n"
        elif role == "user":
            formatted += f"<user>\n{content}\n</user>\n"
        elif role == "assistant":
            formatted += f"<assistant>\n{content}\n</assistant>\n"

    return formatted


def create_lora_config(args) -> LoraConfig:
    """Create LoRA configuration."""
    return LoraConfig(
        r=args.lora_r,
        lora_alpha=args.lora_r * 2,
        lora_dropout=0.05,
        target_modules=["q_proj", "v_proj", "k_proj", "o_proj"],
        bias="none",
        task_type=TaskType.CAUSAL_LM,
    )


class EgyptianVoiceCommandTrainer:
    """Main trainer class for Egyptian voice command fine-tuning."""

    def __init__(self, args):
        self.args = args
        device = args.device if args.device else "cpu"
        if device == "cuda" and not torch.cuda.is_available():
            device = "cpu"
        self.device = torch.device(device)
        logger.info(f"Using device: {self.device}")

        self.model = None
        self.tokenizer = None
        self.trainer = None

    def load_model_and_tokenizer(self):
        """Load base model and tokenizer."""
        model_name = "google/functiongemma-270m-it"
        logger.info(f"Loading model: {model_name}")

        self.tokenizer = AutoTokenizer.from_pretrained(
            model_name,
            trust_remote_code=True,
            padding_side="right",
        )

        if self.tokenizer.pad_token is None:
            self.tokenizer.pad_token = self.tokenizer.eos_token

        self.model = AutoModelForCausalLM.from_pretrained(
            model_name,
            trust_remote_code=True,
            torch_dtype=torch.float32,
        )

        if torch.cuda.is_available():
            self.model = prepare_model_for_kbit_training(self.model)

        logger.info("Model and tokenizer loaded")

    def apply_lora(self):
        """Apply LoRA to the model."""
        logger.info("Applying LoRA...")
        lora_config = create_lora_config(self.args)
        self.model = get_peft_model(self.model, lora_config)
        self.model.print_trainable_parameters()

    def load_datasets(self):
        """Load and prepare datasets."""
        logger.info("Loading datasets...")

        train_data = load_dataset_from_jsonl(self.args.data)
        logger.info(f"Loaded {len(train_data)} training examples")

        eval_data = []
        if self.args.eval_data and os.path.exists(self.args.eval_data):
            eval_data = load_dataset_from_jsonl(self.args.eval_data)
            logger.info(f"Loaded {len(eval_data)} evaluation examples")

        # Format examples
        train_texts = [format_example(ex) for ex in train_data]
        eval_texts = [format_example(ex) for ex in eval_data] if eval_data else []

        # Create datasets
        self.train_dataset = Dataset.from_dict({"text": train_texts})
        if eval_texts:
            self.eval_dataset = Dataset.from_dict({"text": eval_texts})
        else:
            self.eval_dataset = None

        logger.info("Datasets prepared")

    def create_trainer(self):
        """Create the trainer."""
        training_args = TrainingArguments(
            output_dir=self.args.output_dir,
            per_device_train_batch_size=self.args.batch_size,
            per_device_eval_batch_size=self.args.batch_size,
            gradient_accumulation_steps=4,
            learning_rate=self.args.lr,
            num_train_epochs=self.args.epochs,
            warmup_ratio=0.1,
            lr_scheduler_type="cosine",
            logging_steps=10,
            save_steps=100,
            eval_strategy="epoch" if self.eval_dataset else "no",
            save_strategy="epoch" if self.eval_dataset else "no",
            save_total_limit=2,
            fp16=torch.cuda.is_available(),
            load_best_model_at_end=True,
            report_to="none",
        )

        data_collator = DataCollatorForLanguageModeling(
            tokenizer=self.tokenizer,
            mlm=False,
        )

        from trl.trainer.sft_trainer import SFTTrainer

        self.trainer = SFTTrainer(
            model=self.model,
            args=training_args,
            train_dataset=self.train_dataset,
            eval_dataset=self.eval_dataset,
            data_collator=data_collator,
        )

        logger.info("Trainer created")

    def train(self):
        """Start training."""
        logger.info("Starting training...")
        train_result = self.trainer.train()
        logger.info(f"Training completed: {train_result.metrics}")
        return train_result.metrics

    def save_model(self):
        """Save the trained model."""
        logger.info(f"Saving model to {self.args.output_dir}...")
        self.trainer.save_model(self.args.output_dir)
        self.tokenizer.save_pretrained(self.args.output_dir)
        logger.info("Model saved")

    def run(self):
        """Run the complete training pipeline."""
        logger.info("=" * 60)
        logger.info("FunctionGemma Egyptian Dialect Fine-tuning")
        logger.info("=" * 60)

        self.load_model_and_tokenizer()
        self.apply_lora()
        self.load_datasets()
        self.create_trainer()

        if not self.args.dry_run:
            train_metrics = self.train()
            self.save_model()

            logger.info("=" * 60)
            logger.info("Training Summary")
            logger.info("=" * 60)
            logger.info(f"Training Loss: {train_metrics.get('train_loss', 'N/A')}")

        return 0


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description="Fine-tune FunctionGemma for Egyptian Arabic voice commands"
    )
    parser.add_argument(
        "--config",
        type=str,
        default="configs/finetune_config.yaml",
        help="Configuration file path",
    )
    parser.add_argument(
        "--data", type=str, required=True, help="Training data path (JSONL)"
    )
    parser.add_argument(
        "--eval-data", type=str, default=None, help="Evaluation data path"
    )
    parser.add_argument(
        "--output-dir",
        type=str,
        default="./models/functiongemma-270m-egyptian",
        help="Output directory",
    )
    parser.add_argument("--epochs", type=int, default=5, help="Number of epochs")
    parser.add_argument("--batch-size", type=int, default=4, help="Batch size")
    parser.add_argument("--lr", type=float, default=2e-4, help="Learning rate")
    parser.add_argument("--lora-r", type=int, default=16, help="LoRA rank")
    parser.add_argument("--device", type=str, default="cuda", help="Device (cuda/cpu)")
    parser.add_argument(
        "--resume", type=str, default=None, help="Resume from checkpoint"
    )
    parser.add_argument(
        "--dry-run", action="store_true", help="Validate without training"
    )

    args = parser.parse_args()

    if not TRAINING_AVAILABLE:
        logger.error("Training libraries not available. Install with:")
        logger.error("  pip install torch transformers peft datasets trl")
        return 1

    # Override config if provided
    if os.path.exists(args.config):
        config = load_config(args.config)
        for key, value in config.get("training", {}).items():
            if not hasattr(args, key.replace("-", "_")):
                setattr(args, key.replace("-", "_"), value)

    trainer = EgyptianVoiceCommandTrainer(args)
    return trainer.run()


if __name__ == "__main__":
    sys.exit(main())
