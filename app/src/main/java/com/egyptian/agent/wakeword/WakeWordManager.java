package com.egyptian.agent.wakeword;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wake Word Manager
 * 
 * Unified manager for wake word detection with automatic fallback.
 * Manages both Porcupine (primary) and Vosk (fallback) detectors.
 * 
 * Features:
 * - Automatic fallback from Porcupine to Vosk
 * - Configurable wake words
 * - Battery optimization
 * - State management
 */
public class WakeWordManager {
    private static final String TAG = "WakeWordManager";
    private static final String PREFS_NAME = "wakeword_prefs";
    private static final String KEY_SENSITIVITY = "sensitivity";
    private static final String KEY_USE_PORCUPINE = "use_porcupine";
    
    // Singleton instance
    private static WakeWordManager instance;
    
    private final Context context;
    private final SharedPreferences prefs;
    
    private PorcupineWakeWordDetector porcupineDetector;
    private VoskWakeWordDetector voskDetector;
    private WakeWordDetectorInterface activeDetector;
    
    private WakeWordCallback userCallback;
    private AtomicBoolean isInitialized;
    private AtomicBoolean isDestroyed;
    
    private DetectorType currentDetectorType;
    
    /**
     * Detector type enumeration.
     */
    public enum DetectorType {
        PORCUPINE,    // Primary - low power, high accuracy
        VOSK,         // Fallback - higher power, no extra SDK
        NONE          // Not initialized
    }
    
    /**
     * Private constructor for singleton pattern.
     */
    private WakeWordManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.isInitialized = new AtomicBoolean(false);
        this.isDestroyed = new AtomicBoolean(false);
        this.currentDetectorType = DetectorType.NONE;
        
        // Create internal callback that handles fallback
        this.userCallback = null;
    }
    
    /**
     * Get singleton instance.
     */
    public static synchronized WakeWordManager getInstance(Context context) {
        if (instance == null) {
            instance = new WakeWordManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Initialize wake word detection.
     * Call this before starting detection.
     */
    public synchronized void initialize() {
        if (isInitialized.get()) {
            Log.w(TAG, "Already initialized");
            return;
        }
        
        Log.d(TAG, "Initializing wake word manager");
        
        // Try Porcupine first (preferred)
        boolean usePorcupine = prefs.getBoolean(KEY_USE_PORCUPINE, true);
        
        if (usePorcupine) {
            try {
                porcupineDetector = new PorcupineWakeWordDetector(context);
                porcupineDetector.setCallback(internalCallback);
                activeDetector = porcupineDetector;
                currentDetectorType = DetectorType.PORCUPINE;
                Log.i(TAG, "Using Porcupine detector (primary)");
            } catch (Exception e) {
                Log.w(TAG, "Porcupine initialization failed, falling back to Vosk", e);
                currentDetectorType = DetectorType.NONE;
            }
        }
        
        // Fallback to Vosk if Porcupine failed
        if (currentDetectorType == DetectorType.NONE) {
            try {
                voskDetector = new VoskWakeWordDetector(context);
                voskDetector.setCallback(internalCallback);
                activeDetector = voskDetector;
                currentDetectorType = DetectorType.VOSK;
                Log.i(TAG, "Using Vosk detector (fallback)");
            } catch (Exception e) {
                Log.e(TAG, "Vosk initialization failed", e);
                currentDetectorType = DetectorType.NONE;
            }
        }
        
        isInitialized.set(true);
        Log.i(TAG, "Wake word manager initialized with: " + currentDetectorType);
    }
    
    /**
     * Internal callback that handles fallback logic.
     */
    private final WakeWordCallback internalCallback = new WakeWordCallback() {
        @Override
        public void onWakeWordDetected(String wakeWord, float confidence) {
            if (userCallback != null) {
                userCallback.onWakeWordDetected(wakeWord, confidence);
            }
        }
        
        @Override
        public void onError(Exception error) {
            Log.e(TAG, "Detector error: " + error.getMessage());
            
            // Try to switch to fallback detector
            if (currentDetectorType == DetectorType.PORCUPINE && voskDetector == null) {
                Log.i(TAG, "Switching to Vosk fallback");
                try {
                    voskDetector = new VoskWakeWordDetector(context);
                    voskDetector.setCallback(internalCallback);
                    activeDetector = voskDetector;
                    currentDetectorType = DetectorType.VOSK;
                    
                    // Restart detection
                    if (isListening()) {
                        voskDetector.start();
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
        public void onStateChanged(boolean isListening) {
            if (userCallback != null) {
                userCallback.onStateChanged(isListening);
            }
        }
    };
    
    /**
     * Set user callback for wake word events.
     */
    public void setCallback(WakeWordCallback callback) {
        this.userCallback = callback;
    }
    
    /**
     * Start wake word detection.
     */
    public synchronized void start() {
        if (!isInitialized.get()) {
            initialize();
        }
        
        if (activeDetector != null) {
            activeDetector.start();
            Log.d(TAG, "Wake word detection started");
        } else {
            Log.w(TAG, "No active detector available");
        }
    }
    
    /**
     * Stop wake word detection.
     */
    public synchronized void stop() {
        if (activeDetector != null) {
            activeDetector.stop();
            Log.d(TAG, "Wake word detection stopped");
        }
    }
    
    /**
     * Restart wake word detection.
     */
    public synchronized void restart() {
        if (activeDetector != null) {
            activeDetector.restart();
            Log.d(TAG, "Wake word detection restarted");
        }
    }
    
    /**
     * Check if currently listening for wake words.
     */
    public boolean isListening() {
        return activeDetector != null && activeDetector.isListening();
    }
    
    /**
     * Get current detector type.
     */
    public DetectorType getDetectorType() {
        return currentDetectorType;
    }
    
    /**
     * Check if Porcupine is being used.
     */
    public boolean isUsingPorcupine() {
        return currentDetectorType == DetectorType.PORCUPINE;
    }
    
    /**
     * Set detection sensitivity (0.0 to 1.0).
     */
    public void setSensitivity(float sensitivity) {
        prefs.edit().putFloat(KEY_SENSITIVITY, Math.max(0.0f, Math.min(1.0f, sensitivity))).apply();
        Log.d(TAG, "Sensitivity set to: " + sensitivity);
    }
    
    /**
     * Get current sensitivity.
     */
    public float getSensitivity() {
        return prefs.getFloat(KEY_SENSITIVITY, 0.7f);
    }
    
    /**
     * Enable or disable Porcupine (requires restart).
     */
    public void setUsePorcupine(boolean use) {
        prefs.edit().putBoolean(KEY_USE_PORCUPINE, use).apply();
        Log.d(TAG, "Use Porcupine: " + use);
    }
    
    /**
     * Clean up resources.
     */
    public synchronized void destroy() {
        if (isDestroyed.get()) {
            return;
        }

        Log.d(TAG, "Destroying wake word manager");

        stop();

        if (porcupineDetector != null) {
            porcupineDetector.destroy();
            porcupineDetector = null;
        }

        if (voskDetector != null) {
            voskDetector.destroy();
            voskDetector = null;
        }

        activeDetector = null;
        currentDetectorType = DetectorType.NONE;
        isDestroyed.set(true);

        // Clear singleton for recreation
        instance = null;
    }

    /**
     * Interface for wake word detection.
     */
    public interface WakeWordDetectorInterface {
        void setCallback(WakeWordCallback callback);
        void start();
        void stop();
        void restart();
        boolean isListening();
        void destroy();
    }
}
