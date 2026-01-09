package com.egyptian.agent.nlp;

import java.util.HashMap;
import java.util.Map;

public class IntentResult {
    private IntentType intentType;
    private Map<String, String> entities;
    private float confidence;

    public IntentResult() {
        this.intentType = IntentType.UNKNOWN;
        this.entities = new HashMap<>();
        this.confidence = 0.5f; // Default confidence
    }

    public IntentType getIntentType() {
        return intentType;
    }

    public void setIntentType(IntentType intentType) {
        this.intentType = intentType;
    }

    public String getEntity(String key, String defaultValue) {
        return entities.getOrDefault(key, defaultValue);
    }

    public void setEntity(String key, String value) {
        entities.put(key, value);
    }

    public Map<String, String> getAllEntities() {
        return new HashMap<>(entities);
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "IntentResult{" +
                "intentType=" + intentType +
                ", entities=" + entities +
                ", confidence=" + confidence +
                '}';
    }
}