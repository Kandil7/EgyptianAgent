package com.egyptian.agent.executors;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlarmExecutor {

    private static final String TAG = "AlarmExecutor";

    public static void handleCommand(Context context, String command) {
        Log.i(TAG, "Handling alarm command: " + command);

        // Extract time from command
        Calendar alarmTime = extractTimeFromCommand(command);
        
        if (alarmTime == null) {
            TTSManager.speak(context, "مش فاهم الوقت. قول 'نبهني بكرة الصبح' أو 'انبهني بعد ساعة'");
            return;
        }

        // Set the alarm
        setAlarm(context, alarmTime);

        // Confirm to user
        String timeString = formatTime(alarmTime);
        TTSManager.speak(context, "تم ضبط المنبه على الساعة " + timeString);
    }

    private static Calendar extractTimeFromCommand(String command) {
        // Normalize the command
        String normalized = command.toLowerCase();
        
        Calendar calendar = Calendar.getInstance();
        
        // Pattern for "after X hours/minutes"
        Pattern afterPattern = Pattern.compile("بعد\\s*(\\d+)\\s*(ساعات?|دقايق?)");
        Matcher afterMatcher = afterPattern.matcher(normalized);
        
        if (afterMatcher.find()) {
            int amount = Integer.parseInt(afterMatcher.group(1));
            String unit = afterMatcher.group(2);
            
            if (unit.contains("ساع")) {
                calendar.add(Calendar.HOUR_OF_DAY, amount);
            } else if (unit.contains("دقيق")) {
                calendar.add(Calendar.MINUTE, amount);
            }
            
            return calendar;
        }
        
        // Pattern for specific times like "بكرة الصبح", "بكرة الظهر", etc.
        if (normalized.contains("بكرة") || normalized.contains("غدا")) {
            // Set to tomorrow
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            
            if (normalized.contains("الصبح") || normalized.contains("الصباح")) {
                calendar.set(Calendar.HOUR_OF_DAY, 7); // 7 AM
                calendar.set(Calendar.MINUTE, 0);
            } else if (normalized.contains("الظهر")) {
                calendar.set(Calendar.HOUR_OF_DAY, 12); // 12 PM
                calendar.set(Calendar.MINUTE, 0);
            } else if (normalized.contains("العشا") || normalized.contains("المسا")) {
                calendar.set(Calendar.HOUR_OF_DAY, 18); // 6 PM
                calendar.set(Calendar.MINUTE, 0);
            } else {
                // Default to 8 AM if just "بكرة" is mentioned
                calendar.set(Calendar.HOUR_OF_DAY, 8);
                calendar.set(Calendar.MINUTE, 0);
            }
            
            return calendar;
        }
        
        // Pattern for today's times
        Pattern timePattern = Pattern.compile("(\\d{1,2})\\s*(و)?\\s*(\\d{1,2})?");
        Matcher timeMatcher = timePattern.matcher(normalized);
        
        if (timeMatcher.find()) {
            int hour = Integer.parseInt(timeMatcher.group(1));
            int minute = 0;
            
            if (timeMatcher.group(3) != null) {
                minute = Integer.parseInt(timeMatcher.group(3));
            }
            
            // Adjust for 24-hour format
            String period = normalized.substring(timeMatcher.end());
            if (period.contains("الص") || period.contains("صباح")) {
                // AM - leave as is if less than 12, otherwise subtract 12
                if (hour == 12) hour = 0;
            } else if (period.contains("المسا") || period.contains("الم") || period.contains("عشا")) {
                // PM - add 12 if less than 12
                if (hour < 12) hour += 12;
            }
            
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            
            // If the time has already passed today, set for tomorrow
            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            
            return calendar;
        }
        
        return null; // Unable to extract time
    }

    private static void setAlarm(Context context, Calendar alarmTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Create an intent for when the alarm goes off
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            0, 
            intent, 
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
    }

    private static String formatTime(Calendar calendar) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
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