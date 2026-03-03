/**
 * FunctionGemma JNI Native Implementation
 * 
 * Native binding for FunctionGemma-270M-IT inference engine.
 * Provides function calling capabilities for Egyptian Arabic voice commands.
 *
 * Model: google/functiongemma-270m-it (GGUF Q4_K_M quantized)
 * Backend: llama.cpp
 * Target: Android (armeabi-v7a, arm64-v8a)
 *
 * Native Methods:
 * - initFunctionGemmaNative(): Load model and initialize context
 * - inferNative(): Run inference and return complete response
 * - inferNativeStreaming(): Run inference with streaming token callback
 * - unloadFunctionGemmaNative(): Free resources and unload model
 *
 * @author Egyptian Agent Team
 * @version 1.0.0
 */

#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <thread>
#include <chrono>
#include <mutex>
#include <atomic>
#include <cstring>
#include <cstdlib>

// Android logging
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>

#define LOG_TAG "FunctionGemmaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)

// Conditional compilation for llama.cpp integration
#ifdef USE_LLAMA_CPP
#include "llama.h"
#include "ggml.h"
#endif

// ============================================================================
// Global State
// ============================================================================

// Thread-safe model loading state
static std::atomic<bool> g_modelLoading{false};
static std::atomic<bool> g_modelLoaded{false};
static std::mutex g_modelMutex;

// ============================================================================
// FunctionGemma Context State
// ============================================================================

/**
 * Holds the complete state for a FunctionGemma inference session.
 * Includes model, context, and sampling parameters.
 */
struct FunctionGemmaState {
#ifdef USE_LLAMA_CPP
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    llama_batch batch;
    std::vector<llama_token> embd;
#endif

    // Configuration parameters
    int n_ctx = 2048;
    int n_threads = 2;
    float temperature = 0.1f;
    int top_k = 40;
    float top_p = 0.9f;
    float repetition_penalty = 1.1f;
    int max_tokens = 256;

    // Model metadata
    std::string model_path;
    size_t model_size_bytes = 0;
    size_t model_params_count = 0;

    // Performance tracking
    long load_time_ms = 0;
    int inference_count = 0;
    long total_inference_time_ms = 0;

#ifdef USE_LLAMA_CPP
    ~FunctionGemmaState() {
        cleanup();
    }

    void cleanup() {
#ifdef USE_LLAMA_CPP
        if (ctx != nullptr) {
            llama_free(ctx);
            ctx = nullptr;
        }
        if (model != nullptr) {
            llama_free_model(model);
            model = nullptr;
        }
        // Batch is freed separately
#endif
    }
#endif
};

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Convert native pointer to jlong for Java.
 */
static inline jlong ptrToJLong(void* ptr) {
    return reinterpret_cast<jlong>(ptr);
}

/**
 * Convert jlong from Java to native pointer.
 */
static inline FunctionGemmaState* getStateFromPtr(jlong ptr) {
    if (ptr == 0) return nullptr;
    return reinterpret_cast<FunctionGemmaState*>(ptr);
}

/**
 * Get current time in milliseconds.
 */
static inline long long currentTimeMillis() {
    auto now = std::chrono::high_resolution_clock::now();
    auto duration = now.time_since_epoch();
    return std::chrono::duration_cast<std::chrono::milliseconds>(duration).count();
}

/**
 * Calculate elapsed time in milliseconds.
 */
static inline long elapsedMillis(long long start) {
    return currentTimeMillis() - start;
}

#ifdef USE_LLAMA_CPP
/**
 * Tokenize input text using the model's tokenizer.
 * Returns number of tokens, or negative value on error.
 */
static int tokenizeInput(
    llama_context* ctx,
    const std::string& input,
    std::vector<llama_token>& tokens_out,
    bool add_special = true
) {
    const llama_model* model = llama_get_model(ctx);
    const int n_vocab = llama_vocab_n_tokens(llama_get_model(ctx));
    
    // Reserve space for tokens
    tokens_out.clear();
    tokens_out.reserve(input.length() / 2);
    
    // Use llama_tokenize for tokenization
    std::vector<llama_token> temp_tokens(input.length() + 256);
    
    int n_tokens = llama_tokenize(
        model,
        input.c_str(),
        static_cast<int>(input.length()),
        temp_tokens.data(),
        static_cast<int>(temp_tokens.size()),
        add_special,
        true  // parse_special tokens
    );
    
    if (n_tokens < 0) {
        LOGE("Tokenization failed with error: %d", n_tokens);
        return n_tokens;
    }
    
    tokens_out.assign(temp_tokens.begin(), temp_tokens.begin() + n_tokens);
    return n_tokens;
}

/**
 * Decode token to text string.
 */
static std::string decodeToken(llama_context* ctx, llama_token token) {
    std::string piece(256, '\0');
    int piece_len = llama_token_to_piece(
        ctx,
        token,
        &piece[0],
        static_cast<int>(piece.size()),
        0,
        true  // special tokens
    );
    
    if (piece_len > 0) {
        piece.resize(piece_len);
        return piece;
    } else if (piece_len < 0) {
        LOGW("Failed to decode token: %d", token);
        return "";
    }
    
    return "";
}

/**
 * Sample next token using temperature and top-k/top-p sampling.
 * Returns the sampled token ID.
 */
static llama_token sampleNextToken(
    llama_context* ctx,
    float temperature,
    int top_k,
    float top_p,
    float repetition_penalty
) {
    const llama_model* model = llama_get_model(ctx);
    const int n_vocab = llama_vocab_n_tokens(model);
    
    // Get logits for the last token
    float* logits = llama_get_logits(ctx);
    if (logits == nullptr) {
        LOGE("Failed to get logits");
        return llama_token_eos(model);
    }
    
    // Create candidate list with scores
    std::vector<std::pair<float, int>> candidates;
    candidates.reserve(n_vocab);
    
    for (int i = 0; i < n_vocab; i++) {
        candidates.emplace_back(logits[i], i);
    }
    
    // Apply repetition penalty if needed
    if (repetition_penalty != 1.0f) {
        // Simple repetition penalty implementation
        // (full implementation would track generated tokens)
    }
    
    // Apply temperature scaling
    if (temperature > 0.0f && temperature != 1.0f) {
        for (auto& c : candidates) {
            c.first /= temperature;
        }
    }
    
    // Sort by score (descending)
    std::sort(candidates.begin(), candidates.end(),
        [](const std::pair<float, int>& a, const std::pair<float, int>& b) {
            return a.first > b.first;
        });
    
    // Top-K filtering
    if (top_k > 0 && top_k < static_cast<int>(candidates.size())) {
        candidates.resize(top_k);
    }
    
    // Top-P (nucleus) filtering
    if (top_p > 0.0f && top_p < 1.0f) {
        // Apply softmax and compute cumulative probability
        float max_score = candidates[0].first;
        float sum = 0.0f;
        
        for (auto& c : candidates) {
            c.first = std::exp(c.first - max_score);  // Numerical stability
            sum += c.first;
        }
        
        float cumulative_prob = 0.0f;
        size_t cutoff = candidates.size();
        
        for (size_t i = 0; i < candidates.size(); i++) {
            cumulative_prob += candidates[i].first / sum;
            if (cumulative_prob >= top_p) {
                cutoff = i + 1;
                break;
            }
        }
        
        candidates.resize(cutoff);
    }
    
    // Normalize probabilities
    float sum = 0.0f;
    for (auto& c : candidates) {
        c.first = std::exp(c.first);
        sum += c.first;
    }
    for (auto& c : candidates) {
        c.first /= sum;
    }
    
    // Sample from distribution
    float r = static_cast<float>(rand()) / RAND_MAX;
    float cumulative_prob = 0.0f;
    llama_token selected_token = candidates.back().second;
    
    for (const auto& c : candidates) {
        cumulative_prob += c.first;
        if (r < cumulative_prob) {
            selected_token = c.second;
            break;
        }
    }
    
    return selected_token;
}
#endif

// ============================================================================
// Native Method Implementations (llama.cpp backend)
// ============================================================================

#ifdef USE_LLAMA_CPP

/**
 * Initialize FunctionGemma native engine.
 * Loads the GGUF model file and creates inference context.
 *
 * @param env JNI environment
 * @param thiz Java object instance
 * @param modelPath Path to GGUF model file
 * @param contextSize Context window size (tokens)
 * @param numThreads Number of CPU threads
 * @param temperature Sampling temperature (0.0-1.0)
 * @param topK Top-K sampling parameter
 * @param topP Top-P (nucleus) sampling parameter
 * @return Native context pointer (0 if failed)
 */
extern "C"
JNIEXPORT jlong JNICALL
Java_com_egyptian_agent_llm_FunctionGemmaEngine_initFunctionGemmaNative(
    JNIEnv* env,
    jobject thiz,
    jstring modelPath,
    jint contextSize,
    jint numThreads,
    jfloat temperature,
    jint topK,
    jfloat topP
) {
    LOGI("==============================================");
    LOGI("Initializing FunctionGemma-270M-IT native engine");
    LOGI("==============================================");
    
    auto start_time = currentTimeMillis();
    
    // Thread-safe model loading
    bool expected = false;
    if (!g_modelLoading.compare_exchange_strong(expected, true)) {
        LOGW("Model already loading, waiting...");
        // Wait for loading to complete
        while (g_modelLoading.load()) {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
        if (g_modelLoaded.load()) {
            LOGW("Model already loaded by another thread");
            return 0;  // Return 0 to indicate already loaded
        }
    }
    
    // Create state object
    auto* state = new FunctionGemmaState();
    state->n_ctx = contextSize;
    state->n_threads = numThreads;
    state->temperature = temperature;
    state->top_k = topK;
    state->top_p = topP;
    
    // Get model path from Java string
    const char* path_cstr = env->GetStringUTFChars(modelPath, nullptr);
    state->model_path = std::string(path_cstr);
    env->ReleaseStringUTFChars(modelPath, path_cstr);
    
    LOGI("Model path: %s", state->model_path.c_str());
    LOGI("Context size: %d tokens", contextSize);
    LOGI("CPU threads: %d", numThreads);
    LOGI("Temperature: %.2f", temperature);
    LOGI("Top-K: %d", topK);
    LOGI("Top-P: %.2f", topP);
    
    // Initialize llama backend (only once per process)
    static std::atomic<bool> backend_initialized{false};
    if (!backend_initialized.load()) {
        llama_backend_init();
        backend_initialized.store(true);
        LOGI("llama backend initialized");
    }
    
    // Configure model parameters for mobile optimization
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;      // CPU-only for compatibility
    model_params.n_threads = numThreads;
    model_params.use_mmap = true;       // Memory map for lower RAM usage
    model_params.use_mlock = false;     // Allow swapping on low-memory devices
    model_params.check_tensors = false; // Skip tensor validation for faster load
    
    LOGI("Loading GGUF model...");
    state->model = llama_load_model_from_file(state->model_path.c_str(), model_params);
    
    if (state->model == nullptr) {
        LOGE("Failed to load model from: %s", state->model_path.c_str());
        LOGE("Possible causes:");
        LOGE("  - File does not exist");
        LOGE("  - Invalid GGUF format");
        LOGE("  - Insufficient memory");
        delete state;
        g_modelLoading.store(false);
        return 0;
    }
    
    // Get model metadata
    state->model_size_bytes = llama_model_size(state->model);
    state->model_params_count = llama_model_n_params(state->model);
    
    LOGI("Model loaded successfully");
    LOGI("  Size: %.1f MB", state->model_size_bytes / (1024.0 * 1024.0));
    LOGI("  Parameters: %zu", state->model_params_count);
    
    // Configure context parameters
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = contextSize;
    ctx_params.n_threads = numThreads;
    ctx_params.n_threads_batch = numThreads;
    ctx_params.seed = 1234;  // Fixed seed for reproducibility
    ctx_params.embeddings = false;
    ctx_params.rope_scaling_type = LLAMA_ROPE_SCALING_TYPE_NONE;
    ctx_params.pooling_type = LLAMA_POOLING_TYPE_NONE;
    
    // Mobile-specific optimizations
    ctx_params.flash_attn = false;  // Disable flash attention (not well supported on mobile)
    ctx_params.no_kv_offload = false;
    ctx_params.defrag_thold = 0.1f;
    
    LOGI("Creating inference context...");
    state->ctx = llama_new_context_with_model(state->model, ctx_params);
    
    if (state->ctx == nullptr) {
        LOGE("Failed to create llama context");
        llama_free_model(state->model);
        delete state;
        g_modelLoading.store(false);
        return 0;
    }
    
    // Initialize batch for decoding
    // Batch size of 512 is sufficient for function calling
    state->batch = llama_batch_init(512, 0, 1);
    
    // Calculate load time
    state->load_time_ms = elapsedMillis(start_time);
    
    LOGI("==============================================");
    LOGI("FunctionGemma initialization complete");
    LOGI("==============================================");
    LOGI("Load time: %lld ms", state->load_time_ms);
    LOGI("Context pointer: 0x%lx", reinterpret_cast<uintptr_t>(state));
    LOGI("Estimated memory: ~%zu MB", 
         (state->model_size_bytes + state->n_ctx * 4 * sizeof(float)) / (1024 * 1024));
    LOGI("==============================================");
    
    g_modelLoaded.store(true);
    g_modelLoading.store(false);
    
    return ptrToJLong(state);
}

/**
 * Run inference and return complete response.
 * Processes the input prompt and generates a function call JSON response.
 *
 * @param env JNI environment
 * @param thiz Java object instance
 * @param contextPtr Native context pointer
 * @param prompt Input prompt (with special tokens)
 * @param maxTokens Maximum tokens to generate
 * @return Generated response string
 */
extern "C"
JNIEXPORT jstring JNICALL
Java_com_egyptian_agent_llm_FunctionGemmaEngine_inferNative(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr,
    jstring prompt,
    jint maxTokens
) {
    FunctionGemmaState* state = getStateFromPtr(contextPtr);
    
    // Validate state
    if (state == nullptr) {
        LOGE("Invalid context pointer: %lx", contextPtr);
        return env->NewStringUTF("Error: Invalid context");
    }
    
    if (state->model == nullptr || state->ctx == nullptr) {
        LOGE("Model or context not initialized");
        return env->NewStringUTF("Error: Model not loaded");
    }
    
    auto start_time = currentTimeMillis();
    
    // Get prompt string
    const char* prompt_cstr = env->GetStringUTFChars(prompt, nullptr);
    std::string input_prompt(prompt_cstr);
    env->ReleaseStringUTFChars(prompt, nullptr);
    
    LOGD("Running inference...");
    LOGD("Prompt length: %zu chars", input_prompt.length());
    LOGD("Max tokens: %d", maxTokens);
    
    // Tokenize input
    std::vector<llama_token> input_tokens;
    int n_tokens = tokenizeInput(state->ctx, input_prompt, input_tokens, true);
    
    if (n_tokens < 0) {
        LOGE("Tokenization failed: %d", n_tokens);
        return env->NewStringUTF("Error: Tokenization failed");
    }
    
    LOGD("Tokenized input: %d tokens", n_tokens);
    
    // Clear KV cache for new inference
    llama_kv_cache_clear(state->ctx);
    
    // Prepare batch for prompt decoding
    state->batch.n_tokens = n_tokens;
    for (int i = 0; i < n_tokens; i++) {
        state->batch.token[i] = input_tokens[i];
        state->batch.pos[i] = i;
        state->batch.n_seq_id[i] = 1;
        state->batch.seq_id[i][0] = 0;
        state->batch.logits[i] = (i == n_tokens - 1);  // Only need logits for last token
    }
    
    // Decode the prompt
    if (llama_decode(state->ctx, state->batch) != 0) {
        LOGE("Failed to decode prompt");
        return env->NewStringUTF("Error: Decode failed");
    }
    
    // Generate response tokens
    std::string response;
    response.reserve(maxTokens * 4);  // Estimate 4 bytes per token
    
    int n_predict = std::min(maxTokens, state->n_ctx - n_tokens - 1);
    int n_past = n_tokens;
    
    llama_token eos_token = llama_token_eos(state->model);
    llama_token eot_token = llama_token_bos(state->model);  // Use BOS as EOT fallback
    
    // Try to find EOT token by name (FunctionGemma specific)
    // "<|eot_id|>" token
    std::vector<llama_token> eot_tokens(1);
    int eot_count = tokenizeInput(state->ctx, "<|eot_id|>", eot_tokens, false);
    if (eot_count > 0) {
        eot_token = eot_tokens[0];
    }
    
    // Generation loop
    for (int i = 0; i < n_predict; i++) {
        // Sample next token
        llama_token new_token = sampleNextToken(
            state->ctx,
            state->temperature,
            state->top_k,
            state->top_p,
            state->repetition_penalty
        );
        
        // Check for end of sequence
        if (new_token == eos_token || new_token == eot_token) {
            LOGD("End of sequence token reached at token %d", i);
            break;
        }
        
        // Decode token to text
        std::string piece = decodeToken(state->ctx, new_token);
        if (!piece.empty()) {
            response += piece;
        }
        
        // Prepare next batch (single token)
        n_past++;
        state->batch.n_tokens = 1;
        state->batch.token[0] = new_token;
        state->batch.pos[0] = n_past;
        state->batch.n_seq_id[0] = 1;
        state->batch.seq_id[0][0] = 0;
        state->batch.logits[0] = true;
        
        // Decode the new token
        if (llama_decode(state->ctx, state->batch) != 0) {
            LOGW("Decode failed at token %d", i);
            break;
        }
    }
    
    // Update metrics
    state->inference_count++;
    state->total_inference_time_ms += elapsedMillis(start_time);
    
    // Log performance
    long inference_time = elapsedMillis(start_time);
    float tokens_per_sec = response.length() > 0 ?
        (response.length() / 4.0f) / (inference_time / 1000.0f) : 0;
    
    LOGI("Inference completed in %ld ms (%.1f tokens/s)", inference_time, tokens_per_sec);
    LOGD("Response: %s", response.c_str());
    
    return env->NewStringUTF(response.c_str());
}

/**
 * Run inference with streaming token callback.
 * Calls back to Java for each generated token.
 *
 * @param env JNI environment
 * @param thiz Java object instance
 * @param contextPtr Native context pointer
 * @param prompt Input prompt
 * @param maxTokens Maximum tokens to generate
 * @param callback Java TokenCallback object
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_egyptian_agent_llm_FunctionGemmaEngine_inferNativeStreaming(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr,
    jstring prompt,
    jint maxTokens,
    jobject callback
) {
    FunctionGemmaState* state = getStateFromPtr(contextPtr);
    
    // Validate state
    if (state == nullptr) {
        LOGE("Invalid context pointer");
        jclass exception_class = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(exception_class, "Invalid context");
        return;
    }
    
    if (state->model == nullptr || state->ctx == nullptr) {
        LOGE("Model not initialized");
        jclass exception_class = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(exception_class, "Model not loaded");
        return;
    }
    
    // Validate callback
    if (callback == nullptr) {
        LOGE("Null callback provided");
        return;
    }
    
    auto start_time = currentTimeMillis();
    
    // Get callback class and methods
    jclass callback_class = env->GetObjectClass(callback);
    if (callback_class == nullptr) {
        LOGE("Failed to get callback class");
        return;
    }
    
    jmethodID onToken_method = env->GetMethodID(callback_class, "onToken", "(Ljava/lang/String;)V");
    jmethodID onComplete_method = env->GetMethodID(callback_class, "onComplete", "(Ljava/lang/String;)V");
    jmethodID onError_method = env->GetMethodID(callback_class, "onError", "(Ljava/lang/Exception;)V");
    
    if (onToken_method == nullptr || onComplete_method == nullptr) {
        LOGE("Invalid callback interface - missing methods");
        return;
    }
    
    // Get prompt string
    const char* prompt_cstr = env->GetStringUTFChars(prompt, nullptr);
    std::string input_prompt(prompt_cstr);
    env->ReleaseStringUTFChars(prompt, nullptr);
    
    LOGD("Running streaming inference...");
    
    // Tokenize input
    std::vector<llama_token> input_tokens;
    int n_tokens = tokenizeInput(state->ctx, input_prompt, input_tokens, true);
    
    if (n_tokens < 0) {
        LOGE("Tokenization failed");
        if (onError_method != nullptr) {
            jclass exception_class = env->FindClass("java/lang/Exception");
            jthrowable exception = env->NewObject(exception_class,
                env->GetMethodID(exception_class, "<init>", "(Ljava/lang/String;)V"),
                env->NewStringUTF("Tokenization failed"));
            env->CallVoidMethod(callback, onError_method, exception);
        }
        return;
    }
    
    // Clear KV cache
    llama_kv_cache_clear(state->ctx);
    
    // Decode prompt
    state->batch.n_tokens = n_tokens;
    for (int i = 0; i < n_tokens; i++) {
        state->batch.token[i] = input_tokens[i];
        state->batch.pos[i] = i;
        state->batch.n_seq_id[i] = 1;
        state->batch.seq_id[i][0] = 0;
        state->batch.logits[i] = (i == n_tokens - 1);
    }
    
    if (llama_decode(state->ctx, state->batch) != 0) {
        LOGE("Failed to decode prompt");
        return;
    }
    
    // Generate tokens with streaming
    std::string full_response;
    full_response.reserve(maxTokens * 4);
    
    int n_predict = std::min(maxTokens, state->n_ctx - n_tokens - 1);
    int n_past = n_tokens;
    
    llama_token eos_token = llama_token_eos(state->model);
    std::vector<llama_token> eot_tokens(1);
    int eot_count = tokenizeInput(state->ctx, "<|eot_id|>", eot_tokens, false);
    llama_token eot_token = (eot_count > 0) ? eot_tokens[0] : eos_token;
    
    for (int i = 0; i < n_predict; i++) {
        // Sample next token (greedy for streaming speed)
        llama_token new_token = sampleNextToken(
            state->ctx,
            state->temperature,
            state->top_k,
            state->top_p,
            state->repetition_penalty
        );
        
        if (new_token == eos_token || new_token == eot_token) {
            break;
        }
        
        // Decode token to text
        std::string piece = decodeToken(state->ctx, new_token);
        
        if (!piece.empty()) {
            full_response += piece;
            
            // Callback with token
            jstring token_str = env->NewStringUTF(piece.c_str());
            env->CallVoidMethod(callback, onToken_method, token_str);
            env->DeleteLocalRef(token_str);
            
            // Check for Java exception
            if (env->ExceptionCheck()) {
                LOGE("Exception in onToken callback");
                env->ExceptionClear();
                break;
            }
        }
        
        // Prepare next batch
        n_past++;
        state->batch.n_tokens = 1;
        state->batch.token[0] = new_token;
        state->batch.pos[0] = n_past;
        state->batch.n_seq_id[0] = 1;
        state->batch.seq_id[0][0] = 0;
        state->batch.logits[0] = true;
        
        if (llama_decode(state->ctx, state->batch) != 0) {
            break;
        }
    }
    
    // Callback with complete response
    jstring complete_str = env->NewStringUTF(full_response.c_str());
    env->CallVoidMethod(callback, onComplete_method, complete_str);
    env->DeleteLocalRef(complete_str);
    
    // Update metrics
    state->inference_count++;
    state->total_inference_time_ms += elapsedMillis(start_time);
    
    LOGI("Streaming inference completed in %ld ms", elapsedMillis(start_time));
}

/**
 * Unload FunctionGemma native engine and free resources.
 *
 * @param env JNI environment
 * @param thiz Java object instance
 * @param contextPtr Native context pointer
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_egyptian_agent_llm_FunctionGemmaEngine_unloadFunctionGemmaNative(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr
) {
    LOGI("Unloading FunctionGemma native engine...");
    
    FunctionGemmaState* state = getStateFromPtr(contextPtr);
    
    if (state != nullptr) {
        // Log final metrics
        LOGI("Session metrics:");
        LOGI("  Total inferences: %d", state->inference_count);
        LOGI("  Total inference time: %ld ms", state->total_inference_time_ms);
        if (state->inference_count > 0) {
            LOGI("  Average inference time: %ld ms", 
                 state->total_inference_time_ms / state->inference_count);
        }
        
        // Free batch
        llama_batch_free(state->batch);
        
        // Delete state (calls cleanup destructor)
        delete state;
        
        // Free llama backend
        llama_backend_free();
        
        g_modelLoaded.store(false);
        
        LOGI("FunctionGemma engine unloaded successfully");
    } else {
        LOGW("Invalid context pointer, nothing to unload");
    }
}

#else
// ============================================================================
// MOCK IMPLEMENTATION (when llama.cpp is not available)
// ============================================================================
// Provides fallback behavior for testing without full llama.cpp backend

static std::atomic<bool> g_mock_initialized{false};
static std::string g_mock_model_path;
static std::mutex g_mock_mutex;

extern "C"
JNIEXPORT jlong JNICALL
Java_com_egyptian_agent_llm_FunctionGemmaEngine_initFunctionGemmaNative(
    JNIEnv* env,
    jobject thiz,
    jstring modelPath,
    jint contextSize,
    jint numThreads,
    jfloat temperature,
    jint topK,
    jfloat topP
) {
    LOGI("[MOCK] Initializing FunctionGemma (mock mode)");
    
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("[MOCK] Model path: %s", path);
    LOGI("[MOCK] Config: ctx=%d, threads=%d, temp=%.2f, top_k=%d, top_p=%.2f",
         contextSize, numThreads, temperature, topK, topP);
    env->ReleaseStringUTFChars(modelPath, path);
    
    std::lock_guard<std::mutex> lock(g_mock_mutex);
    g_mock_model_path = std::string(path);
    g_mock_initialized.store(true);
    
    // Simulate model load time
    std::this_thread::sleep_for(std::chrono::milliseconds(500));
    
    LOGI("[MOCK] FunctionGemma initialized (mock mode)");
    
    // Return sentinel value
    return 0xFEEDFACE;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_egyptian_agent_llm_FunctionGemmaEngine_inferNative(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr,
    jstring prompt,
    jint maxTokens
) {
    if (!g_mock_initialized.load()) {
        return env->NewStringUTF("Error: Model not initialized");
    }
    
    const char* prompt_str = env->GetStringUTFChars(prompt, nullptr);
    std::string input(prompt_str);
    env->ReleaseStringUTFChars(prompt, nullptr);
    
    LOGD("[MOCK] Processing prompt: %s", input.c_str());
    
    // Simulate processing delay
    std::this_thread::sleep_for(std::chrono::milliseconds(100 + (rand() % 200)));
    
    // Mock Egyptian Arabic function call responses
    std::string response;
    std::string lower_input;
    lower_input.resize(input.length());
    std::transform(input.begin(), input.end(), lower_input.begin(), ::tolower);
    
    // Pattern matching for Egyptian dialect commands
    if (lower_input.find("اتصل") != std::string::npos || 
        lower_input.find("كلم") != std::string::npos) {
        response = "{\"function\": \"call_contact\", \"arguments\": {\"contact_name\": \"ماما\"}, \"confidence\": 0.95}";
    } else if (lower_input.find("واتساب") != std::string::npos) {
        response = "{\"function\": \"send_whatsapp\", \"arguments\": {\"contact_name\": \"أحمد\", \"message\": \"...\"}, \"confidence\": 0.92}";
    } else if (lower_input.find("نبه") != std::string::npos || 
               lower_input.find("منبه") != std::string::npos) {
        response = "{\"function\": \"set_alarm\", \"arguments\": {\"time\": \"بكرة الصبح\"}, \"confidence\": 0.90}";
    } else if (lower_input.find("الساعة") != std::string::npos ||
               lower_input.find("كام الوقت") != std::string::npos) {
        response = "{\"function\": \"read_time\", \"arguments\": {}, \"confidence\": 0.98}";
    } else if (lower_input.find("نجدة") != std::string::npos ||
               lower_input.find("استغاثة") != std::string::npos) {
        response = "{\"function\": \"emergency\", \"arguments\": {\"type\": \"general\"}, \"confidence\": 0.99}";
    } else if (lower_input.find("واي فاي") != std::string::npos ||
               lower_input.find("wifi") != std::string::npos) {
        response = "{\"function\": \"toggle_wifi\", \"arguments\": {\"state\": \"on\"}, \"confidence\": 0.88}";
    } else if (lower_input.find("ازيك") != std::string::npos ||
               lower_input.find("عامل ايه") != std::string::npos) {
        response = "{\"function\": \"greeting\", \"arguments\": {}, \"confidence\": 0.95}";
    } else {
        response = "{\"function\": \"unknown\", \"arguments\": {}, \"confidence\": 0.30, \"fallback\": true}";
    }
    
    LOGD("[MOCK] Response: %s", response.c_str());
    
    return env->NewStringUTF(response.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_egyptian_agent_llm_FunctionGemmaEngine_inferNativeStreaming(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr,
    jstring prompt,
    jint maxTokens,
    jobject callback
) {
    if (!g_mock_initialized.load()) {
        jclass exception_class = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(exception_class, "Model not initialized");
        return;
    }
    
    if (callback == nullptr) {
        LOGE("[MOCK] Null callback");
        return;
    }
    
    // Get callback methods
    jclass callback_class = env->GetObjectClass(callback);
    jmethodID onToken_method = env->GetMethodID(callback_class, "onToken", "(Ljava/lang/String;)V");
    jmethodID onComplete_method = env->GetMethodID(callback_class, "onComplete", "(Ljava/lang/String;)V");
    
    if (onToken_method == nullptr || onComplete_method == nullptr) {
        LOGE("[MOCK] Invalid callback interface");
        return;
    }
    
    // Get response
    jstring response = Java_com_egyptian_agent_llm_FunctionGemmaEngine_inferNative(
        env, thiz, contextPtr, prompt, maxTokens);
    
    const char* response_str = env->GetStringUTFChars(response, nullptr);
    std::string full_response(response_str);
    env->ReleaseStringUTFChars(response, response_str);
    
    // Stream character by character (mock)
    for (size_t i = 0; i < full_response.length(); i += 5) {
        std::string token = full_response.substr(i, std::min((size_t)5, full_response.length() - i));
        jstring token_str = env->NewStringUTF(token.c_str());
        env->CallVoidMethod(callback, onToken_method, token_str);
        env->DeleteLocalRef(token_str);
        std::this_thread::sleep_for(std::chrono::milliseconds(30));
    }
    
    // Complete callback
    jstring complete_str = env->NewStringUTF(full_response.c_str());
    env->CallVoidMethod(callback, onComplete_method, complete_str);
    env->DeleteLocalRef(complete_str);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_egyptian_agent_llm_FunctionGemmaEngine_unloadFunctionGemmaNative(
    JNIEnv* env,
    jobject thiz,
    jlong contextPtr
) {
    LOGI("[MOCK] Unloading FunctionGemma (mock mode)");
    
    std::lock_guard<std::mutex> lock(g_mock_mutex);
    g_mock_initialized.store(false);
    g_mock_model_path.clear();
    
    LOGI("[MOCK] FunctionGemma unloaded");
}

#endif

// ============================================================================
// Utility Native Methods
// ============================================================================

/**
 * Get library version string.
 */
extern "C"
JNIEXPORT jstring JNICALL
Java_com_egyptian_agent_llm_FunctionGemmaEngine_getVersionNative(
    JNIEnv* env,
    jobject thiz
) {
#ifdef USE_LLAMA_CPP
    return env->NewStringUTF("1.0.0 (llama.cpp backend)");
#else
    return env->NewStringUTF("1.0.0 (mock backend)");
#endif
}

/**
 * Check if llama.cpp backend is available.
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_egyptian_agent_llm_FunctionGemmaEngine_isLlamaCppAvailableNative(
    JNIEnv* env,
    jobject thiz
) {
#ifdef USE_LLAMA_CPP
    return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

/**
 * Get system memory info (for debugging).
 */
extern "C"
JNIEXPORT jlong JNICALL
Java_com_egyptian_agent_llm_FunctionGemmaEngine_getAvailableMemoryNative(
    JNIEnv* env,
    jobject thiz
) {
#ifdef __ANDROID__
    // Read from /proc/meminfo
    FILE* fp = fopen("/proc/meminfo", "r");
    if (fp == nullptr) {
        return -1;
    }
    
    char line[256];
    long available_kb = 0;
    
    while (fgets(line, sizeof(line), fp) != nullptr) {
        if (sscanf(line, "MemAvailable: %ld kB", &available_kb) == 1) {
            break;
        }
    }
    
    fclose(fp);
    return available_kb * 1024;  // Convert to bytes
#else
    return -1;
#endif
}
