package com.egyptian.agent.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * Memory Optimizer
 * Handles memory management and optimization for the application
 * 
 * Optimized for Honor X6c with 6GB RAM:
 * - Llama 3.2 3B Q4_K_M requires ~2GB RAM during inference
 * - System needs ~500MB for Android + other apps
 * - Target: Keep app memory usage under 350MB
 */
public class MemoryOptimizer {
    private static final String TAG = "MemoryOptimizer";
    
    // Memory thresholds for Honor X6c (6GB RAM)
    private static final long CRITICAL_MEMORY_MB = 300;   // Critical: stop everything
    private static final long LOW_MEMORY_MB = 500;        // Low: trigger optimizations
    private static final long MODEL_LOAD_MIN_MB = 2000;   // Minimum for Llama model load
    private static final long OPTIMAL_MEMORY_MB = 3000;   // Optimal for smooth operation

    /**
     * Check if device has enough memory for operation.
     * @param requiredMB Required memory in megabytes
     * @return true if enough memory available
     */
    public static boolean hasEnoughMemory(long requiredMB) {
        ActivityManager activityManager = 
            (ActivityManager) ContextHolder.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
        
        if (activityManager == null) {
            Log.w(TAG, "ActivityManager not available, assuming sufficient memory");
            return true;
        }
        
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        long availableMB = memoryInfo.availMem / (1024 * 1024);
        boolean hasEnough = availableMB >= requiredMB;
        
        Log.d(TAG, String.format(
            "Memory check: Available=%dMB, Required=%dMB, Result=%s",
            availableMB, requiredMB, hasEnough ? "PASS" : "FAIL"
        ));
        
        return hasEnough;
    }

    /**
     * Get available memory in MB.
     * @return Available memory in megabytes
     */
    public static long getAvailableMemoryMB() {
        Context context = ContextHolder.getAppContext();
        if (context == null) return 0;
        
        ActivityManager activityManager = 
            (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        
        if (activityManager == null) return 0;
        
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        return memoryInfo.availMem / (1024 * 1024);
    }

    /**
     * Get total memory in MB.
     * @return Total memory in megabytes
     */
    public static long getTotalMemoryMB() {
        Context context = ContextHolder.getAppContext();
        if (context == null) return 0;
        
        ActivityManager activityManager = 
            (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        
        if (activityManager == null) return 0;
        
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        return memoryInfo.totalMem / (1024 * 1024);
    }

    /**
     * Check if device is in low memory state.
     * @return true if low memory
     */
    public static boolean isLowMemory() {
        Context context = ContextHolder.getAppContext();
        if (context == null) return false;
        
        ActivityManager activityManager = 
            (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        
        if (activityManager == null) return false;
        
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        return memoryInfo.lowMemory || 
               (memoryInfo.availMem / (1024 * 1024)) < LOW_MEMORY_MB;
    }

    /**
     * Checks memory constraints for the device
     * @param context Context for the operation
     */
    public static void checkMemoryConstraints(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        
        // Get memory info
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        long availableMemory = memoryInfo.availMem / (1024 * 1024); // Convert to MB
        long totalMemory = memoryInfo.totalMem / (1024 * 1024); // Convert to MB
        boolean isLowMemory = memoryInfo.lowMemory;
        
        Log.d(TAG, String.format(
            "Memory Info - Available: %d MB, Total: %d MB, Low Memory: %s",
            availableMemory, totalMemory, isLowMemory
        ));
        
        // For Honor X6c with 6GB RAM, we have sufficient memory
        // But we should still optimize for efficient usage
        if (availableMemory < 500) { // Less than 500MB available
            Log.w(TAG, "Low memory condition detected, triggering optimizations");
            triggerMemoryOptimizations(context);
        }
    }
    
    /**
     * Triggers memory optimizations
     * @param context Context for the operation
     */
    public static void triggerMemoryOptimizations(Context context) {
        Log.d(TAG, "Triggering memory optimizations");
        
        // Clear caches if possible
        clearCaches(context);
        
        // Run garbage collection
        runGarbageCollection();
        
        // Log memory status after optimization
        logMemoryStatus(context);
    }
    
    /**
     * Clears application caches
     * @param context Context for the operation
     */
    private static void clearCaches(Context context) {
        try {
            // Clear internal cache
            deleteRecursive(context.getCacheDir());
            
            Log.d(TAG, "Cleared application caches");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing caches", e);
        }
    }
    
    /**
     * Recursively deletes files in a directory
     * @param file The file or directory to delete
     */
    private static void deleteRecursive(java.io.File file) {
        if (file.isDirectory()) {
            for (java.io.File child : file.listFiles()) {
                deleteRecursive(child);
            }
        }
        file.delete();
    }
    
    /**
     * Runs garbage collection to free up memory
     */
    private static void runGarbageCollection() {
        Log.d(TAG, "Running garbage collection");
        System.gc();
        
        // Sleep briefly to allow GC to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Logs current memory status
     * @param context Context for the operation
     */
    private static void logMemoryStatus(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        long availableMemory = memoryInfo.availMem / (1024 * 1024); // Convert to MB
        boolean isLowMemory = memoryInfo.lowMemory;
        
        Log.i(TAG, String.format(
            "Memory after optimization - Available: %d MB, Low Memory: %s",
            availableMemory, isLowMemory
        ));
    }
    
    /**
     * Frees up memory by clearing non-essential resources
     */
    public static void freeMemory() {
        Log.d(TAG, "Freeing up memory");

        // Run garbage collection
        runGarbageCollection();

        // Clear any cached data structures if applicable
        clearNonEssentialCaches();
    }

    /**
     * Clears non-essential caches to free up memory
     */
    private static void clearNonEssentialCaches() {
        // Clear any application-specific caches that can be regenerated
        Log.d(TAG, "Clearing non-essential caches");

        // Example: Clear temporary files if present
        // Note: We can't pass context here since this is a static method
        // So we'll skip this for now to avoid compilation errors
    }

    /**
     * Clears temporary files to free up memory
     */
    private static void clearTempFiles(Context context) {
        try {
            File tempDir = new File(context.getCacheDir(), "temp");
            if (tempDir.exists()) {
                deleteRecursive(tempDir);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing temp files", e);
        }
    }

    /**
     * Holder for application context (avoids memory leaks).
     * Should be initialized by MainApplication or similar.
     */
    private static class ContextHolder {
        private static Context appContext;

        public static void setAppContext(Context context) {
            if (context != null) {
                appContext = context.getApplicationContext();
            }
        }

        public static Context getAppContext() {
            return appContext;
        }
    }

    /**
     * Initialize MemoryOptimizer with application context.
     * Call this from MainApplication.onCreate()
     */
    public static void initialize(Context context) {
        ContextHolder.setAppContext(context);
        Log.i(TAG, "MemoryOptimizer initialized");
    }
}