package com.egyptian.agent.accessibility;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;

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
        
        if (isEnabled) {
            TTSManager.setSeniorSettings(context);
        }
    }
    
    /**
     * Enables senior mode
     * @param context Context for the operation
     */
    public static void enable(Context context) {
        isEnabled = true;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_SENIOR_MODE_ENABLED, true);
        editor.apply();
        
        TTSManager.setSeniorSettings(context);
        Log.i(TAG, "Senior mode enabled");
    }
    
    /**
     * Disables senior mode
     * @param context Context for the operation
     */
    public static void disable(Context context) {
        isEnabled = false;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_SENIOR_MODE_ENABLED, false);
        editor.apply();
        
        TTSManager.resetNormalSettings();
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
        // For now, allow all commands but in a real implementation
        // this could filter out complex or potentially confusing commands
        return true;
    }
    
    /**
     * Handles restricted commands in senior mode
     * @param context Context for the operation
     * @param command The restricted command
     */
    public static void handleRestrictedCommand(Context context, String command) {
        Log.d(TAG, "Restricted command in senior mode: " + command);
        TTSManager.speak(context, "الأمر ده مش متاح في وضع كبار السن");
    }
}