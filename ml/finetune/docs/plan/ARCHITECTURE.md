# Architecture – EgyptianAgent

## 1. High-level architecture

- User voice input → ASR → Normalization → Orchestrator → Fast/Slow Path → Execution → TTS.
- Fast Path: direct intent execution.
- Slow Path: UI navigation via accessibility tree.

## 2. Modules (كما في الريبو)

- `app/`: تطبيق Android الأساسي (Activities, Services, UI).
- `agents/`: logic الخاصة بـ Hybrid Orchestrator، Fast Path، Slow Path.
- `datasets/`: بيانات training/eval للأوامر.
- `models/`: النماذج المخزنة محليًا (FunctionGemma, ASR, TTS).
- `tools/adb/`: سكربتات وأدوات للتحكم في الجهاز عبر ADB.
- `configs/`: إعدادات النظام/النماذج.
- `docs/`: التوثيق.

## 3. Internal APIs (على مستوى conceptual)

### Orchestrator API

- Input: normalized text, ASR confidence, context.
- Output: routing decision (Fast/Slow), selected intent/plan.

### Fast Path Engine

- Input: normalized text.
- Output: intent + slots.
- Implementation: FunctionGemma intent classification.

### Slow Path Engine

- Input: goal description / command text.
- Output: sequence of UI actions (tap, type, scroll، إلخ).

### Execution Layer

- Call Execution
- WhatsApp Execution
- Alarm/Timer Execution
- Device Control Execution
- Emergency workflows

## 4. Data flow

1. AudioStream → ASR → Text.
2. Text → Normalizer.
3. NormalizedText → Orchestrator.
4. Orchestrator → Fast or Slow Path.
5. Fast Path → Intent Engine → Execution.
6. Slow Path → UI agent → Actions sequence → Result.
7. Result → TTS feedback.

## 5. Observability

- Logs للأوامر، intents، النتائج.
- تقييم accuracy per intent (CALL_CONTACT, SEND_WHATSAPP, إلخ).
- تقييم latency لكل path.
