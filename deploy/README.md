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
- `verify_deployment.sh` - Deployment verification (Bash/Linux/macOS)
- `verify_deployment.ps1` - Deployment verification (PowerShell/Windows) ⭐ NEW

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
- `init_gradle_wrapper.ps1` - Initialize Gradle wrapper (Windows) ⭐ NEW

### 🪟 Windows Setup (`setup/`) ⭐ NEW
- `windows_setup.ps1` - Automated Windows development environment setup

## Usage

### Windows (PowerShell)

```powershell
# Setup development environment
.\scripts\setup\windows_setup.ps1 -Auto

# Initialize Gradle wrapper
.\scripts\utils\init_gradle_wrapper.ps1

# Build the project
.\gradlew.bat assembleDebug

# Deploy to production
.\scripts\deploy\deploy_production.sh

# Run verification
.\scripts\deploy\verify_deployment.ps1
```

### Linux/macOS (Bash)

```bash
# Build the project
./deploy/build/scripts/build.sh

# Deploy to production
./deploy/scripts-deploy/deploy_production.sh

# Run tests
./scripts/test/run_functiongemma_tests.sh

# Download models
./ml/finetune/scripts/download_functiongemma_model.sh

# Run verification
./deploy/scripts-deploy/verify_deployment.sh
```

## Making Scripts Executable

If scripts are not executable, run:
```bash
chmod +x scripts/**/*.sh
```

## Windows Setup Guide

For Windows developers, see:
- [Windows Quick Start Guide](../docs/deployment/WINDOWS_QUICKSTART.md)
- [Deployment Prerequisites](../docs/deployment/DEPLOYMENT_PREREQUISITES.md)

## Script Execution Policy (Windows)

If you encounter execution policy errors on Windows:

```powershell
# For current session only
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

# Or allow signed scripts
Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
```
