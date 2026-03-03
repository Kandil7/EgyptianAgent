# Egyptian Voice Commands Dataset

This directory contains the Egyptian Arabic voice commands dataset used for training and evaluating the EgyptianAgent ASR and NLU models.

## Dataset Structure

```
egyptian_voice_commands/
├── train.jsonl    # Training data (80%)
├── eval.jsonl     # Validation data (10%)
└── test.jsonl     # Test data (10%)
```

## Data Format

Each line in the JSONL files follows this format:

```json
{
  "id": "unique_command_id",
  "text": "اتصل بأمي",
  "transcription": "اتصل بأمي",
  "intent": "CALL_CONTACT",
  "entities": {
    "contact_name": "أمي"
  },
  "audio_path": "optional_path_to_audio.wav",
  "speaker_id": "optional_speaker_id",
  "dialect_variant": "cairo",
  "confidence": 1.0
}
```

## Dataset Statistics

| Split | Count | Purpose |
|-------|-------|---------|
| Train | 10,000+ | Model training |
| Eval | 1,250+ | Hyperparameter tuning |
| Test | 1,250+ | Final evaluation |

## Intent Categories

| Intent | Description | Example |
|--------|-------------|---------|
| CALL_CONTACT | Make a phone call | "اتصل بأمي" |
| SEND_WHATSAPP | Send WhatsApp message | "ابعت واتساب" |
| SET_ALARM | Set alarm/reminder | "نبهني بكرة" |
| GET_INFO | Get information | "الجو عامل إيه" |
| EMERGENCY | Emergency call | "يا نجدة" |

## Dialect Variants

- **Cairo** - Standard Egyptian Arabic
- **Alexandria** - Coastal dialect variations
- **Delta** - Lower Egypt variations
- **Upper Egypt** - Sa'idi influences

## Usage

### Loading Dataset

```python
import json

def load_dataset(split='train'):
    data = []
    with open(f'egyptian_voice_commands/{split}.jsonl', 'r', encoding='utf-8') as f:
        for line in f:
            data.append(json.loads(line))
    return data
```

### Training

```bash
# Finetune model with Egyptian dataset
python scripts/finetune/finetune_functiongemma_egyptian.py \
    --train_data datasets/egyptian_voice_commands/train.jsonl \
    --eval_data datasets/egyptian_voice_commands/eval.jsonl
```

### Evaluation

```bash
# Evaluate model accuracy
python scripts/finetune/evaluate_egyptian_accuracy.py \
    --test_data datasets/egyptian_voice_commands/test.jsonl
```

## Contributing

To contribute new commands:

1. Add to appropriate split file
2. Ensure proper intent classification
3. Include dialect variant if applicable
4. Submit pull request

## License

Dataset is licensed under the same license as the main project (MIT).

## Citation

If you use this dataset in your research, please cite:

```
@dataset{egyptian_voice_commands_2026,
  title = {Egyptian Voice Commands Dataset},
  author = {EgyptianAgent Team},
  year = {2026},
  url = {https://github.com/your-org/egyptian-agent}
}
```
