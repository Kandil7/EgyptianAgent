package com.egyptian.agent.nlp;

public class IntentResult {
    private IntentType intentType;
    private float confidence;
    private java.util.Map<String, String> entities;

    public IntentResult() {
        this.entities = new java.util.HashMap<>();
        this.confidence = 0.0f;
        this.intentType = IntentType.UNKNOWN;
    }

    public IntentType getIntentType() {
        return intentType;
    }

    public void setIntentType(IntentType intentType) {
        this.intentType = intentType;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public String getEntity(String key, String defaultValue) {
        return entities.getOrDefault(key, defaultValue);
    }

    public void setEntity(String key, String value) {
        entities.put(key, value);
    }

    public java.util.Map<String, String> getEntities() {
        return entities;
    }

    public void setEntities(java.util.Map<String, String> entities) {
        this.entities = entities;
    }
}