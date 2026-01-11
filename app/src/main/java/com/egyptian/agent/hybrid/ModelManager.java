package com.egyptian.agent.hybrid;

import android.content.Context;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

/**
 * Manager for handling AI model loading, unloading, and lifecycle
 */
public class ModelManager {
    private static final String TAG = "ModelManager";
    
    private static ModelManager instance;
    private final Context context;
    private final Map<String, Object> loadedModels;
    
    private ModelManager(Context context) {
        this.context = context.getApplicationContext();
        this.loadedModels = new HashMap<>();
    }
    
    public static synchronized ModelManager getInstance(Context context) {
        if (instance == null) {
            instance = new ModelManager(context);
        }
        return instance;
    }
    
    /**
     * Loads a model with the given configuration
     */
    public boolean loadModel(String modelName, ModelConfig config) {
        Log.i(TAG, "Attempting to load model: " + modelName);
        
        // Check if model is already loaded
        if (loadedModels.containsKey(modelName)) {
            Log.i(TAG, "Model already loaded: " + modelName);
            return true;
        }
        
        try {
            // Load the appropriate model based on type
            Object modelInstance = createModelInstance(modelName, config);
            
            if (modelInstance != null) {
                loadedModels.put(modelName, modelInstance);
                Log.i(TAG, "Model loaded successfully: " + modelName);
                return true;
            } else {
                Log.e(TAG, "Failed to create model instance: " + modelName);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading model: " + modelName, e);
            return false;
        }
    }
    
    /**
     * Creates an instance of the appropriate model
     */
    private Object createModelInstance(String modelName, ModelConfig config) {
        // For now, we'll handle the OpenPhone model specifically
        if ("openphone-3b".equalsIgnoreCase(modelName)) {
            try {
                // Attempt to load the OpenPhone model
                OpenPhoneModel model = new OpenPhoneModel(context.getAssets(), config.getModelPath());
                return model;
            } catch (Exception e) {
                Log.e(TAG, "Error creating OpenPhone model instance", e);
                return null;
            }
        }
        
        Log.w(TAG, "Unknown model type: " + modelName);
        return null;
    }
    
    /**
     * Unloads a specific model
     */
    public boolean unloadModel(String modelName) {
        Object model = loadedModels.remove(modelName);
        
        if (model != null) {
            // Perform cleanup based on model type
            if (model instanceof OpenPhoneModel) {
                ((OpenPhoneModel) model).unload();
            }
            
            Log.i(TAG, "Model unloaded: " + modelName);
            return true;
        }
        
        Log.w(TAG, "Model not found for unloading: " + modelName);
        return false;
    }
    
    /**
     * Gets a loaded model instance
     */
    public Object getModel(String modelName) {
        return loadedModels.get(modelName);
    }
    
    /**
     * Checks if a model is currently loaded
     */
    public boolean isModelLoaded(String modelName) {
        return loadedModels.containsKey(modelName);
    }
    
    /**
     * Unloads all models
     */
    public void unloadAllModels() {
        for (Map.Entry<String, Object> entry : loadedModels.entrySet()) {
            Object model = entry.getValue();
            if (model instanceof OpenPhoneModel) {
                ((OpenPhoneModel) model).unload();
            }
        }
        loadedModels.clear();
        Log.i(TAG, "All models unloaded");
    }
    
    /**
     * Configuration class for model loading
     */
    public static class ModelConfig {
        private String modelPath;
        private String modelType;
        private int maxMemoryUsage;
        
        public ModelConfig() {
            // Set defaults
            this.modelType = "pytorch";
            this.maxMemoryUsage = 512 * 1024 * 1024; // 512MB default
        }
        
        public String getModelPath() {
            return modelPath;
        }
        
        public void setModelPath(String modelPath) {
            this.modelPath = modelPath;
        }
        
        public String getModelType() {
            return modelType;
        }
        
        public void setModelType(String modelType) {
            this.modelType = modelType;
        }
        
        public int getMaxMemoryUsage() {
            return maxMemoryUsage;
        }
        
        public void setMaxMemoryUsage(int maxMemoryUsage) {
            this.maxMemoryUsage = maxMemoryUsage;
        }
    }
}