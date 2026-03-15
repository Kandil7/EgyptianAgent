package com.egyptian.agent.core;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PocketSphinx Wake Word Detector Stub
 * Stub implementation since PocketSphinx library is unavailable
 * 
 * This stub provides a placeholder implementation that can be used
 * during development and testing when the actual PocketSphinx library
 * is not available.
 */
public class PocketSphinxWakeWordDetector {
    private static final String TAG = "PocketSphinxDetector";

    private Context context;
    private WakeWordCallback callback;
    private boolean isListening = false;
    private ExecutorService executor;

    // Wake word variations for Egyptian dialect
    private static final String[] EGYPTIAN_WAKE_WORDS = {
        "ya sa7bi",    // يا صاحبي
        "ya kabir",    // يا كبير
        "yakbir",      // يا كبير (different pronunciation)
        "yas7bi",      // يا صاحبي (different pronunciation)
        "ya3am",       // يا عمي
        "ya 3am"       // يا عمي (separated)
    };

    public interface WakeWordCallback {
        void onWakeWordDetected();
    }

    public PocketSphinxWakeWordDetector(Context context, WakeWordCallback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
        this.executor = Executors.newSingleThreadExecutor();
        Log.w(TAG, "PocketSphinxWakeWordDetector stub initialized - wake word detection disabled");
    }

    /**
     * Initializes the wake word detector (stub implementation)
     */
    public void initialize() throws IOException {
        Log.w(TAG, "PocketSphinx initialization skipped - using stub implementation");
        // Stub: No actual initialization
    }

    /**
     * Starts listening for wake words (stub implementation)
     */
    public void startListening() {
        Log.d(TAG, "Start listening called - stub implementation");
        isListening = true;
        // Stub: No actual listening
    }

    /**
     * Stops listening for wake words
     */
    public void stopListening() {
        Log.d(TAG, "Stop listening called");
        isListening = false;
        // Stub: No actual stop needed
    }

    /**
     * Shuts down the wake word detector
     */
    public void shutdown() {
        Log.d(TAG, "Shutdown called");
        isListening = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * Checks if the detector is currently listening
     * @return true if listening, false otherwise
     */
    public boolean isListening() {
        return isListening;
    }

    /**
     * Gets the list of supported wake words
     * @return Array of wake word strings
     */
    public String[] getSupportedWakeWords() {
        return EGYPTIAN_WAKE_WORDS.clone();
    }

    /**
     * Sets the sensitivity of the wake word detector
     * @param sensitivity Sensitivity value (0.0 to 1.0)
     */
    public void setSensitivity(float sensitivity) {
        Log.d(TAG, "Sensitivity set to: " + sensitivity + " (stub - no effect)");
    }

    /**
     * Checks if the detector is ready for use
     * @return true if ready, false otherwise
     */
    public boolean isReady() {
        // Stub: Always return false since we don't have actual detection
        return false;
    }
}
