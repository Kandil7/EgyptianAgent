package com.egyptian.agent.hybrid;

import android.content.Context;
import android.util.Log;

import com.egyptian.agent.core.Quantum;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.CrashLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hybrid Orchestrator for intent determination
 * Combines multiple AI models for improved accuracy
 */
public class HybridOrchestrator {
    private static final String TAG = "HybridOrchestrator";
    
    private Context context;
    private ExecutorService executorService;
    private OpenPhoneIntegration openPhoneIntegration;
    private boolean isInitialized = false;

    public HybridOrchestrator(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        
        // Initialize OpenPhone integration
        initializeOpenPhoneIntegration();
    }

    /**
     * Initializes the OpenPhone integration
     */
    private void initializeOpenPhoneIntegration() {
        try {
            openPhoneIntegration = new OpenPhoneIntegration(context);
            isInitialized = true;
            Log.i(TAG, "Hybrid Orchestrator initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize OpenPhone integration", e);
            CrashLogger.logError(context, e);
        }
    }

    /**
     * Determines the intent of the given command
     * @param command The command to analyze
     * @param callback Callback to receive the result
     */
    public void determineIntent(String command, IntentCallback callback) {
        if (!isInitialized) {
            Log.w(TAG, "Hybrid Orchestrator not initialized, using fallback");
            
            // Use fallback processing
            executorService.execute(() -> {
                Quantum quantum = new Quantum(context);
                quantum.processCommand(command);
                
                // Return unknown since we can't determine intent without proper initialization
                IntentResult result = new IntentResult();
                callback.onResult(result);
            });
            
            return;
        }

        // Use OpenPhone integration for intent determination
        openPhoneIntegration.analyzeText(command, new OpenPhoneIntegration.AnalysisCallback() {
            @Override
            public void onResult(IntentResult result) {
                Log.d(TAG, "OpenPhone integration result: " + result.getIntentType());
                callback.onResult(result);
            }

            @Override
            public void onFallbackRequired(String reason) {
                Log.d(TAG, "OpenPhone requires fallback: " + reason);
                
                // Try EgyptianNormalizer as fallback
                IntentResult fallbackResult = EgyptianNormalizer.classifyBasicIntent(command);
                
                if (fallbackResult.getIntentType().equals(com.egyptian.agent.nlp.IntentType.UNKNOWN)) {
                    // If still unknown, use Quantum class
                    Quantum quantum = new Quantum(context);
                    quantum.processCommand(command);
                    
                    // Return unknown since Quantum doesn't return structured results
                    callback.onResult(fallbackResult);
                } else {
                    callback.onResult(fallbackResult);
                }
            }
        });
    }

    /**
     * Callback interface for intent determination
     */
    public interface IntentCallback {
        void onResult(IntentResult result);
    }

    /**
     * Checks if the orchestrator is ready for use
     * @return true if initialized, false otherwise
     */
    public boolean isReady() {
        return isInitialized && openPhoneIntegration != null && openPhoneIntegration.isReady();
    }

    /**
     * Destroys the orchestrator and cleans up resources
     */
    public void destroy() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        
        if (openPhoneIntegration != null) {
            openPhoneIntegration.destroy();
        }
    }
}