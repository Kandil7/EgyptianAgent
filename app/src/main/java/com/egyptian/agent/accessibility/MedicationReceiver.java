package com.egyptian.agent.accessibility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.VibrationManager;

/**
 * BroadcastReceiver to handle medication reminder triggers
 */
public class MedicationReceiver extends BroadcastReceiver {
    private static final String TAG = "MedicationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("com.egyptian.agent.MEDICATION_REMINDER")) {
            String medicationName = intent.getStringExtra("medication_name");
            String dosage = intent.getStringExtra("dosage");

            Log.i(TAG, "Medication reminder triggered: " + medicationName + " - " + dosage);

            // Speak the medication reminder
            if (medicationName != null && dosage != null) {
                TTSManager.speak(context, "موعد تناول " + medicationName + " - الجرعة: " + dosage);
            } else if (medicationName != null) {
                TTSManager.speak(context, "موعد تناول " + medicationName);
            } else {
                TTSManager.speak(context, "موعد تناول الدواء");
            }

            // Vibrate to get attention
            VibrationManager.vibrateLong(context);

            // If senior mode is enabled, repeat the reminder after 5 minutes
            if (SeniorMode.isEnabled()) {
                scheduleFollowUpReminder(context, medicationName, dosage);
            }
        }
    }

    /**
     * Schedules a follow-up reminder after 5 minutes if the user didn't acknowledge
     */
    private void scheduleFollowUpReminder(Context context, String medicationName, String dosage) {
        // In a real implementation, this would schedule another alarm for 5 minutes later
        // For now, we'll just log that a follow-up would be scheduled
        Log.i(TAG, "Follow-up reminder would be scheduled for: " + medicationName);
    }
}