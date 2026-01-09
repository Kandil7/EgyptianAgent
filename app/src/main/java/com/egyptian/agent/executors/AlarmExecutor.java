package com.egyptian.agent.executors;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.CrashLogger;
import com.egyptian.agent.utils.VibrationManager;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlarmExecutor {

    private static final String TAG = "AlarmExecutor";

    /**
     * Handle alarm command from user
     */
    public static void handleCommand(Context context, String command) {
        Log.i(TAG, "Handling alarm command: " + command);

        // Normalize Egyptian dialect
        String normalizedCommand = EgyptianNormalizer.normalize(command);
        Log.d(TAG, "Normalized command: " + normalizedCommand);

        // Extract time information
        Calendar alarmTime = extractAlarmTime(normalizedCommand);
        
        if (alarmTime == null) {
            TTSManager.speak(context, "متى عايز التنبيه؟ قول الوقت");
            // In a real app, we would wait for time input
            return;
        }

        // Senior mode requires confirmation
        if (SeniorMode.isEnabled()) {
            VibrationManager.vibrateShort(context);
            TTSManager.speak(context, "عايز أضع تنبيه لـ " + formatTime(alarmTime) + "؟ قول 'نعم' بس، ولا 'لا'");
            SpeechConfirmation.waitForConfirmation(context, 10000, confirmed -> {
                if (confirmed) {
                    setAlarm(context, alarmTime);
                    TTSManager.speak(context, "تم ضبط التنبيه لـ " + formatTime(alarmTime));
                } else {
                    TTSManager.speak(context, "ما ضبطتش التنبيه");
                }
            });
            return;
        }

        // Standard confirmation for normal mode
        TTSManager.speak(context, "عايز أضع تنبيه لـ " + formatTime(alarmTime) + "؟ قول 'نعم' أو 'لا'");
        SpeechConfirmation.waitForConfirmation(context, confirmed -> {
            if (confirmed) {
                setAlarm(context, alarmTime);
                TTSManager.speak(context, "تم ضبط التنبيه لـ " + formatTime(alarmTime));
            } else {
                TTSManager.speak(context, "ما ضبطتش التنبيه");
            }
        });
    }

    /**
     * Extract time information from command
     */
    private static Calendar extractAlarmTime(String command) {
        Calendar now = Calendar.getInstance();
        Calendar alarmTime = (Calendar) now.clone();

        // Look for specific time patterns
        Pattern timePattern = Pattern.compile("الساعة\\s*(\\d{1,2})\\s*(?:و\\s*(\\d{1,2})\\s*(?:دقيقه|دقايق)?)?");
        Matcher timeMatcher = timePattern.matcher(command);
        
        if (timeMatcher.find()) {
            int hour = Integer.parseInt(timeMatcher.group(1));
            int minute = 0;
            
            if (timeMatcher.group(2) != null) {
                minute = Integer.parseInt(timeMatcher.group(2));
            }
            
            alarmTime.set(Calendar.HOUR_OF_DAY, hour);
            alarmTime.set(Calendar.MINUTE, minute);
            alarmTime.set(Calendar.SECOND, 0);
            
            // If the time has already passed today, set for tomorrow
            if (alarmTime.before(now)) {
                alarmTime.add(Calendar.DAY_OF_MONTH, 1);
            }
            
            return alarmTime;
        }

        // Look for relative time patterns (in X minutes/hours)
        Pattern relativePattern = Pattern.compile("بعد\\s*(\\d+)\\s*(?:ساعه|ساعات|دقيقه|دقايق)");
        Matcher relativeMatcher = relativePattern.matcher(command);
        
        if (relativeMatcher.find()) {
            int amount = Integer.parseInt(relativeMatcher.group(1));
            String unit = relativeMatcher.group(0);
            
            if (unit.contains("ساعه") || unit.contains("ساعات")) {
                alarmTime.add(Calendar.HOUR_OF_DAY, amount);
            } else if (unit.contains("دقيقه") || unit.contains("دقايق")) {
                alarmTime.add(Calendar.MINUTE, amount);
            }
            
            return alarmTime;
        }

        // Look for common time expressions
        if (command.contains("الصبح") || command.contains("الصباح")) {
            alarmTime.set(Calendar.HOUR_OF_DAY, 7); // Default morning time
            alarmTime.set(Calendar.MINUTE, 0);
            alarmTime.set(Calendar.SECOND, 0);
            
            if (alarmTime.before(now)) {
                alarmTime.add(Calendar.DAY_OF_MONTH, 1);
            }
            
            return alarmTime;
        } else if (command.contains("الظهر")) {
            alarmTime.set(Calendar.HOUR_OF_DAY, 12);
            alarmTime.set(Calendar.MINUTE, 0);
            alarmTime.set(Calendar.SECOND, 0);
            
            if (alarmTime.before(now)) {
                alarmTime.add(Calendar.DAY_OF_MONTH, 1);
            }
            
            return alarmTime;
        } else if (command.contains("المساء") || command.contains("العشيه")) {
            alarmTime.set(Calendar.HOUR_OF_DAY, 18);
            alarmTime.set(Calendar.MINUTE, 0);
            alarmTime.set(Calendar.SECOND, 0);
            
            if (alarmTime.before(now)) {
                alarmTime.add(Calendar.DAY_OF_MONTH, 1);
            }
            
            return alarmTime;
        } else if (command.contains("الليل")) {
            alarmTime.set(Calendar.HOUR_OF_DAY, 22);
            alarmTime.set(Calendar.MINUTE, 0);
            alarmTime.set(Calendar.SECOND, 0);
            
            if (alarmTime.before(now)) {
                alarmTime.add(Calendar.DAY_OF_MONTH, 1);
            }
            
            return alarmTime;
        } else if (command.contains("بكرة") || command.contains("غدا")) {
            alarmTime.add(Calendar.DAY_OF_MONTH, 1);
            alarmTime.set(Calendar.HOUR_OF_DAY, 8); // Default tomorrow morning
            alarmTime.set(Calendar.MINUTE, 0);
            alarmTime.set(Calendar.SECOND, 0);
            
            return alarmTime;
        } else if (command.contains("النهارده") || command.contains("اليوم")) {
            // Keep the current time but adjust to a reasonable alarm time
            alarmTime.add(Calendar.MINUTE, 5); // Set for 5 minutes from now as example
            return alarmTime;
        }

        // If no time found, return null
        return null;
    }

    /**
     * Set the alarm using Android's AlarmManager
     */
    private static void setAlarm(Context context, Calendar alarmTime) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            
            // Create an intent for the alarm
            Intent alarmIntent = new Intent(context, AlarmReceiver.class);
            alarmIntent.setAction("com.egyptian.agent.ALARM_TRIGGERED");
            alarmIntent.putExtra("alarm_time", alarmTime.getTimeInMillis());
            
            // Create a pending intent
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                alarmIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Set the alarm
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime.getTimeInMillis(),
                    pendingIntent
                );
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime.getTimeInMillis(),
                    pendingIntent
                );
            }

            Log.i(TAG, "Alarm set for: " + alarmTime.getTime());
        } catch (Exception e) {
            Log.e(TAG, "Failed to set alarm", e);
            CrashLogger.logError(context, e);
            TTSManager.speak(context, "حصل مشكلة في ضبط التنبيه");
        }
    }

    /**
     * Format time for speech output
     */
    private static String formatTime(Calendar time) {
        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);
        
        String amPm = "ص";
        if (hour >= 12) {
            amPm = "م";
            if (hour > 12) {
                hour -= 12;
            }
        }
        if (hour == 0) {
            hour = 12;
        }
        
        return String.format("%d:%02d %s", hour, minute, amPm);
    }
}