package com.egyptian.agent.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.SystemClock;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.executors.EmergencyHandler;
import com.egyptian.agent.utils.CrashLogger;
import com.egyptian.agent.utils.VibrationManager;

public class VolumeButtonReceiver extends BroadcastReceiver {

    private static final String TAG = "VolumeButtonReceiver";
    private static final long TRIPLE_CLICK_TIME_WINDOW = 1000; // 1 second window
    private static final int TRIPLE_CLICK_COUNT = 3;

    private int clickCount = 0;
    private long lastClickTime = 0;
    private boolean isEmergencyMode = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (AudioManager.VOLUME_CHANGED_ACTION.equals(intent.getAction())) {
                handleVolumeButtonPress(context, intent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling volume button press", e);
            CrashLogger.logError(context, e);
        }
    }

    private void handleVolumeButtonPress(Context context, Intent intent) {
        // Only handle volume down button presses for emergency
        // Volume up is often used for camera and other system functions
        int currentVolume = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, -1);
        int previousVolume = intent.getIntExtra(AudioManager.EXTRA_PREV_VOLUME_STREAM_VALUE, -1);
        int streamType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);

        // We only care about ring/multimedia volume stream
        if (streamType != AudioManager.STREAM_RING && streamType != AudioManager.STREAM_MUSIC) {
            return;
        }

        // Volume down button press
        if (currentVolume < previousVolume) {
            long currentTime = SystemClock.elapsedRealtime();

            // Check if this is within the triple-click time window
            if (currentTime - lastClickTime < TRIPLE_CLICK_TIME_WINDOW) {
                clickCount++;
            } else {
                clickCount = 1;
            }

            lastClickTime = currentTime;

            Log.d(TAG, "Volume button press detected. Click count: " + clickCount);

            // Triple click detected - trigger emergency
            if (clickCount >= TRIPLE_CLICK_COUNT) {
                handleTripleClickEmergency(context);
                clickCount = 0; // Reset counter
            } else if (clickCount == 1) {
                // Single click in senior mode can trigger assistant
                handleSingleClick(context);
            }
        }
    }

    private void handleTripleClickEmergency(Context context) {
        Log.w(TAG, "TRIPLE CLICK EMERGENCY DETECTED!");

        // Short vibration to confirm detection
        VibrationManager.vibrateShort(context);

        // Trigger emergency without confirmation
        EmergencyHandler emergencyHandler = new EmergencyHandler(context);
        emergencyHandler.trigger(context, true);

        // Special announcement
        TTSManager.speak(context, "طوارئ! بتتصل بالنجدة والإسعاف دلوقتي!");

        // Prevent further processing for a while
        isEmergencyMode = true;
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            isEmergencyMode = false;
        }, 30000); // 30 seconds cooldown
    }

    private void handleSingleClick(Context context) {
        if (isEmergencyMode) return;

        // In senior mode, single click on volume down can activate the assistant
        if (SeniorMode.isEnabled()) {
            // Vibrate to confirm activation
            VibrationManager.vibrateShort(context);

            // Start listening for command
            Intent serviceIntent = new Intent(context, com.egyptian.agent.core.VoiceService.class);
            serviceIntent.setAction("com.egyptian.agent.action.START_LISTENING");
            context.startService(serviceIntent);

            Log.i(TAG, "Assistant activated via volume button in senior mode");
        }
    }
}