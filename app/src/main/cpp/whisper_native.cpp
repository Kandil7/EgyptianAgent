#include <jni.h>
#include <string>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>

#define TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#ifdef USE_FASTER_WHISPER
#include "whisper.h"
#endif

extern "C" {

// Global variables to hold model and context
static struct whisper_context *g_context = nullptr;
static bool g_model_loaded = false;

JNIEXPORT jint JNICALL
Java_com_egyptian_agent_hybrid_WhisperEngine_init(JNIEnv *env, jclass clazz, jstring modelPath) {
#ifndef USE_FASTER_WHISPER
    LOGE("Faster-whisper not compiled in. Please build with faster-whisper support.");
    return -1;
#else
    if (g_context != nullptr) {
        whisper_free(g_context);
    }

    const char *model_path = env->GetStringUTFChars(modelPath, nullptr);
    if (model_path == nullptr) {
        LOGE("Failed to get model path string");
        return -1;
    }

    LOGI("Loading Whisper model from: %s", model_path);

    g_context = whisper_init_from_file(model_path);
    env->ReleaseStringUTFChars(modelPath, model_path);

    if (g_context == nullptr) {
        LOGE("Failed to load Whisper model");
        return -1;
    }

    g_model_loaded = true;
    LOGI("Whisper model loaded successfully");
    return 0;
#endif
}

JNIEXPORT jstring JNICALL
Java_com_egyptian_agent_hybrid_WhisperEngine_transcribeNative(JNIEnv *env, jclass clazz, 
                                                              jstring audioPath, jstring modelPath) {
#ifndef USE_FASTER_WHISPER
    LOGE("Faster-whisper not compiled in. Returning mock response.");
    return env->NewStringUTF("Whisper not available in this build");
#else
    if (!g_model_loaded || g_context == nullptr) {
        LOGE("Whisper model not loaded");
        return env->NewStringUTF("");
    }

    const char *audio_path = env->GetStringUTFChars(audioPath, nullptr);
    if (audio_path == nullptr) {
        LOGE("Failed to get audio path string");
        return env->NewStringUTF("");
    }

    const char *model_path = env->GetStringUTFChars(modelPath, nullptr);
    if (model_path == nullptr) {
        env->ReleaseStringUTFChars(audioPath, audio_path);
        LOGE("Failed to get model path string");
        return env->NewStringUTF("");
    }

    // Load audio file - in a real implementation, this would use a proper audio loading library
    // For now, we'll use a simplified approach
    std::string result = "Mock transcription result for: " + std::string(audio_path);

    // Release strings
    env->ReleaseStringUTFChars(audioPath, audio_path);
    env->ReleaseStringUTFChars(modelPath, model_path);

    // In a real implementation, we would:
    // 1. Load the audio file
    // 2. Convert to the appropriate format (typically 16kHz mono PCM)
    // 3. Run the Whisper transcription
    // 4. Return the result

    // For demonstration purposes, we'll return a mock result
    // In a real implementation, this would be the actual transcription
    return env->NewStringUTF(result.c_str());
#endif
}

JNIEXPORT void JNICALL
Java_com_egyptian_agent_hybrid_WhisperEngine_unload(JNIEnv *env, jclass clazz) {
#ifndef USE_FASTER_WHISPER
    LOGI("Whisper unload called (mock implementation)");
#else
    if (g_context != nullptr) {
        whisper_free(g_context);
        g_context = nullptr;
        g_model_loaded = false;
        LOGI("Whisper model unloaded");
    }
#endif
}

} // extern "C"