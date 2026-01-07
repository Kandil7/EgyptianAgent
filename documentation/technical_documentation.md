# Technical Documentation for Egyptian Agent

## Architecture Overview

The Egyptian Agent is a voice-controlled assistant designed specifically for Egyptian seniors and visually impaired users. It runs as a system app on Honor X6c devices and operates primarily through voice commands in Egyptian dialect.

## Core Components

### 1. VoiceService
- Main service handling voice recognition and wake word detection
- Manages audio focus and wake locks
- Routes commands to appropriate executors

### 2. WakeWordDetector
- Handles "يا صاحبي"/"يا كبير" wake word detection
- Uses Vosk for offline speech recognition
- Implements Egyptian dialect support

### 3. VoskSTTEngine
- Offline speech-to-text engine
- Processes Egyptian Arabic dialect
- Integrates with custom vocabulary

### 4. EgyptianNormalizer
- Converts Egyptian dialect to standard commands
- Handles common Egyptian expressions and variations
- Extracts entities like contact names and times

### 5. Command Executors
- CallExecutor: Handles phone calls
- WhatsAppExecutor: Manages WhatsApp messaging
- AlarmExecutor: Sets alarms and reminders
- CallLogExecutor: Reads missed calls

### 6. Accessibility Features
- SeniorMode: Simplified interface for seniors
- FallDetector: Automatic fall detection using accelerometer
- VibrationManager: Haptic feedback system

## Permissions and System Integration

The app requires system-level permissions to function as a system app:
- CALL_PHONE: For making phone calls
- RECORD_AUDIO: For voice recognition
- BODY_SENSORS: For fall detection
- WAKE_LOCK: To maintain operation in background
- SYSTEM_ALERT_WINDOW: For overlay functionality

## Egyptian Dialect Support

The system includes comprehensive support for Egyptian Arabic dialect:
- Common expressions and variations
- Egyptian-specific names and places
- Phonetical variations and colloquialisms

## Emergency Features

- Automatic fall detection with accelerometer
- Triple-volume-button press for emergency
- Direct connection to emergency services
- Location sharing in emergencies