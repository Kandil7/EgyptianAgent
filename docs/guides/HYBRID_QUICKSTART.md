# EgyptianAgent - Hybrid AI Quick Start Guide

**Version:** 3.0.0  
**Last Updated:** March 14, 2026  
**Time Required:** 5 minutes  

---

## 🚀 Quick Start: Hybrid AI Features

This guide will help you set up and test the **Hybrid AI Architecture** in 5 minutes, combining fast intent-based commands with powerful UI navigation capabilities.

---

## Prerequisites

Before starting, ensure you have:

| Requirement | Details |
|-------------|---------|
| **Device** | Android 12+ device (Honor X6c recommended) |
| **Storage** | 2.5GB+ free space |
| **ADB** | Android SDK Platform Tools installed |
| **App** | EgyptianAgent APK built |
| **Models** | FunctionGemma & Whisper models downloaded |

---

## Step 1: Install the Application (1 minute)

### Build the APK

```bash
cd K:\business\projects_v2\EgyptianAgent

# Build debug APK (faster for testing)
./gradlew assembleDebug

# Or build release APK
./gradlew assembleRelease
```

### Install on Device

```bash
# Connect device and install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Verify installation
adb shell pm list packages | grep egyptian
```

**Expected Output:**
```
com.egyptian.agent
```

---

## Step 2: Enable Accessibility Service (1 minute)

The Hybrid AI requires the Accessibility Service for UI navigation.

### Method 1: Manual Setup (Recommended)

1. Open **Settings** on your device
2. Go to **Accessibility** → **Installed Services**
3. Find **EgyptianAgent**
4. Toggle **ON**
5. Confirm the warning dialog

### Method 2: ADB Command

```bash
# Enable accessibility service
adb shell settings put secure enabled_accessibility_services \
  com.egyptian.agent/.accessibility.EgyptianAccessibilityService
```

### Verify It's Enabled

```bash
adb shell settings get secure enabled_accessibility_services
```

**Expected Output:** Should contain `com.egyptian.agent`

---

## Step 3: Grant Permissions (1 minute)

Grant all required permissions for full functionality:

```bash
# Grant all permissions at once
adb shell pm grant com.egyptian.agent android.permission.RECORD_AUDIO
adb shell pm grant com.egyptian.agent android.permission.CALL_PHONE
adb shell pm grant com.egyptian.agent android.permission.READ_CONTACTS
adb shell pm grant com.egyptian.agent android.permission.WRITE_CONTACTS
adb shell pm grant com.egyptian.agent android.permission.SEND_SMS
adb shell pm grant com.egyptian.agent android.permission.READ_SMS
adb shell pm grant com.egyptian.agent android.permission.BODY_SENSORS
adb shell pm grant com.egyptian.agent android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.egyptian.agent android.permission.CAMERA
adb shell pm grant com.egyptian.agent android.permission.READ_EXTERNAL_STORAGE
```

### Verify Permissions

```bash
adb shell dumpsys package com.egyptian.agent | grep "granted=true"
```

---

## Step 4: Deploy Models (1 minute)

### Deploy FunctionGemma Model

```bash
# Download if not already downloaded
./scripts/model/download_functiongemma_model.sh

# Deploy to device
./scripts/deploy/deploy_functiongemma.sh
```

### Deploy Whisper Model

```bash
# Download Egyptian Whisper model
./scripts/model/download_whisper_model.sh
```

### Verify Models

```bash
adb shell ls -lh /data/local/llm/
```

**Expected Output:**
```
-rw-r--r-- 1 root root 288M functiongemma-270m-it-Q4_K_M.gguf
-rw-r--r-- 1 root root 150M whisper-egyptian-v1.bin
```

---

## Step 5: Test Fast Path Command (30 seconds)

Fast Path handles simple commands in ~350ms using FunctionGemma.

### Test Command

```bash
# Test a simple call command (Egyptian Arabic)
adb shell am start -n com.egyptian.agent/.VoiceActivity \
  --es command "اتصل بماما"
```

### Monitor Logs

```bash
adb logcat | grep -E "(FunctionGemma|FastPath|Intent)"
```

**Expected Output:**
```
FunctionGemma: Intent classified as CALL_CONTACT (confidence: 0.97)
FastPath: Executing call to "ماما"
TTS: "تمام، باتصل بماما دلوقتي"
```

### More Fast Path Examples

| Egyptian Command | English | Expected Action |
|-----------------|---------|-----------------|
| "اتصل ببابا" | Call dad | Initiate call |
| "ابعت واتساب" | Send WhatsApp | Open WhatsApp |
| "نبهني بكرة الصبح" | Wake me tomorrow | Set alarm |
| "افتح الواتساب" | Open WhatsApp | Launch app |
| "قفل الواي فاي" | Turn off WiFi | Toggle setting |

---

## Step 6: Test Slow Path Command (30 seconds)

Slow Path handles complex UI navigation tasks in 2-5 seconds.

### Test Command

```bash
# Test a complex navigation command
adb shell am start -n com.egyptian.agent/.VoiceActivity \
  --es command "افتح الفيسبوك وشوف الأخبار"
```

### Monitor Logs

```bash
adb logcat | grep -E "(SlowPath|UINavigation|Accessibility)"
```

**Expected Output:**
```
HybridOrchestrator: Routing to SLOW_PATH (requires UI navigation)
SlowPath: Launching Facebook...
UINavigation: Executing tap on "News Feed"
AccessibilityTree: Found 45 interactive elements
TTS: "تمام، بفتح فيسبوك..."
```

### More Slow Path Examples

| Egyptian Command | English | Expected Actions |
|-----------------|---------|------------------|
| "دور على فيديو في اليوتيوب" | Search YouTube | Launch → Search → Play |
| "احجز أوبر للبيت" | Book Uber home | Launch → Enter destination → Confirm |
| "انشر صورة على انستجرام" | Post on Instagram | Launch → Select → Edit → Post |
| "اقرا الرسالة اللي جاية" | Read incoming message | Launch → Open → Read |

---

## Step 7: Run a Workflow (30 seconds)

Workflows are pre-defined sequences of UI actions for common tasks.

### Deploy Workflows

```bash
# Copy workflows to device
adb shell mkdir -p /sdcard/Android/data/com.egyptian.agent/files/workflows

# Push workflow files
adb push configs/workflows/morning_routine.yaml \
  /sdcard/Android/data/com.egyptian.agent/files/workflows/
```

### Execute Workflow

```bash
# Run morning routine workflow
adb shell am startservice \
  -n com.egyptian.agent/.workflow.WorkflowService \
  --es workflow "morning_routine"
```

### Available Workflows

| Workflow | Egyptian Name | Description |
|----------|---------------|-------------|
| `morning_routine.yaml` | روتين الصباح | Weather, news, WhatsApp status |
| `bedtime_routine.yaml` | وقت النوم | Alarm, notifications off |
| `check_social.yaml` | شوف السوشيال | Check Facebook, Instagram |
| `send_whatsapp_broadcast.yaml` | ابعت للجميع | Send to multiple contacts |
| `book_uber.yaml` | احجز أوبر | Uber booking flow |
| `check_email.yaml` | اقرا الايميل | Gmail inbox check |
| `youtube_search.yaml` | دور على فيديو | Search and play videos |
| `settings_toggle.yaml` | غيّر الإعدادات | WiFi, Bluetooth, brightness |
| `emergency_check.yaml` | اطمن على العيلة | Call family sequentially |
| `grocery_list.yaml` | قائمة المشتريات | Add items to notes app |

---

## ✅ Verification Checklist

Run the comprehensive verification script to confirm everything is working:

```bash
# Run all checks
./scripts/deploy/verify_deployment.sh

# Or run with auto-fix
./scripts/deploy/verify_deployment.sh --auto-fix

# Save report to file
./scripts/deploy/verify_deployment.sh --output-file deployment_report.md
```

**Expected Output:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  EgyptianAgent Deployment Verification
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[INFO] Starting verification...
[INFO] Timestamp: 20260314_143022

  Check 1: Build Verification
  [INFO] ✓ Debug APK available for testing

  Check 2: Device Connection
  [INFO] ✓ Device connected and authorized

  ...

  Overall: ✅ ALL CHECKS PASSED
```

---

## 🔧 Troubleshooting Tips

### Issue: "No device connected"

**Solution:**
```bash
# Check ADB connection
adb devices

# Restart ADB server
adb kill-server
adb start-server

# Reconnect device
adb devices
```

### Issue: "Accessibility service not found"

**Solution:**
1. Uninstall and reinstall the app
2. Ensure accessibility service is declared in `AndroidManifest.xml`
3. Manually enable in Settings → Accessibility

### Issue: "Models not found"

**Solution:**
```bash
# Re-download models
./scripts/model/download_functiongemma_model.sh
./scripts/model/download_whisper_model.sh

# Re-deploy
./scripts/deploy/deploy_functiongemma.sh
```

### Issue: "Permission denied"

**Solution:**
```bash
# Grant all permissions
for perm in RECORD_AUDIO CALL_PHONE READ_CONTACTS; do
  adb shell pm grant com.egyptian.agent android.permission.$perm
done
```

### Issue: "App crashes on launch"

**Solution:**
```bash
# Check logs
adb logcat | grep -E "(EgyptianAgent|FATAL)"

# Clear app data
adb shell pm clear com.egyptian.agent

# Reinstall
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Issue: "Slow path not working"

**Solution:**
1. Verify accessibility service is enabled
2. Check accessibility logs: `adb logcat | grep Accessibility`
3. Ensure target app has accessible UI elements

---

## 📊 Performance Expectations

| Metric | Target | Expected |
|--------|--------|----------|
| Fast Path Latency | <2.0s | ~1.5s |
| Slow Path Latency | <5.0s | ~3.5s |
| Routing Decision | <100ms | ~50ms |
| Memory Usage | <800MB | ~600MB |
| Battery Drain | <5%/hr | ~3.5%/hr |

---

## 🎯 Next Steps

After completing this quick start:

1. **Read the Full Documentation:**
   - [Hybrid Architecture](docs/architecture/HYBRID_ARCHITECTURE.md)
   - [Deployment Guide](docs/deployment/DEPLOYMENT_GUIDE.md)
   - [User Manual (Arabic)](docs/guides/user_manual_ar.md)

2. **Customize Workflows:**
   - Create custom YAML workflows for your daily tasks
   - See workflow format in `configs/workflows/`

3. **Fine-tune Models:**
   - Improve Egyptian dialect accuracy
   - See `docs/guides/FUNCTIONGEMMA_FINETUNING_GUIDE.md`

4. **Run Tests:**
   - Execute full test suite
   - See `docs/testing/FUNCTIONGEMMA_TEST_PLAN.md`

---

## 📞 Getting Help

| Resource | Link |
|----------|------|
| Documentation | [docs/](docs/) |
| Troubleshooting | [docs/guides/TROUBLESHOOTING.md](docs/guides/TROUBLESHOOTING.md) |
| GitHub Issues | [Report a bug](https://github.com/Kandil7/EgyptianAgent/issues) |
| User Manual (EN) | [docs/guides/user_manual.md](docs/guides/user_manual.md) |
| User Manual (AR) | [docs/guides/user_manual_ar.md](docs/guides/user_manual_ar.md) |

---

<div align="center">

**Made with ❤️ for the Egyptian community**

*Empowering voices, one command at a time.*

---

**Quick Start Version:** 3.0.0  
**Last Updated:** March 14, 2026

</div>
