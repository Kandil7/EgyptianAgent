package com.egyptian.agent.executors;

import android.content.Context;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Executor for reading the current time
 */
public class TimeExecutor {
    private static final String TAG = "TimeExecutor";

    public static void handleCommand(Context context) {
        Log.i(TAG, "Handling time command");

        try {
            // Get current time
            Date currentTime = new Date();
            
            // Format time in Egyptian Arabic
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", new Locale("ar"));
            String timeString = timeFormat.format(currentTime);
            
            // Convert English AM/PM to Arabic equivalents
            timeString = timeString.replace("AM", "ص").replace("PM", "م");
            
            // Format date in Egyptian Arabic
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ar"));
            String dateString = dateFormat.format(currentTime);
            
            // Speak the time and date
            String fullDateTime = "الساعة " + timeString + " في يوم " + dateString;
            TTSManager.speak(context, fullDateTime);
            
            Log.d(TAG, "Time spoken: " + fullDateTime);
        } catch (Exception e) {
            Log.e(TAG, "Error reading time", e);
            TTSManager.speak(context, "مقدرش أقرا الوقت دلوقتي");
        }
    }
}