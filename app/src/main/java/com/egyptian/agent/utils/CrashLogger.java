package com.egyptian.agent.utils;

import android.content.Context;
import android.util.Log;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Production-grade crash reporting system for Egyptian Agent
 * Logs errors securely while protecting user privacy
 */
public class CrashLogger implements UncaughtExceptionHandler {
    private static final String TAG = "CrashLogger";
    private static final String CRASH_LOG_FILE = "crash_log.txt";
    
    private static CrashLogger instance;
    private static UncaughtExceptionHandler defaultExceptionHandler;
    private Context context;
    
    private CrashLogger(Context context) {
        this.context = context.getApplicationContext();
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
    
    public static synchronized void registerGlobalExceptionHandler(Context context) {
        if (instance == null) {
            instance = new CrashLogger(context);
        }
    }
    
    public static void logError(Context context, Throwable throwable) {
        logError(context, "General Error", throwable);
    }
    
    public static void logError(Context context, String message, Throwable throwable) {
        String errorDetails = formatError(message, throwable);
        Log.e(TAG, errorDetails);
        
        // Write to secure local log file
        writeToFile(context, errorDetails);
    }
    
    private static String formatError(String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        
        sb.append("=== CRASH LOG ===\n");
        sb.append("Timestamp: ").append(sdf.format(new Date())).append("\n");
        sb.append("Message: ").append(message).append("\n");
        sb.append("Exception: ").append(throwable.getClass().getName()).append("\n");
        sb.append("Cause: ").append(throwable.getMessage()).append("\n");
        sb.append("Stack Trace:\n");
        
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        
        sb.append("==================\n\n");
        
        return sb.toString();
    }
    
    private static void writeToFile(Context context, String errorDetails) {
        try {
            FileWriter writer = new FileWriter(new java.io.File(context.getFilesDir(), CRASH_LOG_FILE), true);
            writer.append(errorDetails);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write crash log to file", e);
        }
    }
    
    /**
     * Retrieves crash logs for diagnostics
     */
    public static String getRecentLogs(Context context) {
        try {
            java.io.File logFile = new java.io.File(context.getFilesDir(), CRASH_LOG_FILE);
            if (!logFile.exists()) {
                return "No crash logs found.";
            }
            
            java.util.Scanner scanner = new java.util.Scanner(logFile);
            StringBuilder content = new StringBuilder();
            while (scanner.hasNextLine()) {
                content.append(scanner.nextLine()).append("\n");
            }
            scanner.close();
            
            return content.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to read crash logs", e);
            return "Error reading crash logs: " + e.getMessage();
        }
    }
    
    /**
     * Clears crash logs
     */
    public static void clearLogs(Context context) {
        try {
            java.io.File logFile = new java.io.File(context.getFilesDir(), CRASH_LOG_FILE);
            if (logFile.exists()) {
                logFile.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear crash logs", e);
        }
    }
    
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Log.e(TAG, "Uncaught exception in thread: " + thread.getName(), ex);
        logError(context, "Uncaught Exception in " + thread.getName(), ex);
        
        // Allow the default handler to run as well
        if (defaultExceptionHandler != null) {
            defaultExceptionHandler.uncaughtException(thread, ex);
        } else {
            // If no default handler, exit gracefully
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        }
    }
}