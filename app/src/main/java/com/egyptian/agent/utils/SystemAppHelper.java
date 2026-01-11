package com.egyptian.agent.utils;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import androidx.core.content.ContextCompat;
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

    /**
     * Disables Honor's aggressive memory killer for this app
     * This is critical for maintaining the voice service on Honor devices
     */
    public static void disableHonorMemoryKiller(Context context) {
        try {
            // Check if we have the required permissions
            if (Settings.canDrawOverlays(context)) {
                // For Honor devices, we need to add the app to the protected apps list
                // This is typically done by launching the system settings
                Intent intent = new Intent();
                intent.setComponent(new android.content.ComponentName(
                    "com.hihonor.android.settings",
                    "com.hihonor.android.settings.BackgroundStartUpManagerActivity"
                ));

                // Add the app to protected apps
                intent.putExtra("packageName", context.getPackageName());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (context.getPackageManager().resolveActivity(intent, 0) != null) {
                    context.startActivity(intent);
                    Log.i(TAG, "Attempted to add app to protected apps on Honor device");
                } else {
                    // Alternative approach for different Honor/HiHonor versions
                    Intent alternativeIntent = new Intent();
                    alternativeIntent.setComponent(new android.content.ComponentName(
                        "com.android.settings",
                        "com.android.settings.Settings$HighPowerApplicationsActivity"
                    ));
                    alternativeIntent.putExtra("packageName", context.getPackageName());
                    alternativeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    if (context.getPackageManager().resolveActivity(alternativeIntent, 0) != null) {
                        context.startActivity(alternativeIntent);
                        Log.i(TAG, "Attempted to add app to protected apps via alternative method");
                    } else {
                        Log.w(TAG, "Could not find system settings activity to disable memory killer");
                    }
                }
            } else {
                Log.w(TAG, "Cannot access overlay settings to disable memory killer");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error attempting to disable Honor memory killer", e);
            CrashLogger.logError(context, e);
        }
    }
}