package com.egyptian.agent.accessibility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.VibrationManager;

/**
 * Broadcast receiver for medication reminders
 */
public class MedicationReceiver extends BroadcastReceiver {
    private static final String TAG = "MedicationReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Medication reminder received");
        
        // Get medication details from intent
        String medicationName = intent.getStringExtra("medication_name");
        String instructions = intent.getStringExtra("instructions");
        
        // Speak the reminder
        String reminderText = "تذكير بتناول الدواء: " + medicationName;
        if (instructions != null && !instructions.isEmpty()) {
            reminderText += ". " + instructions;
        }
        
        TTSManager.speak(context, reminderText);
        
        // Vibrate to get attention
        VibrationManager.vibrateLong(context);
        
        Log.i(TAG, "Medication reminder processed for: " + medicationName);
    }
}