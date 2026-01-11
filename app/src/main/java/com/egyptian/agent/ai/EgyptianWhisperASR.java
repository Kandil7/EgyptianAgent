package com.egyptian.agent.ai;

import android.content.Context;
import android.util.Log;
import java.io.File;

/**
 * Egyptian Whisper ASR Integration
 * Handles speech-to-text conversion specifically for Egyptian Arabic dialect
 */
public class EgyptianWhisperASR {
    private static final String TAG = "EgyptianWhisperASR";
    
    private Context context;
    private boolean isInitialized = false;
    private String modelPath;

    static {
        try {
            System.loadLibrary("egyptian_whisper"); // Loads the native Whisper library
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load egyptian_whisper native library", e);
        }
    }

    public EgyptianWhisperASR(Context context) {
        this.context = context;
        
        // Initialize Whisper ASR in the background
        new Thread(() -> {
            try {
                Log.i(TAG, "Initializing Whisper Egyptian ASR...");
                
                // Extract model from assets to internal storage
                modelPath = extractModelToInternalStorage();
                
                if (modelPath == null) {
                    Log.e(TAG, "Failed to extract Whisper model");
                    return;
                }
                
                // Initialize the native Whisper model
                int result = initWhisper(modelPath);
                
                if (result == 0) {
                    isInitialized = true;
                    Log.i(TAG, "Whisper Egyptian ASR initialized successfully");
                } else {
                    Log.e(TAG, "Failed to initialize Whisper ASR, result: " + result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Whisper ASR", e);
            }
        }).start();
    }

    /**
     * Transcribes Egyptian Arabic speech to text
     * @param audioPath Path to the audio file
     * @return Transcribed text
     */
    public String transcribe(String audioPath) {
        if (!isInitialized) {
            Log.e(TAG, "Whisper ASR not initialized. Call initialize first.");
            return "Whisper ASR not initialized";
        }

        try {
            // Perform transcription using native code
            String result = transcribeNative(audioPath);
            Log.d(TAG, "Whisper transcription completed: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error during Whisper transcription", e);
            return "Error during transcription: " + e.getMessage();
        }
    }

    /**
     * Extracts the Whisper model file from assets to internal storage
     * @return Path to the extracted model file, or null if failed
     */
    private String extractModelToInternalStorage() {
        // In a real implementation, this would extract the Whisper model
        // from assets to internal storage for faster access
        return extractWhisperModelFromAssets();
    }

    /**
     * Extracts the Whisper model from assets to internal storage
     */
    private String extractWhisperModelFromAssets() {
        File modelDir = new File(context.getFilesDir(), "models/whisper-egy");

        // Create models directory if it doesn't exist
        if (!modelDir.exists()) {
            if (!modelDir.mkdirs()) {
                Log.e(TAG, "Failed to create Whisper models directory");
                return null;
            }
        }

        // Define the model file name
        String modelFileName = "ggml-model-whisper.bin"; // Common Whisper model file name
        File modelFile = new File(modelDir, modelFileName);

        // Check if model already exists
        if (modelFile.exists()) {
            Log.d(TAG, "Whisper model already exists: " + modelFile.getAbsolutePath());
            return modelFile.getAbsolutePath();
        }

        // Extract model from assets
        try {
            java.io.InputStream inputStream = context.getAssets().open("models/whisper/" + modelFileName);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(modelFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            Log.d(TAG, "Whisper model extracted to: " + modelFile.getAbsolutePath());
            return modelFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error extracting Whisper model from assets", e);
            return null;
        }
    }

    /**
     * Checks if the ASR is ready for transcription
     * @return True if initialized, false otherwise
     */
    public boolean isReady() {
        return isInitialized;
    }

    /**
     * Cleans up resources
     */
    public void cleanup() {
        if (isInitialized) {
            unloadWhisper();
            isInitialized = false;
            Log.i(TAG, "Whisper ASR unloaded");
        }
    }

    // Native methods
    private static native int initWhisper(String modelPath);
    private static native String transcribeNative(String audioPath);
    private static native void unloadWhisper();
}