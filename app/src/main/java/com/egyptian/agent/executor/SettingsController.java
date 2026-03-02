package com.egyptian.agent.executor;

import android.content.Context;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.nlu.IntentResult;
import com.egyptian.agent.service.SystemController;

/**
 * Settings Controller
 * 
 * Handles system settings commands:
 * - WiFi toggle
 * - Bluetooth toggle
 * - Flashlight toggle
 * - Volume control
 * - Brightness control
 */
public class SettingsController {
    private static final String TAG = "SettingsController";
    
    private final Context context;
    private final SystemController systemController;
    
    public SettingsController(Context context) {
        this.context = context.getApplicationContext();
        this.systemController = new SystemController(context);
    }
    
    /**
     * Toggle WiFi.
     */
    public ExecutorResult toggleWifi(IntentResult intent) {
        try {
            // Determine if enabling or disabling based on command
            String originalText = intent.getOriginalText().toLowerCase();
            boolean enable = !originalText.contains("قفل") && 
                            !originalText.contains("اقفل") && 
                            !originalText.contains("اطفئ") &&
                            !originalText.contains("اوقف");
            
            boolean success = systemController.toggleWifi(enable);
            
            if (success) {
                String message = enable ? "الواي فاي اتشغل" : "الواي فاي اتقفل";
                TTSManager.speak(context, message);
                Log.i(TAG, "WiFi toggled: " + (enable ? "ON" : "OFF"));
                return ExecutorResult.success(message, "WIFI:" + (enable ? "ON" : "OFF"));
            } else {
                TTSManager.speak(context, "مش قادر اتحكم في الواي فاي");
                return ExecutorResult.error("Failed to toggle WiFi");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling WiFi", e);
            TTSManager.speak(context, "حصل خطأ في الواي فاي");
            return ExecutorResult.error("Error: " + e.getMessage());
        }
    }
    
    /**
     * Toggle Bluetooth.
     */
    public ExecutorResult toggleBluetooth(IntentResult intent) {
        try {
            String originalText = intent.getOriginalText().toLowerCase();
            boolean enable = !originalText.contains("قفل") && 
                            !originalText.contains("اقفل") && 
                            !originalText.contains("اطفئ");
            
            boolean success = systemController.toggleBluetooth(enable);
            
            if (success) {
                String message = enable ? "البلوتوث اتشغل" : "البلوتوث اتقفل";
                TTSManager.speak(context, message);
                Log.i(TAG, "Bluetooth toggled: " + (enable ? "ON" : "OFF"));
                return ExecutorResult.success(message, "BLUETOOTH:" + (enable ? "ON" : "OFF"));
            } else {
                TTSManager.speak(context, "مش قادر اتحكم في البلوتوث");
                return ExecutorResult.error("Failed to toggle Bluetooth");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling Bluetooth", e);
            TTSManager.speak(context, "حصل خطأ في البلوتوث");
            return ExecutorResult.error("Error: " + e.getMessage());
        }
    }
    
    /**
     * Toggle Flashlight.
     */
    public ExecutorResult toggleFlashlight(IntentResult intent) {
        try {
            String originalText = intent.getOriginalText().toLowerCase();
            boolean enable = !originalText.contains("قفل") && 
                            !originalText.contains("اقفل") && 
                            !originalText.contains("اطفئ");
            
            boolean success = systemController.toggleFlashlight(enable);
            
            if (success) {
                String message = enable ? "الفلاش اتشغل" : "الفلاش اتقفل";
                TTSManager.speak(context, message);
                Log.i(TAG, "Flashlight toggled: " + (enable ? "ON" : "OFF"));
                return ExecutorResult.success(message, "FLASHLIGHT:" + (enable ? "ON" : "OFF"));
            } else {
                TTSManager.speak(context, "مش قادر اتحكم في الفلاش");
                return ExecutorResult.error("Failed to toggle flashlight");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling flashlight", e);
            TTSManager.speak(context, "حصل خطأ في الفلاش");
            return ExecutorResult.error("Error: " + e.getMessage());
        }
    }
    
    /**
     * Cancel current operation.
     */
    public void cancel() {
        Log.d(TAG, "Cancelling settings operation");
    }
}
