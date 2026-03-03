# EgyptianAgent Scripts

This directory contains all project scripts organized by function.

## Script Categories

### 🔨 Build Scripts (`build/`)
- `build.sh` - Main build script
- `build_production.sh` - Production build script
- `build_functiongemma.sh` - FunctionGemma build script
- `build_native_libs.sh` - Native library build script

### 🚀 Deploy Scripts (`deploy/`)
- `deploy_production.sh` - Production deployment script
- `deploy_functiongemma.sh` - FunctionGemma deployment script
- `initialize_submodules.sh` - Git submodules initialization

### 🤖 Model Scripts (`model/`)
- `download_functiongemma_model.sh` - Download FunctionGemma model
- `download_whisper_model.sh` - Download Whisper model
- `convert_to_gguf.sh` - Convert model to GGUF format
- `convert_llama_model.sh` - Convert Llama model
- `setup_llama_model.sh` - Setup Llama model

### 🧪 Test Scripts (`test/`)
- `run_functiongemma_tests.sh` - Run FunctionGemma test suite
- `test_integration.sh` - Run integration tests

### 🎯 Finetune Scripts (`finetune/`)
- `finetune_functiongemma_egyptian.py` - Egyptian dialect finetuning
- `evaluate_egyptian_accuracy.py` - Accuracy evaluation script

### 🛠️ Utility Scripts (`utils/`)
- `verify_implementation.sh` - Implementation verification
- `fetch_models.sh` - Fetch all required models
- `security_audit.sh` - Security audit script
- `honor_battery_fix.sh` - Honor device battery optimization fix
- `install_as_system_app.sh` - Install as system application
- `complete_build.sh` - Complete build process

## Usage

All scripts should be executed from the project root:

```bash
# Build the project
./scripts/build/build.sh

# Deploy to production
./scripts/deploy/deploy_production.sh

# Run tests
./scripts/test/run_functiongemma_tests.sh

# Download models
./scripts/model/download_functiongemma_model.sh
```

## Making Scripts Executable

If scripts are not executable, run:
```bash
chmod +x scripts/**/*.sh
```
