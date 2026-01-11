package com.egyptian.agent.core;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Whisper-based ASR engine for Egyptian dialect speech recognition
 * Uses llama.cpp for efficient inference on mobile devices
 */
public class WhisperASREngine {
    private static final String TAG = "WhisperASREngine";
    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = 16384; // Larger buffer for Whisper
    
    private final Context context;
    private final String modelPath;
    private final DeviceClassDetector.DeviceClass deviceClass;
    
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;
    private boolean isProcessing = false;
    
    private final ExecutorService inferenceExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService audioProcessingExecutor = Executors.newSingleThreadExecutor();
    
    private ASRCallback callback;
    
    public interface ASRCallback {
        void onResult(String text);
        void onError(Exception error);
    }
    
    public WhisperASREngine(Context context, String modelPath) {
        this.context = context;
        this.modelPath = modelPath;
        this.deviceClass = ((MainApplication) context.getApplicationContext()).getDeviceClass();
        
        Log.i(TAG, "Whisper ASR Engine initialized for device class: " + deviceClass.name() + 
              " with model: " + modelPath);
    }
    
    /**
     * Initializes the Whisper ASR engine
     */
    public void initialize() throws Exception {
        Log.i(TAG, "Initializing Whisper ASR engine with model: " + modelPath);
        
        // Verify model exists
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            throw new IOException("Whisper model not found: " + modelPath);
        }
        
        // Configure audio recording
        int minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        );
        
        int bufferSize = Math.max(minBufferSize, BUFFER_SIZE);
        
        audioRecord = new AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        );
        
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new Exception("Failed to initialize AudioRecord");
        }
        
        Log.i(TAG, "Whisper ASR engine initialized successfully");
    }
    
    /**
     * Starts listening for speech
     */
    public void startListening(ASRCallback callback) {
        if (isRecording) {
            Log.w(TAG, "Already recording, ignoring start request");
            return;
        }
        
        this.callback = callback;
        isRecording = true;
        
        try {
            audioRecord.startRecording();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start audio recording", e);
            if (callback != null) {
                callback.onError(e);
            }
            return;
        }
        
        recordingThread = new Thread(() -> {
            Log.i(TAG, "Starting audio recording thread");
            
            // Buffer to hold audio data
            short[] audioBuffer = new short[BUFFER_SIZE / 2]; // 16-bit samples
            
            while (isRecording) {
                try {
                    int numRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                    
                    if (numRead > 0 && !isProcessing) {
                        // Process the audio chunk in a separate thread to avoid blocking recording
                        final short[] chunk = new short[numRead];
                        System.arraycopy(audioBuffer, 0, chunk, 0, numRead);
                        
                        isProcessing = true;
                        
                        audioProcessingExecutor.submit(() -> {
                            try {
                                // Check if this audio chunk has enough energy to warrant processing
                                if (hasSignificantEnergy(chunk)) {
                                    // Process the audio chunk for speech recognition
                                    processAudioChunk(chunk);
                                }
                            } finally {
                                isProcessing = false;
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error during audio recording", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            }
            
            Log.i(TAG, "Audio recording thread ending");
        });
        
        recordingThread.start();
        Log.i(TAG, "Started listening for speech");
    }
    
    /**
     * Checks if the audio chunk has significant energy (not just noise)
     */
    private boolean hasSignificantEnergy(short[] audioData) {
        long sum = 0;
        for (short sample : audioData) {
            sum += sample * sample; // Square to get energy
        }
        double rms = Math.sqrt((double) sum / audioData.length);
        return rms > 500; // Threshold for significant audio
    }
    
    /**
     * Processes an audio chunk through Whisper for speech recognition
     */
    private void processAudioChunk(short[] audioData) {
        // Submit inference task to executor
        Future<?> inferenceTask = inferenceExecutor.submit(() -> {
            try {
                Log.d(TAG, "Starting Whisper inference for audio chunk of " + audioData.length + " samples");
                
                // Convert short array to float array for Whisper processing
                float[] floatAudioData = new float[audioData.length];
                for (int i = 0; i < audioData.length; i++) {
                    floatAudioData[i] = audioData[i] / 32768.0f; // Normalize to [-1, 1]
                }
                
                // Perform Whisper transcription
                String result = transcribeWithWhisper(floatAudioData);
                
                if (result != null && !result.trim().isEmpty()) {
                    Log.i(TAG, "Whisper transcription result: " + result);
                    
                    // Apply Egyptian dialect normalization
                    String normalizedResult = com.egyptian.agent.stt.EgyptianNormalizer.normalize(result);
                    
                    if (callback != null) {
                        callback.onResult(normalizedResult);
                    }
                } else {
                    Log.d(TAG, "Whisper returned empty result");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during Whisper inference", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * Transcribes audio using Whisper model
     * This is a placeholder implementation - actual implementation would interface with llama.cpp
     */
    private String transcribeWithWhisper(float[] audioData) {
        // In a real implementation, this would call the Whisper JNI interface
        // For now, we'll simulate the transcription
        
        Log.d(TAG, "Transcribing " + audioData.length + " audio samples with Whisper");
        
        // Simulate processing delay based on device class
        try {
            switch (deviceClass) {
                case LOW:
                    Thread.sleep(2000); // 2 seconds for low-end devices
                    break;
                case MID:
                    Thread.sleep(1200); // 1.2 seconds for mid-range devices
                    break;
                case HIGH:
                case ELITE:
                    Thread.sleep(800); // 0.8 seconds for high-end devices
                    break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // This is where the actual Whisper transcription would happen
        // For now, return a simulated result
        return simulateWhisperTranscription(audioData);
    }
    
    /**
     * Simulates Whisper transcription for demonstration purposes
     */
    private String simulateWhisperTranscription(float[] audioData) {
        // In a real implementation, this would call the actual Whisper model
        // For simulation, we'll return a placeholder based on some characteristics of the audio
        
        // Just for simulation purposes, return some common Egyptian phrases
        // based on the length and characteristics of the audio
        if (audioData.length > 10000) {
            return "يا كبير اتصل بأمي";
        } else if (audioData.length > 5000) {
            return "بدي أكلم بابا";
        } else {
            return "أنا هنا";
        }
    }
    
    /**
     * Stops listening for speech
     */
    public void stopListening() {
        Log.i(TAG, "Stopping Whisper ASR engine");
        
        isRecording = false;
        
        try {
            if (recordingThread != null && recordingThread.isAlive()) {
                recordingThread.interrupt();
                recordingThread.join(2000); // Wait up to 2 seconds
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        try {
            if (audioRecord != null) {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
                audioRecord = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error releasing audio recorder", e);
        }
        
        Log.i(TAG, "Whisper ASR engine stopped");
    }
    
    /**
     * Cleans up resources
     */
    public void destroy() {
        stopListening();
        
        inferenceExecutor.shutdown();
        audioProcessingExecutor.shutdown();
        
        Log.i(TAG, "Whisper ASR engine destroyed");
    }
}