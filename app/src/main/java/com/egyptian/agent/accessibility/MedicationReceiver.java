package com.egyptian.agent.accessibility;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.VibrationManager;

import java.util.Date;

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
        // For now, we'll implement the actual scheduling
        scheduleFollowUpReminderImpl(context, medicationName, dosage);
    }

    /**
     * Actually schedules a follow-up reminder after 5 minutes
     */
    private void scheduleFollowUpReminderImpl(Context context, String medicationName, String dosage) {
        // Create an intent for the follow-up reminder
        Intent followUpIntent = new Intent(context, MedicationReceiver.class);
        followUpIntent.setAction("com.egyptian.agent.MEDICATION_REMINDER_FOLLOWUP");
        followUpIntent.putExtra("medication_name", medicationName);
        followUpIntent.putExtra("dosage", dosage);

        // Create a pending intent
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationName.hashCode(), // Use medication name hash as request code
            followUpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Get alarm manager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Schedule the alarm for 5 minutes from now
        long triggerTime = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes in milliseconds

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        Log.i(TAG, "Follow-up reminder scheduled for: " + medicationName + " at " + new Date(triggerTime));
    }
}