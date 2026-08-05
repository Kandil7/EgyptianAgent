package com.egyptian.agent.asr;

/**
 * ASR Engine Interface
 * 
 * Common interface for all speech-to-text engines.
 */
public interface ASREngineInterface {
    
    /**
     * Initialize the ASR engine.
     * @return true if initialization successful
     */
    boolean initialize();
    
    /**
     * Transcribe audio file to text.
     * @param audioPath Path to audio file
     * @return Transcription result
     */
    ASRResult transcribe(String audioPath);
    
    /**
     * Transcribe audio data (real-time streaming).
     * @param audioData PCM audio samples (16-bit, 16kHz, mono)
     * @return Partial or final transcription
     */
    ASRResult transcribeStream(short[] audioData);
    
    /**
     * Start continuous listening.
     * @param callback Callback for recognition results
     */
    void startListening(RecognitionCallback callback);
    
    /**
     * Stop listening.
     */
    void stopListening();
    
    /**
     * Check if engine is ready.
     * @return true if ready
     */
    boolean isReady();
    
    /**
     * Clean up resources.
     */
    void destroy();
    
    /**
     * Callback for recognition results.
     */
    interface RecognitionCallback {
        void onResult(ASRResult result);
        void onError(Exception error);
        void onPartialResult(ASRResult partialResult);
    }
}
