package com.egyptian.agent.hybrid;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.egyptian.agent.utils.CrashLogger;
import com.egyptian.agent.utils.MemoryOptimizer;

public class ModelLoadingService extends Service {

    private static final String TAG = "ModelLoadingService";
    private static final String ACTION_LOAD_MODEL = "com.egyptian.agent.action.LOAD_MODEL";
    private static final String EXTRA_MODEL_NAME = "model_name";
    private static final String EXTRA_CONFIG = "config";

    private ModelManager modelManager;
    private volatile boolean isLoading = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "ModelLoadingService created");

        // Initialize model manager
        modelManager = ModelManager.getInstance(this);

        // Optimize memory before loading models
        MemoryOptimizer.checkMemoryConstraints(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ModelLoadingService started with intent: " + intent);

        if (intent != null && ACTION_LOAD_MODEL.equals(intent.getAction())) {
            String modelName = intent.getStringExtra(EXTRA_MODEL_NAME);
            // ModelConfig config = intent.getParcelableExtra(EXTRA_CONFIG); // Would need to implement Parcelable

            if (modelName != null && !isLoading) {
                isLoading = true;
                loadModelAsync(modelName);
            }
        }

        // We want this service to continue running until it is explicitly stopped
        return START_STICKY;
    }

    private void loadModelAsync(String modelName) {
        new Thread(() -> {
            try {
                Log.i(TAG, "Starting to load model: " + modelName);

                // Create default configuration for the model
                ModelManager.ModelConfig config = new ModelManager.ModelConfig();
                config.setModelPath("model/" + modelName);

                // Load the model using ModelManager
                boolean success = modelManager.loadModel(modelName, config);

                if (success) {
                    Log.i(TAG, "Model loaded successfully: " + modelName);
                    
                    // Optimize memory after loading
                    MemoryOptimizer.checkMemoryConstraints(this);
                } else {
                    Log.e(TAG, "Failed to load model: " + modelName);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading model: " + modelName, e);
                CrashLogger.logError(this, e);
            } finally {
                isLoading = false;
                
                // Stop the service if no more work is needed
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
        Log.i(TAG, "ModelLoadingService destroyed");
        super.onDestroy();
    }

    /**
     * Static method to start the model loading service
     */
    public static void startModelLoadingService(android.content.Context context, String modelName) {
        Intent intent = new Intent(context, ModelLoadingService.class);
        intent.setAction(ACTION_LOAD_MODEL);
        intent.putExtra(EXTRA_MODEL_NAME, modelName);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}