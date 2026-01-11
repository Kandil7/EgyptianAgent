# Egyptian Agent - Production Deployment Guide

## Overview
This document provides detailed instructions for deploying the Egyptian Agent as a production system application on Honor X6c devices. The application requires root access to function as a system app with full capabilities.

## Prerequisites

### Device Requirements
- Honor X6c (MediaTek Helio G81 Ultra, 6GB RAM)
- Android 12+ 
- Bootloader unlocked
- Root access (Magisk recommended)

### Software Requirements
- Android SDK with platform-tools (ADB)
- Gradle build system
- Device drivers installed on deployment machine

## Deployment Process

### 1. Prepare the Device
1. Unlock the device bootloader:
   ```bash
   adb reboot bootloader
   fastboot oem unlock
   ```

2. Install Magisk for root access:
   ```bash
   fastboot flash boot magisk_patched.img
   ```

3. Boot the device and complete Magisk installation
4. Verify root access: `adb shell su -c "id"` (should show uid=0)

### 2. Build the Application
1. Navigate to the project directory
2. Run the build script:
   ```bash
   ./gradlew assembleRelease
   ```

### 3. Deploy as System App
1. Run the deployment script:
   ```bash
   ./deploy_production.sh
   ```
   
   This script will:
   - Verify prerequisites
   - Build the release APK
   - Backup any existing installation
   - Deploy the APK to the system partition
   - Set proper permissions
   - Optimize for Honor X6c
   - Reboot the device

### 4. Manual Deployment (Alternative)
If using the script is not possible, follow these manual steps:

1. Build the APK:
   ```bash
   ./gradlew assembleRelease
   ```

2. Push the APK to the device:
   ```bash
   adb push app/build/outputs/apk/release/EgyptianAgent-production.apk /sdcard/
   ```

3. Install as system app:
   ```bash
   adb shell su -c "mkdir -p /system/priv-app/EgyptianAgent"
   adb shell su -c "cp /sdcard/EgyptianAgent-production.apk /system/priv-app/EgyptianAgent/"
   adb shell su -c "chmod 644 /system/priv-app/EgyptianAgent/EgyptianAgent-production.apk"
   ```

4. Apply optimizations:
   ```bash
   adb shell su -c "dumpsys deviceidle whitelist +com.egyptian.agent"
   adb shell cmd appops set com.egyptian.agent RUN_IN_BACKGROUND allow
   ```

5. Reboot the device:
   ```bash
   adb reboot
   ```

## Post-Deployment Verification

### 1. Verify Installation
After the device reboots, verify the installation:
```bash
adb shell pm list packages | grep egyptian.agent
```

### 2. Check System App Status
```bash
adb shell pm list packages -s | grep egyptian.agent
```

### 3. Verify Permissions
```bash
adb shell dumpsys package com.egyptian.agent | grep permission
```

### 4. Test Core Functionality
1. Activate the assistant by saying "يا صاحبي"
2. Test basic commands like "اتصل بماما"
3. Verify voice recognition and response

## Security Considerations

### Certificate Pinning
The application implements certificate pinning for secure communication. Ensure the correct certificates are in place for production.

### App Integrity Verification
The application performs runtime integrity checks. Any modification to the APK will cause the app to refuse to run.

### Root Detection
The application detects rooted devices and adjusts security measures accordingly.

## Troubleshooting

### Common Issues

#### App Not Starting After Installation
- Ensure the device was rebooted after installation
- Verify the APK has proper permissions (644)
- Check system logs: `adb logcat | grep -i egyptian`

#### Voice Recognition Not Working
- Verify microphone permissions are granted
- Check that the assistant service is running: `adb shell dumpsys activity services | grep VoiceService`
- Ensure the wake word model is properly loaded

#### System App Installation Failed
- Verify root access: `adb shell su -c "id"`
- Check available storage space
- Ensure the system partition is not read-only

### Recovery Process
If deployment fails or causes issues:

1. Boot into recovery mode
2. Restore from backup if available
3. Or manually remove the app:
   ```bash
   adb shell su -c "rm -rf /system/priv-app/EgyptianAgent"
   ```

## Maintenance and Updates

### OTA Updates
The application supports secure OTA updates through the `SecureOTAUpdater` class. Updates are verified using certificate pinning and checksums.

### Log Collection
Collect logs for debugging:
```bash
adb logcat -v time > egyptian_agent_logs.txt
```

### Performance Monitoring
The application includes performance monitoring. Access performance data through:
```bash
adb shell dumpsys batterystats | grep egyptian.agent
```

## Production Environment Requirements

### Device Configuration
- Screen timeout: Disabled or set to maximum
- Power management: Adaptive battery disabled for the app
- Doze mode: Whitelisted
- App optimization: Disabled

### Network Requirements
- Primary: Offline operation (all core features)
- Secondary: Internet for OTA updates and fallback services
- Security: Certificate pinning for all network communication

## Rollback Procedure

If a critical issue is discovered:

1. Use the backup created during deployment
2. Or manually remove the system app:
   ```bash
   adb shell su -c "rm -rf /system/priv-app/EgyptianAgent"
   ```
3. Reboot the device
4. Install a previous stable version if needed

## Support and Monitoring

### Analytics
Privacy-compliant analytics are collected using the `PrivacyCompliantAnalytics` class. Data is stored locally and can be retrieved for analysis.

### Crash Reporting
Crash logs are stored securely and can be accessed through the `CrashLogger` class.

### Performance Metrics
Performance metrics are continuously monitored by the `PerformanceMonitor` class and can be accessed programmatically.

---

For additional support, contact the development team through the established communication channels.