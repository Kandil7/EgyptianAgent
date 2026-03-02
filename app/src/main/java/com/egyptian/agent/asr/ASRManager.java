package com.egyptian.agent.asr;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ASR Manager
 * 
 * Unified manager for speech-to-text with automatic fallback.
 * Manages both Whisper (primary) and Vosk (fallback) engines.
 * 
 * Features:
 * - Automatic fallback from Whisper to Vosk
 * - Performance monitoring
 * - Model management
 * - Memory optimization
 */
public class ASRManager {
    private static final String TAG = "ASRManager";
    private static final String PREFS_NAME = "asr_prefs";
    private static final String KEY_USE_WHISPER = "use_whisper";
    private static final String KEY_MODEL_SIZE = "model_size";
    
    // Singleton instance
    private static ASRManager instance;
    
    private final Context context;
    private final SharedPreferences prefs;
    
    private WhisperASREngine whisperEngine;
    private VoskASREngine voskEngine;
    private ASREngineInterface activeEngine;
    
    private ASREngineInterface.RecognitionCallback userCallback;
    private AtomicBoolean isInitialized;
    private AtomicBoolean isDestroyed;
    
    private EngineType currentEngineType;
    
    /**
     * Engine type enumeration.
     */
    public enum EngineType {
        WHISPER,    // Primary - high accuracy
        VOSK,       // Fallback - lower memory
        NONE        // Not initialized
    }
    
    /**
     * Private constructor for singleton pattern.
     */
    private ASRManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.isInitialized = new AtomicBoolean(false);
        this.isDestroyed = new AtomicBoolean(false);
        this.currentEngineType = EngineType.NONE;
    }
    
    /**
     * Get singleton instance.
     */
    public static synchronized ASRManager getInstance(Context context) {
        if (instance == null) {
            instance = new ASRManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Initialize ASR manager.
     */
    public synchronized void initialize() {
        initialize(EngineType.WHISPER);
    }
    
    /**
     * Initialize ASR manager with specified engine preference.
     */
    public synchronized void initialize(EngineType preferredEngine) {
        if (isInitialized.get()) {
            Log.w(TAG, "Already initialized");
            return;
        }
        
        Log.d(TAG, "Initializing ASR manager");
        
        // Try preferred engine first
        if (preferredEngine == EngineType.WHISPER) {
            try {
                whisperEngine = new WhisperASREngine(context);
                whisperEngine.initialize();
                activeEngine = whisperEngine;
                currentEngineType = EngineType.WHISPER;
                Log.i(TAG, "Using Whisper engine (primary)");
            } catch (Exception e) {
                Log.w(TAG, "Whisper initialization failed, falling back to Vosk", e);
                currentEngineType = EngineType.NONE;
            }
        }
        
        // Fallback to Vosk if Whisper failed
        if (currentEngineType == EngineType.NONE) {
            try {
                voskEngine = new VoskASREngine(context);
                voskEngine.initialize();
                activeEngine = voskEngine;
                currentEngineType = EngineType.VOSK;
                Log.i(TAG, "Using Vosk engine (fallback)");
            } catch (Exception e) {
                Log.e(TAG, "Vosk initialization failed", e);
                currentEngineType = EngineType.NONE;
            }
        }
        
        isInitialized.set(true);
        Log.i(TAG, "ASR manager initialized with: " + currentEngineType);
    }
    
    /**
     * Internal callback that handles fallback logic.
     */
    private final ASREngineInterface.RecognitionCallback internalCallback = new ASREngineInterface.RecognitionCallback() {
        @Override
        public void onResult(ASRResult result) {
            if (userCallback != null) {
                userCallback.onResult(result);
            }
        }
        
        @Override
        public void onError(Exception error) {
            Log.e(TAG, "Engine error: " + error.getMessage());
            
            // Try to switch to fallback engine
            if (currentEngineType == EngineType.WHISPER && voskEngine == null) {
                Log.i(TAG, "Switching to Vosk fallback");
                try {
                    voskEngine = new VoskASREngine(context);
                    voskEngine.initialize();
                    activeEngine = voskEngine;
                    currentEngineType = EngineType.VOSK;
                    
                    // Restart listening if was active
                    if (isListening()) {
                        voskEngine.startListening(internalCallback);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to switch to Vosk", e);
                }
            }
            
            if (userCallback != null) {
                userCallback.onError(error);
            }
        }
        
        @Override
        public void onPartialResult(ASRResult partialResult) {
            if (userCallback != null) {
                userCallback.onPartialResult(partialResult);
            }
        }
    };
    
    /**
     * Set user callback for ASR events.
     */
    public void setCallback(ASREngineInterface.RecognitionCallback callback) {
        this.userCallback = callback;
    }
    
    /**
     * Transcribe audio file.
     */
    public ASRResult transcribe(String audioPath) {
        if (!isInitialized.get()) {
            initialize();
        }
        
        if (activeEngine != null) {
            return activeEngine.transcribe(audioPath);
        }
        
        Log.w(TAG, "No active engine available");
        return new ASRResult();
    }
    
    /**
     * Start continuous listening.
     */
    public void startListening() {
        if (!isInitialized.get()) {
            initialize();
        }
        
        if (activeEngine != null) {
            activeEngine.startListening(internalCallback);
            Log.d(TAG, "ASR listening started");
        } else {
            Log.w(TAG, "No active engine available");
        }
    }
    
    /**
     * Stop listening.
     */
    public void stopListening() {
        if (activeEngine != null) {
            activeEngine.stopListening();
            Log.d(TAG, "ASR listening stopped");
        }
    }
    
    /**
     * Check if currently listening.
     */
    public boolean isListening() {
        return activeEngine != null && 
               (whisperEngine != null && whisperEngine.isReady() || 
                voskEngine != null && voskEngine.isReady());
    }
    
    /**
     * Get current engine type.
     */
    public EngineType getEngineType() {
        return currentEngineType;
    }
    
    /**
     * Check if Whisper is being used.
     */
    public boolean isUsingWhisper() {
        return currentEngineType == EngineType.WHISPER;
    }
    
    /**
     * Get performance statistics.
     */
    public long getLastInferenceTime() {
        if (whisperEngine != null && currentEngineType == EngineType.WHISPER) {
            return whisperEngine.getLastInferenceTime();
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
        
        Log.d(TAG, "Destroying ASR manager");
        
        stopListening();
        
        if (whisperEngine != null) {
            whisperEngine.destroy();
            whisperEngine = null;
        }
        
        if (voskEngine != null) {
            voskEngine.destroy();
            voskEngine = null;
        }
        
        activeEngine = null;
        currentEngineType = EngineType.NONE;
        isDestroyed.set(true);
        
        // Clear singleton for recreation
        instance = null;
    }
}
