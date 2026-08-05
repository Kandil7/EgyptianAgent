package com.egyptian.agent.service;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * System Controller
 * 
 * Handles system-level operations with root and non-root modes.
 * Provides abstraction for system commands like WiFi, Bluetooth, etc.
 * 
 * Features:
 * - Root command execution
 * - Non-root fallback
 * - System settings control
 * - Permission handling
 */
public class SystemController {
    private static final String TAG = "SystemController";
    
    private final Context context;
    private boolean hasRootAccess;
    
    public SystemController(Context context) {
        this.context = context.getApplicationContext();
        this.hasRootAccess = checkRootAccess();
    }
    
    /**
     * Check if device has root access.
     */
    public boolean checkRootAccess() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
            outputStream.writeBytes("echo test\n");
            outputStream.flush();
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            Log.d(TAG, "Root access not available");
            return false;
        }
    }
    
    /**
     * Toggle WiFi.
     */
    public boolean toggleWifi(boolean enable) {
        if (hasRootAccess) {
            return executeRootCommand(enable ? "svc wifi enable" : "svc wifi disable");
        } else {
            // Fallback to Settings intent
            return openWifiSettings();
        }
    }
    
    /**
     * Toggle Bluetooth.
     */
    public boolean toggleBluetooth(boolean enable) {
        if (hasRootAccess) {
            return executeRootCommand(enable ? "svc bluetooth enable" : "svc bluetooth disable");
        } else {
            // Fallback to Settings intent
            return openBluetoothSettings();
        }
    }
    
    /**
     * Toggle Flashlight.
     */
    public boolean toggleFlashlight(boolean enable) {
        if (hasRootAccess) {
            return executeRootCommand(enable ? "svc torch on" : "svc torch off");
        }
        return false;
    }
    
    /**
     * Set brightness.
     */
    public boolean setBrightness(int level) {
        if (hasRootAccess) {
            return executeRootCommand("settings put system screen_brightness " + level);
        } else {
            // Try system settings
            try {
                Settings.System.putInt(
                    context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    level
                );
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to set brightness", e);
                return false;
            }
        }
    }
    
    /**
     * Set volume.
     */
    public boolean setVolume(int streamType, int level) {
        if (hasRootAccess) {
            String streamName = getStreamName(streamType);
            return executeRootCommand("input keyevent KEYCODE_VOLUME_" + (level > 0 ? "UP" : "DOWN"));
        } else {
            // Use AudioManager
            android.media.AudioManager audioManager = 
                (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setStreamVolume(streamType, level, 0);
                return true;
            }
            return false;
        }
    }
    
    /**
     * Open WiFi settings.
     */
    public boolean openWifiSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to open WiFi settings", e);
            return false;
        }
    }
    
    /**
     * Open Bluetooth settings.
     */
    public boolean openBluetoothSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Bluetooth settings", e);
            return false;
        }
    }
    
    /**
     * Execute root command.
     */
    private boolean executeRootCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
            
            outputStream.writeBytes(command + "\n");
            outputStream.flush();
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            
            process.waitFor();
            
            boolean success = process.exitValue() == 0;
            Log.d(TAG, "Root command '" + command + "' executed: " + success);
            return success;
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Error executing root command", e);
            return false;
        }
    }
    
    /**
     * Get stream name for volume control.
     */
    private String getStreamName(int streamType) {
        switch (streamType) {
            case android.media.AudioManager.STREAM_MUSIC:
                return "music";
            case android.media.AudioManager.STREAM_RING:
                return "ring";
            case android.media.AudioManager.STREAM_NOTIFICATION:
                return "notification";
            case android.media.AudioManager.STREAM_ALARM:
                return "alarm";
            default:
                return "music";
        }
    }
    
    /**
     * Check if root access is available.
     */
    public boolean hasRootAccess() {
        return hasRootAccess;
    }
    
    /**
     * Refresh root access check.
     */
    public void refreshRootAccess() {
        hasRootAccess = checkRootAccess();
    }
}
