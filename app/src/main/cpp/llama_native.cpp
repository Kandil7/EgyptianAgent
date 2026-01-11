#include <jni.h>
#include <string>
#include <thread>
#include <vector>
#include <sstream>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include "llama.h"

// Global variables to hold model and context
static llama_model* g_model = nullptr;
static llama_context* g_ctx = nullptr;
static bool g_initialized = false;

extern "C"
JNIEXPORT jint JNICALL
Java_com_egyptian_agent_hybrid_LlamaNative_init(JNIEnv *env, jobject thiz, jstring model_path, jobject asset_manager) {
    if (g_initialized) {
        llama_free(g_ctx);
        llama_free_model(g_model);
    }

    const char* path = env->GetStringUTFChars(model_path, 0);
    
    // Initialize llama
    llama_backend_init(false);

    // Load the model
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // Use CPU for compatibility
    
    g_model = llama_load_model_from_file(path, model_params);
    if (g_model == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "LlamaNative", "Failed to load model: %s", path);
        env->ReleaseStringUTFChars(model_path, path);
        return -1;
    }

    // Create context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.seed = 1234;
    ctx_params.n_ctx = 2048; // Context size
    ctx_params.n_threads = 4; // Number of threads
    ctx_params.n_threads_batch = 4;

    g_ctx = llama_new_context_with_model(g_model, ctx_params);
    if (g_ctx == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "LlamaNative", "Failed to create context");
        llama_free_model(g_model);
        env->ReleaseStringUTFChars(model_path, path);
        return -1;
    }

    g_initialized = true;
    env->ReleaseStringUTFChars(model_path, path);
    
    __android_log_print(ANDROID_LOG_INFO, "LlamaNative", "Model loaded successfully: %s", path);
    return 0;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_egyptian_agent_hybrid_LlamaNative_infer(JNIEnv *env, jobject thiz, jstring prompt, jint max_tokens) {
    if (!g_initialized || g_model == NULL || g_ctx == NULL) {
        return env->NewStringUTF("Model not initialized");
    }

    const char* prompt_str = env->GetStringUTFChars(prompt, 0);
    
    // Tokenize the input
    std::vector<llama_token> tokens_list;
    tokens_list.resize(strlen(prompt_str) * 4); // Rough estimate
    
    int n_tokens = llama_tokenize(g_model, prompt_str, tokens_list.data(), tokens_list.size(), true);
    if (n_tokens < 0) {
        tokens_list.resize(-n_tokens);
        n_tokens = llama_tokenize(g_model, prompt_str, tokens_list.data(), tokens_list.size(), true);
        if (n_tokens < 0) {
            env->ReleaseStringUTFChars(prompt, prompt_str);
            return env->NewStringUTF("Error tokenizing input");
        }
    }
    tokens_list.resize(n_tokens);

    // Evaluate the input
    if (llama_decode(g_ctx, llama_batch_get_one(tokens_list.data(), tokens_list.size())) != 0) {
        env->ReleaseStringUTFChars(prompt, prompt_str);
        return env->NewStringUTF("Error evaluating input");
    }

    // Generate response
    std::string response = "";
    for (int i = 0; i < max_tokens; i++) {
        // Sample the next token
        llama_token_data_array candidates_p = { llama_get_logits_ith(g_ctx, -1), 1, false };
        llama_token next_token = llama_sample_token(g_ctx, &candidates_p);

        // Check if it's an end of sequence token
        if (next_token == llama_token_eos(g_model)) {
            break;
        }

        // Decode the token to text
        std::string token_str = llama_token_to_piece(g_ctx, next_token);
        response += token_str;

        // Prepare the next batch
        llama_batch batch = llama_batch_get_one(&next_token, 1);
        
        // Evaluate the next token
        if (llama_decode(g_ctx, batch) != 0) {
            break;
        }
    }

    env->ReleaseStringUTFChars(prompt, prompt_str);
    return env->NewStringUTF(response.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_egyptian_agent_hybrid_LlamaNative_unload(JNIEnv *env, jobject thiz) {
    if (g_initialized) {
        if (g_ctx) {
            llama_free(g_ctx);
            g_ctx = nullptr;
        }
        if (g_model) {
            llama_free_model(g_model);
            g_model = nullptr;
        }
        llama_backend_free();
        g_initialized = false;
    }
}