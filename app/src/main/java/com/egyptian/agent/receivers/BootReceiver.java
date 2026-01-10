package com.egyptian.agent.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.egyptian.agent.core.VoiceService;
import com.egyptian.agent.utils.CrashLogger;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "BootReceiver received: " + intent.getAction());

        try {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction()) ||
                "com.htc.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {

                // Start the voice service after boot
                startVoiceService(context);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in BootReceiver", e);
            CrashLogger.logError(context, e);
        }
    }

    private void startVoiceService(Context context) {
        try {
            Intent serviceIntent = new Intent(context, VoiceService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.i(TAG, "VoiceService started after boot");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start VoiceService after boot", e);
            CrashLogger.logError(context, e);
        }
    }
}