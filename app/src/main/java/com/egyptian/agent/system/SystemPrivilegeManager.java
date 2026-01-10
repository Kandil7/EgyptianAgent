package com.egyptian.agent.system;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuProvider;
import rikka.shizuku.SystemServiceHelper;

/**
 * Manager for system-level privileges using Shizuku API
 * This enables the Egyptian Agent to operate with system-level permissions
 * without requiring full root access in all scenarios
 */
public class SystemPrivilegeManager {
    private static final String TAG = "SystemPrivilegeManager";
    private static boolean isInitialized = false;
    private static final int REQUEST_CODE = 1001;

    /**
     * Initializes Shizuku for system-level operations
     * @param context Application context
     */
    public static void initialize(Context context) {
        if (isInitialized) {
            Log.d(TAG, "Shizuku already initialized");
            return;
        }

        // Setup Shizuku callbacks
        Shizuku.addRequestPermissionResultListener(permissionResultListener);
        Shizuku.addBinderReceivedListener(binderReceivedListener);
        
        Log.i(TAG, "Shizuku system privilege manager initialized");
        isInitialized = true;
    }

    /**
     * Checks if the app has system-level permissions via Shizuku
     * @return true if system permissions are available
     */
    public static boolean hasSystemPrivileges() {
        if (Shizuku.isPreV11()) {
            return true; // On pre-v11, we assume privileges are available
        }
        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests system-level permissions via Shizuku
     * @param context Application context
     */
    public static void requestSystemPrivileges(Context context) {
        if (hasSystemPrivileges()) {
            Log.i(TAG, "System privileges already granted");
            return;
        }

        if (Shizuku.isPreV11()) {
            Log.w(TAG, "Running on pre-v11 Shizuku, system privileges may not be needed");
            return;
        }

        try {
            Shizuku.requestPermission(REQUEST_CODE);
            Log.i(TAG, "Requested system privileges via Shizuku");
        } catch (Exception e) {
            Log.e(TAG, "Failed to request Shizuku permissions", e);
        }
    }

    // Listener for permission results
    private static final Shizuku.OnRequestPermissionResultListener permissionResultListener = 
        new Shizuku.OnRequestPermissionResultListener() {
            @Override
            public void onRequestPermissionResult(int requestCode, int grantResult) {
                if (requestCode == REQUEST_CODE) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Log.i(TAG, "System privileges granted via Shizuku");
                    } else {
                        Log.w(TAG, "System privileges denied via Shizuku");
                    }
                }
            }
        };

    // Listener for when Shizuku binder is received
    private static final Shizuku.OnBinderReceivedListener binderReceivedListener = 
        new Shizuku.OnBinderReceivedListener() {
            @Override
            public void onBinderReceived() {
                Log.i(TAG, "Shizuku binder received, system service access available");
            }
        };

    /**
     * Cleans up resources and listeners
     */
    public static void cleanup() {
        Shizuku.removeRequestPermissionResultListener(permissionResultListener);
        Shizuku.removeBinderReceivedListener(binderReceivedListener);
        isInitialized = false;
    }
}