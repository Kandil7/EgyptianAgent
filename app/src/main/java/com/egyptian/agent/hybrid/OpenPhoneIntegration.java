package com.egyptian.agent.hybrid;

import android.content.Context;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.MemoryOptimizer;
import com.egyptian.agent.utils.CrashLogger;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenPhoneIntegration {
    private static final String TAG = "OpenPhoneIntegration";
    private static final int MODEL_LOAD_TIMEOUT = 30000; // 30 seconds
    private static final float MIN_CONFIDENCE_THRESHOLD = 0.65f;

    private OpenPhoneModel localModel;
    private ExecutorService inferenceExecutor;
    private boolean isModelLoaded = false;
    private long lastInferenceTime = 0;
    private Context context;

    public OpenPhoneIntegration(Context context) {
        this.context = context;
        inferenceExecutor = Executors.newSingleThreadExecutor();

        // Load model in background
        new Thread(() -> {
            try {
                Log.i(TAG, "Loading OpenPhone-3B model...");
                localModel = new OpenPhoneModel(context.getAssets(), "model/openphone-3b");
                isModelLoaded = true;
                Log.i(TAG, "OpenPhone-3B model loaded successfully");

                // Check memory constraints
                MemoryOptimizer.checkMemoryConstraints(context);

            } catch (Exception e) {
                Log.e(TAG, "Failed to load OpenPhone model", e);
                CrashLogger.logError(context, e);
                TTSManager.speak(context, "حصل مشكلة في تشغيل الذكاء المتقدم. المزايا الأساسية شغالة");
            }
        }).start();
    }

    /**
     * Analyzes text using the local OpenPhone model
     * @param normalizedText the unified text after normalization
     * @param callback callback with the result
     */
    public void analyzeText(String normalizedText, AnalysisCallback callback) {
        if (!isModelLoaded) {
            Log.w(TAG, "Model not loaded yet, using fallback");
            callback.onFallbackRequired("Model still loading");
            return;
        }

        // Check rate limiting
        if (System.currentTimeMillis() - lastInferenceTime < 1000) {
            Log.w(TAG, "Rate limiting active");
            callback.onFallbackRequired("High request frequency");
            return;
        }

        inferenceExecutor.execute(() -> {
            try {
                lastInferenceTime = System.currentTimeMillis();

                // Apply additional Egyptian rules before sending to model
                String enhancedText = applyEgyptianEnhancements(normalizedText);

                // Run the model
                long startTime = System.currentTimeMillis();
                JSONObject result = localModel.analyze(enhancedText);
                long endTime = System.currentTimeMillis();

                Log.i(TAG, String.format("Inference completed in %d ms", endTime - startTime));

                // Check result confidence
                float confidence = result.optFloat("confidence", 0.0f);
                if (confidence < MIN_CONFIDENCE_THRESHOLD) {
                    callback.onFallbackRequired("Low confidence: " + confidence);
                    return;
                }

                // Convert result to unified format
                IntentResult intentResult = parseModelResult(result);

                // Apply Egyptian post-processing rules
                applyEgyptianPostProcessing(intentResult);

                callback.onResult(intentResult);

            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Out of memory during inference", e);
                MemoryOptimizer.freeMemory();
                callback.onFallbackRequired("Memory constraints");
            } catch (Exception e) {
                Log.e(TAG, "Error during inference", e);
                callback.onFallbackRequired("Processing error");
            }
        });
    }

    private String applyEgyptianEnhancements(String text) {
        // Enhance text with Egyptian context before sending to model
        return EgyptianNormalizer.enhanceWithEgyptianContext(text);
    }

    private void applyEgyptianPostProcessing(IntentResult result) {
        // Modify results based on Egyptian rules
        EgyptianNormalizer.applyPostProcessingRules(result);

        // Special handling for Egyptian contact names
        if (result.getIntentType() == IntentType.CALL_CONTACT ||
            result.getIntentType() == IntentType.SEND_WHATSAPP) {
            String contactName = result.getEntity("contact", "");
            if (!contactName.isEmpty()) {
                result.setEntity("contact", EgyptianNormalizer.normalizeContactName(contactName));
            }
        }
    }

    private IntentResult parseModelResult(JSONObject jsonResult) {
        // Convert OpenPhone result to our intent system
        IntentResult result = new IntentResult();

        // Determine intent type
        String intentStr = jsonResult.optString("intent", "UNKNOWN");
        result.setIntentType(IntentType.fromOpenPhoneString(intentStr));

        // Extract entities
        JSONObject entities = jsonResult.optJSONObject("entities");
        if (entities != null) {
            for (String key : entities.keySet()) {
                String value = entities.optString(key, "");
                result.setEntity(key, value);
            }
        }

        // Set confidence level
        result.setConfidence(jsonResult.optFloat("confidence", 0.7f));

        return result;
    }

    public boolean isReady() {
        return isModelLoaded;
    }

    public void destroy() {
        if (localModel != null) {
            localModel.unload();
        }
        if (inferenceExecutor != null) {
            inferenceExecutor.shutdownNow();
        }
    }

    public interface AnalysisCallback {
        void onResult(IntentResult result);
        void onFallbackRequired(String reason);
    }
}