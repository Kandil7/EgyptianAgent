package com.egyptian.agent.performance;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.CrashLogger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Performance monitoring utility for production environment
 * Monitors system resources and application performance
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    private static final long MONITORING_INTERVAL_MS = 30000; // 30 seconds
    
    private static PerformanceMonitor instance;
    private Context context;
    private ScheduledExecutorService scheduler;
    private volatile boolean isMonitoring = false;
    private Handler mainHandler;
    
    private PerformanceMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized PerformanceMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new PerformanceMonitor(context);
        }
        return instance;
    }
    
    /**
     * Start performance monitoring
     */
    public void startMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "Performance monitoring already started");
            return;
        }
        
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        
        scheduler.scheduleAtFixedRate(this::performCheck, 0, MONITORING_INTERVAL_MS, TimeUnit.MILLISECONDS);
        isMonitoring = true;
        
        Log.i(TAG, "Performance monitoring started");
    }
    
    /**
     * Stop performance monitoring
     */
    public void stopMonitoring() {
        if (!isMonitoring) {
            Log.w(TAG, "Performance monitoring already stopped");
            return;
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        
        isMonitoring = false;
        
        Log.i(TAG, "Performance monitoring stopped");
    }
    
    /**
     * Perform a single performance check
     */
    private void performCheck() {
        try {
            // Check memory usage
            checkMemoryUsage();
            
            // Check CPU usage
            checkCpuUsage();
            
            // Check battery level
            checkBatteryLevel();
            
            // Check storage space
            checkStorageSpace();
            
        } catch (Exception e) {
            Log.e(TAG, "Error during performance check", e);
            CrashLogger.logError(context, e);
        }
    }
    
    /**
     * Check memory usage and take action if needed
     */
    private void checkMemoryUsage() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        long totalMemory = memoryInfo.totalMem;
        long availableMemory = memoryInfo.availMem;
        long usedMemory = totalMemory - availableMemory;
        double memoryUsagePercent = (double) usedMemory / totalMemory * 100.0;
        
        Log.d(TAG, String.format("Memory: %.2f%% used (%d MB / %d MB)", 
                memoryUsagePercent, 
                usedMemory / (1024 * 1024), 
                totalMemory / (1024 * 1024)));
        
        // Check if memory usage is critically high
        if (memoryUsagePercent > 85.0) {
            Log.w(TAG, "High memory usage detected: " + String.format("%.2f%%", memoryUsagePercent));
            
            // Take action on main thread
            mainHandler.post(() -> {
                TTSManager.speak(context, "الجهاز معبان شوية. بخليه شوية.");
                
                // Trigger garbage collection
                System.gc();
                
                // Log the event
                CrashLogger.logWarning(context, String.format("High memory usage: %.2f%%", memoryUsagePercent));
            });
        }
    }
    
    /**
     * Check CPU usage
     */
    private void checkCpuUsage() {
        // For Android, we can't directly get CPU usage percentage easily
        // Instead, we'll monitor the application's CPU time
        Debug.MemoryInfo memInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memInfo);
        
        // Log dalvik/native heap usage
        Log.d(TAG, String.format("Dalvik Heap: %d KB, Native Heap: %d KB", 
                memInfo.getMemoryStat("dalvik.heap.used"), 
                memInfo.getMemoryStat("native.heap.used")));
    }
    
    /**
     * Check battery level
     */
    private void checkBatteryLevel() {
        // Get battery level using battery manager
        android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
        android.content.Intent batteryStatus = context.registerReceiver(null, ifilter);
        
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
            
            float batteryPct = level * 100.0f / scale;
            
            Log.d(TAG, String.format("Battery: %.2f%%", batteryPct));
            
            // Warn if battery is low
            if (batteryPct < 15.0) {
                Log.w(TAG, "Low battery detected: " + String.format("%.2f%%", batteryPct));
                
                mainHandler.post(() -> {
                    TTSManager.speak(context, "البطارية ضعيفة. فضل الشحن.");
                    
                    // Log the event
                    CrashLogger.logWarning(context, String.format("Low battery: %.2f%%", batteryPct));
                });
            }
        }
    }
    
    /**
     * Check available storage space
     */
    private void checkStorageSpace() {
        java.io.File dataDir = Environment.getDataDirectory();
        long totalSpace = dataDir.getTotalSpace();
        long freeSpace = dataDir.getFreeSpace();
        double freeSpacePercent = (double) freeSpace / totalSpace * 100.0;
        
        Log.d(TAG, String.format("Storage: %.2f%% free (%d GB / %d GB)", 
                freeSpacePercent, 
                freeSpace / (1024 * 1024 * 1024), 
                totalSpace / (1024 * 1024 * 1024)));
        
        // Warn if storage is low
        if (freeSpacePercent < 10.0) {
            Log.w(TAG, "Low storage space: " + String.format("%.2f%% free", freeSpacePercent));
            
            mainHandler.post(() -> {
                TTSManager.speak(context, "المساحة خلصت شوية. ممكن تحذف حاجات.");
                
                // Log the event
                CrashLogger.logWarning(context, String.format("Low storage: %.2f%% free", freeSpacePercent));
            });
        }
    }
    
    /**
     * Get current memory usage statistics
     */
    public MemoryStats getMemoryStats() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        long totalMemory = memoryInfo.totalMem;
        long availableMemory = memoryInfo.availMem;
        long usedMemory = totalMemory - availableMemory;
        double memoryUsagePercent = (double) usedMemory / totalMemory * 100.0;
        
        return new MemoryStats(totalMemory, availableMemory, usedMemory, memoryUsagePercent);
    }
    
    /**
     * Get current battery level
     */
    public float getBatteryLevel() {
        android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
        android.content.Intent batteryStatus = context.registerReceiver(null, ifilter);
        
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
            return level * 100.0f / scale;
        }
        
        return -1.0f; // Unknown
    }
    
    /**
     * Check if monitoring is currently active
     */
    public boolean isMonitoringActive() {
        return isMonitoring;
    }
    
    /**
     * Memory statistics data class
     */
    public static class MemoryStats {
        public final long totalMemory;
        public final long availableMemory;
        public final long usedMemory;
        public final double usagePercentage;
        
        public MemoryStats(long totalMemory, long availableMemory, long usedMemory, double usagePercentage) {
            this.totalMemory = totalMemory;
            this.availableMemory = availableMemory;
            this.usedMemory = usedMemory;
            this.usagePercentage = usagePercentage;
        }
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        stopMonitoring();
    }
}