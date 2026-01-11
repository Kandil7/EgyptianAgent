package com.egyptian.agent.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.egyptian.agent.core.VoiceService;

/**
 * Boot Receiver
 * Receives boot completed broadcast and starts the voice service
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            "android.intent.action.QUICKBOOT_POWERON".equals(action) ||
            "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
            
            Log.i(TAG, "Boot completed received, starting voice service");
            
            try {
                // Start the voice service
                Intent serviceIntent = new Intent(context, VoiceService.class);
                serviceIntent.setAction("com.egyptian.agent.action.START_VOICE_SERVICE");
                
                // Use the appropriate start method based on Android version
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
                
                Log.i(TAG, "Voice service started after boot");
            } catch (Exception e) {
                Log.e(TAG, "Error starting voice service after boot", e);
            }
        }
    }
}