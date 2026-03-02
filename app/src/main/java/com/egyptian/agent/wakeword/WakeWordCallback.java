package com.egyptian.agent.wakeword;

/**
 * Callback interface for wake word detection events.
 * Implemented by components that need to respond to wake word detection.
 */
public interface WakeWordCallback {
    /**
     * Called when a wake word is detected.
     * @param wakeWord The detected wake word ("يا صاحبي" or "يا كبير")
     * @param confidence Detection confidence (0.0 to 1.0)
     */
    void onWakeWordDetected(String wakeWord, float confidence);
    
    /**
     * Called when an error occurs during wake word detection.
     * @param error The error that occurred
     */
    void onError(Exception error);
    
    /**
     * Called when the detector state changes.
     * @param isListening Whether the detector is currently listening
     */
    void onStateChanged(boolean isListening);
}
