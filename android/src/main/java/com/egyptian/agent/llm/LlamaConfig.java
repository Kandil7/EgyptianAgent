package com.egyptian.agent.llm;

/**
 * Llama Configuration
 * 
 * Configuration class for Llama model settings.
 */
public class LlamaConfig {
    
    // Model settings
    public static final String DEFAULT_MODEL_FILE = "llama-3.2-3b-Q4_K_M.gguf";
    public static final int DEFAULT_CONTEXT_SIZE = 2048;
    public static final int DEFAULT_NUM_THREADS = 4;
    public static final int DEFAULT_MAX_TOKENS = 128;
    
    // Performance settings
    public static final long MODEL_LOAD_TIMEOUT_MS = 30000;
    public static final int MIN_MEMORY_MB = 2000; // 2GB required
    public static final int OPTIMAL_MEMORY_MB = 3000; // 3GB optimal
    
    // Inference settings
    public static final float TEMPERATURE = 0.7f;
    public static final int TOP_K = 40;
    public static final float TOP_P = 0.9f;
    public static final float REPETITION_PENALTY = 1.1f;
    
    // Quantization options
    public enum Quantization {
        Q4_K_M,     // Best balance (default)
        Q4_0,       // Faster, less accurate
        Q5_K_M,     // More accurate, slower
        Q8_0,       // Highest accuracy, slowest
        F16,        // Full precision (not recommended for mobile)
        F32         // Maximum precision (not recommended for mobile)
    }
    
    private String modelFile;
    private int contextSize;
    private int numThreads;
    private int maxTokens;
    private Quantization quantization;
    private boolean useGPU;
    
    /**
     * Default configuration.
     */
    public LlamaConfig() {
        this.modelFile = DEFAULT_MODEL_FILE;
        this.contextSize = DEFAULT_CONTEXT_SIZE;
        this.numThreads = DEFAULT_NUM_THREADS;
        this.maxTokens = DEFAULT_MAX_TOKENS;
        this.quantization = Quantization.Q4_K_M;
        this.useGPU = false; // CPU only for compatibility
    }
    
    public String getModelFile() {
        return modelFile;
    }
    
    public void setModelFile(String modelFile) {
        this.modelFile = modelFile;
    }
    
    public int getContextSize() {
        return contextSize;
    }
    
    public void setContextSize(int contextSize) {
        this.contextSize = contextSize;
    }
    
    public int getNumThreads() {
        return numThreads;
    }
    
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public Quantization getQuantization() {
        return quantization;
    }
    
    public void setQuantization(Quantization quantization) {
        this.quantization = quantization;
    }
    
    public boolean isUseGPU() {
        return useGPU;
    }
    
    public void setUseGPU(boolean useGPU) {
        this.useGPU = useGPU;
    }
    
    /**
     * Create builder for configuration.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for LlamaConfig.
     */
    public static class Builder {
        private final LlamaConfig config;
        
        public Builder() {
            config = new LlamaConfig();
        }
        
        public Builder modelFile(String modelFile) {
            config.setModelFile(modelFile);
            return this;
        }
        
        public Builder contextSize(int contextSize) {
            config.setContextSize(contextSize);
            return this;
        }
        
        public Builder numThreads(int numThreads) {
            config.setNumThreads(numThreads);
            return this;
        }
        
        public Builder maxTokens(int maxTokens) {
            config.setMaxTokens(maxTokens);
            return this;
        }
        
        public Builder quantization(Quantization quantization) {
            config.setQuantization(quantization);
            return this;
        }
        
        public Builder useGPU(boolean useGPU) {
            config.setUseGPU(useGPU);
            return this;
        }
        
        public LlamaConfig build() {
            return config;
        }
    }
}
