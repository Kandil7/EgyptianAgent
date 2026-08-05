# AGENTS.md

Egyptian Agent: a 100%-offline Android voice assistant for Egyptian-Arabic seniors. Hybrid AI: "fast path" (FunctionGemma-270M intent classification) + "slow path" (accessibility-tree UI navigation). Repo = Android app (`app/`) + LoRA fine-tune pipeline (Python) + datasets + YAML UI workflows.

## Build environment (Windows, current machine)

- Gradle 8.13 wrapper, AGP 8.13.2, Kotlin 2.1.0. `gradle.properties` pins the Gradle JVM to `C:\Program Files\Android\Android Studio\jbr` — machine-specific; on other machines override with `-Dorg.gradle.java.home` or edit the file.
- **The build fails out of the box: "SDK location not found."** There is no `local.properties` and `ANDROID_HOME`/`ANDROID_SDK_ROOT` are unset. The SDK actually lives at `%LOCALAPPDATA%\Android\Sdk` (platforms 28–36, build-tools 34/35/36). Create `local.properties` with `sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk` (or set `ANDROID_HOME`) before any `gradlew` call. `build.bat` is stale (hardcodes `C:\Android\Sdk` which does not exist).
- NDK: project requires `25.2.9519653`; only NDK 27.0.12077973 is installed — native builds must auto-download NDK 25.x (needs license acceptance).
- **No APK has ever been produced in this repo** (`app/build/outputs/apk` does not exist). `BUILD_STATUS_REPORT.md` (2026-03-14) documents ~100 prior compile errors; the current state is unverified until you actually build. The "Build: Passing" badge in README is aspirational, not factual.
- After configuring the SDK, sanity-check with `gradlew.bat :app:compileDebugJavaWithJavac` before assuming anything compiles.

## Critical: `java.exclude` source-set quirk

`app/build.gradle` removes whole packages from Java compilation via `sourceSets.main.java.exclude` — **edits to these packages do NOT affect the app build**: `asr`, `stt`, `nlu`, `ai`, `llm`, `executor`, `executors`, `hybrid`, `receivers`, `vision`, `wakeword`, `backup`, `performance`, `feedback`, `test`, `testing`, `com/saiy/**`, plus specific files in `core/` (`VoiceService`, `WakeWordDetector`, `MainApplication`, ...), `accessibility/` (`FallDetectionService`, `MedicationScheduler`, `SeniorModeManager`, ...), `service/` (`EgyptianAgentSessionService`, ...), `utils/` and `nlp/IntentRouter.java`.

- The excludes are a compile-error mitigation, not an architecture decision. The manifest still references many excluded classes (`.core.VoiceService`, `.receivers.BootReceiver`, `.hybrid.ModelLoadingService`, `.service.EgyptianAgentSessionService`, ...) — packaging can still fail on these even after the SDK is fixed.
- `java.exclude` affects Java only: the Kotlin hybrid engine (`hybrid/*.kt`: `HybridOrchestrator.kt`, `AccessibilityTreeParser.kt`, `UIActions.kt`, `UINavigationEngine.kt`, `WorkflowEngine.kt`, `PromptTemplates.kt`) IS compiled.
- If your task touches excluded code, adjust the exclude list and verify with a real compile; don't assume the file is live.

## Architecture map

- Duplicate/mirrored packages exist from project history: `executor/` vs `executors/`, `asr/` vs `stt/`, `nlp/` vs `nlu/`, `test/` vs `testing/` — same components in multiple variants with API drift (e.g., multiple `IntentType` definitions). Before editing, find which variant is actually compiled.
- Entry points: `.ui.MainActivity` (launcher), `.ui.EgyptianAgentApplication` (Application class). Manifest uses `sharedUserId="android.uid.system"` — system-app/root installation model; `deployAsSystemApp` Gradle task exists.
- Compiled "fast path" surface (subject to excludes): `ui/`, `contacts/`, `emergency/`, `navigation/`, `security/`, `system/`, `updates/`, `workflow/`, `analytics/`, partial `core/` and `service/`.
- 10 YAML workflows live in `app/src/main/assets/workflows/` (morning_routine, book_uber, ...) and are parsed by the Kotlin `WorkflowEngine`.
- Native: `app/CMakeLists.txt` builds `llama_native` + `whisper_native` as **mock implementations by default** (`USE_LLAMA_CPP=OFF`, `USE_WHISPER=OFF`). Real integration requires the shallow submodules `external/llama.cpp` / `external/whisper.cpp` (init with `git submodule update --init`).

## Tests

- Unit tests are JUnit 5 (Jupiter) + JUnit 4 vintage + Robolectric + Mockito (`useJUnitPlatform()`), in `app/src/test/java` (29 files).
- Run: `gradlew.bat testDebugUnitTest` · single class: `gradlew.bat testDebugUnitTest --tests "com.egyptian.agent.ai.FunctionGemmaIntentEngineTest"`.
- Coverage: custom tasks `jacocoTestReport`, `jacocoCoverageVerification` (80% overall, 90% for `nlu.*`/`executor.*`) defined in `app/build.gradle`.
- `spotlessCheck` (in CONTRIBUTING.md) is stale — no spotless/ktlint/checkstyle plugin is configured anywhere.
- Fixtures: `app/src/test/resources/egyptian_test_commands.json`, `egyptian_dialect_test_corpus.json`. Tests assert on Arabic input — don't transliterate or "fix" them.
- Many unit tests target excluded packages (e.g., `ai.FunctionGemmaIntentEngineTest`, `nlu/*`) and will not compile until those classes are re-included. `androidTest` (3 files) needs a rooted device.

## ML / data pipeline (Python)

- Fine-tune: `pip install -r requirements_functiongemma.txt` then `python scripts/finetune/finetune_functiongemma_egyptian.py --config configs/finetune_config.yaml` (LoRA on `google/functiongemma-270m-it`; output in `models/functiongemma-270m-egyptian`).
- Datasets are committed: `datasets/egyptian_voice_commands/{train=665,eval=50,test=102}.jsonl` (function-calling `messages` format) and `datasets/egyptian_ui_navigation/{train=50,test=20}.jsonl`.
- LoRA adapters (`.safetensors`, checkpoints-2338/4175) ARE in git; base model weights and `.gguf` files are gitignored (download via `scripts/model/download_functiongemma_model.sh`, convert via `scripts/model/convert_to_gguf.sh`). The app expects the model at `/data/data/com.egyptian.agent/models/functiongemma-270m-it.gguf` (BuildConfig `MODEL_PATH`).

## Repo conventions & reality check

- All `scripts/**` are bash (.sh) on a Windows-only dev setup; PowerShell helpers exist: `scripts/deploy/verify_deployment.ps1`, `scripts/utils/init_gradle_wrapper.ps1`, `scripts/setup/windows_setup.ps1`. `.github/workflows/` is empty — there is no CI.
- Arabic (Egyptian dialect) is first-class: `resConfigs` are only `ar`/`en`, RTL enabled. Preserve Arabic strings/commands in code, tests, and datasets.
- Version drift: docs say v2.0.0, but `build.gradle` version is 1.1.0 (`versionCode 10100`). Bump the Gradle `ext` values, not the docs.
- Persona/instruction sources: `.claude/skills/` (17 `egyptian-*` skills loadable via the skill tool), `agents/*.md` (original persona prompts), and `agents/OPENCODE_INTEGRATION.md` maps them to OpenCode subagent types.
- README.md / INDEX.md / PROJECT_STRUCTURE.md describe an idealized state; trust build.gradle, the manifest, and this file when they conflict.
