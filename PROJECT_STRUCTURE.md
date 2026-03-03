# EgyptianAgent - Project Structure

**Last Updated:** 2026-03-03  
**Version:** 2.0 (Reorganized)  

---

## 📁 Project Structure Overview

```
EgyptianAgent/
├── 📄 README.md                    # Main project documentation
├── 📄 CONTRIBUTING.md              # Contribution guidelines
├── 📄 SECURITY.md                  # Security policy
├── 📄 LICENSE                      # Project license
├── 📄 build.gradle                 # Root build configuration
├── 📄 settings.gradle              # Project settings
├── 📄 gradle.properties            # Gradle properties
├── 📄 CMakeLists.txt               # Native build configuration
├── 📄 requirements_functiongemma.txt  # Python dependencies
│
├── 📦 app/                         # Android application module
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
│   │   │   │   ├── emergency/     # Emergency handling
│   │   │   │   ├── accessibility/ # Accessibility features
│   │   │   │   └── utils/         # Utilities
│   │   │   ├── cpp/               # Native C++ code
│   │   │   ├── assets/            # App assets
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                  # Unit tests
│   │   └── androidTest/           # Instrumented tests
│   └── build.gradle
│
├── 📚 docs/                        # All documentation
│   ├── README.md                   # Documentation index
│   ├── FUNCTIONGEMMA_QUICKSTART.md # Quick start guide
│   │
│   ├── architecture/               # Architecture docs
│   │   ├── README.md
│   │   ├── ARCHITECTURE.md
│   │   ├── PRODUCTION_ARCHITECTURE.md
│   │   └── FUNCTIONGEMMA_ARCHITECTURE.md
│   │
│   ├── deployment/                 # Deployment guides
│   │   ├── README.md
│   │   ├── DEPLOYMENT_GUIDE.md
│   │   ├── FUNCTIONGEMMA_DEPLOYMENT_GUIDE.md
│   │   └── production_deployment_guide.md
│   │
│   ├── api/                        # API references
│   │   ├── README.md
│   │   ├── API_REFERENCE.md
│   │   └── FUNCTIONGEMMA_API_REFERENCE.md
│   │
│   ├── guides/                     # User & developer guides
│   │   ├── README.md
│   │   ├── TROUBLESHOOTING.md
│   │   ├── user_manual.md
│   │   ├── user_manual_ar.md
│   │   ├── RELEASE_NOTES.md
│   │   ├── FUNCTIONGEMMA_FINETUNING_GUIDE.md
│   │   └── FUNCTIONGEMMA_MIGRATION_GUIDE.md
│   │
│   ├── testing/                    # Testing documentation
│   │   ├── README.md
│   │   ├── FUNCTIONGEMMA_TEST_PLAN.md
│   │   └── TEST_SUITE.md
│   │
│   ├── performance/                # Performance benchmarks
│   │   ├── README.md
│   │   └── FUNCTIONGEMMA_PERFORMANCE_BENCHMARKS.md
│   │
│   ├── integration/                # Integration guides
│   │   ├── README.md
│   │   ├── FUNCTIONGEMMA_INTEGRATION.md
│   │   └── saiyy_ps_integration.md
│   │
│   └── archive/                    # Archived documents
│       ├── README.md
│       ├── reports/               # Old reports
│       ├── summaries/             # Old summaries
│       ├── plans/                 # Old plans
│       └── validation/            # Old validation docs
│
├── 🛠️ scripts/                     # All scripts
│   ├── README.md                   # Scripts index
│   ├── build/                     # Build scripts
│   │   ├── build.sh
│   │   ├── build_production.sh
│   │   ├── build_functiongemma.sh
│   │   └── build_native_libs.sh
│   │
│   ├── deploy/                    # Deployment scripts
│   │   ├── deploy_production.sh
│   │   ├── deploy_functiongemma.sh
│   │   └── initialize_submodules.sh
│   │
│   ├── model/                     # Model management
│   │   ├── download_functiongemma_model.sh
│   │   ├── download_whisper_model.sh
│   │   ├── convert_to_gguf.sh
│   │   ├── convert_llama_model.sh
│   │   ├── setup_llama_model.sh
│   │   └── setup_functiongemma_model.sh
│   │
│   ├── test/                      # Test scripts
│   │   ├── run_functiongemma_tests.sh
│   │   └── test_integration.sh
│   │
│   ├── finetune/                  # Fine-tuning scripts
│   │   ├── finetune_functiongemma_egyptian.py
│   │   └── evaluate_egyptian_accuracy.py
│   │
│   └── utils/                     # Utility scripts
│       ├── verify_implementation.sh
│       ├── fetch_models.sh
│       ├── security_audit.sh
│       ├── honor_battery_fix.sh
│       ├── install_as_system_app.sh
│       └── complete_build.sh
│
├── 📊 datasets/                    # Training datasets
│   ├── README.md
│   └── egyptian_voice_commands/
│       ├── train.jsonl            # 500+ training examples
│       ├── eval.jsonl             # 50 validation examples
│       └── test.jsonl             # 100 test examples
│
├── ⚙️ configs/                     # Configuration files
│   ├── README.md
│   └── finetune_config.yaml       # Fine-tuning configuration
│
├── 🤖 agents/                      # Agent definitions
│   ├── index.md
│   ├── Technical_Lead_Agent.md
│   ├── Product_Manager_Agent.md
│   ├── Senior_Android_Engineer_System_Agent.md
│   └── ... (15 agent definitions)
│
├── 🔧 external/                    # External dependencies
│   ├── llama.cpp/                 # Llama.cpp submodule
│   ├── whisper.cpp/               # Whisper.cpp submodule
│   └── faster-whisper/            # Faster Whisper
│
├── 🧪 vllm_config/                 # vLLM configuration
│
├── 🔒 .github/                     # GitHub configuration
│   ├── ISSUE_TEMPLATE/
│   ├── workflows/
│   └── ...
│
└── 🗄️ _migration_backup_*/         # Migration backup (safe to delete)
```

---

## 📊 File Statistics

| Category | Count | Location |
|----------|-------|----------|
| **Root Files** | 12 | `/` |
| **Documentation** | 25+ | `/docs/` |
| **Scripts** | 21 | `/scripts/` |
| **Test Files** | 50+ | `/app/src/test/` |
| **Java Source** | 100+ | `/app/src/main/java/` |
| **Dataset Files** | 3 | `/datasets/` |
| **Config Files** | 2 | `/configs/` |
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

### `/scripts/` - Automation Scripts
All executable scripts organized by function:
- **build/** - Build automation
- **deploy/** - Deployment automation
- **model/** - Model download and conversion
- **test/** - Test execution
- **finetune/** - Model fine-tuning
- **utils/** - Utility scripts

### `/datasets/` - Training Data
Egyptian Arabic voice command datasets:
- **egyptian_voice_commands/** - 650+ examples for fine-tuning

### `/app/` - Android Application
Main application code:
- **src/main/java/** - Java source code
- **src/main/cpp/** - Native C++ code
- **src/test/** - Unit tests
- **src/androidTest/** - Instrumented tests

---

## 🚀 Quick Start

### Build the Project
```bash
# Standard build
./scripts/build/build.sh

# Production build
./scripts/build/build_production.sh

# FunctionGemma variant
./scripts/build/build_functiongemma.sh
```

### Deploy to Device
```bash
# Deploy production version
./scripts/deploy/deploy_production.sh

# Deploy FunctionGemma model
./scripts/deploy/deploy_functiongemma.sh
```

### Run Tests
```bash
# Run all FunctionGemma tests
./scripts/test/run_functiongemma_tests.sh --all

# Run unit tests only
./scripts/test/run_functiongemma_tests.sh --unit

# Run with coverage
./scripts/test/run_functiongemma_tests.sh --coverage
```

### Fine-tune Model
```bash
# Install dependencies
pip install -r requirements_functiongemma.txt

# Fine-tune FunctionGemma
python scripts/finetune/finetune_functiongemma_egyptian.py \
  --config configs/finetune_config.yaml

# Evaluate accuracy
python scripts/finetune/evaluate_egyptian_accuracy.py
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

**2026-03-03 - Major Reorganization (v2.0)**
- Consolidated all documentation into `/docs/`
- Organized all scripts into `/scripts/`
- Archived 26+ old reports and summaries
- Removed duplicate files
- Created comprehensive README files for each directory

**Before:** 47 files in root directory  
**After:** 12 files in root directory

---

## 📌 Important Notes

1. **Backup**: Migration backup available in `_migration_backup_*/`
2. **Git History**: File history preserved via `git mv`
3. **References**: Update any external references to old paths
4. **CI/CD**: Update build pipelines to use new script paths
5. **Documentation**: All active docs in `/docs/`, archive in `/docs/archive/`

---

## ✅ Post-Migration Checklist

- [x] All files moved to new structure
- [x] Backup created
- [x] README files created for all directories
- [x] Root directory cleaned
- [x] Scripts organized by function
- [x] Documentation organized by category
- [x] Test files organized by component
- [x] Obsolete files archived/deleted
- [ ] Update CI/CD pipelines (if applicable)
- [ ] Update external documentation links (if applicable)
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
