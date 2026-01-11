package com.egyptian.agent.core;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Device class detection system for optimizing AI model selection
 * Based on available RAM, CPU capabilities, and hardware features
 */
public class DeviceClassDetector {
    private static final String TAG = "DeviceClassDetector";
    
    public enum DeviceClass {
        LOW,    // 2-3GB RAM, basic processors (Unisoc, older Helio)
        MID,    // 4-6GB RAM, mid-range processors (Helio G85+, Snapdragon 4xx/6xx)
        HIGH,   // 8GB+ RAM, high-end processors (Snapdragon 7xx/8xx, Dimensity 8xx+)
        ELITE   // 12GB+ RAM, flagship processors (Snapdragon 8 Gen series)
    }
    
    /**
     * Detects the device class based on hardware capabilities
     */
    public static DeviceClass detectDevice(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        
        // Get memory info
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        
        long totalRam = getTotalRAM();
        long availableRam = memInfo.totalMem;
        
        Log.d(TAG, String.format("Device RAM - Total: %d MB, Available: %d MB", 
                totalRam / (1024 * 1024), availableRam / (1024 * 1024)));
        
        // Classify based on RAM
        DeviceClass deviceClass = classifyByRAM(totalRam);
        
        // Further refine classification based on CPU and other factors
        deviceClass = refineClassification(context, deviceClass, totalRam);
        
        Log.i(TAG, "Detected device class: " + deviceClass.name());
        return deviceClass;
    }
    
    /**
     * Classifies device based on total RAM
     */
    private static DeviceClass classifyByRAM(long totalRam) {
        if (totalRam < 3_000_000_000L) { // Less than 3GB
            return DeviceClass.LOW;
        } else if (totalRam < 6_000_000_000L) { // Less than 6GB
            return DeviceClass.MID;
        } else if (totalRam < 10_000_000_000L) { // Less than 10GB
            return DeviceClass.HIGH;
        } else {
            return DeviceClass.ELITE;
        }
    }
    
    /**
     * Refines the classification based on additional factors
     */
    private static DeviceClass refineClassification(Context context, DeviceClass initialClass, long totalRam) {
        // Check if device is the target Honor X6c
        if (isHonorX6c()) {
            Log.d(TAG, "Detected Honor X6c - setting to MID class for optimal performance");
            return DeviceClass.MID; // Honor X6c has 6GB RAM, classified as MID
        }
        
        // Check for 64-bit support
        boolean is64Bit = Build.SUPPORTED_64_BIT_ABIS.length > 0;
        
        // Check for NPU/NNAPI support
        boolean hasNNAPI = hasNNAPISupport(context);
        
        // Adjust classification based on additional factors
        DeviceClass refinedClass = initialClass;
        
        switch (initialClass) {
            case LOW:
                // On LOW class, if device has NNAPI support, it might be capable of more
                if (hasNNAPI) {
                    refinedClass = DeviceClass.MID;
                }
                break;
                
            case MID:
                // On MID class, if device has strong NNAPI support and 64-bit, could be HIGH
                if (hasNNAPI && is64Bit && totalRam >= 6_000_000_000L) {
                    refinedClass = DeviceClass.HIGH;
                }
                break;
                
            case HIGH:
                // On HIGH class, if device lacks NNAPI support, might be better as MID
                if (!hasNNAPI && totalRam < 8_000_000_000L) {
                    refinedClass = DeviceClass.MID;
                }
                break;
                
            case ELITE:
                // Elite class remains elite regardless of other factors
                break;
        }
        
        return refinedClass;
    }
    
    /**
     * Checks if the device is Honor X6c
     */
    private static boolean isHonorX6c() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String model = Build.MODEL.toLowerCase();
        
        return (manufacturer.contains("honor") || manufacturer.contains("huawei")) && 
               (model.contains("x6c") || model.contains("x6 c"));
    }
    
    /**
     * Checks if the device supports NNAPI (Neural Networks API)
     */
    private static boolean hasNNAPISupport(Context context) {
        // Check if the device has the required NNAPI permissions and features
        PackageManager pm = context.getPackageManager();
        
        // Check for NNAPI feature support
        boolean hasFeature = pm.hasSystemFeature(PackageManager.FEATURE_NFC) || // Placeholder - actual check varies
                             pm.hasSystemFeature("android.hardware.neural_networks");
        
        // For Android 8.1+, NNAPI is available
        boolean isSupportedVersion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1;
        
        // Additional check: try to access NNAPI classes
        try {
            Class.forName("android.hardware.neuralnetworks.nnapi.NnApi");
            return true;
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "NNAPI class not found on this device");
        }
        
        return isSupportedVersion && hasFeature;
    }
    
    /**
     * Gets the total RAM of the device
     */
    private static long getTotalRAM() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"));
            String line = reader.readLine();
            reader.close();
            
            if (line != null) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    long memTotalKB = Long.parseLong(parts[1]);
                    return memTotalKB * 1024; // Convert KB to bytes
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading total RAM", e);
        }
        
        // Fallback: estimate based on available memory info
        ActivityManager am = (ActivityManager) 
            android.app.ApplicationProvider.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memInfo);
        
        // This is an approximation - available memory is not total memory
        return memInfo.totalMem;
    }
    
    /**
     * Gets the recommended model configuration for the device class
     */
    public static ModelConfiguration getRecommendedModelConfig(DeviceClass deviceClass) {
        switch (deviceClass) {
            case LOW:
                return new ModelConfiguration(
                    "whisper-tiny",       // ASR model
                    null,                 // No LLM on low-end devices
                    "espeak",             // TTS engine
                    "grammar_only",       // Intent detection method
                    1000                  // Max inference time in ms
                );
                
            case MID:
                return new ModelConfiguration(
                    "whisper-base",       // ASR model
                    "gemma2:2b-q4",       // LLM model
                    "piper",              // TTS engine
                    "grammar_rules",      // Intent detection method
                    1800                  // Max inference time in ms
                );
                
            case HIGH:
                return new ModelConfiguration(
                    "whisper-small",      // ASR model
                    "llama3:3b-q5",       // LLM model
                    "coqui-tts",          // TTS engine
                    "full-llm",           // Intent detection method
                    2500                  // Max inference time in ms
                );
                
            case ELITE:
                return new ModelConfiguration(
                    "whisper-medium",     // ASR model
                    "mixtral:8x7b-q4",    // LLM model
                    "elevenlabs-local",   // TTS engine
                    "full-llm",           // Intent detection method
                    3500                  // Max inference time in ms
                );
                
            default:
                return new ModelConfiguration(
                    "whisper-tiny",       // ASR model
                    null,                 // No LLM on low-end devices
                    "espeak",             // TTS engine
                    "grammar_only",       // Intent detection method
                    1000                  // Max inference time in ms
                );
        }
    }
    
    /**
     * Configuration class for model recommendations
     */
    public static class ModelConfiguration {
        public final String asrModel;
        public final String llmModel;
        public final String ttsEngine;
        public final String intentMethod;
        public final int maxInferenceTimeMs;
        
        public ModelConfiguration(String asrModel, String llmModel, String ttsEngine, 
                                 String intentMethod, int maxInferenceTimeMs) {
            this.asrModel = asrModel;
            this.llmModel = llmModel;
            this.ttsEngine = ttsEngine;
            this.intentMethod = intentMethod;
            this.maxInferenceTimeMs = maxInferenceTimeMs;
        }
        
        @Override
        public String toString() {
            return String.format("ModelConfig{ASR=%s, LLM=%s, TTS=%s, Intent=%s, MaxTime=%dms}",
                    asrModel, llmModel, ttsEngine, intentMethod, maxInferenceTimeMs);
        }
    }
}