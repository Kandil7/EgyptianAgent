package com.egyptian.agent.wakeword;

/**
 * Callback interface for wake word detection events.
 */
public interface WakeWordCallback {
    void onWakeWordDetected(String wakeWord, float confidence);
    void onError(Exception error);
    void onStateChanged(boolean isListening);
}
