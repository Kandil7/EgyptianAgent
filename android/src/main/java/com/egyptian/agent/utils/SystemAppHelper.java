package com.egyptian.agent.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

/**
 * System App Helper
 * Provides utilities for system-level operations
 */
public class SystemAppHelper {
    private static final String TAG = "SystemAppHelper";
    
    /**
     * Keeps the app alive by preventing it from being killed by the system
     * @param context Context for the operation
     */
    public static void keepAlive(Context context) {
        try {
            // Acquire wake lock to keep the app running
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "EgyptianAgent:KeepAlive"
            );
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
            
            Log.d(TAG, "Acquired wake lock to keep app alive");
        } catch (Exception e) {
            Log.e(TAG, "Failed to acquire wake lock", e);
        }
    }
    
    /**
     * Checks if the app is running in the background
     * @param context Context for the operation
     * @return true if running in background, false otherwise
     */
    public static boolean isAppInBackground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = context.getPackageName();
        
        for (ActivityManager.RunningAppProcessInfo appProcess : 
             activityManager.getRunningAppProcesses()) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                && appProcess.processName.equals(packageName)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Brings the app to the foreground
     * @param context Context for the operation
     */
    public static void bringAppToForeground(Context context) {
        Intent launchIntent = context.getPackageManager()
            .getLaunchIntentForPackage(context.getPackageName());
        
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(launchIntent);
        }
    }
}