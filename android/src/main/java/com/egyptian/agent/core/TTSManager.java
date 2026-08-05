package com.egyptian.agent.core;

import android.content.Context;
import android.util.Log;

/**
 * Text-to-Speech Manager for Egyptian Agent
 * Handles speaking functionality with Egyptian dialect support
 */
public class TTSManager {
    private static final String TAG = "TTSManager";
    private static TTSEngine ttsEngine;
    private static Context applicationContext;
    private static boolean isInitialized = false;
    private static boolean isSeniorMode = false;
    private static float speechRate = 1.0f;
    private static float pitch = 1.0f;
    private static float volume = 1.0f;

    /**
     * Initializes the TTS Manager
     * @param context Application context
     */
    public static void initialize(Context context) {
        applicationContext = context.getApplicationContext();

        ttsEngine = new TTSEngine();
        ttsEngine.initialize(context);

        isInitialized = true;
        Log.i(TAG, "TTS Manager initialized successfully");
    }

    /**
     * Speaks the given text
     * @param context Context for the operation
     * @param text Text to speak
     */
    public static void speak(Context context, String text) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, skipping: " + text);
            return;
        }

        // Apply Egyptian dialect transformations if needed
        String processedText = applyEgyptianTransformations(text);

        // Create speech parameters
        TTSEngine.SpeechParams params = new TTSEngine.SpeechParams();
        params.setRate(speechRate);
        params.setPitch(pitch);
        params.setVolume(isSeniorMode ? volume * 1.5f : volume);

        // Speak the text
        ttsEngine.speak(processedText, params, new TTSEngine.SpeechCallback() {
            @Override
            public void onCompleted() {
                Log.d(TAG, "TTS completed: " + processedText);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "TTS error: " + errorMessage);
            }
        });
    }

    /**
     * Applies Egyptian dialect transformations to the text
     * @param text Original text
     * @return Processed text with Egyptian dialect considerations
     */
    private static String applyEgyptianTransformations(String text) {
        String transformedText = text;

        // Example transformations (these would be more extensive in reality):
        // Replace formal Arabic words with Egyptian colloquial equivalents
        // This is a simplified example - a real implementation would be more comprehensive
        transformedText = transformedText.replace("السيد", "العم");
        transformedText = transformedText.replace("السيدة", "الست");
        transformedText = transformedText.replace("الرجل", "الراجل");

        // Additional Egyptian dialect transformations
        transformedText = transformedText.replace("أنا", "انا");
        transformedText = transformedText.replace("إلى", "لـ");
        transformedText = transformedText.replace("في", "فـ");
        transformedText = transformedText.replace("أن", "إن");

        // Normalize common Egyptian expressions
        transformedText = transformedText.replace("إزاى", "إزي");
        transformedText = transformedText.replace("أزاى", "أزي");
        transformedText = transformedText.replace("فين", "mana");

        return transformedText;
    }

    /**
     * Applies more comprehensive Egyptian dialect transformations
     * @param text Original text
     * @return Processed text with Egyptian dialect considerations
     */
    public static String applyComprehensiveEgyptianTransformations(String text) {
        String transformedText = applyEgyptianTransformations(text);

        // Additional transformations specific to Egyptian dialect
        // These might include phonetic adaptations, common expressions, etc.

        // Common Egyptian expressions
        transformedText = transformedText.replace("أهلاً وسهلاً", "اهلا وسهلا");
        transformedText = transformedText.replace("شكراً", "شكرا");
        transformedText = transformedText.replace("من فضلك", "من فضلك ياريت");
        transformedText = transformedText.replace("عذراً", "آسف");

        // More phonetic adaptations
        transformedText = transformedText.replace("محمد", "محمود"); // Common mispronunciation adaptation
        transformedText = transformedText.replace("الUniversity", "الجامعة"); // Adapt foreign terms

        // Additional Egyptian dialect transformations
        // Verb conjugations and informal expressions
        transformedText = transformedText.replace("أريد", "عايز");
        transformedText = transformedText.replace("أريدي", "عايزة"); // Feminine form
        transformedText = transformedText.replace("يريد", "عايز");
        transformedText = transformedText.replace("تريد", "عايزة");
        transformedText = transformedText.replace("أحب", "بحب");
        transformedText = transformedText.replace("أكره", "مبيحبش");
        transformedText = transformedText.replace("لا أعرف", "ماعرفش");
        transformedText = transformedText.replace("لا أستطيع", "ماقدرش");
        transformedText = transformedText.replace("متى", "إمتى");
        transformedText = transformedText.replace("أين", "فين");
        transformedText = transformedText.replace("لماذا", "ليه");
        transformedText = transformedText.replace("الآن", "دلوقتي");
        transformedText = transformedText.replace("غداً", "بكرة");
        transformedText = transformedText.replace("البارحة", "النهاردة");
        transformedText = transformedText.replace("البارحة", "امبارح");

        // Common Egyptian colloquial phrases
        transformedText = transformedText.replace("كيف الحال", "إزيك");
        transformedText = transformedText.replace("كيف حالك", "إزيك");
        transformedText = transformedText.replace("كيف حالها", "إزيها");
        transformedText = transformedText.replace("كيف حاله", "إزيه");
        transformedText = transformedText.replace("هل تسمعني", "الله يسمعك");
        transformedText = transformedText.replace("أراك لاحقاً", "بلاش");
        transformedText = transformedText.replace("لا بأس", "أكيد");
        transformedText = transformedText.replace("ربما", "ممكن");

        return transformedText;
    }

    /**
     * Sets speech rate for TTS
     * @param context Context for the operation
     * @param rate The speech rate (1.0 = normal, <1.0 = slower, >1.0 = faster)
     */
    public static void setSpeechRate(Context context, float rate) {
        speechRate = rate;
        if (ttsEngine != null && isInitialized) {
            // We'll apply this rate in the speak method
        }
        Log.d(TAG, "Speech rate set to: " + rate);
    }

    /**
     * Sets pitch for TTS
     * @param context Context for the operation
     * @param p The pitch (1.0 = normal, <1.0 = lower, >1.0 = higher)
     */
    public static void setPitch(Context context, float p) {
        pitch = p;
        if (ttsEngine != null && isInitialized) {
            // We'll apply this pitch in the speak method
        }
        Log.d(TAG, "Pitch set to: " + p);
    }

    /**
     * Sets volume for TTS
     * @param context Context for the operation
     * @param vol The volume (1.0 = normal, <1.0 = quieter, >1.0 = louder)
     */
    public static void setVolume(Context context, float vol) {
        volume = vol;
        Log.d(TAG, "Volume set to: " + vol);
    }

    /**
     * Sets senior mode settings for TTS
     * @param context Context for the operation
     */
    public static void setSeniorSettings(Context context) {
        isSeniorMode = true;
        speechRate = 0.8f; // Slower speech
        volume = 1.2f;     // Louder volume
        Log.d(TAG, "Senior mode TTS settings applied");

        // Apply settings to current TTS engine if available
        if (ttsEngine != null && isInitialized) {
            ttsEngine.setVoiceType(TTSEngine.VoiceType.SENIOR);
        }
    }

    /**
     * Resets to normal TTS settings
     */
    public static void resetNormalSettings() {
        isSeniorMode = false;
        speechRate = 1.0f;
        pitch = 1.0f;
        volume = 1.0f;
        Log.d(TAG, "Normal TTS settings restored");

        // Apply settings to current TTS engine if available
        if (ttsEngine != null && isInitialized) {
            ttsEngine.setVoiceType(TTSEngine.VoiceType.NORMAL);
        }
    }

    /**
     * Stops current TTS playback
     */
    public static void stop() {
        if (ttsEngine != null && isInitialized) {
            ttsEngine.stopSpeaking();
        }
    }

    /**
     * Shuts down the TTS engine
     */
    public static void shutdown() {
        if (ttsEngine != null) {
            // No explicit shutdown method in TTSEngine interface
            isInitialized = false;
        }
    }

    /**
     * Checks if TTS is initialized
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Gets the current senior mode status
     * @return true if senior mode is active, false otherwise
     */
    public static boolean isSeniorMode() {
        return isSeniorMode;
    }
}