package com.egyptian.agent.asr;

/**
 * ASR Result Container
 * 
 * Holds the result of speech-to-text recognition.
 */
public class ASRResult {
    private String text;
    private float confidence;
    private long durationMs;
    private boolean isFinal;
    private String language;
    
    /**
     * Create empty result.
     */
    public ASRResult() {
        this.text = "";
        this.confidence = 0.0f;
        this.durationMs = 0;
        this.isFinal = false;
        this.language = "ar";
    }
    
    /**
     * Create result with text.
     */
    public ASRResult(String text) {
        this.text = text;
        this.confidence = 1.0f;
        this.durationMs = 0;
        this.isFinal = true;
        this.language = "ar";
    }
    
    /**
     * Create result with all fields.
     */
    public ASRResult(String text, float confidence, long durationMs, boolean isFinal) {
        this.text = text;
        this.confidence = confidence;
        this.durationMs = durationMs;
        this.isFinal = isFinal;
        this.language = "ar";
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
    
    public boolean isFinal() {
        return isFinal;
    }
    
    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    /**
     * Check if result is empty.
     */
    public boolean isEmpty() {
        return text == null || text.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return "ASRResult{" +
                "text='" + text + '\'' +
                ", confidence=" + confidence +
                ", isFinal=" + isFinal +
                '}';
    }
}
