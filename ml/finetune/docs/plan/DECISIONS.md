# DECISIONS (ADR) – EgyptianAgent

## ADR-001: Local-only processing

- Decision: كل المعالجة (ASR, NLU, Intent Engine, UI Agent) تتم على الجهاز فقط.
- Rationale: الخصوصية، الاعتمادية، دعم مستخدمين بدون إنترنت.

## ADR-002: استخدام FunctionGemma-270M كنموذج intent engine

- Decision: اختيار FunctionGemma-270M-IT كنموذج intent classification الأساسي.
- Rationale: حجم صغير (حوالي 288MB)، latency منخفض (~350ms)، RAM ~550MB، مع accuracy ~95%.

## ADR-003: Hybrid Fast/Slow Path

- Decision: تقسيم التنفيذ إلى Fast Path (intent-based) وSlow Path (UI navigation).
- Rationale: Fast Path لأوامر بسيطة، Slow Path لمهام معقدة داخل UI.

## ADR-004: استخدام EgyptianWhisper ASR

- Decision: استخدام نموذج ASR مخصص للهجة المصرية.
- Rationale: تحسين جودة التعرف على الكلام باللهجة، دعم أوامر طبيعية.

## ADR-005: Target device – Honor X6c

- Decision: الاستهداف الأساسي لجهاز Honor X6c (أو مواصفات مشابهة).
- Rationale: جهاز متاح في السوق المصري، 6GB RAM، مناسب للاختبارات.

## ADR-006: Root + privileged app

- Decision: تشغيل بعض الخصائص في مستوى privileged app مع root/Magisk.
- Rationale: الحصول على صلاحيات أعمق للتكامل مع النظام (screen-off operation، إلخ).

## ADR-007: Senior mode support

- Decision: دعم وضع خاص لكبار السن.
- Rationale: الجمهور الأساسي يحتاج UX أكثر بساطة وصوت أعلى وأوضح.
