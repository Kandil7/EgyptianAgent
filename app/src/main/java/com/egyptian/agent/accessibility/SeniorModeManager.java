package com.egyptian.agent.accessibility;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.CrashLogger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Senior Mode Manager implementing all features specified in the SRD
 */
public class SeniorModeManager {
    private static final String TAG = "SeniorModeManager";
    private static final String PREFS_NAME = "senior_mode_prefs";
    private static final String KEY_IS_ENABLED = "is_senior_mode_enabled";
    
    private static boolean isEnabled = false;
    private Context context;
    private SeniorModeConfig config;
    
    public SeniorModeManager(Context context) {
        this.context = context;
        loadPreferences();
        initializeConfig();
    }
    
    private void loadPreferences() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isEnabled = prefs.getBoolean(KEY_IS_ENABLED, false);
    }
    
    private void initializeConfig() {
        config = new SeniorModeConfig();
        // Load saved settings or use defaults
        loadSavedConfig();
    }
    
    private void loadSavedConfig() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Load audio settings
        config.setSpeechRate(prefs.getFloat("speech_rate", 0.75f));
        config.setPitch(prefs.getFloat("pitch", 0.9f));
        config.setVolume(prefs.getFloat("volume", 1.0f));
        
        // Load guardian info
        config.setGuardianPhoneNumber(prefs.getString("guardian_phone", ""));
        config.setSendDailyReports(prefs.getBoolean("send_daily_reports", true));
        
        // Load medications
        loadMedicationsFromStorage();
    }
    
    public void enable(Context context) {
        if (isEnabled) return;
        
        Log.i(TAG, "Enabling Senior Mode");
        isEnabled = true;
        savePreferences();
        
        // Apply senior-specific settings
        applySeniorSettings(context);
        
        // Notify components
        notifySeniorModeEnabled();
        
        // Special greeting for seniors
        TTSManager.speak(context, "تم تفعيل وضع كبار السن. قول 'يا كبير' لأي حاجة.");
    }
    
    public void disable(Context context) {
        if (!isEnabled) return;
        
        Log.i(TAG, "Disabling Senior Mode");
        isEnabled = false;
        savePreferences();
        
        // Restore normal settings
        restoreNormalSettings(context);
        
        // Notify components
        notifySeniorModeDisabled();
        
        TTSManager.speak(context, "تم إيقاف وضع كبار السن");
    }
    
    private void savePreferences() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_ENABLED, isEnabled);
        editor.apply();
    }
    
    private void applySeniorSettings(Context context) {
        // Apply TTS settings
        TTSManager.setSpeechRate(context, config.getSpeechRate());
        TTSManager.setPitch(context, config.getPitch());
        TTSManager.setVolume(context, config.getVolume());
        
        // Apply other senior-specific configurations
        Log.i(TAG, "Senior settings applied");
    }
    
    private void restoreNormalSettings(Context context) {
        // Restore normal TTS settings
        TTSManager.setSpeechRate(context, 1.0f);
        TTSManager.setPitch(context, 1.0f);
        TTSManager.setVolume(context, 0.9f);
        
        Log.i(TAG, "Normal settings restored");
    }
    
    private void notifySeniorModeEnabled() {
        // Notify other components that senior mode is enabled
        // This could involve sending broadcasts or calling other managers
        Log.i(TAG, "Notifying components of senior mode enabled");
    }
    
    private void notifySeniorModeDisabled() {
        // Notify other components that senior mode is disabled
        Log.i(TAG, "Notifying components of senior mode disabled");
    }
    
    public static boolean isEnabled() {
        return isEnabled;
    }
    
    public SeniorModeConfig getConfig() {
        return config;
    }
    
    public void updateConfig(SeniorModeConfig newConfig) {
        this.config = newConfig;
        saveConfigToPreferences();
        
        if (isEnabled) {
            // Apply changes immediately
            applySeniorSettings(context);
        }
    }
    
    private void saveConfigToPreferences() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Save audio settings
        editor.putFloat("speech_rate", config.getSpeechRate());
        editor.putFloat("pitch", config.getPitch());
        editor.putFloat("volume", config.getVolume());
        
        // Save guardian info
        editor.putString("guardian_phone", config.getGuardianPhoneNumber());
        editor.putBoolean("send_daily_reports", config.isSendDailyReports());
        
        editor.apply();
    }
    
    public void addMedicationReminder(MedicationReminder reminder) {
        config.getMedicationReminders().add(reminder);
        saveConfigToPreferences();
        
        // Schedule the reminder
        // In a real implementation, this would use AlarmManager or WorkManager
        Log.i(TAG, "Medication reminder added: " + reminder.getMedicationName());
    }
    
    public List<MedicationReminder> getMedicationReminders() {
        return new ArrayList<>(config.getMedicationReminders());
    }
    
    public void setGuardianPhoneNumber(String phoneNumber) {
        config.setGuardianPhoneNumber(phoneNumber);
        saveConfigToPreferences();
    }
    
    public String getGuardianPhoneNumber() {
        return config.getGuardianPhoneNumber();
    }

    /**
     * Loads medications from persistent storage
     */
    private void loadMedicationsFromStorage() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String medicationsJson = prefs.getString("medications_list", "[]");

        try {
            JSONArray jsonArray = new JSONArray(medicationsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject medObj = jsonArray.getJSONObject(i);
                MedicationReminder reminder = new MedicationReminder(
                    medObj.getString("name"),
                    medObj.getString("dosage"),
                    medObj.getLong("timeInMillis")
                );
                config.getMedicationReminders().add(reminder);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing medications from storage", e);
        }
    }

    /**
     * Saves medications to persistent storage
     */
    private void saveMedicationsToStorage() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        try {
            JSONArray jsonArray = new JSONArray();
            for (MedicationReminder reminder : config.getMedicationReminders()) {
                JSONObject medObj = new JSONObject();
                medObj.put("name", reminder.getMedicationName());
                medObj.put("dosage", reminder.getDosage());
                medObj.put("timeInMillis", reminder.getTimeInMillis());
                jsonArray.put(medObj);
            }
            editor.putString("medications_list", jsonArray.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error serializing medications to storage", e);
        }

        editor.apply();
    }

    /**
     * Gets singleton instance of SeniorModeManager
     */
    public static SeniorModeManager getInstance(Context context) {
        if (instance == null) {
            instance = new SeniorModeManager(context.getApplicationContext());
        }
        return instance;
    }

    private static SeniorModeManager instance;
}