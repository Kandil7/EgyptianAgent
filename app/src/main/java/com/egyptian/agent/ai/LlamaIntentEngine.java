package com.egyptian.agent.ai;

import android.content.Context;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.MemoryOptimizer;
import com.egyptian.agent.utils.CrashLogger;
import org.json.JSONObject;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Advanced Intent Engine using Llama 3.2 3B for Egyptian dialect understanding
 * Integrates Whisper Egyptian ASR for speech-to-text conversion
 */
public class LlamaIntentEngine {
    private static final String TAG = "LlamaIntentEngine";
    private static final int MODEL_LOAD_TIMEOUT = 30000; // 30 seconds
    private static final float MIN_CONFIDENCE_THRESHOLD = 0.85f; // Higher threshold for accuracy

    private ExecutorService inferenceExecutor;
    private boolean isModelLoaded = false;
    private long lastInferenceTime = 0;
    private Context context;
    private EgyptianWhisperASR whisperASR;

    public LlamaIntentEngine(Context context) {
        this.context = context;
        inferenceExecutor = Executors.newSingleThreadExecutor();
        this.whisperASR = new EgyptianWhisperASR(context);

        // Load the Llama model in the background
        new Thread(() -> {
            try {
                Log.i(TAG, "Loading Llama 3.2 3B Q4_K_M model for Egyptian dialect...");

                // Initialize the Llama model
                int result = LlamaNative.initializeModel(context, "llama-3.2-3b-Q4_K_M.gguf");

                if (result == 0) {
                    isModelLoaded = true;
                    Log.i(TAG, "Llama 3.2 3B model loaded successfully");

                    // Verify model with a simple Egyptian dialect test
                    String testResponse = LlamaNative.infer("Egyptian Arabic: Classify this command: \"يا حكيم اتصل بماما\". Intent:", 64);
                    Log.d(TAG, "Model test response: " + testResponse);
                    
                    // Warm up the model
                    warmUpModel();
                } else {
                    Log.e(TAG, "Failed to load Llama model, result: " + result);
                    TTSManager.speak(context, "تحذير: مشكلة في تحميل نموذج لاما. بستخدم الإعدادات الافتراضية");
                }

                // Check memory constraints for the device
                MemoryOptimizer.checkMemoryConstraints(context);

            } catch (Exception e) {
                Log.e(TAG, "Failed to load Llama model", e);
                CrashLogger.logError(context, e);
                TTSManager.speak(context, "حصل مشكلة في تشغيل نموذج لاما. المزايا الأساسية شغالة");
            }
        }).start();
    }

    /**
     * Process Egyptian speech using Whisper ASR + Llama Intent Classification
     * @param audioPath Path to the audio file
     * @return IntentResult with parsed command
     */
    public IntentResult processEgyptianSpeech(String audioPath) {
        if (!isModelLoaded) {
            Log.w(TAG, "Llama model not loaded yet, using fallback");
            return fallbackProcessing(audioPath);
        }

        try {
            // 1. Whisper Egyptian ASR
            String egyptianText = whisperASR.transcribe(audioPath);
            Log.d(TAG, "Whisper ASR result: " + egyptianText);

            // Apply Egyptian dialect normalization
            String normalizedText = EgyptianNormalizer.normalize(egyptianText);
            
            // 2. Llama 3.2 3B Intent Classification
            String prompt = createClassificationPrompt(normalizedText);
            String intentJson = LlamaNative.infer(prompt, 128);
            
            Log.d(TAG, "Llama classification result: " + intentJson);

            // 3. Parse + Execute
            IntentResult result = parseIntent(intentJson, normalizedText);
            
            // Apply Egyptian-specific post-processing
            applyEgyptianPostProcessing(result);
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error processing Egyptian speech", e);
            return fallbackProcessing(audioPath);
        }
    }

    /**
     * Creates a prompt for Llama to classify Egyptian dialect commands
     * @param text The input text to classify
     * @return Formatted prompt for Llama
     */
    private String createClassificationPrompt(String text) {
        return "Egyptian Arabic Voice Assistant. Classify the following command into one of these categories: " +
               "CALL_PERSON, SEND_WHATSAPP, SEND_VOICE_MESSAGE, SET_ALARM, READ_TIME, READ_MISSED_CALLS, EMERGENCY, UNKNOWN. " +
               "Provide the response in JSON format with 'intent', 'entities' (person_name, time, message), and 'confidence' fields. " +
               "Command: \"" + text + "\". Response:";
    }

    /**
     * Parses the Llama model response into an IntentResult
     * @param rawResponse Raw response from the model
     * @param originalText Original input text
     * @return Parsed IntentResult
     */
    private IntentResult parseIntent(String rawResponse, String originalText) {
        IntentResult result = new IntentResult();

        try {
            // Try to parse as JSON first
            if (rawResponse.trim().startsWith("{") && rawResponse.trim().endsWith("}")) {
                JSONObject jsonResponse = new JSONObject(rawResponse.trim());

                // Extract intent
                String intentStr = jsonResponse.optString("intent", "UNKNOWN");
                result.setIntentType(com.egyptian.agent.nlp.IntentType.fromOpenPhoneString(intentStr));

                // Extract entities
                if (jsonResponse.has("entities")) {
                    JSONObject entities = jsonResponse.getJSONObject("entities");
                    for (String key : entities.keySet()) {
                        result.setEntity(key, entities.getString(key));
                    }
                }

                // Extract confidence if available
                if (jsonResponse.has("confidence")) {
                    result.setConfidence((float) jsonResponse.getDouble("confidence"));
                } else {
                    // Set a default confidence based on response quality
                    result.setConfidence(calculateDefaultConfidence(rawResponse, originalText));
                }
            } else {
                // If not JSON, try to extract intent from plain text
                result = extractIntentFromPlainText(rawResponse, originalText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing Llama response", e);
            // Fallback to plain text extraction
            result = extractIntentFromPlainText(rawResponse, originalText);
        }

        return result;
    }

    /**
     * Applies Egyptian post-processing rules to the result
     * @param result Intent result to modify
     */
    private void applyEgyptianPostProcessing(IntentResult result) {
        // Apply Egyptian-specific post-processing rules
        EgyptianNormalizer.applyPostProcessingRules(result);

        // Handle Egyptian contact names specially
        if (result.getIntentType() == com.egyptian.agent.nlp.IntentType.CALL_CONTACT ||
            result.getIntentType() == com.egyptian.agent.nlp.IntentType.SEND_WHATSAPP) {
            String contactName = result.getEntity("person_name", "");
            if (!contactName.isEmpty()) {
                result.setEntity("contact", EgyptianNormalizer.normalizeContactName(contactName));
            }
        }
    }

    /**
     * Warms up the model to ensure it's ready for inference
     */
    private void warmUpModel() {
        try {
            // Run a simple test to warm up the model
            String warmupPrompt = "Egyptian Arabic: Classify this command: \"اتصل بأمي\". Intent: CALL_PERSON";
            String warmupResult = LlamaNative.infer(warmupPrompt, 32);
            Log.d(TAG, "Model warmed up with result: " + warmupResult);
        } catch (Exception e) {
            Log.e(TAG, "Error warming up model", e);
        }
    }

    /**
     * Fallback processing when Llama model is not available
     * @param audioPath Path to the audio file
     * @return IntentResult from fallback processing
     */
    private IntentResult fallbackProcessing(String audioPath) {
        // Use Whisper ASR + basic rule-based processing
        String egyptianText = whisperASR.transcribe(audioPath);
        return EgyptianNormalizer.classifyBasicIntent(egyptianText);
    }

    /**
     * Calculates a default confidence score based on response quality
     * @param response Model response
     * @param originalText Original input
     * @return Confidence score
     */
    private float calculateDefaultConfidence(String response, String originalText) {
        // Base confidence
        float confidence = 0.7f;

        // Increase confidence if response contains relevant keywords
        if (response.toLowerCase().contains("call") ||
            response.toLowerCase().contains("contact") ||
            response.toLowerCase().contains("اتصل") ||
            response.toLowerCase().contains("كلم")) {
            confidence += 0.15f;
        }

        // Increase confidence if response is not too generic
        if (!response.toLowerCase().contains("unknown") &&
            !response.toLowerCase().contains("not sure") &&
            !response.toLowerCase().contains("غير معروف")) {
            confidence += 0.15f;
        }

        // Cap at 0.98
        return Math.min(confidence, 0.98f);
    }

    /**
     * Extracts intent from plain text response
     * @param response Plain text response
     * @param originalText Original input
     * @return IntentResult
     */
    private IntentResult extractIntentFromPlainText(String response, String originalText) {
        IntentResult result = new IntentResult();

        // Simple keyword-based intent detection as fallback
        String lowerResponse = response.toLowerCase();

        if (lowerResponse.contains("call") || lowerResponse.contains("contact") ||
            lowerResponse.contains("اتصل") || lowerResponse.contains("كلم")) {
            result.setIntentType(com.egyptian.agent.nlp.IntentType.CALL_CONTACT);
        } else if (lowerResponse.contains("whatsapp") || lowerResponse.contains("message") ||
                   lowerResponse.contains("واتساب") || lowerResponse.contains("رسالة")) {
            result.setIntentType(com.egyptian.agent.nlp.IntentType.SEND_WHATSAPP);
        } else if (lowerResponse.contains("alarm") || lowerResponse.contains("remind") ||
                   lowerResponse.contains("نبه") || lowerResponse.contains("ذكر")) {
            result.setIntentType(com.egyptian.agent.nlp.IntentType.SET_ALARM);
        } else if (lowerResponse.contains("time") || lowerResponse.contains("hour") ||
                   lowerResponse.contains("الوقت") || lowerResponse.contains("الساعة")) {
            result.setIntentType(com.egyptian.agent.nlp.IntentType.READ_TIME);
        } else if (lowerResponse.contains("emergency") || lowerResponse.contains("ngda") ||
                   lowerResponse.contains("استغاثة") || lowerResponse.contains("طوارئ")) {
            result.setIntentType(com.egyptian.agent.nlp.IntentType.EMERGENCY);
        } else {
            result.setIntentType(com.egyptian.agent.nlp.IntentType.UNKNOWN);
        }

        // Set a lower confidence for plain text extraction
        result.setConfidence(calculateDefaultConfidence(response, originalText) * 0.8f);

        return result;
    }

    /**
     * Checks if the model is ready for inference
     * @return True if loaded, false otherwise
     */
    public boolean isReady() {
        return isModelLoaded && whisperASR.isReady();
    }

    /**
     * Cleans up resources
     */
    public void destroy() {
        if (inferenceExecutor != null) {
            inferenceExecutor.shutdownNow();
        }

        // Unload the native model
        LlamaNative.unloadModel();
        
        // Clean up Whisper ASR
        whisperASR.cleanup();
    }
}