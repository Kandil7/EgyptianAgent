package ai.openphone;

import android.content.res.AssetManager;

/**
 * Mock implementation of the OpenPhone library for demonstration purposes
 * In a real implementation, this would be replaced with the actual OpenPhone SDK
 */
public class OpenPhone {
    
    public static class Builder {
        private String modelPath;
        private AssetManager assetsManager;
        private OpenPhoneConfig config;
        
        public Builder setModelPath(String modelPath) {
            this.modelPath = modelPath;
            return this;
        }
        
        public Builder setAssetsManager(AssetManager assetsManager) {
            this.assetsManager = assetsManager;
            return this;
        }
        
        public Builder setConfig(OpenPhoneConfig config) {
            this.config = config;
            return this;
        }
        
        public OpenPhone build() {
            return new OpenPhone(modelPath, assetsManager, config);
        }
    }
    
    private final String modelPath;
    private final AssetManager assetsManager;
    private final OpenPhoneConfig config;
    
    private OpenPhone(String modelPath, AssetManager assetsManager, OpenPhoneConfig config) {
        this.modelPath = modelPath;
        this.assetsManager = assetsManager;
        this.config = config;
    }
    
    public OpenPhoneResult process(String prompt) {
        // Mock implementation - in real scenario this would call the actual OpenPhone model
        return new OpenPhoneResult(mockProcess(prompt));
    }
    
    private String mockProcess(String prompt) {
        // This is a mock implementation that simulates the OpenPhone model response
        // In a real implementation, this would call the actual model
        
        // For demo purposes, we'll create a basic response based on the prompt
        if (prompt.contains("اتصل") || prompt.contains("كلم") || prompt.contains("رن")) {
            // Detect call intent
            return "{\"intent\": \"CALL_CONTACT\", \"entities\": {\"contact\": \"person\"}, \"confidence\": 0.85}";
        } else if (prompt.contains("واتساب") || prompt.contains("ابعت") || prompt.contains("رساله")) {
            // Detect WhatsApp intent
            return "{\"intent\": \"SEND_WHATSAPP\", \"entities\": {\"contact\": \"person\", \"message\": \"message\"}, \"confidence\": 0.82}";
        } else if (prompt.contains("انبهني") || prompt.contains("نبهني") || prompt.contains("ذكرني")) {
            // Detect alarm intent
            return "{\"intent\": \"SET_ALARM\", \"entities\": {\"time\": \"time\"}, \"confidence\": 0.78}";
        } else if (prompt.contains("الوقت") || prompt.contains("الساعه") || prompt.contains("كام")) {
            // Detect time intent
            return "{\"intent\": \"READ_TIME\", \"entities\": {}, \"confidence\": 0.95}";
        } else if (prompt.contains("Emergency") || prompt.contains("ngda") || prompt.contains("estghatha")) {
            // Detect emergency intent
            return "{\"intent\": \"EMERGENCY\", \"entities\": {}, \"confidence\": 0.98}";
        } else {
            // Unknown intent
            return "{\"intent\": \"UNKNOWN\", \"entities\": {}, \"confidence\": 0.30}";
        }
    }
    
    public void unload() {
        // Cleanup resources
    }
}