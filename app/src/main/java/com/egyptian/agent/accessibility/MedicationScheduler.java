package com.egyptian.agent.accessibility;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.VibrationManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Schedules medication reminders for senior users
 */
public class MedicationScheduler {
    private static final String TAG = "MedicationScheduler";
    private static final String MEDICATION_REMINDER_ACTION = "com.egyptian.agent.MEDICATION_REMINDER";

    private static List<MedicationReminder> medicationReminders = new ArrayList<>();
    private static Context context;

    public static void initialize(Context appContext) {
        context = appContext.getApplicationContext();
    }

    /**
     * Adds a medication reminder to the scheduler
     */
    public static void addMedicationReminder(MedicationReminder reminder) {
        medicationReminders.add(reminder);

        // Schedule the reminder using AlarmManager
        scheduleMedicationReminder(reminder);
        Log.i(TAG, "Medication reminder added: " + reminder.getMedicationName());
    }

    /**
     * Schedules a medication reminder using AlarmManager
     */
    private static void scheduleMedicationReminder(MedicationReminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Create intent for the medication reminder
        Intent intent = new Intent(context, MedicationReceiver.class);
        intent.setAction(MEDICATION_REMINDER_ACTION);
        intent.putExtra("medication_name", reminder.getMedicationName());
        intent.putExtra("dosage", reminder.getDosage());

        // Create unique request code for this reminder
        int requestCode = reminder.getMedicationName().hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set the alarm
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.getTimeInMillis(),
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reminder.getTimeInMillis(),
                pendingIntent
            );
        }

        Log.i(TAG, "Medication reminder scheduled for: " + new java.util.Date(reminder.getTimeInMillis()));
    }

    /**
     * Removes a medication reminder from the scheduler
     */
    public static void removeMedicationReminder(MedicationReminder reminder) {
        medicationReminders.remove(reminder);

        // Cancel the alarm for this reminder
        cancelMedicationReminder(reminder);
        Log.i(TAG, "Medication reminder removed: " + reminder.getMedicationName());
    }

    /**
     * Cancels a medication reminder
     */
    private static void cancelMedicationReminder(MedicationReminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Create the same intent that was used to schedule the reminder
        Intent intent = new Intent(context, MedicationReceiver.class);
        intent.setAction(MEDICATION_REMINDER_ACTION);
        intent.putExtra("medication_name", reminder.getMedicationName());
        intent.putExtra("dosage", reminder.getDosage());

        // Create the same request code
        int requestCode = reminder.getMedicationName().hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Cancel the alarm
        alarmManager.cancel(pendingIntent);

        Log.i(TAG, "Medication reminder cancelled: " + reminder.getMedicationName());
    }

    /**
     * Gets all scheduled medication reminders
     */
    public static List<MedicationReminder> getMedicationReminders() {
        return new ArrayList<>(medicationReminders);
    }

    /**
     * Clears all medication reminders
     */
    public static void clearAllReminders() {
        for (MedicationReminder reminder : medicationReminders) {
            cancelMedicationReminder(reminder);
        }
        medicationReminders.clear();
        Log.i(TAG, "All medication reminders cleared");
    }
}