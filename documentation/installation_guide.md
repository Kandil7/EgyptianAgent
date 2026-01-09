# Installation Guide for Egyptian Agent

## Overview
This guide provides step-by-step instructions for installing the Egyptian Agent as a system app on Honor X6c devices. The application requires root access to function as a system app with full accessibility features.

## Prerequisites

### Hardware Requirements
- Honor X6c device (MediaTek Helio G81 Ultra, 6GB RAM)
- USB cable for connecting to computer
- Computer with Windows, macOS, or Linux

### Software Requirements
- Android SDK Platform Tools (ADB)
- Fastboot
- Magisk (for root access)
- Unlocked bootloader

### Download Required Files
1. Download Android SDK Platform Tools from Google Developer website
2. Download Magisk APK from official Magisk releases
3. Ensure you have the Egyptian Agent APK file

## Pre-Installation Steps

### 1. Enable Developer Options
1. Go to Settings > About Phone
2. Tap "Build Number" 7 times
3. Return to Settings > System > Developer Options
4. Enable "USB Debugging"

### 2. Enable OEM Unlocking
1. Go to Settings > System > Developer Options
2. Enable "OEM Unlocking" option
3. Confirm any prompts that appear

### 3. Install ADB on Your Computer
1. Extract Android SDK Platform Tools to a folder
2. Add the folder to your system PATH (Windows) or create an alias (Mac/Linux)
3. Verify installation: `adb version`

## Installation Process

### Step 1: Unlock Bootloader
1. Power off your Honor X6c
2. Boot into fastboot mode:
   - Hold Volume Down + Power buttons until you see the logo
3. Connect to computer via USB
4. Open terminal/command prompt and navigate to ADB folder
5. Execute the unlock command:
   ```bash
   adb reboot bootloader
   fastboot oem unlock
   ```
6. On the device, use volume keys to select "Unlock" and power button to confirm
7. Wait for the process to complete (device will factory reset)

### Step 2: Install Magisk for Root Access
1. Rename your Magisk APK file to `magisk.apk`
2. Push the file to your device:
   ```bash
   adb push magisk.apk /sdcard/
   ```
3. Boot into recovery mode:
   ```bash
   fastboot boot path/to/recovery.img
   ```
   (If you don't have a custom recovery, you may need to temporarily flash TWRP)
4. In recovery, install Magisk from /sdcard/magisk.apk
5. Reboot the device

### Step 3: Patch Boot Image with Magisk (Alternative Method)
If you prefer not to flash a custom recovery:
1. Rename your downloaded Magisk APK to `Magisk.apk`
2. Use Magisk Manager to patch your boot image:
   - Open Magisk Manager on your device
   - Go to Install → Select and Patch a File
   - Select your boot image file
   - This creates a patched image file in Downloads
3. Transfer the patched image to your computer
4. Flash the patched image:
   ```bash
   adb reboot bootloader
   fastboot flash boot path/to/patched_boot.img
   fastboot reboot
   ```

### Step 4: Build and Install Egyptian Agent
1. Clone or download the Egyptian Agent source code
2. Navigate to the project directory
3. Build the application:
   ```bash
   ./gradlew assembleRelease
   ```
4. Push the APK to your device:
   ```bash
   adb push app/build/outputs/apk/release/EgyptianAgent-release.apk /sdcard/
   ```

### Step 5: Install as System App
1. Gain root access via ADB:
   ```bash
   adb shell
   su
   ```
2. Create system app directory:
   ```bash
   mkdir -p /system/priv-app/EgyptianAgent
   ```
3. Copy APK to system directory:
   ```bash
   cp /sdcard/EgyptianAgent-release.apk /system/priv-app/EgyptianAgent/
   ```
4. Set proper permissions:
   ```bash
   chmod 644 /system/priv-app/EgyptianAgent/EgyptianAgent-release.apk
   ```
5. Set SELinux context (if needed):
   ```bash
   chcon u:object_r:system_file:s0 /system/priv-app/EgyptianAgent/EgyptianAgent-release.apk
   ```

### Step 6: Grant Required Permissions
1. Reboot your device:
   ```bash
   reboot
   ```
2. After reboot, open Settings > Apps > Egyptian Agent
3. Grant all requested permissions:
   - Microphone (required for voice recognition)
   - Phone (required for calls)
   - Contacts (required for contact access)
   - Body Sensors (required for fall detection)
   - Location (required for emergency location sharing)
   - SMS (required for emergency notifications)

### Step 7: Apply Honor Battery Optimizations
1. Push the battery optimization script:
   ```bash
   adb push scripts/honor_battery_fix.sh /sdcard/
   ```
2. Execute the script with root privileges:
   ```bash
   adb shell su -c "sh /sdcard/honor_battery_fix.sh"
   ```

### Step 8: Configure Auto-Start
1. Go to Settings > Apps > Special Access > Auto-launch
2. Enable auto-launch for Egyptian Agent
3. Go to Settings > Battery > Power Plan > Apps with Heavy Usage
4. Add Egyptian Agent to protected apps list

## Post-Installation Configuration

### 1. Initial Setup
1. Open the Egyptian Agent app
2. Follow the initial setup wizard
3. Configure emergency contacts
4. Set up guardian notifications (optional)
5. Configure medication reminders (if needed)

### 2. Senior Mode Activation
- To activate senior mode, say "يا كبير، شغل وضع كبار السن"
- Or use the dedicated button in the app interface

### 3. Emergency Contacts Setup
1. Open the app settings
2. Navigate to Emergency Settings
3. Add at least 3 emergency contacts
4. Test the emergency call functionality

## Verification Steps

### 1. Check Service Status
Verify the voice service is running:
```bash
adb shell dumpsys activity services | grep VoiceService
```

### 2. Check Permissions
Verify all permissions are granted:
```bash
adb shell dumpsys package com.egyptian.agent | grep permission
```

### 3. Test Basic Functionality
1. Say "يا صاحبي" to activate the assistant
2. Test a simple command like "اتصل بأمي"
3. Verify the response is audible and correct

### 4. Test Emergency Features
1. Test the emergency activation phrase "يا نجدة"
2. Verify emergency contacts are called
3. Check if SMS notifications are sent

## Troubleshooting

### Common Issues and Solutions

#### Issue: App not appearing after system installation
- **Solution**: Reboot the device. If still not visible, check if the APK was copied correctly to the system directory.

#### Issue: Voice service not starting
- **Solution**: Check if the app has all required permissions, especially microphone and phone permissions.

#### Issue: Wake word not detected
- **Solution**: Check microphone permissions and ensure no other app is using the microphone exclusively.

#### Issue: Battery drain
- **Solution**: Verify that the battery optimization script ran correctly and that the app is in the protected apps list.

#### Issue: Emergency contacts not receiving notifications
- **Solution**: Verify SMS permissions are granted and emergency contacts are properly configured.

### Logs for Debugging
Check application logs:
```bash
adb logcat -s "EgyptianAgent:*"
```

Check system service logs:
```bash
adb logcat | grep -i "VoiceService\|Egyptian"
```

## Uninstallation

### Complete Removal
If you need to uninstall the system app:
1. Use the uninstall script provided:
   ```bash
   adb shell su -c "/sdcard/uninstall_egyptian_agent.sh"
   ```
2. Or manually remove:
   ```bash
   adb shell su
   rm -rf /system/priv-app/EgyptianAgent
   pm uninstall com.egyptian.agent
   ```

## Support

For technical support:
- Check the application logs using ADB
- Verify all installation steps were completed
- Ensure the device meets all requirements
- Contact the development team through the in-app feedback system

## Notes
- The application requires root access to function as a system app
- Removing root access may cause the application to malfunction
- Regular system updates may require reinstallation of the system app
- Always backup your data before performing system-level installations