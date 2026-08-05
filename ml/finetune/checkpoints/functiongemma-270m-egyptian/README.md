---
language:
- ar
- en
library_name: peft
license: gemma
tags:
- gemma
- functiongemma
- lora
- peft
- egyptian-arabic
- function-calling
- on-device
pipeline_tag: text-generation
base_model: google/functiongemma-270m-it
---

# FunctionGemma-270M Egyptian (LoRA Adapter)

This is a **PEFT/LoRA adapter** on top of **[`google/functiongemma-270m-it`](https://huggingface.co/google/functiongemma-270m-it)**,
fine-tuned for **Egyptian Arabic voice commands and mobile actions**
as part of the **EgyptianAgent** project.

The goal is to build an **on-device Egyptian Arabic assistant** that can
understand natural voice commands and trigger mobile actions via function calling.

---

## Base Model

- **Base model**: [`google/functiongemma-270m-it`](https://huggingface.co/google/functiongemma-270m-it)
- **Architecture**: Gemma 3 270M (decoder-only, function-calling optimized)
- **Tokenizer**: same as base model
- **Chat template**: stored as `chat_template.jinja` in this repo, aligned with FunctionGemma official template.

This adapter does **not** include the base model weights.
You must load the base model and then apply this adapter using PEFT.

---

## Training Data (Egyptian Voice Commands)

The adapter is trained on the `egyptian_voice_commands` dataset from the
[`Kandil7/EgyptianAgent`](https://github.com/Kandil7/EgyptianAgent) repository.

Each training example is a conversation with explicit function calling:

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

The dataset covers Egyptian Arabic voice commands mapped to mobile actions:

- **CALL_CONTACT** – phone calls to contacts
- **SEND_WHATSAPP** – WhatsApp messages (text / voice / image / location / link)
- **SET_ALARM** – alarms and reminders
- **OPEN_APP** – opening apps by name
- **DEVICE_CONTROL** – volume, brightness, Wi‑Fi, etc.
- **EMERGENCY_CALL** – emergency contacts or services
- (and other utility actions used by EgyptianAgent)

The utterances are written in **Egyptian Arabic** (Cairo / Delta style),
with natural phrasing like “اتصل بماما”, “ابعت لآدم على الواتساب”, “نبهني بكرة الساعة تمانية الصبح”, etc.

---

## Training Setup (LoRA / PEFT)

The adapter was trained with **parameter-efficient fine-tuning (LoRA)** using:

- **Library**: TRL + PEFT + Transformers
- **PEFT method**: LoRA
- **Target modules**: `q_proj`, `k_proj`, `v_proj`, `o_proj` (Gemma attention blocks)
- **Hyperparameters** (aligned with FunctionGemma mobile-actions recipe):

  - `r`: 8
  - `lora_alpha`: 16
  - `lora_dropout`: 0.05
  - `bias`: `"none"`
  - `task_type`: `"CAUSAL_LM"`

- **Sequence length**: 512
- **Batch size**: tuned for consumer GPU (gradient accumulation)
- **Optimizer / LR schedule**: AdamW with cosine schedule

The LoRA adapter adds only a small number of parameters on top of the base model,
so the final model remains **~270M parameters** and suitable for **on-device deployment**.

> **Note**: These hyperparameters differ slightly from the official FunctionGemma mobile-actions recipe (which uses `r=16, alpha=32`). This was a deliberate choice for faster training on consumer hardware. For production deployment, consider retraining with the official recipe.

---

## Usage (Transformers + PEFT)

### 1. Load Base Model and Adapter

```python
from transformers import AutoTokenizer, AutoModelForCausalLM
from peft import PeftModel

base_model_id = "google/functiongemma-270m-it"
adapter_id = "Kandil7/functiongemma-270m-egyptian-mobile-action"

tokenizer = AutoTokenizer.from_pretrained(base_model_id)
base_model = AutoModelForCausalLM.from_pretrained(base_model_id, device_map="auto")

model = PeftModel.from_pretrained(base_model, adapter_id)
model.eval()
```

### 2. Run an Egyptian Command (Function Calling)

```python
messages = [
    {
        "role": "system",
        "content": "You are a function calling assistant for Egyptian Arabic voice commands. "
                   "Always respond with a JSON object describing the function to call."
    },
    {
        "role": "user",
        "content": "ابعت لأحمد رسالة على الواتساب قوله إني هتأخر شوية"
    }
]

inputs = tokenizer.apply_chat_template(
    messages,
    return_tensors="pt",
    add_generation_prompt=True
).to(model.device)

outputs = model.generate(
    inputs,
    max_new_tokens=128,
    temperature=0.1,
    do_sample=False
)

response = tokenizer.decode(outputs[0], skip_special_tokens=True)
print(response)
```

The expected output is a JSON string like:

```json
{
  "function": "send_whatsapp",
  "arguments": {
    "contact_name": "أحمد",
    "message_text": "إني هتأخر شوية"
  }
}
```

You can then parse this JSON in your app and call the corresponding mobile action.

---

## Intended Use and Limitations

**Intended Use:**

- On-device or edge deployment for **Egyptian Arabic voice assistants**
- Intent detection and function calling for **mobile actions**
- Integration with projects like **EgyptianAgent** targeting:
  - elderly users
  - visually impaired users
  - offline / privacy-preserving setups

**Limitations:**

- Trained mainly on **Egyptian Arabic**; other dialects may be less accurate
- Focused on **mobile actions** (calls, WhatsApp, alarms, device control),
  not general-purpose chat or open-domain QA
- Function signatures and arguments are tailored to EgyptianAgent's tool definitions

---

## How to Cite / Credit

If you use this adapter, please cite:

- `google/functiongemma-270m-it` as the base model
- `Kandil7/EgyptianAgent` as the source of Egyptian voice commands and integration code

---

## ملخص بالعربي (Short Arabic Summary)

هذا النموذج هو **محوّل LoRA** فوق
`google/functiongemma-270m-it`،
مُدرَّب على **أوامر صوتية باللهجة المصرية** لتحويلها إلى **استدعاءات دوال (Function Calling)** خاصة بأوامر الموبايل (اتصال، واتساب، منبه، فتح تطبيق، إلخ).

الغرض الرئيسي:

- بناء **مساعد شخصي** يعمل بالكامل على الجهاز،
- يفهم أوامر المصريين الطبيعية،
- ويحوّلها إلى أوامر منظّمة (JSON) يمكن تنفيذها على الهاتف.

طريقة الاستخدام:

1. تحميل الموديل الأساسي `google/functiongemma-270m-it`.
2. تحميل الـ adapter `Kandil7/functiongemma-270m-egyptian-mobile-action`.
3. تمرير الأوامر المصرية (نصًا أو من تفريغ صوتي) والحصول على JSON يحدِّد الدالة والباراميترات.

هذا العمل جزء من مشروع **EgyptianAgent**، المصمم خصيصًا لخدمة المستخدم المصري على الأجهزة الضعيفة.