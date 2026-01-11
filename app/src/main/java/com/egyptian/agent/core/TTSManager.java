package com.egyptian.agent.core;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.utils.CrashLogger;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TTSManager {

    private static final String TAG = "TTSManager";
    private static TextToSpeech tts;
    private static boolean isInitialized = false;
    private static boolean isSpeaking = false;
    private static final Object ttsLock = new Object();

    // Senior mode settings
    private static float seniorSpeechRate = 0.75f;
    private static float seniorPitch = 0.9f;
    private static float seniorVolume = 1.0f;

    // Normal mode settings
    private static float normalSpeechRate = 1.0f;
    private static float normalPitch = 1.0f;
    private static float normalVolume = 0.9f;

    // Thread pool for TTS operations
    private static final ExecutorService ttsExecutor = Executors.newSingleThreadExecutor();

    /**
     * Initialize TTS engine
     */
    public static void initialize(Context context) {
        if (tts != null) {
            return;
        }

        Log.i(TAG, "Initializing TTS engine");

        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("ar", "EG"));

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Arabic language not supported");
                    CrashLogger.logError(context, new Exception("Arabic TTS not supported"));
                } else {
                    // Set default parameters
                    tts.setSpeechRate(normalSpeechRate);
                    tts.setPitch(normalPitch);
                    isInitialized = true;
                    Log.i(TAG, "TTS engine initialized successfully");
                }
            } else {
                Log.e(TAG, "TTS initialization failed");
                CrashLogger.logError(context, new Exception("TTS initialization failed"));
            }
        });

        // Set up utterance progress listener
        setupUtteranceListener();

        // Set audio attributes for better handling on Honor devices
        setupAudioAttributes();
    }

    private static void setupUtteranceListener() {
        if (tts == null) return;

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                isSpeaking = true;
                Log.d(TAG, "TTS started speaking: " + utteranceId);
            }

            @Override
            public void onDone(String utteranceId) {
                isSpeaking = false;
                Log.d(TAG, "TTS finished speaking: " + utteranceId);
            }

            @Override
            @Deprecated
            public void onError(String utteranceId) {
                isSpeaking = false;
                Log.e(TAG, "TTS error on utterance: " + utteranceId);
            }

            @Override
            public void onError(String utteranceId, int errorCode) {
                isSpeaking = false;
                Log.e(TAG, "TTS error on utterance: " + utteranceId + ", code: " + errorCode);
            }
        });
    }

    private static void setupAudioAttributes() {
        if (tts == null) return;

        try {
            // Set audio attributes to ensure TTS works properly on Honor devices
            Bundle params = new Bundle();
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
            // Note: AudioAttributes builder is not available in older Android versions
            // This is a simplified version for compatibility
        } catch (Exception e) {
            Log.w(TAG, "Failed to set audio attributes", e);
        }
    }

    /**
     * Speak text with default parameters
     */
    public static void speak(Context context, String text) {
        speak(context, text, null);
    }

    /**
     * Speak text with custom parameters
     */
    public static void speak(Context context, String text, Bundle params) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        // Initialize TTS if needed
        if (!isInitialized) {
            initialize(context);
        }

        // Use senior mode settings if enabled
        Bundle ttsParams = params != null ? params : new Bundle();
        applyCurrentModeSettings(ttsParams);

        final String utteranceId = "utt_" + System.currentTimeMillis();

        Log.i(TAG, "Speaking: " + text);

        // Execute on background thread to avoid blocking UI
        ttsExecutor.execute(() -> {
            synchronized (ttsLock) {
                if (isSpeaking) {
                    // Wait for current speech to finish
                    try {
                        ttsLock.wait(3000); // Wait up to 3 seconds
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                // Add utterance ID to parameters
                HashMap<String, String> ttsParamsMap = new HashMap<>();
                ttsParamsMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);

                // Set volume if specified
                if (ttsParams.containsKey(TextToSpeech.Engine.KEY_PARAM_VOLUME)) {
                    float volume = ttsParams.getFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME);
                    ttsParamsMap.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, String.valueOf(volume));
                }

                // Set stream type if specified
                if (ttsParams.containsKey(TextToSpeech.Engine.KEY_PARAM_STREAM)) {
                    int stream = ttsParams.getInt(TextToSpeech.Engine.KEY_PARAM_STREAM);
                    ttsParamsMap.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(stream));
                }

                // Speak with parameters
                int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, ttsParamsMap, utteranceId);

                if (result == TextToSpeech.ERROR) {
                    Log.e(TAG, "TTS speak failed for text: " + text);
                    CrashLogger.logError(context, new Exception("TTS speak failed"));
                }
            }
        });
    }

    /**
     * Speak with high priority (emergency situations)
     */
    public static void speakWithPriority(Context context, String text, boolean priority) {
        Bundle params = new Bundle();

        if (priority) {
            // Maximum volume and priority
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM);
        }

        speak(context, text, params);
    }

    /**
     * Apply current mode settings (normal or senior) to TTS parameters
     */
    private static void applyCurrentModeSettings(Bundle params) {
        float speechRate, pitch, volume;

        if (SeniorMode.isEnabled()) {
            speechRate = seniorSpeechRate;
            pitch = seniorPitch;
            volume = seniorVolume;
        } else {
            speechRate = normalSpeechRate;
            pitch = normalPitch;
            volume = normalVolume;
        }

        // Apply settings
        if (tts != null) {
            tts.setSpeechRate(speechRate);
            tts.setPitch(pitch);
        }

        // Volume needs to be handled differently
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume);
    }

    /**
     * Check if TTS is currently speaking
     */
    public static boolean isSpeaking() {
        return isSpeaking;
    }

    /**
     * Stop current speech
     */
    public static void stopSpeaking() {
        if (tts != null && isSpeaking) {
            tts.stop();
            isSpeaking = false;
            Log.i(TAG, "TTS stopped speaking");
        }
    }

    /**
     * Set speech rate
     */
    public static void setSpeechRate(Context context, float rate) {
        if (tts != null) {
            tts.setSpeechRate(rate);
            Log.d(TAG, "TTS speech rate set to: " + rate);

            if (SeniorMode.isEnabled()) {
                seniorSpeechRate = rate;
            } else {
                normalSpeechRate = rate;
            }
        } else {
            CrashLogger.logWarning(context, "TTS not initialized when setting speech rate");
        }
    }

    /**
     * Set pitch
     */
    public static void setPitch(Context context, float pitch) {
        if (tts != null) {
            tts.setPitch(pitch);
            Log.d(TAG, "TTS pitch set to: " + pitch);

            if (SeniorMode.isEnabled()) {
                seniorPitch = pitch;
            } else {
                normalPitch = pitch;
            }
        } else {
            CrashLogger.logWarning(context, "TTS not initialized when setting pitch");
        }
    }

    /**
     * Set volume
     */
    public static void setVolume(Context context, float volume) {
        if (SeniorMode.isEnabled()) {
            seniorVolume = volume;
        } else {
            normalVolume = volume;
        }
        Log.d(TAG, "TTS volume set to: " + volume);
    }

    /**
     * Special setup for senior mode
     */
    public static void setSeniorSettings(Context context) {
        Log.i(TAG, "Applying senior mode TTS settings");
        setSpeechRate(context, seniorSpeechRate);
        setPitch(context, seniorPitch);

        // Set maximum volume for seniors
        seniorVolume = 1.0f;

        // Special announcement for senior mode activation
        speak(context, "وضع كبار السن نشط. الصوت هيبقى أبطأ وأعلى عشان الوضوح");
    }

    /**
     * Release TTS resources
     */
    public static void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            isInitialized = false;
            isSpeaking = false;
            Log.i(TAG, "TTS engine shutdown");
        }

        // Shutdown executor
        ttsExecutor.shutdown();
    }
}