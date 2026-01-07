package com.egyptian.agent.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class ContactCache {
    
    private static final String TAG = "ContactCache";
    private static final String PREF_NAME = "contact_cache";
    private static Map<String, String> cache = new HashMap<>();
    
    public static void initialize(Context context) {
        // Load cached contacts from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getValue() instanceof String) {
                cache.put(entry.getKey(), (String) entry.getValue());
            }
        }
        
        Log.d(TAG, "Loaded " + cache.size() + " contacts from cache");
    }
    
    public static String get(Context context, String contactName) {
        String number = cache.get(contactName.toLowerCase());
        if (number != null) {
            Log.d(TAG, "Cache hit for: " + contactName);
        } else {
            Log.d(TAG, "Cache miss for: " + contactName);
        }
        return number;
    }
    
    public static void put(Context context, String contactName, String number) {
        String key = contactName.toLowerCase();
        cache.put(key, number);
        
        // Persist to SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, number);
        editor.apply();
        
        Log.d(TAG, "Cached contact: " + contactName + " -> " + number);
    }
    
    public static void remove(Context context, String contactName) {
        String key = contactName.toLowerCase();
        cache.remove(key);
        
        // Remove from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(key);
        editor.apply();
        
        Log.d(TAG, "Removed from cache: " + contactName);
    }
    
    public static void clear(Context context) {
        cache.clear();
        
        // Clear SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        
        Log.d(TAG, "Cleared contact cache");
    }
}