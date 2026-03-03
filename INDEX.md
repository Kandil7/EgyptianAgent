# EgyptianAgent - Project Index

**Quick Navigation Guide**  
**Last Updated:** 2026-03-03  
**Version:** 2.0  

---

## 🚀 Quick Links

### For New Users
- **[Project Overview](README.md)** - What is EgyptianAgent?
- **[User Manual (Arabic)](docs/guides/user_manual_ar.md)** - How to use the app
- **[Quick Start Guide](docs/FUNCTIONGEMMA_QUICKSTART.md)** - Get started quickly

### For Developers
- **[Architecture](docs/architecture/ARCHITECTURE.md)** - System design
- **[API Reference](docs/api/API_REFERENCE.md)** - Code documentation
- **[Deployment Guide](docs/deployment/DEPLOYMENT_GUIDE.md)** - How to deploy

### For Contributors
- **[Contributing Guide](CONTRIBUTING.md)** - How to contribute
- **[Project Structure](PROJECT_STRUCTURE.md)** - File organization
- **[Security Policy](SECURITY.md)** - Security guidelines

---

## 📚 Documentation by Category

### 🏗️ Architecture
- **[Main Architecture](docs/architecture/ARCHITECTURE.md)** - High-level system design
- **[Production Architecture](docs/architecture/PRODUCTION_ARCHITECTURE.md)** - Production setup
- **[FunctionGemma Architecture](docs/architecture/FUNCTIONGEMMA_ARCHITECTURE.md)** - AI engine details

### 🚀 Deployment
- **[Deployment Guide](docs/deployment/DEPLOYMENT_GUIDE.md)** - General deployment
- **[FunctionGemma Deployment](docs/deployment/FUNCTIONGEMMA_DEPLOYMENT_GUIDE.md)** - AI model deployment
- **[Production Deployment](docs/deployment/production_deployment_guide.md)** - Production instructions

### 📖 API & Reference
- **[API Reference](docs/api/API_REFERENCE.md)** - Complete API docs
- **[FunctionGemma API](docs/api/FUNCTIONGEMMA_API_REFERENCE.md)** - FunctionGemma-specific API

### 📚 Guides & Manuals
- **[User Manual (English)](docs/guides/user_manual.md)** - English user guide
- **[User Manual (Arabic)](docs/guides/user_manual_ar.md)** - Arabic user guide
- **[Troubleshooting](docs/guides/TROUBLESHOOTING.md)** - Common issues
- **[Release Notes](docs/guides/RELEASE_NOTES.md)** - Version history
- **[FunctionGemma Fine-tuning](docs/guides/FUNCTIONGEMMA_FINETUNING_GUIDE.md)** - Model training
- **[Migration Guide](docs/guides/FUNCTIONGEMMA_MIGRATION_GUIDE.md)** - Upgrade guide

### 🧪 Testing
- **[Test Plan](docs/testing/FUNCTIONGEMMA_TEST_PLAN.md)** - Testing strategy
- **[Test Suite](docs/testing/TEST_SUITE.md)** - Test documentation

### ⚡ Performance
- **[Performance Benchmarks](docs/performance/FUNCTIONGEMMA_PERFORMANCE_BENCHMARKS.md)** - Speed & accuracy

### 🔗 Integration
- **[FunctionGemma Integration](docs/integration/FUNCTIONGEMMA_INTEGRATION.md)** - Integration guide
- **[Saiy-PS Integration](docs/integration/saiyy_ps_integration.md)** - Voice engine setup

### 📦 Scripts
- **[Scripts Index](scripts/README.md)** - All automation scripts
  - **Build:** `scripts/build/` - Build automation
  - **Deploy:** `scripts/deploy/` - Deployment scripts
  - **Model:** `scripts/model/` - Model management
  - **Test:** `scripts/test/` - Test execution
  - **Fine-tune:** `scripts/finetune/` - Training scripts
  - **Utils:** `scripts/utils/` - Utility scripts

### 📊 Datasets
- **[Datasets Index](datasets/README.md)** - Training data documentation
- **Egyptian Voice Commands:** `datasets/egyptian_voice_commands/`

### ⚙️ Configuration
- **[Configs Index](configs/README.md)** - Configuration documentation
- **Fine-tuning Config:** `configs/finetune_config.yaml`

### 🤖 Agents
- **[Agent Definitions](agents/index.md)** - AI agent documentation

---

## 🛠️ Common Tasks

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
# Deploy app
./scripts/deploy/deploy_production.sh

# Deploy FunctionGemma model
./scripts/deploy/deploy_functiongemma.sh
```

### Run Tests
```bash
# All tests
./scripts/test/run_functiongemma_tests.sh --all

# Unit tests only
./scripts/test/run_functiongemma_tests.sh --unit

# With coverage
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

### Download Models
```bash
# Download FunctionGemma model
./scripts/model/download_functiongemma_model.sh

# Convert to GGUF format
./scripts/model/convert_to_gguf.sh
```

---

## 📁 Project Structure Quick Reference

```
EgyptianAgent/
├── 📄 README.md              ← You are here (project index)
├── 📄 PROJECT_STRUCTURE.md   ← Detailed file structure
├── 📄 CONTRIBUTING.md        ← Contribution guide
├── 📄 SECURITY.md            ← Security policy
│
├── 📚 docs/                  ← All documentation
│   ├── architecture/         ← System architecture
│   ├── deployment/           ← Deployment guides
│   ├── api/                  ← API references
│   ├── guides/               ← User guides
│   ├── testing/              ← Test documentation
│   ├── performance/          ← Performance benchmarks
│   ├── integration/          ← Integration guides
│   └── archive/              ← Historical docs
│
├── 🛠️ scripts/               ← All automation scripts
│   ├── build/                ← Build scripts
│   ├── deploy/               ← Deployment scripts
│   ├── model/                ← Model management
│   ├── test/                 ← Test scripts
│   ├── finetune/             ← Fine-tuning scripts
│   └── utils/                ← Utility scripts
│
├── 📦 app/                   ← Android application
│   ├── src/main/java/        ← Source code
│   ├── src/main/cpp/         ← Native code
│   ├── src/test/             ← Unit tests
│   └── src/androidTest/      ← Instrumented tests
│
├── 📊 datasets/              ← Training datasets
├── ⚙️ configs/               ← Configuration files
├── 🤖 agents/                ← Agent definitions
└── 🔧 external/              ← External dependencies
```

---

## 🎯 Key Components

### AI Engines
- **FunctionGemma-270M-IT** - Primary intent classification (288MB, fast)
- **Llama 3.2 3B** - Fallback engine (2GB, comprehensive)
- **Whisper Egyptian ASR** - Speech-to-text for Egyptian dialect

### Core Services
- **VoiceService** - Main voice recognition service
- **WakeWordDetector** - "يا صاحبي" and "يا كبير" detection
- **TTSManager** - Text-to-speech with Egyptian dialect
- **EmergencyService** - Fall detection and emergency response

### Key Features
- **Egyptian Dialect Support** - 95%+ accuracy
- **Senior Mode** - Slower, louder audio
- **Offline Operation** - 100% local processing
- **System-level Access** - Works even when screen locked

---

## 📞 Support & Help

### Getting Help
1. **Check Documentation** - Browse [docs/](docs/)
2. **Troubleshooting** - See [docs/guides/TROUBLESHOOTING.md](docs/guides/TROUBLESHOOTING.md)
3. **User Manual** - Read [docs/guides/user_manual_ar.md](docs/guides/user_manual_ar.md)
4. **Open Issue** - Report on GitHub

### Contact
- **Project Lead:** EgyptianAgent Team
- **Email:** [See GitHub](https://github.com/)
- **Issues:** [GitHub Issues](https://github.com/egyptian-agent/issues)

---

## 📊 Project Status

### Current Version
- **Version:** 1.1.0
- **Build:** FunctionGemma-enabled
- **Status:** ✅ Production Ready
- **Last Updated:** 2026-03-03

### Key Metrics
| Metric | Value | Target |
|--------|-------|--------|
| **Accuracy** | 95.2% | 95%+ ✅ |
| **Load Time** | 4.8s | <5s ✅ |
| **Inference** | 350ms | <500ms ✅ |
| **Memory** | 550MB | <600MB ✅ |

---

## 🎓 Learning Path

### For New Developers
1. Read [Architecture](docs/architecture/ARCHITECTURE.md)
2. Review [API Reference](docs/api/API_REFERENCE.md)
3. Study [FunctionGemma Integration](docs/integration/FUNCTIONGEMMA_INTEGRATION.md)
4. Run [Tests](scripts/test/)
5. Start contributing!

### For End Users
1. Read [User Manual (Arabic)](docs/guides/user_manual_ar.md)
2. Follow [Quick Start](docs/FUNCTIONGEMMA_QUICKSTART.md)
3. Review [Troubleshooting](docs/guides/TROUBLESHOOTING.md)
4. Enjoy using EgyptianAgent!

---

## 🔗 External Resources

### Dependencies
- **llama.cpp:** [GitHub](https://github.com/ggerganov/llama.cpp)
- **whisper.cpp:** [GitHub](https://github.com/ggerganov/whisper.cpp)
- **FunctionGemma:** [HuggingFace](https://huggingface.co/google/functiongemma-270m-it)

### Tools
- **Android Studio:** [Download](https://developer.android.com/studio)
- **Gradle:** [Documentation](https://docs.gradle.org/)
- **Python:** [Download](https://www.python.org/)

---

## 📝 Quick Reference Card

### Intent Types Supported
- `CALL_CONTACT` - Make phone calls
- `SEND_WHATSAPP` - Send WhatsApp messages
- `SET_ALARM` - Set alarms/reminders
- `READ_TIME` - Read current time
- `EMERGENCY` - Emergency situations
- `OPEN_APP` - Open applications
- `DEVICE_CONTROL` - Control device settings

### Egyptian Commands Examples
- "اتصل بماما" → Call mom
- "ابعت واتساب لأحمد" → Send WhatsApp to Ahmed
- "نبهني بكرة الصبح" → Wake me up tomorrow morning
- "الساعة كام؟" → What time is it?
- "يا نجدة" → Emergency!

---

## ✅ Checklists

### First-Time Setup
- [ ] Install Android Studio
- [ ] Install Python 3.8+
- [ ] Clone repository
- [ ] Initialize submodules: `./scripts/deploy/initialize_submodules.sh`
- [ ] Download models: `./scripts/model/download_functiongemma_model.sh`
- [ ] Build project: `./scripts/build/build.sh`
- [ ] Deploy to device: `./scripts/deploy/deploy_production.sh`

### Daily Development
- [ ] Pull latest changes
- [ ] Run tests: `./scripts/test/run_functiongemma_tests.sh --unit`
- [ ] Build: `./scripts/build/build.sh`
- [ ] Deploy: `adb install -r app/build/outputs/apk/debug/*.apk`

### Before Commit
- [ ] Run all tests
- [ ] Check code style
- [ ] Update documentation if needed
- [ ] Add tests for new features
- [ ] Commit with clear message

---

## 📖 Document Navigation

| Need | Document | Location |
|------|----------|----------|
| **What is this?** | README.md | Root |
| **How to build?** | scripts/README.md | scripts/ |
| **How to deploy?** | docs/deployment/DEPLOYMENT_GUIDE.md | docs/deployment/ |
| **Architecture?** | docs/architecture/ARCHITECTURE.md | docs/architecture/ |
| **API docs?** | docs/api/API_REFERENCE.md | docs/api/ |
| **User guide?** | docs/guides/user_manual_ar.md | docs/guides/ |
| **Troubleshooting?** | docs/guides/TROUBLESHOOTING.md | docs/guides/ |
| **Testing?** | docs/testing/FUNCTIONGEMMA_TEST_PLAN.md | docs/testing/ |

---

**Last Updated:** 2026-03-03  
**Maintained By:** EgyptianAgent Team  
**Next Review:** 2026-04-03
