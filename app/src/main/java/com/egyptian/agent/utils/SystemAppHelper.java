package com.egyptian.agent.utils;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import com.egyptian.agent.utils.CrashLogger;

public class SystemAppHelper {

    private static final String TAG = "SystemAppHelper";

    /**
     * Keep the app alive on devices with aggressive battery optimization
     * Specifically designed for Honor devices
     */
    public static void keepAlive(Context context) {
        try {
            // Acquire wake lock to prevent CPU from sleeping
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "EgyptianAgent::KeepAlive"
            );

            // Hold the wake lock for a reasonable duration
            wakeLock.acquire(30 * 60 * 1000L /*30 minutes*/);

            Log.i(TAG, "System app keep-alive mechanism activated");

            // Release wake lock after a delay to prevent battery drain
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                    Log.i(TAG, "Wake lock released");
                }
            }, 30 * 60 * 1000L); // 30 minutes

        } catch (Exception e) {
            Log.e(TAG, "Failed to acquire wake lock", e);
            CrashLogger.logError(context, e);
        }
    }
}