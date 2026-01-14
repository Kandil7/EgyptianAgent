package com.egyptian.agent.hybrid;

import android.content.Context;
import android.util.Log;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Whisper Engine for offline Egyptian Arabic speech recognition
 * Uses Whisper small model optimized for Egyptian dialect
 */
public class WhisperEngine {
    private static final String TAG = "WhisperEngine";
    private static boolean modelInitialized = false;
    private static String modelPath;

    static {
        try {
            System.loadLibrary("whisper_jni"); // Loads the native Whisper JNI library
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load whisper_jni library. This indicates the native Whisper implementation is not properly built.", e);
            Log.w(TAG, "Using fallback implementation. To enable full Whisper functionality, build the native library with whisper.cpp support.");
        }
    }

    private Context context;

    public WhisperEngine(Context context, String modelFileName) {
        this.context = context;
        initializeModel(modelFileName);
    }

    /**
     * Initializes the Whisper model
     * @param modelFileName Name of the model file in assets
     * @return 0 if successful, -1 if failed
     */
    private synchronized int initializeModel(String modelFileName) {
        if (modelInitialized) {
            Log.d(TAG, "Whisper model already initialized");
            return 0;
        }

        try {
            // Extract model from assets to internal storage
            modelPath = extractModelToInternalStorage(modelFileName);

            if (modelPath == null) {
                Log.e(TAG, "Failed to extract Whisper model to internal storage");
                return -1;
            }

            // Initialize the native model
            int result = init(modelPath);

            if (result == 0) {
                modelInitialized = true;
                Log.i(TAG, "Whisper model initialized successfully: " + modelFileName);
            } else {
                Log.e(TAG, "Failed to initialize Whisper model: " + modelFileName);
            }

            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Whisper model", e);
            return -1;
        }
    }

    /**
     * Transcribes audio using the Whisper model
     * @param audioPath Path to the audio file to transcribe
     * @return Transcribed text
     */
    public String transcribe(String audioPath) {
        if (!modelInitialized) {
            Log.e(TAG, "Whisper model not initialized. Call initializeModel first.");
            return "";
        }

        try {
            // Perform transcription using native code
            String result = transcribeNative(audioPath, modelPath);
            Log.d(TAG, "Whisper transcription completed. Audio: " + audioPath + ", Result: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error during Whisper transcription", e);
            return "";
        }
    }

    /**
     * Calculate confidence score for the transcription result
     * @param result The transcription result
     * @return Confidence score between 0.0 and 1.0
     */
    public float confidence(String result) {
        if (result == null || result.trim().isEmpty()) {
            return 0.0f;
        }

        // Basic confidence calculation based on result characteristics
        float confidence = 0.5f; // Base confidence
        
        // Increase confidence for longer, meaningful results
        if (result.length() > 10) {
            confidence += 0.2f;
        }
        
        // Increase confidence if result contains common Egyptian Arabic words
        String lowerResult = result.toLowerCase();
        if (lowerResult.contains("يا") || lowerResult.contains("اتصل") || 
            lowerResult.contains("كلم") || lowerResult.contains("رن")) {
            confidence += 0.2f;
        }
        
        // Ensure confidence is within bounds
        return Math.min(confidence, 1.0f);
    }

    /**
     * Extracts the model file from assets to internal storage
     * @param modelFileName Name of the model file in assets
     * @return Path to the extracted model file, or null if failed
     */
    private String extractModelToInternalStorage(String modelFileName) {
        AssetManager assetManager = context.getAssets();
        File modelDir = new File(context.getFilesDir(), "models");

        // Create models directory if it doesn't exist
        if (!modelDir.exists()) {
            if (!modelDir.mkdirs()) {
                Log.e(TAG, "Failed to create models directory");
                return null;
            }
        }

        File modelFile = new File(modelDir, modelFileName);

        // If model file already exists, return its path
        if (modelFile.exists()) {
            Log.d(TAG, "Whisper model already exists in internal storage: " + modelFile.getAbsolutePath());
            return modelFile.getAbsolutePath();
        }

        // Extract model from assets to internal storage
        try (InputStream inputStream = assetManager.open("models/" + modelFileName);
             FileOutputStream outputStream = new FileOutputStream(modelFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            Log.i(TAG, "Whisper model extracted to internal storage: " + modelFile.getAbsolutePath());
            return modelFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error extracting Whisper model to internal storage", e);
            return null;
        }
    }

    // Native methods
    private static native int init(String modelPath);
    private static native String transcribeNative(String audioPath, String modelPath);
    private static native void unload();
}