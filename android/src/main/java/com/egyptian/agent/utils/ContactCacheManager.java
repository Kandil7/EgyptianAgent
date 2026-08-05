package com.egyptian.agent.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contact Cache Manager
 * Manages caching of contact information for improved performance
 */
public class ContactCacheManager {
    private static final String TAG = "ContactCacheManager";
    private static final String PREFS_NAME = "contact_cache";
    private static final int DEFAULT_MAX_SIZE = 100;
    
    private static int maxSize = DEFAULT_MAX_SIZE;
    private static Map<String, ContactEntry> cache = new HashMap<>();
    private static SharedPreferences sharedPreferences;
    
    /**
     * Contact cache entry
     */
    public static class ContactEntry {
        public String name;
        public String phoneNumber;
        public String normalizedNumber;
        public long lastAccessed;
        
        public ContactEntry(String name, String phoneNumber) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.normalizedNumber = normalizeNumber(phoneNumber);
            this.lastAccessed = System.currentTimeMillis();
        }
        
        private String normalizeNumber(String number) {
            if (number == null) return "";
            return number.replaceAll("[^0-9+]", "");
        }
    }
    
    /**
     * Initialize the cache manager
     */
    public static void init(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "ContactCacheManager initialized with max size: " + maxSize);
    }
    
    /**
     * Set the maximum cache size
     */
    public static void setSize(int size) {
        maxSize = size;
        Log.d(TAG, "ContactCacheManager max size set to: " + size);
        
        // Trim cache if needed
        while (cache.size() > maxSize) {
            trimCache();
        }
    }
    
    /**
     * Get the current cache size
     */
    public static int getSize() {
        return cache.size();
    }
    
    /**
     * Get the maximum cache size
     */
    public static int getMaxSize() {
        return maxSize;
    }
    
    /**
     * Add a contact to the cache
     */
    public static void put(String key, ContactEntry entry) {
        if (cache.size() >= maxSize) {
            trimCache();
        }
        cache.put(key, entry);
        Log.d(TAG, "Added contact to cache: " + key);
    }
    
    /**
     * Get a contact from the cache
     */
    public static ContactEntry get(String key) {
        ContactEntry entry = cache.get(key);
        if (entry != null) {
            entry.lastAccessed = System.currentTimeMillis();
        }
        return entry;
    }
    
    /**
     * Remove a contact from the cache
     */
    public static void remove(String key) {
        cache.remove(key);
        Log.d(TAG, "Removed contact from cache: " + key);
    }
    
    /**
     * Clear the entire cache
     */
    public static void clear() {
        cache.clear();
        Log.d(TAG, "Contact cache cleared");
    }
    
    /**
     * Trim the cache by removing least recently used entries
     */
    private static void trimCache() {
        if (cache.isEmpty()) return;
        
        String leastUsedKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, ContactEntry> entry : cache.entrySet()) {
            if (entry.getValue().lastAccessed < oldestTime) {
                oldestTime = entry.getValue().lastAccessed;
                leastUsedKey = entry.getKey();
            }
        }
        
        if (leastUsedKey != null) {
            cache.remove(leastUsedKey);
            Log.d(TAG, "Trimmed LRU contact: " + leastUsedKey);
        }
    }
    
    /**
     * Get all cached contacts
     */
    public static List<ContactEntry> getAll() {
        return new ArrayList<>(cache.values());
    }
    
    /**
     * Check if cache contains a key
     */
    public static boolean contains(String key) {
        return cache.containsKey(key);
    }
    
    /**
     * Save cache to persistent storage
     */
    public static void saveToPrefs() {
        if (sharedPreferences == null) return;
        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("cache_size", cache.size());
        editor.apply();
        
        Log.d(TAG, "Cache saved to preferences");
    }
    
    /**
     * Load cache from persistent storage
     */
    public static void loadFromPrefs() {
        if (sharedPreferences == null) return;
        
        int size = sharedPreferences.getInt("cache_size", 0);
        Log.d(TAG, "Loaded cache size from preferences: " + size);
    }
}
