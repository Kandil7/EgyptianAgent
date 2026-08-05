# Technical Specification – EgyptianAgent

## 1. Overview

EgyptianAgent يعتمد على معمارية هجينة Hybrid AI تجمع ما بين:

- FunctionGemma-270M-IT كنموذج intent classification صغير وسريع.
- EgyptianWhisper ASR لتحويل الصوت إلى نص.
- LLM/logic لتنفيذ UI navigation (Slow Path) عبر شجرة الـ Accessibility.

## 2. Tech stack

- Android (Kotlin/Java) للتطبيق الأساسي.
- Android Accessibility APIs لقراءة/التحكم في واجهة التطبيقات.
- FunctionGemma-270M-IT (Google Gemma-based) كموديل intent engine.
- Whisper-based Egyptian ASR Engine.
- TTS engine لصوت مصري.
- Gradle/Android Studio لبناء المشروع.
- سكربتات Bash/PowerShell لإدارة التثبيت والتنصيب.

## 3. Fast Path pipeline

1. Wake word → يفتح الميكروفون.
2. ASR: تحويل الصوت المصري إلى نص.
3. Text normalization: توحيد اللهجة/النَص.
4. FunctionGemma intent engine: تصنيف intent + extraction لبعض المتغيرات.
5. Intent execution layer: تنفيذ action (call, WhatsApp, alarm, إلخ).

## 4. Slow Path pipeline

1. نفس الخطوات الأولى حتى الوصول للنص.
2. hybrid orchestrator يقرر أن المهمة تحتاج UI navigation.
3. UI Navigation Engine:
   - قراءة شجرة الـ Accessibility.
   - اتخاذ قرار: tap, type, swipe, scroll, إلخ.
   - تكرار حتى تحقيق الهدف.

## 5. Models

- FunctionGemma-270M-IT: intent engine.
- EgyptianWhisper ASR: speech-to-text.
- إمكان استخدام Llama 3.2 3B كـ fallback (فى بعض الإصدارات).

## 6. Performance targets (كما في README الأصلي)

- Model size: ~288MB لـ FunctionGemma.
- RAM usage: ~550MB.
- Cold load: ~4.8s.
- Warm load: ~1.2s.
- Inference latency: ~350ms متوسط.
- Battery: ~3%/hour إضافية.

## 7. Security & Privacy

- كل المعالجة محليًا.
- لا يتم إرسال بيانات صوت/نص لخوادم.
- تخزين مشفر للنماذج والـ configs.
- permissions مضبوطة (MIC, CALL, CONTACTS، إلخ) مع توثيق.
