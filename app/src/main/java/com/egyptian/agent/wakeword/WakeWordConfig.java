package com.egyptian.agent.wakeword;

/**
 * Wake Word Configuration
 * 
 * Configuration class for wake word detection settings.
 */
public class WakeWordConfig {
    
    // Default wake words
    public static final String DEFAULT_WAKE_WORD_PRIMARY = "يا صاحبي";
    public static final String DEFAULT_WAKE_WORD_SENIOR = "يا كبير";
    
    // Audio settings
    public static final int SAMPLE_RATE = 16000;
    public static final int CHANNELS = 1; // Mono
    public static final int BITS_PER_SAMPLE = 16;
    
    // Detection settings
    public static final float DEFAULT_SENSITIVITY = 0.7f;
    public static final float MIN_SENSITIVITY = 0.1f;
    public static final float MAX_SENSITIVITY = 1.0f;
    
    // Performance settings
    public static final int DETECTION_PAUSE_MS = 2000; // Pause after detection
    public static final int BUFFER_SIZE_MS = 500; // Audio buffer size
    public static final int FRAME_LENGTH = 512; // Porcupine frame size
    
    // Battery optimization
    public static final boolean LOW_POWER_MODE_DEFAULT = true;
    public static final int THREAD_PRIORITY_MIN = Thread.MIN_PRIORITY;
    
    private String primaryWakeWord;
    private String seniorWakeWord;
    private float sensitivity;
    private boolean lowPowerMode;
    private boolean usePorcupine;
    
    /**
     * Default configuration.
     */
    public WakeWordConfig() {
        this.primaryWakeWord = DEFAULT_WAKE_WORD_PRIMARY;
        this.seniorWakeWord = DEFAULT_WAKE_WORD_SENIOR;
        this.sensitivity = DEFAULT_SENSITIVITY;
        this.lowPowerMode = LOW_POWER_MODE_DEFAULT;
        this.usePorcupine = true;
    }
    
    /**
     * Get primary wake word.
     */
    public String getPrimaryWakeWord() {
        return primaryWakeWord;
    }
    
    /**
     * Set primary wake word.
     */
    public void setPrimaryWakeWord(String wakeWord) {
        this.primaryWakeWord = wakeWord;
    }
    
    /**
     * Get senior mode wake word.
     */
    public String getSeniorWakeWord() {
        return seniorWakeWord;
    }
    
    /**
     * Set senior mode wake word.
     */
    public void setSeniorWakeWord(String wakeWord) {
        this.seniorWakeWord = wakeWord;
    }
    
    /**
     * Get detection sensitivity.
     */
    public float getSensitivity() {
        return sensitivity;
    }
    
    /**
     * Set detection sensitivity (0.0 to 1.0).
     */
    public void setSensitivity(float sensitivity) {
        this.sensitivity = Math.max(MIN_SENSITIVITY, Math.min(MAX_SENSITIVITY, sensitivity));
    }
    
    /**
     * Check if low power mode is enabled.
     */
    public boolean isLowPowerMode() {
        return lowPowerMode;
    }
    
    /**
     * Enable or disable low power mode.
     */
    public void setLowPowerMode(boolean enabled) {
        this.lowPowerMode = enabled;
    }
    
    /**
     * Check if Porcupine should be used.
     */
    public boolean isUsePorcupine() {
        return usePorcupine;
    }
    
    /**
     * Enable or disable Porcupine.
     */
    public void setUsePorcupine(boolean use) {
        this.usePorcupine = use;
    }
    
    /**
     * Create builder for configuration.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for WakeWordConfig.
     */
    public static class Builder {
        private final WakeWordConfig config;
        
        public Builder() {
            config = new WakeWordConfig();
        }
        
        public Builder primaryWakeWord(String wakeWord) {
            config.setPrimaryWakeWord(wakeWord);
            return this;
        }
        
        public Builder seniorWakeWord(String wakeWord) {
            config.setSeniorWakeWord(wakeWord);
            return this;
        }
        
        public Builder sensitivity(float sensitivity) {
            config.setSensitivity(sensitivity);
            return this;
        }
        
        public Builder lowPowerMode(boolean enabled) {
            config.setLowPowerMode(enabled);
            return this;
        }
        
        public Builder usePorcupine(boolean use) {
            config.setUsePorcupine(use);
            return this;
        }
        
        public WakeWordConfig build() {
            return config;
        }
    }
}
