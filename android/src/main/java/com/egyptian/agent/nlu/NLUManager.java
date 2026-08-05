package com.egyptian.agent.nlu;

import android.content.Context;
import android.util.Log;

import com.egyptian.agent.llm.LlamaNLUClassifier;
import com.egyptian.agent.utils.CrashLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * NLU Manager
 * 
 * Unified Natural Language Understanding manager with hybrid classification.
 * Combines multiple classifiers for optimal accuracy and performance.
 * 
 * Classification Pipeline:
 * 1. Llama 3.2 3B (if available, confidence > 0.85)
 * 2. Rule-Based Classifier (fallback, fast)
 * 3. Egyptian Normalizer (basic pattern matching)
 * 
 * Features:
 * - Automatic fallback between classifiers
 * - Confidence threshold management
 * - Performance monitoring
 * - Egyptian dialect optimization
 */
public class NLUManager {
    private static final String TAG = "NLUManager";
    
    // Confidence thresholds
    private static final float HIGH_CONFIDENCE_THRESHOLD = 0.85f;
    private static final float MEDIUM_CONFIDENCE_THRESHOLD = 0.6f;
    private static final float LOW_CONFIDENCE_THRESHOLD = 0.4f;
    
    // Singleton instance
    private static NLUManager instance;
    
    private final Context context;
    
    private LlamaNLUClassifier llamaClassifier;
    private RuleBasedClassifier ruleBasedClassifier;
    
    private ExecutorService executorService;
    private AtomicBoolean isInitialized;
    private AtomicBoolean isDestroyed;
    
    private boolean useLlama;
    
    /**
     * Private constructor for singleton pattern.
     */
    private NLUManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.isInitialized = new AtomicBoolean(false);
        this.isDestroyed = new AtomicBoolean(false);
        this.useLlama = true;
        
        // Initialize rule-based classifier (always available)
        ruleBasedClassifier = new RuleBasedClassifier();
    }
    
    /**
     * Get singleton instance.
     */
    public static synchronized NLUManager getInstance(Context context) {
        if (instance == null) {
            instance = new NLUManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Initialize NLU manager.
     */
    public synchronized void initialize() {
        initialize(true);
    }
    
    /**
     * Initialize NLU manager with Llama option.
     */
    public synchronized void initialize(boolean enableLlama) {
        if (isInitialized.get()) {
            Log.w(TAG, "Already initialized");
            return;
        }
        
        Log.d(TAG, "Initializing NLU manager");
        
        this.useLlama = enableLlama;
        
        if (useLlama) {
            // Initialize Llama classifier in background
            executorService.execute(() -> {
                try {
                    llamaClassifier = new LlamaNLUClassifier(context);
                    Log.i(TAG, "Llama classifier initialized");
                } catch (Exception e) {
                    Log.w(TAG, "Llama classifier initialization failed, using fallback", e);
                    CrashLogger.logError(context, e);
                    llamaClassifier = null;
                }
                isInitialized.set(true);
            });
        } else {
            isInitialized.set(true);
        }
        
        Log.i(TAG, "NLU manager initialized (Llama: " + useLlama + ")");
    }
    
    /**
     * Classify intent from text.
     * Synchronous version - blocks until result is available.
     */
    public IntentResult classify(String text) {
        if (!isInitialized.get()) {
            Log.w(TAG, "Not initialized, using rule-based fallback");
            return ruleBasedClassifier.classify(text);
        }
        
        if (text == null || text.isEmpty()) {
            return new IntentResult();
        }
        
        long startTime = System.currentTimeMillis();
        IntentResult result;
        
        // Try Llama first if available
        if (useLlama && llamaClassifier != null && llamaClassifier.isReady()) {
            try {
                result = llamaClassifier.classify(text);
                
                // Check confidence
                if (result.getConfidence() >= HIGH_CONFIDENCE_THRESHOLD) {
                    result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
                    Log.d(TAG, "Llama classification: " + result.getIntentType() + 
                          " (confidence: " + result.getConfidence() + 
                          ", time: " + result.getProcessingTimeMs() + "ms)");
                    return result;
                }
                
                Log.d(TAG, "Llama confidence too low (" + result.getConfidence() + "), using fallback");
            } catch (Exception e) {
                Log.e(TAG, "Llama classification failed, using fallback", e);
                CrashLogger.logError(context, e);
            }
        }
        
        // Fall back to rule-based classifier
        result = ruleBasedClassifier.classify(text);
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        
        Log.d(TAG, "Rule-based classification: " + result.getIntentType() + 
              " (confidence: " + result.getConfidence() + 
              ", time: " + result.getProcessingTimeMs() + "ms)");
        
        return result;
    }
    
    /**
     * Classify intent asynchronously.
     */
    public void classifyAsync(String text, ClassificationCallback callback) {
        executorService.execute(() -> {
            try {
                IntentResult result = classify(text);
                if (callback != null) {
                    callback.onResult(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Async classification failed", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * Check if Llama classifier is available.
     */
    public boolean isLlamaAvailable() {
        return llamaClassifier != null && llamaClassifier.isReady();
    }
    
    /**
     * Enable or disable Llama classifier.
     */
    public void setUseLlama(boolean use) {
        this.useLlama = use;
        Log.d(TAG, "Use Llama: " + use);
    }
    
    /**
     * Get performance statistics.
     */
    public long getLastClassificationTime() {
        if (llamaClassifier != null) {
            return llamaClassifier.getLastClassificationTime();
        }
        return 0;
    }
    
    /**
     * Clean up resources.
     */
    public synchronized void destroy() {
        if (isDestroyed.get()) {
            return;
        }
        
        Log.d(TAG, "Destroying NLU manager");
        
        if (llamaClassifier != null) {
            llamaClassifier.destroy();
            llamaClassifier = null;
        }
        
        ruleBasedClassifier = null;
        
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        
        isDestroyed.set(true);
        
        // Clear singleton for recreation
        instance = null;
    }
    
    /**
     * Callback for async classification.
     */
    public interface ClassificationCallback {
        void onResult(IntentResult result);
        void onError(Exception error);
    }
}
