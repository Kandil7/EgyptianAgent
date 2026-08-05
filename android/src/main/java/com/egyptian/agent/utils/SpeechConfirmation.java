package com.egyptian.agent.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.stt.VoskSTTEngine;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpeechConfirmation {

    /**
     * Synchronous wait for user confirmation (yes/no) through voice input
     * @param context Application context
     * @return true if user confirmed, false otherwise
     */
    public static boolean waitForConfirmation(Context context) {
        return waitForConfirmation(context, 5000);
    }

    /**
     * Synchronous wait for user confirmation with custom timeout
     * @param context Application context
     * @param timeoutMs Timeout in milliseconds
     * @return true if user confirmed, false otherwise
     */
    public static boolean waitForConfirmation(Context context, long timeoutMs) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean result = new AtomicBoolean(false);

        waitForConfirmation(context, timeoutMs, confirmed -> {
            result.set(confirmed);
            latch.countDown();
        });

        try {
            latch.await(timeoutMs + 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return result.get();
    }

    /**
     * Wait for user confirmation (yes/no) through voice input
     */
    public static void waitForConfirmation(Context context, ConfirmationCallback callback) {
        waitForConfirmation(context, 5000, callback); // Default 5 seconds timeout
    }

    /**
     * Wait for user confirmation with custom timeout
     */
    public static void waitForConfirmation(Context context, long timeoutMs, ConfirmationCallback callback) {
        TTSManager.speak(context, "أنا بسمع...");

        try {
            VoskSTTEngine sttEngine = new VoskSTTEngine(context);

            // Wait for initialization
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                sttEngine.startListening(new VoskSTTEngine.STTCallback() {
                    @Override
                    public void onResult(String text) {
                        boolean isConfirmed = isConfirmation(text);
                        callback.onResult(isConfirmed);
                        sttEngine.destroy();
                    }
                });

                // Set timeout
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    sttEngine.stopListening();
                    sttEngine.destroy();
                    callback.onResult(false); // Default to false on timeout
                }, timeoutMs);
            }, 500);

        } catch (Exception e) {
            TTSManager.speak(context, "حصل خطأ في الاستماع");
            callback.onResult(false);
        }
    }

    /**
     * Wait for a specific command from user
     */
    public static void waitForCommand(Context context, long timeoutMs, CommandCallback callback) {
        TTSManager.speak(context, "أنا بسمع...");

        try {
            VoskSTTEngine sttEngine = new VoskSTTEngine(context);
            
            // Wait for initialization
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                sttEngine.startListening(new VoskSTTEngine.STTCallback() {
                    @Override
                    public void onResult(String text) {
                        callback.onResult(text);
                        sttEngine.destroy();
                    }
                });

                // Set timeout
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    sttEngine.stopListening();
                    sttEngine.destroy();
                    callback.onResult("");
                }, timeoutMs);
            }, 500);

        } catch (Exception e) {
            TTSManager.speak(context, "حصل خطأ في الاستماع");
            callback.onResult("");
        }
    }
    
    /**
     * Check if the recognized text is a confirmation
     */
    private static boolean isConfirmation(String text) {
        if (text == null) return false;
        
        String lowerText = text.toLowerCase();
        
        // Egyptian dialect confirmations
        return lowerText.contains("نعم") || 
               lowerText.contains("أكيد") || 
               lowerText.contains("تمام") || 
               lowerText.contains("أيوة") || 
               lowerText.contains("ايوة") || 
               lowerText.contains("صح") || 
               lowerText.contains("صحيح") ||
               lowerText.contains("عايز") ||
               lowerText.contains("محتاج");
    }
    
    /**
     * Check if the recognized text is a negative response
     */
    private static boolean isNegative(String text) {
        if (text == null) return false;
        
        String lowerText = text.toLowerCase();
        
        // Egyptian dialect negative responses
        return lowerText.contains("لا") || 
               lowerText.contains("ميهوش") || 
               lowerText.contains("ميهيتش") || 
               lowerText.contains("مفيش") || 
               lowerText.contains("مش عايز") ||
               lowerText.contains("لاthanks") ||
               lowerText.contains("مش محتاج");
    }
    
    public interface ConfirmationCallback {
        void onResult(boolean confirmed);
    }
    
    public interface CommandCallback {
        void onResult(String command);
    }
}