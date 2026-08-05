package ai.openphone;

/**
 * Mock implementation of OpenPhoneConfig
 */
public class OpenPhoneConfig {
    
    public static class Builder {
        private String language;
        private String dialect;
        private int maxTokens;
        private float temperature;
        private int topK;
        private float topP;
        
        public Builder setLanguage(String language) {
            this.language = language;
            return this;
        }
        
        public Builder setDialect(String dialect) {
            this.dialect = dialect;
            return this;
        }
        
        public Builder setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Builder setTemperature(float temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public Builder setTopK(int topK) {
            this.topK = topK;
            return this;
        }
        
        public Builder setTopP(float topP) {
            this.topP = topP;
            return this;
        }
        
        public OpenPhoneConfig build() {
            return new OpenPhoneConfig(language, dialect, maxTokens, temperature, topK, topP);
        }
    }
    
    private final String language;
    private final String dialect;
    private final int maxTokens;
    private final float temperature;
    private final int topK;
    private final float topP;
    
    private OpenPhoneConfig(String language, String dialect, int maxTokens, float temperature, int topK, float topP) {
        this.language = language;
        this.dialect = dialect;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.topK = topK;
        this.topP = topP;
    }
    
    // Getters
    public String getLanguage() { return language; }
    public String getDialect() { return dialect; }
    public int getMaxTokens() { return maxTokens; }
    public float getTemperature() { return temperature; }
    public int getTopK() { return topK; }
    public float getTopP() { return topP; }
}