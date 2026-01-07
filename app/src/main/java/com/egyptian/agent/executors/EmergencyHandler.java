package com.egyptian.agent.executors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.core.VibrationManager;
import com.egyptian.agent.utils.CrashLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmergencyHandler implements SensorEventListener {

    private static final String TAG = "EmergencyHandler";
    private static final int FALL_DETECTION_THRESHOLD = 20; // m/s²
    private static final int FALL_CONFIRMATION_TIME = 5000; // 5 seconds
    private static final int EMERGENCY_CALL_TIMEOUT = 30000; // 30 seconds

    private final Context context;
    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private boolean isFallDetected = false;
    private long fallDetectionTime = 0;
    private boolean isEmergencyActive = false;
    private boolean isSeniorModeEnabled = false;

    // Emergency contacts for different scenarios
    private static final List<String> POLICE_NUMBERS = Arrays.asList("122", "121", "0225757114"); // 122 for general police, 121 for emergency police
    private static final List<String> AMBULANCE_NUMBERS = Arrays.asList("123", "0225757115"); // 123 for ambulance
    private static final List<String> FIRE_NUMBERS = Arrays.asList("180", "0225757116"); // 180 for fire department

    // User-defined emergency contacts (would be stored in preferences in real app)
    private final List<String> userEmergencyContacts = new ArrayList<>();

    // Media player for emergency sounds
    private MediaPlayer emergencyMediaPlayer;

    // Phone state listener to detect if call is answered
    private final TelephonyManager telephonyManager;
    private boolean isCallActive = false;

    public EmergencyHandler(Context context) {
        this.context = context;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        // Initialize user emergency contacts (in real app, this would be loaded from preferences)
        initializeUserEmergencyContacts();

        // Register phone state listener
        registerPhoneStateListener();
    }

    private void initializeUserEmergencyContacts() {
        // In a real app, these would be loaded from user preferences
        userEmergencyContacts.add("01000000000"); // Example contact 1
        userEmergencyContacts.add("01111111111"); // Example contact 2
    }

    private void registerPhoneStateListener() {
        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                super.onCallStateChanged(state, phoneNumber);
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        isCallActive = true;
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        isCallActive = false;
                        break;
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * Check if the command contains emergency keywords
     */
    public static boolean isEmergency(String command) {
        String normalized = command.toLowerCase();
        return normalized.contains("نجدة") ||
               normalized.contains("استغاثة") ||
               normalized.contains("طوارئ") ||
               normalized.contains("مش قادر") ||
               normalized.contains("حد يجي") ||
               normalized.contains("إسعاف") ||
               normalized.contains("شرطة") ||
               normalized.contains("حرقان") ||
               normalized.contains("طلق ناري");
    }

    /**
     * Trigger emergency response
     */
    public static void trigger(Context context) {
        trigger(context, false);
    }

    /**
     * Trigger emergency response with option to force without confirmation
     */
    public static void trigger(Context context, boolean force) {
        if (isEmergencyActive) {
            Log.w(TAG, "Emergency already in progress");
            return;
        }

        Log.i(TAG, "Emergency triggered. Force mode: " + force);
        isEmergencyActive = true;

        // Start emergency audio feedback
        playEmergencyAlert();

        // Vibrate in emergency pattern
        VibrationManager.vibrateEmergency(context);

        // Determine emergency type and contacts to call
        List<String> emergencyContacts = determineEmergencyContacts(context);

        // In senior mode or force mode, make calls immediately without confirmation
        if (isSeniorModeEnabled || force) {
            executeEmergencyCalls(context, emergencyContacts);
        } else {
            // Ask for confirmation in normal mode
            TTSManager.speak(context, "ده إجراء طوارئ! قول 'نعم' لو الموضوع خطير فعلاً");
            // SpeechConfirmation.waitForConfirmation(context, 10000, confirmed -> {
            //     if (confirmed) {
            //         executeEmergencyCalls(context, emergencyContacts);
            //     } else {
            //         cancelEmergency();
            //         TTSManager.speak(context, "تم إلغاء وضع الطوارئ");
            //     }
            // });
        }
    }

    /**
     * Determine which emergency contacts to call based on context
     */
    private static List<String> determineEmergencyContacts(Context context) {
        List<String> contacts = new ArrayList<>();

        // In senior mode, prioritize user-defined contacts and ambulance
        if (SeniorMode.isEnabled()) {
            contacts.addAll(getUserEmergencyContacts(context));
            contacts.addAll(AMBULANCE_NUMBERS);
            return contacts;
        }

        // In normal mode, determine based on command context
        // For now, include all emergency services
        contacts.addAll(POLICE_NUMBERS);
        contacts.addAll(AMBULANCE_NUMBERS);
        contacts.addAll(getUserEmergencyContacts(context));

        return contacts;
    }

    /**
     * Get user-defined emergency contacts
     */
    private static List<String> getUserEmergencyContacts(Context context) {
        // In a real app, this would load from preferences
        List<String> contacts = new ArrayList<>();
        contacts.add("01000000000"); // Placeholder
        contacts.add("01111111111"); // Placeholder
        return contacts;
    }

    /**
     * Execute emergency calls to all contacts
     */
    public static void executeEmergencyCalls(Context context, List<String> contacts) {
        Log.i(TAG, "Executing emergency calls to " + contacts.size() + " contacts");

        TTSManager.speak(context, "بتصل بأرقام الطوارئ دلوقتي. إتقعد مكانك ومتتحركش.");

        for (String number : contacts) {
            try {
                // Clean the number
                String cleanNumber = number.replaceAll("[^0-9+]", "");

                // Place the call
                placeCall(context, cleanNumber);

                Log.i(TAG, "Emergency call placed to: " + cleanNumber);

                // Wait for call to be answered or timeout
                waitForCallResponse();

                // If call was answered, we might want to stop calling other numbers
                // But for critical emergencies, we continue to all numbers
            } catch (Exception e) {
                Log.e(TAG, "Failed to place emergency call to " + number, e);
                CrashLogger.logError(context, e);
            }
        }

        // After all calls, provide status update
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isCallActive) {
                TTSManager.speak(context, "أرقام الطوارئ محدش رد. إحنا مستمرين في المحاولة كل دقيقتين.");
                scheduleFollowupCalls(context, contacts);
            } else {
                TTSManager.speak(context, "أحدهم رد على المكالمة. المساعدة جاية.");
            }
        }, EMERGENCY_CALL_TIMEOUT);
    }

    /**
     * Place an emergency call
     */
    private static void placeCall(Context context, String number) {
        try {
            android.content.Intent callIntent = new android.content.Intent(android.content.Intent.ACTION_CALL);
            callIntent.setData(android.net.Uri.parse("tel:" + number));
            callIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);

            // Critical permission handling for system app
            context.startActivity(callIntent);
            Log.i(TAG, "Emergency call placed to: " + number);
        } catch (Exception e) {
            Log.e(TAG, "Emergency call failed", e);
            CrashLogger.logError(context, e);
            TTSManager.speak(context, "حصل مشكلة في إجراء مكالمة الطوارئ. حاول تاني");
        }
    }

    /**
     * Wait for call response (answered or not)
     */
    private static void waitForCallResponse() {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 15000) { // Wait up to 15 seconds
            // In a real implementation, we would check the call state
            // if (isCallActive) {
            //     return; // Call was answered
            // }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Schedule follow-up calls if no one answered
     */
    private static void scheduleFollowupCalls(Context context, List<String> contacts) {
        // In a real app, this would use AlarmManager or WorkManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // For Android 8.0+, use WorkManager for reliability
            androidx.work.PeriodicWorkRequest followupWork = new androidx.work.PeriodicWorkRequest.Builder(
                EmergencyFollowupWorker.class, 2, java.util.concurrent.TimeUnit.MINUTES)
                .addTag("emergency_followup")
                .setInputData(new androidx.work.Data.Builder()
                    .putStringArrayList("contacts", new ArrayList<>(contacts))
                    .build())
                .build();

            androidx.work.WorkManager.getInstance(context).enqueue(followupWork);
        } else {
            // For older Android versions, use AlarmManager
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, EmergencyFollowupReceiver.class);
            intent.setAction("com.egyptian.agent.action.EMERGENCY_FOLLOWUP");
            intent.putStringArrayListExtra("contacts", new ArrayList<>(contacts));

            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                context, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

            if (alarmManager != null) {
                alarmManager.setRepeating(
                    android.app.AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 120000, // 2 minutes
                    120000, // 2 minutes interval
                    pendingIntent
                );
            }
        }

        Log.i(TAG, "Scheduled follow-up calls for emergency contacts");
    }

    /**
     * Check if emergency is currently active
     */
    public static boolean isEmergencyActive() {
        return isEmergencyActive;
    }

    /**
     * Cancel emergency procedures
     */
    public static void cancelEmergency() {
        Log.i(TAG, "Cancelling emergency procedures");
        isEmergencyActive = false;

        stopEmergencyAudio();

        // Stop any ongoing vibrations
        VibrationManager.cancelVibration(context);
    }

    /**
     * Play emergency alert sound
     */
    private static void playEmergencyAlert() {
        // This is a placeholder - in a real app, we would play an actual sound
        // Could use MediaPlayer to play emergency alert from resources
    }

    /**
     * Stop emergency alert sound
     */
    private static void stopEmergencyAudio() {
        // Stop media player if it exists
    }

    /**
     * Start fall detection monitoring
     */
    public static void startFallDetection(Context context) {
        // This would typically be handled by the FallDetector class
        // This method is kept for compatibility
    }

    /**
     * Stop fall detection monitoring
     */
    public static void stopFallDetection(Context context) {
        // This would typically be handled by the FallDetector class
        // This method is kept for compatibility
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Calculate acceleration magnitude
            double acceleration = Math.sqrt(x*x + y*y + z*z);

            // Check for fall pattern (sudden high acceleration followed by no movement)
            if (acceleration > FALL_DETECTION_THRESHOLD) {
                if (!isFallDetected) {
                    Log.w(TAG, "Potential fall detected! Acceleration: " + acceleration);
                    isFallDetected = true;
                    fallDetectionTime = System.currentTimeMillis();

                    // Schedule confirmation check after 5 seconds
                    new Handler(Looper.getMainLooper()).postDelayed(this::confirmFall, FALL_CONFIRMATION_TIME);
                }
            }
        }
    }

    private void confirmFall() {
        if (isFallDetected && (System.currentTimeMillis() - fallDetectionTime) >= FALL_CONFIRMATION_TIME) {
            Log.e(TAG, "Fall confirmed! Triggering emergency response");
            isFallDetected = false;

            // Trigger emergency without confirmation in fall scenarios
            trigger(context, true);

            // Special message for fall detection
            TTSManager.speak(context, "يا كبير! لقيت إنك وقعت. بيتصل بالإسعاف دلوقتي! إتقعد مكانك ومتتحركش.");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    /**
     * Enable senior mode specific behaviors
     */
    public static void enableSeniorMode() {
        isSeniorModeEnabled = true;
        Log.i(TAG, "Senior mode enabled for emergency handling");
    }

    /**
     * Disable senior mode specific behaviors
     */
    public static void disableSeniorMode() {
        isSeniorModeEnabled = false;
        Log.i(TAG, "Senior mode disabled for emergency handling");
    }

    /**
     * Cleanup resources
     */
    public void destroy() {
        stopFallDetection(context);
        cancelEmergency();

        if (emergencyMediaPlayer != null) {
            emergencyMediaPlayer.release();
            emergencyMediaPlayer = null;
        }

        // Unregister phone state listener
        telephonyManager.listen(null, PhoneStateListener.LISTEN_NONE);
    }
}