package com.egyptian.agent.executors;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.receivers.AlarmReceiver;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executor for alarm-related commands
 * Handles setting alarms and reminders based on voice commands
 */
public class AlarmExecutor {
    private static final String TAG = "AlarmExecutor";

    /**
     * Handles an alarm command
     * @param context Context for the operation
     * @param command The command to process
     */
    public static void handleCommand(Context context, String command) {
        Log.d(TAG, "Handling alarm command: " + command);

        try {
            // Extract time from command
            Calendar alarmTime = extractTime(command);
            if (alarmTime == null) {
                TTSManager.speak(context, "الوقت مش واضح");
                return;
            }

            // Set the alarm
            setAlarm(context, alarmTime);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling alarm command: " + command, e);
            TTSManager.speak(context, "حصل مشكلة في ضبط المنبه");
        }
    }

    /**
     * Extracts time from command
     * @param command The command to extract from
     * @return Calendar object with the extracted time, or null if not found
     */
    private static Calendar extractTime(String command) {
        // Get current time
        Calendar now = Calendar.getInstance();
        Calendar alarmTime = (Calendar) now.clone();
        
        // Patterns for time extraction
        // This is a simplified implementation - a real one would be more comprehensive
        Pattern timePattern = Pattern.compile("(\\d{1,2})[\\s:]*([صمس]?)[\\s]*(?:[\\s:]*(\\d{1,2}))?");
        Matcher matcher = timePattern.matcher(command);
        
        if (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            String period = matcher.group(2) != null ? matcher.group(2).toLowerCase() : "";
            String minuteStr = matcher.group(3);
            int minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;
            
            // Handle 12-hour format
            if (hour <= 12) {
                if (period.contains("م") || period.contains("pm")) {
                    hour = (hour % 12) + 12; // Convert PM to 24-hour
                } else if (period.contains("ص") || period.contains("am")) {
                    hour = hour % 12; // Convert AM to 24-hour
                }
            }
            
            alarmTime.set(Calendar.HOUR_OF_DAY, hour);
            alarmTime.set(Calendar.MINUTE, minute);
            alarmTime.set(Calendar.SECOND, 0);
            
            // If the time has already passed today, set for tomorrow
            if (alarmTime.compareTo(now) <= 0) {
                alarmTime.add(Calendar.DAY_OF_MONTH, 1);
            }
            
            return alarmTime;
        }
        
        // Check for relative time expressions
        if (command.contains("بعد") || command.contains("ساعة")) {
            // Extract number of hours
            Pattern hourPattern = Pattern.compile("([\\d]+)\\s*(?:ساعات?|ساعة)");
            Matcher hourMatcher = hourPattern.matcher(command);
            
            if (hourMatcher.find()) {
                int hoursToAdd = Integer.parseInt(hourMatcher.group(1));
                alarmTime.add(Calendar.HOUR_OF_DAY, hoursToAdd);
                return alarmTime;
            }
            
            // Extract number of minutes
            Pattern minutePattern = Pattern.compile("([\\d]+)\\s*(?:دقائق?|دقيقة)");
            Matcher minuteMatcher = minutePattern.matcher(command);
            
            if (minuteMatcher.find()) {
                int minutesToAdd = Integer.parseInt(minuteMatcher.group(1));
                alarmTime.add(Calendar.MINUTE, minutesToAdd);
                return alarmTime;
            }
        }
        
        // Check for "tomorrow" or "after tomorrow"
        if (command.contains("بكرة") || command.contains("الغد")) {
            alarmTime.add(Calendar.DAY_OF_MONTH, 1);
            // Set to morning if no specific time mentioned
            if (!command.contains(":") && !command.matches(".*\\d+.*")) {
                alarmTime.set(Calendar.HOUR_OF_DAY, 8); // Default to 8 AM
                alarmTime.set(Calendar.MINUTE, 0);
            }
            return alarmTime;
        }
        
        if (command.contains("بعد بكرة") || command.contains("تاني")) {
            alarmTime.add(Calendar.DAY_OF_MONTH, 2);
            // Set to morning if no specific time mentioned
            if (!command.contains(":") && !command.matches(".*\\d+.*")) {
                alarmTime.set(Calendar.HOUR_OF_DAY, 8); // Default to 8 AM
                alarmTime.set(Calendar.MINUTE, 0);
            }
            return alarmTime;
        }
        
        return null;
    }

    /**
     * Sets an alarm
     * @param context Context for the operation
     * @param alarmTime The time to set the alarm for
     */
    private static void setAlarm(Context context, Calendar alarmTime) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            
            // Create intent for the alarm
            Intent intent = new Intent(context, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | 
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );
            
            // Set the alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
            
            // Format the alarm time for TTS
            String timeString = formatTime(alarmTime);
            
            Log.d(TAG, "Alarm set for: " + timeString);
            TTSManager.speak(context, "تم ضبط المنبه لـ " + timeString);
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting alarm", e);
            TTSManager.speak(context, "مقدرش أضبط المنبه");
        }
    }

    /**
     * Formats time for TTS
     * @param calendar Calendar object with the time
     * @return Formatted time string
     */
    private static String formatTime(Calendar calendar) {
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        String period = "ص";
        if (hour >= 12) {
            period = "م";
            if (hour > 12) {
                hour -= 12;
            }
        }
        if (hour == 0) {
            hour = 12;
        }
        
        return String.format("%d:%02d %s", hour, minute, period);
    }
}