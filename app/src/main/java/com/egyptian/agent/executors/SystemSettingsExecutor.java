package com.egyptian.agent.executors;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.CrashLogger;

/**
 * Executor for system settings commands like brightness, volume, etc.
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

    private static void adjustBrightness(Context context, boolean increase) {
        try {
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
        // Airplane mode requires system-level permissions and is restricted
        TTSManager.speak(context, "الوضع الطيران مظبط من الإعدادات");
    }

    private static void toggleWiFi(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, use the new API
            Intent intent = new Intent(Settings.Panel.ACTION_WIFI);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            // For older versions, we need system-level permissions
            TTSManager.speak(context, "الواي فاي مظبط من الإعدادات");
        }
    }

    private static void toggleBluetooth(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, use the new API
            Intent intent = new Intent(Settings.Panel.ACTION_BLUETOOTH);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            // For older versions, we need special permissions
            TTSManager.speak(context, "البلوتوث مظبط من الإعدادات");
        }
    }

    private static void toggleLocation(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+, use the new API
            Intent intent = new Intent(Settings.Panel.ACTION_LOCATION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            // For older versions, open location settings
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}