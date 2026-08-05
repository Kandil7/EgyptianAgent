package com.egyptian.agent.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Model Cache Manager
 * Manages caching of AI models for improved startup performance
 */
public class ModelCacheManager {
    private static final String TAG = "ModelCacheManager";
    private static final String PREFS_NAME = "model_cache";
    private static final long DEFAULT_MAX_SIZE_BYTES = 500 * 1024 * 1024; // 500 MB default
    
    private static long maxSizeBytes = DEFAULT_MAX_SIZE_BYTES;
    private static Map<String, ModelEntry> cache = new HashMap<>();
    private static SharedPreferences sharedPreferences;
    private static File cacheDir;
    
    /**
     * Model cache entry
     */
    public static class ModelEntry {
        public String modelPath;
        public String modelHash;
        public long sizeBytes;
        public long lastAccessed;
        public boolean isValid;
        
        public ModelEntry(String path, long size) {
            this.modelPath = path;
            this.modelHash = computeHash(path);
            this.sizeBytes = size;
            this.lastAccessed = System.currentTimeMillis();
            this.isValid = true;
        }
        
        private String computeHash(String path) {
            // Simple hash based on path and modification time
            File file = new File(path);
            if (file.exists()) {
                return Integer.toHexString(path.hashCode() + (int)(file.lastModified() & 0xFFFFFFFFL));
            }
            return Integer.toHexString(path.hashCode());
        }
    }
    
    /**
     * Initialize the cache manager
     */
    public static void init(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        cacheDir = new File(context.getCacheDir(), "models");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        Log.d(TAG, "ModelCacheManager initialized with max size: " + maxSizeBytes + " bytes");
    }
    
    /**
     * Set the maximum cache size in bytes
     */
    public static void setSize(long sizeBytes) {
        maxSizeBytes = sizeBytes;
        Log.d(TAG, "ModelCacheManager max size set to: " + sizeBytes + " bytes (" + (sizeBytes / 1024 / 1024) + " MB)");
        
        // Trim cache if needed
        while (getCurrentCacheSize() > maxSizeBytes) {
            trimCache();
        }
    }
    
    /**
     * Get the current cache size
     */
    public static long getSize() {
        return getCurrentCacheSize();
    }
    
    /**
     * Get the maximum cache size
     */
    public static long getMaxSize() {
        return maxSizeBytes;
    }
    
    /**
     * Add a model to the cache
     */
    public static void put(String modelId, ModelEntry entry) {
        if (getCurrentCacheSize() + entry.sizeBytes > maxSizeBytes) {
            trimCache();
        }
        cache.put(modelId, entry);
        Log.d(TAG, "Added model to cache: " + modelId + " (" + entry.sizeBytes + " bytes)");
    }
    
    /**
     * Get a model from the cache
     */
    public static ModelEntry get(String modelId) {
        ModelEntry entry = cache.get(modelId);
        if (entry != null) {
            entry.lastAccessed = System.currentTimeMillis();
        }
        return entry;
    }
    
    /**
     * Remove a model from the cache
     */
    public static void remove(String modelId) {
        ModelEntry entry = cache.remove(modelId);
        if (entry != null) {
            // Delete the cached model file
            File modelFile = new File(entry.modelPath);
            if (modelFile.exists()) {
                modelFile.delete();
            }
        }
        Log.d(TAG, "Removed model from cache: " + modelId);
    }
    
    /**
     * Clear the entire cache
     */
    public static void clear() {
        // Delete all cached model files
        if (cacheDir != null && cacheDir.exists()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        cache.clear();
        Log.d(TAG, "Model cache cleared");
    }
    
    /**
     * Trim the cache by removing least recently used models
     */
    private static void trimCache() {
        if (cache.isEmpty()) return;
        
        String leastUsedKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, ModelEntry> entry : cache.entrySet()) {
            if (entry.getValue().lastAccessed < oldestTime) {
                oldestTime = entry.getValue().lastAccessed;
                leastUsedKey = entry.getKey();
            }
        }
        
        if (leastUsedKey != null) {
            ModelEntry removed = cache.remove(leastUsedKey);
            if (removed != null) {
                File modelFile = new File(removed.modelPath);
                if (modelFile.exists()) {
                    modelFile.delete();
                }
            }
            Log.d(TAG, "Trimmed LRU model: " + leastUsedKey);
        }
    }
    
    /**
     * Get the current total cache size in bytes
     */
    private static long getCurrentCacheSize() {
        long totalSize = 0;
        for (ModelEntry entry : cache.values()) {
            totalSize += entry.sizeBytes;
        }
        return totalSize;
    }
    
    /**
     * Check if a model is cached and valid
     */
    public static boolean isCached(String modelId) {
        ModelEntry entry = cache.get(modelId);
        return entry != null && entry.isValid && new File(entry.modelPath).exists();
    }
    
    /**
     * Invalidate a cached model
     */
    public static void invalidate(String modelId) {
        ModelEntry entry = cache.get(modelId);
        if (entry != null) {
            entry.isValid = false;
        }
    }
    
    /**
     * Get cache directory
     */
    public static File getCacheDir() {
        return cacheDir;
    }
    
    /**
     * Save cache metadata to persistent storage
     */
    public static void saveToPrefs() {
        if (sharedPreferences == null) return;
        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("cache_count", cache.size());
        editor.putLong("cache_size", getCurrentCacheSize());
        editor.apply();
        
        Log.d(TAG, "Cache metadata saved to preferences");
    }
    
    /**
     * Load cache metadata from persistent storage
     */
    public static void loadFromPrefs() {
        if (sharedPreferences == null) return;
        
        int count = sharedPreferences.getInt("cache_count", 0);
        long size = sharedPreferences.getLong("cache_size", 0);
        Log.d(TAG, "Loaded cache metadata: " + count + " models, " + size + " bytes");
    }
}
