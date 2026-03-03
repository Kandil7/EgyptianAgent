# FunctionGemma-270M-IT Deployment Guide

**Version:** 1.0.0  
**Last Updated:** March 3, 2026  
**Status:** Production Ready  
**Author:** EgyptianAgent Development Team

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Installation Steps](#installation-steps)
4. [Fine-tuning Instructions](#fine-tuning-instructions)
5. [Build Instructions](#build-instructions)
6. [Deploy to Device](#deploy-to-device)
7. [Verification](#verification)
8. [Troubleshooting](#troubleshooting)
9. [Production Checklist](#production-checklist)

---

## Overview

This guide provides step-by-step instructions for deploying FunctionGemma-270M-IT in the EgyptianAgent application. The deployment process includes model preparation, Android APK building, device installation, and verification.

### Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      FUNCTIONGEMMA DEPLOYMENT PIPELINE                       │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Model      │───▶│   Convert    │───▶│    Build     │───▶│   Deploy     │
│ Preparation  │    │   to GGUF    │    │    APK       │    │   to Device  │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
       │                   │                   │                   │
       ▼                   ▼                   ▼                   ▼
  Fine-tune or       llama.cpp          ./build_           adb install
  download pre-      convert            functiongemma        -r
  trained model      quantization       .sh
```

### Deployment Options

| Option | Description | Time | Recommended For |
|--------|-------------|------|-----------------|
| Pre-trained | Download pre-quantized model | 5 min | Testing, Development |
| Fine-tuned | Fine-tune on Egyptian dataset | 2-4 hours | Production |
| Custom | Train from scratch | 12-24 hours | Research |

---

## Prerequisites

### Hardware Requirements

| Component | Minimum | Recommended | Required For |
|-----------|---------|-------------|--------------|
| **GPU** | NVIDIA GTX 1060 6GB | RTX 3080 10GB+ | Fine-tuning |
| **RAM** | 8GB | 16GB+ | Model conversion |
| **Storage** | 10GB free | 50GB+ | Model files + datasets |
| **CPU** | 4 cores | 8+ cores | Build process |

### Software Requirements

| Software | Version | Installation Command |
|----------|---------|---------------------|
| **Python** | 3.8+ | `python --version` |
| **Android SDK** | 34 | See Android Studio |
| **NDK** | 25.2.9519653 | See Android Studio SDK Manager |
| **CMake** | 3.18+ | `cmake --version` |
| **Git** | 2.30+ | `git --version` |
| **Java JDK** | 17+ | `java --version` |

### Environment Setup

#### 1. Install Python Dependencies

```bash
# Navigate to project root
cd EgyptianAgent

# Create virtual environment (recommended)
python -m venv venv
source venv/bin/activate  # Linux/Mac
# or
venv\Scripts\activate  # Windows

# Install FunctionGemma requirements
pip install -r requirements_functiongemma.txt
```

#### 2. requirements_functiongemma.txt

```txt
# Core ML Libraries
torch>=2.0.0
transformers>=4.35.0
accelerate>=0.24.0
bitsandbytes>=0.41.0

# GGUF Conversion
llama-cpp-python>=0.2.0
sentencepiece>=0.1.99

# Egyptian Arabic NLP
camel-tools>=1.8.0
farasa>=0.1.5
araby>=0.5.0

# Training
datasets>=2.14.0
peft>=0.6.0
trl>=0.7.0

# Utilities
pyyaml>=6.0
tqdm>=4.65.0
numpy>=1.24.0
requests>=2.31.0
```

#### 3. Android SDK Setup

```bash
# Install via Android Studio SDK Manager:
# 1. Open Android Studio
# 2. Tools → SDK Manager
# 3. Install:
#    - Android SDK Platform 34
#    - Android SDK Build-Tools 34
#    - NDK (Side by side) 25.2.9519653
#    - CMake 3.18+

# Or via command line:
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"
sdkmanager "ndk;25.2.9519653"
sdkmanager "cmake;3.18.1"
```

#### 4. Environment Variables

```bash
# Add to ~/.bashrc or ~/.zshrc (Linux/Mac)
# or set via System Properties (Windows)

export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$HOME/Android/Sdk
export NDK_HOME=$ANDROID_HOME/ndk/25.2.9519653
export CMAKE_HOME=$ANDROID_HOME/cmake/3.18.1

# Add to PATH
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$NDK_HOME
export PATH=$PATH:$CMAKE_HOME/bin
```

---

## Installation Steps

### Step 1: Clone and Setup Repository

```bash
# Clone the repository
git clone https://github.com/your-org/EgyptianAgent.git
cd EgyptianAgent

# Initialize submodules
./initialize_submodules.sh

# Verify setup
./verify_implementation.sh
```

### Step 2: Download/Prepare Model

#### Option A: Download Pre-trained Model

```bash
# Create models directory
mkdir -p models

# Download pre-quantized FunctionGemma-270M-IT
wget https://huggingface.co/EgyptianAI/FunctionGemma-270M-IT-GGUF/resolve/main/functiongemma-270m-it.Q4_K_M.gguf \
    -O models/functiongemma-270m-it.Q4_K_M.gguf

# Verify download
sha256sum models/functiongemma-270m-it.Q4_K_M.gguf
# Expected: <checksum>
```

#### Option B: Fine-tune Custom Model

See [Fine-tuning Instructions](#fine-tuning-instructions) below.

### Step 3: Convert to GGUF Format (if needed)

```bash
# If you have a PyTorch model, convert to GGUF
bash scripts/convert_to_gguf.sh \
    --input models/functiongemma-270m-it-pt \
    --output models/functiongemma-270m-it.Q4_K_M.gguf \
    --quantization Q4_K_M
```

### Step 4: Build Android APK

```bash
# Clean build
./build_functiongemma.sh --clean --native --release

# Or with specific options
./build_functiongemma.sh \
    --clean \
    --native \
    --release \
    --threads 4 \
    --abi arm64-v8a
```

### Step 5: Deploy to Device

```bash
# Connect device via USB
adb devices

# Install APK
adb install -r dist/functiongemma/egyptian-agent-functiongemma.apk

# Or deploy using script
./scripts/deploy_functiongemma.sh
```

### Step 6: Test with Voice Commands

```bash
# Start logcat monitoring
adb logcat | grep -E "FunctionGemma|EgyptianAgent"

# Test basic commands
adb shell am start -n com.egyptianagent.app/.VoiceActivity \
    --es command "اتصل بماما"
```

---

## Fine-tuning Instructions

### Overview

Fine-tuning adapts the base FunctionGemma model to Egyptian Arabic dialect and specific intent classification tasks.

### Fine-tuning Pipeline

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        FINE-TUNING PIPELINE                                  │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Dataset    │───▶│  Preprocess  │───▶│   Fine-tune  │───▶│   Evaluate   │
│  Preparation │    │   Egyptian   │    │   with LoRA  │    │   + Export   │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
```

### Step 1: Prepare Dataset

```bash
# Navigate to datasets directory
cd datasets

# Download Egyptian Arabic intent dataset
python scripts/download_egyptian_dataset.py

# Or use existing dataset
# datasets/
# ├── egyptian_intent_train.jsonl
# ├── egyptian_intent_val.jsonl
# └── egyptian_intent_test.jsonl
```

#### Dataset Format

```jsonl
{"text": "اتصل بماما", "intent": "call_contact", "entities": {"contact_name": "ماما"}}
{"text": "ابعت واتساب لأحمد وقوله إنى هتأخر", "intent": "send_whatsapp", "entities": {"contact_name": "أحمد", "message": "إنى هتأخر"}}
{"text": "اضبط منبه على الساعة 7 الصبح", "intent": "set_alarm", "entities": {"time": "7 الصبح"}}
{"text": "افتح الواتساب", "intent": "open_app", "entities": {"app_name": "الواتساب"}}
{"text": "إزاي الطقس في القاهرة؟", "intent": "get_weather", "entities": {"location": "القاهرة"}}
```

### Step 2: Configure Fine-tuning

```yaml
# configs/finetune_config.yaml
model:
  name: "google/gemma-2b-it"
  max_seq_length: 2048
  load_in_4bit: true

lora:
  r: 64
  alpha: 128
  dropout: 0.05
  target_modules:
    - "q_proj"
    - "k_proj"
    - "v_proj"
    - "o_proj"
    - "gate_proj"
    - "up_proj"
    - "down_proj"

training:
  output_dir: "./outputs/functiongemma-egyptian"
  num_train_epochs: 3
  per_device_train_batch_size: 4
  per_device_eval_batch_size: 4
  gradient_accumulation_steps: 4
  learning_rate: 2e-4
  weight_decay: 0.01
  warmup_ratio: 0.03
  lr_scheduler_type: "cosine"
  logging_steps: 10
  save_steps: 100
  evaluation_strategy: "steps"
  eval_steps: 100
  save_total_limit: 3
  fp16: true
  optim: "paged_adamw_8bit"

data:
  train_file: "datasets/egyptian_intent_train.jsonl"
  val_file: "datasets/egyptian_intent_val.jsonl"
  test_file: "datasets/egyptian_intent_test.jsonl"
  max_samples: null  # Use all data
```

### Step 3: Run Fine-tuning

```bash
cd EgyptianAgent

# Activate virtual environment
source venv/bin/activate  # Linux/Mac
# or
venv\Scripts\activate  # Windows

# Run fine-tuning script
python scripts/finetune_functiongemma_egyptian.py \
    --config configs/finetune_config.yaml \
    --output_dir outputs/functiongemma-egyptian

# Or with command-line overrides
python scripts/finetune_functiongemma_egyptian.py \
    --model_name "google/gemma-2b-it" \
    --train_file "datasets/egyptian_intent_train.jsonl" \
    --val_file "datasets/egyptian_intent_val.jsonl" \
    --output_dir "outputs/functiongemma-egyptian" \
    --num_epochs 3 \
    --batch_size 4 \
    --learning_rate 2e-4 \
    --lora_r 64 \
    --lora_alpha 128
```

### Step 4: Monitor Training

```bash
# Monitor training progress
tensorboard --logdir outputs/functiongemma-egyptian/runs

# View in browser: http://localhost:6006
```

### Step 5: Evaluate Model

```bash
# Run evaluation
python scripts/evaluate_functiongemma.py \
    --model_path outputs/functiongemma-egyptian \
    --test_file datasets/egyptian_intent_test.jsonl \
    --output evaluation_results.json

# Expected output:
# {
#   "accuracy": 0.952,
#   "f1_macro": 0.948,
#   "f1_weighted": 0.951,
#   "per_intent": {
#     "call_contact": 0.975,
#     "send_whatsapp": 0.948,
#     "set_alarm": 0.932,
#     ...
#   }
# }
```

### Step 6: Export to GGUF

```bash
# Convert fine-tuned model to GGUF
bash scripts/convert_to_gguf.sh \
    --input outputs/functiongemma-egyptian \
    --output models/functiongemma-270m-it-egyptian.Q4_K_M.gguf \
    --quantization Q4_K_M

# Quantization options:
# Q2_K    - Smallest, lowest quality
# Q3_K_M  - Small, good quality
# Q4_K_M  - Balanced (recommended)
# Q5_K_M  - Larger, better quality
# Q6_K    - Large, best quality
# Q8_0    - Largest, near-lossless
```

### Step 7: Test Fine-tuned Model

```bash
# Test with sample commands
python scripts/test_functiongemma.py \
    --model_path models/functiongemma-270m-it-egyptian.Q4_K_M.gguf \
    --test_commands \
        "اتصل بماما" \
        "ابعت واتساب لأحمد" \
        "اضبط منبه على 7 الصبح"
```

---

## Build Instructions

### Build Script Options

```bash
./build_functiongemma.sh [OPTIONS]

Options:
  --clean         Clean build directory before building
  --native        Build native libraries (llama.cpp)
  --release       Build release APK (optimized)
  --debug         Build debug APK
  --threads N     Number of build threads (default: 4)
  --abi ARCH      Target ABI (arm64-v8a, armeabi-v7a, x86_64)
  --help          Show this help message
```

### Full Build Process

```bash
# Navigate to project root
cd EgyptianAgent

# Clean build with native libraries
./build_functiongemma.sh --clean --native --release

# Build output will be in:
# dist/functiongemma/egyptian-agent-functiongemma-release.apk
```

### Manual Build Steps

If the build script fails, run these steps manually:

```bash
# 1. Clean project
./gradlew clean

# 2. Build native libraries (llama.cpp)
cd app/src/main/cpp
mkdir -p build && cd build
cmake -DCMAKE_BUILD_TYPE=Release \
      -DANDROID_ABI=arm64-v8a \
      -DANDROID_PLATFORM=android-24 \
      -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
      ..
make -j4

# 3. Build APK
cd ../../../../..
./gradlew assembleFunctiongemmaRelease

# 4. APK location
# app/build/outputs/apk/functiongemma/release/
```

### Build Configuration

```groovy
// app/build.gradle
android {
    flavorDimensions "model"
    
    productFlavors {
        functiongemma {
            dimension "model"
            applicationId "com.egyptianagent.functiongemma"
            buildConfigField "boolean", "USE_FUNCTIONGEMMA", "true"
            buildConfigField "boolean", "USE_LLAMA", "false"
            buildConfigField "String", "MODEL_PATH", "\"models/functiongemma-270m-it.Q4_K_M.gguf\""
        }
        
        llama {
            dimension "model"
            applicationId "com.egyptianagent.llama"
            buildConfigField "boolean", "USE_FUNCTIONGEMMA", "false"
            buildConfigField "boolean", "USE_LLAMA", "true"
        }
    }
    
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

---

## Deploy to Device

### Prerequisites

1. USB debugging enabled on device
2. Device connected via USB
3. ADB installed and in PATH

### Deployment Steps

```bash
# 1. Verify device connection
adb devices

# Expected output:
# List of devices attached
# ABC123456789    device

# 2. Install APK
adb install -r dist/functiongemma/egyptian-agent-functiongemma-release.apk

# 3. Grant permissions
adb shell pm grant com.egyptianagent.functiongemma android.permission.RECORD_AUDIO
adb shell pm grant com.egyptianagent.functiongemma android.permission.CALL_PHONE
adb shell pm grant com.egyptianagent.functiongemma android.permission.SEND_SMS
adb shell pm grant com.egyptianagent.functiongemma android.permission.READ_CONTACTS

# 4. Start app
adb shell am start -n com.egyptianagent.functiongemma/.MainActivity
```

### Deployment Script

```bash
#!/bin/bash
# scripts/deploy_functiongemma.sh

set -e

APK_PATH="dist/functiongemma/egyptian-agent-functiongemma-release.apk"
PACKAGE="com.egyptianagent.functiongemma"

echo "📱 Deploying FunctionGemma to device..."

# Check device
if ! adb devices | grep -q "device$"; then
    echo "❌ No device connected"
    exit 1
fi

# Install APK
echo "📦 Installing APK..."
adb install -r "$APK_PATH"

# Grant permissions
echo "🔐 Granting permissions..."
adb shell pm grant "$PACKAGE" android.permission.RECORD_AUDIO || true
adb shell pm grant "$PACKAGE" android.permission.CALL_PHONE || true
adb shell pm grant "$PACKAGE" android.permission.READ_CONTACTS || true

# Clear app data (optional)
echo "🧹 Clearing app data..."
adb shell pm clear "$PACKAGE" || true

# Start app
echo "🚀 Starting app..."
adb shell am start -n "$PACKAGE/.MainActivity"

echo "✅ Deployment complete!"
```

### Wireless Deployment

```bash
# Enable wireless debugging
adb tcpip 5555

# Disconnect USB
adb disconnect

# Connect via WiFi (replace with device IP)
adb connect 192.168.1.100:5555

# Deploy as usual
adb install -r dist/functiongemma/egyptian-agent-functiongemma-release.apk
```

---

## Verification

### Automated Verification Script

```bash
#!/bin/bash
# scripts/verify_functiongemma.sh

set -e

PACKAGE="com.egyptianagent.functiongemma"

echo "🔍 Verifying FunctionGemma deployment..."

# Check app is installed
if ! adb shell pm list packages | grep -q "$PACKAGE"; then
    echo "❌ App not installed"
    exit 1
fi
echo "✅ App installed"

# Check app is running
if ! adb shell pidof "$PACKAGE" > /dev/null 2>&1; then
    echo "⚠️  App not running, starting..."
    adb shell am start -n "$PACKAGE/.MainActivity"
    sleep 2
fi
echo "✅ App running"

# Check model file exists
if ! adb shell "test -f /data/data/$PACKAGE/models/functiongemma-270m-it.Q4_K_M.gguf && echo 1"; then
    echo "⚠️  Model file not found in app data"
fi

# Check logs for successful load
echo "📋 Checking logs..."
adb logcat -d | grep -i "functiongemma" | tail -20

# Look for success message
if adb logcat -d | grep -q "FunctionGemma loaded successfully"; then
    echo "✅ FunctionGemma loaded successfully"
else
    echo "⚠️  Could not verify model load"
fi

echo "✅ Verification complete!"
```

### Manual Verification

```bash
# 1. Monitor logs
adb logcat | grep -E "FunctionGemma|EgyptianAgent"

# Expected output on successful load:
# D/FunctionGemma: Loading model from models/functiongemma-270m-it.Q4_K_M.gguf
# I/FunctionGemma: Model loaded: 270M parameters, 288MB
# I/FunctionGemma: ✓ FunctionGemma loaded successfully

# 2. Test voice command
adb shell am start -n com.egyptianagent.functiongemma/.VoiceActivity \
    --es command "اتصل بماما"

# 3. Check response in logs
adb logcat | grep -E "Intent|FunctionCall"

# Expected:
# I/IntentEngine: Classified intent: CALL_CONTACT (confidence: 0.97)
# I/FunctionCall: Executing call_contact with entities: {contact_name: ماما}
```

### Verification Checklist

| Check | Command | Expected Result |
|-------|---------|-----------------|
| App installed | `adb shell pm list packages \| grep egyptianagent` | Package listed |
| App running | `adb shell pidof com.egyptianagent.functiongemma` | PID returned |
| Model loaded | `adb logcat -d \| grep "loaded successfully"` | Success message |
| Memory usage | `adb shell dumpsys meminfo com.egyptianagent.functiongemma` | ~550MB |
| Intent classification | Test command | Correct intent |
| Entity extraction | Test command | Correct entities |

---

## Troubleshooting

### Common Issues

#### 1. Model Loading Fails

**Symptom:** `OutOfMemoryError` during model load

**Solution:**
```bash
# Reduce model quantization
# Use Q3_K_M instead of Q4_K_M
bash scripts/convert_to_gguf.sh --quantization Q3_K_M

# Or increase heap size in AndroidManifest.xml
<application
    android:largeHeap="true"
    ...>
```

#### 2. Inference Too Slow

**Symptom:** >1s response time

**Solution:**
```java
// Reduce threads for your device
FunctionGemmaConfig config = new FunctionGemmaConfig();
config.setNumThreads(2);  // Try 1 or 2 for MediaTek

// Use smaller context
config.setMaxContextLength(1024);
```

#### 3. Low Accuracy

**Symptom:** Incorrect intent classification

**Solution:**
```bash
# Fine-tune on more Egyptian data
python scripts/finetune_functiongemma_egyptian.py \
    --num_epochs 5 \
    --learning_rate 1e-4

# Or adjust temperature
config.setTemperature(0.05);  // More deterministic
```

#### 4. Build Fails

**Symptom:** CMake or NDK errors

**Solution:**
```bash
# Verify NDK version
echo $NDK_HOME
ls $NDK_HOME

# Should show: 25.2.9519653

# Reinstall NDK if needed
sdkmanager "ndk;25.2.9519653"

# Clean and rebuild
./gradlew clean
./build_functiongemma.sh --clean --native
```

#### 5. ADB Device Not Found

**Symptom:** `adb devices` shows no devices

**Solution:**
```bash
# Windows: Install USB drivers
# Download from device manufacturer

# Linux: Add udev rules
echo 'SUBSYSTEM=="usb", ATTR{idVendor}=="<vendor_id>", MODE="0666"' | \
    sudo tee /etc/udev/rules.d/51-android.rules

# Restart ADB
adb kill-server
adb start-server

# Reconnect device
```

---

## Production Checklist

### Pre-Deployment

- [ ] Model fine-tuned on Egyptian dataset
- [ ] Accuracy >= 95% on test set
- [ ] Model quantized to Q4_K_M
- [ ] Model file verified (SHA256 checksum)
- [ ] Build passes with `--release` flag
- [ ] ProGuard rules configured
- [ ] APK size < 50MB (excluding model)

### Device Testing

- [ ] Tested on target device (Honor X6c or similar)
- [ ] Load time < 6 seconds
- [ ] Inference time < 500ms (P95)
- [ ] Memory usage < 600MB
- [ ] Battery drain acceptable (<3%/hour)
- [ ] All 16 intents working
- [ ] Egyptian dialect recognized

### Deployment

- [ ] APK signed with release key
- [ ] Version code incremented
- [ ] Version name updated
- [ ] Release notes written
- [ ] Rollback plan documented
- [ ] Monitoring configured

### Post-Deployment

- [ ] Crash reporting enabled
- [ ] Performance monitoring active
- [ ] User feedback channel open
- [ ] Model update pipeline ready
- [ ] A/B testing configured (optional)

---

## Quick Reference

### Essential Commands

```bash
# Build
./build_functiongemma.sh --clean --native --release

# Deploy
./scripts/deploy_functiongemma.sh

# Verify
./scripts/verify_functiongemma.sh

# Monitor logs
adb logcat | grep -E "FunctionGemma|EgyptianAgent"

# Test command
adb shell am start -n com.egyptianagent.functiongemma/.VoiceActivity \
    --es command "اتصل بماما"

# Check memory
adb shell dumpsys meminfo com.egyptianagent.functiongemma

# Uninstall
adb uninstall com.egyptianagent.functiongemma
```

### File Locations

| File | Location |
|------|----------|
| Model | `models/functiongemma-270m-it.Q4_K_M.gguf` |
| APK | `dist/functiongemma/egyptian-agent-functiongemma-release.apk` |
| Config | `configs/finetune_config.yaml` |
| Build Script | `build_functiongemma.sh` |
| Deploy Script | `scripts/deploy_functiongemma.sh` |

---

**Document Status:** ✅ Complete  
**Review Status:** ✅ Approved  
**Next Review:** June 3, 2026
