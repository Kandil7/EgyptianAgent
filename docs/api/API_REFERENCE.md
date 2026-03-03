# Egyptian Agent API Reference

## Table of Contents

1. [Core Services](#core-services)
2. [Intent Classification](#intent-classification)
3. [Command Executors](#command-executors)
4. [Utilities](#utilities)
5. [Models](#models)

---

## Core Services

### VoiceService

Main voice interaction service that handles the complete voice pipeline.

```java
// Starting the voice service
Intent intent = new Intent(context, VoiceService.class);
context.startService(intent);

// Stopping the voice service
context.stopService(intent);
```

**Methods:**
- `void onCreate()` - Initializes ASR, NLU, and TTS engines
- `int onStartCommand(Intent intent, int flags, int startId)` - Handles start commands
- `void onDestroy()` - Cleanup resources

### TTSManager

Text-to-speech manager with Egyptian dialect support.

```java
// Initialize
TTSManager.initialize(context);

// Speak text
TTSManager.speak(context, "اهلا بك يا صاحبي");

// Senior mode
TTSManager.setSeniorSettings(context);

// Stop speaking
TTSManager.stop();
```

**Methods:**
- `static void initialize(Context context)` - Initialize TTS engine
- `static void speak(Context context, String text)` - Speak text
- `static void setSeniorSettings(Context context)` - Enable senior mode
- `static void setSpeechRate(Context context, float rate)` - Set speech rate
- `static void stop()` - Stop current speech

---

## Intent Classification

### RuleBasedClassifier

Fast rule-based intent classifier using regex patterns.

```java
RuleBasedClassifier classifier = new RuleBasedClassifier();
IntentResult result = classifier.classify("اتصل بماما");

// Result contains:
result.getIntent();        // IntentType.CALL_CONTACT
result.getConfidence();    // 0.95
result.getSlots();         // Map of extracted entities
```

### TFLiteIntentClassifier

ML-based intent classifier using TensorFlow Lite.

```java
TFLiteIntentClassifier classifier = new TFLiteIntentClassifier(context);
ClassificationResult result = classifier.classify("شغل الواي فاي");

// Result contains:
result.intent;      // IntentType.TOGGLE_WIFI
result.confidence;  // 0.87
```

### IntentType Enum

Supported intent types:

```java
enum IntentType {
    CALL_CONTACT,      // Make a phone call
    SEND_WHATSAPP,    // Send WhatsApp message
    SET_ALARM,         // Set alarm/reminder
    TOGGLE_WIFI,      // Toggle WiFi
    TOGGLE_BLUETOOTH, // Toggle Bluetooth
    OPEN_APP,          // Open an application
    SEND_SMS,          // Send SMS
    READ_TIME,         // Read current time
    GREETING,          // Greeting/reeting
    EMERGENCY,         // Emergency situation
    CONVERSATION,      // General conversation
    THANK_YOU,         // Thanks
    GOODBYE,           // Farewell
    UNKNOWN            // Unrecognized intent
}
```

---

## Command Executors

### CallExecutor

Executes phone call commands.

```java
CallExecutor executor = new CallExecutor(context);

// Make call
Intent intent = new IntentResult(IntentType.CALL_CONTACT);
intent.putSlot("contact", "ماما");
ExecutorResult result = executor.execute(intent);

// Emergency call
result = executor.executeEmergencyCall("123");
```

**Methods:**
- `ExecutorResult execute(NLUResult intent)` - Execute call command
- `ExecutorResult executeEmergencyCall(String phoneNumber)` - Emergency call

### WhatsAppExecutor

Sends WhatsApp messages.

```java
WhatsAppExecutor executor = new WhatsAppExecutor(context);

IntentResult intent = new IntentResult(IntentType.SEND_WHATSAPP);
intent.putSlot("contact", "أحمد");
intent.putSlot("message", "السلام عليكم");

ExecutorResult result = executor.execute(intent);
```

### AlarmExecutor

Sets alarms and reminders.

```java
AlarmExecutor executor = new AlarmExecutor(context);

IntentResult intent = new IntentResult(IntentType.SET_ALARM);
intent.putSlot("time", "08:00");
intent.putSlot("label", "موعد الدوا");

ExecutorResult result = executor.execute(intent);
```

### EmergencyHandler

Handles emergency situations.

```java
// Trigger emergency
EmergencyHandler.trigger(context, false);

// With automatic location
EmergencyHandler.triggerWithLocation(context);
```

---

## Utilities

### ArabicContactMatcher

Contact resolution with Arabic name matching.

```java
ArabicContactMatcher matcher = new ArabicContactMatcher(context);

// Find contact
ContactEntry contact = matcher.findContact("ماما");

// Get multiple matches for disambiguation
List<ContactEntry> matches = matcher.findContacts("أحمد");
```

### MemoryOptimizer

Memory management utilities.

```java
// Check available memory
boolean hasMemory = MemoryOptimizer.hasEnoughMemory(2000);

// Get available memory in MB
long availableMB = MemoryOptimizer.getAvailableMemoryMB();

// Check low memory state
boolean isLow = MemoryOptimizer.isLowMemory();

// Trigger optimization
MemoryOptimizer.triggerMemoryOptimizations(context);
```

### WhatsAppService

WhatsApp integration.

```java
WhatsAppService service = new WhatsAppService();

// Check if WhatsApp is installed
boolean installed = service.isWhatsAppInstalled(context);

// Send message
service.sendWhatsAppMessage(context, "0123456789", "مرحبا");
```

---

## Models

### ModelManager

Manages AI model loading and lifecycle.

```java
// Load Whisper model
ModelManager.loadWhisperModel(context);

// Load Llama model
ModelManager.loadLlamaModel(context);

// Check model status
ModelStatus status = ModelManager.getModelStatus(ModelType.WHISPER);

// Unload model
ModelManager.unloadModel(ModelType.LLAMA);
```

### WakeWordDetector

Wake word detection service.

```java
// Start detection
WakeWordDetector detector = new WakeWordDetector(context);
detector.startDetection();

// Set callback
detector.setCallback(new WakeWordCallback() {
    @Override
    public void onWakeWordDetected(String wakeWord) {
        // Handle wake word
    }
});

// Stop detection
detector.stopDetection();
```

---

## Data Classes

### IntentResult

```java
IntentResult {
    IntentType intent;
    float confidence;
    Map<String, Slot> slots;
    String originalText;
}
```

### ExecutorResult

```java
ExecutorResult {
    ExecutionStatus status;  // SUCCESS, FAILED, PERMISSION_DENIED
    String message;
    Map<String, Object> data;
}
```

### ContactEntry

```java
ContactEntry {
    long id;
    String name;
    String phoneNumber;
    String normalizedNumber;
    String nickname;
}
```

---

*Last Updated: 2026-03-03*
*Version: 1.1.0*
