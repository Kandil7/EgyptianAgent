# Egyptian Agent - Complete Setup and Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Development Environment Setup](#development-environment-setup)
4. [Model Conversion Process](#model-conversion-process)
5. [Android Application Setup](#android-application-setup)
6. [Building the Application](#building-the-application)
7. [Device Preparation](#device-preparation)
8. [Installation Process](#installation-process)
9. [Post-Installation Configuration](#post-installation-configuration)
10. [Testing and Validation](#testing-and-validation)
11. [Troubleshooting](#troubleshooting)

## Overview

The Egyptian Agent is a revolutionary voice-controlled assistant designed specifically for Egyptian seniors and visually impaired users. It operates completely hands-free using voice commands in Egyptian dialect and runs as a system app on Honor X6c devices. The application features advanced AI capabilities with Llama 3.2 3B Q4_K_M model for local processing, ensuring complete privacy and offline functionality.

### Key Features
- Voice-only interaction - no screen touch required
- Senior Mode with slower, louder audio and automatic fall detection
- Smart Emergencies with automatic connection to emergency services
- Simple commands that understand Egyptian dialect
- Offline operation for all core features
- System-level access even when screen is locked
- Medication reminders and guardian notifications
- Advanced Egyptian dialect normalization
- Contact management with automatic addition capabilities
- Location services for emergency situations
- Accessibility features for visually impaired users

### Target Device
- **Primary Device**: Honor X6c (MediaTek Helio G81 Ultra, 6GB RAM)
- **OS Requirements**: Android 12+
- **Special Requirements**: Bootloader unlocked + Root (Magisk)

## Prerequisites

### Hardware Requirements
- Honor X6c device (MediaTek Helio G81 Ultra, 6GB RAM)
- Computer with at least 8GB RAM for building
- USB cable for device connection

### Software Requirements
- Android Studio (latest version)
- Android SDK with build tools
- ADB (Android Debug Bridge)
- Git
- Python 3.11+
- Java JDK 11+

### Account Requirements
- Hugging Face account for downloading the Llama 3.2 3B model
- Device with unlocked bootloader and root access

## Development Environment Setup

### 1. Clone the Repository
```bash
git clone https://github.com/Kandil7/EgyptianAgent.git
cd EgyptianAgent
```

### 2. Install Required Tools
```bash
# Install Python dependencies
pip install torch==2.4.1 transformers==4.51.3 huggingface-hub==0.25.1 sentencepiece protobuf accelerate

# Install Git LFS if not already installed
git lfs install
```

### 3. Set Up Android Development Environment
- Install Android Studio
- Install Android SDK Platform 34
- Install Android SDK Build-Tools
- Install Android NDK (r26c or later)
- Install Android SDK Command-line Tools

### 4. Configure Environment Variables
```bash
# Set ANDROID_HOME to your Android SDK path
export ANDROID_HOME=/path/to/android/sdk

# Set ANDROID_NDK to your NDK path
export ANDROID_NDK=/path/to/android/ndk
```

## Model Conversion Process

### 1. Download and Convert the Llama 3.2 3B Model

Run the automated conversion script:
```bash
cd scripts
chmod +x convert_llama_model.sh
./convert_llama_model.sh
```

This script will:
- Clone the llama.cpp repository
- Build llama.cpp for your system
- Download the Llama 3.2 3B Instruct model from Hugging Face
- Convert the model to GGUF F16 format
- Quantize the model to Q4_K_M format (optimized for mobile)
- Move the quantized model to the app's assets directory
- Create model metadata

### 2. Manual Conversion Steps (Alternative)

If you prefer to run the steps manually:

#### Clone and Build llama.cpp
```bash
git clone https://github.com/ggerganov/llama.cpp
cd llama.cpp
git checkout b3842f  # Using commit from Jan 11, 2026
make clean && make -j LLAMA_CUBLAS=0
```

#### Download the Model
```bash
huggingface-cli download meta-llama/Llama-3.2-3B-Instruct \
  --local-dir llama-3.2-3b --local-dir-use-symlinks False
```

#### Convert to GGUF F16
```bash
python convert_hf_to_gguf.py llama-3.2-3b \
  --outfile llama-3.2-3b-f16.gguf \
  --outtype f16 \
  --vocab-type bpe \
  --model-type instruct
```

#### Quantize to Q4_K_M
```bash
./llama-quantize llama-3.2-3b-f16.gguf \
  llama-3.2-3b-Q4_K_M.gguf Q4_K_M \
  --update-ctx llama-3.2-3b-Q4_K_M.gguf
```

#### Move to Assets Directory
```bash
mkdir -p ../app/src/main/assets/model/llama-3.2-3b-q4_k_m
cp llama-3.2-3b-Q4_K_M.gguf ../app/src/main/assets/model/llama-3.2-3b-q4_k_m/
```

## Android Application Setup

### 1. Project Structure
The project follows this structure:
```
EgyptianAgent/
├── app/
│   ├── src/main/
│   │   ├── java/com/egyptian/agent/
│   │   │   ├── core/
│   │   │   ├── stt/
│   │   │   ├── executors/
│   │   │   ├── accessibility/
│   │   │   ├── hybrid/
│   │   │   ├── utils/
│   │   │   └── performance/
│   │   ├── assets/
│   │   │   ├── model/           # Contains the Llama model
│   │   │   ├── senior_voices/
│   │   │   └── emergency_audio/
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── scripts/
├── documentation/
└── build.gradle
```

### 2. JNI and Native Libraries
The application uses JNI to interface with the Llama model through native C++ code:
- Located in `app/src/main/cpp/`
- Uses CMake for building native libraries
- Interfaces with llama.cpp for model inference

### 3. Model Integration
The Llama model is integrated through:
- `LlamaNative.java` - Java JNI wrapper
- `LlamaModelIntegration.java` - Integration with the existing architecture
- `OpenPhoneIntegration.java` - Updated to use Llama as primary model with fallback

## Building the Application

### 1. Build Using Gradle
```bash
# Build the release version
./gradlew assembleRelease

# Build the debug version
./gradlew assembleDebug
```

### 2. Build Using the Provided Script
```bash
chmod +x build.sh
./build.sh --release --target honor-x6c --clean
```

### 3. Verify the Build
After building, the APK will be located at:
```
app/build/outputs/apk/release/EgyptianAgent-release.apk
```

## Device Preparation

### 1. Enable Developer Options
- Go to Settings > About Phone
- Tap "Build Number" 7 times to enable Developer Options

### 2. Enable USB Debugging
- Go to Settings > Developer Options
- Enable "USB Debugging"

### 3. Unlock Bootloader
```bash
# Reboot to bootloader
adb reboot bootloader

# Unlock the bootloader (this will factory reset the device)
fastboot oem unlock
```

### 4. Install Magisk for Root Access
- Download the latest Magisk APK
- Install Magisk on your device
- Reboot to recovery
- Flash the Magisk patched boot image
- Reboot the device

## Installation Process

### 1. Push APK to Device
```bash
adb push app/build/outputs/apk/release/EgyptianAgent-release.apk /sdcard/
```

### 2. Install as System App
```bash
# Gain root access
adb shell su

# Create system app directory
mkdir -p /system/priv-app/EgyptianAgent

# Copy APK to system directory
cp /sdcard/EgyptianAgent-release.apk /system/priv-app/EgyptianAgent/

# Set proper permissions
chmod 644 /system/priv-app/EgyptianAgent/EgyptianAgent-release.apk
```

### 3. Alternative: Use Installation Script
```bash
chmod +x scripts/install_as_system_app.sh
adb push scripts/install_as_system_app.sh /sdcard/
adb shell su -c "sh /sdcard/install_as_system_app.sh"
```

### 4. Apply Honor Battery Fixes
```bash
adb push scripts/honor_battery_fix.sh /sdcard/
adb shell su -c "sh /sdcard/honor_battery_fix.sh"
```

### 5. Reboot the Device
```bash
adb reboot
```

## Post-Installation Configuration

### 1. Grant Permissions
After installation and reboot:
- Open the app once to initialize
- Go to Settings > Apps > Egyptian Agent > Permissions
- Grant all requested permissions:
  - Phone (for calls)
  - Contacts (for contact access)
  - Microphone (for voice recognition)
  - Body Sensors (for fall detection)
  - Location (for emergency services)
  - SMS (for emergency notifications)

### 2. Set as Device Administrator
- Go to Settings > Security > Device Administrators
- Enable Egyptian Agent

### 3. Configure Emergency Contacts
- Open the Egyptian Agent app
- Navigate to Settings > Emergency
- Add emergency contacts

### 4. Configure Senior Mode (Optional)
- Activate senior mode by saying "يا كبير، شغل وضع كبار السن"
- Or through the app settings

## Testing and Validation

### 1. Basic Functionality Test
- Say "يا صاحبي" to activate the assistant
- Test basic commands:
  - "اتصل بأمي" (Call mother)
  - "قولي الوقت" (Tell time)
  - "نبهني بكرة الصبح" (Set alarm for tomorrow morning)

### 2. Senior Mode Test
- Activate senior mode
- Test senior-specific commands
- Verify slower, louder audio

### 3. Emergency Features Test
- Test emergency activation with "يا نجدة"
- Verify emergency contact calling
- Test fall detection (if applicable)

### 4. Performance Validation
- Monitor app performance and battery usage
- Verify that the Llama model loads correctly
- Check response times for voice commands

## Troubleshooting

### Common Issues and Solutions

#### Issue: App crashes on startup
**Solution:**
- Check if the device has sufficient RAM (6GB+ recommended)
- Verify the model file is correctly placed in assets
- Check logcat for specific error messages

#### Issue: Voice commands not recognized
**Solution:**
- Ensure microphone permissions are granted
- Check if another app is using the microphone
- Verify the Vosk model is correctly loaded

#### Issue: Model fails to load
**Solution:**
- Verify the Llama model file exists in assets/model/
- Check if the device has enough storage space
- Ensure the model file is not corrupted

#### Issue: App doesn't work as system app
**Solution:**
- Verify the APK is placed in /system/priv-app/
- Check file permissions are set to 644
- Confirm the device is rooted

#### Issue: Battery drain
**Solution:**
- Check if background services are running unnecessarily
- Verify the app is not constantly waking the device
- Review the honor_battery_fix.sh script

### Diagnostic Commands
```bash
# Check if service is running
adb shell dumpsys activity services | grep VoiceService

# View logs
adb logcat -s "VoiceService:EgyptianNormalizer:SeniorMode:EmergencyHandler:LlamaNative"

# Check permissions
adb shell dumpsys package com.egyptian.agent | grep permission

# Check memory usage
adb shell dumpsys meminfo com.egyptian.agent
```

### Recovery Procedures
If the app causes system instability:
1. Boot into recovery mode
2. Uninstall the app from system partition
3. Clear cache partition
4. Reboot normally

## Maintenance and Updates

### Updating the Model
To update the Llama model:
1. Follow the model conversion process again
2. Replace the model file in assets/model/
3. Rebuild the application
4. Reinstall as system app

### Updating the Application
- Pull the latest code from the repository
- Make any necessary adjustments
- Rebuild and reinstall following the installation process

---

## Support

For technical support, contact the development team through the in-app feedback system or emergency guardian notification feature.

For development inquiries, refer to the technical documentation in the `documentation/` directory.