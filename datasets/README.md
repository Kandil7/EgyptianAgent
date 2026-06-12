---
license: mit
language:
- ar
- en
tags:
- egyptian-arabic
- voice-commands
- function-calling
- intent-classification
- mobile-actions
- ui-navigation
- low-resource-language
task_categories:
- text-classification
- token-classification
- text-generation
---

# Egyptian Voice Commands Dataset

This repository contains the **Egyptian Arabic voice commands dataset** used for training and evaluating the **EgyptianAgent** ASR and NLU models.

## Dataset Structure

```
egyptian_voice_commands/
├── train.jsonl    # Training data (665 examples)
├── eval.jsonl     # Validation data (50 examples)
└── test.jsonl     # Test data (102 examples)

egyptian_ui_navigation/
├── train.jsonl    # Training data (50 examples)
└── test.jsonl     # Test data (20 examples)
```

## Data Format

### Egyptian Voice Commands (Function Calling Format)

Each line in the JSONL files follows the **conversation format** for supervised fine-tuning:

```json
{
  "messages": [
    {
      "role": "system",
      "content": "You are a function calling assistant for Egyptian Arabic voice commands. Call functions based on user requests."
    },
    {
      "role": "user",
      "content": "اتصل بماما"
    },
    {
      "role": "assistant",
      "content": "{\"function\": \"call_contact\", \"arguments\": {\"contact_name\": \"ماما\"}}"
    }
  ]
}
```

### UI Navigation Commands

Each line contains structured metadata for mobile UI automation:

```json
{
  "id": "ui_nav_001",
  "command": "افتح الفيسبوك وشوف الأخبار",
  "command_en": "Open Facebook and check news",
  "intent_type": "ui_navigation",
  "target_app": "com.facebook.katana",
  "expected_actions": ["launch", "tap_news_feed", "scroll"],
  "is_multi_step": true,
  "difficulty": "medium",
  "expected_success": true
}
```

## Dataset Statistics

| Dataset | Split | Count | Purpose |
|---------|-------|-------|---------|
| Voice Commands | Train | 665 | Model training |
| Voice Commands | Eval | 50 | Hyperparameter tuning |
| Voice Commands | Test | 102 | Final evaluation |
| UI Navigation | Train | 50 | Model training |
| UI Navigation | Test | 20 | Final evaluation |

## Intent Categories (Voice Commands)

| Intent | Description | Example |
|--------|-------------|---------|
| CALL_CONTACT | Make a phone call | "اتصل بماما" |
| SEND_WHATSAPP | Send WhatsApp message | "ابعت واتساب لأحمد" |
| SET_ALARM | Set alarm/reminder | "نبهني بكرة الساعة تمانية" |
| OPEN_APP | Open application | "افتح اليوتيوب" |
| DEVICE_CONTROL | Control device settings | "زود الصوت" |
| READ_MESSAGES | Read notifications | "اقرأ الرسائل" |
| EMERGENCY | Emergency call | "يا نجدة" |

## Dialect Variants

- **Cairo** - Standard Egyptian Arabic
- **Alexandria** - Coastal dialect variations
- **Delta** - Lower Egypt variations
- **Upper Egypt** - Sa'idi influences

## Usage

### Loading with Hugging Face Datasets

```python
from datasets import load_dataset

# Load voice commands dataset
dataset = load_dataset("Kandil7/egyptian-voice-commands", data_dir="egyptian_voice_commands")

# Load UI navigation dataset
ui_dataset = load_dataset("Kandil7/egyptian-voice-commands", data_dir="egyptian_ui_navigation")

print(dataset["train"][0])
```

### Loading with Python (JSONL)

```python
import json

def load_dataset(split='train', dataset_type='voice_commands'):
    """Load dataset from JSONL files."""
    data = []
    dir_name = 'egyptian_voice_commands' if dataset_type == 'voice_commands' else 'egyptian_ui_navigation'
    with open(f'{dir_name}/{split}.jsonl', 'r', encoding='utf-8') as f:
        for line in f:
            data.append(json.loads(line))
    return data

# Usage
train_data = load_dataset('train', 'voice_commands')
eval_data = load_dataset('eval', 'voice_commands')
test_data = load_dataset('test', 'voice_commands')
```

### Training with EgyptianAgent

```bash
# Finetune FunctionGemma with Egyptian dataset
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

## Related Models

- **FunctionGemma-270M Egyptian (LoRA)**: [`Kandil7/functiongemma-270m-egyptian-mobile-action`](https://huggingface.co/Kandil7/functiongemma-270m-egyptian-mobile-action)
- **Merged Model**: [`Kandil7/functiongemma-270m-egyptian-mobile-action-merged`](https://huggingface.co/Kandil7/functiongemma-270m-egyptian-mobile-action-merged)

## Contributing

To contribute new commands:

1. Add to appropriate split file
2. Ensure proper intent classification
3. Include dialect variant if applicable
4. Submit pull request

## License

Dataset is licensed under **MIT License**.

## Citation

If you use this dataset in your research, please cite:

```bibtex
@dataset{egyptian_voice_commands_2026,
  title = {Egyptian Voice Commands Dataset},
  author = {EgyptianAgent Team},
  year = {2026},
  url = {https://huggingface.co/datasets/Kandil7/egyptian-voice-commands}
}
```

---

## ملخص بالعربي (Arabic Summary)

هذا المستودع يحتوي على **مجموعة بيانات الأوامر الصوتية باللهجة المصرية** المستخدمة لتطوير **EgyptianAgent** - مساعد صوتي للمستخدمين المصريين.

**المحتويات:**
- **أوامر صوتية** (665 تدريب، 50 تحقق، 102 اختبار) - بصيغة محادثات للـ Function Calling
- **تنقل واجهة المستخدم** (50 تدريب، 20 اختبار) - لأتمتة التطبيقات على الموبايل

**الاستخدام:** تطوير نماذج تفهم الأوامر المصرية الطبيعية وتحولها لإجراءات على الهاتف (اتصال، واتساب، منبه، فتح تطبيقات، إلخ).