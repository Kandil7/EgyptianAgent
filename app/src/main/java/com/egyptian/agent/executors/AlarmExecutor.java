package com.egyptian.agent.executors;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.VibrationManager;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Handles alarm and reminder execution with Egyptian dialect support
 */
public class AlarmExecutor {
    private static final String TAG = "AlarmExecutor";
    private static final String ALARM_ACTION = "com.egyptian.agent.ALARM_TRIGGERED";

    /**
     * Handles an alarm command from the user
     * @param context Application context
     * @param command The raw command from speech recognition
     */
    public static void handleCommand(Context context, String command) {
        Log.i(TAG, "Handling alarm command: " + command);

        // Normalize Egyptian dialect
        String normalizedCommand = EgyptianNormalizer.normalize(command);
        Log.d(TAG, "Normalized command: " + normalizedCommand);

        // Extract time and message
        AlarmCommand parsedCommand = parseAlarmCommand(normalizedCommand);
        
        if (parsedCommand.timeInMillis == 0) {
            TTSManager.speak(context, "متى عايز التنبيه؟ قول الوقت");
            return;
        }

        // Senior mode special handling
        if (SeniorMode.isEnabled()) {
            VibrationManager.vibrateShort(context);
            TTSManager.speak(context, "عايز تنبيه بعنوان \"" + parsedCommand.message + 
                "\" في " + formatTime(parsedCommand.timeInMillis) + "؟ قول 'نعم' بس، ولا 'لا'");
            
            // Wait for user confirmation using speech recognition
            SpeechConfirmation.waitForConfirmation(context, 10000, confirmed -> {
                if (confirmed) {
                    VibrationManager.vibrateLong(context);
                    scheduleAlarm(context, parsedCommand);
                    TTSManager.speak(context, "تم ضبط التنبيه بعنوان \"" + parsedCommand.message +
                        "\" في " + formatTime(parsedCommand.timeInMillis));
                } else {
                    TTSManager.speak(context, "ما ضبطتش التنبيه. قول 'يا كبير' لو عايز تضبط تنبيه تاني");
                }
            });
            
            return;
        }

        // Standard confirmation
        TTSManager.speak(context, "عايز تنبيه بعنوان \"" + parsedCommand.message + 
            "\" في " + formatTime(parsedCommand.timeInMillis) + "؟ قول 'نعم' أو 'لا'");
        
        // For this demo, we'll proceed directly
        scheduleAlarm(context, parsedCommand);
    }

    /**
     * Parses the alarm command to extract time and message
     */
    private static AlarmCommand parseAlarmCommand(String command) {
        long timeInMillis = 0;
        String message = "تذكير";
        
        // Try to extract time from command
        timeInMillis = extractTimeFromCommand(command);
        
        // If no time was extracted, try to parse relative time
        if (timeInMillis == 0) {
            timeInMillis = extractRelativeTime(command);
        }
        
        // Extract message content
        message = extractMessage(command);
        
        return new AlarmCommand(timeInMillis, message);
    }

    /**
     * Extracts absolute time from command
     */
    private static long extractTimeFromCommand(String command) {
        // Look for time patterns like "at 8 am", "at 3:30 pm", etc.
        // Also look for Egyptian dialect patterns

        // Try to parse specific times using regex
        String[] timePatterns = {
            "الساعة\\s*(\\d{1,2})(?::(\\d{2}))?",
            "الساعه\\s*(\\d{1,2})(?::(\\d{2}))?",
            "at\\s*(\\d{1,2})(?::(\\d{2}))?",
            "(\\d{1,2})\\s*o'clock",
            "(\\d{1,2})\\s*(am|pm)",
            "(\\d{1,2}):(\\d{2})\\s*(am|pm)"
        };

        for (String pattern : timePatterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(command);

            if (m.find()) {
                try {
                    int hour = Integer.parseInt(m.group(1));
                    int minute = 0;

                    // Handle 12-hour format
                    if (m.groupCount() >= 2 && m.group(2) != null && !m.group(2).matches("am|pm")) {
                        minute = Integer.parseInt(m.group(2));
                    }

                    String period = null;
                    if (m.groupCount() >= 3 && m.group(3) != null) {
                        period = m.group(3).toLowerCase();
                    } else if (m.groupCount() >= 2 && m.group(2) != null && m.group(2).matches("am|pm")) {
                        period = m.group(2).toLowerCase();
                    }

                    if (period != null) {
                        if (period.equals("pm") && hour != 12) {
                            hour += 12;
                        } else if (period.equals("am") && hour == 12) {
                            hour = 0;
                        }
                    }

                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, minute);
                    cal.set(Calendar.SECOND, 0);

                    // If time has passed today, set for tomorrow
                    if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                        cal.add(Calendar.DAY_OF_MONTH, 1);
                    }

                    return cal.getTimeInMillis();
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing time from command: " + command, e);
                }
            }
        }

        // Try to parse Egyptian time expressions
        if (command.contains("الصبح") || command.contains("sobh") || command.contains("morning")) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 7); // 7 AM
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);

            // If it's already past 7 AM today, set for tomorrow
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
            return cal.getTimeInMillis();
        } else if (command.contains("الظهر") || command.contains("zuhr") || command.contains("noon")) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 12); // 12 PM
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);

            // If it's already past 12 PM today, set for tomorrow
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
            return cal.getTimeInMillis();
        } else if (command.contains("العشية") || command.contains("mesa2") || command.contains("evening")) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 18); // 6 PM
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);

            // If it's already past 6 PM today, set for tomorrow
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
            return cal.getTimeInMillis();
        } else if (command.contains("الليل") || command.contains("layl") || command.contains("night")) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 22); // 10 PM
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);

            // If it's already past 10 PM today, set for tomorrow
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
            return cal.getTimeInMillis();
        }

        return 0;
    }

    /**
     * Extracts relative time from command (e.g., "in 1 hour", "after 30 minutes")
     */
    private static long extractRelativeTime(String command) {
        Calendar cal = Calendar.getInstance();
        
        // Look for patterns like "in X hours/minutes", "after X hours/minutes"
        if (command.contains("after") || command.contains("ba3d") || command.contains("bet")) {
            // Extract number and unit
            String[] words = command.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                if (words[i].equals("after") || words[i].equals("ba3d") || words[i].equals("bet")) {
                    if (i + 2 < words.length) {
                        try {
                            int amount = Integer.parseInt(words[i + 1]);
                            
                            if (words[i + 2].contains("hour") || words[i + 2].contains("sa3a")) {
                                cal.add(Calendar.HOUR_OF_DAY, amount);
                                return cal.getTimeInMillis();
                            } else if (words[i + 2].contains("minute") || words[i + 2].contains("dakika")) {
                                cal.add(Calendar.MINUTE, amount);
                                return cal.getTimeInMillis();
                            }
                        } catch (NumberFormatException e) {
                            // Ignore if not a number
                        }
                    }
                }
            }
        }
        
        // Look for "tomorrow" patterns
        if (command.contains("بكرة") || command.contains("bokra") || command.contains("tomorrow")) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            
            // Set to morning if not specified otherwise
            if (!command.contains("الصبح") && !command.contains("sobh") && 
                !command.contains("morning") && !command.contains("الظهر") && 
                !command.contains("zuhr") && !command.contains("noon") &&
                !command.contains("العشية") && !command.contains("mesa2") && 
                !command.contains("evening") && !command.contains("الليل") && 
                !command.contains("layl") && !command.contains("night")) {
                cal.set(Calendar.HOUR_OF_DAY, 8); // 8 AM
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
            }
            
            return cal.getTimeInMillis();
        }
        
        // Look for "now" patterns
        if (command.contains("دلوقتي") || command.contains("dalo2ty") || command.contains("now")) {
            cal.add(Calendar.MINUTE, 1); // Set for 1 minute from now
            return cal.getTimeInMillis();
        }
        
        return 0;
    }

    /**
     * Extracts message content from command
     */
    private static String extractMessage(String command) {
        // Remove time-related phrases to get the message
        String message = command.replaceAll("set alarm|set reminder|remind me|think me|nbhny|anbhny|zkry|thker", "")
                                .replaceAll("at [0-9]{1,2}|at [0-9]{1,2}:[0-9]{2}", "")
                                .replaceAll("الساعة [0-9]{1,2}|الساعه [0-9]{1,2}", "")
                                .replaceAll("in [0-9]+ (hours?|minutes?)", "")
                                .replaceAll("after [0-9]+ (hours?|minutes?)", "")
                                .replaceAll("tomorrow|bokra|بكرة", "")
                                .replaceAll("morning|sobh|الصبح", "")
                                .replaceAll("evening|mesa2|العشية", "")
                                .replaceAll("night|layl|الليل", "")
                                .replaceAll("now|dalo2ty|دلوقتي", "")
                                .trim();
        
        // If message is empty, use a default
        if (message.isEmpty()) {
            message = "تذكير";
        }
        
        return message;
    }

    /**
     * Schedules the alarm
     */
    private static void scheduleAlarm(Context context, AlarmCommand alarmCommand) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        // Create intent for the alarm
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ALARM_ACTION);
        intent.putExtra("message", alarmCommand.message);
        
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
                alarmCommand.timeInMillis,
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                alarmCommand.timeInMillis,
                pendingIntent
            );
        }
        
        Log.i(TAG, "Alarm scheduled for: " + new Date(alarmCommand.timeInMillis));
    }

    /**
     * Formats time for speech output
     */
    private static String formatTime(long timeInMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timeInMillis));
    }

    /**
     * Data class to hold parsed alarm command
     */
    private static class AlarmCommand {
        final long timeInMillis;
        final String message;
        
        AlarmCommand(long timeInMillis, String message) {
            this.timeInMillis = timeInMillis;
            this.message = message;
        }
    }
    
    /**
     * BroadcastReceiver to handle alarm triggers
     */
    public static class AlarmReceiver extends android.content.BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            
            // Speak the reminder
            TTSManager.speak(context, "تذكير: " + message);
            
            // Vibrate to get attention
            VibrationManager.vibrateLong(context);
            
            Log.i(TAG, "Alarm triggered: " + message);
        }
    }
}