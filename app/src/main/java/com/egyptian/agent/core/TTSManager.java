package com.egyptian.agent.core;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.os.Build;
import java.util.Locale;
import java.util.HashMap;
import java.util.Random;

/**
 * Text-to-Speech Manager for Egyptian Agent
 * Handles speaking functionality with Egyptian dialect support
 */
public class TTSManager {
    private static final String TAG = "TTSManager";
    private static TextToSpeech textToSpeech;
    private static Context applicationContext;
    private static boolean isInitialized = false;
    private static boolean isSeniorMode = false;
    private static float speechRate = 1.0f;
    private static float volume = 1.0f;

    /**
     * Initializes the TTS Manager
     * @param context Application context
     */
    public static void initialize(Context context) {
        applicationContext = context.getApplicationContext();
        
        textToSpeech = new TextToSpeech(applicationContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // Set language to Arabic
                    int result = textToSpeech.setLanguage(new Locale("ar"));
                    
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "This Language is not supported");
                        // Fallback to English if Arabic is not available
                        textToSpeech.setLanguage(Locale.ENGLISH);
                    } else {
                        Log.i(TAG, "TTS Initialized successfully with Arabic language");
                    }
                    
                    // Set default speech rate and pitch
                    textToSpeech.setSpeechRate(speechRate);
                    textToSpeech.setPitch(1.0f);
                    
                    isInitialized = true;
                } else {
                    Log.e(TAG, "TTS Initialization failed");
                }
            }
        });

        // Set up utterance progress listener
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Log.d(TAG, "TTS started: " + utteranceId);
            }

            @Override
            public void onDone(String utteranceId) {
                Log.d(TAG, "TTS completed: " + utteranceId);
            }

            @Override
            public void onError(String utteranceId) {
                Log.e(TAG, "TTS error: " + utteranceId);
            }
        });
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

        // Generate unique utterance ID
        String utteranceId = String.valueOf(System.currentTimeMillis());

        // Speak the text
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);

        // Apply senior mode settings if enabled
        if (isSeniorMode) {
            textToSpeech.setSpeechRate(0.8f); // Slower speech
            textToSpeech.setVolume(volume * 1.5f); // Louder volume
        } else {
            textToSpeech.setSpeechRate(speechRate);
            textToSpeech.setVolume(volume);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech.speak(processedText, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        } else {
            textToSpeech.speak(processedText, TextToSpeech.QUEUE_FLUSH, params);
        }
    }

    /**
     * Applies Egyptian dialect transformations to the text
     * @param text Original text
     * @return Processed text with Egyptian dialect considerations
     */
    private static String applyEgyptianTransformations(String text) {
        // In a real implementation, this would apply transformations
        // specific to Egyptian Arabic pronunciation
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

        return transformedText;
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
    }

    /**
     * Resets to normal TTS settings
     */
    public static void resetNormalSettings() {
        isSeniorMode = false;
        speechRate = 1.0f;
        volume = 1.0f;
        Log.d(TAG, "Normal TTS settings restored");
    }

    /**
     * Stops current TTS playback
     */
    public static void stop() {
        if (textToSpeech != null && isInitialized) {
            textToSpeech.stop();
        }
    }

    /**
     * Shuts down the TTS engine
     */
    public static void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.shutdown();
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