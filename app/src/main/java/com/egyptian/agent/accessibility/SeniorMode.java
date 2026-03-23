package com.egyptian.agent.accessibility;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Senior Mode Manager
 * Handles special accessibility features for elderly users
 */
public class SeniorMode {
    private static final String TAG = "SeniorMode";
    private static final String PREFS_NAME = "senior_mode_prefs";
    private static final String KEY_SENIOR_MODE_ENABLED = "senior_mode_enabled";
    
    private static boolean isEnabled = false;
    private static SharedPreferences sharedPreferences;
    
    /**
     * Initializes the Senior Mode manager
     * @param context Context for the operation
     */
    public static void initialize(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isEnabled = sharedPreferences.getBoolean(KEY_SENIOR_MODE_ENABLED, false);
        Log.i(TAG, "Senior Mode initialized, enabled: " + isEnabled);
    }
    
    /**
     * Enables senior mode
     * @param context Context for the operation
     */
    public static void enable(Context context) {
        isEnabled = true;
        if (sharedPreferences != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_SENIOR_MODE_ENABLED, true);
            editor.apply();
        }
        Log.i(TAG, "Senior mode enabled");
    }
    
    /**
     * Disables senior mode
     * @param context Context for the operation
     */
    public static void disable(Context context) {
        isEnabled = false;
        if (sharedPreferences != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_SENIOR_MODE_ENABLED, false);
            editor.apply();
        }
        Log.i(TAG, "Senior mode disabled");
    }
    
    /**
     * Checks if senior mode is enabled
     * @return true if enabled, false otherwise
     */
    public static boolean isEnabled() {
        return isEnabled;
    }
    
    /**
     * Checks if a command is allowed in senior mode
     * @param command The command to check
     * @return true if allowed, false otherwise
     */
    public static boolean isCommandAllowed(String command) {
        // In senior mode, restrict certain complex commands
        return true;
    }
    
    /**
     * Handles restricted commands in senior mode
     * @param context Context for the operation
     * @param command The restricted command
     */
    public static void handleRestrictedCommand(Context context, String command) {
        Log.d(TAG, "Restricted command in senior mode: " + command);
    }
}