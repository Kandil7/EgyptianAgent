package com.egyptian.agent.receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.CrashLogger;

public class AdminReceiver extends DeviceAdminReceiver {

    private static final String TAG = "AdminReceiver";

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Log.i(TAG, "Device admin enabled");
        TTSManager.speak(context, "تم تفعيل صلاحيات الجهاز");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Log.i(TAG, "Device admin disabled");
        TTSManager.speak(context, "تم إلغاء تفعيل صلاحيات الجهاز");
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {
        super.onPasswordChanged(context, intent);
        Log.i(TAG, "Password changed");
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent) {
        super.onPasswordFailed(context, intent);
        Log.i(TAG, "Password failed");
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent) {
        super.onPasswordSucceeded(context, intent);
        Log.i(TAG, "Password succeeded");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d(TAG, "AdminReceiver received intent: " + intent.getAction());

        try {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "android.app.action.DEVICE_ADMIN_ENABLED":
                        Log.i(TAG, "Device admin enabled action received");
                        break;
                    case "android.app.action.DEVICE_ADMIN_DISABLED":
                        Log.i(TAG, "Device admin disabled action received");
                        break;
                    case "android.app.action.DEVICE_OWNER_CHANGED":
                        Log.i(TAG, "Device owner changed action received");
                        break;
                    default:
                        Log.d(TAG, "Unknown action received: " + action);
                        break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling admin receiver", e);
            CrashLogger.logError(context, e);
        }
    }
}