# EgyptianAgent Production Deployment Guide

**Version:** 1.1.0  
**Target Device:** Honor X6c (MediaTek Helio G81 Ultra, 6GB RAM, Android 12)  
**Last Updated:** March 2, 2026

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Build Configuration](#build-configuration)
3. [Building the Application](#building-the-application)
4. [APK Signing](#apk-signing)
5. [Deployment Options](#deployment-options)
6. [System App Installation](#system-app-installation)
7. [Magisk Module Installation](#magisk-module-installation)
8. [Verification](#verification)
9. [Rollback Procedure](#rollback-procedure)
10. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Development Environment

| Tool | Version | Installation |
|------|---------|--------------|
| JDK | 17+ | [Adoptium](https://adoptium.net/) |
| Android SDK | API 34 | Android Studio |
| NDK | 25.2.9519653 | SDK Manager |
| CMake | 3.22.1+ | SDK Manager |
| Git | 2.30+ | [git-scm.com](https://git-scm.com/) |

### Device Requirements

| Requirement | Status |
|-------------|--------|
| Honor X6c (or compatible) | Required |
| Android 12+ | Required |
| Root access (Magisk) | Required for system app |
| USB Debugging enabled | Required |
| Minimum 6GB RAM | Required |

### Environment Setup

```bash
# Verify Java installation
java -version  # Should show JDK 17+

# Verify Android SDK
echo $ANDROID_HOME  # Should point to SDK location

# Verify ADB
adb version

# Verify NDK
$ANDROID_HOME/ndk/25.2.9519653/source.properties
```

---

## Build Configuration

### gradle.properties (Recommended Settings)

```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.enableR8.fullMode=true
android.ndkVersion=25.2.9519653
```

### Signing Configuration

For production builds, create a release keystore:

```bash
# Generate release keystore
keytool -genkey -v -keystore keystore/release.keystore \
  -alias egyptian_agent \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass <STORE_PASSWORD> \
  -keypass <KEY_PASSWORD> \
  -dname "CN=Your Name, OU=Your Org, O=Your Company, L=City, ST=State, C=EG"
```

Store credentials securely and use environment variables:

```bash
export KEYSTORE_PASSWORD="your_store_password"
export KEY_ALIAS="egyptian_agent"
export KEY_PASSWORD="your_key_password"
```

---

## Building the Application

### Quick Build (Debug)

```bash
# Initialize submodules (first time only)
./initialize_submodules.sh

# Build debug APK
./build.sh --debug --install
```

### Production Build (Release)

```bash
# Clean build with release signing
./build.sh --release --clean

# Or using Gradle directly
./gradlew clean assembleRelease
```

### Native Build (with llama.cpp)

```bash
# Initialize native libraries
./initialize_submodules.sh

# Build with native LLM support
./build.sh --release --native
```

### Build Output Locations

| Build Type | Output Path |
|------------|-------------|
| Debug | `app/build/outputs/apk/debug/` |
| Release | `app/build/outputs/apk/release/` |
| ABI-specific | `app/build/outputs/apk/release/` |

---

## APK Signing

### Verify Signature

```bash
# Verify APK signature
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk

# Check signature details
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

### Expected Output

```
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
```

---

## Deployment Options

### Option 1: Regular App Installation

```bash
# Install as regular user app
adb install -r app/build/outputs/apk/release/app-release.apk

# Grant permissions
adb shell pm grant com.egyptian.agent android.permission.RECORD_AUDIO
adb shell pm grant com.egyptian.agent android.permission.CALL_PHONE
adb shell pm grant com.egyptian.agent android.permission.READ_CONTACTS
```

**Limitations:**
- Limited background execution
- May be killed by battery optimization
- Some system features unavailable

### Option 2: System App Installation (Recommended)

See [System App Installation](#system-app-installation) below.

### Option 3: Magisk Module

See [Magisk Module Installation](#magisk-module-installation) below.

---

## System App Installation

### Prerequisites

- Rooted device with Magisk
- ADB installed and working
- System partition writable (or Magisk)

### Installation Steps

```bash
#!/bin/bash
# deploy_system_app.sh

APK_PATH="app/build/outputs/apk/release/app-release.apk"
SYSTEM_DIR="/system/priv-app/EgyptianAgent"

# Push APK to device
adb push "$APK_PATH" /sdcard/EgyptianAgent.apk

# Create system directory
adb shell su -c "mkdir -p $SYSTEM_DIR"

# Copy APK to system
adb shell su -c "cp /sdcard/EgyptianAgent.apk $SYSTEM_DIR/EgyptianAgent.apk"

# Set permissions
adb shell su -c "chmod 644 $SYSTEM_DIR/EgyptianAgent.apk"
adb shell su -c "chown root:root $SYSTEM_DIR/EgyptianAgent.apk"

# Reboot
adb reboot
```

### Post-Installation

After reboot, verify installation:

```bash
# Check if app is installed as system app
adb shell dumpsys package com.egyptian.agent | grep -i "system"

# Expected: "systemDir=/system/priv-app/EgyptianAgent"

# Grant runtime permissions
adb shell pm grant com.egyptian.agent android.permission.RECORD_AUDIO
adb shell pm grant com.egyptian.agent android.permission.CALL_PHONE
adb shell pm grant com.egyptian.agent android.permission.READ_CONTACTS
adb shell pm grant com.egyptian.agent android.permission.BODY_SENSORS
```

### Battery Optimization Exemption

```bash
# Disable battery optimization
adb shell dumpsys deviceidle whitelist +com.egyptian.agent

# Allow background activity
adb shell cmd appops set com.egyptian.agent RUN_IN_BACKGROUND allow
adb shell cmd appops set com.egyptian.agent WAKE_LOCK allow
```

---

## Magisk Module Installation

### Create Magisk Module

```bash
# Create module structure
mkdir -p egyptian_agent_magisk/system/priv-app/EgyptianAgent
mkdir -p egyptian_agent_magisk/META-INF/com/google/android

# Copy APK
cp app/build/outputs/apk/release/app-release.apk \
   egyptian_agent_magisk/system/priv-app/EgyptianAgent/EgyptianAgent.apk

# Create update-binary
cat > egyptian_agent_magisk/META-INF/com/google/android/update-binary << 'EOF'
#!/sbin/sh
umask 022
cp /data/local/tmp/EgyptianAgent.apk /system/priv-app/EgyptianAgent/
chmod 644 /system/priv-app/EgyptianAgent/EgyptianAgent.apk
set_perm_recursive 0 0 0755 0644 /system/priv-app/EgyptianAgent
EOF

# Create updater-script
echo "#MAGISK" > egyptian_agent_magisk/META-INF/com/google/android/updater-script

# Create module.prop
cat > egyptian_agent_magisk/module.prop << 'EOF'
id=egyptian_agent
name=EgyptianAgent System App
version=1.1.0
versionCode=10100
author=EgyptianAgent Team
description=Voice-controlled assistant for Egyptian seniors
EOF

# Package module
cd egyptian_agent_magisk
zip -r ../EgyptianAgent-magisk-v1.1.0.zip *
```

### Install Magisk Module

```bash
# Push module to device
adb push EgyptianAgent-magisk-v1.1.0.zip /sdcard/

# Install via Magisk
adb shell su -c "magisk --install-module /sdcard/EgyptianAgent-magisk-v1.1.0.zip"

# Or use Magisk app UI
# Open Magisk → Modules → Install from storage → Select ZIP

# Reboot
adb reboot
```

---

## Verification

### Installation Verification

```bash
# Check package installation
adb shell pm list packages | grep egyptian.agent

# Check system app status
adb shell dumpsys package com.egyptian.agent | grep -E "versionName|systemDir|enabled"

# Check permissions
adb shell dumpsys package com.egyptian.agent | grep -A 20 "granted=true"
```

### Functionality Verification

```bash
# Start the app
adb shell am start -n com.egyptian.agent/.ui.MainActivity

# Check service status
adb shell dumpsys activity services | grep -i egyptian

# Test voice service
adb shell am startservice \
  -n com.egyptian.agent/.core.VoiceService \
  -a com.egyptian.agent.action.START_VOICE_SERVICE
```

### Log Monitoring

```bash
# Monitor app logs
adb logcat -s EgyptianAgent:* LlamaNative:* EgyptWhisper:*

# Check for errors
adb logcat -s EgyptianAgent:* | grep -i error
```

---

## Rollback Procedure

### If Installation Fails

```bash
# Boot to safe mode (if device bootloop)
adb reboot safe

# Remove system app
adb shell su -c "rm -rf /system/priv-app/EgyptianAgent"

# Reboot
adb reboot
```

### Rollback to Previous Version

```bash
# Uninstall current version
adb shell pm uninstall com.egyptian.agent

# Remove system installation
adb shell su -c "rm -rf /system/priv-app/EgyptianAgent"

# Install previous version
adb install -r EgyptianAgent-previous-version.apk

# Reboot
adb reboot
```

### Magisk Module Removal

```bash
# Remove via Magisk
adb shell su -c "magisk --remove-module egyptian_agent"

# Or manually
adb shell su -c "rm -rf /data/adb/modules/egyptian_agent"

# Reboot
adb reboot
```

---

## Troubleshooting

### Common Build Errors

| Error | Solution |
|-------|----------|
| `NDK not found` | Set `android.ndkVersion` in build.gradle |
| `CMake not found` | Install CMake via Android SDK Manager |
| `Signing failed` | Check keystore path and passwords |
| `Out of memory` | Increase `org.gradle.jvmargs` |
| `Submodule not found` | Run `./initialize_submodules.sh` |

### Common Installation Errors

| Error | Solution |
|-------|----------|
| `INSTALL_FAILED_SYSTEM_SIZE` | Remove old version first |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Uninstall, then install |
| `Permission denied` | Ensure root access |
| `App keeps crashing` | Check logcat for errors |
| `Voice service not starting` | Grant microphone permission |

### Device-Specific Issues (Honor X6c)

| Issue | Solution |
|-------|----------|
| Aggressive battery optimization | Add to whitelist |
| Background service killed | Set as system app |
| Microphone access denied | Grant permission manually |
| Boot animation stuck | Remove system app, reboot |

### Debug Commands

```bash
# Check root access
adb shell su -c "id"

# Check system partition
adb shell mount | grep system

# Check available space
adb shell df -h /system

# Check app installation
adb shell pm path com.egyptian.agent

# Force stop app
adb shell am force-stop com.egyptian.agent

# Clear app data
adb shell pm clear com.egyptian.agent
```

---

## Release Checklist

- [ ] All tests passing
- [ ] APK signed with release key
- [ ] APK signature verified
- [ ] Version code/name updated
- [ ] CHANGELOG.md updated
- [ ] Git tag created (v1.1.0)
- [ ] Release notes written
- [ ] APK uploaded to GitHub Releases
- [ ] Documentation updated
- [ ] Team notified

---

## Contact & Support

For issues or questions:
- Check existing issues on GitHub
- Review logs with `adb logcat`
- Consult ARCHITECTURE.md for system design

---

*This document is part of the EgyptianAgent project documentation.*
