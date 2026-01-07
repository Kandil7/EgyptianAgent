package com.egyptian.agent.executors;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.CrashLogger;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlarmExecutor {
    
    private static final String TAG = "AlarmExecutor";
    
    public static void handleCommand(Context context, String command) {
        Log.i(TAG, "Handling alarm command: " + command);
        
        // Normalize the command
        String normalizedCommand = EgyptianNormalizer.normalize(command);
        Log.d(TAG, "Normalized command: " + normalizedCommand);
        
        // Extract time from command
        String timeExpression = EgyptianNormalizer.extractTimeExpression(normalizedCommand);
        Log.i(TAG, "Extracted time: " + timeExpression);
        
        if (timeExpression.isEmpty()) {
            TTSManager.speak(context, "قول الوقت اللي عايز تنامه. مثلاً: نبهني بكرة الصبح");
            return;
        }
        
        // Parse the time expression and set alarm
        setAlarm(context, timeExpression);
    }
    
    private static void setAlarm(Context context, String timeExpression) {
        try {
            Calendar alarmTime = parseTimeExpression(timeExpression);
            
            if (alarmTime == null) {
                TTSManager.speak(context, "مافهمتش الوقت. قول مثلاً: نبهني بعد ساعة أو نبهني بكرة الصبح");
                return;
            }
            
            // Create intent for the alarm
            Intent alarmIntent = new Intent("com.egyptian.agent.ALARM_TRIGGERED");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                alarmIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Get alarm manager
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            
            if (alarmManager != null) {
                // Set the alarm
                alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pendingIntent);
                
                // Confirm to user
                String confirmation = formatAlarmConfirmation(alarmTime);
                TTSManager.speak(context, confirmation);
                
                Log.i(TAG, "Alarm set for: " + alarmTime.getTime());
            } else {
                TTSManager.speak(context, "مافيش خدمة تنبيهات في النظام");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting alarm", e);
            TTSManager.speak(context, "حصل خطأ في ضبط التنبيه");
            CrashLogger.logError(context, e);
        }
    }
    
    private static Calendar parseTimeExpression(String timeExpression) {
        Calendar calendar = Calendar.getInstance();
        
        // Handle "after X hours/minutes" expressions
        Pattern afterPattern = Pattern.compile("بعد\\s+(\\d+)\\s+(ساع[ةه]|دقيق[ةه])");
        Matcher afterMatcher = afterPattern.matcher(timeExpression);
        
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
        
        // Handle "tomorrow morning/afternoon/evening" expressions
        if (timeExpression.contains("بكرة") && timeExpression.contains("الصبح")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1); // Tomorrow
            calendar.set(Calendar.HOUR_OF_DAY, 7); // 7 AM
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            return calendar;
        } else if (timeExpression.contains("بكرة") && timeExpression.contains("الظهر")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1); // Tomorrow
            calendar.set(Calendar.HOUR_OF_DAY, 12); // 12 PM
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            return calendar;
        } else if (timeExpression.contains("بكرة") && (timeExpression.contains("العشيه") || timeExpression.contains("الليل"))) {
            calendar.add(Calendar.DAY_OF_YEAR, 1); // Tomorrow
            calendar.set(Calendar.HOUR_OF_DAY, 20); // 8 PM
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            return calendar;
        }
        
        // Handle specific hour expressions like "الساعة 7"
        Pattern hourPattern = Pattern.compile("الساع[هةه]\\s+(\\d+)");
        Matcher hourMatcher = hourPattern.matcher(timeExpression);
        
        if (hourMatcher.find()) {
            int hour = Integer.parseInt(hourMatcher.group(1));
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            
            // If the time has already passed today, set for tomorrow
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            
            return calendar;
        }
        
        return null; // Could not parse
    }
    
    private static String formatAlarmConfirmation(Calendar alarmTime) {
        // Format the confirmation message based on the alarm time
        Calendar now = Calendar.getInstance();
        long diffInMillis = alarmTime.getTimeInMillis() - now.getTimeInMillis();
        long diffInHours = diffInMillis / (1000 * 60 * 60);
        
        if (diffInHours < 24) {
            // Same day - just say the time
            int hour = alarmTime.get(Calendar.HOUR_OF_DAY);
            int minute = alarmTime.get(Calendar.MINUTE);
            return String.format("تم ضبط التنبيه للساعة %d:%02d", hour, minute);
        } else {
            // Different day - include the date
            int day = alarmTime.get(Calendar.DAY_OF_MONTH);
            int month = alarmTime.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
            int hour = alarmTime.get(Calendar.HOUR_OF_DAY);
            int minute = alarmTime.get(Calendar.MINUTE);
            return String.format("تم ضبط التنبيه لليوم %d/%d الساعة %d:%02d", day, month, hour, minute);
        }
    }
}