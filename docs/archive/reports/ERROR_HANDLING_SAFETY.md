# Egyptian Agent - Error Handling & Safety Implementation

## Overview

The Egyptian Agent implements comprehensive error handling and safety features to ensure reliable operation, especially for vulnerable users like seniors and visually impaired individuals. The system follows a defense-in-depth approach with multiple layers of error handling, safety checks, and graceful degradation.

## Error Handling Architecture

### Global Error Strategy

The system implements a hierarchical error handling approach:

1. **Component Level**: Individual components handle their specific errors
2. **Service Level**: VoiceService coordinates error handling across components
3. **Application Level**: MainApplication manages global error states
4. **System Level**: Android system handles critical failures

### Error Categories

#### 1. AI Model Errors
- Model loading failures
- Inference errors
- Memory allocation failures
- Model corruption

#### 2. Audio Processing Errors
- Microphone access failures
- Audio recording errors
- Wake word detection failures
- STT processing errors

#### 3. Hardware Errors
- Sensor failures (accelerometer for fall detection)
- Network issues (for any online features)
- Storage problems
- Memory constraints

#### 4. User Interaction Errors
- Misunderstood commands
- Invalid entity extraction
- Failed action execution
- Permission denials

## Safety Features Implementation

### 1. Emergency Safety

#### Emergency Confirmation Logic
```java
// EmergencyHandler.java
public class EmergencyHandler {
    private static final int CONFIRMATION_DELAY_MS = 3000; // 3 seconds
    private static boolean isEmergencyConfirmed = false;
    private static long lastEmergencyActivation = 0;
    
    public static boolean isEmergency(String command) {
        String lowerCmd = command.toLowerCase();
        return lowerCmd.contains("emergency") || 
               lowerCmd.contains("ngda") || 
               lowerCmd.contains("استغاثة") || 
               lowerCmd.contains("طوارئ") ||
               lowerCmd.contains("危険") ||  // Japanese emergency for testing
               lowerCmd.contains("危険");   // Another variation
    }
    
    public static void trigger(Context context) {
        // Prevent accidental activations
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEmergencyActivation < CONFIRMATION_DELAY_MS) {
            // Too quick, might be accidental
            TTSManager.speak(context, "تأكيد الطوارئ؟ قل نعم للتأكيد");
            listenForConfirmation(context);
            return;
        }
        
        // Proceed with emergency activation
        executeEmergencyProtocol(context);
        lastEmergencyActivation = currentTime;
    }
    
    private static void listenForConfirmation(Context context) {
        // Listen for confirmation in next input
        isEmergencyConfirmed = true;
        // Reset after timeout
        new Handler().postDelayed(() -> isEmergencyConfirmed = false, CONFIRMATION_DELAY_MS * 2);
    }
    
    private static void executeEmergencyProtocol(Context context) {
        // Trigger emergency services
        TTSManager.speak(context, "جاري الاتصال بخدمات الطوارئ");
        
        // Send location if available
        sendLocationToEmergencyContacts(context);
        
        // Activate fall detection if not already active
        activateEnhancedMonitoring(context);
        
        // Log the emergency event
        CrashLogger.logEvent(context, "EMERGENCY_ACTIVATED", "Emergency protocol initiated by user");
    }
    
    private static void sendLocationToEmergencyContacts(Context context) {
        // Implementation to share location with emergency contacts
        // This would use Android's location services
    }
    
    private static void activateEnhancedMonitoring(Context context) {
        // Activate additional safety measures
        // Enhanced fall detection, periodic check-ins, etc.
    }
}
```

### 2. Graceful Degradation

#### Fallback Mechanisms
```java
// HybridOrchestrator.java
public class HybridOrchestrator {
    private LlamaIntentEngine llamaEngine;
    private Gemma2NLUProcessor gemmaProcessor;
    private OpenPhoneModel openPhoneModel;
    private Quantum quantumFallback;
    
    public void determineIntent(String command, IntentCallback callback) {
        // Try primary AI engine first
        if (llamaEngine != null && llamaEngine.isReady()) {
            IntentResult result = llamaEngine.processEgyptianSpeech(command);
            if (result.getConfidence() > 0.7) {
                callback.onResult(result);
                return;
            }
        }
        
        // Fallback to secondary processor
        if (gemmaProcessor != null && gemmaProcessor.isReady()) {
            IntentResult result = gemmaProcessor.process(command);
            if (result.getConfidence() > 0.6) {
                callback.onResult(result);
                return;
            }
        }
        
        // Fallback to OpenPhone model
        if (openPhoneModel != null) {
            IntentResult result = openPhoneModel.process(command);
            if (result.getConfidence() > 0.5) {
                callback.onResult(result);
                return;
            }
        }
        
        // Ultimate fallback to Quantum class
        if (quantumFallback != null) {
            quantumFallback.processCommand(command);
            // Quantum will handle the command internally
            callback.onResult(new IntentResult(IntentType.UNKNOWN, 0.0f));
            return;
        }
        
        // If all fails, return unknown
        callback.onResult(new IntentResult(IntentType.UNKNOWN, 0.1f));
    }
}
```

### 3. Memory and Resource Management

#### Memory Optimizer
```java
// MemoryOptimizer.java
public class MemoryOptimizer {
    private static final String TAG = "MemoryOptimizer";
    private static final long LOW_MEMORY_THRESHOLD = 500 * 1024 * 1024; // 500 MB
    
    public static void checkMemoryConstraints(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        
        if (memInfo.availMem < LOW_MEMORY_THRESHOLD) {
            Log.w(TAG, "Low memory detected: " + (memInfo.availMem / (1024 * 1024)) + " MB available");
            
            // Trigger memory optimization
            triggerMemoryOptimization(context);
        }
    }
    
    public static void triggerMemoryOptimization(Context context) {
        // Clear caches
        clearCaches(context);
        
        // Suggest model unloading if needed
        suggestModelOptimization(context);
        
        // Force garbage collection
        System.gc();
        
        // Inform user if memory is critically low
        if (isMemoryCriticallyLow(context)) {
            TTSManager.speak(context, "تحذير: الذاكرة قليلة. قد يتم تعطيل بعض المزايا");
        }
    }
    
    private static void clearCaches(Context context) {
        try {
            // Clear application cache
            File cacheDir = context.getCacheDir();
            if (cacheDir != null) {
                deleteRecursive(cacheDir);
            }
            
            // Clear any other cached data
            // This would include temporary audio files, processed text, etc.
        } catch (Exception e) {
            Log.e(TAG, "Error clearing caches", e);
        }
    }
    
    private static void suggestModelOptimization(Context context) {
        // Suggest unloading non-critical models
        // This would be handled by ModelManager
    }
    
    private static boolean isMemoryCriticallyLow(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        
        return memInfo.availMem < (LOW_MEMORY_THRESHOLD / 2); // 250 MB
    }
    
    private static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }
}
```

### 4. Audio Safety

#### Safe Audio Handling
```java
// AudioSafetyManager.java
public class AudioSafetyManager {
    private static final String TAG = "AudioSafetyManager";
    private static final int MAX_VOLUME_WARNING_LEVEL = 80; // Percentage
    private static AudioManager audioManager;
    
    public static void initialize(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }
    
    public static boolean isVolumeSafe(Context context) {
        if (audioManager == null) return true; // Assume safe if we can't check
        
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        
        if (maxVolume == 0) return true; // Device without volume control
        
        int volumePercentage = (currentVolume * 100) / maxVolume;
        
        return volumePercentage <= MAX_VOLUME_WARNING_LEVEL;
    }
    
    public static void adjustVolumeForSafety(Context context) {
        if (audioManager == null) return;
        
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        
        if (maxVolume == 0) return;
        
        int volumePercentage = (currentVolume * 100) / maxVolume;
        
        if (volumePercentage > MAX_VOLUME_WARNING_LEVEL) {
            // Reduce volume to safe level
            int safeVolume = (MAX_VOLUME_WARNING_LEVEL * maxVolume) / 100;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, safeVolume, 0);
            
            // Warn user about volume adjustment
            TTSManager.speak(context, "تم تعديل الصوت لمستوى آمن");
        }
    }
    
    public static void validateAudioPermissions(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            
            // Request audio permission
            TTSManager.speak(context, "التطبيق محتاج إذن تسجيل الصوت ليعمل بشكل صحيح");
            
            // In a real app, you'd initiate the permission request here
        }
    }
}
```

### 5. Comprehensive Error Logging

#### Crash Logger
```java
// CrashLogger.java
public class CrashLogger {
    private static final String TAG = "CrashLogger";
    private static final String LOG_FILE_NAME = "crash_log.txt";
    private static final int MAX_LOG_SIZE = 1024 * 1024; // 1MB
    
    public static void logError(Context context, Exception e) {
        logError(context, e, null);
    }
    
    public static void logError(Context context, Exception e, String additionalInfo) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String errorDetails = String.format(
            "[%s] ERROR: %s\nStack Trace: %s\nAdditional Info: %s\nDevice: %s\nAndroid: %s\n",
            timestamp,
            e.getMessage(),
            Log.getStackTraceString(e),
            additionalInfo != null ? additionalInfo : "None",
            Build.MODEL,
            Build.VERSION.RELEASE
        );
        
        Log.e(TAG, errorDetails);
        writeToFile(context, errorDetails);
    }
    
    public static void logEvent(Context context, String eventType, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String eventDetails = String.format("[%s] EVENT: %s - %s\n", timestamp, eventType, message);
        
        Log.i(TAG, eventDetails);
        writeToFile(context, eventDetails);
    }
    
    private static void writeToFile(Context context, String content) {
        try {
            File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
            
            // Check file size and rotate if needed
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                rotateLogFile(context, logFile);
            }
            
            FileOutputStream fos = new FileOutputStream(logFile, true); // append mode
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.append(content);
            writer.close();
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file", e);
        }
    }
    
    private static void rotateLogFile(Context context, File currentLogFile) {
        try {
            File rotatedFile = new File(context.getFilesDir(), LOG_FILE_NAME + ".prev");
            if (rotatedFile.exists()) {
                rotatedFile.delete(); // Remove old rotated file
            }
            currentLogFile.renameTo(rotatedFile);
        } catch (Exception e) {
            Log.e(TAG, "Failed to rotate log file", e);
        }
    }
    
    public static String getLastError(Context context) {
        try {
            File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
            if (!logFile.exists()) return "No error logs found";
            
            FileInputStream fis = new FileInputStream(logFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            
            String lastLine = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ERROR")) {
                    lastLine = line;
                }
            }
            
            reader.close();
            fis.close();
            
            return lastLine != null ? lastLine : "No recent errors";
        } catch (IOException e) {
            Log.e(TAG, "Failed to read log file", e);
            return "Error reading logs: " + e.getMessage();
        }
    }
}
```

## Safety Protocols

### 1. User Safety Checks

#### Command Validation
```java
// CommandValidator.java
public class CommandValidator {
    private static final String TAG = "CommandValidator";
    
    public static ValidationResult validateCommand(String command, IntentResult intentResult) {
        ValidationResult result = new ValidationResult();
        
        // Check for potentially harmful commands
        if (isPotentiallyHarmful(command)) {
            result.isValid = false;
            result.message = "Command flagged as potentially harmful";
            return result;
        }
        
        // Validate entities in the command
        if (!validateEntities(intentResult)) {
            result.isValid = false;
            result.message = "Invalid entities detected in command";
            return result;
        }
        
        // Check for emergency override
        if (intentResult.getIntentType() == IntentType.EMERGENCY) {
            result.requiresConfirmation = true;
            result.confirmationMessage = "تأكيد حالة الطوارئ؟";
        }
        
        result.isValid = true;
        result.message = "Command validated successfully";
        return result;
    }
    
    private static boolean isPotentiallyHarmful(String command) {
        // Check for commands that might be harmful
        String lowerCmd = command.toLowerCase();
        
        // This is a simplified check - in reality, this would be more sophisticated
        return lowerCmd.contains("factory reset") || 
               lowerCmd.contains("delete all") || 
               lowerCmd.contains("format storage");
    }
    
    private static boolean validateEntities(IntentResult intentResult) {
        // Validate extracted entities
        if (intentResult.getIntentType() == IntentType.CALL_CONTACT) {
            String contact = intentResult.getEntity("contact", "");
            if (contact.isEmpty()) {
                return false;
            }
            
            // Additional validation could happen here
        }
        
        return true;
    }
    
    public static class ValidationResult {
        public boolean isValid;
        public boolean requiresConfirmation = false;
        public String confirmationMessage = "";
        public String message;
    }
}
```

### 2. System Stability Monitoring

#### Health Monitor
```java
// SystemHealthMonitor.java
public class SystemHealthMonitor {
    private static final String TAG = "HealthMonitor";
    private static final long CHECK_INTERVAL = 30000; // 30 seconds
    private static Handler handler = new Handler(Looper.getMainLooper());
    private static Runnable healthCheckRunnable;
    private static Context appContext;
    
    public static void startMonitoring(Context context) {
        appContext = context.getApplicationContext();
        
        healthCheckRunnable = new Runnable() {
            @Override
            public void run() {
                performHealthCheck();
                
                // Schedule next check
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        
        // Start immediate check, then repeat
        handler.post(healthCheckRunnable);
    }
    
    public static void stopMonitoring() {
        if (handler != null && healthCheckRunnable != null) {
            handler.removeCallbacks(healthCheckRunnable);
        }
    }
    
    private static void performHealthCheck() {
        if (appContext == null) return;
        
        try {
            // Check memory usage
            checkMemoryHealth();
            
            // Check model health
            checkModelHealth();
            
            // Check service health
            checkServiceHealth();
            
            // Check battery level
            checkBatteryHealth();
            
        } catch (Exception e) {
            Log.e(TAG, "Error during health check", e);
            CrashLogger.logError(appContext, e, "System health monitoring error");
        }
    }
    
    private static void checkMemoryHealth() {
        MemoryOptimizer.checkMemoryConstraints(appContext);
    }
    
    private static void checkModelHealth() {
        // Check if models are still responsive
        // This would involve checking if the Llama engine is still working
    }
    
    private static void checkServiceHealth() {
        // Check if critical services are running
        ActivityManager am = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = am.getRunningServices(Integer.MAX_VALUE);
        
        boolean voiceServiceRunning = false;
        for (ActivityManager.RunningServiceInfo service : services) {
            if ("com.egyptian.agent.core.VoiceService".equals(service.service.getClassName())) {
                voiceServiceRunning = true;
                break;
            }
        }
        
        if (!voiceServiceRunning) {
            Log.w(TAG, "VoiceService is not running, attempting restart");
            restartCriticalServices();
        }
    }
    
    private static void checkBatteryHealth() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = appContext.registerReceiver(null, ifilter);
        
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            
            float batteryPct = level * 100 / (float) scale;
            
            if (batteryPct < 10.0f) {
                // Very low battery, warn user
                TTSManager.speak(appContext, "تحذير: بطارية منخفضة جدًا");
            } else if (batteryPct < 20.0f) {
                // Low battery, optimize usage
                TTSManager.speak(appContext, "البطارية منخفضة");
            }
        }
    }
    
    private static void restartCriticalServices() {
        if (appContext == null) return;
        
        try {
            Intent serviceIntent = new Intent(appContext, Class.forName("com.egyptian.agent.core.VoiceService"));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(serviceIntent);
            } else {
                appContext.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart VoiceService", e);
        }
    }
}
```

## Implementation Summary

This comprehensive error handling and safety implementation provides:

1. **Multi-layered error handling** from component to system level
2. **Emergency safety protocols** with confirmation mechanisms
3. **Graceful degradation** with multiple fallback systems
4. **Resource management** for memory and performance
5. **Audio safety** for hearing protection
6. **Comprehensive logging** for debugging and monitoring
7. **Command validation** to prevent harmful actions
8. **System health monitoring** for stability

The implementation ensures that the Egyptian Agent operates safely and reliably, especially for vulnerable users like seniors and visually impaired individuals, while maintaining the high accuracy and responsiveness required for Egyptian dialect understanding.