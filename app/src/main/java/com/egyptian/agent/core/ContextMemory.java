package com.egyptian.agent.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

/**
 * ContextMemory - Manages contextual information across conversations
 * Stores last interactions, preferences, and conversation history
 */
public class ContextMemory {
    private static final String TAG = "ContextMemory";
    private static final String PREFS_NAME = "context_memory_prefs";
    
    private static ContextMemory instance;
    private SharedPreferences prefs;
    private Map<String, String> volatileMemory; // In-memory cache for quick access
    
    private ContextMemory(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        volatileMemory = new HashMap<>();
        
        // Load persistent values into volatile memory
        loadPersistentMemory();
    }
    
    public static synchronized ContextMemory getInstance(Context context) {
        if (instance == null) {
            instance = new ContextMemory(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Loads persistent memory values into volatile memory
     */
    private void loadPersistentMemory() {
        volatileMemory.put("last_contact", prefs.getString("last_contact", ""));
        volatileMemory.put("last_app", prefs.getString("last_app", ""));
        volatileMemory.put("last_action", prefs.getString("last_action", ""));
        volatileMemory.put("user_preference_location", prefs.getString("user_preference_location", ""));
        volatileMemory.put("user_preference_volume", prefs.getString("user_preference_volume", "medium"));
        volatileMemory.put("senior_mode_enabled", prefs.getString("senior_mode_enabled", "false"));
    }
    
    /**
     * Saves volatile memory to persistent storage
     */
    private void saveToPersistentStorage() {
        SharedPreferences.Editor editor = prefs.edit();
        
        for (Map.Entry<String, String> entry : volatileMemory.entrySet()) {
            editor.putString(entry.getKey(), entry.getValue());
        }
        
        editor.apply();
    }
    
    /**
     * Gets a value from context memory
     */
    public String getValue(String key) {
        return volatileMemory.getOrDefault(key, "");
    }
    
    /**
     * Sets a value in context memory
     */
    public void setValue(String key, String value) {
        volatileMemory.put(key, value);
        // Also save to persistent storage
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }
    
    /**
     * Gets the last mentioned contact
     */
    public String getLastContact() {
        return getValue("last_contact");
    }
    
    /**
     * Sets the last mentioned contact
     */
    public void setLastContact(String contact) {
        setValue("last_contact", contact);
    }
    
    /**
     * Gets the last used app
     */
    public String getLastApp() {
        return getValue("last_app");
    }
    
    /**
     * Sets the last used app
     */
    public void setLastApp(String app) {
        setValue("last_app", app);
    }
    
    /**
     * Gets the last performed action
     */
    public String getLastAction() {
        return getValue("last_action");
    }
    
    /**
     * Sets the last performed action
     */
    public void setLastAction(String action) {
        setValue("last_action", action);
    }
    
    /**
     * Clears all context memory
     */
    public void clearAll() {
        volatileMemory.clear();
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        
        // Reinitialize with defaults
        loadPersistentMemory();
    }
    
    /**
     * Updates context based on current interaction
     */
    public void updateContext(String intent, String entity, String value) {
        switch (intent.toLowerCase()) {
            case "call_person":
                setLastContact(value);
                setLastAction("call");
                break;
            case "send_msg":
            case "send_voice":
                setLastContact(value);
                setLastAction("message");
                break;
            case "play_music":
            case "play_quran":
                setLastApp(entity); // Could be artist, song, etc.
                setLastAction("media");
                break;
            case "open_app":
                setLastApp(value);
                setLastAction("open_app");
                break;
            default:
                Log.d(TAG, "Unknown intent for context update: " + intent);
        }
    }
    
    /**
     * Gets context summary for debugging
     */
    public String getContextSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Context Memory Summary:\n");
        for (Map.Entry<String, String> entry : volatileMemory.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}