# Product Specification – EgyptianAgent

## 1. تعريف المنتج

EgyptianAgent هو مساعد صوتي يعمل على Android، يستمع لأوامر باللهجة المصرية، يحوّلها إلى نص، يصنّفها إلى intents، ثم ينفّذها عبر Fast Path أو Slow Path:

- Fast Path: أوامر مباشرة (Call, WhatsApp, Alarm, Device Control، إلخ).
- Slow Path: مهام UI متعددة الخطوات باستخدام Accessibility + Workflows.

## 2. حالات الاستخدام الأساسية

### 2.1 أوامر Fast Path

- "اتصل بماما" → CALL_CONTACT
- "ابعت واتساب لـ أحمد" → SEND_WHATSAPP
- "نبهني بكرة الصبح" → SET_ALARM
- "افتح الواتساب" → OPEN_APP
- "قفل الواي فاي" → DEVICE_CONTROL

### 2.2 أوامر Slow Path

- "افتح الفيسبوك وشوف الأخبار"
- "احجز أوبر من البيت للشغل"
- "دور على فيديو كوكب في اليوتيوب"
- "اقرا آخر رسالة واتساب جت"

هذه الأوامر تتطلب فتح تطبيقات، قراءة UI، اتخاذ قرارات خطوة بخطوة.

## 3. سلوك المنتج

### 3.1 Activation

- wake words: "يا صاحبي" (standard)، "يا كبير" (senior mode).
- استيقاظ من خلفية النظام أو الشاشة المغلقة.

### 3.2 Fast Path behavior

- إذا كان intent واضحًا وثقة التصنيف > 0.85 → يذهب إلى Fast Path.
- تنفيذ مباشر عبر intent engine → action layer.
- Latency ~350ms داخل الجهاز (حسب القياسات).

### 3.3 Slow Path behavior

- إذا كان intent غير واضح أو المهمة معقدة → Slow Path.
- يتم تحليل شجرة الـ Accessibility.
- يُنفّذ تسلسل actions (tap, type, scroll, إلخ) بناء على خطة LLM.
- Latency بين 2–5 ثوانٍ في المتوسط.

### 3.4 Senior Mode

- صوت TTS أعلى وأبطأ.
- feedback أوضح (تكرار، تأكيدات إضافية).
- wake word خاص: "يا كبير".

### 3.5 Error Handling

- في Fast Path: feedback صوتي إذا فشل (مثلاً contact غير معروف).
- في Slow Path: retries محدودة، fallback إلى user prompt.

## 4. سيناريوهات تفصيلية

### 4.1 مكالمة طوارئ

- المستخدم يقول: "يا نجدة".
- يتم التعرف على intent: EMERGENCY_CALL.
- Fast Path ينفّذ: الاتصال بcontact معين، أو trigger لخط طوارئ.

### 4.2 Morning Routine Workflow

- المستخدم يقول: "روتين الصباح".
- Slow Path workflow:
  - فتح تطبيق الطقس → قراءة الطقس.
  - فتح تطبيق الأخبار → قراءة عناوين مختصرة.
  - إرسال رسالة واتساب للعائلة.

## 5. UX behavior

- TTS مصري للردود.
- prompts واضحة لكل خطوة.
- دعم feedback صوتي مستمر، لأن المستخدم قد يكون لا يرى الشاشة.

## 6. Modes

- Standard Mode: للمستخدمين العاديين.
- Senior Mode: لكبار السن، مع tempo أبطأ وصوت أعلى.

## 7. Acceptance Criteria

- تنفيذ 95% من أوامر Fast Path بنجاح في بيئة الاختبار.
- تنفيذ >85% من مهام Slow Path الشائعة.
- زمن استجابة Fast Path أقل من 2s end-to-end.
- نجاح wake word detection مع معدلات false positives/negatives مقبولة.
