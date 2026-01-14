package com.egyptian.agent.core;

import android.app.*;
import android.content.*;
import android.os.*;
import android.speech.tts.*;
import android.util.*;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.executors.EmergencyHandler;
import com.egyptian.agent.stt.VoskSTTEngine;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.hybrid.HybridOrchestrator;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.ai.LlamaIntentEngine;
import com.egyptian.agent.hybrid.HybridASR;
import com.egyptian.agent.utils.CrashLogger;
import com.egyptian.agent.utils.SystemAppHelper;
import com.egyptian.agent.system.SystemPrivilegeManager;
import java.util.*;

public class VoiceService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "VoiceService";
    private static final int NOTIFICATION_ID = 1;

    private VoskSTTEngine sttEngine;
    private HybridASR hybridASR;  // New Hybrid ASR engine
    private WakeWordDetector wakeWordDetector;
    private AudioManager audioManager;
    private HybridOrchestrator hybridOrchestrator;
    private LlamaIntentEngine llamaIntentEngine;  // New Llama integration
    private ModelManager modelManager;
    private boolean isListening = false;
    private boolean isProcessing = false;
    private Handler mainHandler;
    private PowerManager.WakeLock wakeLock;

    // Critical for Honor devices
    private ForegroundServiceDelegate foregroundDelegate;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "VoiceService created");

        // Check system privileges
        if (!SystemPrivilegeManager.hasSystemPrivileges()) {
            Log.w(TAG, "System privileges not available, requesting...");
            SystemPrivilegeManager.requestSystemPrivileges(this);
        } else {
            Log.i(TAG, "System privileges already available");
        }

        // Initialize model manager based on device class
        modelManager = new ModelManager(this);

        // Critical initialization sequence for Honor X6c
        initializeWakeLock();
        initializeAudioManager();
        initializeModelBasedOnDeviceClass(); // Initialize models based on device class
        initializeHybridOrchestrator(); // Initialize the new orchestrator
        initializeLlamaIntentEngine(); // Initialize Llama Intent Engine
        initializeHybridASR(); // Initialize the new Hybrid ASR engine
        initializeWakeWord();
        initializeForegroundService();

        // Honor-specific battery optimization bypass
        SystemAppHelper.keepAlive(this);

        // Auto-start if device rebooted
        registerBootReceiver();
    }

    private void initializeWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "EgyptianAgent::VoiceWakeLock"
        );
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
    }

    private void initializeModelBasedOnDeviceClass() {
        Log.i(TAG, "Initializing models based on device class: " +
              modelManager.getDeviceClass().name());

        // Initialize models asynchronously based on device class
        modelManager.initializeModels(new ModelManager.ModelInitializationCallback() {
            @Override
            public void onComplete(boolean success) {
                if (success) {
                    Log.i(TAG, "All models initialized successfully for device class: " +
                          modelManager.getDeviceClass().name());

                    // Initialize STT engine with appropriate model
                    initializeSTTEngine();
                } else {
                    Log.e(TAG, "Failed to initialize all models, using fallback");
                    initializeSTTEngine(); // Initialize with default/fallback models
                    TTSManager.speak(VoiceService.this, "تحذير: بعض النماذج لم تتحمل بشكل كامل");
                }
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error initializing models", error);
                initializeSTTEngine(); // Initialize with default/fallback models
                TTSManager.speak(VoiceService.this, "حصل مشكلة في تهيئة النماذج. بستخدم الإعدادات الافتراضية");
            }
        });
    }

    private void initializeHybridOrchestrator() {
        try {
            hybridOrchestrator = new HybridOrchestrator(this);
            Log.i(TAG, "Hybrid Orchestrator initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Hybrid Orchestrator", e);
            CrashLogger.logError(this, e);
            TTSManager.speak(this, "حصل مشكلة في تهيئة الذكاء الاصطناعي المتقدم");
        }
    }

    private void initializeLlamaIntentEngine() {
        try {
            llamaIntentEngine = new LlamaIntentEngine(this);
            Log.i(TAG, "Llama Intent Engine initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Llama Intent Engine", e);
            CrashLogger.logError(this, e);
            TTSManager.speak(this, "حصل مشكلة في تهيئة نموذج لاما");
        }
    }

    private void initializeHybridASR() {
        try {
            hybridASR = new HybridASR(this);
            Log.i(TAG, "Hybrid ASR engine initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Hybrid ASR engine", e);
            CrashLogger.logError(this, e);
            TTSManager.speak(this, "حصل مشكلة في تهيئة محرك التعرف على الكلام الهجين");
        }
    }

    private void initializeAudioManager() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    }

    private void initializeSTTEngine() {
        try {
            // Use the appropriate model based on device class
            String modelPath = modelManager.getAsrModelPath();
            if (modelPath != null) {
                // Initialize with the specific model for this device class
                sttEngine = new VoskSTTEngine(this, modelPath);
                Log.i(TAG, "STT Engine initialized with model: " + modelPath);
            } else {
                // Fallback to default initialization
                sttEngine = new VoskSTTEngine(this);
                Log.i(TAG, "STT Engine initialized with default model");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize STT engine", e);
            CrashLogger.logError(this, e);
            TTSManager.speak(this, "حصل خطأ في تهيئة المساعد الصوتي");
        }
    }

    private void initializeWakeWord() {
        wakeWordDetector = new WakeWordDetector(this, new WakeWordDetector.WakeWordCallback() {
            @Override
            public void onWakeWordDetected() {
                handleWakeWordDetected();
            }
        });
    }

    private void initializeForegroundService() {
        foregroundDelegate = new ForegroundServiceDelegate(this);
        foregroundDelegate.startForegroundService();
    }

    private void handleWakeWordDetected() {
        if (isProcessing) return;

        isProcessing = true;
        isListening = true;

        // Critical audio handling
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);

        // Vibration feedback for visually impaired users
        // VibrationManager.vibrateShort(this);

        // Senior mode special handling
        if (SeniorMode.isEnabled()) {
            TTSManager.setSeniorSettings(this);
            TTSManager.speak(this, "قول يا كبير");
        } else {
            TTSManager.speak(this, "أوامرك؟");
        }

        // Start listening for command
        // Using the original VoskSTTEngine for real-time listening
        sttEngine.startListening(result -> {
            handleUserCommand(result);
            isListening = false;
            isProcessing = false;
            restartWakeWordListening();
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

        // Use the Llama Intent Engine for advanced Egyptian dialect processing
        if (llamaIntentEngine != null && llamaIntentEngine.isReady()) {
            // Process command through Llama Intent Engine
            IntentResult result = llamaIntentEngine.processEgyptianSpeech(command);

            // Process the result
            if (result.getIntentType() == IntentType.UNKNOWN) {
                // If Llama doesn't recognize, try hybrid orchestrator as fallback
                if (hybridOrchestrator != null) {
                    // Normalize the command using Egyptian dialect processing
                    String normalizedCommand = EgyptianNormalizer.normalize(command);

                    // Use the hybrid orchestrator to determine intent
                    hybridOrchestrator.determineIntent(normalizedCommand, hybridResult -> {
                        // Process the result on the main thread
                        new Handler(Looper.getMainLooper()).post(() -> {
                            // If the orchestrator returns unknown, try the Quantum class
                            if (hybridResult.getIntentType() == IntentType.UNKNOWN) {
                                Quantum quantum = new Quantum(VoiceService.this);
                                quantum.processCommand(command);
                                restartWakeWordListening();
                            } else {
                                processIntentResult(hybridResult, command);
                            }
                        });
                    });
                } else {
                    // Fallback to Quantum class for intent detection
                    Quantum quantum = new Quantum(this);
                    quantum.processCommand(command);
                    restartWakeWordListening();
                }
            } else {
                // Process the result from Llama Intent Engine
                processIntentResult(result, command);
            }
        } else {
            // Fallback to hybrid orchestrator if Llama is not ready
            if (hybridOrchestrator != null) {
                // Normalize the command using Egyptian dialect processing
                String normalizedCommand = EgyptianNormalizer.normalize(command);

                // Use the hybrid orchestrator to determine intent
                hybridOrchestrator.determineIntent(normalizedCommand, result -> {
                    // Process the result on the main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        // If the orchestrator returns unknown, try the Quantum class
                        if (result.getIntentType() == IntentType.UNKNOWN) {
                            Quantum quantum = new Quantum(VoiceService.this);
                            quantum.processCommand(command);
                            restartWakeWordListening();
                        } else {
                            processIntentResult(result, command);
                        }
                    });
                });
            } else {
                // Fallback to Quantum class for intent detection
                Quantum quantum = new Quantum(this);
                quantum.processCommand(command);
                restartWakeWordListening();
            }
        }
    }

    private void processIntentResult(IntentResult result, String originalCommand) {
        Log.i(TAG, "Processing intent result: " + result.getIntentType() + " with confidence: " + result.getConfidence());

        // Check if confidence is too low
        if (result.getConfidence() < 0.5) {
            TTSManager.speak(this, "مش فاهمك كويس. قول الأمر تاني");
            restartWakeWordListening();
            return;
        }

        // Process based on intent type
        switch (result.getIntentType()) {
            case CALL_CONTACT:
                String contactName = result.getEntity("contact", "");
                if (!contactName.isEmpty()) {
                    TTSManager.speak(this, "بتتصل بـ " + contactName);
                    CallExecutor.handleCommand(this, originalCommand);
                } else {
                    TTSManager.speak(this, "مين اللي عايز تتصل بيه؟");
                }
                break;
            case SEND_WHATSAPP:
                String recipient = result.getEntity("contact", "");
                String message = result.getEntity("message", "");
                if (!recipient.isEmpty() && !message.isEmpty()) {
                    TTSManager.speak(this, "ببعت رسالة لـ " + recipient);
                    WhatsAppExecutor.handleCommand(this, originalCommand);
                } else {
                    TTSManager.speak(this, "عايز تبعت رسالة لحد معين؟");
                }
                break;
            case SET_ALARM:
                String time = result.getEntity("time", "");
                if (!time.isEmpty()) {
                    TTSManager.speak(this, "بأضع تنبيه لـ " + time);
                    AlarmExecutor.handleCommand(this, originalCommand);
                } else {
                    TTSManager.speak(this, "متى عايز التنبيه؟");
                }
                break;
            case READ_TIME:
                String currentTime = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
                TTSManager.speak(this, "الساعة " + currentTime);
                restartWakeWordListening();
                break;
            case UNKNOWN:
            default:
                // Try with Quantum class for additional intent detection
                Quantum quantum = new Quantum(this);
                quantum.processCommand(originalCommand);
                // If still unknown, handle as unknown
                if (!quantum.getLastContact().isEmpty()) {
                    // Command was processed by Quantum
                    restartWakeWordListening();
                } else {
                    handleUnknownCommand(originalCommand);
                }
        }
    }

    private void handleEmergencyCommand() {
        EmergencyHandler.trigger(this);
        // VibrationManager.vibrateEmergency(this);
        restartWakeWordListening();
    }

    private void handleSeniorRestrictedCommand(String command) {
        SeniorMode.handleRestrictedCommand(this, command);
        restartWakeWordListening();
    }

    private void handleUnknownCommand(String command) {
        TTSManager.speak(this, "مش فاهمك. قول حاجة تانية");
        restartWakeWordListening();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        if (sttEngine != null) {
            sttEngine.destroy();
        }

        if (wakeWordDetector != null) {
            wakeWordDetector.destroy();
        }

        if (hybridOrchestrator != null) {
            hybridOrchestrator.destroy();
        }

        if (llamaIntentEngine != null) {
            llamaIntentEngine.destroy();
        }

        if (hybridASR != null) {
            hybridASR.destroy();
        }

        if (audioManager != null) {
            audioManager.abandonAudioFocus(this);
        }

        // Critical for memory management on 6GB RAM devices
        System.gc();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            if (sttEngine != null) {
                sttEngine.pauseListening();
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            if (isListening && sttEngine != null) {
                sttEngine.resumeListening();
            }
        }
    }

    private void restartWakeWordListening() {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }

        mainHandler.postDelayed(() -> {
            if (!isListening && !isProcessing && wakeWordDetector != null) {
                wakeWordDetector.restartListening();
            }
        }, 1500);
    }

    // Placeholder class for foreground service delegate
    private static class ForegroundServiceDelegate {
        private Context context;

        public ForegroundServiceDelegate(Context context) {
            this.context = context;
        }

        public void startForegroundService() {
            // Create notification channel for Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    "VOICE_SERVICE_CHANNEL",
                    "Voice Service Channel",
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Service for voice recognition");
                
                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.createNotificationChannel(channel);
            }

            // Create and start foreground notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "VOICE_SERVICE_CHANNEL")
                .setContentTitle("الوكيل المصري")
                .setContentText("المساعد الصوتي شغال")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder icon
                .setPriority(NotificationCompat.PRIORITY_LOW);

            startForeground(NOTIFICATION_ID, builder.build());
        }
    }
}