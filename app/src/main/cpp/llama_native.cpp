#include <jni.h>
#include <string>
#include <vector>
#include <sstream>
#include <thread>
#include <chrono>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>

#define LOG_TAG "LlamaNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// Conditional compilation for llama.cpp integration
#if USE_LLAMA_CPP
#include "llama.h"

// Structure to hold Llama context state
struct LlamaContextState {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    llama_batch batch;
    std::vector<llama_token> embd;
    int n_ctx = 2048;
    int n_threads = 4;
    float temperature = 0.7f;
    int top_k = 40;
    float top_p = 0.9f;
    float repetition_penalty = 1.1f;
    
    ~LlamaContextState() {
        if (ctx) llama_free(ctx);
        if (model) llama_free_model(model);
        llama_batch_free(batch);
    }
};

// Helper function to get state from pointer
static LlamaContextState* getStateFromPtr(long ptr) {
    return reinterpret_cast<LlamaContextState*>(ptr);
}

// Helper function to convert pointer to jlong
static long ptrToJLong(void* ptr) {
    return reinterpret_cast<jlong>(ptr);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_egyptian_agent_llm_LlamaEngine_initLlamaNative(
    JNIEnv *env,
    jobject thiz,
    jstring modelPath,
    jint contextSize,
    jint numThreads,
    jfloat temperature,
    jint topK,
    jfloat topP,
    jfloat repetitionPenalty
) {
    LOGI("Initializing Llama native engine...");
    LOGI("  Model path: %s", env->GetStringUTFChars(modelPath, nullptr));
    LOGI("  Context size: %d", contextSize);
    LOGI("  Threads: %d", numThreads);
    LOGI("  Temperature: %.2f", temperature);
    LOGI("  Top-K: %d", topK);
    LOGI("  Top-P: %.2f", topP);
    LOGI("  Repetition penalty: %.2f", repetitionPenalty);

    auto start = std::chrono::high_resolution_clock::now();

    // Create state object
    auto* state = new LlamaContextState();
    state->n_ctx = contextSize;
    state->n_threads = numThreads;
    state->temperature = temperature;
    state->top_k = topK;
    state->top_p = topP;
    state->repetition_penalty = repetitionPenalty;

    const char* path = env->GetStringUTFChars(modelPath, nullptr);

    // Initialize llama backend
    llama_backend_init();

    // Model parameters - optimized for mobile
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;  // CPU only for compatibility on Honor X6c
    model_params.n_threads = numThreads;
    model_params.use_mmap = true;   // Memory map for lower RAM usage
    model_params.use_mlock = false; // Don't lock memory (allows swapping)

    LOGI("Loading model from: %s", path);
    state->model = llama_load_model_from_file(path, model_params);
    
    if (state->model == nullptr) {
        LOGE("Failed to load model: %s", path);
        env->ReleaseStringUTFChars(modelPath, path);
        delete state;
        llama_backend_free();
        return 0;
    }

    // Context parameters - optimized for Egyptian Arabic
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = contextSize;
    ctx_params.n_threads = numThreads;
    ctx_params.n_threads_batch = numThreads;
    ctx_params.seed = 1234;  // Fixed seed for reproducibility
    ctx_params.embeddings = false;
    ctx_params.rope_scaling_type = LLAMA_ROPE_SCALING_TYPE_NONE;
    ctx_params.pooling_type = LLAMA_POOLING_TYPE_NONE;
    
    // Memory optimization for 6GB RAM devices
    ctx_params.flash_attn = false;  // Disable FA on mobile (not well supported)
    ctx_params.no_perf = false;

    LOGI("Creating context with n_ctx=%d, n_threads=%d", contextSize, numThreads);
    state->ctx = llama_new_context_with_model(state->model, ctx_params);
    
    if (state->ctx == nullptr) {
        LOGE("Failed to create context");
        llama_free_model(state->model);
        env->ReleaseStringUTFChars(modelPath, path);
        delete state;
        llama_backend_free();
        return 0;
    }

    // Initialize batch for decoding
    state->batch = llama_batch_init(512, 0, 1);

    env->ReleaseStringUTFChars(modelPath, path);

    auto end = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
    LOGI("✓ Model loaded successfully in %lld ms", duration.count());
    LOGI("  Model size: %zu MB", llama_model_size(state->model) / (1024 * 1024));
    LOGI("  Model parameters: %zu", llama_model_n_params(state->model));

    return ptrToJLong(state);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_egyptian_agent_llm_LlamaEngine_inferNative(
    JNIEnv *env,
    jobject thiz,
    jlong contextPtr,
    jstring prompt,
    jint maxTokens
) {
    LlamaContextState* state = getStateFromPtr(contextPtr);
    
    if (state == nullptr || state->model == nullptr || state->ctx == nullptr) {
        LOGE("Invalid context or model");
        return env->NewStringUTF("Error: Model not initialized");
    }

    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    LOGD("Running inference with prompt: %s", promptStr);
    LOGD("Max tokens: %d", maxTokens);

    auto start = std::chrono::high_resolution_clock::now();

    // Tokenize the input prompt
    std::vector<llama_token> tokens_list;
    const int n_prompt_max = 4096;
    tokens_list.resize(n_prompt_max);
    
    int n_tokens = llama_tokenize(
        state->model,
        promptStr,
        strlen(promptStr),
        tokens_list.data(),
        tokens_list.size(),
        true,   // add_special
        true    // parse_special
    );
    
    if (n_tokens < 0) {
        LOGE("Tokenization failed: %d", n_tokens);
        env->ReleaseStringUTFChars(prompt, promptStr);
        return env->NewStringUTF("Error: Tokenization failed");
    }
    
    tokens_list.resize(n_tokens);
    LOGD("Tokenized prompt: %d tokens", n_tokens);

    // Clear previous KV cache
    llama_kv_cache_clear(state->ctx);

    // Decode the prompt
    state->batch.n_tokens = n_tokens;
    for (int i = 0; i < n_tokens; i++) {
        state->batch.token[i] = tokens_list[i];
        state->batch.pos[i] = i;
        state->batch.n_seq_id[i] = 1;
        state->batch.seq_id[i][0] = 0;
        state->batch.logits[i] = false;
    }
    state->batch.logits[n_tokens - 1] = true;  // Need logits for last token

    if (llama_decode(state->ctx, state->batch) != 0) {
        LOGE("Failed to decode prompt");
        env->ReleaseStringUTFChars(prompt, promptStr);
        return env->NewStringUTF("Error: Decode failed");
    }

    // Generate response tokens
    std::string response;
    int n_predict = std::min(maxTokens, state->n_ctx - n_tokens - 1);
    int n_past = n_tokens;
    
    llama_token eos_token = llama_token_eos(state->model);
    
    // Sampling parameters
    llama_sampling_params sparams;
    sparams.temp = state->temperature;
    sparams.top_k = state->top_k;
    sparams.top_p = state->top_p;
    sparams.penalty_repeat = state->repetition_penalty;
    sparams.penalty_freq = 0.0f;
    sparams.penalty_present = 0.0f;

    for (int i = 0; i < n_predict; i++) {
        // Sample the next token
        llama_token new_token_id;
        
        // Simple greedy sampling for speed (can be enhanced with llama_sampling)
        auto logits = llama_get_logits(state->ctx);
        auto n_vocab = llama_vocab_n_tokens(llama_get_model(state->ctx));
        
        // Apply temperature
        std::vector<std::pair<float, int>> scores;
        scores.reserve(n_vocab);
        for (int j = 0; j < n_vocab; j++) {
            scores.push_back({logits[j], j});
        }
        
        // Sort by score (descending)
        std::sort(scores.begin(), scores.end(), 
            [](const std::pair<float, int>& a, const std::pair<float, int>& b) {
                return a.first > b.first;
            });
        
        // Top-K filtering
        if (state->top_k > 0 && state->top_k < n_vocab) {
            scores.resize(state->top_k);
        }
        
        // Top-P (nucleus) filtering
        if (state->top_p > 0.0f && state->top_p < 1.0f) {
            float cumulative_prob = 0.0f;
            size_t cutoff = scores.size();
            for (size_t j = 0; j < scores.size(); j++) {
                // Apply softmax
                float prob = std::exp(scores[j].first / state->temperature);
                cumulative_prob += prob;
                if (cumulative_prob > state->top_p) {
                    cutoff = j + 1;
                    break;
                }
            }
            scores.resize(cutoff);
        }
        
        // Sample from remaining candidates
        float sum = 0.0f;
        for (auto& s : scores) {
            s.first = std::exp(s.first / state->temperature);
            sum += s.first;
        }
        for (auto& s : scores) {
            s.first /= sum;
        }
        
        // Random sampling
        float r = static_cast<float>(rand()) / RAND_MAX;
        cumulative_prob = 0.0f;
        new_token_id = scores.back().second;
        for (const auto& s : scores) {
            cumulative_prob += s.first;
            if (r < cumulative_prob) {
                new_token_id = s.second;
                break;
            }
        }

        // Check for end of sequence
        if (new_token_id == eos_token) {
            LOGD("EOS token reached");
            break;
        }

        // Decode the token to text
        std::string piece(256, '\0');
        int piece_len = llama_token_to_piece(
            state->ctx, 
            new_token_id, 
            &piece[0], 
            piece.size(),
            0,
            true
        );
        if (piece_len > 0) {
            piece.resize(piece_len);
            response += piece;
        }

        // Prepare next batch
        n_past++;
        state->batch.n_tokens = 1;
        state->batch.token[0] = new_token_id;
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

    env->ReleaseStringUTFChars(prompt, promptStr);

    auto end = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
    float tokens_per_sec = response.length() > 0 ? 
        (response.length() / 4.0f) / (duration.count() / 1000.0f) : 0;
    
    LOGI("Inference completed in %lld ms (%.1f tokens/s)", duration.count(), tokens_per_sec);
    LOGD("Response: %s", response.c_str());

    return env->NewStringUTF(response.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_egyptian_agent_llm_LlamaEngine_inferNativeStreaming(
    JNIEnv *env,
    jobject thiz,
    jlong contextPtr,
    jstring prompt,
    jint maxTokens,
    jobject callback
) {
    LlamaContextState* state = getStateFromPtr(contextPtr);
    
    if (state == nullptr || state->model == nullptr || state->ctx == nullptr) {
        LOGE("Invalid context or model");
        jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(exceptionClass, "Model not initialized");
        return;
    }

    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    LOGD("Running streaming inference with prompt: %s", promptStr);

    // Get callback methods
    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    jmethodID onCompleteMethod = env->GetMethodID(callbackClass, "onComplete", "(Ljava/lang/String;)V");
    jmethodID onErrorMethod = env->GetMethodID(callbackClass, "onError", "(Ljava/lang/Exception;)V");

    // Tokenize the input prompt
    std::vector<llama_token> tokens_list;
    const int n_prompt_max = 4096;
    tokens_list.resize(n_prompt_max);
    
    int n_tokens = llama_tokenize(
        state->model,
        promptStr,
        strlen(promptStr),
        tokens_list.data(),
        tokens_list.size(),
        true,
        true
    );
    
    if (n_tokens < 0) {
        LOGE("Tokenization failed");
        env->ReleaseStringUTFChars(prompt, promptStr);
        return;
    }
    tokens_list.resize(n_tokens);

    // Clear KV cache
    llama_kv_cache_clear(state->ctx);

    // Decode prompt
    state->batch.n_tokens = n_tokens;
    for (int i = 0; i < n_tokens; i++) {
        state->batch.token[i] = tokens_list[i];
        state->batch.pos[i] = i;
        state->batch.n_seq_id[i] = 1;
        state->batch.seq_id[i][0] = 0;
        state->batch.logits[i] = false;
    }
    state->batch.logits[n_tokens - 1] = true;

    if (llama_decode(state->ctx, state->batch) != 0) {
        LOGE("Failed to decode prompt");
        env->ReleaseStringUTFChars(prompt, promptStr);
        return;
    }

    // Generate tokens with streaming
    std::string fullResponse;
    int n_predict = std::min(maxTokens, state->n_ctx - n_tokens - 1);
    int n_past = n_tokens;
    llama_token eos_token = llama_token_eos(state->model);

    for (int i = 0; i < n_predict; i++) {
        // Sample next token (simplified sampling)
        auto logits = llama_get_logits(state->ctx);
        auto n_vocab = llama_vocab_n_tokens(llama_get_model(state->ctx));
        
        std::vector<std::pair<float, int>> scores;
        scores.reserve(n_vocab);
        for (int j = 0; j < n_vocab; j++) {
            scores.push_back({logits[j], j});
        }
        std::sort(scores.begin(), scores.end(), 
            [](const auto& a, const auto& b) { return a.first > b.first; });
        
        if (state->top_k > 0) {
            scores.resize(std::min((size_t)state->top_k, scores.size()));
        }
        
        // Greedy sampling for speed in streaming
        llama_token new_token_id = scores[0].second;

        if (new_token_id == eos_token) break;

        // Decode token to text
        std::string piece(256, '\0');
        int piece_len = llama_token_to_piece(state->ctx, new_token_id, &piece[0], piece.size(), 0, true);
        
        if (piece_len > 0) {
            piece.resize(piece_len);
            fullResponse += piece;
            
            // Callback with token
            jstring tokenStr = env->NewStringUTF(piece.c_str());
            env->CallVoidMethod(callback, onTokenMethod, tokenStr);
            env->DeleteLocalRef(tokenStr);
        }

        // Check for exception
        if (env->ExceptionCheck()) {
            LOGE("Exception in callback");
            env->ReleaseStringUTFChars(prompt, promptStr);
            return;
        }

        // Prepare next batch
        n_past++;
        state->batch.n_tokens = 1;
        state->batch.token[0] = new_token_id;
        state->batch.pos[0] = n_past;
        state->batch.n_seq_id[0] = 1;
        state->batch.seq_id[0][0] = 0;
        state->batch.logits[0] = true;

        if (llama_decode(state->ctx, state->batch) != 0) {
            break;
        }
    }

    env->ReleaseStringUTFChars(prompt, promptStr);

    // Callback with complete response
    jstring completeStr = env->NewStringUTF(fullResponse.c_str());
    env->CallVoidMethod(callback, onCompleteMethod, completeStr);
    env->DeleteLocalRef(completeStr);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_egyptian_agent_llm_LlamaEngine_unloadLlamaNative(
    JNIEnv *env,
    jobject thiz,
    jlong contextPtr
) {
    LOGI("Unloading Llama native engine...");
    
    LlamaContextState* state = getStateFromPtr(contextPtr);
    
    if (state != nullptr) {
        delete state;
        llama_backend_free();
        LOGI("✓ Llama engine unloaded");
    }
}

#else
// ============================================================================
// MOCK IMPLEMENTATION (when llama.cpp is not available)
// ============================================================================
// This provides fallback behavior when the native llama.cpp library is not built

static bool g_initialized = false;
static std::string g_model_path = "";

extern "C"
JNIEXPORT jlong JNICALL
Java_com_egyptian_agent_llm_LlamaEngine_initLlamaNative(
    JNIEnv *env,
    jobject thiz,
    jstring modelPath,
    jint contextSize,
    jint numThreads,
    jfloat temperature,
    jint topK,
    jfloat topP,
    jfloat repetitionPenalty
) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("[MOCK] Initializing Llama (mock mode): %s", path);
    
    g_model_path = std::string(path);
    g_initialized = true;
    
    env->ReleaseStringUTFChars(modelPath, path);
    
    // Return a non-zero pointer to indicate "success"
    // In mock mode, we use a sentinel value
    return 0xDEADBEEF;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_egyptian_agent_llm_LlamaEngine_inferNative(
    JNIEnv *env,
    jobject thiz,
    jlong contextPtr,
    jstring prompt,
    jint maxTokens
) {
    if (!g_initialized) {
        return env->NewStringUTF("Model not initialized");
    }

    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    std::string input(promptStr);
    env->ReleaseStringUTFChars(prompt, promptStr);

    // Mock Egyptian Arabic responses based on input patterns
    std::string response;
    
    if (input.find("ازيك") != std::string::npos || input.find("عامل ايه") != std::string::npos) {
        response = "أنا بخير، الحمد لله. أقدر أساعدك إيه؟";
    } else if (input.find("شكرا") != std::string::npos) {
        response = "عفواً، أنا هنا للمساعدة.";
    } else if (input.find("مع السلامة") != std::string::npos || input.find("باي") != std::string::npos) {
        response = "مع السلامة، في أمان الله.";
    } else if (input.find("اتصل") != std::string::npos || input.find("كلم") != std::string::npos) {
        response = "تمام، جاري الاتصال...";
    } else if (input.find("واتساب") != std::string::npos) {
        response = "تمام، جاري إرسال الواتساب...";
    } else if (input.find("نبهني") != std::string::npos || input.find("ذكرني") != std::string::npos) {
        response = "تمام، ضبطت المنبه.";
    } else {
        response = "ممكن توضحلي أكتر عشان أقدر أساعدك؟";
    }

    // Simulate processing delay
    std::this_thread::sleep_for(std::chrono::milliseconds(200));

    return env->NewStringUTF(response.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_egyptian_agent_llm_LlamaEngine_inferNativeStreaming(
    JNIEnv *env,
    jobject thiz,
    jlong contextPtr,
    jstring prompt,
    jint maxTokens,
    jobject callback
) {
    if (!g_initialized) {
        jclass exceptionClass = env->FindClass("java/lang/RuntimeException");
        env->ThrowNew(exceptionClass, "Model not initialized");
        return;
    }

    // Get callback methods
    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    jmethodID onCompleteMethod = env->GetMethodID(callbackClass, "onComplete", "(Ljava/lang/String;)V");

    const char* promptStr = env->GetStringUTFChars(prompt, nullptr);
    std::string input(promptStr);
    env->ReleaseStringUTFChars(prompt, promptStr);

    std::string response = "رد تجريبي - النموذج في وضع المحاكاة";
    
    // Stream character by character (mock)
    for (size_t i = 0; i < response.length(); i += 3) {
        std::string token = response.substr(i, std::min((size_t)3, response.length() - i));
        jstring tokenStr = env->NewStringUTF(token.c_str());
        env->CallVoidMethod(callback, onTokenMethod, tokenStr);
        env->DeleteLocalRef(tokenStr);
        std::this_thread::sleep_for(std::chrono::milliseconds(50));
    }

    // Complete callback
    jstring completeStr = env->NewStringUTF(response.c_str());
    env->CallVoidMethod(callback, onCompleteMethod, completeStr);
    env->DeleteLocalRef(completeStr);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_egyptian_agent_llm_LlamaEngine_unloadLlamaNative(
    JNIEnv *env,
    jobject thiz,
    jlong contextPtr
) {
    LOGI("[MOCK] Unloading Llama (mock mode)");
    g_initialized = false;
    g_model_path = "";
}
#endif
