package com.egyptian.agent.core;

import android.app.*;
import android.content.*;
import android.os.*;
import android.speech.tts.*;
import android.util.*;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.executors.EmergencyHandler;
import com.egyptian.agent.stt.VoskSTTEngine;
import com.egyptian.agent.utils.SystemAppHelper;
import java.util.*;

public class VoiceService extends Service implements AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "VoiceService";
    private static final int NOTIFICATION_ID = 1;

    private VoskSTTEngine sttEngine;
    private WakeWordDetector wakeWordDetector;
    private AudioManager audioManager;
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

        // Critical initialization sequence for Honor X6c
        initializeWakeLock();
        initializeAudioManager();
        initializeSTTEngine();
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

    private void initializeAudioManager() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
    }

    private void initializeSTTEngine() {
        try {
            sttEngine = new VoskSTTEngine(this);
            Log.i(TAG, "Vosk STT Engine initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize STT engine", e);
            // In a real implementation, we would have a CrashLogger utility
            // CrashLogger.logError(this, e);
            TTSManager.speak(this, "حصل خطأ في تهيئة المساعد الصوتي");
        }
    }

    private void initializeWakeWord() {
        wakeWordDetector = new WakeWordDetector(this, () -> {
            if (isWakeWordDetected()) {
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

        // Process normal command
        IntentType intent = IntentRouter.detectIntent(command);
        switch (intent) {
            case CALL_CONTACT:
                // CallExecutor.handleCommand(this, command);
                break;
            case SEND_WHATSAPP:
                // WhatsAppExecutor.handleCommand(this, command);
                break;
            case SET_ALARM:
                // AlarmExecutor.handleCommand(this, command);
                break;
            case READ_MISSED_CALLS:
                // CallLogExecutor.handleCommand(this, command);
                break;
            default:
                handleUnknownCommand(command);
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

    private boolean isWakeWordDetected() {
        // This would be implemented in the actual WakeWordDetector
        return true; // Placeholder
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