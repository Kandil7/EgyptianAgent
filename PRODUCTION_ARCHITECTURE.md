# Egyptian Agent - Production Architecture Document

## Executive Summary

The Egyptian Agent is a production-grade, offline-first voice assistant designed specifically for Egyptian seniors and visually impaired users. This document details the complete system architecture, module boundaries, and implementation specifications.

---

## 1. System Overview

### 1.1 Target Specifications
- **Device**: Honor X6c (MediaTek Helio G81 Ultra, 6GB RAM)
- **Android Version**: 12+ (API 31+)
- **Languages**: Egyptian Arabic (primary), English (secondary)
- **Privacy**: 100% offline processing, no data leaves device
- **Response Time**: <2.5s end-to-end
- **Memory Usage**: <500MB RAM
- **Battery Drain**: <5% per hour

### 1.2 Core Capabilities
```
┌─────────────────────────────────────────────────────────────────┐
│                    EGYPTIAN AGENT SYSTEM                       │
├─────────────────────────────────────────────────────────────────┤
│  Wake Word Detection    │  "يا صاحبي" / "يا كبير"              │
│  Speech-to-Text         │  Whisper.cpp + Vosk fallback         │
│  Intent Classification  │  Llama 3.2 3B + Rule-based hybrid    │
│  System Integration     │  VoiceInteractionService + Root      │
│  Command Execution      │  Calls, WhatsApp, Alarms, Settings   │
│  Text-to-Speech         │  Offline TTS with Egyptian dialect   │
│  Emergency Features     │  Fall detection, SOS triggers        │
│  Senior Mode            │  Accessibility optimizations         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Architecture Diagram

### 2.1 High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         USER INTERFACE LAYER                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │   Overlay   │  │  Senior UI  │  │ Emergency   │  │  Settings   │   │
│  │   (Siri)    │  │   (Large)   │  │    UI       │  │    Panel    │   │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      SERVICE ORCHESTRATION LAYER                        │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │              VoiceInteractionService (System-Level)             │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │   │
│  │  │  WakeWordMgr    │  │  AudioFocusMgr  │  │  LifecycleMgr   │ │   │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                   VoiceService (Foreground)                     │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        AI PROCESSING LAYER                              │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    Speech-to-Text Pipeline                      │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │   │
│  │  │  whisper.cpp │  │   Vosk STT   │  │  Fallback    │          │   │
│  │  │  (Primary)   │  │  (Fallback)  │  │  (Pattern)   │          │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘          │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │               Intent Classification Pipeline                    │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │   │
│  │  │  Llama 3.2   │  │  TFLite      │  │  Rule-Based  │          │   │
│  │  │  3B Q4_K_M   │  │  Classifier  │  │  + Regex     │          │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘          │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        EXECUTOR LAYER                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │ Communication│  │   Settings   │  │    Apps      │                 │
│  │  Controller  │  │  Controller  │  │  Controller  │                 │
│  └──────────────┘  └──────────────┘  └──────────────┘                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                 │
│  │    Alarm     │  │  Emergency   │  │   System     │                 │
│  │  Controller  │  │  Controller  │  │  Controller  │                 │
│  └──────────────┘  └──────────────┘  └──────────────┘                 │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      SYSTEM INTEGRATION LAYER                           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │              Android System APIs + Root Commands                │   │
│  │  Telephony │ Contacts │ WhatsApp │ Settings │ AlarmManager     │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Data Flow Architecture

```
User Voice Command
       │
       ▼
┌──────────────────┐
│  Wake Word       │◄── Porcupine / Vosk
│  Detection       │
└──────────────────┘
       │
       ▼
┌──────────────────┐
│  Audio Capture   │◄── AudioRecord (16kHz mono)
└──────────────────┘
       │
       ▼
┌──────────────────┐
│  whisper.cpp     │◄── Primary ASR
│  ASR             │
└──────────────────┘
       │
       ▼
┌──────────────────┐
│  Egyptian        │◄── Dialect normalization
│  Normalizer      │
└──────────────────┘
       │
       ▼
┌──────────────────┐
│  Llama 3.2 3B    │◄── Intent classification
│  Intent Engine   │
└──────────────────┘
       │
       ▼
┌──────────────────┐
│  Intent Router   │◄── Route to executor
└──────────────────┘
       │
       ▼
┌──────────────────┐
│  Command         │◄── Execute action
│  Executor        │
└──────────────────┘
       │
       ▼
┌──────────────────┐
│  TTS Response    │◄── Speak confirmation
└──────────────────┘
```

---

## 3. Module Specifications

### 3.1 Wake Word Detection Module

**Location**: `app/src/main/java/com/egyptian/agent/wakeword/`

**Components**:
```
wakeword/
├── PorcupineWakeWordDetector.java    # Primary wake word engine
├── VoskWakeWordDetector.java         # Fallback wake word engine
├── WakeWordManager.java              # Unified wake word manager
├── WakeWordConfig.java               # Configuration class
└── WakeWordCallback.java             # Callback interface
```

**Specifications**:
- Wake Words: "يا صاحبي" (Ya Sahbi), "يا كبير" (Ya Kabeer)
- Detection Latency: <200ms
- False Positive Rate: <1 per hour
- Battery Impact: <3% per hour
- Sample Rate: 16kHz mono

**Implementation**:
```java
public interface WakeWordDetector {
    void start();
    void stop();
    void setCallback(WakeWordCallback callback);
    boolean isListening();
    void destroy();
}

public interface WakeWordCallback {
    void onWakeWordDetected(String wakeWord);
    void onError(Exception error);
}
```

### 3.2 Speech-to-Text (ASR) Module

**Location**: `app/src/main/java/com/egyptian/agent/asr/`

**Components**:
```
asr/
├── WhisperASREngine.java             # Primary whisper.cpp engine
├── VoskASREngine.java                # Fallback Vosk engine
├── ASRManager.java                   # Unified ASR manager
├── AudioRecorder.java                # Audio capture utility
├── AudioProcessor.java               # Audio preprocessing
└── ASRResult.java                    # ASR result container
```

**Specifications**:
- Primary Engine: whisper.cpp (ggml-base.en / multilingual)
- Fallback Engine: Vosk (vosk-model-small-ar)
- Sample Rate: 16kHz
- Audio Format: PCM 16-bit mono
- Latency: <1s for 5s audio
- Egyptian Dialect Accuracy: >90%

**Native Bindings**:
```cpp
// whisper_native.cpp
extern "C" {
    jint initWhisper(JNIEnv* env, jobject, jstring modelPath);
    jstring transcribeNative(JNIEnv* env, jobject, jfloatArray audioData);
    void unloadWhisper(JNIEnv* env, jobject);
}
```

### 3.3 Intent Classification (NLU) Module

**Location**: `app/src/main/java/com/egyptian/agent/nlu/`

**Components**:
```
nlu/
├── LlamaNLUClassifier.java           # Llama 3.2 3B classifier
├── TFLiteClassifier.java             # Lightweight TFLite classifier
├── RuleBasedClassifier.java          # Rule-based + regex
├── EgyptianNormalizer.java           # Dialect normalization
├── IntentRouter.java                 # Intent routing logic
├── IntentResult.java                 # Intent result container
└── IntentType.java                   # Intent type enum
```

**Intent Types**:
```java
public enum IntentType {
    CALL_CONTACT,           // Make phone call
    SEND_WHATSAPP,          // Send WhatsApp message
    SEND_VOICE_MESSAGE,     // Send voice note
    SET_ALARM,              // Set alarm/reminder
    READ_TIME,              // Read current time
    READ_MISSED_CALLS,      // Read missed calls
    TOGGLE_WIFI,            // Toggle WiFi
    TOGGLE_BLUETOOTH,       // Toggle Bluetooth
    OPEN_APP,               // Open application
    EMERGENCY,              // Emergency trigger
    SENIOR_ASSIST,          // Senior-specific help
    UNKNOWN                 // Unknown intent
}
```

**Classification Pipeline**:
```
1. Egyptian Dialect Normalization
   ↓
2. Llama 3.2 3B Classification (if confidence > 0.85)
   ↓
3. TFLite Classifier (if Llama unavailable)
   ↓
4. Rule-Based Fallback (regex patterns)
   ↓
5. Intent Result with entities
```

### 3.4 LLM Integration Module

**Location**: `app/src/main/java/com/egyptian/agent/llm/`

**Components**:
```
llm/
├── LlamaEngine.java                # Main Llama 3.2 engine
├── LlamaConfig.java                # Llama configuration
├── ModelLoader.java                # Model loading utility
├── PromptBuilder.java              # Prompt template builder
├── Tokenizer.java                  # Token management
└── InferenceRunner.java            # Inference execution
```

**Specifications**:
- Model: Llama 3.2 3B Q4_K_M (1.64GB)
- Context Size: 2048 tokens
- Threads: 4 (optimized for Helio G81 Ultra)
- Inference Time: <2s for 128 tokens
- Memory Usage: <300MB during inference

**Native Bindings**:
```cpp
// llama_native.cpp
extern "C" {
    jint initLlama(JNIEnv* env, jobject, jstring modelPath, jint threads);
    jstring inferNative(JNIEnv* env, jobject, jstring prompt, jint maxTokens);
    void unloadLlama(JNIEnv* env, jobject);
}
```

### 3.5 System Integration Module

**Location**: `app/src/main/java/com/egyptian/agent/service/`

**Components**:
```
service/
├── EgyptianAgentService.java       # VoiceInteractionService
├── VoiceInteractionSession.java    # Voice interaction session
├── SystemController.java           # System-level controller
├── RootCommandExecutor.java        # Root command execution
├── PermissionManager.java          # Permission management
└── ServiceConfig.java              # Service configuration
```

**VoiceInteractionService**:
```xml
<service
    android:name=".service.EgyptianAgentService"
    android:permission="android.permission.BIND_VOICE_INTERACTION"
    android:exported="true">
    <intent-filter>
        <action android:name="android.service.voice.VoiceInteractionService" />
    </intent-filter>
    <meta-data
        android:name="android.voice_interaction"
        android:resource="@xml/voice_interaction_service" />
</service>
```

### 3.6 Executor Layer

**Location**: `app/src/main/java/com/egyptian/agent/executor/`

**Components**:
```
executor/
├── CommandExecutor.java            # Base executor interface
├── CommunicationController.java    # Calls, SMS, WhatsApp
├── SettingsController.java         # WiFi, Bluetooth, Settings
├── AppsController.java             # App launching
├── AlarmController.java            # Alarms, reminders
├── EmergencyController.java        # Emergency features
├── SystemController.java           # System-level commands
└── ExecutorResult.java             # Execution result
```

**Command Interface**:
```java
public interface CommandExecutor {
    ExecutorResult execute(IntentResult intent);
    boolean canExecute(IntentType type);
    void cancel();
}
```

### 3.7 Text-to-Speech Module

**Location**: `app/src/main/java/com/egyptian/agent/tts/`

**Components**:
```
tts/
├── TTSEngine.java                  # Main TTS engine
├── EgyptianTTSProcessor.java       # Egyptian dialect processor
├── SeniorTTSConfig.java            # Senior mode TTS config
├── TTSVoice.java                   # Voice configuration
└── TTSCallback.java                # TTS callback
```

**Specifications**:
- Engine: Google TTS (offline) / RHVoice
- Language: Arabic (Egyptian dialect)
- Speech Rate: 0.8x (senior mode), 1.0x (normal)
- Pitch: Normal
- Volume: Enhanced for seniors

### 3.8 UI Components Module

**Location**: `app/src/main/java/com/egyptian/agent/ui/`

**Components**:
```
ui/
├── VoiceOverlayView.java           # Siri-like bubble overlay
├── SeniorModeActivity.java         # Senior mode UI
├── EmergencyActivity.java          # Emergency UI
├── SettingsActivity.java           # Settings panel
├── OverlayService.java             # Overlay management
└── UIConfig.java                   # UI configuration
```

---

## 4. Performance Specifications

### 4.1 Response Time Budget

| Stage | Target | Maximum |
|-------|--------|---------|
| Wake Word Detection | <200ms | <500ms |
| Audio Capture | <100ms | <200ms |
| ASR (Whisper) | <800ms | <1500ms |
| Intent Classification | <500ms | <1000ms |
| Command Execution | <300ms | <500ms |
| TTS Response | <200ms | <400ms |
| **Total** | **<2.1s** | **<4.1s** |

### 4.2 Memory Budget

| Component | Typical | Peak |
|-----------|---------|------|
| Wake Word Detector | 20MB | 50MB |
| ASR Engine | 100MB | 200MB |
| Llama 3.2 3B | 200MB | 350MB |
| TTS Engine | 30MB | 50MB |
| UI Components | 50MB | 100MB |
| System Overhead | 50MB | 100MB |
| **Total** | **450MB** | **850MB** |

### 4.3 Battery Budget

| Component | Drain/Hour |
|-----------|------------|
| Wake Word Detection | 2-3% |
| Background Service | 1% |
| ASR Processing | 0.5% per use |
| Llama Inference | 1% per use |
| **Total (idle)** | **3-4%** |
| **Total (active)** | **<5%** |

---

## 5. Security & Privacy

### 5.1 Data Protection
- **100% Offline Processing**: No data leaves device
- **No Audio Storage**: Real-time processing, immediate deletion
- **Encrypted Models**: Model files encrypted at rest
- **Secure Permissions**: Minimal necessary permissions

### 5.2 Permission Model
```xml
<!-- Core Permissions -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.READ_CONTACTS" />

<!-- System Permissions -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.BIND_VOICE_INTERACTION" />

<!-- Optional Root Permissions -->
<uses-permission android:name="android.permission.DEVICE_POWER" />
<uses-permission android:name="android.permission.MODIFY_PHONE_STATE" />
```

### 5.3 Security Measures
- Input validation on all commands
- Rate limiting on sensitive operations
- Confirmation dialogs for critical actions
- Secure model storage with encryption
- Anti-tampering checks

---

## 6. Error Handling & Recovery

### 6.1 Error Categories

| Category | Handling Strategy |
|----------|-------------------|
| ASR Failure | Fallback to Vosk → Pattern matching |
| Llama Failure | Fallback to TFLite → Rule-based |
| Permission Denied | Graceful degradation |
| Low Memory | Model unloading, GC trigger |
| Service Crash | Auto-restart with backoff |

### 6.2 Recovery Mechanisms
```java
public class ErrorHandler {
    public static void handleASRError(Context ctx, Exception e) {
        // Fallback chain
        if (WhisperASREngine.isAvailable()) {
            switchToVosk();
        } else if (PatternMatcher.isAvailable()) {
            usePatternMatching();
        } else {
            notifyUser("تعذر فهم الأمر");
        }
    }
    
    public static void handleLlamaError(Context ctx, Exception e) {
        // Fallback chain
        if (TFLiteClassifier.isAvailable()) {
            switchToTFLite();
        } else if (RuleBasedClassifier.isAvailable()) {
            useRuleBased();
        } else {
            notifyUser("حدث خطأ في المعالجة");
        }
    }
}
```

---

## 7. Testing Strategy

### 7.1 Test Categories

| Test Type | Coverage | Tools |
|-----------|----------|-------|
| Unit Tests | 80%+ | JUnit, Mockito |
| Integration Tests | Core flows | Espresso |
| Egyptian Dialect Tests | 100% commands | Custom suite |
| Performance Tests | Response time | Android Profiler |
| Battery Tests | Drain rate | Battery Historian |

### 7.2 Egyptian Dialect Test Commands

```java
public class EgyptianDialectTestSuite {
    // Call commands
    "اتصل بأمي"
    "كلم بابا"
    "رن على ماما"
    
    // WhatsApp commands
    "ابعت واتساب لـ أحمد"
    "قول لـ سارة إنى هتأخر"
    
    // Alarm commands
    "نبهني بكرة الصبح"
    "ذكرني بعد ساعة"
    
    // Emergency commands
    "يا نجدة"
    "استغاثة"
    "مش قادر"
}
```

---

## 8. Deployment Architecture

### 8.1 Build Pipeline

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Source    │────▶│    Build    │────▶│    Test     │
│   Code      │     │   (Gradle)  │     │   Suite     │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                               ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Deploy to  │◀────│   Release   │◀────│   Sign &    │
│   Device    │     │    APK      │     │  Package    │
└─────────────┘     └─────────────┘     └─────────────┘
```

### 8.2 Deployment Modes

| Mode | Description | Requirements |
|------|-------------|--------------|
| Debug | Development testing | ADB, USB debugging |
| Release | Production APK | Signing key |
| System App | Deep integration | Root, system partition |
| VoiceInteraction | Default assistant | User configuration |

---

## 9. Monitoring & Observability

### 9.1 Metrics Collection

```java
public class MetricsCollector {
    // Performance metrics
    void recordInferenceTime(long durationMs);
    void recordASRLatency(long durationMs);
    void recordMemoryUsage(long bytes);
    
    // Quality metrics
    void recordIntentConfidence(float confidence);
    void recordUserFeedback(boolean positive);
    
    // Error metrics
    void recordError(String category, Exception e);
}
```

### 9.2 Logging Strategy

| Level | Purpose | Destination |
|-------|---------|-------------|
| ERROR | Critical failures | Logcat + File |
| WARN | Recoverable issues | Logcat |
| INFO | Normal operations | Logcat (debug) |
| DEBUG | Detailed tracing | Logcat (debug) |

---

## 10. Configuration Management

### 10.1 Build Configuration

```gradle
android {
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt')
            buildConfigField "boolean", "USE_LLAMA_CPP", "true"
            buildConfigField "boolean", "USE_WHISPER", "true"
        }
    }
}
```

### 10.2 Runtime Configuration

```java
public class AgentConfig {
    public static final String WAKE_WORD_PRIMARY = "يا صاحبي";
    public static final String WAKE_WORD_SENIOR = "يا كبير";
    public static final float CONFIDENCE_THRESHOLD = 0.85f;
    public static final int MAX_RESPONSE_TOKENS = 128;
    public static final boolean SENIOR_MODE_DEFAULT = false;
}
```

---

## 11. Appendix

### 11.1 Glossary

| Term | Definition |
|------|------------|
| ASR | Automatic Speech Recognition |
| NLU | Natural Language Understanding |
| TTS | Text-to-Speech |
| Q4_K_M | 4-bit quantization (K-quants Medium) |
| VoiceInteractionService | Android system service for voice assistants |

### 11.2 References

- [llama.cpp](https://github.com/ggerganov/llama.cpp)
- [whisper.cpp](https://github.com/ggerganov/whisper.cpp)
- [Vosk ASR](https://github.com/alphacep/vosk-android)
- [Android VoiceInteractionService](https://developer.android.com/reference/android/service/voice/VoiceInteractionService)

---

*Document Version: 1.0*
*Last Updated: March 2026*
*Author: Egyptian Agent Team*
