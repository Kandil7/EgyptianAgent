package com.egyptian.agent.nlp;

import java.util.HashMap;
import java.util.Map;

/**
 * Result of intent classification
 * Contains the classified intent type, confidence score, and extracted entities
 */
public class IntentResult {
    private IntentType intentType;
    private float confidence;
    private Map<String, String> entities;

    public IntentResult() {
        this.intentType = IntentType.UNKNOWN;
        this.confidence = 0.0f;
        this.entities = new HashMap<>();
    }

    public IntentResult(IntentType intentType, float confidence) {
        this.intentType = intentType;
        this.confidence = confidence;
        this.entities = new HashMap<>();
    }

    /**
     * Gets the intent type
     * @return The intent type
     */
    public IntentType getIntentType() {
        return intentType;
    }

    /**
     * Sets the intent type
     * @param intentType The intent type to set
     */
    public void setIntentType(IntentType intentType) {
        this.intentType = intentType;
    }

    /**
     * Gets the confidence score
     * @return The confidence score (0.0 to 1.0)
     */
    public float getConfidence() {
        return confidence;
    }

    /**
     * Sets the confidence score
     * @param confidence The confidence score (0.0 to 1.0)
     */
    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    /**
     * Sets an entity
     * @param key The entity key
     * @param value The entity value
     */
    public void setEntity(String key, String value) {
        entities.put(key, value);
    }

    /**
     * Gets an entity value
     * @param key The entity key
     * @return The entity value, or empty string if not found
     */
    public String getEntity(String key) {
        return entities.getOrDefault(key, "");
    }

    /**
     * Gets an entity value with default
     * @param key The entity key
     * @param defaultValue The default value if key not found
     * @return The entity value or default value
     */
    public String getEntity(String key, String defaultValue) {
        return entities.getOrDefault(key, defaultValue);
    }

    /**
     * Gets all entities
     * @return Map of all entities
     */
    public Map<String, String> getEntities() {
        return new HashMap<>(entities);
    }

    /**
     * Checks if the result is unknown
     * @return true if intent type is UNKNOWN, false otherwise
     */
    public boolean isUnknown() {
        return intentType == IntentType.UNKNOWN;
    }

    /**
     * Checks if the result has high confidence
     * @param threshold The confidence threshold
     * @return true if confidence is above threshold, false otherwise
     */
    public boolean hasHighConfidence(float threshold) {
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
}