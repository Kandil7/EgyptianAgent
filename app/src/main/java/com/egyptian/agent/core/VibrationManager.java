package com.egyptian.agent.core;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

public class VibrationManager {

    private static final String TAG = "VibrationManager";

    /**
     * Perform a short vibration
     */
    public static void vibrateShort(Context context) {
        vibratePattern(context, new long[]{0, 200});
    }

    /**
     * Perform a long vibration
     */
    public static void vibrateLong(Context context) {
        vibratePattern(context, new long[]{0, 500});
    }

    /**
     * Perform emergency vibration pattern
     */
    public static void vibrateEmergency(Context context) {
        vibratePattern(context, new long[]{0, 500, 200, 500, 200, 500});
    }

    /**
     * Perform custom vibration pattern
     */
    public static void vibratePattern(Context context, long[] pattern) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

            if (vibrator == null) {
                Log.e(TAG, "Vibrator service not available");
                return;
            }

            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // For Android O and above
                    VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1); // -1 means no repeat
                    vibrator.vibrate(effect);
                } else {
                    // For older Android versions
                    vibrator.vibrate(pattern, -1); // -1 means no repeat
                }
            } else {
                Log.w(TAG, "Device does not have a vibrator");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Vibration permission denied", e);
        } catch (Exception e) {
            Log.e(TAG, "Error performing vibration", e);
        }
    }

    /**
     * Cancel ongoing vibration
     */
    public static void cancelVibration(Context context) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.cancel();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling vibration", e);
        }
    }
}