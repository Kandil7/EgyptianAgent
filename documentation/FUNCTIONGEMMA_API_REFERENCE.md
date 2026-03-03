# FunctionGemma-270M-IT API Reference

**Version:** 1.0.0  
**Last Updated:** March 3, 2026  
**Status:** Production Ready  
**Author:** EgyptianAgent Development Team

---

## Table of Contents

1. [Overview](#overview)
2. [FunctionGemmaEngine Class](#functiongemmaengine-class)
3. [FunctionGemmaIntentEngine Class](#functiongemmaintentengine-class)
4. [FunctionCallSchema](#functioncallschema)
5. [IntentResult](#intentresult)
6. [Configuration Classes](#configuration-classes)
7. [Callback Interfaces](#callback-interfaces)
8. [Code Examples](#code-examples)
9. [Error Handling](#error-handling)

---

## Overview

This API reference documents the public interfaces for the FunctionGemma-270M-IT integration in EgyptianAgent. All classes are thread-safe and designed for Android applications.

### Package Structure

```
com.egyptianagent.functiongemma
├── FunctionGemmaEngine.java
├── FunctionGemmaIntentEngine.java
├── FunctionGemmaConfig.java
├── FunctionCallSchema.java
├── FunctionCallResult.java
├── IntentResult.java
├── IntentType.java
├── Entity.java
├── PerformanceMetrics.java
└── callbacks
    ├── TokenCallback.java
    ├── IntentCallback.java
    └── FunctionCallback.java
```

### Threading Model

| Class | Thread-Safe | Blocking Methods | Async Available |
|-------|-------------|------------------|-----------------|
| FunctionGemmaEngine | Yes | `generateResponse()`, `callFunction()` | Yes |
| FunctionGemmaIntentEngine | Yes | `classifyIntent()`, `processEgyptianSpeech()` | Yes |
| FunctionGemmaConfig | Yes | None | N/A |

---

## FunctionGemmaEngine Class

The core inference engine for FunctionGemma-270M-IT.

### Constructors

```java
/**
 * Creates a FunctionGemmaEngine with default configuration.
 * 
 * @param context Android application context
 * @throws ModelLoadException if model fails to load
 */
public FunctionGemmaEngine(Context context);

/**
 * Creates a FunctionGemmaEngine with custom configuration.
 * 
 * @param context Android application context
 * @param config Custom configuration options
 * @throws ModelLoadException if model fails to load
 */
public FunctionGemmaEngine(Context context, FunctionGemmaConfig config);
```

#### Usage Example

```java
// Default configuration
FunctionGemmaEngine engine = new FunctionGemmaEngine(context);

// Custom configuration
FunctionGemmaConfig config = new FunctionGemmaConfig()
    .setModelPath("models/functiongemma-custom.Q4_K_M.gguf")
    .setMaxTokens(512)
    .setTemperature(0.1f)
    .setNumThreads(2);

FunctionGemmaEngine engine = new FunctionGemmaEngine(context, config);
```

### Methods

#### generateResponse

```java
/**
 * Generates a response for the given user query.
 * 
 * @param userQuery The input text (Egyptian Arabic supported)
 * @param maxTokens Maximum tokens to generate
 * @return Generated response text
 * @throws InferenceException if inference fails
 * @throws TimeoutException if inference times out (>5s)
 */
public String generateResponse(String userQuery, int maxTokens);

/**
 * Generates a response with default max tokens (256).
 */
public String generateResponse(String userQuery);
```

**Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `userQuery` | String | Required | Input text in any language (Egyptian Arabic optimized) |
| `maxTokens` | int | 256 | Maximum tokens to generate (1-2048) |

**Returns:** Generated response text

**Throws:**

| Exception | Condition |
|-----------|-----------|
| `InferenceException` | Model inference fails |
| `TimeoutException` | Response takes >5 seconds |
| `IllegalStateException` | Engine not ready (`!isReady()`) |

**Example:**

```java
FunctionGemmaEngine engine = new FunctionGemmaEngine(context);
String response = engine.generateResponse("ما هي عاصمة مصر؟", 128);
// Response: "عاصمة مصر هي القاهرة."
```

---

#### generateResponseAsync

```java
/**
 * Asynchronously generates a response for the given user query.
 * 
 * @param userQuery The input text
 * @param callback Callback for response or error
 */
public void generateResponseAsync(String userQuery, TokenCallback callback);

/**
 * Asynchronously generates a response with custom max tokens.
 */
public void generateResponseAsync(String userQuery, int maxTokens, TokenCallback callback);
```

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `userQuery` | String | Input text |
| `maxTokens` | int | Maximum tokens to generate |
| `callback` | TokenCallback | Callback for results |

**Callback Methods:**

```java
public interface TokenCallback {
    /**
     * Called when a token is generated (streaming).
     */
    void onToken(String token);
    
    /**
     * Called when generation is complete.
     */
    void onComplete(String fullResponse);
    
    /**
     * Called when an error occurs.
     */
    void onError(Exception error);
}
```

**Example:**

```java
engine.generateResponseAsync("احكي لي نكتة", new TokenCallback() {
    @Override
    public void onToken(String token) {
        // Stream tokens to UI
        textView.append(token);
    }
    
    @Override
    public void onComplete(String fullResponse) {
        Log.d("Gemma", "Complete: " + fullResponse);
    }
    
    @Override
    public void onError(Exception error) {
        Log.e("Gemma", "Error", error);
    }
});
```

---

#### callFunction

```java
/**
 * Calls a function with the given arguments.
 * 
 * @param functionName Name of the function to call
 * @param args Function arguments
 * @return Function call result
 * @throws FunctionNotFoundException if function doesn't exist
 * @throws InvalidArgumentsException if arguments are invalid
 */
public FunctionCallResult callFunction(
    String functionName, 
    Map<String, Object> args
);
```

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `functionName` | String | Name from FunctionCallSchema |
| `args` | Map<String, Object> | Function arguments |

**Returns:** `FunctionCallResult` containing execution result

**Example:**

```java
Map<String, Object> args = new HashMap<>();
args.put("contact_name", "ماما");

FunctionCallResult result = engine.callFunction("call_contact", args);

if (result.isSuccess()) {
    Log.d("Gemma", "Call initiated successfully");
} else {
    Log.e("Gemma", "Call failed: " + result.getErrorMessage());
}
```

---

#### callFunctionAsync

```java
/**
 * Asynchronously calls a function.
 * 
 * @param functionName Name of the function to call
 * @param args Function arguments
 * @param callback Callback for result
 */
public void callFunctionAsync(
    String functionName, 
    Map<String, Object> args,
    FunctionCallback callback
);
```

**Example:**

```java
Map<String, Object> args = Map.of(
    "contact_name", "أحمد",
    "message", "هتأخر شوية"
);

engine.callFunctionAsync("send_whatsapp", args, new FunctionCallback() {
    @Override
    public void onSuccess(FunctionCallResult result) {
        Log.d("Gemma", "WhatsApp sent");
    }
    
    @Override
    public void onError(Exception error) {
        Log.e("Gemma", "Error", error);
    }
});
```

---

#### State Methods

```java
/**
 * Checks if the engine is ready for inference.
 * 
 * @return true if model is loaded and ready
 */
public boolean isReady();

/**
 * Checks if the engine is currently loading.
 * 
 * @return true if model is loading
 */
public boolean isLoading();

/**
 * Gets the current loading progress (0.0 to 1.0).
 * 
 * @return Loading progress
 */
public float getLoadingProgress();

/**
 * Destroys the engine and releases resources.
 * Must be called when engine is no longer needed.
 */
public void destroy();
```

**Usage:**

```java
FunctionGemmaEngine engine = new FunctionGemmaEngine(context);

// Wait for ready
while (!engine.isReady()) {
    Thread.sleep(100);
}

// Use engine...

// Cleanup
engine.destroy();
```

---

#### getPerformanceMetrics

```java
/**
 * Gets performance metrics for the engine.
 * 
 * @return PerformanceMetrics object
 */
public PerformanceMetrics getPerformanceMetrics();
```

**PerformanceMetrics Structure:**

```java
public class PerformanceMetrics {
    private long modelLoadTimeMs;      // Time to load model
    private long totalInferenceTimeMs; // Total inference time
    private int totalInferences;       // Number of inferences
    private long avgInferenceTimeMs;   // Average inference time
    private long p95InferenceTimeMs;   // 95th percentile
    private long p99InferenceTimeMs;   // 99th percentile
    private long peakMemoryUsageBytes; // Peak memory usage
    private long currentMemoryUsageBytes; // Current memory usage
    
    // Getters...
}
```

**Example:**

```java
PerformanceMetrics metrics = engine.getPerformanceMetrics();
Log.d("Gemma", String.format(
    "Avg inference: %dms, P95: %dms, Memory: %dMB",
    metrics.getAvgInferenceTimeMs(),
    metrics.getP95InferenceTimeMs(),
    metrics.getCurrentMemoryUsageBytes() / (1024 * 1024)
));
```

---

## FunctionGemmaIntentEngine Class

Specialized engine for Egyptian Arabic intent classification.

### Constructor

```java
/**
 * Creates a FunctionGemmaIntentEngine.
 * 
 * @param context Android application context
 * @throws ModelLoadException if model fails to load
 */
public FunctionGemmaIntentEngine(Context context);

/**
 * Creates a FunctionGemmaIntentEngine with custom configuration.
 */
public FunctionGemmaIntentEngine(Context context, FunctionGemmaConfig config);
```

### Methods

#### processEgyptianSpeech

```java
/**
 * Processes Egyptian Arabic speech and returns intent.
 * Combines ASR transcription with intent classification.
 * 
 * @param audioPath Path to audio file
 * @return IntentResult with classified intent and entities
 * @throws ASRException if speech recognition fails
 * @throws InferenceException if intent classification fails
 */
public IntentResult processEgyptianSpeech(String audioPath);
```

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `audioPath` | String | Path to audio file (WAV, 16kHz, mono) |

**Returns:** `IntentResult` with intent type, confidence, and entities

**Example:**

```java
FunctionGemmaIntentEngine engine = new FunctionGemmaIntentEngine(context);
IntentResult result = engine.processEgyptianSpeech("/sdcard/recordings/voice_command.wav");

if (result.getIntentType() == IntentType.CALL_CONTACT) {
    String contact = result.getEntity("contact_name");
    // Initiate call to contact
}
```

---

#### classifyIntent

```java
/**
 * Classifies the intent of the given text.
 * 
 * @param text Input text (Egyptian Arabic supported)
 * @return IntentResult with classified intent and entities
 * @throws InferenceException if classification fails
 */
public IntentResult classifyIntent(String text);
```

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `text` | String | Input text |

**Returns:** `IntentResult` object

**Example:**

```java
IntentResult result = engine.classifyIntent("اتصل بماما");

IntentType intent = result.getIntentType();      // CALL_CONTACT
float confidence = result.getConfidence();       // 0.97
String contact = result.getEntity("contact_name"); // "ماما"
```

---

#### classifyIntentAsync

```java
/**
 * Asynchronously classifies the intent of the given text.
 * 
 * @param text Input text
 * @param callback Callback for result
 */
public void classifyIntentAsync(String text, IntentCallback callback);
```

**Callback Interface:**

```java
public interface IntentCallback {
    /**
     * Called when intent classification is complete.
     */
    void onIntent(IntentResult result);
    
    /**
     * Called when an error occurs.
     */
    void onError(Exception error);
}
```

**Example:**

```java
engine.classifyIntentAsync("ابعت واتساب لأحمد", new IntentCallback() {
    @Override
    public void onIntent(IntentResult result) {
        if (result.getIntentType() == IntentType.SEND_WHATSAPP) {
            String contact = result.getEntity("contact_name");
            String message = result.getEntity("message");
            
            // Execute WhatsApp send
            sendWhatsApp(contact, message);
        }
    }
    
    @Override
    public void onError(Exception error) {
        showError("Failed to process command");
    }
});
```

---

#### State Methods

```java
/**
 * Checks if the engine is ready.
 */
public boolean isReady();

/**
 * Destroys the engine and releases resources.
 */
public void destroy();
```

---

## FunctionCallSchema

Defines the 16 supported functions for EgyptianAgent.

### Supported Functions

| Function ID | Name | Description | Required Parameters | Optional Parameters |
|-------------|------|-------------|---------------------|---------------------|
| `F001` | `call_contact` | Place a phone call | `contact_name` | - |
| `F002` | `send_whatsapp` | Send WhatsApp message | `contact_name`, `message` | - |
| `F003` | `set_alarm` | Set an alarm | `time` | `label`, `repeat` |
| `F004` | `set_reminder` | Set a reminder | `time`, `description` | - |
| `F005` | `open_app` | Open an application | `app_name` | - |
| `F006` | `close_app` | Close an application | `app_name` | - |
| `F007` | `get_weather` | Get weather forecast | - | `location` |
| `F008` | `calculate` | Perform calculation | `expression` | - |
| `F009` | `set_timer` | Set countdown timer | `duration` | `label` |
| `F010` | `play_music` | Play music | - | `song_name`, `artist` |
| `F011` | `pause_music` | Pause music | - | - |
| `F012` | `skip_track` | Skip to next track | - | - |
| `F013` | `control_volume` | Adjust volume | `direction` | `level` |
| `F014` | `toggle_wifi` | Toggle WiFi | `action` | - |
| `F015` | `toggle_bluetooth` | Toggle Bluetooth | `action` | - |
| `F016` | `emergency_call` | Emergency services | - | - |

### Function Schemas

#### call_contact

```json
{
  "name": "call_contact",
  "description": "Place a phone call to a contact",
  "parameters": {
    "type": "object",
    "required": ["contact_name"],
    "properties": {
      "contact_name": {
        "type": "string",
        "description": "Name of the contact (Egyptian Arabic)"
      }
    }
  },
  "examples": [
    {"input": "اتصل بماما", "output": {"contact_name": "ماما"}},
    {"input": "كلم بابا", "output": {"contact_name": "بابا"}}
  ]
}
```

#### send_whatsapp

```json
{
  "name": "send_whatsapp",
  "description": "Send a WhatsApp message",
  "parameters": {
    "type": "object",
    "required": ["contact_name", "message"],
    "properties": {
      "contact_name": {
        "type": "string",
        "description": "Name of the contact"
      },
      "message": {
        "type": "string",
        "description": "Message content"
      }
    }
  },
  "examples": [
    {
      "input": "ابعت واتساب لأحمد وقوله إنى هتأخر",
      "output": {"contact_name": "أحمد", "message": "إنى هتأخر"}
    }
  ]
}
```

#### set_alarm

```json
{
  "name": "set_alarm",
  "description": "Set an alarm",
  "parameters": {
    "type": "object",
    "required": ["time"],
    "properties": {
      "time": {
        "type": "string",
        "description": "Time for alarm (Egyptian Arabic format)"
      },
      "label": {
        "type": "string",
        "description": "Optional label for the alarm"
      },
      "repeat": {
        "type": "array",
        "items": {"type": "string"},
        "description": "Days to repeat: [sunday, monday, ...]"
      }
    }
  },
  "examples": [
    {
      "input": "اضبط منبه على الساعة 7 الصبح",
      "output": {"time": "7 الصبح"}
    },
    {
      "input": "اضبط منبه على 8 الصبح وسميه شغل",
      "output": {"time": "8 الصبح", "label": "شغل"}
    }
  ]
}
```

### Getting Function Schema

```java
/**
 * Gets the schema for a specific function.
 */
public static FunctionSchema getFunctionSchema(String functionName);

/**
 * Gets all supported function schemas.
 */
public static List<FunctionSchema> getAllFunctionSchemas();

/**
 * Validates arguments against a function schema.
 */
public static boolean validateArguments(
    String functionName, 
    Map<String, Object> args
);
```

---

## IntentResult

Result of intent classification with entity extraction.

### Structure

```java
public class IntentResult {
    private IntentType intentType;      // Classified intent
    private float confidence;           // Confidence score (0.0-1.0)
    private Map<String, Object> entities; // Extracted entities
    private String rawText;             // Original input text
    private String normalizedText;      // Normalized text
    private long processingTimeMs;      // Processing time
    private String modelVersion;        // Model version used
    
    // Getters
    public IntentType getIntentType();
    public float getConfidence();
    public Map<String, Object> getEntities();
    public String getRawText();
    public String getNormalizedText();
    public long getProcessingTimeMs();
    public String getModelVersion();
    
    // Entity access
    public <T> T getEntity(String name);
    public <T> T getEntity(String name, T defaultValue);
    public boolean hasEntity(String name);
    public Set<String> getEntityNames();
    
    // Validation
    public boolean isSuccess();
    public boolean isUnknown();
    
    // Factory methods
    public static IntentResult unknown(String message);
    public static IntentResult error(String message, Exception cause);
}
```

### IntentType Enum

```java
public enum IntentType {
    CALL_CONTACT("call_contact"),
    SEND_WHATSAPP("send_whatsapp"),
    SET_ALARM("set_alarm"),
    SET_REMINDER("set_reminder"),
    OPEN_APP("open_app"),
    CLOSE_APP("close_app"),
    GET_WEATHER("get_weather"),
    CALCULATE("calculate"),
    SET_TIMER("set_timer"),
    PLAY_MUSIC("play_music"),
    PAUSE_MUSIC("pause_music"),
    SKIP_TRACK("skip_track"),
    CONTROL_VOLUME("control_volume"),
    TOGGLE_WIFI("toggle_wifi"),
    TOGGLE_BLUETOOTH("toggle_bluetooth"),
    EMERGENCY_CALL("emergency_call"),
    UNKNOWN("unknown");
    
    private final String functionName;
    
    public String getFunctionName();
    public static IntentType fromFunctionName(String name);
}
```

### Usage Examples

```java
IntentResult result = engine.classifyIntent("اتصل بماما");

// Check intent
if (result.getIntentType() == IntentType.CALL_CONTACT) {
    // Get entities
    String contact = result.getEntity("contact_name");
    
    // Check confidence
    if (result.getConfidence() >= 0.9f) {
        // High confidence - proceed
    }
}

// Check for unknown intent
if (result.isUnknown()) {
    showError("لم أفهم الطلب");
}

// Get all entities
for (String entityName : result.getEntityNames()) {
    Object value = result.getEntity(entityName);
    Log.d("Entity", entityName + " = " + value);
}
```

### Entity Extraction

| Entity Name | Type | Description | Example Values |
|-------------|------|-------------|----------------|
| `contact_name` | String | Contact name | "ماما", "بابا", "أحمد" |
| `message` | String | Message content | "إنى هتأخر", "سلام" |
| `time` | String | Time specification | "7 الصبح", "3 العصر" |
| `date` | String | Date specification | "بكرة", "الجمعة" |
| `app_name` | String | Application name | "الواتساب", "فيسبوك" |
| `location` | String | Location name | "القاهرة", "الإسكندرية" |
| `expression` | String | Math expression | "5 + 3", "100 * 2" |
| `duration` | String | Time duration | "10 دقائق", "ساعة" |
| `action` | String | Toggle action | "on", "off", "toggle" |
| `direction` | String | Volume direction | "up", "down" |
| `level` | Integer | Volume level | 1-10 |
| `song_name` | String | Song name | "يا مسا", "3 دقائق" |
| `artist` | String | Artist name | "محمد عبد الوهاب" |
| `label` | String | Label/description | "شغل", "دواء" |
| `repeat` | List<String> | Repeat days | ["sunday", "monday"] |

---

## Configuration Classes

### FunctionGemmaConfig

```java
public class FunctionGemmaConfig {
    // Model configuration
    private String modelPath;
    private int maxContextLength;
    
    // Inference configuration
    private int maxTokens;
    private float temperature;
    private int topK;
    private float topP;
    
    // Threading
    private int numThreads;
    
    // Memory
    private boolean useMemoryMapping;
    private int memorySizeMB;
    
    // Constructors
    public FunctionGemmaConfig();
    public FunctionGemmaConfig(String modelPath);
    
    // Fluent setters
    public FunctionGemmaConfig setModelPath(String path);
    public FunctionGemmaConfig setMaxContextLength(int length);
    public FunctionGemmaConfig setMaxTokens(int tokens);
    public FunctionGemmaConfig setTemperature(float temp);
    public FunctionGemmaConfig setTopK(int k);
    public FunctionGemmaConfig setTopP(float p);
    public FunctionGemmaConfig setNumThreads(int threads);
    public FunctionGemmaConfig setUseMemoryMapping(boolean use);
    public FunctionGemmaConfig setMemorySizeMB(int size);
    
    // Getters
    public String getModelPath();
    public int getMaxContextLength();
    public int getMaxTokens();
    public float getTemperature();
    public int getTopK();
    public float getTopP();
    public int getNumThreads();
    public boolean isUseMemoryMapping();
    public int getMemorySizeMB();
    
    // Validation
    public void validate() throws ConfigurationException;
    
    // Default configuration
    public static FunctionGemmaConfig getDefault();
    public static FunctionGemmaConfig getOptimizedForLowEnd();
}
```

### Default Values

| Property | Default | Recommended Range |
|----------|---------|-------------------|
| `modelPath` | `models/functiongemma-270m-it.Q4_K_M.gguf` | - |
| `maxContextLength` | 2048 | 512-4096 |
| `maxTokens` | 256 | 1-2048 |
| `temperature` | 0.1 | 0.0-0.3 (function calling) |
| `topK` | 40 | 10-100 |
| `topP` | 0.9 | 0.8-0.95 |
| `numThreads` | 2 | 1-4 |
| `useMemoryMapping` | true | true/false |
| `memorySizeMB` | 512 | 256-1024 |

---

## Callback Interfaces

### TokenCallback

```java
public interface TokenCallback {
    void onToken(String token);
    void onComplete(String fullResponse);
    void onError(Exception error);
}
```

### IntentCallback

```java
public interface IntentCallback {
    void onIntent(IntentResult result);
    void onError(Exception error);
}
```

### FunctionCallback

```java
public interface FunctionCallback {
    void onSuccess(FunctionCallResult result);
    void onError(Exception error);
}
```

### LoadingCallback

```java
public interface LoadingCallback {
    void onProgress(float progress);  // 0.0 to 1.0
    void onComplete();
    void onError(Exception error);
}
```

---

## Code Examples

### Basic Usage

```java
// Initialize engine
FunctionGemmaIntentEngine engine = new FunctionGemmaIntentEngine(context);

// Wait for ready
while (!engine.isReady()) {
    Thread.sleep(100);
}

// Classify intent
IntentResult result = engine.classifyIntent("اتصل بماما");

// Process result
if (result.getIntentType() == IntentType.CALL_CONTACT) {
    String contact = result.getEntity("contact_name");
    makePhoneCall(contact);
}

// Cleanup
engine.destroy();
```

### Async Usage

```java
FunctionGemmaIntentEngine engine = new FunctionGemmaIntentEngine(context);

engine.classifyIntentAsync("ابعت واتساب لأحمد", new IntentCallback() {
    @Override
    public void onIntent(IntentResult result) {
        if (result.getIntentType() == IntentType.SEND_WHATSAPP) {
            String contact = result.getEntity("contact_name");
            String message = result.getEntity("message");
            sendWhatsApp(contact, message);
        }
    }
    
    @Override
    public void onError(Exception error) {
        Log.e("App", "Intent classification failed", error);
    }
});
```

### Direct Function Calling

```java
FunctionGemmaEngine gemma = new FunctionGemmaEngine(context);

// Call contact function
FunctionCallResult callResult = gemma.callFunction("call_contact", 
    Map.of("contact_name", "ماما"));

if (callResult.isSuccess()) {
    Log.d("App", "Call initiated");
} else {
    Log.e("App", "Call failed: " + callResult.getErrorMessage());
}

// Send WhatsApp
FunctionCallResult whatsappResult = gemma.callFunction("send_whatsapp",
    Map.of(
        "contact_name", "أحمد",
        "message", "هتأخر شوية"
    ));
```

### Speech Processing

```java
FunctionGemmaIntentEngine engine = new FunctionGemmaIntentEngine(context);

// Process recorded speech
IntentResult result = engine.processEgyptianSpeech(audioFilePath);

switch (result.getIntentType()) {
    case CALL_CONTACT:
        String contact = result.getEntity("contact_name");
        dialNumber(lookupContact(contact));
        break;
        
    case SET_ALARM:
        String time = result.getEntity("time");
        String label = result.getEntity("label", "منبه");
        setAlarm(parseTime(time), label);
        break;
        
    case OPEN_APP:
        String appName = result.getEntity("app_name");
        openApp(appName);
        break;
        
    default:
        showClarification();
}
```

### Streaming Response

```java
FunctionGemmaEngine engine = new FunctionGemmaEngine(context);

engine.generateResponseAsync("احكي لي قصة قصيرة", new TokenCallback() {
    private StringBuilder fullResponse = new StringBuilder();
    
    @Override
    public void onToken(String token) {
        fullResponse.append(token);
        textView.setText(fullResponse.toString());
    }
    
    @Override
    public void onComplete(String response) {
        Log.d("App", "Story complete: " + response);
    }
    
    @Override
    public void onError(Exception error) {
        showError("Failed to generate story");
    }
});
```

### Performance Monitoring

```java
FunctionGemmaEngine engine = new FunctionGemmaEngine(context);

// After several inferences...
PerformanceMetrics metrics = engine.getPerformanceMetrics();

Log.d("Performance", String.format(
    "Load time: %dms\n" +
    "Avg inference: %dms\n" +
    "P95: %dms\n" +
    "P99: %dms\n" +
    "Memory: %dMB\n" +
    "Total inferences: %d",
    metrics.getModelLoadTimeMs(),
    metrics.getAvgInferenceTimeMs(),
    metrics.getP95InferenceTimeMs(),
    metrics.getP99InferenceTimeMs(),
    metrics.getCurrentMemoryUsageBytes() / (1024 * 1024),
    metrics.getTotalInferences()
));
```

---

## Error Handling

### Exception Hierarchy

```
Exception
├── FunctionGemmaException
│   ├── ModelLoadException
│   ├── InferenceException
│   ├── ConfigurationException
│   └── TimeoutException
├── ASRException
├── FunctionNotFoundException
└── InvalidArgumentsException
```

### Exception Handling

```java
try {
    FunctionGemmaEngine engine = new FunctionGemmaEngine(context);
    IntentResult result = engine.classifyIntent("اتصل بماما");
    
} catch (ModelLoadException e) {
    // Model failed to load
    Log.e("App", "Model load failed", e);
    showModelLoadError();
    
} catch (InferenceException e) {
    // Inference failed
    Log.e("App", "Inference failed", e);
    showRetryOption();
    
} catch (TimeoutException e) {
    // Inference timed out
    Log.w("App", "Inference timed out", e);
    showTimeoutMessage();
    
} catch (FunctionNotFoundException e) {
    // Unknown function
    Log.e("App", "Function not found", e);
    
} catch (InvalidArgumentsException e) {
    // Invalid function arguments
    Log.e("App", "Invalid arguments", e);
    
} catch (Exception e) {
    // Generic error
    Log.e("App", "Unexpected error", e);
}
```

### Error Codes

| Error Code | Exception | Description |
|------------|-----------|-------------|
| `E001` | `ModelLoadException` | Model file not found |
| `E002` | `ModelLoadException` | Invalid model format |
| `E003` | `ModelLoadException` | Insufficient memory |
| `E004` | `InferenceException` | Inference failed |
| `E005` | `TimeoutException` | Operation timed out |
| `E006` | `ConfigurationException` | Invalid configuration |
| `E007` | `FunctionNotFoundException` | Function not found |
| `E008` | `InvalidArgumentsException` | Invalid arguments |
| `E009` | `ASRException` | Speech recognition failed |
| `E010` | `ASRException` | Audio file not found |

---

**Document Status:** ✅ Complete  
**Review Status:** ✅ Approved  
**Next Review:** June 3, 2026
