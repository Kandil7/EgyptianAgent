# EgyptianAgent - Project Index

**Quick Navigation Hub**  
**Last Updated:** 2026-03-03  
**Version:** 2.0.0 (FunctionGemma)  
**Status:** ✅ Production Ready

---

## 🚀 Quick Links

### For New Users
| Document | Description | Time |
|----------|-------------|------|
| **[Project Overview](README.md)** | What is EgyptianAgent? | 5 min |
| **[User Manual (Arabic)](docs/guides/user_manual_ar.md)** | How to use the app | 10 min |
| **[Quick Start Guide](docs/FUNCTIONGEMMA_QUICKSTART.md)** | Get started quickly | 5 min |
| **[Troubleshooting](docs/guides/TROUBLESHOOTING.md)** | Common issues & fixes | As needed |

### For Developers
| Document | Description | Time |
|----------|-------------|------|
| **[Architecture](docs/architecture/ARCHITECTURE.md)** | System design | 15 min |
| **[API Reference](docs/api/API_REFERENCE.md)** | Code documentation | Reference |
| **[Deployment Guide](docs/deployment/DEPLOYMENT_GUIDE.md)** | How to deploy | 20 min |
| **[FunctionGemma Integration](docs/integration/FUNCTIONGEMMA_INTEGRATION.md)** | AI engine setup | 30 min |

### For Contributors
| Document | Description | Time |
|----------|-------------|------|
| **[Contributing Guide](CONTRIBUTING.md)** | How to contribute | 10 min |
| **[Project Structure](PROJECT_STRUCTURE.md)** | File organization | 5 min |
| **[Security Policy](SECURITY.md)** | Security guidelines | 10 min |
| **[Test Plan](docs/testing/FUNCTIONGEMMA_TEST_PLAN.md)** | Testing requirements | 15 min |

---

## 📚 Documentation by Category

### 🏗️ Architecture Documentation

| Document | Description | Status |
|----------|-------------|--------|
| **[Main Architecture](docs/architecture/ARCHITECTURE.md)** | High-level system design | ✅ Complete |
| **[Production Architecture](docs/architecture/PRODUCTION_ARCHITECTURE.md)** | Production setup & scaling | ✅ Complete |
| **[FunctionGemma Architecture](docs/architecture/FUNCTIONGEMMA_ARCHITECTURE.md)** | AI engine details | ✅ Complete |

### 🚀 Deployment Documentation

| Document | Description | Status |
|----------|-------------|--------|
| **[Deployment Guide](docs/deployment/DEPLOYMENT_GUIDE.md)** | General deployment | ✅ Complete |
| **[FunctionGemma Deployment](docs/deployment/FUNCTIONGEMMA_DEPLOYMENT_GUIDE.md)** | AI model deployment | ✅ Complete |
| **[Production Deployment](docs/deployment/production_deployment_guide.md)** | Production instructions | ✅ Complete |

### 📖 API & Reference Documentation

| Document | Description | Status |
|----------|-------------|--------|
| **[API Reference](docs/api/API_REFERENCE.md)** | Complete API docs | ✅ Complete |
| **[FunctionGemma API](docs/api/FUNCTIONGEMMA_API_REFERENCE.md)** | FunctionGemma-specific API | ✅ Complete |

### 📚 Guides & Manuals

| Document | Description | Language | Status |
|----------|-------------|----------|--------|
| **[User Manual (English)](docs/guides/user_manual.md)** | English user guide | EN | ✅ Complete |
| **[User Manual (Arabic)](docs/guides/user_manual_ar.md)** | Arabic user guide | AR | ✅ Complete |
| **[Troubleshooting](docs/guides/TROUBLESHOOTING.md)** | Common issues | EN/AR | ✅ Complete |
| **[Release Notes](docs/guides/RELEASE_NOTES.md)** | Version history | EN | ✅ Complete |
| **[Fine-tuning Guide](docs/guides/FUNCTIONGEMMA_FINETUNING_GUIDE.md)** | Model training | EN | ✅ Complete |
| **[Migration Guide](docs/guides/FUNCTIONGEMMA_MIGRATION_GUIDE.md)** | Upgrade guide | EN | ✅ Complete |

### 🧪 Testing Documentation

| Document | Description | Status |
|----------|-------------|--------|
| **[Test Plan](docs/testing/FUNCTIONGEMMA_TEST_PLAN.md)** | Testing strategy | ✅ Complete |
| **[Test Suite](docs/testing/TEST_SUITE.md)** | Test documentation | ✅ Complete |

### ⚡ Performance Documentation

| Document | Description | Status |
|----------|-------------|--------|
| **[Performance Benchmarks](docs/performance/FUNCTIONGEMMA_PERFORMANCE_BENCHMARKS.md)** | Speed & accuracy metrics | ✅ Complete |

### 🔗 Integration Documentation

| Document | Description | Status |
|----------|-------------|--------|
| **[FunctionGemma Integration](docs/integration/FUNCTIONGEMMA_INTEGRATION.md)** | Integration guide | ✅ Complete |
| **[Saiy-PS Integration](docs/integration/saiyy_ps_integration.md)** | Voice engine setup | ✅ Complete |

---

## 🛠️ Common Tasks

### Build the Project

```bash
# Standard build
./deploy/build/scripts/build.sh

# Production build
./deploy/build/scripts/build_production.sh

# FunctionGemma variant (recommended)
./deploy/build/scripts/build_functiongemma.sh --release --native
```

| Script | Purpose | Output |
|--------|---------|--------|
| `build.sh` | Standard debug build | Debug APK |
| `build_production.sh` | Optimized release build | Release APK |
| `build_functiongemma.sh` | FunctionGemma build | FunctionGemma APK |
| `build_native_libs.sh` | Native library build | .so files |

### Deploy to Device

```bash
# Deploy app
./deploy/scripts-deploy/deploy_production.sh

# Deploy FunctionGemma model
./deploy/scripts-deploy/deploy_functiongemma.sh

# Initialize submodules (first time)
./deploy/scripts-deploy/initialize_submodules.sh
```

| Script | Purpose | Time |
|--------|---------|------|
| `deploy_production.sh` | Deploy release APK | 2 min |
| `deploy_functiongemma.sh` | Deploy with model | 3 min |
| `initialize_submodules.sh` | Setup dependencies | 5 min |

### Run Tests

```bash
# All tests
./ml/finetune/scripts/run_functiongemma_tests.sh --all

# Unit tests only
./ml/finetune/scripts/run_functiongemma_tests.sh --unit

# With coverage
./ml/finetune/scripts/run_functiongemma_tests.sh --coverage
```

| Flag | Description | Coverage Target |
|------|-------------|-----------------|
| `--all` | Run all tests | 95%+ |
| `--unit` | Unit tests only | 90%+ |
| `--integration` | Integration tests | 95%+ |
| `--coverage` | Generate coverage report | HTML + XML |

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

| Script | Purpose | Time |
|--------|---------|------|
| `finetune_functiongemma_egyptian.py` | Fine-tune on Egyptian data | 2-4 hours |
| `evaluate_egyptian_accuracy.py` | Evaluate model accuracy | 30 min |

### Download Models

```bash
# Download FunctionGemma model
./ml/finetune/scripts/download_functiongemma_model.sh

# Download Whisper model
./ml/finetune/scripts/download_whisper_model.sh

# Convert to GGUF format
./ml/finetune/scripts/convert_to_gguf.sh
```

| Script | Model | Size | Time |
|--------|-------|------|------|
| `download_functiongemma_model.sh` | FunctionGemma-270M | 288MB | 5 min |
| `download_whisper_model.sh` | Whisper Egyptian | 500MB | 10 min |
| `convert_to_gguf.sh` | GGUF conversion | - | 15 min |

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
├── 📱 android/               ← Android application module
│   ├── src/main/java/        ← Source code
│   ├── src/main/cpp/         ← Native code
│   ├── src/main/assets/      ← Assets & YAML workflows
│   ├── src/test/             ← Unit tests
│   └── src/androidTest/      ← Instrumented tests
│
├── 🚀 deploy/                ← Build, deploy & device tooling
│   ├── build/                ← Build scripts & Gradle helper
│   ├── android/              ← SDK/device setup (Windows PS + bash)
│   ├── scripts-deploy/       ← Deployment scripts
│   ├── hf/                   ← HuggingFace dataset upload tools
│   └── device/               ← Device-side utilities
│
├── 🧠 ml/                    ← ML pipeline (fine-tuning, data, prompts)
│   ├── requirements.txt      ← Python dependencies
│   ├── prompts/              ← Generation/system prompts
│   └── finetune/             ← Fine-tuning scripts, configs, datasets
│
├── 🤖 agents/                ← Agent definitions
└── 🔧 external/              ← External dependencies (submodules)
```

---

## 🎯 Key Components

### AI Engines

| Engine | Parameters | Size | Purpose |
|--------|------------|------|---------|
| **FunctionGemma-270M-IT** | 270M | 288MB | Primary intent classification |
| **Llama 3.2 3B** | 3B | 2GB | Fallback engine (optional) |
| **Whisper Egyptian ASR** | 244M | 500MB | Speech-to-text |

### Core Services

| Service | Purpose | Status |
|---------|---------|--------|
| **VoiceService** | Main voice recognition service | ✅ Active |
| **WakeWordDetector** | "يا صاحبي" and "يا كبير" detection | ✅ Active |
| **TTSManager** | Text-to-speech with Egyptian dialect | ✅ Active |
| **EmergencyService** | Fall detection and emergency response | ✅ Active |

### Key Features

| Feature | Description | Status |
|---------|-------------|--------|
| **Egyptian Dialect Support** | 95%+ accuracy | ✅ Production |
| **Senior Mode** | Slower, louder audio | ✅ Production |
| **Offline Operation** | 100% local processing | ✅ Production |
| **System-level Access** | Works even when screen locked | ✅ Production |

---

## 📞 Support & Help

### Getting Help

| Step | Action | Link |
|------|--------|------|
| 1 | Check Documentation | [docs/](docs/) |
| 2 | Troubleshooting | [docs/guides/TROUBLESHOOTING.md](docs/guides/TROUBLESHOOTING.md) |
| 3 | User Manual | [docs/guides/user_manual_ar.md](docs/guides/user_manual_ar.md) |
| 4 | Open Issue | [GitHub Issues](https://github.com/Kandil7/EgyptianAgent/issues) |

### Contact Information

| Channel | Details |
|---------|---------|
| **Project Lead** | EgyptianAgent Team |
| **GitHub** | [Kandil7/EgyptianAgent](https://github.com/Kandil7/EgyptianAgent) |
| **Issues** | [GitHub Issues](https://github.com/Kandil7/EgyptianAgent/issues) |
| **Email** | support@egyptianagent.com |

---

## 📊 Project Status

### Current Version

| Attribute | Value |
|-----------|-------|
| **Version** | 2.0.0 (FunctionGemma) |
| **Build** | FunctionGemma-enabled |
| **Status** | ✅ Production Ready |
| **Last Updated** | 2026-03-03 |

### Key Metrics

| Metric | Value | Target | Status |
|--------|-------|--------|--------|
| **Accuracy** | 95.2% | 95%+ | ✅ Pass |
| **Load Time** | 4.8s | <5s | ✅ Pass |
| **Inference** | 350ms | <500ms | ✅ Pass |
| **Memory** | 550MB | <600MB | ✅ Pass |
| **Battery/Hour** | 3% | <5% | ✅ Pass |

### Release History

| Version | Date | Key Changes |
|---------|------|-------------|
| **2.0.0** | 2026-03-03 | FunctionGemma integration |
| **1.1.0** | 2026-02-15 | Senior mode improvements |
| **1.0.0** | 2026-01-12 | Initial release |

---

## 🎓 Learning Path

### For New Developers

| Step | Resource | Time |
|------|----------|------|
| 1 | [Architecture Overview](docs/architecture/ARCHITECTURE.md) | 15 min |
| 2 | [API Reference](docs/api/API_REFERENCE.md) | 30 min |
| 3 | [FunctionGemma Integration](docs/integration/FUNCTIONGEMMA_INTEGRATION.md) | 30 min |
| 4 | [Run Tests](ml/finetune/scripts/) | 1 hour |
| 5 | Start contributing! | - |

### For End Users

| Step | Resource | Time |
|------|----------|------|
| 1 | [User Manual (Arabic)](docs/guides/user_manual_ar.md) | 10 min |
| 2 | [Quick Start](docs/FUNCTIONGEMMA_QUICKSTART.md) | 5 min |
| 3 | [Troubleshooting](docs/guides/TROUBLESHOOTING.md) | As needed |
| 4 | Enjoy using EgyptianAgent! | - |

### For Contributors

| Step | Resource | Time |
|------|----------|------|
| 1 | [Contributing Guide](CONTRIBUTING.md) | 10 min |
| 2 | [Coding Standards](CONTRIBUTING.md#coding-standards) | 10 min |
| 3 | [Test Plan](docs/testing/FUNCTIONGEMMA_TEST_PLAN.md) | 15 min |
| 4 | Make your first contribution! | - |

---

## 🔗 External Resources

### Dependencies

| Dependency | Repository | License |
|------------|------------|---------|
| **llama.cpp** | [GitHub](https://github.com/ggerganov/llama.cpp) | MIT |
| **whisper.cpp** | [GitHub](https://github.com/ggerganov/whisper.cpp) | MIT |
| **FunctionGemma** | [HuggingFace](https://huggingface.co/google/functiongemma-270m-it) | Gemma |

### Tools

| Tool | Purpose | Link |
|------|---------|------|
| **Android Studio** | IDE | [Download](https://developer.android.com/studio) |
| **Gradle** | Build system | [Documentation](https://docs.gradle.org/) |
| **Python** | Scripts & fine-tuning | [Download](https://www.python.org/) |
| **ADB** | Device debugging | [Setup](https://developer.android.com/tools/adb) |

---

## 📝 Quick Reference Card

### Intent Types Supported

| Intent | Description | Example |
|--------|-------------|---------|
| `CALL_CONTACT` | Make phone calls | "اتصل بماما" |
| `SEND_WHATSAPP` | Send WhatsApp messages | "ابعت واتساب لأحمد" |
| `SET_ALARM` | Set alarms/reminders | "نبهني بكرة الصبح" |
| `READ_TIME` | Read current time | "الساعة كام؟" |
| `EMERGENCY` | Emergency situations | "يا نجدة" |
| `OPEN_APP` | Open applications | "افتح الواتساب" |
| `DEVICE_CONTROL` | Control device settings | "افتح الواي فاي" |

### Egyptian Commands Examples

| Command | Translation | Intent |
|---------|-------------|--------|
| "اتصل بماما" | Call mom | CALL_CONTACT |
| "ابعت واتساب لأحمد" | Send WhatsApp to Ahmed | SEND_WHATSAPP |
| "نبهني بكرة الصبح" | Wake me up tomorrow morning | SET_ALARM |
| "الساعة كام؟" | What time is it? | READ_TIME |
| "يا نجدة" | Emergency! | EMERGENCY |

### Wake Words

| Wake Word | Mode | Translation |
|-----------|------|-------------|
| "يا صاحبي" | Standard | Oh my friend |
| "يا كبير" | Senior | Oh elder (respectful) |

---

## ✅ Checklists

### First-Time Setup

- [ ] Install Android Studio
- [ ] Install Python 3.8+
- [ ] Clone repository
- [ ] Initialize submodules: `./deploy/scripts-deploy/initialize_submodules.sh`
- [ ] Download models: `./ml/finetune/scripts/download_functiongemma_model.sh`
- [ ] Build project: `./deploy/build/scripts/build_functiongemma.sh`
- [ ] Deploy to device: `./deploy/scripts-deploy/deploy_functiongemma.sh`

### Daily Development

- [ ] Pull latest changes
- [ ] Run tests: `./ml/finetune/scripts/run_functiongemma_tests.sh --unit`
- [ ] Build: `./deploy/build/scripts/build.sh`
- [ ] Deploy: `adb install -r android/build/outputs/apk/debug/*.apk`

### Before Commit

- [ ] Run all tests
- [ ] Check code style
- [ ] Update documentation if needed
- [ ] Add tests for new features
- [ ] Commit with clear message

### Pre-Release

- [ ] All tests passing (95%+)
- [ ] Performance benchmarks met
- [ ] Documentation updated
- [ ] Release notes written
- [ ] Version code/name updated
- [ ] Git tag created

---

## 📖 Document Navigation

| Need | Document | Location |
|------|----------|----------|
| **What is this?** | README.md | Root |
| **How to build?** | deploy/build/scripts/README.md | deploy/build/scripts/ |
| **How to deploy?** | docs/deployment/DEPLOYMENT_GUIDE.md | docs/deployment/ |
| **Architecture?** | docs/architecture/ARCHITECTURE.md | docs/architecture/ |
| **API docs?** | docs/api/API_REFERENCE.md | docs/api/ |
| **User guide?** | docs/guides/user_manual_ar.md | docs/guides/ |
| **Troubleshooting?** | docs/guides/TROUBLESHOOTING.md | docs/guides/ |
| **Testing?** | docs/testing/FUNCTIONGEMMA_TEST_PLAN.md | docs/testing/ |
| **Performance?** | docs/performance/FUNCTIONGEMMA_PERFORMANCE_BENCHMARKS.md | docs/performance/ |

---

## 📋 Documentation Maintenance

### Review Schedule

| Document | Review Frequency | Next Review |
|----------|------------------|-------------|
| README.md | Monthly | 2026-04-03 |
| INDEX.md | Monthly | 2026-04-03 |
| Architecture docs | Quarterly | 2026-06-03 |
| API Reference | Per release | Next release |
| User guides | Quarterly | 2026-06-03 |

### Maintainers

| Role | Responsibility |
|------|----------------|
| **Technical Lead** | Architecture, API docs |
| **Documentation Lead** | User guides, troubleshooting |
| **QA Lead** | Test documentation |
| **Release Manager** | Release notes |

---

**Last Updated:** 2026-03-03  
**Maintained By:** EgyptianAgent Team  
**Next Review:** 2026-04-03  
**Document Version:** 2.0.0
