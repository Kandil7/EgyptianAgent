package com.egyptian.agent.core;

import android.app.Application;
import android.util.Log;
import com.egyptian.agent.utils.CrashLogger;
import com.egyptian.agent.system.SystemPrivilegeManager;
import com.egyptian.agent.performance.PerformanceMonitor;
import com.egyptian.agent.security.DataEncryptionManager;
import com.egyptian.agent.backup.BackupRestoreManager;
import com.egyptian.agent.feedback.UserFeedbackSystem;
import com.egyptian.agent.performance.HonorX6cPerformanceOptimizer;

public class MainApplication extends Application {

    private static final String TAG = "MainApplication";
    private PerformanceMonitor performanceMonitor;
    private BackupRestoreManager backupRestoreManager;
    private UserFeedbackSystem userFeedbackSystem;
    private DeviceClassDetector.DeviceClass deviceClass;
    private HonorX6cPerformanceOptimizer performanceOptimizer;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Egyptian Agent Application starting");

        // Detect device class early in the application lifecycle
        deviceClass = DeviceClassDetector.detectDevice(this);
        Log.i(TAG, "Device class detected: " + deviceClass.name());

        // Initialize crash logger
        CrashLogger.registerGlobalExceptionHandler(this);

        // Initialize system privileges via Shizuku
        SystemPrivilegeManager.initialize(this);

        // Initialize data encryption
        DataEncryptionManager.getInstance(this);

        // Initialize performance monitoring
        performanceMonitor = PerformanceMonitor.getInstance(this);
        performanceMonitor.startMonitoring();

        // Initialize performance optimizer for Honor X6c
        performanceOptimizer = new HonorX6cPerformanceOptimizer(this);

        // Check if we're running on Honor X6c and apply optimizations
        if (HonorX6cPerformanceOptimizer.isHonorX6c()) {
            Log.i(TAG, "Honor X6c device detected, applying optimizations");
            performanceOptimizer.optimizeForHonorX6c();
        } else {
            Log.w(TAG, "Non-Honor X6c device detected, skipping device-specific optimizations");
        }

        // Initialize backup/restore manager
        backupRestoreManager = BackupRestoreManager.getInstance(this);

        // Initialize user feedback system
        userFeedbackSystem = UserFeedbackSystem.getInstance(this);

        // Initialize TTS engine
        TTSManager.initialize(this);

        // Additional initialization can go here
        initializeServices();
    }

    private void initializeServices() {
        // Initialize any background services
        Log.i(TAG, "All services initialized");

        // Log recommended model configuration
        DeviceClassDetector.ModelConfiguration config =
            DeviceClassDetector.getRecommendedModelConfig(deviceClass);
        Log.i(TAG, "Recommended model configuration: " + config.toString());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Cleanup resources
        TTSManager.shutdown();
        SystemPrivilegeManager.cleanup();
        if (performanceMonitor != null) {
            performanceMonitor.cleanup();
        }
        if (userFeedbackSystem != null) {
            userFeedbackSystem.cleanup();
        }
        if (performanceOptimizer != null) {
            performanceOptimizer.disableOptimizations();
        }
        Log.i(TAG, "Egyptian Agent Application terminated");
    }

    /**
     * Get the detected device class for the application
     */
    public DeviceClassDetector.DeviceClass getDeviceClass() {
        return deviceClass;
    }
}