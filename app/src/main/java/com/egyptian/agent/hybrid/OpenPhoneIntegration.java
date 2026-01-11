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

/**
 * Updated OpenPhoneIntegration to use Llama 3.2 3B as the primary model
 * Maintains backward compatibility with the OpenPhone model as fallback
 */
public class OpenPhoneIntegration {
    private static final String TAG = "OpenPhoneIntegration";
    private static final int MODEL_LOAD_TIMEOUT = 30000; // 30 seconds
    private static final float MIN_CONFIDENCE_THRESHOLD = 0.65f;

    private LlamaModelIntegration llamaModel;  // Primary model
    private OpenPhoneModel legacyModel;        // Fallback model
    private ExecutorService inferenceExecutor;
    private boolean isLlamaModelLoaded = false;
    private boolean isLegacyModelLoaded = false;
    private long lastInferenceTime = 0;

    public OpenPhoneIntegration(Context context) {
        inferenceExecutor = Executors.newSingleThreadExecutor();

        // Load the primary Llama model in the background
        llamaModel = new LlamaModelIntegration(context);
        
        // Load the legacy OpenPhone model as fallback
        new Thread(() -> {
            try {
                Log.i(TAG, "Loading legacy OpenPhone-3B model as fallback...");
                legacyModel = new OpenPhoneModel(context, "openphone-3b");
                isLegacyModelLoaded = legacyModel.isReady();
                Log.i(TAG, "OpenPhone-3B fallback model loaded successfully: " + isLegacyModelLoaded);

                // Check memory constraints for the device
                MemoryOptimizer.checkMemoryConstraints(context);

            } catch (Exception e) {
                Log.e(TAG, "Failed to load OpenPhone fallback model", e);
                CrashLogger.logError(context, e);
                // Continue with just the Llama model
            }
        }).start();
    }

    /**
     * Analyzes text using the Llama model as primary, with OpenPhone as fallback
     * @param normalizedText The normalized text to analyze
     * @param callback Callback for the result
     */
    public void analyzeText(String normalizedText, AnalysisCallback callback) {
        // Check if Llama model is ready first
        if (llamaModel.isReady()) {
            // Use the Llama model
            llamaModel.analyzeText(normalizedText, new LlamaModelIntegration.AnalysisCallback() {
                @Override
                public void onResult(IntentResult result) {
                    callback.onResult(result);
                }

                @Override
                public void onFallbackRequired(String reason) {
                    Log.d(TAG, "Llama model requires fallback: " + reason);
                    // Try the legacy model as fallback
                    if (isLegacyModelLoaded && legacyModel.isReady()) {
                        analyzeWithLegacyModel(normalizedText, callback);
                    } else {
                        callback.onFallbackRequired(reason);
                    }
                }
            });
        } else if (isLegacyModelLoaded && legacyModel.isReady()) {
            // If Llama isn't ready, try the legacy model
            analyzeWithLegacyModel(normalizedText, callback);
        } else {
            // Both models unavailable
            Log.w(TAG, "Both models not loaded yet, using fallback");
            callback.onFallbackRequired("Both models still loading");
        }
    }

    /**
     * Analyzes text using the legacy OpenPhone model
     */
    private void analyzeWithLegacyModel(String normalizedText, AnalysisCallback callback) {
        // Apply rate limiting similar to the original implementation
        if (System.currentTimeMillis() - lastInferenceTime < 1000) {
            Log.w(TAG, "Rate limiting active");
            callback.onFallbackRequired("High request frequency");
            return;
        }

        inferenceExecutor.execute(() -> {
            try {
                lastInferenceTime = System.currentTimeMillis();

                // Apply Egyptian enhancements before sending to the model
                String enhancedText = applyEgyptianEnhancements(normalizedText);

                // Run the legacy model
                long startTime = System.currentTimeMillis();
                Map<String, Object> result = legacyModel.analyze(enhancedText);
                long endTime = System.currentTimeMillis();

                Log.i(TAG, String.format("Legacy model inference completed in %d ms", endTime - startTime));

                // Check result confidence
                Float confidence = (Float) result.get("confidence");
                if (confidence == null || confidence < MIN_CONFIDENCE_THRESHOLD) {
                    callback.onFallbackRequired("Low confidence: " + (confidence != null ? confidence : 0.0f));
                    return;
                }

                // Parse the model result
                IntentResult intentResult = parseModelResult(result);

                // Apply Egyptian post-processing
                applyEgyptianPostProcessing(intentResult);

                callback.onResult(intentResult);

            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Out of memory during legacy model inference", e);
                MemoryOptimizer.freeMemory();
                callback.onFallbackRequired("Memory constraints");
            } catch (Exception e) {
                Log.e(TAG, "Error during legacy model inference", e);
                callback.onFallbackRequired("Processing error");
            }
        });
    }

    private String applyEgyptianEnhancements(String text) {
        // Enhance text with Egyptian rules before sending to the model
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

    private IntentResult parseModelResult(Map<String, Object> modelResult) {
        // Convert OpenPhone result to our intent system
        IntentResult result = new IntentResult();

        // Determine intent type
        String intentStr = (String) modelResult.get("intent");
        if (intentStr == null) intentStr = "UNKNOWN";
        result.setIntentType(IntentType.fromOpenPhoneString(intentStr));

        // Extract entities
        @SuppressWarnings("unchecked")
        Map<String, String> entities = (Map<String, String>) modelResult.get("entities");
        if (entities != null) {
            for (Map.Entry<String, String> entry : entities.entrySet()) {
                result.setEntity(entry.getKey(), entry.getValue());
            }
        }

        // Set confidence level
        Float confidence = (Float) modelResult.get("confidence");
        if (confidence != null) {
            result.setConfidence(confidence);
        } else {
            result.setConfidence(0.7f);
        }

        return result;
    }

    public boolean isReady() {
        // Return true if either model is ready
        return llamaModel.isReady() || (isLegacyModelLoaded && legacyModel.isReady());
    }

    public void destroy() {
        if (llamaModel != null) {
            llamaModel.destroy();
        }
        if (legacyModel != null) {
            legacyModel.unload();
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