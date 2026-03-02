package com.egyptian.agent.llm;

import android.content.Context;
import android.util.Log;

import com.egyptian.agent.utils.CrashLogger;
import com.egyptian.agent.utils.MemoryOptimizer;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Llama Engine
 * 
 * General-purpose Llama 3.2 3B engine for conversational responses.
 * Used for clarification questions and complex queries.
 * 
 * Features:
 * - Conversational response generation
 * - Egyptian dialect awareness
 * - Context-aware responses
 * - Memory optimized for mobile
 */
public class LlamaEngine {
    private static final String TAG = "LlamaEngine";
    
    // Model configuration
    private static final String MODEL_FILE = "llama-3.2-3b-Q4_K_M.gguf";
    private static final int DEFAULT_MAX_TOKENS = 128;
    private static final int CONTEXT_SIZE = 2048;
    private static final int NUM_THREADS = 4;
    
    private final Context context;
    
    private ExecutorService inferenceExecutor;
    private AtomicBoolean isModelLoaded;
    private AtomicBoolean isDestroyed;
    
    private long llamaContext;
    private String conversationHistory;
    
    /**
     * Create Llama engine.
     */
    public LlamaEngine(Context context) {
        this.context = context.getApplicationContext();
        this.inferenceExecutor = Executors.newSingleThreadExecutor();
        this.isModelLoaded = new AtomicBoolean(false);
        this.isDestroyed = new AtomicBoolean(false);
        this.conversationHistory = "";
        
        // Load model in background
        loadModelAsync();
    }
    
    /**
     * Load model asynchronously.
     */
    private void loadModelAsync() {
        new Thread(() -> {
            try {
                Log.i(TAG, "Loading Llama 3.2 3B for conversation...");
                
                if (!MemoryOptimizer.hasEnoughMemory(2000)) {
                    Log.w(TAG, "Insufficient memory for Llama model");
                    return;
                }
                
                File modelFile = getModelFile();
                
                llamaContext = initLlamaNative(
                    modelFile.getAbsolutePath(),
                    CONTEXT_SIZE,
                    NUM_THREADS
                );
                
                if (llamaContext != 0) {
                    isModelLoaded.set(true);
                    Log.i(TAG, "Llama conversation engine loaded");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading Llama model", e);
                CrashLogger.logError(context, e);
            }
        }).start();
    }
    
    /**
     * Get model file path.
     */
    private File getModelFile() {
        File outputFile = new File(context.getFilesDir(), "models/" + MODEL_FILE);
        if (!outputFile.exists()) {
            outputFile.getParentFile().mkdirs();
        }
        return outputFile;
    }
    
    /**
     * Generate conversational response.
     */
    public String generateResponse(String userQuery) {
        return generateResponse(userQuery, DEFAULT_MAX_TOKENS);
    }
    
    /**
     * Generate conversational response with token limit.
     */
    public String generateResponse(String userQuery, int maxTokens) {
        if (!isModelLoaded.get()) {
            Log.w(TAG, "Model not loaded");
            return getFallbackResponse(userQuery);
        }
        
        try {
            // Build prompt with conversation history
            String prompt = buildConversationPrompt(userQuery);
            
            // Run inference
            String response = inferNative(llamaContext, prompt, maxTokens);
            
            // Update conversation history
            updateConversationHistory(userQuery, response);
            
            Log.d(TAG, "Generated response: " + response);
            return response;
        } catch (Exception e) {
            Log.e(TAG, "Error generating response", e);
            return getFallbackResponse(userQuery);
        }
    }
    
    /**
     * Generate response for clarification.
     */
    public String generateClarification(String originalQuery, String missingInfo) {
        String prompt = "The user asked: \"" + originalQuery + "\"\n" +
                       "I need to know: " + missingInfo + "\n" +
                       "Ask the user for this information in Egyptian Arabic, politely and briefly.\n" +
                       "Response:";
        
        try {
            if (!isModelLoaded.get()) {
                return "ممكن توضحلي أكتر؟";
            }
            
            return inferNative(llamaContext, prompt, 64);
        } catch (Exception e) {
            Log.e(TAG, "Error generating clarification", e);
            return "ممكن توضحلي أكتر؟";
        }
    }
    
    /**
     * Build conversation prompt with history.
     */
    private String buildConversationPrompt(String userQuery) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an Egyptian Arabic voice assistant. ");
        prompt.append("Respond in Egyptian Arabic dialect, briefly and naturally.\n\n");
        
        if (!conversationHistory.isEmpty()) {
            prompt.append("Conversation history:\n");
            prompt.append(conversationHistory);
            prompt.append("\n\n");
        }
        
        prompt.append("User: ").append(userQuery).append("\n");
        prompt.append("Assistant:");
        
        return prompt.toString();
    }
    
    /**
     * Update conversation history.
     */
    private void updateConversationHistory(String userQuery, String response) {
        String entry = "User: " + userQuery + "\nAssistant: " + response + "\n";
        
        if (conversationHistory.length() + entry.length() > CONTEXT_SIZE * 4) {
            // Truncate old history
            int truncateAt = conversationHistory.indexOf("\n", conversationHistory.length() / 2);
            if (truncateAt > 0) {
                conversationHistory = conversationHistory.substring(truncateAt + 1);
            }
        }
        
        conversationHistory += entry;
    }
    
    /**
     * Get fallback response when model is unavailable.
     */
    private String getFallbackResponse(String query) {
        // Simple pattern-based responses
        String lower = query.toLowerCase();
        
        if (lower.contains("ازيك") || lower.contains("عامل ايه")) {
            return "أنا بخير، الحمد لله. أقدر أساعدك إيه؟";
        } else if (lower.contains("شكرا")) {
            return "عفواً، أنا هنا للمساعدة.";
        } else if (lower.contains("مع السلامة") || lower.contains("باي")) {
            return "مع السلامة، في أمان الله.";
        } else {
            return "ممكن توضحلي أكتر عشان أقدر أساعدك؟";
        }
    }
    
    /**
     * Clear conversation history.
     */
    public void clearHistory() {
        conversationHistory = "";
        Log.d(TAG, "Conversation history cleared");
    }
    
    /**
     * Check if model is ready.
     */
    public boolean isReady() {
        return isModelLoaded.get() && llamaContext != 0;
    }
    
    /**
     * Clean up resources.
     */
    public void destroy() {
        if (isDestroyed.get()) return;
        
        Log.d(TAG, "Destroying Llama engine");
        
        if (llamaContext != 0) {
            unloadLlamaNative(llamaContext);
            llamaContext = 0;
        }
        
        if (inferenceExecutor != null) {
            inferenceExecutor.shutdownNow();
            inferenceExecutor = null;
        }
        
        isDestroyed.set(true);
    }
    
    // Native methods for llama.cpp
    private native long initLlamaNative(String modelPath, int contextSize, int numThreads);
    private native String inferNative(long context, String prompt, int maxTokens);
    private native void unloadLlamaNative(long context);
}
