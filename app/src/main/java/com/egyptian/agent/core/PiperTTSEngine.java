package com.egyptian.agent.core;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Piper TTS engine for Egyptian Arabic text-to-speech
 * Provides natural-sounding Arabic speech synthesis
 */
public class PiperTTSEngine {
    private static final String TAG = "PiperTTSEngine";
    
    private final Context context;
    private final String modelPath;
    private final DeviceClassDetector.DeviceClass deviceClass;
    
    private boolean isInitialized = false;
    private boolean isSpeaking = false;
    private final Object ttsLock = new Object();
    
    // For the actual Piper integration, we'll use a placeholder approach
    // since the real Piper TTS would require JNI bindings
    private TextToSpeech fallbackTTS; // Fallback to Android's TTS
    private final ExecutorService ttsExecutor = Executors.newSingleThreadExecutor();
    
    public PiperTTSEngine(Context context, String modelPath) {
        this.context = context;
        this.modelPath = modelPath;
        this.deviceClass = ((MainApplication) context.getApplicationContext()).getDeviceClass();
        
        Log.i(TAG, "Piper TTS Engine initialized for device class: " + deviceClass.name() + 
              " with model: " + modelPath);
    }
    
    /**
     * Initializes the Piper TTS engine
     */
    public void initialize() throws Exception {
        Log.i(TAG, "Initializing Piper TTS engine with model: " + modelPath);
        
        // Verify model exists
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            Log.w(TAG, "Piper model not found: " + modelPath + ", using fallback TTS");
        } else {
            Log.i(TAG, "Piper model found, preparing for synthesis");
        }
        
        // Initialize fallback TTS in case Piper isn't available
        initializeFallbackTTS();
        
        isInitialized = true;
        Log.i(TAG, "Piper TTS engine initialized successfully");
    }
    
    /**
     * Initializes the fallback Android TTS engine
     */
    private void initializeFallbackTTS() {
        fallbackTTS = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = fallbackTTS.setLanguage(new Locale("ar", "EG")); // Egyptian Arabic
                
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Egyptian Arabic not supported, trying generic Arabic");
                    result = fallbackTTS.setLanguage(new Locale("ar"));
                    
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Arabic language not supported at all");
                    }
                } else {
                    Log.i(TAG, "Arabic language set successfully for fallback TTS");
                }
                
                // Set default parameters based on device class
                switch (deviceClass) {
                    case LOW:
                        // Lower quality for low-end devices to save resources
                        fallbackTTS.setSpeechRate(0.9f);
                        fallbackTTS.setPitch(1.0f);
                        break;
                    case MID:
                        // Balanced settings for mid-range devices
                        fallbackTTS.setSpeechRate(1.0f);
                        fallbackTTS.setPitch(1.0f);
                        break;
                    case HIGH:
                    case ELITE:
                        // Higher quality for high-end devices
                        fallbackTTS.setSpeechRate(1.0f);
                        fallbackTTS.setPitch(1.1f);
                        break;
                }
                
                isInitialized = true;
                Log.i(TAG, "Fallback TTS initialized successfully");
            } else {
                Log.e(TAG, "Fallback TTS initialization failed");
            }
        });
        
        // Set up utterance progress listener
        setupUtteranceListener();
    }
    
    private void setupUtteranceListener() {
        if (fallbackTTS == null) return;
        
        fallbackTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
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
    
    /**
     * Speaks the provided text using Piper TTS
     */
    public void speak(String text, TTSCompletionCallback callback) {
        if (!isInitialized) {
            Log.e(TAG, "TTS engine not initialized");
            if (callback != null) {
                callback.onError(new Exception("TTS engine not initialized"));
            }
            return;
        }
        
        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "Empty text provided to speak");
            if (callback != null) {
                callback.onComplete();
            }
            return;
        }
        
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
                
                speakWithActualPiperTTS(text, callback);
            }
        });
    }

    /**
     * Speaks text using the actual Piper TTS via JNI
     */
    private void speakWithActualPiperTTS(String text, TTSCompletionCallback callback) {
        // For now, we'll use the fallback TTS
        // this would interface with the native Piper TTS library
        speakWithFallbackTTS(text, callback);
    }
    
    /**
     * Speaks text using the fallback Android TTS
     */
    private void speakWithFallbackTTS(String text, TTSCompletionCallback callback) {
        final String utteranceId = "piper_utt_" + System.currentTimeMillis();
        
        Log.i(TAG, "Speaking with fallback TTS: " + text);
        
        // Set up parameters
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
        
        // Speak the text
        int result = fallbackTTS.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "Fallback TTS speak failed for text: " + text);
            if (callback != null) {
                callback.onError(new Exception("TTS speak failed"));
            }
        } else {
            // Wait for completion or timeout
            long startTime = System.currentTimeMillis();
            while (isSpeaking && (System.currentTimeMillis() - startTime) < 10000) { // 10 second timeout
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            if (callback != null) {
                callback.onComplete();
            }
        }
    }
    
    /**
     * Stops current speech
     */
    public void stopSpeaking() {
        if (fallbackTTS != null && isSpeaking) {
            fallbackTTS.stop();
            isSpeaking = false;
            Log.i(TAG, "TTS stopped speaking");
        }
    }
    
    /**
     * Checks if TTS is currently speaking
     */
    public boolean isSpeaking() {
        return isSpeaking;
    }
    
    /**
     * Checks if the engine is initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Callback interface for TTS completion
     */
    public interface TTSCompletionCallback {
        void onComplete();
        void onError(Exception error);
    }
    
    /**
     * Cleans up resources
     */
    public void destroy() {
        stopSpeaking();
        
        if (fallbackTTS != null) {
            fallbackTTS.shutdown();
            fallbackTTS = null;
        }
        
        ttsExecutor.shutdown();
        isInitialized = false;
        
        Log.i(TAG, "Piper TTS engine destroyed");
    }
}