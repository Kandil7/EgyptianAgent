# EgyptianAgent - Project Structure

**Last Updated:** 2026-08-04  
**Version:** 1.1.0 (Reorganized)

---

## 📁 Project Structure Overview

```
EgyptianAgent/
├── 📄 README.md                    # Main project documentation
├── 📄 INDEX.md                     # Quick navigation hub
├── 📄 CONTRIBUTING.md              # Contribution guidelines
├── 📄 SECURITY.md                  # Security policy
├── 📄 AGENTS.md                    # Agent/LLM workflow instructions
├── 📄 LICENSE                      # Project license
├── 📄 build.gradle                 # Root build configuration
├── 📄 settings.gradle              # Project settings (module: android)
├── 📄 gradle.properties            # Gradle properties
├── 📄 gradlew / gradlew.bat        # Gradle wrapper (repo root)
├── 📄 local.properties.example     # SDK path template (copy → local.properties)
├── 📄 CMakeLists.txt               # Native build configuration
│
├── 📱 android/                     # Android application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/egyptian/agent/
│   │   │   │   ├── ai/            # AI engines (FunctionGemma, Llama)
│   │   │   │   ├── llm/           # LLM integration
│   │   │   │   ├── nlp/           # NLP processing
│   │   │   │   ├── nlu/           # NLU components
│   │   │   │   ├── asr/           # Automatic Speech Recognition
│   │   │   │   ├── stt/           # Speech-to-Text
│   │   │   │   ├── core/          # Core services
│   │   │   │   ├── service/       # Android services
│   │   │   │   ├── executor/      # Command executors
│   │   │   │   ├── executors/     # Legacy executors (see AGENTS.md)
│   │   │   │   ├── emergency/     # Emergency handling
│   │   │   │   ├── accessibility/ # Accessibility features
│   │   │   │   ├── hybrid/        # Hybrid orchestrator (fast/slow path)
│   │   │   │   ├── navigation/    # UI navigation engine (slow path)
│   │   │   │   ├── workflow/      # YAML workflow engine
│   │   │   │   ├── vision/        # Vision fallback
│   │   │   │   ├── security/      # Security & privacy
│   │   │   │   ├── analytics/     # Privacy-compliant analytics
│   │   │   │   ├── backup/        # Backup/restore
│   │   │   │   ├── feedback/      # User feedback
│   │   │   │   ├── performance/   # Performance monitoring
│   │   │   │   ├── updates/       # OTA updates
│   │   │   │   ├── contacts/      # Arabic contact matching
│   │   │   │   ├── system/        # System privilege management
│   │   │   │   ├── wakeword/      # Wake word detection
│   │   │   │   ├── receivers/     # Broadcast receivers
│   │   │   │   ├── ui/            # Activities & UI
│   │   │   │   └── utils/         # Utilities
│   │   │   ├── cpp/               # Native C++ code
│   │   │   ├── assets/            # App assets (models, workflows, grammars)
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                  # Unit tests (JUnit 5 + Robolectric)
│   │   └── androidTest/           # Instrumented tests
│   ├── build.gradle
│   └── keystore/                  # Signing keys (debug only, gitignored)
│
├── 🚀 deploy/                      # Build, deploy & device tooling
│   ├── README.md
│   ├── build/                     # Build scripts & Gradle wrapper helpers
│   │   ├── scripts/               # build.sh, build_functiongemma.sh, ...
│   │   ├── install_gradle.ps1
│   │   └── install_jdk17.ps1
│   ├── android/                   # SDK/device setup (Windows PS + bash)
│   │   ├── windows_setup.ps1
│   │   ├── install_android_sdk.ps1
│   │   └── verify_implementation.sh
│   ├── scripts-deploy/            # Deployment scripts
│   │   ├── deploy_production.sh
│   │   ├── deploy_functiongemma.sh
│   │   ├── initialize_submodules.sh
│   │   └── verify_deployment.sh/.ps1
│   ├── hf/                        # HuggingFace dataset upload tools
│   │   ├── upload_to_hf.py
│   │   └── merge_and_push.py
│   └── device/                    # Device-side utilities
│
├── 🧠 ml/                          # ML pipeline
│   ├── requirements.txt           # Python dependencies
│   ├── prompts/                   # Generation/system prompts
│   │   ├── system_prompt.txt
│   │   ├── intent_generation_prompt.md
│   │   └── negative_examples.md
│   └── finetune/
│       ├── scripts/               # Fine-tuning, eval, data-gen scripts
│       │   ├── finetune_functiongemma_egyptian.py
│       │   ├── evaluate_egyptian_accuracy.py
│       │   ├── generate_synthetic_data.py
│       │   └── download_functiongemma_model.sh
│       ├── configs/               # Fine-tuning configuration
│       ├── data/                  # Datasets
│       │   ├── voice_commands/    # train/eval/test.jsonl (messages format)
│       │   ├── ui_navigation/     # train/test.jsonl
│       │   └── egyptian_commands/ # Schema + seed samples
│       ├── checkpoints/           # LoRA adapters (committed)
│       ├── evaluation/            # Evaluation reports
│       └── docs/                  # ML planning docs
│
├── 📚 docs/                        # All documentation
│   ├── README.md                   # Documentation index
│   ├── FUNCTIONGEMMA_QUICKSTART.md # Quick start guide
│   ├── architecture/               # Architecture docs
│   ├── deployment/                 # Deployment guides
│   ├── api/                        # API references
│   ├── guides/                     # User & developer guides
│   ├── testing/                    # Testing documentation
│   ├── performance/                # Performance benchmarks
│   ├── integration/                # Integration guides
│   └── archive/                    # Archived documents & old reports
│
├── 🤖 agents/                      # Agent/persona definitions
│   ├── index.md
│   ├── OPENCODE_INTEGRATION.md
│   └── *.md                        # 16 persona definitions
│
├── 🔧 external/                    # External dependencies (git submodules)
│   ├── llama.cpp/                  # Llama.cpp submodule
│   └── whisper.cpp/                # Whisper.cpp submodule
│
├── ⚙️ .github/                     # GitHub configuration
│   └── workflows/                  # ci.yml, test-suite.yml, release.yml
│
├── 🛠️ tools/                       # Local SDK/adb tooling (gitignored)
└── 📊 ml/finetune/data/            # Datasets live under ml/ (see above)
```

---

## 📊 File Statistics

| Category | Count | Location |
|----------|-------|----------|
| **Root Files** | 16 | `/` |
| **Documentation** | 25+ | `/docs/` |
| **Build Scripts** | 6 | `/deploy/build/scripts/` |
| **Deploy Scripts** | 5 | `/deploy/scripts-deploy/` |
| **Device/SDK Scripts** | 25 | `/deploy/android/` |
| **ML Scripts** | 18 | `/ml/finetune/scripts/` |
| **Test Files** | 35+ | `/android/src/test/` |
| **Java/Kotlin Source** | 250+ | `/android/src/main/java/` |
| **Dataset Files** | 8 | `/ml/finetune/data/` |
| **Config Files** | 4 | `/ml/finetune/configs/` |
| **Agent Definitions** | 16 | `/agents/` |

---

## 🎯 Key Directories

### `/docs/` - Documentation Hub
All project documentation organized by category:
- **architecture/** - System architecture documents
- **deployment/** - Deployment guides and instructions
- **api/** - API references
- **guides/** - User manuals and troubleshooting
- **testing/** - Test plans and strategies
- **performance/** - Performance benchmarks
- **integration/** - Integration guides
- **archive/** - Historical documents (old reports, summaries)

### `/deploy/` - Build, Deploy & Device Tooling
All executable scripts organized by function:
- **build/** - Build automation + Gradle helper tooling
- **scripts-deploy/** - Deployment automation
- **android/** - SDK/device setup (Windows PowerShell + bash)
- **hf/** - HuggingFace dataset upload tools
- **device/** - Device-side utilities

### `/ml/` - ML Pipeline
Egyptian Arabic voice command ML pipeline:
- **finetune/scripts/** - Fine-tuning, evaluation, and synthetic-data scripts
- **finetune/data/** - voice_commands (train/eval/test), ui_navigation, egyptian_commands (schema + seeds)
- **finetune/configs/** - Fine-tuning configuration
- **finetune/checkpoints/** - LoRA adapters (committed)
- **prompts/** - System and generation prompts

### `/android/` - Android Application
Main application code:
- **src/main/java/** - Java/Kotlin source code
- **src/main/cpp/** - Native C++ code
- **src/main/assets/** - Assets (models, YAML workflows, grammars)
- **src/test/** - Unit tests
- **src/androidTest/** - Instrumented tests

---

## 🚀 Quick Start

### Build the Project
```bash
# Standard build
./deploy/build/scripts/build.sh

# Production build
./deploy/build/scripts/build_production.sh

# FunctionGemma variant
./deploy/build/scripts/build_functiongemma.sh
```

### Deploy to Device
```bash
# Deploy production version
./deploy/scripts-deploy/deploy_production.sh

# Deploy FunctionGemma model
./deploy/scripts-deploy/deploy_functiongemma.sh
```

### Run Tests
```bash
# Run all FunctionGemma tests
./ml/finetune/scripts/run_functiongemma_tests.sh --all

# Run unit tests only
./ml/finetune/scripts/run_functiongemma_tests.sh --unit

# Run with coverage
./ml/finetune/scripts/run_functiongemma_tests.sh --coverage
```

### Fine-tune Model
```bash
# Install dependencies
pip install -r ml/requirements.txt

# Fine-tune FunctionGemma
python ml/finetune/scripts/finetune_functiongemma_egyptian.py \
  --config ml/finetune/configs/finetune_config.yaml

# Evaluate accuracy
python ml/finetune/scripts/evaluate_egyptian_accuracy.py
```

---

## 📝 Documentation Navigation

| Need | Document |
|------|----------|
| **Getting Started** | [README.md](README.md) |
| **Architecture** | [docs/architecture/ARCHITECTURE.md](docs/architecture/ARCHITECTURE.md) |
| **Deployment** | [docs/deployment/DEPLOYMENT_GUIDE.md](docs/deployment/DEPLOYMENT_GUIDE.md) |
| **API Reference** | [docs/api/API_REFERENCE.md](docs/api/API_REFERENCE.md) |
| **User Manual (Arabic)** | [docs/guides/user_manual_ar.md](docs/guides/user_manual_ar.md) |
| **Troubleshooting** | [docs/guides/TROUBLESHOOTING.md](docs/guides/TROUBLESHOOTING.md) |
| **FunctionGemma Guide** | [docs/FUNCTIONGEMMA_QUICKSTART.md](docs/FUNCTIONGEMMA_QUICKSTART.md) |
| **Testing** | [docs/testing/FUNCTIONGEMMA_TEST_PLAN.md](docs/testing/FUNCTIONGEMMA_TEST_PLAN.md) |

---

## 🗂️ File Organization Principles

1. **Single Source of Truth**: Each file has one canonical location
2. **Logical Grouping**: Files grouped by function, not type
3. **Clear Hierarchy**: Directory structure reflects importance and usage
4. **Easy Navigation**: README.md in every major directory
5. **Git-Friendly**: Structure optimized for version control
6. **Build-Ready**: Scripts organized for CI/CD integration

---

## 🔄 Migration History

**2026-08-04 - Repository Reorganization (v1.1.0)**
- Moved Android module: `app/` → `android/`
- Reorganized scripts: `scripts/` → `deploy/` (build, android, scripts-deploy, hf, device)
- Moved ML pipeline: `datasets/`, `configs/`, `models/`, `vllm_config/` → `ml/`
- Restored Gradle wrapper to repo root (`gradlew`, `gradlew.bat`, `gradle/wrapper/`)
- Moved test harness classes from `src/main/java` to `src/test/java`
- Updated `.gitignore`, `.gitattributes`, added `local.properties.example`
- Updated root docs (README, INDEX, PROJECT_STRUCTURE, CONTRIBUTING)
- Old reports archived to `docs/archive/reports/`

**2026-03-03 - Earlier Reorganization (v2.0)**
- Consolidated all documentation into `/docs/`
- Archived old reports and summaries
- Removed duplicate files

---

## 📌 Important Notes

1. **Backup**: Old reports archived in `docs/archive/reports/`
2. **Git History**: File history preserved via `git mv`
3. **References**: Update any external references to old paths (`app/`, `scripts/`, `datasets/`)
4. **CI/CD**: `.github/workflows/` references `:android:` module tasks
5. **Documentation**: All active docs in `/docs/`, archive in `/docs/archive/`

---

## ✅ Post-Migration Checklist

- [x] All files moved to new structure
- [x] Root directory cleaned
- [x] Scripts organized by function
- [x] Documentation organized by category
- [x] Test files organized by component
- [x] Obsolete files archived/deleted
- [x] Root docs updated (README, INDEX, PROJECT_STRUCTURE, CONTRIBUTING)
- [x] `.gitignore` / `.gitattributes` updated for new layout
- [x] `local.properties.example` added
- [ ] Commit changes to version control

---

## 📞 Support

For questions about the project structure:
- Review this document
- Check directory-specific README files
- See [docs/guides/TROUBLESHOOTING.md](docs/guides/TROUBLESHOOTING.md)
- Open an issue on GitHub

---

**Document Version:** 2.0  
**Maintained By:** EgyptianAgent Team  
**Review Date:** Quarterly
