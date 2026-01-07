package com.egyptian.agent.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashLogger {

    private static final String TAG = "CrashLogger";
    private static final String LOGS_DIRECTORY = "egyptian_agent_logs";
    private static final int MAX_LOG_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_LOG_FILES = 5;

    // Singleton instance
    private static CrashLogger instance;
    private Context context;

    private CrashLogger(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized CrashLogger getInstance(Context context) {
        if (instance == null) {
            instance = new CrashLogger(context);
        }
        return instance;
    }

    /**
     * Log an error with detailed information
     */
    public static void logError(Context context, Throwable error) {
        getInstance(context).logErrorInternal(error);
    }

    /**
     * Log a warning message
     */
    public static void logWarning(Context context, String message) {
        getInstance(context).logWarningInternal(message);
    }

    /**
     * Internal method to log error
     */
    private void logErrorInternal(Throwable error) {
        if (error == null) return;

        try {
            String logEntry = buildLogEntry("ERROR", getStackTrace(error));
            writeLog(logEntry);

            Log.e(TAG, "Error logged: " + error.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Failed to log error", e);
        }
    }

    /**
     * Internal method to log warning
     */
    private void logWarningInternal(String message) {
        if (message == null || message.isEmpty()) return;

        try {
            String logEntry = buildLogEntry("WARNING", message);
            writeLog(logEntry);

            Log.w(TAG, "Warning logged: " + message);
        } catch (Exception e) {
            Log.e(TAG, "Failed to log warning", e);
        }
    }

    /**
     * Build formatted log entry
     */
    private String buildLogEntry(String level, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
        String deviceId = android.provider.Settings.Secure.getString(
            context.getContentResolver(),
            android.provider.Settings.Secure.ANDROID_ID
        );

        return String.format(
            "[%s] [%s] [Device: %s] [Version: %s]\n%s\n\n",
            timestamp,
            level,
            deviceId,
            "1.0", // Using placeholder version
            message
        );
    }

    /**
     * Get stack trace as string
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) return "";

        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Write log entry to file
     */
    private void writeLog(String logEntry) throws IOException {
        // Create logs directory
        File logsDir = new File(context.getExternalFilesDir(null), LOGS_DIRECTORY);
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }

        // Rotate log files if needed
        rotateLogFiles(logsDir);

        // Create/write to log file
        File logFile = new File(logsDir, "current_log.txt");

        try (FileWriter writer = new FileWriter(logFile, true)) { // Append mode
            writer.write(logEntry);
        }

        // Check file size and rotate if needed
        checkAndRotateLogFile(logFile);
    }

    /**
     * Rotate log files to prevent excessive size
     */
    private void rotateLogFiles(File logsDir) {
        File currentLogFile = new File(logsDir, "current_log.txt");

        if (currentLogFile.exists() && currentLogFile.length() > MAX_LOG_SIZE) {
            // Rename old files
            for (int i = MAX_LOG_FILES - 1; i >= 1; i--) {
                File oldFile = new File(logsDir, "log_" + i + ".txt");
                File newFile = new File(logsDir, "log_" + (i + 1) + ".txt");
                if (oldFile.exists()) {
                    oldFile.renameTo(newFile);
                }
            }

            // Rename current log to log_1
            File log1File = new File(logsDir, "log_1.txt");
            if (currentLogFile.exists()) {
                currentLogFile.renameTo(log1File);
            }
        }
    }

    /**
     * Check log file size and rotate if needed
     */
    private void checkAndRotateLogFile(File logFile) {
        if (logFile.length() > MAX_LOG_SIZE) {
            // In a real app, we would compress and archive old logs
            // For now, just truncate the file
            try {
                logFile.delete();
            } catch (Exception e) {
                Log.e(TAG, "Failed to rotate log file", e);
            }
        }
    }

    /**
     * Read logs (for sending to guardian)
     */
    public String readLogs() {
        try {
            File logsDir = new File(context.getExternalFilesDir(null), LOGS_DIRECTORY);
            File currentLogFile = new File(logsDir, "current_log.txt");

            if (!currentLogFile.exists()) {
                return "No logs available";
            }

            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(currentLogFile));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();

            return content.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to read logs", e);
        }

        return "Failed to read logs";
    }

    /**
     * Send logs to guardian via WhatsApp
     */
    public void sendLogsToGuardian(Context context) {
        // if (!SeniorMode.isEnabled()) {
        //     TTSManager.speak(context, "الميزة دي متاحة بس في وضع كبار السن");
        //     return;
        // }
        //
        // TTSManager.speak(context, "عايز أبعت سجلات الأخطاء لولي أمرك؟ قول 'نعم'");
        //
        // SpeechConfirmation.waitForConfirmation(context, confirmed -> {
        //     if (confirmed) {
        //         String logs = readLogs();
        //         WhatsAppExecutor.sendLogsToGuardian(context, logs);
        //         TTSManager.speak(context, "السجلات اتبعتت. الشكر لله");
        //     } else {
        //         TTSManager.speak(context, "ما بعتناش السجلات");
        //     }
        // });
    }

    /**
     * Clear all logs (privacy feature)
     */
    public void clearAllLogs() {
        try {
            File logsDir = new File(context.getExternalFilesDir(null), LOGS_DIRECTORY);
            if (logsDir.exists()) {
                File[] files = logsDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
                logsDir.delete();
            }

            TTSManager.speak(context, "كل السجلات اتمسحت");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear logs", e);
            TTSManager.speak(context, "حصل مشكلة في مسح السجلات");
        }
    }

    /**
     * Register global exception handler
     */
    public static void registerGlobalExceptionHandler(Context context) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            getInstance(context).logErrorInternal(throwable);

            // Also speak the error in senior mode
            // if (SeniorMode.isEnabled()) {
            //     TTSManager.speak(context, "حصل خطأ غير متوقع. بعت الإبلاغ للدعم");
            //     WhatsAppExecutor.sendEmergencyWhatsApp(context, "01000000000", "Critical App Crash");
            // }

            // Call original handler if exists
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        });
    }
}