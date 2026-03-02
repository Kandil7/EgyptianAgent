package com.egyptian.agent.llm;

import android.content.Context;
import android.util.Log;

import com.egyptian.agent.utils.CrashLogger;
import com.egyptian.agent.utils.MemoryOptimizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
 * - Streaming token generation support
 *
 * Performance Targets (Honor X6c - Helio G81 Ultra):
 * - Model load time: <30s (cold), <5s (warm)
 * - Token generation: <1.5s for 128 tokens
 * - Memory usage: <350MB during inference
 */
public class LlamaEngine {
    private static final String TAG = "LlamaEngine";

    // Model configuration - optimized for Honor X6c
    private static final String MODEL_FILE = "llama-3.2-3b-Q4_K_M.gguf";
    private static final int DEFAULT_MAX_TOKENS = 128;
    private static final int CONTEXT_SIZE = 2048;
    
    // Thread configuration for Helio G81 Ultra (8 cores: 2x A75 + 6x A55)
    // Use 4 threads for optimal balance between parallelism and overhead
    private static final int NUM_THREADS = 4;
    
    // Inference parameters for Egyptian Arabic
    private static final float TEMPERATURE = 0.7f;      // Balance creativity/consistency
    private static final int TOP_K = 40;                 // Sample from top 40 tokens
    private static final float TOP_P = 0.9f;             // Nucleus sampling
    private static final float REPETITION_PENALTY = 1.1f; // Reduce repetition
    private static final int N_KEEP = 48;                // Keep special tokens

    private final Context context;
    private final LlamaConfig config;

    private ExecutorService inferenceExecutor;
    private ExecutorService modelLoadExecutor;
    private AtomicBoolean isModelLoaded;
    private AtomicBoolean isDestroyed;
    private AtomicBoolean isLoading;

    private long llamaContext;
    private String conversationHistory;
    
    // Performance metrics
    private long lastInferenceTimeMs;
    private int totalInferences;
    private long totalInferenceTimeMs;

    // Callback for streaming tokens
    public interface TokenCallback {
        void onToken(String token);
        void onComplete(String fullResponse);
        void onError(Exception error);
    }

    /**
     * Create Llama engine with default configuration.
     */
    public LlamaEngine(Context context) {
        this(context, new LlamaConfig());
    }

    /**
     * Create Llama engine with custom configuration.
     */
    public LlamaEngine(Context context, LlamaConfig config) {
        this.context = context.getApplicationContext();
        this.config = config;
        this.inferenceExecutor = Executors.newSingleThreadExecutor();
        this.modelLoadExecutor = Executors.newSingleThreadExecutor();
        this.isModelLoaded = new AtomicBoolean(false);
        this.isDestroyed = new AtomicBoolean(false);
        this.isLoading = new AtomicBoolean(false);
        this.conversationHistory = "";
        this.lastInferenceTimeMs = 0;
        this.totalInferences = 0;
        this.totalInferenceTimeMs = 0;

        // Load model in background
        loadModelAsync();
    }

    /**
     * Load model asynchronously with memory checks.
     */
    private void loadModelAsync() {
        if (isLoading.get()) {
            Log.w(TAG, "Model already loading");
            return;
        }

        modelLoadExecutor.execute(() -> {
            isLoading.set(true);
            try {
                Log.i(TAG, "Loading Llama 3.2 3B for conversation...");

                // Check memory availability (need ~2.5GB for model + runtime)
                if (!MemoryOptimizer.hasEnoughMemory(2000)) {
                    Log.w(TAG, "Insufficient memory for Llama model (need 2GB+)");
                    isLoading.set(false);
                    return;
                }

                // Extract model from assets if needed
                File modelFile = getModelFile();
                if (!modelFile.exists()) {
                    Log.i(TAG, "Extracting model from assets...");
                    extractModelFromAssets(MODEL_FILE, modelFile);
                }

                if (!modelFile.exists()) {
                    Log.e(TAG, "Model file not found: " + modelFile.getAbsolutePath());
                    isLoading.set(false);
                    return;
                }

                Log.i(TAG, "Initializing native Llama with: " + 
                      "path=" + modelFile.getAbsolutePath() +
                      ", ctx=" + CONTEXT_SIZE +
                      ", threads=" + NUM_THREADS);

                // Initialize native Llama with optimized parameters
                llamaContext = initLlamaNative(
                    modelFile.getAbsolutePath(),
                    CONTEXT_SIZE,
                    NUM_THREADS,
                    TEMPERATURE,
                    TOP_K,
                    TOP_P,
                    REPETITION_PENALTY
                );

                if (llamaContext != 0) {
                    isModelLoaded.set(true);
                    Log.i(TAG, "✓ Llama 3.2 3B conversation engine loaded successfully");
                    Log.i(TAG, "  Context pointer: 0x" + Long.toHexString(llamaContext));
                    
                    // Warm up the model with a simple prompt
                    warmUpModel();
                } else {
                    Log.e(TAG, "✗ Failed to load Llama model - native init returned 0");
                    CrashLogger.logError(context, new RuntimeException("Llama native init failed"));
                }
            } catch (Exception e) {
                Log.e(TAG, "✗ Error loading Llama model", e);
                CrashLogger.logError(context, e);
            } finally {
                isLoading.set(false);
            }
        });
    }

    /**
     * Extract model from assets to internal storage.
     */
    private void extractModelFromAssets(String modelName, File outputFile) throws IOException {
        outputFile.getParentFile().mkdirs();
        
        try (InputStream inputStream = context.getAssets().open("model/" + modelName);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            long lastProgressTime = 0;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                
                // Log progress every 500MB
                if (System.currentTimeMillis() - lastProgressTime > 2000) {
                    Log.i(TAG, "Extracting model: " + (totalBytes / (1024 * 1024)) + "MB...");
                    lastProgressTime = System.currentTimeMillis();
                }
            }
            
            Log.i(TAG, "Model extracted: " + (totalBytes / (1024 * 1024)) + "MB");
        }
    }

    /**
     * Get model file path.
     */
    private File getModelFile() {
        return new File(context.getFilesDir(), "models/" + MODEL_FILE);
    }

    /**
     * Warm up the model with a simple test prompt.
     */
    private void warmUpModel() {
        if (!isModelLoaded.get() || llamaContext == 0) return;

        try {
            Log.d(TAG, "Warming up model...");
            String warmupPrompt = "أهلاً وسهلاً";
            String result = inferNative(llamaContext, warmupPrompt, 32);
            Log.d(TAG, "Model warmed up: " + result);
        } catch (Exception e) {
            Log.e(TAG, "Error warming up model", e);
        }
    }

    /**
     * Generate conversational response (blocking).
     */
    public String generateResponse(String userQuery) {
        return generateResponse(userQuery, DEFAULT_MAX_TOKENS);
    }

    /**
     * Generate conversational response with token limit (blocking).
     */
    public String generateResponse(String userQuery, int maxTokens) {
        if (!isModelLoaded.get()) {
            Log.w(TAG, "Model not loaded, using fallback");
            return getFallbackResponse(userQuery);
        }

        long startTime = System.currentTimeMillis();

        try {
            // Build prompt with conversation history
            String prompt = buildConversationPrompt(userQuery);

            // Run inference
            String response = inferNative(llamaContext, prompt, maxTokens);

            // Update conversation history
            updateConversationHistory(userQuery, response);

            // Record metrics
            lastInferenceTimeMs = System.currentTimeMillis() - startTime;
            totalInferences++;
            totalInferenceTimeMs += lastInferenceTimeMs;

            Log.d(TAG, "Generated response in " + lastInferenceTimeMs + "ms: " + response);
            return response;
        } catch (Exception e) {
            Log.e(TAG, "Error generating response", e);
            CrashLogger.logError(context, e);
            return getFallbackResponse(userQuery);
        }
    }

    /**
     * Generate response with streaming callback (async).
     */
    public void generateResponseAsync(String userQuery, TokenCallback callback) {
        generateResponseAsync(userQuery, DEFAULT_MAX_TOKENS, callback);
    }

    /**
     * Generate response with streaming callback and token limit (async).
     */
    public void generateResponseAsync(String userQuery, int maxTokens, TokenCallback callback) {
        if (!isModelLoaded.get()) {
            if (callback != null) {
                callback.onComplete(getFallbackResponse(userQuery));
            }
            return;
        }

        inferenceExecutor.execute(() -> {
            try {
                String prompt = buildConversationPrompt(userQuery);
                StringBuilder fullResponse = new StringBuilder();

                // Use streaming inference if available
                if (supportsStreaming()) {
                    inferNativeStreaming(llamaContext, prompt, maxTokens, new TokenCallback() {
                        @Override
                        public void onToken(String token) {
                            fullResponse.append(token);
                            if (callback != null) {
                                callback.onToken(token);
                            }
                        }

                        @Override
                        public void onComplete(String response) {
                            updateConversationHistory(userQuery, response);
                            if (callback != null) {
                                callback.onComplete(response);
                            }
                        }

                        @Override
                        public void onError(Exception error) {
                            if (callback != null) {
                                callback.onError(error);
                            }
                        }
                    });
                } else {
                    // Fallback to non-streaming
                    String response = inferNative(llamaContext, prompt, maxTokens);
                    updateConversationHistory(userQuery, response);
                    if (callback != null) {
                        callback.onComplete(response);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in async generation", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * Check if streaming is supported by native layer.
     */
    private boolean supportsStreaming() {
        // Check if streaming method is available
        try {
            getClass().getDeclaredMethod("inferNativeStreaming", 
                long.class, String.class, int.class, TokenCallback.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Generate response for clarification.
     */
    public String generateClarification(String originalQuery, String missingInfo) {
        String prompt = "أنت مساعد صوتي مصري. " +
                       "المستخدم سأل: \"" + originalQuery + "\"\n" +
                       "أحتاج أعرف: " + missingInfo + "\n" +
                       "اسأل المستخدم عن المعلومة دي بالمصري، بأدب وباختصار.\n" +
                       "الرد:";

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
     * Build conversation prompt with history for Egyptian Arabic.
     */
    private String buildConversationPrompt(String userQuery) {
        StringBuilder prompt = new StringBuilder();

        // System prompt in Egyptian Arabic for better dialect handling
        prompt.append("أنت مساعد صوتي مصري بتتكلم مصري عامي طبيعي.\n");
        prompt.append("رد بإيجاز وبطريقة ودية وطبيعية زي المصريين.\n");
        prompt.append("استخدم اللهجة المصرية في ردك.\n\n");

        if (!conversationHistory.isEmpty()) {
            prompt.append("تاريخ المحادثة:\n");
            prompt.append(conversationHistory);
            prompt.append("\n\n");
        }

        prompt.append("المستخدم: ").append(userQuery).append("\n");
        prompt.append("المساعد:");

        return prompt.toString();
    }

    /**
     * Update conversation history with size management.
     */
    private void updateConversationHistory(String userQuery, String response) {
        String entry = "المستخدم: " + userQuery + "\nالمساعد: " + response + "\n";

        // Truncate if history exceeds ~75% of context window (in chars, rough estimate)
        int maxHistoryChars = CONTEXT_SIZE * 3; // ~3 bytes per token average
        
        if (conversationHistory.length() + entry.length() > maxHistoryChars) {
            // Keep only the last half of the history
            int truncateAt = conversationHistory.length() / 2;
            int newlinePos = conversationHistory.indexOf("\n", truncateAt);
            if (newlinePos > 0) {
                conversationHistory = conversationHistory.substring(newlinePos + 1);
            }
            Log.d(TAG, "Truncated conversation history to " + conversationHistory.length() + " chars");
        }

        conversationHistory += entry;
    }

    /**
     * Get fallback response when model is unavailable.
     */
    private String getFallbackResponse(String query) {
        // Simple pattern-based responses in Egyptian Arabic
        String lower = query.toLowerCase();

        if (lower.contains("ازيك") || lower.contains("عامل ايه") || lower.contains("أهلاً")) {
            return "أنا بخير، الحمد لله. أقدر أساعدك إيه؟";
        } else if (lower.contains("شكرا") || lower.contains("يسلمو")) {
            return "عفواً، أنا هنا للمساعدة.";
        } else if (lower.contains("مع السلامة") || lower.contains("باي") || lower.contains("سلام")) {
            return "مع السلامة، في أمان الله.";
        } else if (lower.contains("مين") || lower.contains("إيه")) {
            return "ممكن توضحلي أكتر عشان أقدر أساعدك؟";
        } else {
            return "ممكن تعيد وتوضحلي أكتر؟";
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
     * Check if model is currently loading.
     */
    public boolean isLoading() {
        return isLoading.get();
    }

    /**
     * Get average inference time.
     */
    public long getAverageInferenceTimeMs() {
        if (totalInferences == 0) return 0;
        return totalInferenceTimeMs / totalInferences;
    }

    /**
     * Get total number of inferences.
     */
    public int getTotalInferences() {
        return totalInferences;
    }

    /**
     * Get last inference time.
     */
    public long getLastInferenceTimeMs() {
        return lastInferenceTimeMs;
    }

    /**
     * Get memory usage estimate.
     */
    public long getEstimatedMemoryUsageMB() {
        if (!isModelLoaded.get()) return 0;
        // Llama 3.2 3B Q4_K_M: ~1.64GB model + ~300MB runtime = ~2GB
        return 2000;
    }

    /**
     * Clean up resources.
     */
    public void destroy() {
        if (isDestroyed.get()) return;

        Log.d(TAG, "Destroying Llama engine...");

        if (llamaContext != 0) {
            unloadLlamaNative(llamaContext);
            llamaContext = 0;
        }

        if (inferenceExecutor != null) {
            inferenceExecutor.shutdownNow();
            inferenceExecutor = null;
        }

        if (modelLoadExecutor != null) {
            modelLoadExecutor.shutdownNow();
            modelLoadExecutor = null;
        }

        isModelLoaded.set(false);
        isDestroyed.set(true);
        
        Log.d(TAG, "Llama engine destroyed");
    }

    // ========================================================================
    // Native Methods for llama.cpp
    // Updated signatures to include inference parameters
    // ========================================================================
    
    /**
     * Initialize native Llama context with full parameters.
     * @param modelPath Path to GGUF model file
     * @param contextSize Context window size (tokens)
     * @param numThreads Number of CPU threads
     * @param temperature Sampling temperature
     * @param topK Top-K sampling
     * @param topP Nucleus sampling (top-P)
     * @param repetitionPenalty Repetition penalty
     * @return Native context pointer (0 if failed)
     */
    private native long initLlamaNative(
        String modelPath,
        int contextSize,
        int numThreads,
        float temperature,
        int topK,
        float topP,
        float repetitionPenalty
    );
    
    /**
     * Run inference and return complete response.
     * @param context Native context pointer
     * @param prompt Input prompt
     * @param maxTokens Maximum tokens to generate
     * @return Generated response text
     */
    private native String inferNative(long context, String prompt, int maxTokens);
    
    /**
     * Run inference with streaming token callback.
     * @param context Native context pointer
     * @param prompt Input prompt
     * @param maxTokens Maximum tokens to generate
     * @param callback Token callback for streaming
     */
    private native void inferNativeStreaming(
        long context, 
        String prompt, 
        int maxTokens, 
        TokenCallback callback
    );
    
    /**
     * Unload native Llama context and free resources.
     * @param context Native context pointer
     */
    private native void unloadLlamaNative(long context);
}
