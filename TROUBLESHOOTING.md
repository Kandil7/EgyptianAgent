# EgyptianAgent Build & Deployment Troubleshooting Guide

**Version:** 1.1.0  
**Last Updated:** March 2, 2026

---

## Quick Reference

| Symptom | Likely Cause | Quick Fix |
|---------|--------------|-----------|
| Build fails with "NDK not found" | NDK not installed | Install NDK 25.2.9519653 |
| CMake configuration fails | CMake not in SDK | Install CMake 3.22.1+ |
| APK signing fails | Keystore issue | Check passwords/path |
| App crashes on launch | Missing permissions | Grant required permissions |
| Voice service not starting | Battery optimization | Whitelist the app |
| Native libs not loading | Submodules not init | Run initialize_submodules.sh |

---

## Build Errors

### ERROR: NDK not found

```
> Could not get CMake installed.
> Please install it from Android SDK Tools.
```

**Solution:**

1. Open Android Studio → SDK Manager
2. Go to "SDK Tools" tab
3. Check "NDK (Side by side)"
4. Select version 25.2.9519653
5. Click Apply

Or set manually in `local.properties`:
```properties
ndk.dir=C\:\\Users\\<user>\\AppData\\Local\\Android\\Sdk\\ndk\\25.2.9519653
```

### ERROR: CMake not found

```
CMake was not found in the following locations:
```

**Solution:**

1. Open Android Studio → SDK Manager
2. Go to "SDK Tools" tab
3. Check "CMake"
4. Select version 3.22.1 or higher
5. Click Apply

### ERROR: Out of Memory during build

```
java.lang.OutOfMemoryError: Java heap space
```

**Solution:**

Update `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -XX:+UseParallelGC
```

### ERROR: Signing configuration missing

```
SigningConfig with name 'release' not found.
```

**Solution:**

1. Create keystore:
```bash
keytool -genkey -v -keystore keystore/release.keystore \
  -alias egyptian_agent -keyalg RSA -keysize 2048 -validity 10000
```

2. Set environment variables:
```bash
export KEYSTORE_PASSWORD="your_password"
export KEY_ALIAS="egyptian_agent"
export KEY_PASSWORD="your_key_password"
```

### ERROR: Submodule initialization fails

```
fatal: No url found for submodule path 'external/llama.cpp'
```

**Solution:**

```bash
# Remove problematic submodule
rm -rf external/llama.cpp
git submodule deinit -f external/llama.cpp

# Reinitialize
git submodule update --init --recursive

# Or use our script
./initialize_submodules.sh
```

### ERROR: Gradle sync failed

```
Could not resolve all files for configuration ':app:debugCompileClasspath'
```

**Solution:**

```bash
# Clean Gradle cache
./gradlew clean --refresh-dependencies

# Delete .gradle folder
rm -rf .gradle

# Re-sync
./gradlew build --refresh-dependencies
```

### ERROR: R8/ProGuard optimization fails

```
R8: Missing class: com.example.SomeClass
```

**Solution:**

Add to `app/proguard-rules.pro`:
```proguard
-keep class com.example.** { *; }
-dontwarn com.example.**
```

---

## Native Build Errors

### ERROR: llama.cpp build fails

```
CMake Error at external/llama.cpp/CMakeLists.txt:XX
```

**Solution:**

1. Ensure submodules are initialized:
```bash
./initialize_submodules.sh
```

2. Check llama.cpp has its submodules:
```bash
cd external/llama.cpp
git submodule update --init --recursive
```

3. Build with mock implementation instead:
```bash
./build.sh --release  # Without --native flag
```

### ERROR: JNI compilation errors

```
error: undefined reference to 'llama_init'
```

**Solution:**

This indicates the native library isn't linked properly. Check:

1. CMakeLists.txt has correct paths
2. USE_LLAMA_CPP flag is set correctly
3. Native library is being built:
```bash
./gradlew externalNativeBuildDebug
```

---

## Installation Errors

### ERROR: INSTALL_FAILED_SYSTEM_SIZE

```
Failure [INSTALL_FAILED_SYSTEM_SIZE]
```

**Cause:** System partition is full.

**Solution:**

```bash
# Check system partition space
adb shell df -h /system

# Remove old installation
adb shell su -c "rm -rf /system/priv-app/EgyptianAgent"

# Reboot and retry
adb reboot
```

### ERROR: INSTALL_FAILED_UPDATE_INCOMPATIBLE

```
Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Package com.egyptian.agent signatures do not match]
```

**Cause:** Different signing key than installed version.

**Solution:**

```bash
# Uninstall existing app
adb uninstall com.egyptian.agent

# Or for system app
adb shell su -c "rm -rf /system/priv-app/EgyptianAgent"
adb reboot

# Then install new version
adb install -r app-release.apk
```

### ERROR: Permission denied (system app)

```
cp: can't create file: Permission denied
```

**Cause:** System partition is read-only.

**Solution:**

```bash
# Remount system as writable
adb shell su -c "mount -o remount,rw /system"

# Or use Magisk (recommended)
# Install as Magisk module instead
```

### ERROR: App crashes immediately

**Diagnosis:**

```bash
# Check crash logs
adb logcat -s EgyptianAgent:* AndroidRuntime:*

# Look for specific errors
adb logcat | grep -i "fatal\|exception\|crash"
```

**Common causes:**

1. Missing permissions:
```bash
adb shell pm grant com.egyptian.agent android.permission.RECORD_AUDIO
adb shell pm grant com.egyptian.agent android.permission.CALL_PHONE
```

2. Native library loading failure:
```bash
adb logcat | grep -i "UnsatisfiedLinkError"
```

3. Missing assets:
```bash
adb shell ls /data/data/com.egyptian.agent/files/
```

---

## Runtime Errors

### ERROR: Voice service not starting

**Symptoms:** App opens but voice service doesn't start.

**Diagnosis:**

```bash
adb shell dumpsys activity services | grep -i egyptian
```

**Solutions:**

1. Grant microphone permission:
```bash
adb shell pm grant com.egyptian.agent android.permission.RECORD_AUDIO
adb shell pm grant com.egyptian.agent android.permission.FOREGROUND_SERVICE_MICROPHONE
```

2. Disable battery optimization:
```bash
adb shell dumpsys deviceidle whitelist +com.egyptian.agent
adb shell cmd appops set com.egyptian.agent RUN_IN_BACKGROUND allow
```

3. Check Foreground Service declaration in AndroidManifest.xml

### ERROR: Wake word not detected

**Symptoms:** "يا صاحبي" doesn't activate the assistant.

**Solutions:**

1. Check Vosk model is present:
```bash
adb shell ls /data/data/com.egyptian.agent/files/models/
```

2. Re-download models:
```bash
./scripts/fetch_models.sh
```

3. Check microphone access:
```bash
adb shell appops get com.egyptian.agent RECORD_AUDIO
# Should show: allowed
```

### ERROR: LLM responses slow or failing

**Symptoms:** Inference takes too long or returns errors.

**Solutions:**

1. Check model file exists and is accessible
2. Verify enough RAM (6GB minimum)
3. Reduce context size in settings
4. Use mock implementation for testing:
```bash
# Build without native LLM
./build.sh --release
```

---

## Device-Specific Issues (Honor X6c)

### Issue: Aggressive battery optimization

**Symptoms:** App killed in background.

**Solution:**

```bash
# Honor-specific battery whitelist
adb shell su -c "pm grant com.egyptian.agent android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
adb shell am start -a android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -d package:com.egyptian.agent

# Or manually:
# Settings → Battery → App Launch → EgyptianAgent → Manage manually
# Enable all options
```

### Issue: Boot animation stuck

**Symptoms:** Device stuck at Honor logo after system app install.

**Solution:**

```bash
# Boot to safe mode
adb reboot safe

# Remove system app
adb shell su -c "rm -rf /system/priv-app/EgyptianAgent"

# Reboot normally
adb reboot
```

### Issue: Microphone not accessible

**Symptoms:** Voice commands not working.

**Solution:**

```bash
# Check microphone permission
adb shell appops get com.egyptian.agent RECORD_AUDIO

# Grant if needed
adb shell pm grant com.egyptian.agent android.permission.RECORD_AUDIO

# Check Honor's privacy settings
# Settings → Privacy → Permission Manager → Microphone
```

---

## Performance Issues

### Issue: High memory usage

**Diagnosis:**

```bash
adb shell dumpsys meminfo com.egyptian.agent
```

**Solutions:**

1. Reduce model size
2. Use quantized models (Q4_K_M)
3. Limit context size
4. Enable R8 full mode

### Issue: Slow inference

**Diagnosis:**

```bash
adb shell dumpsys cpuinfo | grep egyptian
```

**Solutions:**

1. Use smaller model
2. Reduce threads if overheating
3. Enable GPU acceleration (if supported)
4. Close background apps

---

## CI/CD Issues

### ERROR: GitHub Actions build fails

**Common causes:**

1. Missing secrets:
   - `RELEASE_KEYSTORE_B64`
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`

2. Submodule not initialized:
```yaml
# Add to workflow
- name: Initialize submodules
  run: |
    git submodule update --init --recursive
```

3. NDK not available:
```yaml
# Add to workflow
- name: Set up Android SDK
  uses: android-actions/setup-android@v3
  with:
    packages: 'ndk;25.2.9519653'
```

---

## Debugging Tools

### ADB Commands

```bash
# View real-time logs
adb logcat -s EgyptianAgent:*

# Filter by priority
adb logcat *:E  # Errors only
adb logcat *:W  # Warnings and above

# Save logs to file
adb logcat -d > crash_log.txt

# Check app status
adb shell dumpsys package com.egyptian.agent

# Check running services
adb shell dumpsys activity services | grep -i egyptian

# Force stop
adb shell am force-stop com.egyptian.agent

# Clear data
adb shell pm clear com.egyptian.agent

# Check permissions
adb shell dumpsys package com.egyptian.agent | grep permission
```

### Gradle Debug Commands

```bash
# Show full stack trace
./gradlew assembleRelease --stacktrace

# Show build scan
./gradlew assembleRelease --scan

# Profile build
./gradlew assembleRelease --profile

# Show dependencies
./gradlew app:dependencies

# Clean and rebuild
./gradlew clean build --refresh-dependencies
```

---

## Getting Help

1. **Check logs first:** `adb logcat -s EgyptianAgent:*`
2. **Search existing issues:** GitHub Issues
3. **Provide information:**
   - Full error message
   - Build command used
   - Device model and Android version
   - Relevant logcat output

---

*This troubleshooting guide is part of the EgyptianAgent project documentation.*
