package com.egyptian.agent.core;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.egyptian.agent.stt.VoskSTTEngine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wake Word Detector
 * Detects wake words to activate the voice assistant
 * Supports both "يا صاحبي" and "يا كبير" for Egyptian dialect
 */
public class WakeWordDetector {
    private static final String TAG = "WakeWordDetector";
    
    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(
        SAMPLE_RATE, 
        AudioFormat.CHANNEL_IN_MONO, 
        AudioFormat.ENCODING_PCM_16BIT
    );
    
    private Context context;
    private ExecutorService executorService;
    private boolean isListening = false;
    private AudioRecord audioRecord;
    private byte[] buffer;
    private WakeWordCallback callback;
    private VoskSTTEngine sttEngine;
    
    public WakeWordDetector(Context context, WakeWordCallback callback) {
        this.context = context;
        this.callback = callback;
        this.executorService = Executors.newSingleThreadExecutor();
        this.buffer = new byte[BUFFER_SIZE];
        
        // Initialize STT engine for wake word detection
        initializeSTTEngine();
    }
    
    /**
     * Initializes the STT engine for wake word detection
     */
    private void initializeSTTEngine() {
        try {
            // Use a small model for wake word detection to save resources
            sttEngine = new VoskSTTEngine(context, "models/vosk-model-small-ar-wake.zip");
            Log.i(TAG, "Wake word detector initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing STT engine for wake word detection", e);
        }
    }
    
    /**
     * Starts listening for wake words
     */
    public void startListening() {
        if (isListening) {
            Log.w(TAG, "Already listening for wake words");
            return;
        }
        
        isListening = true;
        
        executorService.execute(() -> {
            try {
                // Initialize AudioRecord
                audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    BUFFER_SIZE
                );
                
                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "Failed to initialize AudioRecord");
                    return;
                }
                
                audioRecord.startRecording();
                
                Log.d(TAG, "Started listening for wake words");
                
                while (isListening) {
                    int bytesRead = audioRecord.read(buffer, 0, BUFFER_SIZE);
                    
                    if (bytesRead > 0) {
                        // Process the audio buffer for wake word detection
                        if (detectWakeWord(buffer, bytesRead)) {
                            Log.i(TAG, "Wake word detected!");
                            
                            // Stop recording temporarily to avoid multiple detections
                            audioRecord.stop();
                            
                            // Notify the callback
                            if (callback != null) {
                                callback.onWakeWordDetected();
                            }
                            
                            // Wait a bit before resuming to avoid immediate re-detection
                            Thread.sleep(2000);
                            
                            // Resume recording
                            audioRecord.startRecording();
                        }
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error during wake word detection", e);
            } finally {
                if (audioRecord != null) {
                    audioRecord.stop();
                    audioRecord.release();
                }
            }
        });
    }
    
    /**
     * Stops listening for wake words
     */
    public void stopListening() {
        isListening = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
    }
    
    /**
     * Restarts listening for wake words
     */
    public void restartListening() {
        stopListening();
        startListening();
    }
    
    /**
     * Detects wake words in the audio buffer
     * @param audioBuffer The audio buffer to analyze
     * @param bufferSize The size of the buffer
     * @return true if wake word detected, false otherwise
     */
    private boolean detectWakeWord(byte[] audioBuffer, int bufferSize) {
        // For now, we'll simulate the detection

        // Check if the STT engine is available
        if (sttEngine != null && sttEngine.isInitialized()) {
            // Pass the audio buffer to the STT engine
            // and check if it recognizes the wake words "يا صاحبي" or "يا كبير"
            return detectWakeWordsInAudio(audioBuffer, bufferSize);
        }

        // If no STT engine, return false
        return false;
    }

    /**
     * Detects wake words in the audio buffer
     * @param audioBuffer The audio buffer to analyze
     * @param bufferSize The size of the buffer
     * @return true if wake word detected, false otherwise
     */
    private boolean detectWakeWordsInAudio(byte[] audioBuffer, int bufferSize) {
        // Use the STT engine to convert audio to text
        // and then check for wake words
        try {
            // Convert byte buffer to audio data that can be processed
            // This is a simplified approach - in reality, you'd need to properly decode the audio

            // Use a keyword spotting approach by converting to text first
            String recognizedText = sttEngine.recognizeAudio(audioBuffer, bufferSize);

            if (recognizedText != null) {
                // Check for wake words in the recognized text
                String lowerText = recognizedText.toLowerCase();

                // Check for "يا صاحبي" (ya sa7bi) or "يا كبير" (ya kabeer)
                return lowerText.contains("يا صاحبي") || lowerText.contains("يا كبير") ||
                       lowerText.contains("ya sa7bi") || lowerText.contains("ya kabeer") ||
                       lowerText.contains("ya7ya") || lowerText.contains("big friend");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during wake word detection", e);
        }

        return false;
    }

    /**
     * Destroys the wake word detector and cleans up resources
     */
    public void destroy() {
        stopListening();
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (sttEngine != null) {
            sttEngine.destroy();
        }
    }

    /**
     * Callback interface for wake word detection
     */
    public interface WakeWordCallback {
        void onWakeWordDetected();
    }
}