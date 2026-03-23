# EgyptianAgent - Deployment Prerequisites Guide

**Version:** 1.0.0  
**Last Updated:** 2026-03-14  
**Platform:** Windows 10/11  
**Status:** ✅ Ready for Production

---

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Required Software](#required-software)
3. [Quick Setup (Recommended)](#quick-setup-recommended)
4. [Manual Installation](#manual-installation)
5. [Android Device Setup](#android-device-setup)
6. [Verification Steps](#verification-steps)
7. [Troubleshooting](#troubleshooting)
8. [Next Steps](#next-steps)

---

## System Requirements

### Minimum Requirements

| Component | Requirement | Notes |
|-----------|-------------|-------|
| OS | Windows 10 (64-bit) or Windows 11 | Version 21H2 or later |
| CPU | Intel i5 / AMD Ryzen 5 or equivalent | 4 cores minimum |
| RAM | 8 GB | 16 GB recommended |
| Storage | 10 GB free space | SSD recommended |
| Internet | Required | For downloads and updates |

### Recommended Requirements

| Component | Specification |
|-----------|---------------|
| OS | Windows 11 Pro (64-bit) |
| CPU | Intel i7 / AMD Ryzen 7 or equivalent |
| RAM | 16 GB or more |
| Storage | 20 GB free SSD space |
| USB | USB 3.0 port for device connection |

---

## Required Software

### Core Dependencies

| Software | Version | Purpose | Installation Method |
|----------|---------|---------|---------------------|
| Java JDK | 17 | Build system runtime | Chocolatey / Manual |
| Android SDK Platform Tools | Latest | ADB, Fastboot | Chocolatey / Manual |
| Gradle | 8.13 | Build automation | Chocolatey / Wrapper |
| Git | 2.40+ | Version control | Chocolatey / Manual |

### Optional (Recommended)

| Software | Version | Purpose |
|----------|---------|---------|
| Android Studio | Latest | SDK Manager, Emulator |
| Chocolatey | Latest | Package manager for Windows |
| PowerShell | 7.3+ | Enhanced scripting |

---

## Quick Setup (Recommended)

### Option A: Using Chocolatey Package Manager

**Step 1: Install Chocolatey (if not installed)**

Open PowerShell as **Administrator** and run:

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))
```

**Step 2: Install all prerequisites**

```powershell
# Install Java JDK 17
choco install openjdk17 -y

# Install Android SDK Platform Tools
choco install android-sdk-platform-tools -y

# Install Gradle
choco install gradle -y

# Install Git (if needed)
choco install git -y
```

**Step 3: Verify installations**

```powershell
# Close and reopen PowerShell, then verify
java -version
adb version
gradle --version
git --version
```

**Step 4: Initialize Gradle Wrapper**

```powershell
# Navigate to project directory
cd K:\business\projects_v2\EgyptianAgent

# Initialize Gradle wrapper
gradle wrapper --gradle-version 8.13
```

---

### Option B: Using Automated Setup Script

**Run the setup script:**

```powershell
# Open PowerShell as Administrator
cd K:\business\projects_v2\EgyptianAgent

# Run the automated setup
.\scripts\setup\windows_setup.ps1 -Auto
```

**Interactive mode (choose what to install):**

```powershell
.\scripts\setup\windows_setup.ps1
```

---

## Manual Installation

### 1. Java JDK 17

**Download:**
- [Eclipse Adoptium Temurin 17](https://adoptium.net/temurin/releases/?version=17)
- Direct MSI: [OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.msi](https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.msi)

**Installation:**
1. Run the downloaded MSI installer
2. Follow the installation wizard
3. Default path: `C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot`

**Verify:**
```powershell
java -version
```

**Expected output:**
```
openjdk version "17.0.9" 2023-10-17
OpenJDK Runtime Environment Temurin-17.0.9+9 (build 17.0.9+9)
OpenJDK 64-Bit Server VM Temurin-17.0.9+9 (build 17.0.9+9, mixed mode, sharing)
```

---

### 2. Android SDK Platform Tools (ADB)

**Download:**
- [Android SDK Platform Tools](https://developer.android.com/studio/releases/platform-tools)
- Direct ZIP: [platform-tools-latest-windows.zip](https://dl.google.com/android/repository/platform-tools-latest-windows.zip)

**Installation:**

1. Create directory:
```powershell
mkdir $env:LOCALAPPDATA\Android\Sdk
```

2. Extract the ZIP to:
```
C:\Users\<YourUsername>\AppData\Local\Android\Sdk\platform-tools
```

3. Add to PATH:

**Via GUI:**
1. Press `Win + X` → System
2. Click "Advanced system settings"
3. Click "Environment Variables"
4. Under "User variables", select `Path` → Edit
5. Click "New" and add: `%LOCALAPPDATA%\Android\Sdk\platform-tools`
6. Click OK on all dialogs

**Via PowerShell (Admin):**
```powershell
$platformToolsPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools"
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($currentPath -notlike "*$platformToolsPath*") {
    [Environment]::SetEnvironmentVariable("Path", "$currentPath;$platformToolsPath", "User")
}
```

**Verify:**
```powershell
adb version
```

**Expected output:**
```
Android Debug Bridge version 1.0.41
Version 34.0.5-10916195
Installed as C:\Users\<User>\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

---

### 3. Gradle

**Download:**
- [Gradle 8.13 Distribution](https://services.gradle.org/distributions/gradle-8.13-bin.zip)

**Installation:**

1. Create directory:
```powershell
mkdir C:\Gradle
```

2. Extract ZIP to:
```
C:\Gradle\gradle-8.13
```

3. Add to PATH:

**Via PowerShell (Admin):**
```powershell
$gradlePath = "C:\Gradle\gradle-8.13\bin"
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($currentPath -notlike "*$gradlePath*") {
    [Environment]::SetEnvironmentVariable("Path", "$currentPath;$gradlePath", "User")
}
```

4. Set GRADLE_USER_HOME (optional):
```powershell
[Environment]::SetEnvironmentVariable("GRADLE_USER_HOME", "$env:USERPROFILE\.gradle", "User")
```

**Verify:**
```powershell
gradle --version
```

**Expected output:**
```
Welcome to Gradle 8.13!

Here are the highlights of this release:
...

Gradle 8.13

Build time:    2025-02-25 09:22:14 UTC
Revision:      ...

Kotlin:        2.0.21
Groovy:        3.0.22
Ant:           Apache Ant(TM) version 1.10.15
JVM:           17.0.9 (Eclipse Adoptium 17.0.9+9)
OS:            Windows 11 10.0 amd64
```

---

### 4. Git

**Download:**
- [Git for Windows](https://git-scm.com/download/win)
- Direct: [Git-2.44.0-64-bit.exe](https://github.com/git-for-windows/git/releases/download/v2.44.0.windows.1/Git-2.44.0-64-bit.exe)

**Installation:**
1. Run the installer
2. Use default settings
3. Select "Git from the command line and also from 3rd-party software"

**Verify:**
```powershell
git --version
```

---

## Android Device Setup

### Enable USB Debugging

**For Honor X6c (Android 12) and similar devices:**

#### Step 1: Enable Developer Options

1. Open **Settings** on your device
2. Scroll to **About phone**
3. Find **Build number**
4. Tap **Build number** 7 times rapidly
5. You'll see: *"You are now a developer!"*

#### Step 2: Enable USB Debugging

1. Go to **Settings** → **System** → **Developer options**
2. Find and enable **USB debugging**
3. Accept the warning prompt

#### Step 3: Additional Settings for Honor Devices

For full functionality, also enable:

| Setting | Location | Value |
|---------|----------|-------|
| Disable Permission Monitoring | Developer options | Enabled |
| Stay awake | Developer options | Enabled (optional) |
| USB debugging (Security settings) | Developer options | Enabled |

#### Step 4: Connect and Authorize

1. Connect device via USB cable
2. On your device, a prompt will appear: *"Allow USB debugging?"*
3. Check **"Always allow from this computer"**
4. Tap **OK**

---

### Root Access (For System App Installation)

**Required for:** Full background execution, system-level permissions

#### Install Magisk

1. **Unlock Bootloader** (wipes data):
   - Backup your data first
   - Enable OEM unlocking in Developer options
   - Use fastboot to unlock

2. **Download Magisk:**
   - [Magisk GitHub Releases](https://github.com/topjohnwu/Magisk/releases)

3. **Install Magisk:**
   - Follow the official installation guide
   - [Magisk Installation Guide](https://topjohnwu.github.io/Magisk/install.html)

4. **Verify Root:**
```powershell
adb shell su -c "id"
```

Expected output: `uid=0(root) gid=0(root)`

---

## Verification Steps

### Complete Verification Checklist

Run these commands in PowerShell:

```powershell
# 1. Verify Java
java -version

# 2. Verify ADB
adb version

# 3. Verify Gradle (or wrapper)
gradle --version
# Or for wrapper:
.\gradlew.bat --version

# 4. Verify Git
git --version

# 5. Check device connection
adb devices

# 6. Check device details
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
```

### Expected Outputs

**All checks passed:**
```
✓ Java 17 installed
✓ ADB installed and accessible
✓ Gradle 8.13 installed
✓ Git installed
✓ Android device connected
```

---

## Troubleshooting

### Common Issues

#### 1. "Java is not recognized"

**Problem:** Java not in PATH

**Solution:**
```powershell
# Check JAVA_HOME
echo $env:JAVA_HOME

# Set JAVA_HOME if needed
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot", "User")

# Restart PowerShell and verify
java -version
```

---

#### 2. "ADB not found"

**Problem:** ADB not in PATH

**Solution:**
```powershell
# Check if platform-tools exists
Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Add to PATH manually
$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools"
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
[Environment]::SetEnvironmentVariable("Path", "$currentPath;$adbPath", "User")

# Restart PowerShell
```

---

#### 3. "No devices connected"

**Problem:** Device not detected by ADB

**Solutions:**

1. **Check USB cable:** Use original cable, try different USB port

2. **Install USB drivers:**
   - Honor: [HiSuite](https://consumer.hihonor.com/en/support/hisuite)
   - Generic: [Google USB Driver](https://developer.android.com/studio/run/win-usb)

3. **Restart ADB server:**
```powershell
adb kill-server
adb start-server
adb devices
```

4. **Check device authorization:**
   - Disconnect and reconnect USB
   - Look for authorization prompt on device
   - Enable USB debugging again

---

#### 4. "Gradle wrapper not found"

**Problem:** `gradle-wrapper.jar` missing

**Solution:**
```powershell
# If you have system Gradle
gradle wrapper --gradle-version 8.13

# Or download manually
# https://raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar
# Save to: gradle\wrapper\gradle-wrapper.jar
```

---

#### 5. "BUILD FAILED: SDK not found"

**Problem:** Android SDK location not configured

**Solution:**

Create or edit `local.properties` in project root:

```properties
sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk
```

Or set environment variable:
```powershell
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", "$env:LOCALAPPDATA\Android\Sdk", "User")
```

---

#### 6. "Insufficient permissions"

**Problem:** PowerShell execution policy

**Solution:**
```powershell
# For current session
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

# Or run PowerShell as Administrator
```

---

#### 7. Device shows as "unauthorized"

**Problem:** USB debugging authorization not accepted

**Solution:**
1. Disconnect USB cable
2. On device: Settings → Developer options → Revoke USB debugging authorizations
3. Reconnect USB cable
4. Accept the authorization prompt on device

---

## Next Steps

After completing setup:

### 1. Build the Application

```powershell
cd K:\business\projects_v2\EgyptianAgent

# Debug build
.\gradlew.bat assembleDebug

# Release build
.\gradlew.bat assembleRelease
```

### 2. Deploy to Device

```powershell
# Install debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Or install release APK
adb install -r app\build\outputs\apk\release\app-release.apk
```

### 3. Verify Deployment

```powershell
# Run verification script (Git Bash or WSL)
./scripts/deploy/verify_deployment.sh

# Or check manually
adb shell pm list packages | grep egyptian.agent
```

### 4. Launch the App

```powershell
adb shell am start -n com.egyptian.agent/.MainActivity
```

### 5. Monitor Logs

```powershell
adb logcat -s EgyptianAgent:* FunctionGemma:*
```

---

## Quick Reference

### Environment Variables

| Variable | Value | Purpose |
|----------|-------|---------|
| `JAVA_HOME` | `C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot` | Java location |
| `ANDROID_SDK_ROOT` | `%LOCALAPPDATA%\Android\Sdk` | Android SDK |
| `GRADLE_USER_HOME` | `%USERPROFILE%\.gradle` | Gradle cache |

### PATH Entries

```
%JAVA_HOME%\bin
%LOCALAPPDATA%\Android\Sdk\platform-tools
C:\Gradle\gradle-8.13\bin
```

### Useful Commands

```powershell
# List connected devices
adb devices

# Install APK
adb install -r path\to\app.apk

# Uninstall app
adb uninstall com.egyptian.agent

# Clear app data
adb shell pm clear com.egyptian.agent

# View logs
adb logcat -s EgyptianAgent:*

# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Record screen (30 seconds)
adb shell screenrecord /sdcard/demo.mp4
adb pull /sdcard/demo.mp4
```

---

## Additional Resources

- [EgyptianAgent Deployment Guide](DEPLOYMENT_GUIDE.md)
- [FunctionGemma Deployment Guide](FUNCTIONGEMMA_DEPLOYMENT_GUIDE.md)
- [Production Checklist](../../PRODUCTION_CHECKLIST.md)
- [Android Developer Guide](https://developer.android.com/guide)
- [Gradle User Manual](https://docs.gradle.org/8.13/userguide/userguide.html)

---

**Document Version:** 1.0.0  
**Last Updated:** 2026-03-14  
**Maintained By:** EgyptianAgent Team
