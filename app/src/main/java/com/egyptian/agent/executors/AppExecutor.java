package com.egyptian.agent.executors;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;

import java.util.List;

/**
 * Executor for app management commands (open, close)
 */
public class AppExecutor {
    private static final String TAG = "AppExecutor";

    public static void handleOpenAppCommand(Context context, String appName) {
        Log.i(TAG, "Handling open app command for: " + appName);

        try {
            String packageName = findPackageName(context, appName);
            
            if (packageName != null && !packageName.isEmpty()) {
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(launchIntent);
                    
                    TTSManager.speak(context, "بفتح " + appName);
                } else {
                    TTSManager.speak(context, "مقدرش أفتح " + appName);
                }
            } else {
                TTSManager.speak(context, "ملاقيش تطبيق اسمه " + appName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening app: " + appName, e);
            TTSManager.speak(context, "مقدرش أفتح التطبيق");
        }
    }

    public static void handleCloseAppCommand(Context context, String appName) {
        Log.i(TAG, "Handling close app command for: " + appName);

        // Android doesn't allow apps to directly force stop other apps for security reasons
        // We can only suggest to the user to manage the app
        TTSManager.speak(context, "ممكن تستخدم الإعدادات علشان تغلق " + appName);
    }

    /**
     * Finds the package name for an app by its name
     * @param context The application context
     * @param appName The name of the app to find
     * @return The package name, or null if not found
     */
    private static String findPackageName(Context context, String appName) {
        PackageManager pm = context.getPackageManager();
        
        // Get all installed packages
        List<android.content.pm.ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        
        // Look for the app by name (case-insensitive)
        for (android.content.pm.ApplicationInfo packageInfo : packages) {
            String appNameFromPackage = pm.getApplicationLabel(packageInfo).toString();
            
            if (appNameFromPackage.toLowerCase().contains(appName.toLowerCase()) ||
                appName.toLowerCase().contains(appNameFromPackage.toLowerCase())) {
                return packageInfo.packageName;
            }
        }
        
        // If not found by name, try common app names in Egyptian dialect
        return mapCommonAppNames(appName);
    }

    /**
     * Maps common app names in Egyptian dialect to their package names
     * @param appName The app name in Egyptian dialect
     * @return The package name, or null if not known
     */
    private static String mapCommonAppNames(String appName) {
        // This is a simplified mapping - in a real implementation, this would be more comprehensive
        switch (appName.toLowerCase()) {
            case "وتس أب":
            case "واتساب":
            case "whatsapp":
                return "com.whatsapp";
            case "فيس بوك":
            case "فيسبوك":
            case "facebook":
                return "com.facebook.katana";
            case "انستجرام":
            case "انستا":
            case "instagram":
                return "com.instagram.android";
            case "تيك توك":
            case "tiktok":
                return "com.zhiliaoapp.musically";
            case "يوتيوب":
            case "youtube":
                return "com.google.android.youtube";
            case "مessaging":
            case "messages":
            case "الرسائل":
                return "com.google.android.apps.messaging"; // Default messaging app
            case "camera":
            case "الكاميرا":
            case "كمر":
                return "com.google.android.GoogleCamera"; // Google Camera as example
            case "gallery":
            case "الاستديو":
            case "الصور":
                return "com.google.android.apps.photos"; // Google Photos as example
            case "maps":
            case "الخرائط":
            case "جوجل مابس":
                return "com.google.android.apps.maps";
            case "play store":
            case "بلاي ستور":
            case "البلاي":
                return "com.android.vending";
            default:
                return null;
        }
    }
}