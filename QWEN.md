# Egyptian Agent - QWEN Context

## Project Overview

The Egyptian Agent (الوكيل المصري) is a voice-controlled assistant application designed specifically for Egyptian seniors and visually impaired users. It operates completely hands-free using voice commands in Egyptian dialect and runs as a system app on Honor X6c devices.

### Key Features
- **Voice-only interaction** - No screen touch required, all interaction is through voice commands in Egyptian dialect
- **Senior Mode** - Special mode with slower, louder audio and automatic fall detection
- **Smart Emergencies** - Automatic connection to emergency services in critical situations
- **Simple Commands** - Understands Egyptian dialect expressions like "رن على ماما" and "فايتة عليا"
- **Offline Operation** - All core features work without internet connection
- **System-level Access** - Works even when screen is locked

### Target Device
- **Primary Device**: Honor X6c (MediaTek Helio G81 Ultra, 6GB RAM)
- **OS Requirements**: Android 12+
- **Special Requirements**: Bootloader unlocked + Root (Magisk)

## Project Structure

```
EgyptianAgent/
├── app/
│   ├── src/main/
│   │   ├── java/com/egyptian/agent/
│   │   │   ├── core/           # Voice service, wake word detection
│   │   │   ├── stt/            # Speech-to-text engine (Vosk)
│   │   │   ├── executors/      # Command executors (calls, WhatsApp, etc.)
│   │   │   ├── accessibility/  # Senior mode, fall detection
│   │   │   └── utils/          # Utility classes
│   │   ├── assets/             # Vosk models, audio files
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── scripts/
│   ├── install_as_system_app.sh
│   └── honor_battery_fix.sh
├── documentation/
│   ├── user_manual.md
│   ├── technical_documentation.md
│   └── privacy_policy.md
├── build.gradle
├── settings.gradle
├── build.sh
└── README.md
```

## Building and Running

### Prerequisites
- Android SDK with build tools
- ADB (Android Debug Bridge)
- Device with unlocked bootloader and root access

### Build Process
```bash
# Build the application
./build.sh --release --target honor-x6c

# Or for debug builds
./build.sh --debug --target honor-x6c
```

### Installation Process
1. Unlock the device bootloader:
   ```bash
   adb reboot bootloader
   fastboot oem unlock
   ```

2. Install Magisk for root access:
   ```bash
   fastboot flash boot magisk_patched.img
   ```

3. Build and install as system app:
   ```bash
   ./build.sh --release --target honor-x6c
   adb push app/build/outputs/apk/release/EgyptianAgent-release.apk /sdcard/
   adb shell su -c "mkdir -p /system/priv-app/EgyptianAgent"
   adb shell su -c "cp /sdcard/EgyptianAgent-release.apk /system/priv-app/EgyptianAgent/"
   adb shell su -c "chmod 644 /system/priv-app/EgyptianAgent/EgyptianAgent-release.apk"
   ```

4. Apply Honor battery fixes:
   ```bash
   adb push scripts/honor_battery_fix.sh /sdcard/
   adb shell su -c "sh /sdcard/honor_battery_fix.sh"
   ```

5. Reboot the device:
   ```bash
   adb reboot
   ```

## Core Components

### VoiceService
Main service handling voice recognition and wake word detection. Manages audio focus and wake locks, and routes commands to appropriate executors.

### WakeWordDetector
Handles "يا صاحبي"/"يا كبير" wake word detection using Vosk for offline speech recognition with Egyptian dialect support.

### VoskSTTEngine
Offline speech-to-text engine that processes Egyptian Arabic dialect and integrates with custom vocabulary.

### EgyptianNormalizer
Converts Egyptian dialect to standard commands, handles common Egyptian expressions and variations, and extracts entities like contact names and times.

### Command Executors
- **CallExecutor**: Handles phone calls
- **WhatsAppExecutor**: Manages WhatsApp messaging
- **AlarmExecutor**: Sets alarms and reminders
- **CallLogExecutor**: Reads missed calls

### Accessibility Features
- **SeniorMode**: Simplified interface for seniors
- **FallDetector**: Automatic fall detection using accelerometer
- **VibrationManager**: Haptic feedback system

## Key Commands

### Activation
- Say "يا صاحبي" to activate the assistant
- In senior mode, say "يا كبير" to activate

### Making Calls
- "اتصل بأمي" - Call mother
- "كلم بابا" - Call father
- "رن على ماما" - Call mother
- "اتصل بـ [name]" - Call any contact

### WhatsApp Messages
- "ابعت واتساب لـ [name]" - Send WhatsApp message
- "قول لـ [name] إن [message]" - Send specific message

### Setting Alarms
- "نبهني بكرة الصبح" - Set alarm for tomorrow morning
- "انبهني بعد ساعة" - Set alarm for 1 hour from now
- "ذكرني [time]" - Set reminder for specific time

### Reading Missed Calls
- "قولي المكالمات الفايتة" - Read missed calls
- "شوفلي الفايتة" - Check missed calls

## Senior Mode Features

### Activating Senior Mode
- Say "يا كبير، شغل وضع كبار السن"
- Or press the senior mode button in the app

### Senior Mode Benefits
- Slower, clearer speech
- Simplified command set
- Double confirmation for actions
- Automatic fall detection
- Emergency assistance

### Emergency Features
- Automatic fall detection
- Triple-volume-button press for emergency
- Direct connection to emergency services
- Location sharing in emergencies

## Development Conventions

### Code Style
- Java/Kotlin for Android development
- Egyptian dialect support in all user-facing strings
- Comprehensive error handling and logging
- Memory optimization for 6GB RAM devices

### Testing
- Unit tests for core functionality
- Integration tests for voice commands
- Device-specific testing on Honor X6c

### Architecture
- Android service architecture for background operation
- Offline-first approach for voice processing
- System app permissions for deep integration
- Modular component design for maintainability

## Permissions Required

- RECORD_AUDIO: For voice command recognition
- CALL_PHONE: To make phone calls
- READ_CONTACTS: To access contact names and numbers
- BODY_SENSORS: For fall detection using accelerometer
- SYSTEM_ALERT_WINDOW: For overlay functionality
- WAKE_LOCK: To maintain operation in background

## Privacy and Security

- All voice processing happens offline on the device
- No personal data is sent to external servers
- Call logs and contacts are only accessed locally
- All data is encrypted on the device
- Minimal data access is used for functionality