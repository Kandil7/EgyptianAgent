package com.egyptian.agent.system;

import android.content.Context;
import android.util.Log;

import rikka.shizuku.Shizuku;

/**
 * System Privilege Manager
 * Handles system-level privileges for the application
 */
public class SystemPrivilegeManager {
    private static final String TAG = "SystemPrivilegeManager";
    
    private static boolean hasSystemPrivileges = false;
    
    /**
     * Checks if the app has system privileges
     * @return true if system privileges are available, false otherwise
     */
    public static boolean hasSystemPrivileges() {
        return hasSystemPrivileges;
    }
    
    /**
     * Requests system privileges for the application
     * @param context Context for the operation
     */
    public static void requestSystemPrivileges(Context context) {
        try {
            // Check if Shizuku is available
            if (Shizuku.pingBinder()) {
                // Request Shizuku permission
                Shizuku.requestPermission(1); // Use a unique request code
                Log.d(TAG, "Requested Shizuku permission");
            } else {
                Log.w(TAG, "Shizuku not available, system privileges not accessible");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting system privileges", e);
        }
    }
    
    /**
     * Initializes system privileges
     * @param context Context for the operation
     */
    public static void initialize(Context context) {
        try {
            // Set up Shizuku callbacks
            Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
                if (requestCode == 1) { // Our request code
                    if (grantResult == Shizuku.RESULT_SUCCESS) {
                        Log.i(TAG, "System privileges granted");
                        hasSystemPrivileges = true;
                    } else {
                        Log.w(TAG, "System privileges denied");
                        hasSystemPrivileges = false;
                    }
                }
            });
            
            // Check if already granted
            if (Shizuku.checkSelfPermission() == Shizuku.PERMISSION_GRANTED) {
                hasSystemPrivileges = true;
                Log.i(TAG, "System privileges already granted");
            } else {
                Log.i(TAG, "System privileges not granted, requesting...");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing system privileges", e);
        }
    }
    
    /**
     * Cleans up system privilege resources
     */
    public static void cleanup() {
        try {
            Shizuku.removeRequestPermissionResultListener((requestCode, grantResult) -> {});
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up system privileges", e);
        }
    }
}