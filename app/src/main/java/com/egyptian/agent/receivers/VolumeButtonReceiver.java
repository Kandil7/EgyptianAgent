package com.egyptian.agent.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.executors.EmergencyHandler;

/**
 * Volume Button Receiver
 * Receives volume button events for emergency activation
 */
public class VolumeButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "VolumeButtonReceiver";
    
    private static int volumePressCount = 0;
    private static long lastPressTime = 0;
    private static final int MAX_INTERVAL = 1000; // 1 second
    private static final int TRIPLE_PRESS_COUNT = 3;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if ("android.media.VOLUME_CHANGED_ACTION".equals(action)) {
            // Get the current volume stream type
            int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
            
            // Only respond to volume button presses, not media volume changes
            if (streamType == AudioManager.STREAM_MUSIC) {
                long currentTime = System.currentTimeMillis();
                
                // Reset counter if too much time has passed
                if (currentTime - lastPressTime > MAX_INTERVAL) {
                    volumePressCount = 0;
                }
                
                volumePressCount++;
                lastPressTime = currentTime;
                
                Log.d(TAG, "Volume button pressed, count: " + volumePressCount);
                
                // Check for triple press
                if (volumePressCount >= TRIPLE_PRESS_COUNT) {
                    Log.i(TAG, "Triple volume button press detected - triggering emergency");
                    
                    // Trigger emergency response
                    EmergencyHandler.trigger(context);
                    
                    // Reset counter
                    volumePressCount = 0;
                    lastPressTime = 0;
                }
            }
        }
    }
}