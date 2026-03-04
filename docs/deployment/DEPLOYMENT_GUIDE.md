# EgyptianAgent Deployment Guide

**Version:** 2.0.0  
**Last Updated:** 2026-03-03  
**Status:** ✅ Production Ready

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Build Configuration](#build-configuration)
3. [Building the Application](#building-the-application)
4. [APK Signing](#apk-signing)
5. [Deployment Options](#deployment-options)
6. [System App Installation](#system-app-installation)
7. [Verification](#verification)
8. [Rollback Procedure](#rollback-procedure)
9. [Troubleshooting](#troubleshooting)
10. [Post-Deployment Checks](#post-deployment-checks)

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

---

## Build Configuration

### gradle.properties

```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.enableR8.fullMode=true
android.ndkVersion=25.2.9519653
```

### Signing Configuration

```bash
# Generate release keystore
keytool -genkey -v -keystore keystore/release.keystore \
  -alias egyptian_agent \
  -keyalg RSA -keysize 2048 -validity 10000
```

---

## Building the Application

### Quick Build Commands

```bash
# Debug build
./scripts/build/build.sh --debug

# Release build
./scripts/build/build_production.sh --release

# FunctionGemma build (recommended)
./scripts/build/build_functiongemma.sh --release --native
```

### Build Output Locations

| Build Type | Output Path |
|------------|-------------|
| Debug | `app/build/outputs/apk/debug/` |
| Release | `app/build/outputs/apk/release/` |

---

## APK Signing

### Verify Signature

```bash
apksigner verify --verbose app/build/outputs/apk/release/*.apk
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
adb install -r app/build/outputs/apk/release/*.apk
```

**Limitations:** Limited background execution, may be killed by battery optimization

### Option 2: System App Installation (Recommended)

See [System App Installation](#system-app-installation) below.

---

## System App Installation

### Installation Steps

```bash
#!/bin/bash
APK_PATH="app/build/outputs/apk/release/*.apk"
SYSTEM_DIR="/system/priv-app/EgyptianAgent"

# Push APK to device
adb push "$APK_PATH" /sdcard/EgyptianAgent.apk

# Create system directory
adb shell su -c "mkdir -p $SYSTEM_DIR"

# Copy APK to system
adb shell su -c "cp /sdcard/EgyptianAgent.apk $SYSTEM_DIR/EgyptianAgent.apk"

# Set permissions
adb shell su -c "chmod 644 $SYSTEM_DIR/EgyptianAgent.apk"

# Reboot
adb reboot
```

### Grant Permissions

```bash
adb shell pm grant com.egyptian.agent android.permission.RECORD_AUDIO
adb shell pm grant com.egyptian.agent android.permission.CALL_PHONE
adb shell pm grant com.egyptian.agent android.permission.READ_CONTACTS
adb shell pm grant com.egyptian.agent android.permission.BODY_SENSORS
```

### Battery Optimization Exemption

```bash
adb shell dumpsys deviceidle whitelist +com.egyptian.agent
adb shell cmd appops set com.egyptian.agent RUN_IN_BACKGROUND allow
```

---

## Verification

### Installation Verification

```bash
# Check package installation
adb shell pm list packages | grep egyptian.agent

# Check system app status
adb shell dumpsys package com.egyptian.agent | grep -E "versionName|systemDir"

# Check permissions
adb shell dumpsys package com.egyptian.agent | grep -A 20 "granted=true"
```

### Functionality Verification

```bash
# Start the app
adb shell am start -n com.egyptian.agent/.ui.MainActivity

# Check service status
adb shell dumpsys activity services | grep -i egyptian

# Monitor logs
adb logcat -s EgyptianAgent:* FunctionGemma:*
```

---

## Rollback Procedure

### If Installation Fails

```bash
# Boot to safe mode
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

# Install previous version
adb install -r EgyptianAgent-previous-version.apk
```

---

## Troubleshooting

### Common Build Errors

| Error | Solution |
|-------|----------|
| `NDK not found` | Install NDK 25.2.9519653 |
| `CMake not found` | Install CMake via SDK Manager |
| `Signing failed` | Check keystore path and passwords |
| `Out of memory` | Increase `org.gradle.jvmargs` |

### Common Installation Errors

| Error | Solution |
|-------|----------|
| `INSTALL_FAILED_SYSTEM_SIZE` | Remove old version first |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Uninstall, then install |
| `Permission denied` | Ensure root access |
| `App keeps crashing` | Check logcat for errors |

---

## Post-Deployment Checks

### Verification Checklist

- [ ] App installed successfully
- [ ] All permissions granted
- [ ] Voice service starts automatically
- [ ] Wake word detection working
- [ ] Model loads successfully (<6s)
- [ ] Commands processed correctly
- [ ] Battery optimization disabled
- [ ] No crashes in first hour

### Monitoring Commands

```bash
# Check app status
adb shell dumpsys package com.egyptian.agent

# Monitor logs
adb logcat -s EgyptianAgent:* | grep -i error

# Check memory usage
adb shell dumpsys meminfo com.egyptian.agent
```

---

**Document Version:** 2.0.0  
**Last Updated:** 2026-03-03  
**Next Review:** 2026-06-03
