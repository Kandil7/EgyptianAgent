package com.egyptian.agent.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Crash Logger
 * Handles error logging and crash reporting
 */
public class CrashLogger {
    private static final String TAG = "CrashLogger";
    private static final String LOG_FILE_NAME = "crash_logs.txt";
    
    /**
     * Logs an error with context
     * @param context Context for the operation
     * @param throwable The error to log
     */
    public static void logError(Context context, Throwable throwable) {
        String errorMessage = formatError(throwable);
        Log.e(TAG, errorMessage, throwable);
        
        // Write to file for persistent logging
        writeToFile(context, errorMessage);
    }
    
    /**
     * Logs an error with message
     * @param context Context for the operation
     * @param message The error message
     * @param throwable The error to log
     */
    public static void logError(Context context, String message, Throwable throwable) {
        String errorMessage = formatError(message, throwable);
        Log.e(TAG, errorMessage, throwable);
        
        // Write to file for persistent logging
        writeToFile(context, errorMessage);
    }
    
    /**
     * Formats an error for logging
     * @param throwable The error to format
     * @return Formatted error string
     */
    private static String formatError(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(getTimestamp()).append(" - ");
        sb.append("Error: ").append(throwable.getMessage()).append("\n");
        sb.append("Stack Trace:\n");
        
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("  at ").append(element.toString()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Formats an error with custom message
     * @param message Custom message
     * @param throwable The error to format
     * @return Formatted error string
     */
    private static String formatError(String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(getTimestamp()).append(" - ");
        sb.append("Message: ").append(message).append("\n");
        sb.append("Error: ").append(throwable.getMessage()).append("\n");
        sb.append("Stack Trace:\n");
        
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("  at ").append(element.toString()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Gets current timestamp
     * @return Formatted timestamp
     */
    private static String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    /**
     * Writes error to file
     * @param context Context for the operation
     * @param errorMessage The error message to write
     */
    private static void writeToFile(Context context, String errorMessage) {
        try {
            File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
            FileWriter writer = new FileWriter(logFile, true); // Append mode
            writer.append(errorMessage);
            writer.append("\n---\n"); // Separator between logs
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write error to file", e);
        }
    }
    
    /**
     * Gets the crash log file
     * @param context Context for the operation
     * @return The crash log file
     */
    public static File getLogFile(Context context) {
        return new File(context.getFilesDir(), LOG_FILE_NAME);
    }
    
    /**
     * Clears the crash log file
     * @param context Context for the operation
     */
    public static void clearLogs(Context context) {
        try {
            File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
            if (logFile.exists()) {
                logFile.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear logs", e);
        }
    }
}