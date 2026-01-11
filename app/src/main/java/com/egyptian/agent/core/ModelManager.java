package com.egyptian.agent.core;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Model manager that loads appropriate models based on device class
 * Optimizes for performance and memory usage on different hardware configurations
 */
public class ModelManager {
    private static final String TAG = "ModelManager";
    
    private final Context context;
    private final DeviceClassDetector.DeviceClass deviceClass;
    private final DeviceClassDetector.ModelConfiguration config;
    
    // Model paths for different components
    private String asrModelPath;
    private String llmModelPath;
    private String ttsModelPath;
    
    // Loading state
    private volatile boolean asrModelLoaded = false;
    private volatile boolean llmModelLoaded = false;
    private volatile boolean ttsModelLoaded = false;
    private volatile boolean isLoading = false;
    
    // Thread pool for model loading
    private final ExecutorService modelLoaderExecutor = Executors.newSingleThreadExecutor();
    
    public ModelManager(Context context) {
        this.context = context;
        this.deviceClass = ((MainApplication) context.getApplicationContext()).getDeviceClass();
        this.config = DeviceClassDetector.getRecommendedModelConfig(deviceClass);
        
        Log.i(TAG, "ModelManager initialized for device class: " + deviceClass.name());
        Log.i(TAG, "Recommended config: " + config.toString());
    }
    
    /**
     * Initializes all models asynchronously based on device class
     */
    public void initializeModels(ModelInitializationCallback callback) {
        if (isLoading) {
            Log.w(TAG, "Models are already loading");
            return;
        }
        
        isLoading = true;
        
        modelLoaderExecutor.execute(() -> {
            try {
                Log.i(TAG, "Starting model initialization for device class: " + deviceClass.name());
                
                // Load ASR model
                loadAsrModel();
                
                // Load LLM model if applicable for this device class
                if (config.llmModel != null) {
                    loadLlmModel();
                }
                
                // Load TTS model
                loadTtsModel();
                
                isLoading = false;
                
                if (callback != null) {
                    callback.onComplete(asrModelLoaded && 
                                      (config.llmModel == null || llmModelLoaded) && 
                                      ttsModelLoaded);
                }
                
                Log.i(TAG, "Model initialization completed. ASR: " + asrModelLoaded + 
                      ", LLM: " + llmModelLoaded + ", TTS: " + ttsModelLoaded);
            } catch (Exception e) {
                Log.e(TAG, "Error during model initialization", e);
                isLoading = false;
                
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * Loads the appropriate ASR model based on device class
     */
    private void loadAsrModel() {
        try {
            String modelSourcePath = getAsrModelSourcePath(config.asrModel);
            asrModelPath = copyModelToInternalStorage(modelSourcePath, "asr_model");
            
            if (asrModelPath != null) {
                asrModelLoaded = true;
                Log.i(TAG, "ASR model loaded: " + config.asrModel + " -> " + asrModelPath);
            } else {
                Log.e(TAG, "Failed to load ASR model: " + config.asrModel);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading ASR model", e);
        }
    }
    
    /**
     * Loads the appropriate LLM model based on device class
     */
    private void loadLlmModel() {
        if (config.llmModel == null) {
            Log.d(TAG, "No LLM model required for this device class");
            llmModelLoaded = true; // Mark as loaded since none is needed
            return;
        }
        
        try {
            String modelSourcePath = getLlmModelSourcePath(config.llmModel);
            if (modelSourcePath != null) {
                llmModelPath = copyModelToInternalStorage(modelSourcePath, "llm_model");
                
                if (llmModelPath != null) {
                    llmModelLoaded = true;
                    Log.i(TAG, "LLM model loaded: " + config.llmModel + " -> " + llmModelPath);
                } else {
                    Log.e(TAG, "Failed to load LLM model: " + config.llmModel);
                }
            } else {
                Log.w(TAG, "LLM model not available: " + config.llmModel + ", falling back to rules-based");
                llmModelLoaded = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading LLM model", e);
        }
    }
    
    /**
     * Loads the appropriate TTS model based on device class
     */
    private void loadTtsModel() {
        try {
            String modelSourcePath = getTtsModelSourcePath(config.ttsEngine);
            ttsModelPath = copyModelToInternalStorage(modelSourcePath, "tts_model");
            
            if (ttsModelPath != null) {
                ttsModelLoaded = true;
                Log.i(TAG, "TTS model loaded: " + config.ttsEngine + " -> " + ttsModelPath);
            } else {
                Log.e(TAG, "Failed to load TTS model: " + config.ttsEngine);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading TTS model", e);
        }
    }
    
    /**
     * Gets the source path for ASR model based on model name
     */
    private String getAsrModelSourcePath(String modelName) {
        switch (modelName) {
            case "whisper-tiny":
                return "model/whisper-tiny-q4_k_m.gguf";
            case "whisper-base":
                return "model/whisper-base-q4_k_m.gguf";
            case "whisper-small":
                return "model/whisper-small-q4_k_m.gguf";
            case "whisper-medium":
                return "model/whisper-medium-q4_k_m.gguf";
            case "vosk-model":
            default:
                // Default to Vosk model if specified or if unknown model
                return "model/egyptian_senior/vosk-model-small-ar_eg-0.4.zip";
        }
    }
    
    /**
     * Gets the source path for LLM model based on model name
     */
    private String getLlmModelSourcePath(String modelName) {
        switch (modelName) {
            case "gemma2:2b-q4":
                return "model/gemma2-2b-q4_k_m.gguf";
            case "llama3:3b-q5":
                return "model/llama3-3b-q5_k_m.gguf";
            case "mixtral:8x7b-q4":
                return "model/mixtral-8x7b-q4_k_m.gguf";
            default:
                Log.w(TAG, "Unknown LLM model: " + modelName);
                return null;
        }
    }
    
    /**
     * Gets the source path for TTS model based on engine name
     */
    private String getTtsModelSourcePath(String engineName) {
        switch (engineName) {
            case "piper":
                return "model/piper-egyptian-medium.onnx";
            case "coqui-tts":
                return "model/coqui-tts-arabic.pth";
            case "elevenlabs-local":
                return "model/elevenlabs-arabic-v1.onnx";
            case "espeak":
            default:
                // eSpeak doesn't require a model file, it's built into the system
                return null;
        }
    }
    
    /**
     * Copies model from assets to internal storage
     */
    private String copyModelToInternalStorage(String assetPath, String modelName) {
        try {
            // Check if model already exists in internal storage
            File modelFile = new File(context.getFilesDir(), modelName);
            
            // For now, always copy to ensure we have the right model
            // In production, you might want to check file size/checksum to avoid unnecessary copying
            
            InputStream inputStream = context.getAssets().open(assetPath);
            FileOutputStream outputStream = new FileOutputStream(modelFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            inputStream.close();
            outputStream.close();
            
            Log.d(TAG, "Model copied to: " + modelFile.getAbsolutePath());
            return modelFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error copying model from assets: " + assetPath, e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error copying model: " + assetPath, e);
            return null;
        }
    }
    
    /**
     * Gets the path to the loaded ASR model
     */
    public String getAsrModelPath() {
        return asrModelLoaded ? asrModelPath : null;
    }
    
    /**
     * Gets the path to the loaded LLM model
     */
    public String getLlmModelPath() {
        return llmModelLoaded ? llmModelPath : null;
    }
    
    /**
     * Gets the path to the loaded TTS model
     */
    public String getTtsModelPath() {
        return ttsModelLoaded ? ttsModelPath : null;
    }
    
    /**
     * Checks if all required models are loaded
     */
    public boolean areAllModelsLoaded() {
        boolean llmRequirementMet = config.llmModel == null || llmModelLoaded;
        return asrModelLoaded && llmRequirementMet && ttsModelLoaded;
    }
    
    /**
     * Gets the device class for this model manager
     */
    public DeviceClassDetector.DeviceClass getDeviceClass() {
        return deviceClass;
    }
    
    /**
     * Gets the model configuration for this device class
     */
    public DeviceClassDetector.ModelConfiguration getModelConfiguration() {
        return config;
    }
    
    /**
     * Cleans up resources and shuts down the model manager
     */
    public void cleanup() {
        modelLoaderExecutor.shutdown();
        
        // Delete temporary model files
        if (asrModelPath != null) {
            new File(asrModelPath).delete();
        }
        if (llmModelPath != null) {
            new File(llmModelPath).delete();
        }
        if (ttsModelPath != null) {
            new File(ttsModelPath).delete();
        }
        
        Log.i(TAG, "ModelManager cleaned up");
    }
    
    /**
     * Callback interface for model initialization
     */
    public interface ModelInitializationCallback {
        void onComplete(boolean success);
        void onError(Exception error);
    }
}