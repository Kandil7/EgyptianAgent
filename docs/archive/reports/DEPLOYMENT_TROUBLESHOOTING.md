# EgyptianAgent Hybrid Architecture - Deployment Troubleshooting Guide

**Version:** 3.0.0-hybrid  
**Last Updated:** March 14, 2026  
**Target Device:** Honor X6c (Android 12+)

---

## Quick Reference

| Issue | Quick Fix | Page |
|-------|-----------|------|
| Build fails | Check dependencies, clean build | [Build Issues](#build-issues) |
| ADB not found | Install ADB, add to PATH | [ADB Issues](#adb-issues) |
| Device not detected | Enable USB debugging | [Device Issues](#device-issues) |
| APK install fails | Uninstall old version | [Installation Issues](#installation-issues) |
| Model not loading | Check storage, re-download | [Model Issues](#model-issues) |
| Tests failing | Check device connection | [Test Issues](#test-issues) |
| App crashes | Check logs, permissions | [Runtime Issues](#runtime-issues) |
| Slow performance | Clear cache, restart | [Performance Issues](#performance-issues) |

---

## Build Issues

### Issue: Gradle Build Fails

**Symptoms:**
```
> Task :app:compileDebugKotlin FAILED
Execution failed for task ':app:compileDebugKotlin'.
```

**Possible Causes:**
1. Missing dependencies
2. Kotlin version mismatch
3. Corrupted Gradle cache

**Solutions:**

#### Solution 1: Clean and Rebuild
```bash
# Clean build
./gradlew.bat clean

# Delete Gradle cache
rm -rf ~/.gradle/caches/

# Rebuild
./gradlew.bat assembleDebug
```

#### Solution 2: Check Dependencies
```bash
# Verify dependencies
./gradlew.bat app:dependencies

# Refresh dependencies
./gradlew.bat --refresh-dependencies
```

#### Solution 3: Update Gradle
```bash
# Update Gradle wrapper
./gradlew.bat wrapper --gradle-version 8.2

# Rebuild
./gradlew.bat assembleDebug
```

---

### Issue: Dependency Resolution Failed

**Symptoms:**
```
Could not resolve org.yaml:snakeyaml:2.3
Could not get resource 'https://repo.maven.apache.org/maven2/org/yaml/snakeyaml/2.3/snakeyaml-2.3.pom'
```

**Possible Causes:**
1. Network connectivity issues
2. Repository not accessible
3. Version doesn't exist

**Solutions:**

#### Solution 1: Check Network
```bash
# Test Maven Central access
curl -I https://repo.maven.apache.org/maven2/

# Test Google repository
curl -I https://dl.google.com/dl/android/maven2/
```

#### Solution 2: Clear Gradle Cache
```bash
# Windows
rmdir /s /q %USERPROFILE%\.gradle\caches

# Rebuild
./gradlew.bat assembleDebug --refresh-dependencies
```

#### Solution 3: Use Alternative Repository
```gradle
// In build.gradle
repositories {
    mavenCentral()
    google()
    maven { url 'https://jitpack.io' }  // Fallback
}
```

---

### Issue: Native Build Fails (CMake)

**Symptoms:**
```
CMake Error at CMakeLists.txt:XX
ninja: build failed: subcommand failed
```

**Possible Causes:**
1. CMake not installed
2. NDK version mismatch
3. Native code compilation errors

**Solutions:**

#### Solution 1: Install CMake
```bash
# Via Android Studio
Tools → SDK Manager → SDK Tools → CMake (install)

# Or via command line
sdkmanager "cmake;3.22.1"
```

#### Solution 2: Verify NDK
```bash
# Check NDK version
$ANDROID_HOME/ndk/25.2.9519653/source.properties

# Install correct NDK
sdkmanager "ndk;25.2.9519653"
```

#### Solution 3: Disable Native Build (if not needed)
```gradle
// In app/build.gradle
android {
    defaultConfig {
        externalNativeBuild {
            cmake {
                arguments "-DUSE_LLAMA_CPP=OFF"
                arguments "-DUSE_WHISPER=OFF"
            }
        }
    }
}
```

---

## ADB Issues

### Issue: ADB Command Not Found

**Symptoms:**
```
'adb' is not recognized as an internal or external command
```

**Possible Causes:**
1. ADB not installed
2. ADB not in PATH

**Solutions:**

#### Solution 1: Install ADB
```bash
# Download Platform Tools
# https://developer.android.com/studio/releases/platform-tools

# Extract to C:\platform-tools

# Add to PATH
setx PATH "%PATH%;C:\platform-tools"
```

#### Solution 2: Verify Installation
```bash
# Check ADB version
adb version

# Expected output:
Android Debug Bridge version 1.0.41
Version 34.0.5
```

---

### Issue: ADB Device Not Authorized

**Symptoms:**
```
List of devices attached
HONORX6C1234567890    unauthorized
```

**Possible Causes:**
1. USB debugging not enabled
2. RSA key not accepted

**Solutions:**

#### Solution 1: Enable USB Debugging
```
On Device:
1. Settings → About Phone
2. Tap "Build Number" 7 times
3. Settings → System → Developer Options
4. Enable "USB Debugging"
5. Reconnect USB
```

#### Solution 2: Accept RSA Key
```
On Device:
1. Disconnect and reconnect USB
2. When prompted "Allow USB debugging?"
3. Check "Always allow from this computer"
4. Tap "OK"
```

#### Solution 3: Restart ADB Server
```bash
# Kill ADB server
adb kill-server

# Start ADB server
adb start-server

# List devices
adb devices
```

---

## Device Issues

### Issue: No Device Detected

**Symptoms:**
```
$ adb devices
List of devices attached
```

**Possible Causes:**
1. USB cable not connected
2. USB driver not installed
3. Device in charging mode only

**Solutions:**

#### Solution 1: Check USB Connection
```bash
# Try different USB port
# Try different USB cable
# Ensure cable supports data transfer (not charging only)
```

#### Solution 2: Install USB Driver (Windows)
```
1. Download Honor USB Driver
2. Install driver
3. Device Manager → Update Driver
4. Select "Honor X6c"
```

#### Solution 3: Change USB Mode
```
On Device:
1. Pull down notification shade
2. Tap "Charging via USB"
3. Select "File Transfer" or "MTP"
```

---

### Issue: Insufficient Storage

**Symptoms:**
```
Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]
```

**Possible Causes:**
1. Device storage full
2. Model files taking space

**Solutions:**

#### Solution 1: Check Storage
```bash
# Check available storage
adb shell df -h

# Check app storage
adb shell dumpsys package com.egyptian.agent | grep "codeSize"
```

#### Solution 2: Free Up Space
```bash
# Clear app cache
adb shell pm clear com.egyptian.agent.debug

# Clear download cache
adb shell rm -rf /sdcard/Download/*

# Uninstall unused apps
adb uninstall <package_name>
```

#### Solution 3: Move Models to SD Card
```bash
# Create directory on SD card
adb shell mkdir -p /sdcard/EgyptianAgent/models

# Move models
adb shell mv /data/data/com.egyptian.agent/models/* /sdcard/EgyptianAgent/models/
```

---

## Installation Issues

### Issue: APK Installation Failed

**Symptoms:**
```
Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]
```

**Possible Causes:**
1. Existing installation with different signature
2. Version code conflict

**Solutions:**

#### Solution 1: Uninstall Old Version
```bash
# Uninstall existing app
adb uninstall com.egyptian.agent

# Or with data
adb uninstall com.egyptian.agent.debug

# Install new version
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Solution 2: Install with Replace
```bash
# Force install with replace
adb install -r -d app/build/outputs/apk/debug/app-debug.apk
```

#### Solution 3: Check Signature
```bash
# Verify APK signature
apksigner verify app/build/outputs/apk/debug/app-debug.apk

# If signature mismatch, rebuild with same keystore
```

---

### Issue: Permission Denied

**Symptoms:**
```
Security exception: Permission denied
```

**Possible Causes:**
1. Permissions not granted
2. Special permissions required

**Solutions:**

#### Solution 1: Grant All Permissions
```bash
# Grant all required permissions
adb shell pm grant com.egyptian.agent.debug android.permission.RECORD_AUDIO
adb shell pm grant com.egyptian.agent.debug android.permission.CALL_PHONE
adb shell pm grant com.egyptian.agent.debug android.permission.READ_CONTACTS
adb shell pm grant com.egyptian.agent.debug android.permission.WRITE_CONTACTS
adb shell pm grant com.egyptian.agent.debug android.permission.SEND_SMS
adb shell pm grant com.egyptian.agent.debug android.permission.BODY_SENSORS
adb shell pm grant com.egyptian.agent.debug android.permission.FOREGROUND_SERVICE
adb shell pm grant com.egyptian.agent.debug android.permission.WAKE_LOCK

# Grant special permissions
adb shell appops set com.egyptian.agent.debug SYSTEM_ALERT_WINDOW allow
adb shell dumpsys deviceidle whitelist +com.egyptian.agent.debug
```

#### Solution 2: Verify Permissions
```bash
# Check granted permissions
adb shell dumpsys package com.egyptian.agent.debug | grep -A 20 "granted=true"
```

---

### Issue: Accessibility Service Not Enabled

**Symptoms:**
```
AccessibilityService not available
```

**Possible Causes:**
1. Service not enabled in settings
2. Service crashed

**Solutions:**

#### Solution 1: Enable Accessibility Service
```
On Device:
1. Settings → Accessibility
2. Find "EgyptianAgent Accessibility Service"
3. Toggle ON
4. Confirm "Allow"
```

#### Solution 2: Enable via ADB
```bash
# Enable accessibility service
adb shell settings put secure enabled_accessibility_services com.egyptian.agent.debug/.accessibility.EgyptianAgentAccessibilityService

# Verify
adb shell settings get secure enabled_accessibility_services
```

#### Solution 3: Restart Service
```bash
# Force stop app
adb shell am force-stop com.egyptian.agent.debug

# Restart app
adb shell am start -n com.egyptian.agent.debug/.VoiceActivity
```

---

## Model Issues

### Issue: Model Not Found

**Symptoms:**
```
Model file not found: /sdcard/EgyptianAgent/models/functiongemma-270m-it.gguf
```

**Possible Causes:**
1. Model not downloaded
2. Model path incorrect
3. File permissions issue

**Solutions:**

#### Solution 1: Download Model
```bash
# Run model download script
./scripts/model/download_functiongemma_model.sh

# Or manually download
# https://huggingface.co/egyptian-agent/functiongemma-270m-it
```

#### Solution 2: Verify Model Location
```bash
# Check model directory
adb shell ls -la /sdcard/EgyptianAgent/models/

# Expected output:
# -rw-r--r-- 1 root root 288M functiongemma-270m-it.gguf
# -rw-r--r-- 1 root root 100M whisper-small.en.gguf
```

#### Solution 3: Fix Permissions
```bash
# Set correct permissions
adb shell chmod 644 /sdcard/EgyptianAgent/models/*.gguf

# Verify
adb shell ls -la /sdcard/EgyptianAgent/models/
```

---

### Issue: Model Load Failed

**Symptoms:**
```
Failed to load model: GGML_ASSERT failed
```

**Possible Causes:**
1. Corrupted model file
2. Incompatible model format
3. Insufficient memory

**Solutions:**

#### Solution 1: Verify Model Integrity
```bash
# Check MD5 checksum
adb shell md5sum /sdcard/EgyptianAgent/models/functiongemma-270m-it.gguf

# Expected: a3f2b8c9d1e4f5a6b7c8d9e0f1a2b3c4
```

#### Solution 2: Re-download Model
```bash
# Delete corrupted model
adb shell rm /sdcard/EgyptianAgent/models/functiongemma-270m-it.gguf

# Re-download
./scripts/model/download_functiongemma_model.sh
```

#### Solution 3: Check Memory
```bash
# Check available memory
adb shell dumpsys meminfo | grep "Total RAM"

# Required: >1GB free
```

---

## Test Issues

### Issue: Unit Tests Fail

**Symptoms:**
```
> Task :testDebugUnitTest FAILED

com.egyptian.agent.hybrid.HybridOrchestratorTest
  ✗ RoutingDecision for FAST_PATH with high confidence
```

**Possible Causes:**
1. Test environment not set up
2. Mock not configured correctly
3. Code changed, test outdated

**Solutions:**

#### Solution 1: Clean Test Cache
```bash
# Clean test results
./gradlew.bat cleanTest

# Rebuild tests
./gradlew.bat testDebugUnitTest --rerun-tasks
```

#### Solution 2: Check Test Configuration
```gradle
// In app/build.gradle
testOptions {
    unitTests {
        includeAndroidResources = true
        returnDefaultValues = true
    }
}
```

#### Solution 3: Run Single Test
```bash
# Run specific test class
./gradlew.bat testDebugUnitTest --tests "com.egyptian.agent.hybrid.HybridOrchestratorTest"

# Run specific test method
./gradlew.bat testDebugUnitTest --tests "com.egyptian.agent.hybrid.HybridOrchestratorTest.RoutingDecision_for_FAST_PATH"
```

---

### Issue: Integration Tests Fail

**Symptoms:**
```
> Task :connectedDebugAndroidTest FAILED

com.egyptian.agent.hybrid.UINavigationIntegrationTest
  ✗ facebookNewsFeedNavigation
```

**Possible Causes:**
1. Device not connected
2. App not installed
3. UI element changed

**Solutions:**

#### Solution 1: Verify Device Connection
```bash
# Check device
adb devices

# Ensure device is listed as "device" not "unauthorized"
```

#### Solution 2: Reinstall App
```bash
# Uninstall
adb uninstall com.egyptian.agent.debug

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Run tests
./gradlew.bat connectedAndroidTest
```

#### Solution 3: Update UI Selectors
```kotlin
// In test file, update element selectors
// Old: onView(withId(R.id.old_id))
// New: onView(withId(R.id.new_id))
```

---

## Runtime Issues

### Issue: App Crashes on Start

**Symptoms:**
```
Process: com.egyptian.agent.debug, PID: 12345
java.lang.RuntimeException: Unable to start activity
```

**Possible Causes:**
1. Missing permissions
2. Model not loaded
3. Null pointer exception

**Solutions:**

#### Solution 1: Check Crash Logs
```bash
# Get crash logs
adb logcat -d > crash_log.txt

# Filter for crashes
adb logcat -s "EgyptianAgent" "*:F"
```

#### Solution 2: Clear App Data
```bash
# Clear app data
adb shell pm clear com.egyptian.agent.debug

# Restart app
adb shell am start -n com.egyptian.agent.debug/.VoiceActivity
```

#### Solution 3: Check Permissions
```bash
# Check all permissions
adb shell dumpsys package com.egyptian.agent.debug | grep "granted"
```

---

### Issue: Voice Recognition Not Working

**Symptoms:**
```
Speech recognition failed
No speech input detected
```

**Possible Causes:**
1. Microphone permission not granted
2. Whisper model not loaded
3. Audio hardware issue

**Solutions:**

#### Solution 1: Grant Microphone Permission
```bash
# Grant permission
adb shell pm grant com.egyptian.agent.debug android.permission.RECORD_AUDIO

# Verify
adb shell appops get com.egyptian.agent.debug RECORD_AUDIO
```

#### Solution 2: Check Whisper Model
```bash
# Verify model exists
adb shell ls -lh /sdcard/EgyptianAgent/models/whisper*.gguf

# Expected: ~100MB file
```

#### Solution 3: Test Microphone
```bash
# Test microphone
adb shell am start -a android.media.action.RECORD_SOUND

# Or use voice recorder app
```

---

### Issue: UI Navigation Not Working

**Symptoms:**
```
Accessibility service not responding
UI element not found
```

**Possible Causes:**
1. Accessibility service not enabled
2. Target app not installed
3. UI element selector outdated

**Solutions:**

#### Solution 1: Enable Accessibility Service
```bash
# Check service status
adb shell settings get secure enabled_accessibility_services

# Enable if not set
adb shell settings put secure enabled_accessibility_services com.egyptian.agent.debug/.accessibility.EgyptianAgentAccessibilityService
```

#### Solution 2: Verify Target App
```bash
# Check if target app is installed
adb shell pm list packages | grep facebook

# Install if missing
adb install facebook.apk
```

#### Solution 3: Update Element Selectors
```kotlin
// In UINavigationEngine.kt
// Update element selectors based on current app version
val elementSelectors = mapOf(
    "facebook_news" to listOf("News Feed", "Home", "الأخبار"),
    "whatsapp_send" to listOf("Message", "Send", "إرسال")
)
```

---

## Performance Issues

### Issue: Slow Response Time

**Symptoms:**
```
Command processing takes >5 seconds
UI navigation is sluggish
```

**Possible Causes:**
1. Model loading overhead
2. Memory pressure
3. CPU throttling

**Solutions:**

#### Solution 1: Pre-warm Model
```kotlin
// In Application.onCreate()
override fun onCreate() {
    super.onCreate()
    // Pre-load model on app start
    CoroutineScope(Dispatchers.IO).launch {
        FunctionGemmaEngine.getInstance().loadModel()
    }
}
```

#### Solution 2: Clear Memory
```bash
# Clear app cache
adb shell pm clear com.egyptian.agent.debug

# Restart app
adb shell am force-stop com.egyptian.agent.debug
```

#### Solution 3: Check CPU Throttling
```bash
# Check CPU frequency
adb shell cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq

# Check temperature
adb shell cat /sys/class/thermal/thermal_zone*/temp
```

---

### Issue: High Battery Drain

**Symptoms:**
```
Battery drains >10% per hour
Device gets hot during use
```

**Possible Causes:**
1. Wake lock held too long
2. Background processing
3. Model inference overhead

**Solutions:**

#### Solution 1: Check Battery Usage
```bash
# Get battery stats
adb shell dumpsys batterystats --checkin | grep com.egyptian.agent

# Check wake locks
adb shell dumpsys power | grep "Wake Locks"
```

#### Solution 2: Optimize Wake Word Detection
```kotlin
// Reduce wake word detection frequency
// In WakeWordDetector.kt
private val DETECTION_INTERVAL_MS = 500L  // Increase from 200ms
```

#### Solution 3: Enable Battery Optimization
```bash
# Remove from battery whitelist (if not needed always-on)
adb shell dumpsys deviceidle whitelist -com.egyptian.agent.debug
```

---

## Log Analysis

### How to Capture Logs

```bash
# Capture all logs
adb logcat -d > full_log.txt

# Capture in real-time
adb logcat -v threadtime

# Filter by tag
adb logcat -s "HybridOrchestrator" "FunctionGemma" "UINavigation"

# Filter by priority
adb logcat *:E  # Errors only
adb logcat *:W  # Warnings and above
adb logcat *:I  # Info and above

# Capture crash logs
adb logcat -d > crash_$(date +%Y%m%d_%H%M%S).txt
```

### Common Log Patterns

| Pattern | Meaning | Action |
|---------|---------|--------|
| `I/FunctionGemma: Model loaded` | Model loaded successfully | No action needed |
| `W/HybridOrchestrator: Low confidence` | Intent confidence low | Check command clarity |
| `E/UINavigation: Element not found` | UI element missing | Update selector |
| `F/AndroidRuntime: FATAL EXCEPTION` | App crash | Check crash log |

---

## FAQ

### Q: How do I reset the app to factory settings?

**A:**
```bash
# Clear all app data
adb shell pm clear com.egyptian.agent.debug

# Uninstall and reinstall
adb uninstall com.egyptian.agent.debug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Re-grant permissions
# (See Permission Denied section)
```

---

### Q: How do I update to a new version?

**A:**
```bash
# Build new version
./gradlew.bat assembleDebug

# Install over existing
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or uninstall first for clean install
adb uninstall com.egyptian.agent.debug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

### Q: How do I backup app data?

**A:**
```bash
# Backup app data
adb backup -f backup.ab com.egyptian.agent.debug

# Restore app data
adb restore backup.ab

# Backup models
adb pull /sdcard/EgyptianAgent/models/ ./backup/models/
```

---

### Q: How do I enable debug mode?

**A:**
```bash
# Enable debug logging
adb shell setprop log.tag.HybridOrchestrator DEBUG
adb shell setprop log.tag.FunctionGemma DEBUG
adb shell setprop log.tag.UINavigation DEBUG

# View debug logs
adb logcat -s "HybridOrchestrator:D" "FunctionGemma:D" "UINavigation:D"
```

---

### Q: Where can I get help?

**A:**
1. Check this troubleshooting guide
2. Review logs with `adb logcat`
3. Check GitHub Issues
4. Contact support@egyptianagent.com

---

## Contact Support

If you've tried all solutions and still experiencing issues:

1. **Collect Information:**
   - Device model and Android version
   - App version
   - Full error logs
   - Steps to reproduce

2. **Submit Issue:**
   - GitHub: https://github.com/Kandil7/EgyptianAgent/issues
   - Email: support@egyptianagent.com

3. **Include:**
   ```
   Device: Honor X6c
   Android: 13
   App Version: 3.0.0-hybrid
   Issue: [Description]
   Steps to Reproduce: [Steps]
   Logs: [Attached]
   ```

---

*EgyptianAgent Technical Support*  
*Version 3.0.0-hybrid*  
*March 14, 2026*
