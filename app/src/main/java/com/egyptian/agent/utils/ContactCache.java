package com.egyptian.agent.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class ContactCache {
    private static final String TAG = "ContactCache";
    private static final String PREF_NAME = "contact_cache";
    private static final int MAX_CACHE_SIZE = 100;
    
    private static Map<String, String> memoryCache = new HashMap<>();
    
    /**
     * Get a contact number from cache
     * @param context Application context
     * @param contactName Name of the contact
     * @return Contact number if found, null otherwise
     */
    public static String get(Context context, String contactName) {
        // First check memory cache
        String number = memoryCache.get(contactName);
        if (number != null) {
            Log.d(TAG, "Contact found in memory cache: " + contactName);
            return number;
        }
        
        // Then check persistent cache
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        number = prefs.getString(contactName, null);
        
        if (number != null) {
            Log.d(TAG, "Contact found in persistent cache: " + contactName);
            // Add to memory cache for faster access
            memoryCache.put(contactName, number);
        }
        
        return number;
    }
    
    /**
     * Put a contact number in cache
     * @param context Application context
     * @param contactName Name of the contact
     * @param number Number to cache
     */
    public static void put(Context context, String contactName, String number) {
        // Add to memory cache
        memoryCache.put(contactName, number);
        
        // Limit memory cache size
        if (memoryCache.size() > MAX_CACHE_SIZE) {
            // Simple eviction policy - remove first entry
            String firstKey = memoryCache.keySet().iterator().next();
            memoryCache.remove(firstKey);
        }
        
        // Add to persistent cache
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(contactName, number);
        editor.apply();
        
        Log.d(TAG, "Contact cached: " + contactName + " -> " + number);
    }
    
    /**
     * Clear the contact cache
     * @param context Application context
     */
    public static void clear() {
        memoryCache.clear();
        Log.d(TAG, "Contact memory cache cleared");
    }
    
    /**
     * Remove a specific contact from cache
     * @param context Application context
     * @param contactName Name of the contact to remove
     */
    public static void remove(Context context, String contactName) {
        memoryCache.remove(contactName);
        
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(contactName);
        editor.apply();
        
        Log.d(TAG, "Contact removed from cache: " + contactName);
    }
}