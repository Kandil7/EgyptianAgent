# EgyptianAgent Full Project Overview

### Context

Repo: K:\projects\ai-ml\EgyptianAgent (git). Goal: build a full high-level map of the project — an offline Android voice assistant for Egyptian-Arabic seniors with a hybrid AI architecture (FunctionGemma intent classification + accessibility-tree UI navigation), plus a Python LoRA fine-tuning pipeline. Session goal: understand the entire project in detail, verify actual repo layout vs. stale docs, and produce a navigation map for deeper learning sessions.

### Explanation

## What it is
EgyptianAgent is a 100%-offline Android voice assistant for Egyptian-Arabic seniors and visually impaired users. It listens for wake words ("يا صاحبي" / "يا كبير"), transcribes speech on-device, classifies the intent with a fine-tuned FunctionGemma-270M model, and either executes the action directly (fast path) or navigates other apps' UIs via the accessibility tree (slow path). It requires root/Magisk (manifest uses sharedUserId="android.uid.system").

## Actual repo layout (verified, after a big reorganization app/→android/, scripts/→deploy/, datasets/→ml/finetune/data/)
- android/ — the Android app module (~190 Java/Kotlin files, 35+ packages)
  - src/main/java/com/egyptian/agent/{ui,core,ai,llm,nlu,nlp,asr,stt,executor,executors,hybrid,navigation,workflow,accessibility,emergency,contacts,receivers,service,security,system,updates,analytics,feedback,performance,backup,utils,wakeword,vision,test}
  - src/main/cpp/ — CMakeLists.txt + functiongemma_jni.cpp (33KB), llama_native.cpp, whisper_native.cpp
  - src/main/assets/workflows/ — 10 YAML workflows
  - src/test/ — 36 unit test files (JUnit5 + JUnit4 vintage + Robolectric + Mockito), src/androidTest/ — 3 files
- ml/finetune/ — Python ML pipeline: data/ (voice_commands train=665/eval=50/test=102, ui_navigation train=50/test=20), configs/finetune_config.yaml, checkpoints/ (LoRA adapters + checkpoint-2338/4175), scripts/ (finetune, evaluate, gguf convert, FastAPI api_server.py, Streamlit app.py), evaluation/ (87.25% accuracy report)
- ml/prompts/ + ml/requirements.txt — prompting artifacts and Python deps
- deploy/ — build/deploy scripts (gradlew wrapper, build.bat, install scripts, HF upload scripts, verify_deployment.ps1)
- docs/ — architecture, deployment, API, guides, testing, performance docs + archive/ of old reports (BUILD_STATUS_REPORT.md etc.)
- agents/ — 19 persona agent .md files (Product_Manager, Technical_Lead, ML_Engineer_*, etc.) + OPENCODE_INTEGRATION.md
- .claude/skills/ — 17 egyptian-* skill definitions
- external/ — submodules: llama.cpp, whisper.cpp, faster-whisper (shallow)
- tools/adb — adb helper tools

## Build facts (verified from build.gradle)
- AGP 8.13.2, Kotlin 2.1.0, Java 1.8 target, minSdk 28, targetSdk 34, compileSdk 35, NDK 27.0.12077973 (AGENTS.md says 25.2 required — mismatch), versionName 1.1.0 / versionCode 10100 (docs claim 2.0.0 — stale)
- Native: USE_LLAMA_CPP/USE_WHISPER default OFF → mock JNI stubs; pass -PnativeInference=full to build real llama.cpp/whisper.cpp; ALLOW_MOCK_NATIVE gates the stubs
- java.exclude source-set quirk documented in AGENTS.md — many packages excluded from compilation (this appears fixed in the current android/build.gradle: "no source excludes" comment)
- Debug signing via auto-generated keystore; release signing via env vars; deployAsSystemApp gradle task

## Runtime architecture (hybrid AI)
1. Wake word detection ("يا صاحبي", "يا كبير") — wakeword/ package
2. ASR: EgyptianWhisper / Vosk / Whisper engines (asr/, stt/, ai/EgyptianWhisperASR)
3. Normalization: EgyptianNormalizer (nlu/, stt/)
4. HybridOrchestrator.kt: FunctionGemma classifyIntent → routing:
   - confidence ≥ 0.85 → FAST PATH: direct intent execution (CALL_CONTACT, SEND_WHATSAPP, SET_ALARM, OPEN_APP, device toggles, etc.)
   - confidence < 0.70 or UI-required → SLOW PATH: UINavigationEngine with 28 UI actions (tap/type/swipe/scroll/launch/...) via AccessibilityTreeParser, max 10 iterations, stuck detection after 3
   - 10 pre-built YAML workflows (morning_routine, book_uber, check_social, ...) parsed by WorkflowEngine.kt (SnakeYAML)
5. TTS response in Egyptian Arabic (Piper/TTSManager)
- Manifest components: MainActivity, VoiceService, FallDetectionService, EgyptianAgentService/SessionService, ModelLoadingService, SelfAwareService (com.saiy), BootReceiver, VolumeButtonReceiver (triple-click emergency), MedicationReceiver, AdminReceiver, ShizukuProvider

## ML pipeline
- LoRA fine-tune google/functiongemma-270m-it: r=16, alpha=32, dropout 0.05, target q/k/v/o_proj, 5 epochs, lr 2e-4, cosine, fp16, max_seq 512, eval per epoch, save_total_limit 2
- Data format: function-calling "messages" JSONL (system/user/assistant with {"function": ..., "arguments": {...}})
- Measured evaluation: 87.25% overall accuracy (call_contact 100%, device_control 61%, read_time 60%), mean latency 721ms — README's 95.2%/350ms figures are aspirational/outdated
- GGUF conversion scripts for on-device deployment; app expects /data/data/com.egyptian.agent/models/functiongemma-270m-it.gguf

## Reality-check summary (docs vs. code)
- README/INDEX: "Production Ready 2.0.0, 95.2% accuracy, Build Passing" — aspirational; build.gradle is 1.1.0, measured accuracy is 87.25%, no APK has ever been built, CI workflows empty
- Duplicate/mirrored packages (executor/ vs executors/, asr/ vs stt/, nlu/ vs nlp/, test/ vs testing/) with API drift — multiple IntentType/IntentResult definitions
- Docs describe scripts/app/datasets/configs paths that no longer exist post-reorganization

### Alternatives

1. Cloud-based assistant (Alexa/Google Assistant): far better NLU, but violates the 100% privacy/offline requirement and costs per query. Rejected by product spec.
2. Single larger on-device LLM (Llama 3.2 3B): better accuracy (97.8% per README) but 7x bigger, 7.5x more RAM, 4.7x slower — infeasible on Honor X6c 6GB RAM. Used only as optional fallback (llm/ package, llama_config_honor_x6c.yaml).
3. Pure rule-based NLU (RuleBasedClassifier, TFLiteIntentClassifier): fast and cheap, but poor dialect coverage; kept as legacy/fallback inside nlu/.
4. Pure UI-navigation agent (DroidClaw-style only): handles complex tasks but slow (2-5s) and fragile; kept as slow path only.
5. Vosk vs Whisper for ASR: Vosk (0.3.47) is lighter, Whisper more accurate — both implemented (asr/VoskASREngine, WhisperASREngine).
Rationale for hybrid: routing by confidence threshold gives ~350ms for common commands while keeping complex multi-app tasks possible; progressive enhancement rather than replacement of the intent system.

### Rationale (Why this?)

The hybrid fast/slow split is the core design decision: on-device constraints (6GB RAM Honor X6c, offline, battery <3%/hr) rule out big models, so a 270M function-calling model handles the 80% of simple commands, and accessibility-tree automation covers the long tail of multi-step tasks that intent classification alone cannot do. LoRA (r=16) is chosen to fine-tune cheaply on a small dialect dataset (665 train samples) without catastrophic forgetting. System-app installation (sharedUserId=android.uid.system + root) is required to make calls/emergency features work hands-free without per-interaction permission prompts. Revisit conditions: if accuracy on device_control/read_time classes drops below ~80% target, expand dataset; if the accessibility slow path proves too fragile, restrict it to the 10 YAML workflows; if NDK 25 vs 27 mismatch blocks native builds, align the installed NDK.

### Exercises

1. Trace the command pipeline end-to-end: read VoiceService.java → HybridOrchestrator.kt → FunctionGemmaIntentEngine → an executor (e.g., CallExecutor) and draw a sequence diagram.
2. Compare the two IntentType definitions (nlp/IntentType.java vs nlu/IntentType.java vs core/IntentType.java) — document which classes reference which, and why the API drifted.
3. Run the fine-tune evaluation: python ml/finetune/scripts/evaluate_egyptian_accuracy.py against the 102 test samples; check per-class accuracy vs. the committed evaluation_report.json.
4. Pick one YAML workflow (e.g., book_uber.yaml) and walk through WorkflowEngine.kt parsing → UINavigationEngine execution; add a small test for a new workflow.
5. Read docs/archive/reports/BUILD_STATUS_REPORT.md, then attempt gradlew :android:compileDebugJavaWithJavac and compare actual errors vs. the historical list.

### Next Steps

Suggested learning order: 1) code-walkthrough of HybridOrchestrator.kt (the heart), 2) code-walkthrough of VoiceService.java, 3) design-alternatives for the routing thresholds, 4) deep dive into ml/finetune scripts (finetune_functiongemma_egyptian.py), 5) explain-by-diff on the recent reorganization commit to understand the repo history. Also verify: dataset Arabic encoding correctness (train.jsonl sample displayed as mojibake under PowerShell — likely a PS 5.1 Get-Content encoding artifact, verify with UTF-8 read).

---
