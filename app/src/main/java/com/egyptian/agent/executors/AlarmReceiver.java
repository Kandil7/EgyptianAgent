package com.egyptian.agent.executors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.VibrationManager;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Alarm triggered!");

        // Acquire a wake lock to ensure the device stays awake
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "EgyptianAgent::AlarmWakeLock"
            );
            wakeLock.acquire(10 * 60 * 1000L); // 10 minutes
        }

        try {
            // Speak the alarm message
            TTSManager.speak(context, "الوقت دلوقتي " + getCurrentTime());
            
            // Vibrate to get attention
            VibrationManager.vibratePattern(context, new long[]{0, 1000, 500, 1000, 500, 1000});
            
            // If in senior mode, repeat the message
            if (SeniorMode.isEnabled()) {
                Thread.sleep(3000); // Wait before repeating
                TTSManager.speak(context, "التنبيه اللي ضبطته اتعمل دلوقتي");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in alarm receiver", e);
        } finally {
            // Release the wake lock
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    private String getCurrentTime() {
        java.text.DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(null);
        return dateFormat.format(new java.util.Date());
    }
}