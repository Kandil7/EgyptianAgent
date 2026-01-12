 # EgyptianAgent — GitHub Project Structure

## 📌 Project Name

**EgyptianAgent – MVP & Portfolio Execution**

## 📊 Project View

* **Board (Kanban)**
* Columns:

  1. Backlog
  2. Ready
  3. In Progress
  4. Review
  5. Done

---

# 🧱 Milestones (زمنية)

## Milestone 0 — Project Setup (Week 0)

**Goal:** Repo منضبط + Scope مقفول

### Issues

* [ ] Define MVP scope (lock features)
* [ ] Clean repo structure
* [ ] Remove non-MVP code
* [ ] Create `/docs` folder
* [ ] Add temporary README (vision only)

✅ Exit criteria:

> Repo نظيف + لا Features خارج النطاق

---

## Milestone 1 — Speech-to-Text (Week 1)

**Goal:** STT offline شغال ومستقر

### Issues

* [ ] Evaluate Whisper tiny vs base
* [ ] Integrate Whisper (JNI / wrapper)
* [ ] Implement `SpeechService`
* [ ] Add noise reduction
* [ ] Handle STT failure states
* [ ] Test Egyptian dialect phrases

✅ Exit criteria:

> كلام مصري بسيط → نص مفهوم 80%+

---

## Milestone 2 — Intent Engine (Week 2)

**Goal:** Intent آمن ومغلق

### Issues

* [ ] Define Intent enum
* [ ] Create Intent JSON contract
* [ ] Implement Intent parser
* [ ] Enforce JSON-only output
* [ ] Confidence threshold logic
* [ ] UNKNOWN intent handling

✅ Exit criteria:

> لا تنفيذ بدون Intent واضح + confidence ≥ 0.6

---

## Milestone 3 — Command Router (Week 3)

**Goal:** فصل الذكاء عن التنفيذ

### Issues

* [ ] Implement `CommandRouter`
* [ ] Inject executors
* [ ] Fallback logic
* [ ] Unit test routing logic
* [ ] Remove any direct execution from AI layer

✅ Exit criteria:

> Router نظيف + deterministic

---

## Milestone 4 — Android Executors (Week 4)

**Goal:** تنفيذ أوامر الهاتف بثبات

### Issues

* [ ] Implement `BaseExecutor`
* [ ] Implement `CallExecutor`
* [ ] Implement `WhatsAppExecutor`
* [ ] Implement `AlarmExecutor`
* [ ] Implement `EmergencyExecutor`
* [ ] ContactUtils (lookup)
* [ ] AlarmUtils (time parsing)

✅ Exit criteria:

> 10 أوامر متتالية بدون Crash

---

## Milestone 5 — TTS & Senior Mode (Week 5)

**Goal:** تجربة إنسانية حقيقية

### Issues

* [ ] Implement `TextToSpeechService`
* [ ] Tune Arabic voice
* [ ] Add Senior Mode toggle
* [ ] Slow speech support
* [ ] Voice confirmation for every action

✅ Exit criteria:

> مستخدم كبير سنًا يفهم كل خطوة

---

## Milestone 6 — Error Handling & Safety (Week 5)

**Goal:** لا فشل صامت

### Issues

* [ ] Global error strategy
* [ ] STT error responses
* [ ] Intent clarification flow
* [ ] Executor failure responses
* [ ] Emergency confirmation logic

✅ Exit criteria:

> كل خطأ له رد صوتي واضح

---

## Milestone 7 — Privacy & Ethics (Week 6)

**Goal:** ثقة + مصداقية

### Issues

* [ ] Enforce offline-only mode
* [ ] Disable audio storage
* [ ] Kill mic after execution
* [ ] Wake-word only listening
* [ ] Write `SECURITY.md`

✅ Exit criteria:

> Privacy guarantees مطبقة ومكتوبة

---

## Milestone 8 — Documentation & Demo (Week 6)

**Goal:** المشروع "يتباع"

### Issues

* [ ] Rewrite README (story-based)
* [ ] Add ARCHITECTURE.md
* [ ] Create system diagram
* [ ] Record demo video (2–3 min)
* [ ] Add screenshots / GIFs

✅ Exit criteria:

> أي حد غير تقني يفهم المشروع

---

## Milestone 9 — Testing & Validation (Week 7)

**Goal:** مشروع موثوق

### Issues

* [ ] Dialect stress testing
* [ ] Elderly voice testing
* [ ] Noisy environment test
* [ ] Battery usage test
* [ ] Fix critical bugs

✅ Exit criteria:

> 90% success rate للأوامر الأساسية

---

## Milestone 10 — Release & Positioning (Week 8)

**Goal:** إطلاق MVP

### Issues

* [ ] Final cleanup
* [ ] Version tag v0.1.0
* [ ] Release notes
* [ ] Portfolio positioning text
* [ ] Grant / pitch draft (اختياري)

✅ Exit criteria:

> Project ready for public sharing

---

# 🏷 Labels (مهمة جدًا)

Labels to create:

* `core`
* `ai`
* `android`
* `accessibility`
* `privacy`
* `documentation`
* `critical`

---

# 🎯 How to work daily (practical advice)

* Work on **one Milestone only**
* No more than 2 Issues in In Progress
* Any new thing → Backlog
* Don't expand scope