package com.egyptian.agent.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;

/**
 * Receiver for alarm events
 * Handles when an alarm is triggered
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received");

        try {
            // Initialize TTS if not already initialized
            if (!TTSManager.isInitialized()) {
                TTSManager.initialize(context);
            }

            // Speak the alarm notification
            TTSManager.speak(context, "المنبه اشتغل");
            
            // Additional actions can be added here if needed
            // For example, vibrating the device, showing a notification, etc.
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling alarm", e);
        }
    }
}