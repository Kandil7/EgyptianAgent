package com.egyptian.agent.nlu;

import java.util.HashMap;
import java.util.Map;

/**
 * Intent Result Container
 * 
 * Holds the result of intent classification including:
 * - Intent type
 * - Extracted entities
 * - Confidence score
 */
public class IntentResult {
    private IntentType intentType;
    private Map<String, String> entities;
    private float confidence;
    private String originalText;
    private long processingTimeMs;
    
    /**
     * Create empty result.
     */
    public IntentResult() {
        this.intentType = IntentType.UNKNOWN;
        this.entities = new HashMap<>();
        this.confidence = 0.0f;
        this.originalText = "";
        this.processingTimeMs = 0;
    }
    
    /**
     * Create result with intent type.
     */
    public IntentResult(IntentType intentType) {
        this.intentType = intentType;
        this.entities = new HashMap<>();
        this.confidence = 1.0f;
        this.originalText = "";
        this.processingTimeMs = 0;
    }
    
    /**
     * Create result with all fields.
     */
    public IntentResult(IntentType intentType, float confidence, String originalText) {
        this.intentType = intentType;
        this.entities = new HashMap<>();
        this.confidence = confidence;
        this.originalText = originalText;
        this.processingTimeMs = 0;
    }
    
    public IntentType getIntentType() {
        return intentType;
    }
    
    public void setIntentType(IntentType intentType) {
        this.intentType = intentType;
    }
    
    public Map<String, String> getEntities() {
        return entities;
    }
    
    public void setEntities(Map<String, String> entities) {
        this.entities = entities;
    }
    
    /**
     * Get entity by key.
     */
    public String getEntity(String key) {
        return entities.get(key);
    }
    
    /**
     * Get entity by key with default value.
     */
    public String getEntity(String key, String defaultValue) {
        return entities.getOrDefault(key, defaultValue);
    }
    
    /**
     * Set entity.
     */
    public void setEntity(String key, String value) {
        this.entities.put(key, value);
    }
    
    public float getConfidence() {
        return confidence;
    }
    
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
    
    public String getOriginalText() {
        return originalText;
    }
    
    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }
    
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    /**
     * Check if result is valid (not unknown).
     */
    public boolean isValid() {
        return intentType != IntentType.UNKNOWN && confidence >= 0.5f;
    }
    
    /**
     * Check if confidence meets threshold.
     */
    public boolean meetsThreshold(float threshold) {
        return confidence >= threshold;
    }
    
    @Override
    public String toString() {
        return "IntentResult{" +
                "intentType=" + intentType +
                ", confidence=" + confidence +
                ", entities=" + entities +
                '}';
    }
    
    /**
     * Create a copy of this result.
     */
    public IntentResult copy() {
        IntentResult copy = new IntentResult();
        copy.intentType = this.intentType;
        copy.entities = new HashMap<>(this.entities);
        copy.confidence = this.confidence;
        copy.originalText = this.originalText;
        copy.processingTimeMs = this.processingTimeMs;
        return copy;
    }
}
