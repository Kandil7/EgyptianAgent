package com.egyptian.agent.executor;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.nlu.IntentResult;

import java.util.List;

/**
 * Apps Controller
 * 
 * Handles app-related commands:
 * - Open apps
 * - Close apps
 * - App switching
 */
public class AppsController {
    private static final String TAG = "AppsController";
    
    private final Context context;
    
    // Common app name mappings
    private static final String[][] APP_MAPPINGS = {
        {"واتساب", "com.whatsapp"},
        {"فيسبوك", "com.facebook.katana"},
        {"يوتيوب", "com.google.android.youtube"},
        {"جوجل", "com.google.android.googlequicksearchbox"},
        {"كروم", "com.android.chrome"},
        {"مessages", "com.google.android.apps.messaging"},
        {"تليفون", "com.google.android.dialer"},
        {"كاميرا", "com.android.camera"},
        {"معرض", "com.google.android.apps.photos"},
        {"سجل المكالمات", "com.google.android.dialer"}
    };
    
    public AppsController(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Open app by name.
     */
    public ExecutorResult openApp(IntentResult intent) {
        String appName = intent.getEntity("app", "");
        
        if (appName.isEmpty()) {
            TTSManager.speak(context, "عايز تفتح تطبيق معين؟");
            return ExecutorResult.error("No app specified");
        }
        
        // Find app package name
        String packageName = findAppPackage(appName);
        
        if (packageName == null) {
            TTSManager.speak(context, "مش لاقي تطبيق باسم " + appName);
            return ExecutorResult.error("App not found: " + appName);
        }
        
        try {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                
                TTSManager.speak(context, "فاتح " + appName);
                Log.i(TAG, "Opened app: " + appName + " (" + packageName + ")");
                
                return ExecutorResult.success("Opening " + appName, "OPEN_APP:" + packageName);
            } else {
                TTSManager.speak(context, "التطبيق مش مثبت");
                return ExecutorResult.error("App not installed: " + packageName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening app", e);
            TTSManager.speak(context, "مش قادر افتح التطبيق");
            return ExecutorResult.error("Failed to open app: " + e.getMessage());
        }
    }
    
    /**
     * Find app package name by Egyptian name.
     */
    private String findAppPackage(String appName) {
        if (appName == null || appName.isEmpty()) {
            return null;
        }
        
        // Check mappings first
        for (String[] mapping : APP_MAPPINGS) {
            if (appName.contains(mapping[0])) {
                return mapping[1];
            }
        }
        
        // Search installed apps
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(
            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            0
        );
        
        for (ResolveInfo app : apps) {
            String label = app.loadLabel(pm).toString().toLowerCase();
            if (label.contains(appName.toLowerCase())) {
                return app.activityInfo.packageName;
            }
        }
        
        return null;
    }
    
    /**
     * Cancel current operation.
     */
    public void cancel() {
        Log.d(TAG, "Cancelling apps operation");
    }
}
