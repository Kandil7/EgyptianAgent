package com.egyptian.agent.core;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.utils.CrashLogger;

import java.util.HashMap;
import java.util.Locale;

/**
 * Enhanced TTS Engine implementation for the Egyptian Agent
 */
public class TTSEngine implements TTSEngineInterface {
    private static final String TAG = "TTSEngine";
    private TextToSpeech textToSpeech;
    private boolean isInitialized = false;
    private Context context;
    private VoiceType currentVoiceType = VoiceType.NORMAL;

    public enum VoiceType {
        NORMAL, SENIOR
    }

    public TTSEngine() {
        // Default constructor
    }

    @Override
    public void initialize(Context context) {
        this.context = context;
        textToSpeech = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("ar", "EG"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "This Language is not supported");
                    // Fallback to default language
                    textToSpeech.setLanguage(Locale.getDefault());
                } else {
                    isInitialized = true;
                    Log.d(TAG, "TTS Engine initialized successfully");
                    
                    // Set default parameters
                    setLanguage("ar-EG");
                    setVoiceType(currentVoiceType);
                }
            } else {
                Log.e(TAG, "Initialization Failed!");
            }
        });
    }

    @Override
    public void speak(String text, SpeechParams params, SpeechCallback callback) {
        if (!isInitialized || text == null || text.isEmpty()) {
            if (callback != null) {
                callback.onError("TTS not initialized or text is empty");
            }
            return;
        }

        // Set parameters if provided
        if (params != null) {
            textToSpeech.setSpeechRate(params.getRate());
            textToSpeech.setPitch(params.getPitch());
        }

        // Create utterance ID for callbacks
        String utteranceId = "utterance_" + System.currentTimeMillis();

        // Set up progress listener if callback is provided
        if (callback != null) {
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    // Started speaking
                }

                @Override
                public void onDone(String utteranceId) {
                    callback.onCompleted();
                }

                @Override
                public void onError(String utteranceId) {
                    callback.onError("Error occurred while speaking");
                }

                @Override
                public void onError(String utteranceId, int errorCode) {
                    callback.onError("Error occurred while speaking: " + errorCode);
                }
            });
        }

        // Speak the text
        HashMap<String, String> paramsMap = new HashMap<>();
        paramsMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
        if (params != null) {
            paramsMap.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, String.valueOf(params.getVolume()));
        }

        int result = textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, paramsMap, utteranceId);
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "Error occurred while speaking");
            if (callback != null) {
                callback.onError("Error occurred while speaking");
            }
        }
    }

    @Override
    public void stopSpeaking() {
        if (isInitialized && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
    }

    @Override
    public boolean isSpeaking() {
        return isInitialized && textToSpeech.isSpeaking();
    }

    @Override
    public void setLanguage(String languageCode) {
        if (isInitialized) {
            Locale locale = new Locale(languageCode.substring(0, 2), languageCode.substring(3));
            textToSpeech.setLanguage(locale);
        }
    }

    @Override
    public void setVoiceType(VoiceType voiceType) {
        this.currentVoiceType = voiceType;
        if (isInitialized) {
            // Adjust parameters based on voice type
            if (voiceType == VoiceType.SENIOR) {
                // For seniors: slower speech, higher volume
                textToSpeech.setSpeechRate(0.8f); // Slower
            } else {
                // For normal users: standard speed
                textToSpeech.setSpeechRate(1.0f);
            }
        }
    }
}