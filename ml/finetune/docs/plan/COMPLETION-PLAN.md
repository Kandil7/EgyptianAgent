# Completion Plan – EgyptianAgent

> Derived from `docs/plan/*` (PRD, Product Spec, Tech Spec, Architecture, Data Model, ADRs, Roadmap)
> cross-checked against a **real build and test run** of the repo on 2026-08-04.
>
> This document supersedes the status claims in `BUILD_STATUS_REPORT.md`,
> `TEST_RESULTS.md`, `ACCURACY_TEST_RESULTS.md`, `PERFORMANCE_BENCHMARK_RESULTS.md`,
> and `CHECKPOINT.md`, all of which are **measurably inaccurate** (see §1).

---

## 0. TL;DR

The plan docs and `CHECKPOINT.md` describe a working v3 product. The repo does not
match that description. What actually exists is a **large, mostly-written codebase
that compiles only because ~60% of it is excluded from the build**, wired to
**mock native inference** and a **327-byte placeholder model file**.

Nothing here is a rewrite. The gap is integration, not authorship. Verified numbers:

| Reality check | Measured |
|---|---|
| `:app:assembleDebug` | ✅ **SUCCEEDS** (8m34s, 3 APKs) |
| Java compile errors with all sources enabled | **11 errors in 5 files** (not "100") |
| Unit test compile errors | **60 errors in 5 files** — tests have never run |
| Manifest components missing from the APK | **11 of 14** |
| `libllama_native.so` | 27 KB **mock** |
| LLM model asset | **327-byte text placeholder** |
| Trained model | LoRA adapter only (2.9 MB), no base weights, no GGUF |
| Real measured accuracy | **87.25%** (docs claim 92.4% / 95%) |
| Real measured latency | **721 ms mean** (docs claim 350 ms) |

**The critical insight:** this project is much closer to working than
`BUILD_STATUS_REPORT.md` claims (11 real errors, not 100), but much further from
working than `CHECKPOINT.md` claims (the shipped APK cannot start its own
services). Phase 1 below is ~2 days of work and unlocks everything else.

---

## 1. Verified current state

### 1.1 What I actually ran

```bash
# Created missing local.properties (was absent — build could not configure at all)
echo 'sdk.dir=C:/Users/Dell/AppData/Local/Android/Sdk' > local.properties

# Fixed ndkVersion 25.2.9519653 -> 27.0.12077973 (pinned version not installed)
# build.gradle:23

./gradlew :app:assembleDebug      # => BUILD SUCCESSFUL
./gradlew :app:testDebugUnitTest  # => FAILED, 60 Kotlin compile errors
```

Two blockers were fixed to get this far, and both are committed as part of this
work: the missing `local.properties` (gitignored, so every fresh clone hits it)
and the wrong `ndkVersion` pin.

### 1.2 The central problem: the build lies by exclusion

`app/build.gradle` contains **41 `java.exclude` directives** that remove most of
the product from compilation:

```groovy
java.exclude '**/hybrid/**'   // 15 files - the entire Hybrid Orchestrator
java.exclude '**/asr/**'      //  7 files - speech recognition
java.exclude '**/nlu/**'      //  8 files - intent classification
java.exclude '**/llm/**'      //  9 files - LLM integration
java.exclude '**/executor/**' //  8 files - action execution
java.exclude '**/executors/**'// 18 files
java.exclude '**/wakeword/**' //  6 files - wake word ("يا صاحبي")
java.exclude '**/stt/**'      //  5 files
java.exclude '**/receivers/**'//  5 files - boot, volume, admin
... plus 20 individually-excluded files in core/
```

`BUILD_STATUS_REPORT.md` frames this as "Option 1: Minimal Viable Build
(Recommended)". That option was taken, then documented as if the full product
shipped. The APK is real but hollow.

### 1.3 Proof the APK is hollow

Extracted the dex from `app-arm64-v8a-debug.apk` and scanned for every class the
manifest declares:

```
PRESENT : ui/EgyptianAgentApplication
PRESENT : ui/MainActivity
PRESENT : service/EgyptianAgentService
PRESENT : hybrid/HybridOrchestrator      <- Kotlin, survives (excludes are java.*)
PRESENT : navigation/UINavigationEngine
PRESENT : workflow/WorkflowEngine
MISSING : core/VoiceService               <- the main voice pipeline
MISSING : receivers/BootReceiver          <- so it cannot start on boot
MISSING : receivers/VolumeButtonReceiver  <- emergency triple-click dead
MISSING : receivers/AdminReceiver
MISSING : accessibility/FallDetectionService
MISSING : accessibility/MedicationReceiver
MISSING : executors/EmergencyFollowupReceiver
MISSING : hybrid/ModelLoadingService
MISSING : hybrid/IntegrationTestActivity
MISSING : hybrid/LlamaModelTesterActivity
MISSING : service/EgyptianAgentSessionService
```

Note the asymmetry: the `java.exclude` filters do **not** apply to Kotlin, so the
Kotlin half of the hybrid architecture compiles while the Java half it calls into
does not. **Any attempt to start these components at runtime throws
`ClassNotFoundException`.** The app installs, launches, and does essentially
nothing a user would recognize as the product.

### 1.4 The real error count is 11, not 100

I disabled all 41 excludes and compiled. Full list (javac reported `11 errors`,
no truncation cap hit):

| File | Line | Error |
|---|---|---|
| `wakeword/WakeWordManager.java` | 95 | `PorcupineWakeWordDetector` → `WakeWordDetectorInterface` |
| `wakeword/WakeWordManager.java` | 109 | `VoskWakeWordDetector` → `WakeWordDetectorInterface` |
| `wakeword/WakeWordManager.java` | 143 | `VoskWakeWordDetector` → `WakeWordDetectorInterface` |
| `service/EgyptianAgentSession.java` | 56 | does not override supertype method |
| `service/EgyptianAgentSession.java` | 58 | cannot find symbol |
| `service/EgyptianAgentSession.java` | 173 | cannot find symbol |
| `service/EgyptianAgentSession.java` | 176 | does not override supertype method |
| `service/EgyptianAgentSession.java` | 178 | cannot find symbol |
| `nlu/TFLiteIntentClassifier.java` | 112 | `AssetFileDescriptor` → `String` |
| `hybrid/EgyptianDialectAccuracyTester.java` | 149 | duplicate `onResult(IntentResult)` |
| `core/PerformanceTester.java` | 151 | `wakeDetector` may be uninitialized |

Every one is a local signature/type mismatch. **None** is a missing third-party
library. The report's claims of "PyTorch not in Maven", "PocketSphinx not
available", "RootBeer not in dependencies" are stale — PyTorch, TFLite, Vosk,
ONNX Runtime, ML Kit, Shizuku and libsu all resolve and ship in the APK today
(verified: `libpytorch_jni_lite.so`, `libvosk.so`, `libonnxruntime.so`,
`libtensorflowlite_jni.so` are all packaged).

### 1.5 Native inference is mocked

Submodules are declared but **empty on disk** (`external/llama.cpp` and
`external/whisper.cpp` contain nothing). CMake silently degrades:

```
CMake Warning at CMakeLists.txt:201 (message):
  whisper.cpp not found at .../external/whisper.cpp
CMake Warning at CMakeLists.txt:202 (message):
  Falling back to MOCK implementation
```

Result: `libllama_native.so` = 27 KB, `libwhisper_native.so` = 12 KB. Real
builds are 2–5 MB. `app/build.gradle` also hard-pins the mock path:
`"-DUSE_LLAMA_CPP=OFF", "-DUSE_WHISPER=OFF"`.

This silent fallback is the single most dangerous thing in the repo — it lets a
"successful" build produce a non-functional artifact with only a warning.

### 1.6 There is no deployable model

- `android/src/main/assets/model/llama-3.2-3b-Q4_K_M.gguf` is **327 bytes** of ASCII
  starting `# Llama` — a placeholder, not a model.
- `models/functiongemma-270m-egyptian/` has `adapter_model.safetensors`
  (2.9 MB LoRA adapter), tokenizer, and two checkpoints — but **no base model**
  and **no merged GGUF**.
- ADR-002 and the Tech Spec assume a ~288 MB FunctionGemma GGUF on device. It
  does not exist in any form.

The fine-tuning work is real and valuable; the export half was never done.

### 1.7 Tests have never executed

`grep '@Test'` counts **823 annotations** across 35 files, and the docs claim
"196 tests passing". But `:app:testDebugUnitTest` fails at **compilation** with
60 errors:

| Test file | Errors |
|---|---|
| `hybrid/HybridOrchestratorTest.kt` | 47 |
| `navigation/UIActionsTest.kt` | 8 |
| `accessibility/AccessibilityTreeParserTest.kt` | 3 |
| `workflow/WorkflowEngineTest.kt` | 1 |
| `navigation/UINavigationEngineTest.kt` | 1 |

The tests were written against an API that has since drifted. No test result in
any report is reproducible.

### 1.8 Metrics in docs contradict the repo's own data

`evaluation_results/evaluation_summary.txt` — the only real measurement present:

```
Overall Accuracy: 87.25%   (102 examples, 13 errors)
  device_control:  61.11%   <- worst
  read_time:       60.00%   <- worst
Latency mean:     721.66 ms
Latency p95:      973.35 ms
```

Versus what the docs assert:

| Metric | Docs claim | Repo's real data | Verdict |
|---|---|---|---|
| Accuracy | 92.4% / ~95% | **87.25%** | overstated |
| Fast Path latency | ~350 ms | **721 ms mean** | 2× overstated |
| Tests | 196 passing | **0 compile** | fabricated |
| RAM / cold load / battery | 550 MB / 4.8 s / 3%/hr | no source found | unsourced |

`device_control` at 61% matters most — PRD §6.6 lists WiFi/Bluetooth/Volume/
Brightness as core functional requirements.

### 1.9 Security issues to resolve before any release

- **`android:sharedUserId="android.uid.system"`** (AndroidManifest.xml:55) — this
  is deprecated and, more importantly, makes the app un-installable by normal
  means and permanently marries it to the platform signing key. ADR-006 wants
  privileged operation; `sharedUserId` is the wrong mechanism.
- **Debug keystore is committed** with hardcoded `storePassword 'android'`.
- **Release signing silently degrades**: `System.getenv("KEYSTORE_PASSWORD") ?: ""`
  produces an empty password rather than failing the build.
- `SECURITY.md` and the Tech Spec promise "encrypted storage for models and
  configs"; no encryption of model assets exists.

---

## 2. Strategy

Three sequenced principles:

1. **Make the build honest before making it bigger.** Every exclude removed and
   every mock replaced with a hard failure. A green build must mean a working
   artifact. This is Phase 1 and it is non-negotiable groundwork.
2. **Re-baseline the docs against measured reality.** Delete or correct every
   unverified number. Truthful 87.25% beats fictional 95%.
3. **Then** close functional gaps in PRD priority order, measuring as you go.

Rejected: `BUILD_STATUS_REPORT.md`'s "Option 3: create a new clean project".
With only 11 real compile errors, a rewrite would discard ~197 working source
files to solve a two-day problem.

---

## 3. Phase 1 — Restore build integrity (~2 days) 🔴 BLOCKING

**Goal:** all source compiles, no excludes, no mocks, APK contains every class
its manifest declares.

### 1.1 Fix the 11 compile errors
Work the table in §1.4. Suggested order (cheapest first):
- `PerformanceTester.java:151` — initialize `wakeDetector` (1 line).
- `EgyptianDialectAccuracyTester.java:149` — remove the duplicate
  `onResult(IntentResult)` override.
- `TFLiteIntentClassifier.java:112` — use
  `FileUtil.loadMappedFile(context, path)` or pass the descriptor's path string.
- `WakeWordManager.java` (3 errors) — make `PorcupineWakeWordDetector` and
  `VoskWakeWordDetector` actually `implements WakeWordDetectorInterface`. This is
  the root cause of all three; fix the classes, not the call sites.
- `EgyptianAgentSession.java` (5 errors) — reconcile against the
  `VoiceInteractionSession` API for `compileSdk 35`; method signatures moved.

### 1.2 Delete all 41 `java.exclude` lines
Remove them from `app/build.gradle` sourceSets. Keep only genuine non-source
exclusions (`**/test/**`, `**/com/saiy/**` if that vendored tree is truly dead —
decide explicitly rather than by exclusion).

### 1.3 Make native failure loud instead of silent
In `app/CMakeLists.txt`, replace the two `message(WARNING ... MOCK)` fallbacks
with `message(FATAL_ERROR)` gated on an explicit opt-in flag:

```cmake
if(NOT EXISTS ${WHISPER_CPP_DIR}/CMakeLists.txt)
  if(ALLOW_MOCK_NATIVE)
    message(WARNING "whisper.cpp absent — building MOCK (ALLOW_MOCK_NATIVE=ON)")
  else()
    message(FATAL_ERROR "whisper.cpp not found at ${WHISPER_CPP_DIR}. "
                        "Run: git submodule update --init --recursive")
  endif()
endif()
```

A developer must now *choose* a mock build. Release builds must never set it.

### 1.4 Initialize and build the native submodules
```bash
git submodule update --init --recursive   # currently empty
```
Then flip `-DUSE_LLAMA_CPP=ON -DUSE_WHISPER=ON` in `app/build.gradle`. Expect
real `.so` files of 2–5 MB. Budget time here for NDK 27 / `arm64-v8a` +
`armeabi-v7a` toolchain issues — this is the least predictable task in Phase 1.

### 1.5 Reconcile the manifest with reality
Re-run the dex verification from §1.3 and confirm all 14 components are
`PRESENT`. Add it as a CI gate (§7.2). Also decide the fate of
`com.saiy.android.service.SelfAwareService` — it is declared in the manifest but
belongs to a vendored third-party tree; either integrate or remove the
declaration.

### 1.6 Commit `local.properties` handling
Add `local.properties.template` and document `sdk.dir` in the README so a fresh
clone can configure. (`local.properties` itself stays gitignored — correct.)

**Exit criteria**
- `./gradlew :app:assembleDebug` succeeds with zero `java.exclude` and
  `ALLOW_MOCK_NATIVE=OFF`.
- `libllama_native.so` > 1 MB.
- Dex scan: 14/14 manifest classes present.

---

## 4. Phase 2 — Make the test suite real (~2 days)

**Goal:** the 823 `@Test` annotations actually run, and CI fails when they break.

- **2.1** Fix the 47 errors in `hybrid/HybridOrchestratorTest.kt`. They cluster
  around a drifted `HybridOrchestrator` constructor/result API — align the test
  to the current Kotlin source (or fix the source if the test encodes the
  intended contract; check `PromptTemplates.kt` and `HybridModule.kt` for intent).
- **2.2** Fix `UIActionsTest.kt` (8), `AccessibilityTreeParserTest.kt` (3),
  `WorkflowEngineTest.kt:352` (`Unresolved reference 'exists'`),
  `UINavigationEngineTest.kt:51` (`No parameter named 'description'`).
- **2.3** Resolve the JUnit 4 / JUnit 5 split. `build.gradle` enables
  `useJUnitPlatform()` *and* pulls `junit:junit:4.13.2` + vintage engine, while
  test files mix both. Pick JUnit 5 + vintage for legacy, and verify Robolectric
  4.14.1 actually runs under the platform runner.
- **2.4** Record the **real** pass/fail count. Publish it even if ugly.
- **2.5** Reconcile `jacocoCoverageVerification`: it demands 80% global / 90% on
  `nlu.*` and `executor.*`. Measure first, then set thresholds to
  `current - 2%` as a ratchet. Leaving an unmeetable 90% gate is why it is
  currently never run.

**Exit criteria:** `./gradlew :app:testDebugUnitTest` compiles and runs; real
numbers recorded in `TEST_RESULTS.md`; coverage gate passes at honest thresholds.

---

## 5. Phase 3 — Ship a real model (~3–5 days)

**Goal:** on-device inference with the fine-tuned Egyptian model. Delivers
ADR-002 and Tech Spec §5.

- **3.1 Merge the LoRA adapter into base weights.** Fetch the FunctionGemma-270M
  base, apply `models/functiongemma-270m-egyptian/adapter_model.safetensors`
  (use the better of `checkpoint-2338` / `checkpoint-4175` — compare
  `trainer_state.json`), save merged FP16.
- **3.2 Convert to GGUF and quantize** via `external/llama.cpp`
  (`convert_hf_to_gguf.py` → `llama-quantize` Q4_K_M). Validate the ~288 MB
  target from Tech Spec §6 and record the actual size.
- **3.3 Decide model delivery.** A 288 MB asset inside a 224 MB-already APK is
  not viable for distribution. Recommend **download-on-first-run into app-private
  storage** with SHA-256 verification, matching the `ModelConfig.checksum` field
  the Data Model already specifies. `hybrid/ModelLoadingService` exists for this.
- **3.4 Delete the 327-byte placeholder** `.gguf` and fail loudly on a missing
  model rather than pretending to load it.
- **3.5 Wire the ASR model** — `EgyptianWhisperASR.java` / `WhisperASREngine.java`
  need real whisper.cpp weights (ADR-004). Same download+checksum path.
- **3.6 End-to-end smoke test on device:** wake word → ASR → normalize →
  FunctionGemma → execute → TTS, for one command ("اتصل بماما").

**Exit criteria:** real GGUF loads on a physical Honor X6c; one full voice
command completes without mocks.

**Dependency note:** 3.2 requires Phase 1.4 (submodules built). 3.6 requires
Phase 1 complete.

---

## 6. Phase 4 — Close functional gaps in PRD order (~1 week)

Prioritized by the measured weaknesses in §1.8, not by guesswork.

- **4.1 `device_control` 61% → >90%.** Worst measured intent and a core PRD §6.6
  requirement. Inspect the 13 errors in `evaluation_report.json`; expect Egyptian
  dialect variants for WiFi/Bluetooth/volume/brightness under-represented in
  `datasets/egyptian_voice_commands`. Augment, re-evaluate.
- **4.2 `read_time` 60% → >90%.** Same method; likely a small normalizer fix in
  `EgyptianNormalizer`.
- **4.3 Wake word end-to-end.** `wakeword/` (6 files) was excluded and never
  ran. Verify "يا صاحبي" and senior "يا كبير" (Product Spec §3.1) including
  screen-off operation (PRD §7.5). Measure false-positive/negative rates —
  Acceptance Criteria §7 requires this and no data exists.
- **4.4 Emergency path.** PRD §6.7 and Product Spec §4.1: "يا نجدة" →
  `EMERGENCY_CALL`. `EmergencyFollowupReceiver` and `VolumeButtonReceiver` were
  both missing from the APK — this entire safety-critical path is unverified.
  Treat as highest-severity functional work after 4.1.
- **4.5 Validate the 10 workflows.** All 10 YAML files exist in
  `app/src/main/assets/workflows/` (Phase 3 of the original plan). `WorkflowEngine`
  compiles; execution has never been tested. Run each against real apps.
- **4.6 Senior Mode.** `SeniorMode`/`SeniorModeManager` were excluded. Verify
  slower/louder TTS per Product Spec §3.4.

**Exit criteria:** re-run evaluation, every intent >90%, overall ≥ the 95% PRD
target or a documented reason why not.

---

## 7. Phase 5 — Performance, honestly measured (~3 days)

- **5.1** Build a reproducible benchmark harness. `benchmark/PerformanceBenchmarkTest.java`
  and `androidx.benchmark` are already wired.
- **5.2** Measure on a real Honor X6c, with the real model: cold load, warm load,
  inference latency, RAM, battery drain/hour.
- **5.3** Compare against Tech Spec §6 targets (288 MB / 550 MB / 4.8 s / 1.2 s /
  350 ms / 3%/hr). **Expect to miss 350 ms** — the repo's own data says 721 ms.
  Either optimize or amend the spec. Do not re-publish 350 ms unmeasured.
- **5.4** Optimize only what measurement indicts. Levers available:
  `NNAPIDelegator`, `ModelCacheManager`, quantization level, `DeviceClassDetector`.
- **5.5** Rewrite `PERFORMANCE_BENCHMARK_RESULTS.md` from measured data, with the
  device, build, and date attached.

---

## 8. Phase 6 — Security & release hardening (~3 days)

- **6.1 Remove `android:sharedUserId="android.uid.system"`.** Achieve ADR-006's
  privileged access via the already-integrated **Shizuku** (`dev.rikka.shizuku`
  is a dependency and `ShizukuProvider` is in the manifest) plus the documented
  Magisk/`priv-app` install path. `sharedUserId` is deprecated, blocks normal
  installation, and forces platform-key signing.
- **6.2 Purge the committed debug keystore** and its hardcoded password from
  `app/build.gradle`; generate locally via the existing `createDebugKeystore`
  task.
- **6.3 Fail the release build on missing signing secrets** instead of
  `?: ""`.
- **6.4 Implement the promised encryption at rest** for models/configs
  (`androidx.security:security-crypto` is already a dependency), or amend
  Tech Spec §7 and `SECURITY.md` to stop promising it.
- **6.5 Verify the no-network claim.** ADR-001/PRD §7.1 promise nothing leaves
  the device, yet OkHttp, Retrofit, ML Kit Translate and a
  `networkSecurityConfig` are all present. Either prove no user audio/text egress
  (and document why the HTTP stack exists) or remove it.
- **6.6 Audit the permission set** against PRD §7 and document each.
- **6.7 Run `/security-review`** over the result.

---

## 9. Phase 7 — CI that enforces all of the above (~2 days)

Three workflows exist (`ci.yml`, `release.yml`, `test-suite.yml`) but the
evidence says they are not gating: the tests they run cannot compile, and
`submodules: recursive` is set while the submodules are empty.

- **7.1** Make CI build with zero excludes and `ALLOW_MOCK_NATIVE=OFF`.
- **7.2** Add the **manifest/dex parity check** from §1.3 as a hard gate. This is
  the single highest-value CI addition — it is what would have caught the hollow
  APK.
- **7.3** Gate on real unit tests + the ratcheted coverage threshold.
- **7.4** Add an accuracy-regression gate against
  `evaluation_results/evaluation_report.json`.
- **7.5** Cache the NDK and submodule builds; native compilation will dominate CI
  time.

---

## 10. Phase 8 — Documentation re-baseline (~2 days)

The repo has **142 markdown files** and a serious credibility problem. Fewer,
truer documents.

- **8.1** Rewrite or delete: `BUILD_STATUS_REPORT.md` (wrong: 11 errors not 100,
  and the build succeeds), `CHECKPOINT.md` (claims stable what never ran),
  `TEST_RESULTS.md` + `ACCURACY_TEST_RESULTS.md` (unreproducible),
  `PERFORMANCE_BENCHMARK_RESULTS.md` (unsourced),
  `FINAL_IMPLEMENTATION_REPORT.md`, `PHASE_4_COMPLETION_REPORT.md`.
- **8.2** Adopt a rule: **no metric without a command that reproduces it.**
- **8.3** Consolidate the ~15 overlapping root-level reports into `docs/`.
- **8.4** Make `README.md` build instructions actually work from a clean clone
  (they currently omit `local.properties` and submodule init).
- **8.5** Add ADRs for the decisions this plan makes: Shizuku-over-`sharedUserId`,
  model-download-over-bundling, mock-native-must-be-explicit.
- **8.6** Update `IMPLEMENTATION-PLAN.md` / `CHECKPOINT.md` / `ROADMAP.md` to
  reflect verified state.

---

## 11. Sequencing

```
Phase 1  Build integrity      ~2d   🔴 BLOCKING — nothing else is meaningful first
Phase 2  Real tests           ~2d   ← needs P1
Phase 3  Real model           ~3-5d ← needs P1.4; can overlap P2
Phase 4  Functional gaps      ~1w   ← needs P1, P3
Phase 5  Performance          ~3d   ← needs P3, P4
Phase 6  Security             ~3d   ← can overlap P4/P5
Phase 7  CI enforcement       ~2d   ← needs P1, P2
Phase 8  Docs re-baseline     ~2d   ← last; records measured truth
                              ─────
                              ~4-5 weeks single developer
```

Critical path: **1 → 3 → 4 → 5**. Phases 2, 6, 7 parallelize.

Mapping to the original `IMPLEMENTATION-PLAN.md`: its Phases 1–3 are largely
*written but never integrated* — this plan's Phase 1 is what makes them real.
Its Phases 4–8 map onto Phases 5, 6, 4, 8 here. Roadmap "Long-term" items
(other dialects, SDK extraction, LLM fallback) stay out of scope.

---

## 12. Risks

| Risk | L | Impact | Mitigation |
|---|---|---|---|
| llama.cpp/whisper.cpp won't cross-compile on NDK 27 | **High** | Blocks P3, P5 | Least predictable task in the plan. Pin known-good submodule commits; fall back to an older NDK for native only; ONNX Runtime is already a dependency as an escape hatch |
| Merged model misses ~288 MB / 350 ms targets | **High** | Amends Tech Spec | Repo data already shows 721 ms. Plan to amend the spec, not the measurement |
| `HybridOrchestrator` API drift is deeper than the tests | Med | Extends P2 | 47 errors in one file may indicate the source, not the test, is wrong — read `PromptTemplates.kt`/`HybridModule.kt` for intended contract before choosing a side |
| Removing `sharedUserId` breaks privileged features | Med | Rework P6 | Shizuku already integrated; test on device early in P6 |
| No physical Honor X6c available | Med | Blocks P3.6, P4, P5 | Confirm hardware access **before** starting P3. Emulator cannot validate wake word, battery, or screen-off |
| 87.25% → 95% needs more data than exists | Med | Extends P4 | Target per-intent fixes (61%, 60%) first; they carry most of the deficit |

---

## 13. Definition of done

- [ ] Zero `java.exclude` in `app/build.gradle`; all 197 sources compile
- [ ] `ALLOW_MOCK_NATIVE=OFF`; real `.so` libraries (>1 MB)
- [ ] 14/14 manifest components present in the APK, CI-enforced
- [ ] Unit tests compile and run; real counts published
- [ ] Real fine-tuned GGUF loads on device with checksum verification
- [ ] Every intent >90%; overall accuracy measured and published
- [ ] Latency/RAM/battery measured on Honor X6c, spec amended where missed
- [ ] End-to-end: wake word → ASR → intent → execution → TTS, no mocks
- [ ] Emergency path ("يا نجدة") verified on device
- [ ] All 10 workflows execute successfully
- [ ] `sharedUserId` removed; no committed keystore; release signing fails closed
- [ ] CI gates build + manifest parity + tests + coverage + accuracy
- [ ] Every published metric has a reproducing command
```
