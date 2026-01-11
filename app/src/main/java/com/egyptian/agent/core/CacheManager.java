package com.egyptian.agent.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.ContactsContract;
import android.util.Log;
import android.database.Cursor;

import com.egyptian.agent.stt.EgyptianNormalizer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Cache manager for contacts and previous results
 * Optimizes performance by caching frequently accessed data
 */
public class CacheManager {
    private static final String TAG = "CacheManager";
    private static final String CACHE_PREFS_NAME = "egyptian_agent_cache";
    
    private final Context context;
    private final DeviceClassDetector.DeviceClass deviceClass;
    private final SharedPreferences sharedPreferences;
    
    // In-memory caches
    private final Map<String, Object> memoryCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    
    // Cache for contacts
    private final Map<String, String> contactCache = new ConcurrentHashMap<>();
    private volatile boolean contactsLoaded = false;
    
    // Cache for previous results
    private final Map<String, Object> resultsCache = new ConcurrentHashMap<>();
    
    // Lock for thread-safe operations
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    // Cache configuration based on device class
    private final int maxMemoryCacheSize;
    private final long cacheTTL; // Time-to-live in milliseconds
    
    public CacheManager(Context context) {
        this.context = context;
        this.deviceClass = ((MainApplication) context.getApplicationContext()).getDeviceClass();
        this.sharedPreferences = context.getSharedPreferences(CACHE_PREFS_NAME, Context.MODE_PRIVATE);
        
        // Configure cache size based on device class
        switch (deviceClass) {
            case LOW:
                maxMemoryCacheSize = 50; // Small cache for low-end devices
                cacheTTL = 5 * 60 * 1000; // 5 minutes TTL
                break;
            case MID:
                maxMemoryCacheSize = 100; // Moderate cache for mid-range devices
                cacheTTL = 10 * 60 * 1000; // 10 minutes TTL
                break;
            case HIGH:
                maxMemoryCacheSize = 200; // Larger cache for high-end devices
                cacheTTL = 15 * 60 * 1000; // 15 minutes TTL
                break;
            case ELITE:
                maxMemoryCacheSize = 500; // Large cache for elite devices
                cacheTTL = 30 * 60 * 1000; // 30 minutes TTL
                break;
            default:
                maxMemoryCacheSize = 100;
                cacheTTL = 10 * 60 * 1000;
        }
        
        Log.i(TAG, "CacheManager initialized for device class: " + deviceClass.name() + 
              ", max cache size: " + maxMemoryCacheSize + ", TTL: " + cacheTTL + "ms");
    }
    
    /**
     * Loads contacts into cache
     */
    public void loadContactsToCache() {
        if (contactsLoaded) {
            Log.d(TAG, "Contacts already loaded in cache");
            return;
        }
        
        Log.d(TAG, "Loading contacts to cache");
        
        try {
            Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null, null, null
            );
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(0);
                    String number = cursor.getString(1);
                    
                    if (name != null && number != null) {
                        // Add the original name
                        contactCache.put(name.toLowerCase(), number);
                        
                        // Add normalized versions for Egyptian dialect
                        String normalized = EgyptianNormalizer.normalizeContactName(name);
                        if (!normalized.equals(name)) {
                            contactCache.put(normalized.toLowerCase(), number);
                        }
                    }
                }
                cursor.close();
            }
            
            contactsLoaded = true;
            Log.i(TAG, "Loaded " + contactCache.size() + " contacts to cache");
        } catch (Exception e) {
            Log.e(TAG, "Error loading contacts to cache", e);
        }
    }
    
    /**
     * Gets a contact by name from cache
     */
    public String getContactNumber(String contactName) {
        if (!contactsLoaded) {
            loadContactsToCache();
        }
        
        if (contactName == null) {
            return null;
        }
        
        String normalizedContact = contactName.toLowerCase();
        
        // Try exact match first
        String number = contactCache.get(normalizedContact);
        if (number != null) {
            return number;
        }
        
        // Try normalized match
        String normalized = EgyptianNormalizer.normalizeContactName(contactName).toLowerCase();
        number = contactCache.get(normalized);
        if (number != null) {
            return number;
        }
        
        // Try partial match
        for (Map.Entry<String, String> entry : contactCache.entrySet()) {
            if (entry.getKey().contains(normalizedContact) || 
                normalizedContact.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Gets all cached contacts
     */
    public Map<String, String> getAllContacts() {
        if (!contactsLoaded) {
            loadContactsToCache();
        }
        
        return new HashMap<>(contactCache);
    }
    
    /**
     * Caches a result
     */
    public void cacheResult(String key, Object result) {
        cacheLock.writeLock().lock();
        try {
            // Clean up expired entries if cache is getting too large
            if (memoryCache.size() >= maxMemoryCacheSize) {
                cleanupExpiredEntries();
            }
            
            memoryCache.put(key, result);
            resultsCache.put(key, result);
            cacheTimestamps.put(key, System.currentTimeMillis());
            
            Log.d(TAG, "Cached result with key: " + key);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Gets a cached result
     */
    public Object getCachedResult(String key) {
        cacheLock.readLock().lock();
        try {
            // Check if entry is expired
            Long timestamp = cacheTimestamps.get(key);
            if (timestamp != null && (System.currentTimeMillis() - timestamp) > cacheTTL) {
                // Entry is expired, remove it
                removeCachedResult(key);
                return null;
            }
            
            Object result = memoryCache.get(key);
            if (result != null) {
                Log.d(TAG, "Retrieved cached result with key: " + key);
            }
            return result;
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Removes a cached result
     */
    public void removeCachedResult(String key) {
        cacheLock.writeLock().lock();
        try {
            memoryCache.remove(key);
            resultsCache.remove(key);
            cacheTimestamps.remove(key);
            
            Log.d(TAG, "Removed cached result with key: " + key);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Checks if a result is cached
     */
    public boolean isResultCached(String key) {
        cacheLock.readLock().lock();
        try {
            // Check if entry is expired
            Long timestamp = cacheTimestamps.get(key);
            if (timestamp != null && (System.currentTimeMillis() - timestamp) > cacheTTL) {
                // Entry is expired, remove it
                removeCachedResult(key);
                return false;
            }
            
            return memoryCache.containsKey(key);
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Gets the size of the memory cache
     */
    public int getCacheSize() {
        return memoryCache.size();
    }
    
    /**
     * Cleans up expired entries from the cache
     */
    private void cleanupExpiredEntries() {
        cacheLock.writeLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            List<String> expiredKeys = new ArrayList<>();
            
            for (Map.Entry<String, Long> entry : cacheTimestamps.entrySet()) {
                if ((currentTime - entry.getValue()) > cacheTTL) {
                    expiredKeys.add(entry.getKey());
                }
            }
            
            for (String key : expiredKeys) {
                memoryCache.remove(key);
                resultsCache.remove(key);
                cacheTimestamps.remove(key);
            }
            
            Log.d(TAG, "Cleaned up " + expiredKeys.size() + " expired cache entries");
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Clears all caches
     */
    public void clearAllCaches() {
        cacheLock.writeLock().lock();
        try {
            memoryCache.clear();
            resultsCache.clear();
            cacheTimestamps.clear();
            contactCache.clear();
            contactsLoaded = false;
            
            Log.i(TAG, "Cleared all caches");
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Gets cache statistics
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
            getCacheSize(),
            contactCache.size(),
            maxMemoryCacheSize,
            cacheTTL,
            deviceClass
        );
    }
    
    /**
     * Cache statistics class
     */
    public static class CacheStats {
        public final int memoryCacheSize;
        public final int contactCacheSize;
        public final int maxMemoryCacheSize;
        public final long cacheTTL;
        public final DeviceClassDetector.DeviceClass deviceClass;
        
        public CacheStats(int memoryCacheSize, int contactCacheSize, 
                         int maxMemoryCacheSize, long cacheTTL, 
                         DeviceClassDetector.DeviceClass deviceClass) {
            this.memoryCacheSize = memoryCacheSize;
            this.contactCacheSize = contactCacheSize;
            this.maxMemoryCacheSize = maxMemoryCacheSize;
            this.cacheTTL = cacheTTL;
            this.deviceClass = deviceClass;
        }
        
        @Override
        public String toString() {
            return String.format(
                "CacheStats{memorySize=%d, contactSize=%d, maxSize=%d, TTL=%dms, deviceClass=%s}",
                memoryCacheSize, contactCacheSize, maxMemoryCacheSize, cacheTTL, deviceClass.name()
            );
        }
    }
    
    /**
     * Cleans up resources
     */
    public void destroy() {
        clearAllCaches();
        Log.i(TAG, "CacheManager destroyed");
    }
}