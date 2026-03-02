package com.egyptian.agent.executor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.nlu.IntentResult;
import com.egyptian.agent.receivers.AlarmReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Alarm Controller
 * 
 * Handles alarm and reminder commands:
 * - Set alarms
 * - Set reminders
 * - Read current time
 * - Cancel alarms
 */
public class AlarmController {
    private static final String TAG = "AlarmController";
    
    private final Context context;
    private final AlarmManager alarmManager;
    
    public AlarmController(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    
    /**
     * Set alarm.
     */
    public ExecutorResult setAlarm(IntentResult intent) {
        String timeStr = intent.getEntity("time", "");
        
        if (timeStr.isEmpty()) {
            TTSManager.speak(context, "متى عايز التنبيه؟");
            return ExecutorResult.error("No time specified");
        }
        
        try {
            // Parse time expression
            Calendar calendar = parseTimeExpression(timeStr);
            
            if (calendar == null) {
                TTSManager.speak(context, "مش فاهم الوقت اللي قولته");
                return ExecutorResult.error("Invalid time expression: " + timeStr);
            }
            
            // Set alarm
            setAlarmAt(calendar, timeStr);
            
            String timeDisplay = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.getTime());
            String message = "ظبطت منبه لـ " + timeDisplay;
            TTSManager.speak(context, message);
            
            Log.i(TAG, "Alarm set for: " + timeDisplay);
            return ExecutorResult.success(message, "ALARM:" + calendar.getTimeInMillis());
        } catch (Exception e) {
            Log.e(TAG, "Error setting alarm", e);
            TTSManager.speak(context, "مش قادر اضبط المنبه");
            return ExecutorResult.error("Failed to set alarm: " + e.getMessage());
        }
    }
    
    /**
     * Read current time.
     */
    public ExecutorResult readTime(IntentResult intent) {
        try {
            String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            String day = new SimpleDateFormat("EEEE", new Locale("ar")).format(new Date());
            
            String message = "الساعة " + currentTime + "، " + day;
            TTSManager.speak(context, message);
            
            Log.d(TAG, "Time read: " + message);
            return ExecutorResult.success(message, "TIME:" + currentTime);
        } catch (Exception e) {
            Log.e(TAG, "Error reading time", e);
            TTSManager.speak(context, "في مشكلة في قراءة الوقت");
            return ExecutorResult.error("Failed to read time: " + e.getMessage());
        }
    }
    
    /**
     * Parse Egyptian time expression.
     */
    private Calendar parseTimeExpression(String timeStr) {
        Calendar now = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();
        
        String time = timeStr.toLowerCase();
        
        // Check for relative times
        if (time.contains("بعد ساعة") || time.contains("ساعة")) {
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            return calendar;
        }
        
        if (time.contains("بعد نص ساعة") || time.contains("نص ساعة")) {
            calendar.add(Calendar.MINUTE, 30);
            return calendar;
        }
        
        if (time.contains("بعد دقيقتين") || time.contains("دقيقتين")) {
            calendar.add(Calendar.MINUTE, 2);
            return calendar;
        }
        
        if (time.contains("بعد 10 دقايق") || time.contains("10 دقايق")) {
            calendar.add(Calendar.MINUTE, 10);
            return calendar;
        }
        
        // Check for time of day
        if (time.contains("الصبح") || time.contains("الصباح")) {
            calendar.set(Calendar.HOUR_OF_DAY, 8);
            calendar.set(Calendar.MINUTE, 0);
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            return calendar;
        }
        
        if (time.contains("الظهر")) {
            calendar.set(Calendar.HOUR_OF_DAY, 12);
            calendar.set(Calendar.MINUTE, 0);
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            return calendar;
        }
        
        if (time.contains("العصر")) {
            calendar.set(Calendar.HOUR_OF_DAY, 16);
            calendar.set(Calendar.MINUTE, 0);
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            return calendar;
        }
        
        if (time.contains("المغرب")) {
            calendar.set(Calendar.HOUR_OF_DAY, 18);
            calendar.set(Calendar.MINUTE, 0);
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            return calendar;
        }
        
        if (time.contains("العشا") || time.contains("العشاء")) {
            calendar.set(Calendar.HOUR_OF_DAY, 20);
            calendar.set(Calendar.MINUTE, 0);
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            return calendar;
        }
        
        if (time.contains("الليل")) {
            calendar.set(Calendar.HOUR_OF_DAY, 21);
            calendar.set(Calendar.MINUTE, 0);
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
            return calendar;
        }
        
        // Check for "tomorrow"
        if (time.contains("بكرة")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            // Default to morning if no specific time
            if (!time.contains("الظهر") && !time.contains("العصر") && 
                !time.contains("المغرب") && !time.contains("العشا")) {
                calendar.set(Calendar.HOUR_OF_DAY, 8);
                calendar.set(Calendar.MINUTE, 0);
            }
            return calendar;
        }
        
        return null;
    }
    
    /**
     * Set alarm at specific time.
     */
    private void setAlarmAt(Calendar calendar, String label) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("label", label);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            (int) calendar.getTimeInMillis(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            }
        }
    }
    
    /**
     * Cancel current operation.
     */
    public void cancel() {
        Log.d(TAG, "Cancelling alarm operation");
    }
}
