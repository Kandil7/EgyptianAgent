package com.egyptian.agent.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * Device Class Detector
 * Detects the device class based on hardware specifications
 * Helps determine which models to load based on device capabilities
 */
public class DeviceClassDetector {
    private static final String TAG = "DeviceClassDetector";
    
    /**
     * Enum representing different device classes
     */
    public enum DeviceClass {
        LOW_END,
        MID_RANGE,
        HIGH_END,
        FLAGSHIP
    }
    
    /**
     * Model configuration for different device classes
     */
    public static class ModelConfiguration {
        private final String modelSize;
        private final int maxModelSizeMB;
        private final int maxThreads;
        private final int memoryBudgetMB;
        
        public ModelConfiguration(String modelSize, int maxModelSizeMB, int maxThreads, int memoryBudgetMB) {
            this.modelSize = modelSize;
            this.maxModelSizeMB = maxModelSizeMB;
            this.maxThreads = maxThreads;
            this.memoryBudgetMB = memoryBudgetMB;
        }
        
        public String getModelSize() { return modelSize; }
        public int getMaxModelSizeMB() { return maxModelSizeMB; }
        public int getMaxThreads() { return maxThreads; }
        public int getMemoryBudgetMB() { return memoryBudgetMB; }
        
        @Override
        public String toString() {
            return "ModelConfiguration{" +
                    "modelSize='" + modelSize + '\'' +
                    ", maxModelSizeMB=" + maxModelSizeMB +
                    ", maxThreads=" + maxThreads +
                    ", memoryBudgetMB=" + memoryBudgetMB +
                    '}';
        }
    }
    
    /**
     * Detects the device class based on hardware specifications
     * @param context Context for accessing system information
     * @return The detected device class
     */
    public static DeviceClass detectDevice(Context context) {
        // Get device information
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String model = Build.MODEL.toLowerCase();
        int sdkVersion = Build.VERSION.SDK_INT;

        // Get memory information
        long totalMemory = getAvailableMemory(context);
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        Log.d(TAG, String.format(
            "Device Info: %s %s, SDK: %d, RAM: %d MB, Cores: %d",
            manufacturer, model, sdkVersion, totalMemory / (1024 * 1024), availableProcessors
        ));

        // Check for specific target device (Honor X6c)
        if (isHonorX6c(manufacturer, model)) {
            Log.i(TAG, "Honor X6c device detected");
            return DeviceClass.MID_RANGE; // Honor X6c with 6GB RAM is mid-range
        }

        // Determine device class based on specifications
        if (totalMemory < 3 * 1024 * 1024 * 1024L) { // Less than 3GB
            return DeviceClass.LOW_END;
        } else if (totalMemory < 6 * 1024 * 1024 * 1024L) { // Less than 6GB
            if (availableProcessors >= 6) {
                return DeviceClass.MID_RANGE;
            } else {
                return DeviceClass.LOW_END;
            }
        } else if (totalMemory < 8 * 1024 * 1024 * 1024L) { // Less than 8GB
            if (availableProcessors >= 8) {
                return DeviceClass.HIGH_END;
            } else {
                return DeviceClass.MID_RANGE;
            }
        } else { // 8GB or more
            return DeviceClass.FLAGSHIP;
        }
    }
    
    /**
     * Gets the recommended model configuration for the device class
     * @param deviceClass The device class
     * @return The recommended model configuration
     */
    public static ModelConfiguration getRecommendedModelConfig(DeviceClass deviceClass) {
        switch (deviceClass) {
            case LOW_END:
                return new ModelConfiguration("small", 500, 2, 1024); // Small model, 2 threads, 1GB budget
            case MID_RANGE:
                return new ModelConfiguration("medium", 1500, 4, 2048); // Medium model, 4 threads, 2GB budget
            case HIGH_END:
                return new ModelConfiguration("large", 3000, 6, 3072); // Large model, 6 threads, 3GB budget
            case FLAGSHIP:
                return new ModelConfiguration("xlarge", 5000, 8, 4096); // XL model, 8 threads, 4GB budget
            default:
                return new ModelConfiguration("small", 500, 2, 1024); // Default to small
        }
    }
    
    /**
     * Gets available memory on the device
     * @param context Context for accessing system services
     * @return Available memory in bytes
     */
    private static long getAvailableMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.totalMem; // Total physical memory
    }
    
    /**
     * Checks if the device is Honor X6c
     * @param manufacturer Device manufacturer
     * @param model Device model
     * @return true if device is Honor X6c, false otherwise
     */
    public static boolean isHonorX6c(String manufacturer, String model) {
        return (manufacturer.contains("honor") || manufacturer.contains("huawei")) &&
               (model.contains("x6c") || model.contains("x6 c"));
    }
}