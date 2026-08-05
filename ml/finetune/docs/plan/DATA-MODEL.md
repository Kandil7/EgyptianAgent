# Data Model – EgyptianAgent

> ملاحظة: الريبو الأصلي يحتوي على evaluation_results وdatasets، لكن هنا نضع تصورًا منظمًا لطبقة البيانات المستمرة (إن طُبّقت داخل التطبيق).

## 1. Core entities (تصور)

### CommandEvent

- id
- timestamp
- raw_transcript
- normalized_text
- asr_confidence
- intent
- slots_json
- path_used (fast/slow)
- success (bool)
- error_code (إن وجد)

### ExecutionLog

- id
- command_event_id
- action_type
- tool_name
- status (success/fail)
- latency_ms

### EmergencyEvent

- id
- command_event_id
- contact_called
- result
- timestamp

### ModelConfig

- id
- model_name (FunctionGemma-270M, Whisper-Egyptian, إلخ)
- version
- path
- checksum
- enabled

### UserProfile (اختياري)

- id
- mode (standard/senior)
- preferred_voice
- emergency_contacts

## 2. Datasets

- أوامر CALL_CONTACT (مئات الأمثلة).
- أوامر SEND_WHATSAPP.
- أوامر SET_ALARM.
- أوامر OPEN_APP.
- أوامر DEVICE_CONTROL.
- أوامر EMERGENCY.

كلها تُستخدم في تقييم accuracy، وبعضها لتدريب intent engine.
