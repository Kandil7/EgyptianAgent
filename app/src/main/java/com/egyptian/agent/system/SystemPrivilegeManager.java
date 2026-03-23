package com.egyptian.agent.system;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import rikka.shizuku.Shizuku;
import com.egyptian.agent.security.CommandSanitizer;

/**
 * System Privilege Manager
 * Handles system-level privileges for the application
 *
 * Security hardened with:
 * - Command allowlisting via CommandSanitizer
 * - Rate limiting (max 5 commands per 5 minutes)
 * - Input validation for all system operations
 */
public class SystemPrivilegeManager {
    private static final String TAG = "SystemPrivilegeManager";

    // Shizuku constants for permission handling
    public static final int RESULT_SUCCESS = PackageManager.PERMISSION_GRANTED;
    public static final int PERMISSION_GRANTED = PackageManager.PERMISSION_GRANTED;

    private static boolean hasSystemPrivileges = false;
    private static Context appContext = null;

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
        appContext = context.getApplicationContext();
        try {
            // Set up Shizuku callbacks
            Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
                if (requestCode == 1) { // Our request code
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Log.i(TAG, "System privileges granted");
                        hasSystemPrivileges = true;
                    } else {
                        Log.w(TAG, "System privileges denied");
                        hasSystemPrivileges = false;
                    }
                }
            });

            // Check if already granted
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
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
     * Executes a sanitized system command with security validation
     * 
     * @param command The command to execute
     * @throws SecurityException if command is not allowed or rate limit exceeded
     */
    public static void executeSystemCommand(String command) throws SecurityException {
        if (!hasSystemPrivileges) {
            throw new SecurityException("System privileges not available");
        }
        
        // Sanitize and validate command
        String sanitizedCommand = CommandSanitizer.sanitize(command);
        
        Log.d(TAG, "Executing sanitized command: " + sanitizedCommand);
        
        // Execute via Shizuku with proper validation
        try {
            // Command execution would go here via Shizuku
            // For now, we log the validated command
            Log.i(TAG, "Command validated and ready for execution: " + sanitizedCommand);
        } catch (Exception e) {
            Log.e(TAG, "Error executing system command", e);
            throw new SecurityException("Failed to execute command: " + e.getMessage());
        }
    }

    /**
     * Checks remaining commands allowed in current rate limit window
     * 
     * @return Number of commands remaining
     */
    public static int getRemainingCommands() {
        return CommandSanitizer.getRemainingCommands();
    }

    /**
     * Checks if a command is allowed without executing it
     * 
     * @param command The command to check
     * @return true if command is allowed, false otherwise
     */
    public static boolean isCommandAllowed(String command) {
        return CommandSanitizer.isCommandAllowed(command);
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