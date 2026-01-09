package com.egyptian.agent.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class PreferencesHelper {

    private static final String TAG = "PreferencesHelper";
    private static final String PREFS_NAME = "egyptian_agent_prefs";
    
    // Preference keys
    public static final String KEY_SENIOR_MODE_ENABLED = "senior_mode_enabled";
    public static final String KEY_GUARDIAN_PHONE_NUMBER = "guardian_phone_number";
    public static final String KEY_DEFAULT_GUARDIAN_NUMBER = "default_guardian_number";
    public static final String KEY_EMERGENCY_CONTACTS = "emergency_contacts";
    public static final String KEY_WAKE_WORD_SENSITIVITY = "wake_word_sensitivity";
    public static final String KEY_TTS_SPEED = "tts_speed";
    public static final String KEY_TTS_VOLUME = "tts_volume";
    public static final String KEY_LAST_FALL_DETECTION = "last_fall_detection";
    public static final String KEY_USER_PREFERENCES_INITIALIZED = "user_preferences_initialized";

    private static PreferencesHelper instance;
    private SharedPreferences sharedPreferences;
    private Context context;

    private PreferencesHelper(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PreferencesHelper getInstance(Context context) {
        if (instance == null) {
            instance = new PreferencesHelper(context);
        }
        return instance;
    }

    /**
     * Save boolean value to preferences
     */
    public void saveBoolean(String key, boolean value) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(key, value);
            editor.apply();
            Log.d(TAG, "Saved boolean: " + key + " = " + value);
        } catch (Exception e) {
            Log.e(TAG, "Error saving boolean preference: " + key, e);
            CrashLogger.logError(context, e);
        }
    }

    /**
     * Get boolean value from preferences
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        try {
            return sharedPreferences.getBoolean(key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Error getting boolean preference: " + key, e);
            CrashLogger.logError(context, e);
            return defaultValue;
        }
    }

    /**
     * Save string value to preferences
     */
    public void saveString(String key, String value) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(key, value);
            editor.apply();
            Log.d(TAG, "Saved string: " + key + " = " + value);
        } catch (Exception e) {
            Log.e(TAG, "Error saving string preference: " + key, e);
            CrashLogger.logError(context, e);
        }
    }

    /**
     * Get string value from preferences
     */
    public String getString(String key, String defaultValue) {
        try {
            return sharedPreferences.getString(key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Error getting string preference: " + key, e);
            CrashLogger.logError(context, e);
            return defaultValue;
        }
    }

    /**
     * Save integer value to preferences
     */
    public void saveInt(String key, int value) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(key, value);
            editor.apply();
            Log.d(TAG, "Saved int: " + key + " = " + value);
        } catch (Exception e) {
            Log.e(TAG, "Error saving int preference: " + key, e);
            CrashLogger.logError(context, e);
        }
    }

    /**
     * Get integer value from preferences
     */
    public int getInt(String key, int defaultValue) {
        try {
            return sharedPreferences.getInt(key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Error getting int preference: " + key, e);
            CrashLogger.logError(context, e);
            return defaultValue;
        }
    }

    /**
     * Save long value to preferences
     */
    public void saveLong(String key, long value) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(key, value);
            editor.apply();
            Log.d(TAG, "Saved long: " + key + " = " + value);
        } catch (Exception e) {
            Log.e(TAG, "Error saving long preference: " + key, e);
            CrashLogger.logError(context, e);
        }
    }

    /**
     * Get long value from preferences
     */
    public long getLong(String key, long defaultValue) {
        try {
            return sharedPreferences.getLong(key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Error getting long preference: " + key, e);
            CrashLogger.logError(context, e);
            return defaultValue;
        }
    }

    /**
     * Remove a preference
     */
    public void remove(String key) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(key);
            editor.apply();
            Log.d(TAG, "Removed preference: " + key);
        } catch (Exception e) {
            Log.e(TAG, "Error removing preference: " + key, e);
            CrashLogger.logError(context, e);
        }
    }

    /**
     * Check if a preference exists
     */
    public boolean contains(String key) {
        try {
            return sharedPreferences.contains(key);
        } catch (Exception e) {
            Log.e(TAG, "Error checking preference existence: " + key, e);
            CrashLogger.logError(context, e);
            return false;
        }
    }

    /**
     * Clear all preferences (use with caution)
     */
    public void clearAll() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            Log.i(TAG, "All preferences cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing preferences", e);
            CrashLogger.logError(context, e);
        }
    }

    // Convenience methods for specific preferences

    public void setSeniorModeEnabled(boolean enabled) {
        saveBoolean(KEY_SENIOR_MODE_ENABLED, enabled);
    }

    public boolean isSeniorModeEnabled() {
        return getBoolean(KEY_SENIOR_MODE_ENABLED, false);
    }

    public void setGuardianPhoneNumber(String phoneNumber) {
        saveString(KEY_GUARDIAN_PHONE_NUMBER, phoneNumber);
    }

    public String getGuardianPhoneNumber() {
        return getString(KEY_GUARDIAN_PHONE_NUMBER, "");
    }

    public void setDefaultGuardianNumber(String number) {
        saveString(KEY_DEFAULT_GUARDIAN_NUMBER, number);
    }

    public String getDefaultGuardianNumber() {
        return getString(KEY_DEFAULT_GUARDIAN_NUMBER, "");
    }

    public void setLastFallDetectionTime(long time) {
        saveLong(KEY_LAST_FALL_DETECTION, time);
    }

    public long getLastFallDetectionTime() {
        return getLong(KEY_LAST_FALL_DETECTION, 0);
    }

    public void setUserPreferencesInitialized(boolean initialized) {
        saveBoolean(KEY_USER_PREFERENCES_INITIALIZED, initialized);
    }

    public boolean isUserPreferencesInitialized() {
        return getBoolean(KEY_USER_PREFERENCES_INITIALIZED, false);
    }
}