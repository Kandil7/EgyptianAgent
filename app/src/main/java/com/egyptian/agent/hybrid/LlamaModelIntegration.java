package com.egyptian.agent.hybrid;

import android.content.Context;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.MemoryOptimizer;
import com.egyptian.agent.utils.CrashLogger;
import org.json.JSONObject;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Integration class for Llama 3.2 3B model
 * Replaces or supplements the existing OpenPhone model with Llama
 */
public class LlamaModelIntegration {
    private static final String TAG = "LlamaModelIntegration";
    private static final int MODEL_LOAD_TIMEOUT = 30000; // 30 seconds
    private static final float MIN_CONFIDENCE_THRESHOLD = 0.65f;

    private ExecutorService inferenceExecutor;
    private boolean isModelLoaded = false;
    private long lastInferenceTime = 0;
    private Context context;

    public LlamaModelIntegration(Context context) {
        this.context = context;
        inferenceExecutor = Executors.newSingleThreadExecutor();

        // Load the Llama model in the background
        new Thread(() -> {
            try {
                Log.i(TAG, "Loading Llama 3.2 3B Q4_K_M model...");
                
                // Initialize the Llama model
                int result = LlamaNative.initializeModel(context, "llama-3.2-3b-Q4_K_M.gguf");
                
                if (result == 0) {
                    isModelLoaded = true;
                    Log.i(TAG, "Llama 3.2 3B model loaded successfully");
                    
                    // Verify model with a simple test
                    String testResponse = LlamaNative.infer("Question: What is your name? Answer:", 32);
                    Log.d(TAG, "Model test response: " + testResponse);
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
     * Analyzes text using the Llama model
     * @param normalizedText The normalized text to analyze
     * @param callback Callback for the result
     */
    public void analyzeText(String normalizedText, AnalysisCallback callback) {
        if (!isModelLoaded) {
            Log.w(TAG, "Llama model not loaded yet, using fallback");
            callback.onFallbackRequired("Model still loading");
            return;
        }

        // Rate limiting to prevent overloading the model
        if (System.currentTimeMillis() - lastInferenceTime < 1000) {
            Log.w(TAG, "Rate limiting active");
            callback.onFallbackRequired("High request frequency");
            return;
        }

        inferenceExecutor.execute(() -> {
            try {
                lastInferenceTime = System.currentTimeMillis();

                // Enhance the text with Egyptian context before sending to the model
                String enhancedText = applyEgyptianEnhancements(normalizedText);
                
                // Format the prompt for the Llama model
                String prompt = formatPromptForLlama(enhancedText);

                // Run inference
                long startTime = System.currentTimeMillis();
                String rawResponse = LlamaNative.infer(prompt, 128); // Limit response length
                long endTime = System.currentTimeMillis();

                Log.i(TAG, String.format("Llama inference completed in %d ms", endTime - startTime));
                Log.d(TAG, "Raw Llama response: " + rawResponse);

                // Parse the model response
                IntentResult intentResult = parseLlamaResponse(rawResponse, enhancedText);

                // Apply Egyptian post-processing rules
                applyEgyptianPostProcessing(intentResult);

                // Check confidence threshold
                if (intentResult.getConfidence() < MIN_CONFIDENCE_THRESHOLD) {
                    Log.w(TAG, "Low confidence result: " + intentResult.getConfidence());
                    callback.onFallbackRequired("Low confidence: " + intentResult.getConfidence());
                    return;
                }

                callback.onResult(intentResult);

            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Out of memory during Llama inference", e);
                MemoryOptimizer.freeMemory();
                callback.onFallbackRequired("Memory constraints");
            } catch (Exception e) {
                Log.e(TAG, "Error during Llama inference", e);
                callback.onFallbackRequired("Processing error: " + e.getMessage());
            }
        });
    }

    /**
     * Formats the input text as a prompt for the Llama model
     * @param text The input text
     * @return Formatted prompt
     */
    private String formatPromptForLlama(String text) {
        // Create a structured prompt for intent classification
        return "Egyptian Arabic Voice Assistant. Classify the following command into one of these categories: " +
               "CALL_CONTACT, SEND_WHATSAPP, SET_ALARM, READ_TIME, READ_MISSED_CALLS, EMERGENCY, UNKNOWN. " +
               "Provide the response in JSON format with 'intent' and 'entities' fields. " +
               "Command: \"" + text + "\". Response:";
    }

    /**
     * Applies Egyptian dialect enhancements to the text
     * @param text Input text
     * @return Enhanced text
     */
    private String applyEgyptianEnhancements(String text) {
        // Apply Egyptian dialect normalization and context enhancement
        return EgyptianNormalizer.enhanceWithEgyptianContext(text);
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
            String contactName = result.getEntity("contact", "");
            if (!contactName.isEmpty()) {
                result.setEntity("contact", EgyptianNormalizer.normalizeContactName(contactName));
            }
        }
    }

    /**
     * Parses the Llama model response into an IntentResult
     * @param rawResponse Raw response from the model
     * @param originalText Original input text
     * @return Parsed IntentResult
     */
    private IntentResult parseLlamaResponse(String rawResponse, String originalText) {
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
     * Calculates a default confidence score based on response quality
     * @param response Model response
     * @param originalText Original input
     * @return Confidence score
     */
    private float calculateDefaultConfidence(String response, String originalText) {
        // Base confidence
        float confidence = 0.6f;
        
        // Increase confidence if response contains relevant keywords
        if (response.toLowerCase().contains("call") || 
            response.toLowerCase().contains("contact") ||
            response.toLowerCase().contains("اتصل") ||
            response.toLowerCase().contains("كلم")) {
            confidence += 0.1f;
        }
        
        // Increase confidence if response is not too generic
        if (!response.toLowerCase().contains("unknown") && 
            !response.toLowerCase().contains("not sure") &&
            !response.toLowerCase().contains("غير معروف")) {
            confidence += 0.1f;
        }
        
        // Cap at 0.95
        return Math.min(confidence, 0.95f);
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
        return isModelLoaded;
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
    }

    /**
     * Callback interface for analysis results
     */
    public interface AnalysisCallback {
        void onResult(IntentResult result);
        void onFallbackRequired(String reason);
    }
}