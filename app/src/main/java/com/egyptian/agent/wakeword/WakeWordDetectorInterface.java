package com.egyptian.agent.wakeword;

/**
 * Interface for wake word detection.
 * Defines the contract for all wake word detector implementations.
 */
public interface WakeWordDetectorInterface {
    void setCallback(WakeWordCallback callback);
    void start();
    void stop();
    void restart();
    boolean isListening();
    void destroy();
}
