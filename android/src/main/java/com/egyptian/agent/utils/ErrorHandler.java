package com.egyptian.agent.utils;

import android.content.Context;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.executors.GuardianNotificationSystem;
import com.egyptian.agent.security.DataEncryptionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Comprehensive error handling utility for production environment
 * Handles different types of errors and provides appropriate responses
 */
public class ErrorHandler {
    private static final String TAG = "ErrorHandler";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Handle a general error with appropriate logging and user feedback
     */
    public static void handleError(Context context, String operation, Throwable error) {
        Log.e(TAG, "Error during operation: " + operation, error);

        // Log the error for diagnostics
        CrashLogger.logError(context, error);

        // Provide user feedback based on the type of error
        String userMessage = getUserFriendlyErrorMessage(operation, error);
        TTSManager.speak(context, userMessage);

        // For critical errors, notify guardian
        if (isCriticalError(error)) {
            notifyGuardianOfError(context, operation, error);
        }
    }

    /**
     * Handle a recoverable error with retry mechanism
     */
    public static void handleRecoverableError(Context context, String operation, Throwable error, Runnable retryAction) {
        Log.w(TAG, "Recoverable error during operation: " + operation, error);

        // Log the warning
        CrashLogger.logWarning(context, "Recoverable error in " + operation + ": " + error.getMessage());

        // Inform user about the issue
        TTSManager.speak(context, "في مشكلة بسيطة. بحاول مرة تانية...");

        // Retry the action in a background thread
        executor.execute(() -> {
            try {
                Thread.sleep(2000); // Wait 2 seconds before retry
                retryAction.run();
            } catch (Exception retryError) {
                Log.e(TAG, "Retry failed for operation: " + operation, retryError);
                CrashLogger.logError(context, retryError);
                
                // If retry fails, inform user
                TTSManager.speak(context, "المحاولة الثانية فشلت. محتاج مساعدة.");
            }
        });
    }

    /**
     * Handle permission-related errors
     */
    public static void handlePermissionError(Context context, String permission) {
        Log.w(TAG, "Permission denied: " + permission);

        // Log the warning
        CrashLogger.logWarning(context, "Permission denied: " + permission);

        // Inform user how to fix
        String message = getPermissionErrorMessage(permission);
        TTSManager.speak(context, message);
    }

    /**
     * Generate user-friendly error message
     */
    private static String getUserFriendlyErrorMessage(String operation, Throwable error) {
        if (error instanceof SecurityException) {
            return "محتاج أذونات خاصة علشان أكمل ده.";
        } else if (error instanceof java.net.ConnectException || 
                   error instanceof java.net.UnknownHostException) {
            return "مفيش اتصال بالإنترنت. بستخدم الوضع المغلق.";
        } else if (error instanceof android.os.DeadObjectException) {
            return "الخدمة متوقفة. ببدأ تاني.";
        } else {
            return "حصل مشكلة في " + getOperationDescription(operation) + ". الشكر لله مفيش مشكلة كبيرة.";
        }
    }

    /**
     * Get description of operation for user messages
     */
    private static String getOperationDescription(String operation) {
        switch (operation) {
            case "placeCall":
                return "المكالمة";
            case "sendMessage":
                return "الرسالة";
            case "setAlarm":
                return "التنبيه";
            case "processVoiceCommand":
                return "الأمر الصوتي";
            case "detectFall":
                return "كشف السقوط";
            case "accessLocation":
                return "الموقع";
            default:
                return "العملية";
        }
    }

    /**
     * Get permission error message
     */
    private static String getPermissionErrorMessage(String permission) {
        switch (permission) {
            case "android.permission.CALL_PHONE":
                return "علشان أ接听 مكالمة، لازم أذن المكالمات. ادخل الإعدادات وشوف الأذونات.";
            case "android.permission.RECORD_AUDIO":
                return "علشان أسمعك، لازم أذن الميكروفون. ادخل الإعدادات وشوف الأذونات.";
            case "android.permission.BODY_SENSORS":
                return "علشان أكشف السقوط، لازم أذن المستشعرات. ادخل الإعدادات وشوف الأذونات.";
            case "android.permission.ACCESS_FINE_LOCATION":
                return "علشان أعرف موقعك في الطوارئ، لازم أذن الموقع. ادخل الإعدادات وشوف الأذونات.";
            default:
                return "محتاج أذن خاص. ادخل الإعدادات وشوف الأذونات.";
        }
    }

    /**
     * Check if error is critical enough to notify guardian
     */
    private static boolean isCriticalError(Throwable error) {
        // Critical errors include security issues, crashes, and service failures
        return error instanceof SecurityException ||
               error instanceof OutOfMemoryError ||
               error instanceof StackOverflowError ||
               error instanceof android.os.DeadObjectException;
    }

    /**
     * Notify guardian of critical error
     */
    private static void notifyGuardianOfError(Context context, String operation, Throwable error) {
        executor.execute(() -> {
            try {
                String errorMessage = "Critical error in " + operation + ": " + error.getClass().getSimpleName() + 
                                     " - " + error.getMessage();
                
                // Use the guardian notification system to alert family members
                GuardianNotificationSystem.notifyGuardiansOfIssue(context, errorMessage);
                
                Log.i(TAG, "Guardian notified of critical error");
            } catch (Exception e) {
                Log.e(TAG, "Failed to notify guardian of error", e);
            }
        });
    }

    /**
     * Handle resource exhaustion errors
     */
    public static void handleResourceExhaustion(Context context, String resourceType) {
        Log.w(TAG, "Resource exhausted: " + resourceType);

        // Log the warning
        CrashLogger.logWarning(context, "Resource exhausted: " + resourceType);

        // Free up memory if possible
        System.gc();

        // Inform user
        TTSManager.speak(context, "الجهاز معبان شوية. بخليه شوية.");
    }

    /**
     * Clean up resources when shutting down
     */
    public static void shutdown() {
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}