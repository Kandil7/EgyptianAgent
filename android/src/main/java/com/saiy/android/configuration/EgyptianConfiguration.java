package com.saiy.android.configuration;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

/**
 * Egyptian-specific configuration for Saiy-PS
 * Contains settings for Egyptian dialect support and senior features
 */
public class EgyptianConfiguration {
    private static final String TAG = "EgyptianConfiguration";
    private static final String PREFS_NAME = "egyptian_agent_prefs";
    
    // Egyptian wake words
    public static final List<String> EGYPTIAN_WAKE_WORDS = Arrays.asList(
        "يا حكيم",    // Hey Wise One
        "يا كبير",    // Hey Elder (for senior mode)
        "ساعدني"     // Help me (for emergencies)
    );
    
    // Egyptian emergency phrases
    public static final List<String> EGYPTIAN_EMERGENCY_PHRASES = Arrays.asList(
        "نجدة", "استغاثة", "مش قادر", "حد يجي",
        "إسعاف", "حرقان", "طلق ناري", "ساعدني"
    );
    
    // Egyptian dialect variations for common commands
    public static final String[] EGYPTIAN_CALL_COMMANDS = {
        "اتصل", "كلم", "رن على", "شيل خط", "رفع تيليفون"
    };
    
    public static final String[] EGYPTIAN_MESSAGE_COMMANDS = {
        "ابعت", "قول لـ", "خلي", "عايز اقول"
    };
    
    public static final String[] EGYPTIAN_ALARM_COMMANDS = {
        "نبهني", "انبهني", "ذكّرني", "خلي تنبيه", "定了"
    };
    
    public static final String[] EGYPTIAN_READ_COMMANDS = {
        "اقرا", "شوف", "قولي", "إيه ده", "شنو ده"
    };
    
    private final SharedPreferences prefs;
    
    public EgyptianConfiguration(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "Egyptian configuration initialized");
    }
    
    /**
     * Check if Egyptian mode is enabled
     */
    public boolean isEgyptianModeEnabled() {
        return prefs.getBoolean("egyptian_mode_enabled", true);
    }
    
    /**
     * Enable or disable Egyptian mode
     */
    public void setEgyptianModeEnabled(boolean enabled) {
        prefs.edit().putBoolean("egyptian_mode_enabled", enabled).apply();
    }
    
    /**
     * Check if senior mode is enabled
     */
    public boolean isSeniorModeEnabled() {
        return prefs.getBoolean("senior_mode_enabled", false);
    }
    
    /**
     * Enable or disable senior mode
     */
    public void setSeniorModeEnabled(boolean enabled) {
        prefs.edit().putBoolean("senior_mode_enabled", enabled).apply();
        
        // Apply senior-specific settings when mode is toggled
        if (enabled) {
            applySeniorSettings();
        } else {
            removeSeniorSettings();
        }
    }
    
    /**
     * Apply senior-specific settings
     */
    private void applySeniorSettings() {
        // Set slower speech rate
        prefs.edit().putFloat("speech_rate", 0.75f).apply();
        
        // Set clearer pronunciation
        prefs.edit().putBoolean("clear_pronunciation", true).apply();
        
        // Enable double confirmation
        prefs.edit().putBoolean("double_confirmation", true).apply();
        
        Log.d(TAG, "Senior settings applied");
    }
    
    /**
     * Remove senior-specific settings
     */
    private void removeSeniorSettings() {
        // Reset to default values
        prefs.edit()
            .remove("speech_rate")
            .remove("clear_pronunciation")
            .remove("double_confirmation")
            .apply();
        
        Log.d(TAG, "Senior settings removed");
    }
    
    /**
     * Get the preferred speech rate for Egyptian dialect
     */
    public float getEgyptianSpeechRate() {
        if (isSeniorModeEnabled()) {
            return prefs.getFloat("speech_rate", 0.75f);
        }
        return prefs.getFloat("speech_rate", 1.0f);
    }
    
    /**
     * Check if clear pronunciation is enabled
     */
    public boolean isClearPronunciationEnabled() {
        if (isSeniorModeEnabled()) {
            return prefs.getBoolean("clear_pronunciation", true);
        }
        return prefs.getBoolean("clear_pronunciation", false);
    }
    
    /**
     * Check if double confirmation is enabled
     */
    public boolean isDoubleConfirmationEnabled() {
        return isSeniorModeEnabled() && 
               prefs.getBoolean("double_confirmation", false);
    }
    
    /**
     * Get the list of allowed commands for senior mode
     */
    public List<String> getAllowedSeniorCommands() {
        // For senior mode, we allow only essential commands
        return Arrays.asList(
            "call",      // Making calls
            "whatsapp",  // Sending messages
            "alarm",     // Setting alarms
            "time",      // Reading time
            "emergency", // Emergency features
            "medication" // Medication reminders
        );
    }
}