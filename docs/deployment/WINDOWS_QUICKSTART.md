# EgyptianAgent - Windows Quick Start Guide

**For new developers setting up on Windows 10/11**

---

## 🚀 Quick Setup (5 Minutes)

### Step 1: Open PowerShell as Administrator

Press `Win + X` and select **Windows PowerShell (Admin)** or **Terminal (Admin)**

### Step 2: Run the Automated Setup

```powershell
# Navigate to project directory
cd K:\business\projects_v2\EgyptianAgent

# Run the setup script
.\scripts\setup\windows_setup.ps1 -Auto
```

This will automatically install:
- ✅ Java JDK 17
- ✅ Android SDK Platform Tools (ADB)
- ✅ Gradle
- ✅ Git (if needed)

### Step 3: Initialize Gradle Wrapper

```powershell
# Initialize the Gradle wrapper
.\scripts\utils\init_gradle_wrapper.ps1
```

### Step 4: Verify Setup

```powershell
# Close and reopen PowerShell, then verify
java -version
adb version
.\gradlew.bat --version
```

---

## 📱 Connect Your Android Device

### Enable USB Debugging

1. **On your Android device:**
   - Settings → About phone
   - Tap "Build number" 7 times
   - Settings → System → Developer options
   - Enable "USB debugging"

2. **Connect via USB:**
   - Connect your device to your computer
   - Accept the "Allow USB debugging" prompt on your device

3. **Verify connection:**
```powershell
adb devices
```

You should see your device listed.

---

## 🔨 Build and Deploy

### Build the App

```powershell
# Debug build (for development)
.\gradlew.bat assembleDebug

# Release build (for production)
.\gradlew.bat assembleRelease
```

### Install on Device

```powershell
# Install debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Or install release APK
adb install -r app\build\outputs\apk\release\app-release.apk
```

### Verify Deployment

```powershell
# Run the verification script
.\scripts\deploy\verify_deployment.ps1
```

### Launch the App

```powershell
adb shell am start -n com.egyptian.agent/.MainActivity
```

---

## 📋 Quick Reference Commands

| Task | Command |
|------|---------|
| Build debug APK | `.\gradlew.bat assembleDebug` |
| Build release APK | `.\gradlew.bat assembleRelease` |
| Install APK | `adb install -r path\to\apk` |
| Check device | `adb devices` |
| View logs | `adb logcat -s EgyptianAgent:*` |
| Uninstall app | `adb uninstall com.egyptian.agent` |
| Run verification | `.\scripts\deploy\verify_deployment.ps1` |

---

## 🛠️ Troubleshooting

### "ADB not found"

```powershell
# Run setup script again
.\scripts\setup\windows_setup.ps1

# Or install manually
choco install android-sdk-platform-tools -y
```

### "Gradle wrapper not found"

```powershell
# Initialize the wrapper
.\scripts\utils\init_gradle_wrapper.ps1
```

### "No devices connected"

1. Enable USB debugging on your device
2. Reconnect the USB cable
3. Accept the authorization prompt on your device
4. Run: `adb kill-server` then `adb start-server`

### "Java not found"

```powershell
# Install Java
choco install openjdk17 -y

# Or run setup script
.\scripts\setup\windows_setup.ps1
```

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [DEPLOYMENT_PREREQUISITES.md](deployment/DEPLOYMENT_PREREQUISITES.md) | Full setup guide with manual installation options |
| [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) | Production deployment procedures |
| [PRODUCTION_CHECKLIST.md](../../PRODUCTION_CHECKLIST.md) | Pre-deployment checklist |

---

## 🎯 Next Steps

After successful setup:

1. **Read the architecture docs** - Understand the hybrid architecture
2. **Review the codebase** - Familiarize yourself with the project structure
3. **Run the tests** - `.\gradlew.bat test`
4. **Start developing** - Make your first change!

---

**Need help?** See [DEPLOYMENT_PREREQUISITES.md](deployment/DEPLOYMENT_PREREQUISITES.md) for detailed troubleshooting.
