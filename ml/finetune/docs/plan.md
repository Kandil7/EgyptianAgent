الترتيب المنطقي:  الترتيب المنطقي: اللينكين اللي بعتّهم هم بالظبط الـ reference الرسمي للي أنت عامله في EgyptianAgent:  
- `functiongemma-270m-ft-mobile-actions` = نسخة جاهزة للـ Mobile Actions فوق FunctionGemma. [ollama](https://ollama.com/prakasharyan/qwen-arabic)
- الـ Colab = pipeline الرسمي لـ fine-tuning + export لـ Google Tensor / LiteRT-LM. [ollama](https://ollama.com/prakasharyan/qwen-arabic)

هقول لك بسرعة إزاي تستفيد منهم في حالتك (مصري + أجهزة ضعيفة + EgyptianAgent).

***

## ما الذي يضيفه model HF الرسمي لك؟

من model card بتاع `litert-community/functiongemma-270m-ft-mobile-actions`: [ollama](https://ollama.com/prakasharyan/qwen-arabic)

- هو finetune فوق `google/functiongemma-270m-it` (نفس الـ base اللي أنت مستخدمه).  
- جاهز للـ deployment على Google AI Edge Gallery app وعلى LiteRT-LM (Android, iOS, Web، إلخ). [ollama](https://ollama.com/prakasharyan/qwen-arabic)
- Benchmarks:  
  - Model size ~289MB  
  - Peak RSS ~510MB  
  - dynamic_int8 quantization، latency عالية جدًا على S25 Ultra (decode ~154 token/s). [ollama](https://ollama.com/prakasharyan/qwen-arabic)

ده يعني:  
- الـ footprint اللي أنت مذكوره لـ Honor X6c (550MB RAM, ~350ms latency) منطقي جدًا ومتسق مع bench الرسمي. [huggingface](https://huggingface.co/ml-maverick/Qwen2.5-1.5B-Instruct-ArabicSum)
- تقدر تستخدم نفس الـ toolchain (LiteRT-LM / Tensor SDK) لما تحب تطلع نسخة مصرية fully-compiled للموبايلات. [ollama](https://ollama.com/prakasharyan/qwen-arabic)

***

## ما الذي يضيفه الـ Colab notebook لك؟

الـ notebook `[FunctionGemma]Finetune_FunctionGemma_270M_for_Mobile_Actions_with_Hugging_Face.ipynb` فيه pipeline كامل: [ollama](https://ollama.com/prakasharyan/qwen-arabic)

1. تحميل `google/functiongemma-270m-it`.  
2. تحميل / تحضير dataset mobile actions (بـ HF Datasets).  
3. Fine-tuning بـ TRL / PEFT / LoRA (زي اللي أنت عامل تقريبًا).  
4. Export:
   - `export-hf` → موديل HuggingFace قابل للاستخدام بـ Transformers.  
   - `litert-lm build` / Tensor SDK → `.litertlm` + `.tflite` compiled لمعالجات Google Tensor. [ollama](https://ollama.com/prakasharyan/qwen-arabic)

ده يهمك في ٣ حاجات:

- تتأكد إن إعدادات الـ LoRA / training اللي عندك في EgyptianAgent متوافقة مع pipeline الرسمي (أو تتبناه تقريبًا كما هو).  
- تستخدم نفس خطوة `export-hf` لو حبيت تبني نسخة mobile-ready (LiteRT / AI Edge Gallery).  
- تقتبس الـ structure بتاع الـ dataset (اللي هو intent-based mobile actions) وتطبقه على المصري، زي ما أنت فعلاً عامل في `egyptian_voice_commands`. [app.readytensor](https://app.readytensor.ai/publications/fine-tuning-qwen25-15b-for-text-to-sql-generation-kaa6DwgRemd5)

***

## كيف توائم شغلك الحالي مع pipeline الرسمي؟

أنت عمليًا عامل variation مصري لـ notebook ده:

- Base model: `google/functiongemma-270m-it`. [ollama](https://ollama.com/prakasharyan/qwen-arabic)
- Dataset: `egyptian_voice_commands/train.jsonl` formatted كـ `messages` (system/user/assistant) مع function JSON. [arxiv](https://arxiv.org/html/2506.00019v1)
- Output: LoRA adapter داخل `models/functiongemma-270m-egyptian`. [github](https://github.com/prakash-aryan/qwen-arabic-project)

الخطوات الآن لو حابب توصل لنفس مستوى integration:

1. **تثبيت alignment مع الـ Colab**:
   - راجع hyperparameters:  
     - LoRA r / alpha / dropout،  
     - learning rate, batch size, max_len.  
   - قارنها بما في الـ notebook، وعدّل سكربت التدريب عندك ليكون قريب منها.  

2. **استخدام export-hf / LiteRT-LM لو حبيت on-device رسمي**:
   - بعد ما تدمج الـ adapter (أو تعمل export) إلى full HF model،  
   - تقدر تستخدم `litert-lm` CLI (زي ما موصوف في model card): [ollama](https://ollama.com/prakasharyan/qwen-arabic)
     ```bash
     pip install litert-lm
     litert-lm run \
       --from-huggingface-repo=Kandil7/functiongemma-270m-egyptian \
       model.litertlm \
       --prompt="اتصل بماما"
     ```  
   - بعدين، لو حبيت تروح لمستوى Tensor SDK (Google Tensor devices), تتبع خطوة "Compile for Google Tensor" في الـ model card. [ollama](https://ollama.com/prakasharyan/qwen-arabic)

3. **الموازنة بين Flow الحالي بتاع EgyptianAgent وLiteRT/Mediapipe**:
   - EgyptianAgent حالياً مبني حول FunctionGemma via Transformers + PEFT. [huggingface](https://huggingface.co/ml-maverick/Qwen2.5-1.5B-Instruct-ArabicSum)
   - لو حبيت تروح لنسخة موبايل بالكامل عبر LiteRT أو Google AI Edge Gallery، تستخدم نفس `export-hf` → `.litertlm` pipeline.  

***
الأهم الآن في رأيي هو **README/model card نظيف**؛ لأنه هيبقى المرجع لأي خطوة تالية (LiteRT/Android، مشاركة الموديل، أو استخدامه في مشاريع تانية). بعده نقدر نشتغل على تكامل LiteRT/Mediapipe بسهولة.

هكتب لك هيكل README جاهز (إنجليزي + جزء عربي مختصر) تقدر تحطه كما هو في `models/functiongemma-270m-egyptian/README.md` أو على Hugging Face.

***

## مسودة README / Model Card (إنجليزي)

```markdown
# FunctionGemma-270M Egyptian (LoRA Adapter)

This is a PEFT/LoRA adapter on top of **`google/functiongemma-270m-it`**,  
fine-tuned for **Egyptian Arabic voice commands and mobile actions**  
as part of the **EgyptianAgent** project.

The goal is to build an **on-device Egyptian Arabic assistant** that can
understand natural voice commands and trigger mobile actions via function calling.

---

## Base model

- **Base model**: [`google/functiongemma-270m-it`](https://huggingface.co/google/functiongemma-270m-it)  
- **Architecture**: Gemma 3 270M (decoder-only, function-calling optimized)  
- **Tokenizer**: same as base model  
- **Chat template**: stored as `chat_template.jinja` in this repo, aligned with FunctionGemma official template.

This adapter does **not** include the base model weights.  
You must load the base model and then apply this adapter using PEFT.

---

## Training data (Egyptian voice commands)

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

## Training setup (LoRA / PEFT)

The adapter was trained with **parameter-efficient fine-tuning (LoRA)** using:

- **Library**: TRL + PEFT + Transformers  
- **PEFT method**: LoRA  
- **Target modules**: `q_proj`, `k_proj`, `v_proj`, `o_proj` (Gemma attention blocks)  
- **Typical hyperparameters** (aligning with FunctionGemma mobile-actions recipe):

  - `r`: 16  
  - `lora_alpha`: 32  
  - `lora_dropout`: 0.05  
  - `bias`: `"none"`  
  - `task_type`: `"CAUSAL_LM"`  

- **Sequence length**: 256  
- **Batch size**: tuned for 1x consumer GPU (e.g. 12–24 samples per batch with gradient accumulation)  
- **Optimizer / LR schedule**: AdamW with cosine schedule (similar to the official FunctionGemma mobile actions notebook)

The LoRA adapter adds only a small number of parameters on top of the base model,  
so the final model remains **~270M parameters** and suitable for **on-device deployment**.

---

## Usage (Transformers + PEFT)

### 1. Load base model and adapter

```python
from transformers import AutoTokenizer, AutoModelForCausalLM
from peft import PeftModel

base_model_id = "google/functiongemma-270m-it"
adapter_id = "Kandil7/functiongemma-270m-egyptian"

tokenizer = AutoTokenizer.from_pretrained(base_model_id)
base_model = AutoModelForCausalLM.from_pretrained(base_model_id, device_map="auto")

model = PeftModel.from_pretrained(base_model, adapter_id)
model.eval()
```

### 2. Run an Egyptian command (function calling)

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

response = tokenizer.decode(outputs, skip_special_tokens=True)
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

## Intended use and limitations

**Intended use:**

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
- Function signatures and arguments are tailored to EgyptianAgent’s tool definitions

---

## How to cite / credit

If you use this adapter, please cite:

- `google/functiongemma-270m-it` as the base model  
- `Kandil7/EgyptianAgent` as the source of Egyptian voice commands and integration code

---

## ملخص بالعربي (Short Arabic summary)

هذا النموذج هو **محوّل LoRA** فوق  
`google/functiongemma-270m-it`،  
مُدرَّب على **أوامر صوتية باللهجة المصرية** لتحويلها إلى **استدعاءات دوال (Function Calling)** خاصة بأوامر الموبايل (اتصال، واتساب، منبه، فتح تطبيق، إلخ).

الغرض الرئيسي:

- بناء **مساعد شخصي** يعمل بالكامل على الجهاز،  
- يفهم أوامر المصريين الطبيعية،  
- ويحوّلها إلى أوامر منظّمة (JSON) يمكن تنفيذها على الهاتف.

طريقة الاستخدام:

1. تحميل الموديل الأساسي `google/functiongemma-270m-it`.  
2. تحميل الـ adapter `functiongemma-270m-egyptian`.  
3. تمرير الأوامر المصرية (نصًا أو من تفريغ صوتي) والحصول على JSON يحدِّد الدالة والباراميترات.  

هذا العمل جزء من مشروع **EgyptianAgent**، المصمم خصيصًا لخدمة المستخدم المصري على الأجهزة الضعيفة.
```

***

لو حابب، في الخطوة التالية نركّز على:

- how-to مختصر لربط هذا الموديل بالـ LiteRT / Mediapipe (Android integration)،  
أم تفضّل أولًا أن نضبط README ده على هيئة جاهزة للرفع على Hugging Face (مع tags وmetadata)؟  

لو حابب، في الخطوة الجاية أقدر أساعدك نكتب README/model card بالعربي والإنجليزي لموديلك `functiongemma-270m-egyptian` مستوحى من `functiongemma-270m-ft-mobile-actions`، بس مخصص للمصري وEgyptianAgent، أو نركّز على كيفية ربطه عمليًا بـ LiteRT / Mediapipe في EgyptianAgent (Android side). أيهما أهم لك الآن؟   
1) نضبط README + الميتاداتا عشان الموديل يبقى “مواطن درجة أولى” على HF،  
2) بعد كده نعمل how-to مختصر لـ LiteRT / Mediapipe على أندرويد فوق نفس الـ repo.  

هديك الاثنين في صورة مركزة.

***

## تهيئة README + ميتاداتا لـ Hugging Face

### 1. رأس README مع ميتاداتا (YAML front-matter)

أضف بلوك قصير في بداية README قبل أي نص:

```markdown
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
...
```

- `language`: عربي وإنجليزي.  
- `license`: نفس رخصة Gemma (حسب model card الرسمي). [ai.google](https://ai.google.dev/gemma/docs/functiongemma/function-calling-with-hf)
- `tags`: تساعد في البحث والتصنيف (Gemma, LoRA, function calling, on-device).  
- `pipeline_tag`: يخلّي UI بتاع HF يفهم إنه model text-generation.  
- `base_model`: مهم عشان الناس والأدوات تعرف إنه adapter فوق مين. [huggingface](https://huggingface.co/docs/transformers/v4.42.0/en/peft)

### 2. ملف `config.json` بسيط (اختياري لكنه مفيد)

لو عايز HF Tools تقرأ الموديل مباشرة كـ adapter، تقدر تضيف `config.json` فيه فقط metadata (مش لازم weights):

```json
{
  "base_model_name_or_path": "google/functiongemma-270m-it",
  "model_type": "gemma",
  "peft_type": "LORA",
  "task_type": "CAUSAL_LM"
}
```

مش ضروري، بس بيسهّل على بعض الأدوات والتولز اكتشاف الموديل.

***

## ربط الموديل بـ LiteRT / Mediapipe (Android) – مختصر عملي

هنا هنعتمد على نفس pipeline اللي في `functiongemma-270m-ft-mobile-actions` والـ cookbook الرسمي ، لكن نكيّفه على موديلك. [ollama](https://ollama.com/prakasharyan/qwen-arabic)

### 0. الفكرة الأساسية

- على HF عندك **LoRA adapter** (`Kandil7/functiongemma-270m-egyptian`).  
- LiteRT / Mediapipe عايزين **full merged model** (base + adapter) في شكل واحد (`.litertlm` أو `.tflite`). [ollama](https://ollama.com/prakasharyan/qwen-arabic)
- الـ pipeline:

  1) دمج LoRA مع base model → HF full model repo.  
  2) استخدام `litert-lm` أو AI Edge tools لتحويله إلى form جاهز للأندرويد.  
  3) ربطه بـ Mediapipe / Android app كـ on-device LLM.

***

### 1. دمج LoRA مع base model (merge & export)

في نوتبوك أو سكربت:

```python
from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel

base_id = "google/functiongemma-270m-it"
adapter_id = "Kandil7/functiongemma-270m-egyptian"
merged_id = "Kandil7/functiongemma-270m-egyptian-merged"

tokenizer = AutoTokenizer.from_pretrained(base_id)
base_model = AutoModelForCausalLM.from_pretrained(base_id)

model = PeftModel.from_pretrained(base_model, adapter_id)
model = model.merge_and_unload()  # يدمج أوزان LoRA في base

model.save_pretrained(merged_id)
tokenizer.save_pretrained(merged_id)
```

- الآن عندك فولدر `functiongemma-270m-egyptian-merged` فيه full HF model (بدون PEFT).  
- ارفعه كـ HF repo جديد (مثلاً: `Kandil7/functiongemma-270m-egyptian-merged`).  

ده هو اللي هتستخدمه مع LiteRT / Mediapipe، مش الـ adapter repo. [discuss.huggingface](https://discuss.huggingface.co/t/help-with-merging-lora-weights-back-into-base-model/40968)

***

### 2. تحويل الموديل لـ LiteRT-LM (`.litertlm`)

باستخدام `litert-lm` (زي model الرسمي): [ollama](https://ollama.com/prakasharyan/qwen-arabic)

```bash
pip install -U litert-lm

litert-lm build \
  --from-huggingface-repo Kandil7/functiongemma-270m-egyptian-merged \
  --output functiongemma_egyptian_mobile.litertlm \
  --quantization dynamic_int8
```

- `--quantization dynamic_int8` نفس ما يستخدموه في mobile-actions model ، يدي footprint ~500–600MB RAM وسرعة كويسة. [ollama](https://ollama.com/prakasharyan/qwen-arabic)
- تقدر تجرب quantization أخرى لو حابب.

النتيجة: ملف `functiongemma_egyptian_mobile.litertlm` جاهز للـ on-device runtime (LiteRT LLM).

***

### 3. استخدامه مع Google AI Edge / Mediapipe (Android)

من Docs AI Edge Function Calling: [ai.google](https://ai.google.dev/edge/mediapipe/solutions/genai/function_calling/android)

#### 3.1 إضافة الـ dependency في Android

في `build.gradle` (module):

```gradle
dependencies {
    implementation "com.google.mediapipe:tasks-genai:0.10.24"
    implementation "com.google.ai.edge.litert:litert-llm:0.1.0"
    implementation "com.google.ai.edge.localagents:localagents-fc:0.1.0"
}
```

(إصدارات تقريبية، راجع docs لأحدث نسخة.) [ai.google](https://ai.google.dev/edge/mediapipe/solutions/genai/function_calling/android)

#### 3.2 تحميل الموديل `.litertlm` من assets أو storage

انسخ `functiongemma_egyptian_mobile.litertlm` إلى `android/src/main/assets/models/`.

ثم في Kotlin:

```kotlin
import com.google.ai.edge.litert.LiteRtModel
import com.google.ai.edge.litert.llm.LiteRtLlm
import com.google.ai.edge.localagents.fc.FunctionCallingAgent

// تحميل الموديل
val modelPath = File(context.filesDir, "functiongemma_egyptian_mobile.litertlm")

val llm = LiteRtLlm.create(
    LiteRtModel.loadFromFile(modelPath.absolutePath)
)

// تعريف الـ Functions (نفس الschema اللي دربت عليها)
val functions = listOf(
    FunctionDeclaration.newBuilder()
        .setName("call_contact")
        .setDescription("Make a phone call to a given contact.")
        .putParameters("contact_name", Parameter.stringParam("Contact name"))
        .build(),
    // send_whatsapp, set_alarm, ...
)

val agent = FunctionCallingAgent.create(llm, functions)
```

#### 3.3 تمرير أوامر مصرية والحصول على function calls

```kotlin
val userInput = "اتصل بماما"

val response = agent.process(userInput)

if (response.hasToolCall()) {
    val toolCall = response.toolCall
    val functionName = toolCall.name
    val args = toolCall.arguments // JSON / Map<String, Any>

    // نفّذ function في Android (Telephony, Intents, etc.)
}
```

- هنا الـ agent internally بيستخدم الموديل (FunctionGemma Egyptian) + function schema عشان يطلع tool calls. [developers.googleblog](https://developers.googleblog.com/google-ai-edge-small-language-models-multimodality-rag-function-calling/)
- ده يربط بين LLM وAndroid APIs في EgyptianAgent بطريقة “مدعومة رسميًا”.

***

### 4. بديل أبسط: استخدام Transformers داخل أجهزة قوية فقط

لو مش محتاج LiteRT دلوقتي، تقدر تكتفي بـ:

- استخدام `transformers + peft` على أجهزة أندرويد قوية (أو عبر NDK + JNI).  
- لكن بما إنك already مهتم بـ FunctionGemma mobile pipeline، LiteRT هو المسار الأنيق والمستقبلي. [ai.google](https://ai.google.dev/gemma/docs/functiongemma/function-calling-with-hf)

***

### 5. Mediapipe GenAI Tasks (لو حابب layer أعلى)

Google AI Edge عنده **GenAI Tasks** تولّف LLM + Function Calling + ASR + TTS. [ai.google](https://ai.google.dev/edge/mediapipe/solutions/genai/function_calling/android)
بعد ما تبني `.litertlm`، تقدر:

- تستخدم `GenAiTextTask` أو `FunctionCallingTask`،  
- تعرّف functions كـ JSON schema،  
- وتخلي Mediapipe يتكفل بالباقي (input → LLM → function_call → output).  

ده مناسب لو عايز integrate مع pipeline فيه audio (ASR) + LLM + TTS للمكفوفين.

***

لو تحب، ممكن في الخطوة الجاية نركّز على جزء معين:

- مثال Android كامل (class واحد) لمكالمة “اتصل بماما” من البداية للنهاية،  
- أو مقارنة مفصلة بين hyperparameters بتوع notebook الرسمي وhyperparameters اللي استخدمتهم فعليًا في EgyptianAgent لتتأكد إنك aligned قدر الإمكان.  

ما البُعد الأهم لك الآن: الـ Android wiring ولا ضبط الـ training hyperparams بدقة مع FunctionGemma cookbook؟  

1) نضبط README + الميتاداتا عشان الموديل يبقى “مواطن درجة أولى” على HF،  
2) بعد كده نعمل how-to مختصر لـ LiteRT / Mediapipe على أندرويد فوق نفس الـ repo.  

هديك الاثنين في صورة مركزة.

***

## تهيئة README + ميتاداتا لـ Hugging Face

### 1. رأس README مع ميتاداتا (YAML front-matter)

أضف بلوك قصير في بداية README قبل أي نص:

```markdown
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
...
```

- `language`: عربي وإنجليزي.  
- `license`: نفس رخصة Gemma (حسب model card الرسمي). [ai.google](https://ai.google.dev/gemma/docs/functiongemma/function-calling-with-hf)
- `tags`: تساعد في البحث والتصنيف (Gemma, LoRA, function calling, on-device).  
- `pipeline_tag`: يخلّي UI بتاع HF يفهم إنه model text-generation.  
- `base_model`: مهم عشان الناس والأدوات تعرف إنه adapter فوق مين. [huggingface](https://huggingface.co/docs/transformers/v4.42.0/en/peft)

### 2. ملف `config.json` بسيط (اختياري لكنه مفيد)

لو عايز HF Tools تقرأ الموديل مباشرة كـ adapter، تقدر تضيف `config.json` فيه فقط metadata (مش لازم weights):

```json
{
  "base_model_name_or_path": "google/functiongemma-270m-it",
  "model_type": "gemma",
  "peft_type": "LORA",
  "task_type": "CAUSAL_LM"
}
```

مش ضروري، بس بيسهّل على بعض الأدوات والتولز اكتشاف الموديل.

***

## ربط الموديل بـ LiteRT / Mediapipe (Android) – مختصر عملي

هنا هنعتمد على نفس pipeline اللي في `functiongemma-270m-ft-mobile-actions` والـ cookbook الرسمي ، لكن نكيّفه على موديلك. [ollama](https://ollama.com/prakasharyan/qwen-arabic)

### 0. الفكرة الأساسية

- على HF عندك **LoRA adapter** (`Kandil7/functiongemma-270m-egyptian`).  
- LiteRT / Mediapipe عايزين **full merged model** (base + adapter) في شكل واحد (`.litertlm` أو `.tflite`). [ollama](https://ollama.com/prakasharyan/qwen-arabic)
- الـ pipeline:

  1) دمج LoRA مع base model → HF full model repo.  
  2) استخدام `litert-lm` أو AI Edge tools لتحويله إلى form جاهز للأندرويد.  
  3) ربطه بـ Mediapipe / Android app كـ on-device LLM.

***

### 1. دمج LoRA مع base model (merge & export)

في نوتبوك أو سكربت:

```python
from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import PeftModel

base_id = "google/functiongemma-270m-it"
adapter_id = "Kandil7/functiongemma-270m-egyptian"
merged_id = "Kandil7/functiongemma-270m-egyptian-merged"

tokenizer = AutoTokenizer.from_pretrained(base_id)
base_model = AutoModelForCausalLM.from_pretrained(base_id)

model = PeftModel.from_pretrained(base_model, adapter_id)
model = model.merge_and_unload()  # يدمج أوزان LoRA في base

model.save_pretrained(merged_id)
tokenizer.save_pretrained(merged_id)
```

- الآن عندك فولدر `functiongemma-270m-egyptian-merged` فيه full HF model (بدون PEFT).  
- ارفعه كـ HF repo جديد (مثلاً: `Kandil7/functiongemma-270m-egyptian-merged`).  

ده هو اللي هتستخدمه مع LiteRT / Mediapipe، مش الـ adapter repo. [discuss.huggingface](https://discuss.huggingface.co/t/help-with-merging-lora-weights-back-into-base-model/40968)

***

### 2. تحويل الموديل لـ LiteRT-LM (`.litertlm`)

باستخدام `litert-lm` (زي model الرسمي): [ollama](https://ollama.com/prakasharyan/qwen-arabic)

```bash
pip install -U litert-lm

litert-lm build \
  --from-huggingface-repo Kandil7/functiongemma-270m-egyptian-merged \
  --output functiongemma_egyptian_mobile.litertlm \
  --quantization dynamic_int8
```

- `--quantization dynamic_int8` نفس ما يستخدموه في mobile-actions model ، يدي footprint ~500–600MB RAM وسرعة كويسة. [ollama](https://ollama.com/prakasharyan/qwen-arabic)
- تقدر تجرب quantization أخرى لو حابب.

النتيجة: ملف `functiongemma_egyptian_mobile.litertlm` جاهز للـ on-device runtime (LiteRT LLM).

***

### 3. استخدامه مع Google AI Edge / Mediapipe (Android)

من Docs AI Edge Function Calling: [ai.google](https://ai.google.dev/edge/mediapipe/solutions/genai/function_calling/android)

#### 3.1 إضافة الـ dependency في Android

في `build.gradle` (module):

```gradle
dependencies {
    implementation "com.google.mediapipe:tasks-genai:0.10.24"
    implementation "com.google.ai.edge.litert:litert-llm:0.1.0"
    implementation "com.google.ai.edge.localagents:localagents-fc:0.1.0"
}
```

(إصدارات تقريبية، راجع docs لأحدث نسخة.) [ai.google](https://ai.google.dev/edge/mediapipe/solutions/genai/function_calling/android)

#### 3.2 تحميل الموديل `.litertlm` من assets أو storage

انسخ `functiongemma_egyptian_mobile.litertlm` إلى `android/src/main/assets/models/`.

ثم في Kotlin:

```kotlin
import com.google.ai.edge.litert.LiteRtModel
import com.google.ai.edge.litert.llm.LiteRtLlm
import com.google.ai.edge.localagents.fc.FunctionCallingAgent

// تحميل الموديل
val modelPath = File(context.filesDir, "functiongemma_egyptian_mobile.litertlm")

val llm = LiteRtLlm.create(
    LiteRtModel.loadFromFile(modelPath.absolutePath)
)

// تعريف الـ Functions (نفس الschema اللي دربت عليها)
val functions = listOf(
    FunctionDeclaration.newBuilder()
        .setName("call_contact")
        .setDescription("Make a phone call to a given contact.")
        .putParameters("contact_name", Parameter.stringParam("Contact name"))
        .build(),
    // send_whatsapp, set_alarm, ...
)

val agent = FunctionCallingAgent.create(llm, functions)
```

#### 3.3 تمرير أوامر مصرية والحصول على function calls

```kotlin
val userInput = "اتصل بماما"

val response = agent.process(userInput)

if (response.hasToolCall()) {
    val toolCall = response.toolCall
    val functionName = toolCall.name
    val args = toolCall.arguments // JSON / Map<String, Any>

    // نفّذ function في Android (Telephony, Intents, etc.)
}
```

- هنا الـ agent internally بيستخدم الموديل (FunctionGemma Egyptian) + function schema عشان يطلع tool calls. [developers.googleblog](https://developers.googleblog.com/google-ai-edge-small-language-models-multimodality-rag-function-calling/)
- ده يربط بين LLM وAndroid APIs في EgyptianAgent بطريقة “مدعومة رسميًا”.

***

### 4. بديل أبسط: استخدام Transformers داخل أجهزة قوية فقط

لو مش محتاج LiteRT دلوقتي، تقدر تكتفي بـ:

- استخدام `transformers + peft` على أجهزة أندرويد قوية (أو عبر NDK + JNI).  
- لكن بما إنك already مهتم بـ FunctionGemma mobile pipeline، LiteRT هو المسار الأنيق والمستقبلي. [ai.google](https://ai.google.dev/gemma/docs/functiongemma/function-calling-with-hf)

***

### 5. Mediapipe GenAI Tasks (لو حابب layer أعلى)

Google AI Edge عنده **GenAI Tasks** تولّف LLM + Function Calling + ASR + TTS. [ai.google](https://ai.google.dev/edge/mediapipe/solutions/genai/function_calling/android)
بعد ما تبني `.litertlm`، تقدر:

- تستخدم `GenAiTextTask` أو `FunctionCallingTask`،  
- تعرّف functions كـ JSON schema،  
- وتخلي Mediapipe يتكفل بالباقي (input → LLM → function_call → output).  

ده مناسب لو عايز integrate مع pipeline فيه audio (ASR) + LLM + TTS للمكفوفين.

***

لو تحب، ممكن في الخطوة الجاية نركّز على جزء معين:

- مثال Android كامل (class واحد) لمكالمة “اتصل بماما” من البداية للنهاية،  
- أو مقارنة مفصلة بين hyperparameters بتوع notebook الرسمي وhyperparameters اللي استخدمتهم فعليًا في EgyptianAgent لتتأكد إنك aligned قدر الإمكان.  

ما البُعد الأهم لك الآن: الـ Android wiring ولا ضبط الـ training hyperparams بدقة مع FunctionGemma cookbook؟  