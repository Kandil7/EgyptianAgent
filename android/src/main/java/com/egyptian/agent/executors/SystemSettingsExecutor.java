package com.egyptian.agent.executors;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.nlp.IntentType;
import com.egyptian.agent.utils.CrashLogger;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;

/**
 * Executor for system settings commands like brightness, volume, etc.
 * Uses Root access (libsu) for direct control when available.
 */
public class SystemSettingsExecutor {
    private static final String TAG = "SystemSettingsExecutor";

    public static void handleCommand(Context context, IntentType intentType) {
        Log.i(TAG, "Handling system settings command: " + intentType);

        switch (intentType) {
            case BRIGHTNESS_UP:
                adjustBrightness(context, true);
                break;
            case BRIGHTNESS_DOWN:
                adjustBrightness(context, false);
                break;
            case VOLUME_UP:
                adjustVolume(context, true);
                break;
            case VOLUME_DOWN:
                adjustVolume(context, false);
                break;
            case AIRPLANE_MODE:
                toggleAirplaneMode(context);
                break;
            case WIFI_TOGGLE:
                toggleWiFi(context);
                break;
            case BLUETOOTH_TOGGLE:
                toggleBluetooth(context);
                break;
            case LOCATION_TOGGLE:
                toggleLocation(context);
                break;
            default:
                TTSManager.speak(context, "مافيش إعداد مطابق.");
        }
    }

    private static boolean isRootAvailable() {
        return ShellUtils.fastCmdResult("id");
    }

    private static void adjustBrightness(Context context, boolean increase) {
        try {
            if (isRootAvailable()) {
                // Use root to set brightness directly
                int current = 128; // Default fallback
                try {
                    String res = ShellUtils.fastCmd("settings get system screen_brightness");
                    current = Integer.parseInt(res);
                } catch (Exception e) {}

                int newValue = increase ? Math.min(current + 50, 255) : Math.max(current - 50, 0);
                ShellUtils.fastCmd("settings put system screen_brightness " + newValue);
                TTSManager.speak(context, increase ? "عليت الإضاءة" : "وطيت الإضاءة");
                return;
            }

            // Check if we have system-level permissions
            if (Settings.System.canWrite(context)) {
                int currentBrightness = Settings.System.getInt(
                    context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS
                );

                int newBrightness;
                if (increase) {
                    newBrightness = Math.min(currentBrightness + 50, 255); // Max 255
                } else {
                    newBrightness = Math.max(currentBrightness - 50, 0); // Min 0
                }

                Settings.System.putInt(
                    context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    newBrightness
                );

                TTSManager.speak(context, increase ? "الشاشة خفت" : "الشاشة عتمت");
            } else {
                // Request permission to write system settings
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                
                TTSManager.speak(context, "التطبيق محتاج إذن تعديل إعدادات النظام");
            }
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error adjusting brightness", e);
            TTSManager.speak(context, "مقدرش أعدل إعدادات السطوع");
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for brightness adjustment", e);
            TTSManager.speak(context, "مافيش إذن لتعديل سطوع الشاشة");
        }
    }

    private static void adjustVolume(Context context, boolean increase) {
        android.media.AudioManager audioManager = 
            (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (audioManager != null) {
            int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
            
            int newVolume;
            if (increase) {
                newVolume = Math.min(currentVolume + 1, maxVolume);
            } else {
                newVolume = Math.max(currentVolume - 1, 0);
            }

            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, newVolume, 0);
            
            TTSManager.speak(context, increase ? "الصوت ارتفع" : "الصوت نقص");
        }
    }

    private static void toggleAirplaneMode(Context context) {
        if (isRootAvailable()) {
            // Toggle airplane mode using root
            // 1 = on, 0 = off. We need to check current state.
            try {
                String current = ShellUtils.fastCmd("settings get global airplane_mode_on");
                boolean isOn = "1".equals(current);
                String newState = isOn ? "0" : "1";

                ShellUtils.fastCmd("settings put global airplane_mode_on " + newState);
                ShellUtils.fastCmd("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state " + (isOn ? "false" : "true"));

                TTSManager.speak(context, isOn ? "قفلت وضع الطيران" : "شغلت وضع الطيران");
            } catch (Exception e) {
                Log.e(TAG, "Error checking airplane mode", e);
                TTSManager.speak(context, "مش عارف أحدد وضع الطيران");
            }
        } else {
            TTSManager.speak(context, "الوضع الطيران مظبط من الإعدادات عشان ماعنديش روت");
            Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    private static void toggleWiFi(Context context) {
        if (isRootAvailable()) {
            // Try enabling/disabling via svc
            android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            boolean isEnabled = wifiManager.isWifiEnabled();

            String cmd = isEnabled ? "disable" : "enable";
            ShellUtils.fastCmd("svc wifi " + cmd);
            TTSManager.speak(context, isEnabled ? "قفلت الواي فاي" : "شغلت الواي فاي");
        } else {
            // Settings.Panel.ACTION_WIFI requires API 29+ and is not consistently supported
            // Use standard WiFi settings instead
            TTSManager.speak(context, "الواي فاي مظبط من الإعدادات");
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    private static void toggleBluetooth(Context context) {
        if (isRootAvailable()) {
            ShellUtils.fastCmd("svc bluetooth toggle"); // Works on many roms
            TTSManager.speak(context, "غيرت حالة البلوتوث");
        } else {
            // Use standard Bluetooth settings (Settings.Panel requires API 30+)
            fallbackToBluetoothSettings(context);
        }
    }

    private static void fallbackToBluetoothSettings(Context context) {
        TTSManager.speak(context, "البلوتوث مظبط من الإعدادات");
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void toggleLocation(Context context) {
        if (isRootAvailable()) {
            // settings put secure location_mode 3 (high accuracy) or 0 (off)
            try {
                String current = ShellUtils.fastCmd("settings get secure location_mode");
                boolean isOn = !"0".equals(current);
                String newState = isOn ? "0" : "3";
                ShellUtils.fastCmd("settings put secure location_mode " + newState);
                TTSManager.speak(context, isOn ? "قفلت الموقع" : "شغلت الموقع");
            } catch (Exception e) {
                Log.e(TAG, "Error toggling location", e);
            }
        } else {
            // Use standard location settings instead of Settings.Panel
            fallbackToLocationSettings(context);
        }
    }

    private static void fallbackToLocationSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
