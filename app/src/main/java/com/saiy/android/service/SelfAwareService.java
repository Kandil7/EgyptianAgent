package com.saiy.android.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.egyptian.agent.stt.VoskSTTEngine;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.hybrid.HybridOrchestrator;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.CrashLogger;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.executors.EmergencyHandler;
import com.egyptian.agent.core.WakeWordDetector;
import com.egyptian.agent.system.SystemPrivilegeManager;

import java.util.Arrays;

/**
 * Saiy-PS SelfAwareService adapted for Egyptian Agent
 * This service serves as the core voice processing service
 * with Egyptian dialect support and senior-focused features
 */
public class SelfAwareService extends Service {
    private static final String TAG = "SelfAwareService";
    
    private VoskSTTEngine egyptianASR;
    private EgyptianTTS egyptianTTS;
    private WakeWordDetector wakeWordDetector;
    private HybridOrchestrator hybridOrchestrator;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SelfAwareService created");

        // Initialize system privileges
        if (!SystemPrivilegeManager.hasSystemPrivileges()) {
            Log.w(TAG, "System privileges not available, requesting...");
            SystemPrivilegeManager.requestSystemPrivileges(this);
        } else {
            Log.i(TAG, "System privileges already available");
        }

        // Load Egyptian language models
        loadEgyptianLanguageModels();

        // Setup Egyptian wake words
        setupEgyptianWakeWords();

        // Initialize hybrid orchestrator
        initializeHybridOrchestrator();

        // Enable senior mode if configured
        enableSeniorModeIfConfigured();
    }

    private void loadEgyptianLanguageModels() {
        try {
            // Initialize Vosk with Egyptian model
            egyptianASR = new VoskSTTEngine(this);
            Log.i(TAG, "Egyptian ASR engine initialized successfully");
            
            // Initialize Egyptian TTS
            TTSManager.initialize(this);
            Log.i(TAG, "Egyptian TTS initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load Egyptian language models", e);
            CrashLogger.logError(this, e);
        }
    }

    private void setupEgyptianWakeWords() {
        // Define Egyptian wake words
        java.util.List<String> egyptianWakeWords = Arrays.asList(
            "يا حكيم",    // Hey Wise One
            "يا كبير",    // Hey Elder (for senior mode)
            "ساعدني"     // Help me (for emergencies)
        );

        // Initialize wake word detector with Egyptian variations
        wakeWordDetector = new WakeWordDetector(this, () -> {
            handleWakeWordDetected();
        });

        Log.i(TAG, "Egyptian wake words configured: " + egyptianWakeWords);
    }

    private void initializeHybridOrchestrator() {
        try {
            hybridOrchestrator = new HybridOrchestrator(this);
            Log.i(TAG, "Hybrid Orchestrator initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Hybrid Orchestrator", e);
            CrashLogger.logError(this, e);
        }
    }

    private void enableSeniorModeIfConfigured() {
        // Check if senior mode should be enabled
        if (SeniorMode.isEnabled()) {
            Log.i(TAG, "Senior mode is enabled");
            TTSManager.setSeniorSettings(this);
        }
    }

    private void handleWakeWordDetected() {
        Log.d(TAG, "Egyptian wake word detected");

        // Provide haptic feedback for visually impaired users
        // VibrationManager.vibrateShort(this);

        // Senior mode special handling
        if (SeniorMode.isEnabled()) {
            TTSManager.speak(this, "قول يا كبير");
        } else {
            TTSManager.speak(this, "أوامرك؟");
        }

        // Start listening for command
        egyptianASR.startListening(result -> {
            handleUserCommand(result);
        });
    }

    private void handleUserCommand(String command) {
        Log.i(TAG, "User command: " + command);

        // Emergency detection first (safety critical)
        if (EmergencyHandler.isEmergency(command)) {
            handleEmergencyCommand();
            return;
        }

        // Senior mode restrictions
        if (SeniorMode.isEnabled() && !SeniorMode.isCommandAllowed(command)) {
            handleSeniorRestrictedCommand(command);
            return;
        }

        // Use the hybrid orchestrator to determine intent
        if (hybridOrchestrator != null) {
            // Normalize the command using Egyptian dialect processing
            String normalizedCommand = EgyptianNormalizer.normalize(command);

            // Use the hybrid orchestrator to determine intent
            hybridOrchestrator.determineIntent(normalizedCommand, result -> {
                // Process the result on the main thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    processIntentResult(result, command);
                });
            });
        } else {
            // Fallback to original processing if orchestrator is not available
            handleUnknownCommand(command);
        }
    }

    private void processIntentResult(IntentResult result, String originalCommand) {
        Log.i(TAG, "Processing intent result: " + result.getIntentType() + " with confidence: " + result.getConfidence());

        // Check if confidence is too low
        if (result.getConfidence() < 0.5) {
            TTSManager.speak(this, "مش فاهمك كويس. قول الأمر تاني");
            return;
        }

        // Process based on intent type (would connect to executors)
        // Implementation would route to appropriate executors
        switch (result.getIntentType()) {
            case CALL_CONTACT:
                // CallExecutor.handleCommand(this, originalCommand);
                break;
            case SEND_WHATSAPP:
                // WhatsAppExecutor.handleCommand(this, originalCommand);
                break;
            case SET_ALARM:
                // AlarmExecutor.handleCommand(this, originalCommand);
                break;
            case READ_TIME:
                String currentTime = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
                TTSManager.speak(this, "الساعة " + currentTime);
                break;
            case UNKNOWN:
            default:
                handleUnknownCommand(originalCommand);
        }
    }

    private void handleEmergencyCommand() {
        EmergencyHandler.trigger(this);
        // VibrationManager.vibrateEmergency(this);
    }

    private void handleSeniorRestrictedCommand(String command) {
        SeniorMode.handleRestrictedCommand(this, command);
    }

    private void handleUnknownCommand(String command) {
        TTSManager.speak(this, "مش فاهمك. قول حاجة تانية");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SelfAwareService started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SelfAwareService destroyed");

        if (egyptianASR != null) {
            egyptianASR.destroy();
        }

        if (wakeWordDetector != null) {
            wakeWordDetector.destroy();
        }

        if (hybridOrchestrator != null) {
            hybridOrchestrator.destroy();
        }
    }

    // Inner class for Egyptian TTS (placeholder)
    private static class EgyptianTTS {
        public EgyptianTTS(SelfAwareService service, String voiceType) {
            // Initialize Egyptian TTS
        }
    }
}