package com.egyptian.agent.accessibility;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.Calendar;

/**
 * Scheduler for medication reminders as specified in the SRD
 */
public class MedicationScheduler {
    private static final String TAG = "MedicationScheduler";
    private Context context;
    private AlarmManager alarmManager;
    
    public MedicationScheduler(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    
    public void scheduleReminders(java.util.List<MedicationReminder> reminders) {
        for (MedicationReminder reminder : reminders) {
            scheduleReminder(reminder);
        }
    }
    
    public void scheduleReminder(MedicationReminder reminder) {
        // Calculate the time for the reminder today
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, reminder.getTime().getHour());
        calendar.set(Calendar.MINUTE, reminder.getTime().getMinute());
        calendar.set(Calendar.SECOND, 0);
        
        // If the time has already passed today, schedule for tomorrow
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        // Create intent for the reminder
        Intent intent = new Intent(context, MedicationReceiver.class);
        intent.putExtra("medication_name", reminder.getMedicationName());
        intent.putExtra("instructions", reminder.getInstructions());
        
        // Create a unique request code for each reminder
        int requestCode = reminder.hashCode();
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Schedule the alarm
        if (reminder.isRecurring()) {
            // For recurring reminders, use setRepeating
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, // Repeat daily
                pendingIntent
            );
        } else {
            // For one-time reminders, use setExactAndAllowWhileIdle
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            }
        }
        
        Log.i(TAG, "Scheduled medication reminder for: " + reminder.getMedicationName() + 
              " at " + reminder.getTime());
    }
    
    public void cancelReminder(MedicationReminder reminder) {
        Intent intent = new Intent(context, MedicationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(pendingIntent);
        Log.i(TAG, "Cancelled medication reminder for: " + reminder.getMedicationName());
    }
    
    public void rescheduleReminders(java.util.List<MedicationReminder> reminders) {
        // Cancel all existing reminders
        for (MedicationReminder reminder : reminders) {
            cancelReminder(reminder);
        }
        
        // Schedule them again with new settings
        scheduleReminders(reminders);
    }
}