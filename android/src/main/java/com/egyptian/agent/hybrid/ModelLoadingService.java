package com.egyptian.agent.hybrid;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.egyptian.agent.core.ModelManager;
import com.egyptian.agent.utils.CrashLogger;

/**
 * Model Loading Service
 * Handles background loading of AI models
 */
public class ModelLoadingService extends Service {
    private static final String TAG = "ModelLoadingService";
    
    private ModelManager modelManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ModelLoadingService created");
        
        // Initialize model manager
        modelManager = new ModelManager(this);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ModelLoadingService started");
        
        // Start loading models in background
        loadModelsAsync();
        
        // Use START_STICKY to restart service if killed
        return START_STICKY;
    }
    
    /**
     * Loads models asynchronously
     */
    private void loadModelsAsync() {
        new Thread(() -> {
            try {
                modelManager.initializeModels(new ModelManager.ModelInitializationCallback() {
                    @Override
                    public void onComplete(boolean success) {
                        Log.i(TAG, "Model loading completed: " + success);
                        
                        // Stop service after loading
                        stopSelf();
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Error loading models", error);
                        CrashLogger.logError(ModelLoadingService.this, error);
                        
                        // Stop service after error
                        stopSelf();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error starting model loading", e);
                CrashLogger.logError(ModelLoadingService.this, e);
                
                // Stop service after error
                stopSelf();
            }
        }).start();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        // This service doesn't provide binding
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ModelLoadingService destroyed");
    }
}