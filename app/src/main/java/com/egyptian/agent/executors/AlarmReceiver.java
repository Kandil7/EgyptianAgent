package com.egyptian.agent.executors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Alarm received");

        // Speak the alarm notification
        TTSManager.speak(context, "المنبه! الوقت دلوقتي");
        
        // You could also trigger other actions here, like:
        // - Playing an alarm sound
        // - Vibrating the device
        // - Showing a notification
    }
}