package com.egyptian.agent.performance;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * PerformanceMonitor - Monitors and optimizes performance for Egyptian Agent
 * Tracks memory usage, CPU, battery, and other performance metrics
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    private static final long MONITOR_INTERVAL_MS = 5000; // 5 seconds
    private static final int MAX_HISTORY_SIZE = 100; // Max samples to keep
    
    private static PerformanceMonitor instance;
    private Context context;
    private ScheduledExecutorService scheduler;
    private Handler mainHandler;
    private volatile boolean isMonitoring = false;
    
    // Performance metrics
    private List<MemorySample> memoryHistory;
    private List<CpuSample> cpuHistory;
    private List<BatterySample> batteryHistory;
    private Map<String, Long> operationTimings; // Track timing of operations
    
    // Thresholds for alerts
    private static final float MEMORY_THRESHOLD = 0.85f; // 85% memory usage threshold
    private static final float BATTERY_THRESHOLD = 0.20f; // 20% battery threshold
    private static final long OPERATION_SLOW_THRESHOLD_MS = 2000; // 2 seconds
    
    private PerformanceMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.memoryHistory = Collections.synchronizedList(new ArrayList<>());
        this.cpuHistory = Collections.synchronizedList(new ArrayList<>());
        this.batteryHistory = Collections.synchronizedList(new ArrayList<>());
        this.operationTimings = new ConcurrentHashMap<>();
    }
    
    public static synchronized PerformanceMonitor getInstance(Context context) {
        if (instance == null) {
            instance = new PerformanceMonitor(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Starts performance monitoring
     */
    public void startMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "Performance monitoring already started");
            return;
        }
        
        isMonitoring = true;
        
        // Schedule periodic monitoring
        scheduler.scheduleAtFixedRate(this::collectMetrics, 0, MONITOR_INTERVAL_MS, TimeUnit.MILLISECONDS);
        
        Log.i(TAG, "Performance monitoring started");
    }
    
    /**
     * Stops performance monitoring
     */
    public void stopMonitoring() {
        isMonitoring = false;
        Log.i(TAG, "Performance monitoring stopped");
    }
    
    /**
     * Collects performance metrics
     */
    private void collectMetrics() {
        if (!isMonitoring) return;
        
        try {
            // Collect memory metrics
            MemorySample memSample = collectMemoryMetrics();
            memoryHistory.add(memSample);
            if (memoryHistory.size() > MAX_HISTORY_SIZE) {
                memoryHistory.remove(0); // Remove oldest sample
            }
            
            // Collect CPU metrics
            CpuSample cpuSample = collectCpuMetrics();
            cpuHistory.add(cpuSample);
            if (cpuHistory.size() > MAX_HISTORY_SIZE) {
                cpuHistory.remove(0); // Remove oldest sample
            }
            
            // Collect battery metrics
            BatterySample batterySample = collectBatteryMetrics();
            batteryHistory.add(batterySample);
            if (batteryHistory.size() > MAX_HISTORY_SIZE) {
                batteryHistory.remove(0); // Remove oldest sample
            }
            
            // Check for performance issues
            checkPerformanceIssues(memSample, cpuSample, batterySample);
            
        } catch (Exception e) {
            Log.e(TAG, "Error collecting metrics", e);
        }
    }
    
    /**
     * Collects memory metrics
     */
    private MemorySample collectMemoryMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        float memoryUsagePercent = (float) usedMemory / maxMemory;
        
        // Get memory info from Debug class
        Debug.MemoryInfo memInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memInfo);
        long nativeHeapSize = memInfo.getTotalPss();
        
        return new MemorySample(System.currentTimeMillis(), usedMemory, maxMemory, 
                               memoryUsagePercent, nativeHeapSize);
    }
    
    /**
     * Collects CPU metrics
     */
    private CpuSample collectCpuMetrics() {
        // Get CPU usage from /proc/stat
        long cpuUsage = getCpuUsage();
        long uptime = SystemClock.elapsedRealtime();
        
        return new CpuSample(System.currentTimeMillis(), cpuUsage, uptime);
    }
    
    /**
     * Gets CPU usage percentage
     */
    private long getCpuUsage() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"));
            String line = reader.readLine();
            reader.close();
            
            if (line != null && line.startsWith("cpu ")) {
                String[] tokens = line.split("\\s+");
                
                long user = Long.parseLong(tokens[1]);
                long nice = Long.parseLong(tokens[2]);
                long system = Long.parseLong(tokens[3]);
                long idle = Long.parseLong(tokens[4]);
                
                long total = user + nice + system + idle;
                long usage = total - idle;
                
                return (usage * 100) / total;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading CPU usage", e);
        }
        
        return -1; // Error
    }
    
    /**
     * Collects battery metrics
     */
    private BatterySample collectBatteryMetrics() {
        Intent batteryIntent = context.registerReceiver(null, 
            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level * 100.0f / scale;
        
        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL;
        
        int health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        String healthStatus = getHealthStatus(health);
        
        return new BatterySample(System.currentTimeMillis(), batteryPct, isCharging, healthStatus);
    }
    
    /**
     * Converts battery health integer to string
     */
    private String getHealthStatus(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_COLD: return "COLD";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "DEAD";
            case BatteryManager.BATTERY_HEALTH_GOOD: return "GOOD";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "OVERHEAT";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "OVER_VOLTAGE";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "FAILURE";
            case BatteryManager.BATTERY_HEALTH_UNKNOWN: return "UNKNOWN";
            default: return "UNKNOWN";
        }
    }
    
    /**
     * Checks for performance issues
     */
    private void checkPerformanceIssues(MemorySample memSample, CpuSample cpuSample, BatterySample batterySample) {
        // Check memory usage
        if (memSample.getMemoryUsagePercent() > MEMORY_THRESHOLD) {
            Log.w(TAG, "HIGH MEMORY USAGE ALERT: " + 
                  String.format("%.2f%%", memSample.getMemoryUsagePercent() * 100));
            
            // Trigger memory optimization
            triggerMemoryOptimization();
        }
        
        // Check battery level
        if (!batterySample.isCharging() && batterySample.getBatteryLevel() < BATTERY_THRESHOLD * 100) {
            Log.w(TAG, "LOW BATTERY ALERT: " + 
                  String.format("%.2f%%", batterySample.getBatteryLevel()));
            
            // Trigger battery optimization
            triggerBatteryOptimization();
        }
    }
    
    /**
     * Triggers memory optimization
     */
    private void triggerMemoryOptimization() {
        // In a real implementation, this would:
        // 1. Clear non-critical caches
        // 2. Reduce model precision if possible
        // 3. Temporarily disable non-critical features
        // 4. Suggest user to close other apps
        
        Log.i(TAG, "Triggering memory optimization...");
        
        // Post to main thread to avoid blocking the monitoring thread
        mainHandler.post(() -> {
            // Perform memory optimization tasks
            performMemoryOptimization();
        });
    }
    
    /**
     * Performs actual memory optimization tasks
     */
    private void performMemoryOptimization() {
        // Force garbage collection
        System.gc();
        
        // Log memory stats after optimization
        MemorySample after = collectMemoryMetrics();
        Log.i(TAG, "Memory optimization completed. Usage after GC: " + 
              String.format("%.2f%%", after.getMemoryUsagePercent() * 100));
    }
    
    /**
     * Triggers battery optimization
     */
    private void triggerBatteryOptimization() {
        // In a real implementation, this would:
        // 1. Reduce speech recognition sensitivity
        // 2. Increase intervals between operations
        // 3. Disable non-critical features temporarily
        // 4. Reduce screen brightness if applicable
        
        Log.i(TAG, "Triggering battery optimization...");
    }
    
    /**
     * Starts timing an operation
     */
    public void startTiming(String operationName) {
        operationTimings.put(operationName, System.currentTimeMillis());
    }
    
    /**
     * Ends timing an operation and logs if it was slow
     */
    public void endTiming(String operationName) {
        Long startTime = operationTimings.remove(operationName);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            
            // Log if operation took longer than threshold
            if (duration > OPERATION_SLOW_THRESHOLD_MS) {
                Log.w(TAG, "SLOW OPERATION: " + operationName + " took " + duration + "ms");
            }
        }
    }
    
    /**
     * Gets current memory statistics
     */
    public MemoryStats getCurrentMemoryStats() {
        if (memoryHistory.isEmpty()) {
            return null;
        }
        
        MemorySample latest = memoryHistory.get(memoryHistory.size() - 1);
        return new MemoryStats(latest.getUsedMemory(), latest.getMaxMemory(), 
                              latest.getMemoryUsagePercent());
    }
    
    /**
     * Gets average CPU usage over time
     */
    public float getAverageCpuUsage() {
        if (cpuHistory.isEmpty()) {
            return -1;
        }
        
        return (float) cpuHistory.stream()
            .mapToLong(CpuSample::getCpuUsage)
            .filter(usage -> usage >= 0) // Filter out error values
            .average()
            .orElse(-1);
    }
    
    /**
     * Gets current battery level
     */
    public float getCurrentBatteryLevel() {
        if (batteryHistory.isEmpty()) {
            return -1;
        }
        
        return batteryHistory.get(batteryHistory.size() - 1).getBatteryLevel();
    }
    
    /**
     * Gets memory usage history
     */
    public List<MemorySample> getMemoryHistory() {
        return new ArrayList<>(memoryHistory);
    }
    
    /**
     * Gets CPU usage history
     */
    public List<CpuSample> getCpuHistory() {
        return new ArrayList<>(cpuHistory);
    }
    
    /**
     * Gets battery history
     */
    public List<BatterySample> getBatteryHistory() {
        return new ArrayList<>(batteryHistory);
    }
    
    /**
     * Cleans up resources
     */
    public void cleanup() {
        stopMonitoring();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
    
    /**
     * Represents a memory sample
     */
    public static class MemorySample {
        private long timestamp;
        private long usedMemory;
        private long maxMemory;
        private float memoryUsagePercent;
        private long nativeHeapSize;
        
        public MemorySample(long timestamp, long usedMemory, long maxMemory, 
                           float memoryUsagePercent, long nativeHeapSize) {
            this.timestamp = timestamp;
            this.usedMemory = usedMemory;
            this.maxMemory = maxMemory;
            this.memoryUsagePercent = memoryUsagePercent;
            this.nativeHeapSize = nativeHeapSize;
        }
        
        // Getters
        public long getTimestamp() { return timestamp; }
        public long getUsedMemory() { return usedMemory; }
        public long getMaxMemory() { return maxMemory; }
        public float getMemoryUsagePercent() { return memoryUsagePercent; }
        public long getNativeHeapSize() { return nativeHeapSize; }
    }
    
    /**
     * Represents a CPU sample
     */
    public static class CpuSample {
        private long timestamp;
        private long cpuUsage; // Percentage (0-100)
        private long uptime;
        
        public CpuSample(long timestamp, long cpuUsage, long uptime) {
            this.timestamp = timestamp;
            this.cpuUsage = cpuUsage;
            this.uptime = uptime;
        }
        
        // Getters
        public long getTimestamp() { return timestamp; }
        public long getCpuUsage() { return cpuUsage; }
        public long getUptime() { return uptime; }
    }
    
    /**
     * Represents a battery sample
     */
    public static class BatterySample {
        private long timestamp;
        private float batteryLevel; // Percentage (0-100)
        private boolean isCharging;
        private String healthStatus;
        
        public BatterySample(long timestamp, float batteryLevel, boolean isCharging, String healthStatus) {
            this.timestamp = timestamp;
            this.batteryLevel = batteryLevel;
            this.isCharging = isCharging;
            this.healthStatus = healthStatus;
        }
        
        // Getters
        public long getTimestamp() { return timestamp; }
        public float getBatteryLevel() { return batteryLevel; }
        public boolean isCharging() { return isCharging; }
        public String getHealthStatus() { return healthStatus; }
    }
    
    /**
     * Represents memory statistics
     */
    public static class MemoryStats {
        private long usedMemory;
        private long maxMemory;
        private float usagePercent;
        
        public MemoryStats(long usedMemory, long maxMemory, float usagePercent) {
            this.usedMemory = usedMemory;
            this.maxMemory = maxMemory;
            this.usagePercent = usagePercent;
        }
        
        // Getters
        public long getUsedMemory() { return usedMemory; }
        public long getMaxMemory() { return maxMemory; }
        public float getUsagePercent() { return usagePercent; }
        
        public String toString() {
            return String.format("Memory: %d/%d MB (%.2f%%)", 
                               usedMemory / (1024 * 1024), 
                               maxMemory / (1024 * 1024), 
                               usagePercent * 100);
        }
    }
}