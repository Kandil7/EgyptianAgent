package com.egyptian.agent.hybrid;

import android.content.Context;
import android.util.Log;
import android.content.res.AssetManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * JNI wrapper for Llama 3.2 3B model integration
 * Handles loading, inference, and management of the GGUF model
 */
public class LlamaNative {
    private static final String TAG = "LlamaNative";
    private static boolean modelInitialized = false;
    private static String modelPath;

    static {
        try {
            System.loadLibrary("llama_native"); // Loads the native library
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load llama_native library", e);
        }
    }

    /**
     * Initializes the Llama model
     * @param context Application context
     * @param modelName Name of the model file in assets
     * @return 0 if successful, -1 if failed
     */
    public static synchronized int initializeModel(Context context, String modelName) {
        if (modelInitialized) {
            Log.d(TAG, "Model already initialized");
            return 0;
        }

        try {
            // Extract model from assets to internal storage
            modelPath = extractModelToInternalStorage(context, modelName);
            
            if (modelPath == null) {
                Log.e(TAG, "Failed to extract model to internal storage");
                return -1;
            }

            // Initialize the native model
            int result = init(modelPath, context.getAssets());
            
            if (result == 0) {
                modelInitialized = true;
                Log.i(TAG, "Llama model initialized successfully: " + modelName);
            } else {
                Log.e(TAG, "Failed to initialize Llama model: " + modelName);
            }
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Llama model", e);
            return -1;
        }
    }

    /**
     * Performs inference on the given prompt
     * @param prompt The input prompt to process
     * @param maxTokens Maximum number of tokens to generate
     * @return The model's response
     */
    public static String infer(String prompt, int maxTokens) {
        if (!modelInitialized) {
            Log.e(TAG, "Model not initialized. Call initializeModel first.");
            return "Model not initialized";
        }

        try {
            // Perform inference using native code
            String response = inferNative(prompt, maxTokens);
            Log.d(TAG, "Inference completed. Prompt: " + prompt + ", Response: " + response);
            return response;
        } catch (Exception e) {
            Log.e(TAG, "Error during inference", e);
            return "Error during inference: " + e.getMessage();
        }
    }

    /**
     * Unloads the model and frees resources
     */
    public static synchronized void unloadModel() {
        if (modelInitialized) {
            unload();
            modelInitialized = false;
            Log.i(TAG, "Llama model unloaded");
        }
    }

    /**
     * Extracts the model file from assets to internal storage
     * @param context Application context
     * @param modelName Name of the model file in assets
     * @return Path to the extracted model file, or null if failed
     */
    private static String extractModelToInternalStorage(Context context, String modelName) {
        AssetManager assetManager = context.getAssets();
        File modelDir = new File(context.getFilesDir(), "models");
        
        // Create models directory if it doesn't exist
        if (!modelDir.exists()) {
            if (!modelDir.mkdirs()) {
                Log.e(TAG, "Failed to create models directory");
                return null;
            }
        }

        File modelFile = new File(modelDir, modelName);
        
        // If model file already exists, return its path
        if (modelFile.exists()) {
            Log.d(TAG, "Model already exists in internal storage: " + modelFile.getAbsolutePath());
            return modelFile.getAbsolutePath();
        }

        // Extract model from assets to internal storage
        try (InputStream inputStream = assetManager.open("model/" + modelName);
             FileOutputStream outputStream = new FileOutputStream(modelFile)) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            Log.i(TAG, "Model extracted to internal storage: " + modelFile.getAbsolutePath());
            return modelFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error extracting model to internal storage", e);
            return null;
        }
    }

    // Native methods
    private static native int init(String modelPath, AssetManager assetManager);
    private static native String inferNative(String prompt, int maxTokens);
    private static native void unload();
}