package com.egyptian.agent.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.egyptian.agent.core.VoiceService;

public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction()) ||
            "com.htc.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            
            Log.i(TAG, "Boot completed, starting voice service");
            
            // Start the voice service
            Intent serviceIntent = new Intent(context, VoiceService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}