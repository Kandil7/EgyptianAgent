# Configuration Files

This directory contains configuration files for the EgyptianAgent project.

## Configuration Files

| File | Purpose |
|------|---------|
| `finetune_config.yaml` | Model finetuning configuration |

## Configuration Structure

### Finetune Configuration (`finetune_config.yaml`)

```yaml
# Model Configuration
model:
  name: "functiongemma-egyptian"
  base_model: "gemma-2b"
  quantization: "Q4_K_M"
  max_seq_length: 512

# Training Configuration
training:
  batch_size: 32
  learning_rate: 2e-5
  num_epochs: 10
  warmup_steps: 100
  weight_decay: 0.01

# Data Configuration
data:
  train_path: "datasets/egyptian_voice_commands/train.jsonl"
  eval_path: "datasets/egyptian_voice_commands/eval.jsonl"
  test_path: "datasets/egyptian_voice_commands/test.jsonl"
  max_samples: null  # null = use all

# Output Configuration
output:
  dir: "models/finetuned"
  save_steps: 500
  logging_steps: 50

# Egyptian Dialect Specific
egyptian:
  normalize_hamza: true
  normalize_alif: true
  remove_tatweel: true
  dialect_variant: "cairo"
```

## Using Configuration

### Load Configuration

```python
import yaml

def load_config(config_path='configs/finetune_config.yaml'):
    with open(config_path, 'r') as f:
        return yaml.safe_load(f)
```

### Override Configuration

```bash
# Override specific values via command line
python scripts/finetune/finetune_functiongemma_egyptian.py \
    --config configs/finetune_config.yaml \
    --override training.learning_rate=1e-5 \
    --override training.num_epochs=20
```

## Creating New Configurations

1. Copy existing configuration
2. Modify parameters as needed
3. Test with small dataset first
4. Document changes

## Best Practices

- Keep sensitive values (API keys, passwords) out of config files
- Use environment variables for secrets
- Version control configuration files
- Document all configuration options

## Related Documentation

- [Finetuning Guide](../docs/guides/FUNCTIONGEMMA_FINETUNING_GUIDE.md)
- [Deployment Guide](../docs/deployment/DEPLOYMENT_GUIDE.md)
- [Scripts](../scripts/README.md)
