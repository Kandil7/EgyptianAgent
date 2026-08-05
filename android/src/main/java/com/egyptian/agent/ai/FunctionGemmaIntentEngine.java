package com.egyptian.agent.ai;

import android.content.Context;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.llm.FunctionGemmaConfig;
import com.egyptian.agent.llm.FunctionGemmaEngine;
import com.egyptian.agent.nlu.IntentResult;
import com.egyptian.agent.nlu.IntentType;
import com.egyptian.agent.nlu.EgyptianNormalizer;
import com.egyptian.agent.utils.CrashLogger;
import com.egyptian.agent.utils.MemoryOptimizer;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FunctionGemma Intent Classification Engine
 *
 * Production-grade intent classification engine for Egyptian Agent.
 * Replaces LlamaIntentEngine with FunctionGemma-270M-IT for faster,
 * more efficient intent classification.
 *
 * Integration:
 * - Works with EgyptianWhisperASR for speech-to-text
 * - Outputs IntentResult matching existing structure
 * - Falls back to EgyptianNormalizer.classifyBasicIntent()
 *
 * Performance Targets (Honor X6c - Helio G81 Ultra):
 * - Model load time: <5 seconds
 * - Intent classification: <500ms
 * - Memory usage: ~550MB
 * - Accuracy: 95%+ on Egyptian dialect
 * - Confidence threshold: 0.85
 *
 * Features:
 * - Egyptian dialect optimization
 * - Function calling format integration
 * - Streaming support for async processing
 * - Comprehensive error handling
 * - Performance logging
 * - Thread-safe operations
 *
 * @author Egyptian Agent Team
 * @version 1.0.0
 */
public class FunctionGemmaIntentEngine {

    private static final String TAG = "FunctionGemmaIntentEngine";

    // ========================================================================
    // Configuration Constants
    // ========================================================================

    /** Minimum confidence threshold for valid intent */
    private static final float MIN_CONFIDENCE_THRESHOLD = 0.85f;

    /** Model load timeout in milliseconds */
    private static final long MODEL_LOAD_TIMEOUT_MS = 5000;

    /** Maximum tokens for intent classification */
    private static final int MAX_TOKENS = 128;

    // ========================================================================
    // Instance Variables
    // ========================================================================

    private final Context context;
    private final FunctionGemmaConfig config;
    private final FunctionGemmaEngine functionGemmaEngine;
    private final EgyptianWhisperASR whisperASR;

    private ExecutorService inferenceExecutor;
    private ExecutorService modelLoadExecutor;
    private AtomicBoolean isReady;
    private AtomicBoolean isDestroyed;
    private AtomicBoolean isLoading;

    // Performance metrics
    private long modelLoadTimeMs;
    private long lastProcessingTimeMs;
    private int totalProcessed;
    private long totalProcessingTimeMs;
    private int successfulClassifications;
    private int fallbackCount;

    // Callback for async intent processing
    public interface IntentCallback {
        void onIntent(IntentResult result);
        void onError(Exception error);
    }

    // ========================================================================
    // Constructors
    // ========================================================================

    /**
     * Create FunctionGemma intent engine with default configuration.
     */
    public FunctionGemmaIntentEngine(Context context) {
        this(context, FunctionGemmaConfig.builder().build());
    }

    /**
     * Create FunctionGemma intent engine with custom configuration.
     */
    public FunctionGemmaIntentEngine(Context context, FunctionGemmaConfig config) {
        this.context = context.getApplicationContext();
        this.config = config;

        this.inferenceExecutor = Executors.newSingleThreadExecutor();
        this.modelLoadExecutor = Executors.newSingleThreadExecutor();
        this.isReady = new AtomicBoolean(false);
        this.isDestroyed = new AtomicBoolean(false);
        this.isLoading = new AtomicBoolean(false);

        this.modelLoadTimeMs = 0;
        this.lastProcessingTimeMs = 0;
        this.totalProcessed = 0;
        this.totalProcessingTimeMs = 0;
        this.successfulClassifications = 0;
        this.fallbackCount = 0;

        // Initialize Whisper ASR
        this.whisperASR = new EgyptianWhisperASR(context);

        // Initialize FunctionGemma engine
        this.functionGemmaEngine = new FunctionGemmaEngine(context, config);

        // Monitor model loading
        monitorModelLoading();

        Log.i(TAG, "FunctionGemmaIntentEngine initialized");
    }

    /**
     * Monitor model loading status.
     */
    private void monitorModelLoading() {
        modelLoadExecutor.execute(() -> {
            long startTime = System.currentTimeMillis();
            long timeout = MODEL_LOAD_TIMEOUT_MS;

            while (!functionGemmaEngine.isReady() && !functionGemmaEngine.isLoading()) {
                try {
                    Thread.sleep(100);

                    if (System.currentTimeMillis() - startTime > timeout) {
                        Log.w(TAG, "Model loading timeout after " + timeout + "ms");
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Wait for model to be ready
            while (!functionGemmaEngine.isReady() && !isDestroyed.get()) {
                try {
                    Thread.sleep(100);

                    if (System.currentTimeMillis() - startTime > timeout * 2) {
                        Log.w(TAG, "Model loading timeout after " + (timeout * 2) + "ms");
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            modelLoadTimeMs = System.currentTimeMillis() - startTime;
            isReady.set(functionGemmaEngine.isReady() && whisperASR.isReady());

            if (isReady.get()) {
                Log.i(TAG, "✓ FunctionGemmaIntentEngine ready");
                Log.i(TAG, "  Model load time: " + modelLoadTimeMs + "ms");
                Log.i(TAG, "  Memory usage: ~" + functionGemmaEngine.getEstimatedMemoryUsageMB() + "MB");
            } else {
                Log.w(TAG, "⚠ FunctionGemmaIntentEngine using fallback mode");
            }
        });
    }

    // ========================================================================
    // Speech Processing (Audio Input)
    // ========================================================================

    /**
     * Process Egyptian speech using Whisper ASR + FunctionGemma Intent Classification.
     * Blocking call - use processEgyptianSpeechAsync for non-blocking.
     *
     * @param audioPath Path to the audio file
     * @return IntentResult with parsed command
     */
    public IntentResult processEgyptianSpeech(String audioPath) {
        long startTime = System.currentTimeMillis();

        try {
            // Check if model is ready
            if (!functionGemmaEngine.isReady()) {
                Log.w(TAG, "FunctionGemma not loaded, using fallback");
                fallbackCount++;
                return fallbackProcessing(audioPath);
            }

            // 1. Whisper Egyptian ASR
            String egyptianText = whisperASR.transcribe(audioPath);
            Log.d(TAG, "Whisper ASR result: " + egyptianText);

            // 2. Apply Egyptian dialect normalization
            String normalizedText = EgyptianNormalizer.normalize(egyptianText);
            Log.d(TAG, "Normalized text: " + normalizedText);

            // 3. FunctionGemma Intent Classification
            IntentResult result = classifyIntent(normalizedText);

            // 4. Apply Egyptian-specific post-processing
            applyEgyptianPostProcessing(result);

            // Record metrics
            lastProcessingTimeMs = System.currentTimeMillis() - startTime;
            totalProcessed++;
            totalProcessingTimeMs += lastProcessingTimeMs;

            if (result.isValid()) {
                successfulClassifications++;
            }

            Log.d(TAG, "Processed speech in " + lastProcessingTimeMs + "ms: " + result);
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error processing Egyptian speech", e);
            CrashLogger.logError(context, e);
            fallbackCount++;
            return fallbackProcessing(audioPath);
        }
    }

    /**
     * Process Egyptian speech asynchronously with callback.
     *
     * @param audioPath Path to the audio file
     * @param callback Callback for result
     */
    public void processEgyptianSpeechAsync(String audioPath, IntentCallback callback) {
        inferenceExecutor.execute(() -> {
            try {
                IntentResult result = processEgyptianSpeech(audioPath);
                if (callback != null) {
                    callback.onIntent(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in async speech processing", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    // ========================================================================
    // Text Processing (Text Input)
    // ========================================================================

    /**
     * Classify intent from text input.
     * Blocking call - use classifyIntentAsync for non-blocking.
     *
     * @param text Input text (Egyptian Arabic)
     * @return IntentResult with classified intent
     */
    public IntentResult classifyIntent(String text) {
        long startTime = System.currentTimeMillis();

        try {
            if (!functionGemmaEngine.isReady()) {
                Log.w(TAG, "FunctionGemma not loaded, using fallback");
                fallbackCount++;
                return EgyptianNormalizer.classifyBasicIntent(text);
            }

            // Process with FunctionGemma
            FunctionGemmaEngine.FunctionCallResult functionResult =
                functionGemmaEngine.processCommand(text);

            // Convert function call to IntentResult
            IntentResult intentResult = convertFunctionCallToIntent(functionResult, text);

            // Check confidence threshold
            if (intentResult.getConfidence() < MIN_CONFIDENCE_THRESHOLD) {
                Log.d(TAG, "Confidence below threshold (" + intentResult.getConfidence() +
                      " < " + MIN_CONFIDENCE_THRESHOLD + "), using fallback");
                fallbackCount++;
                return EgyptianNormalizer.classifyBasicIntent(text);
            }

            lastProcessingTimeMs = System.currentTimeMillis() - startTime;
            totalProcessed++;
            totalProcessingTimeMs += lastProcessingTimeMs;

            if (intentResult.isValid()) {
                successfulClassifications++;
            }

            return intentResult;

        } catch (Exception e) {
            Log.e(TAG, "Error classifying intent", e);
            CrashLogger.logError(context, e);
            fallbackCount++;
            return EgyptianNormalizer.classifyBasicIntent(text);
        }
    }

    /**
     * Classify intent from text input asynchronously.
     *
     * @param text Input text (Egyptian Arabic)
     * @param callback Callback for result
     */
    public void classifyIntentAsync(String text, IntentCallback callback) {
        inferenceExecutor.execute(() -> {
            try {
                IntentResult result = classifyIntent(text);
                if (callback != null) {
                    callback.onIntent(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in async intent classification", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    // ========================================================================
    // Function Call to Intent Conversion
    // ========================================================================

    /**
     * Convert FunctionGemma function call to IntentResult.
     */
    private IntentResult convertFunctionCallToIntent(
            FunctionGemmaEngine.FunctionCallResult functionResult,
            String originalText) {

        IntentResult result = new IntentResult();
        result.setOriginalText(originalText);
        result.setConfidence(functionResult.getConfidence());

        // Map function names to IntentType
        IntentType intentType = mapFunctionToIntentType(functionResult.getFunctionName());
        result.setIntentType(intentType);

        // Map function arguments to entities
        Map<String, String> entities = functionResult.getArguments();
        for (Map.Entry<String, String> entry : entities.entrySet()) {
            result.setEntity(entry.getKey(), entry.getValue());
        }

        // Additional entity extraction based on intent type
        extractAdditionalEntities(result, originalText);

        return result;
    }

    /**
     * Map function name to IntentType.
     */
    private IntentType mapFunctionToIntentType(String functionName) {
        if (functionName == null) return IntentType.UNKNOWN;

        switch (functionName.toLowerCase()) {
            case "call_contact":
                return IntentType.CALL_CONTACT;
            case "send_whatsapp":
                return IntentType.SEND_WHATSAPP;
            case "send_voice_message":
                return IntentType.SEND_VOICE_MESSAGE;
            case "send_sms":
                return IntentType.SEND_SMS;
            case "set_alarm":
                return IntentType.SET_ALARM;
            case "read_time":
                return IntentType.READ_TIME;
            case "read_missed_calls":
                return IntentType.READ_MISSED_CALLS;
            case "emergency":
                return IntentType.EMERGENCY;
            case "open_app":
                return IntentType.OPEN_APP;
            case "toggle_wifi":
                return IntentType.TOGGLE_WIFI;
            case "toggle_bluetooth":
                return IntentType.TOGGLE_BLUETOOTH;
            case "toggle_flashlight":
                return IntentType.TOGGLE_FLASHLIGHT;
            case "weather_query":
                return IntentType.WEATHER_QUERY;
            case "greeting":
                return IntentType.GREETING;
            case "thank_you":
                return IntentType.THANK_YOU;
            case "goodbye":
                return IntentType.GOODBYE;
            default:
                return IntentType.UNKNOWN;
        }
    }

    /**
     * Extract additional entities from original text.
     */
    private void extractAdditionalEntities(IntentResult result, String originalText) {
        IntentType intentType = result.getIntentType();

        // Extract contact name for communication intents
        if (intentType == IntentType.CALL_CONTACT ||
            intentType == IntentType.SEND_WHATSAPP ||
            intentType == IntentType.SEND_VOICE_MESSAGE ||
            intentType == IntentType.SEND_SMS) {

            String contactName = result.getEntity("contact_name");
            if (contactName == null || contactName.isEmpty()) {
                contactName = EgyptianNormalizer.normalizeContactName(
                    extractContactFromText(originalText));
                result.setEntity("contact_name", contactName);
            }
        }

        // Extract time for alarm intents
        if (intentType == IntentType.SET_ALARM) {
            String time = result.getEntity("time");
            if (time == null || time.isEmpty() || time.equals("unknown")) {
                time = EgyptianNormalizer.normalizeTimeExpression(
                    extractTimeFromText(originalText));
                result.setEntity("time", time);
            }
        }
    }

    /**
     * Extract contact name from text.
     */
    private String extractContactFromText(String text) {
        String[] contactKeywords = {"ماما", "بابا", "أحمد", "محمد", "مريم", "فاطمة",
            "علي", "حسن", "حسين", "نور", "سارة", "خالد", "سامي", "هاني",
            "تيتا", "تيتو", "خالو", "عمو", "حبيبي", "حبيبتي"};

        for (String keyword : contactKeywords) {
            if (text.contains(keyword)) {
                return keyword;
            }
        }

        String[] words = text.split("\\s+");
        if (words.length > 0) {
            return words[words.length - 1].replaceAll("[^\\u0600-\\u06FF\\s]", "");
        }

        return "";
    }

    /**
     * Extract time expression from text.
     */
    private String extractTimeFromText(String text) {
        if (text.contains("بكرة")) return "بكرة";
        if (text.contains("النهاردة")) return "النهاردة";
        if (text.contains("بعد ساعة")) return "بعد ساعة";
        if (text.contains("بعد نص ساعة")) return "بعد نص ساعة";
        if (text.contains("الصبح")) return "الصبح";
        if (text.contains("الضهر")) return "الضهر";
        if (text.contains("العصر")) return "العصر";
        if (text.contains("المغرب")) return "المغرب";
        if (text.contains("العشا")) return "العشا";

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + ":00";
        }

        return "";
    }

    // ========================================================================
    // Post-Processing
    // ========================================================================

    /**
     * Apply Egyptian-specific post-processing rules.
     */
    private void applyEgyptianPostProcessing(IntentResult result) {
        if (result == null) return;

        // Apply Egyptian normalizer post-processing
        EgyptianNormalizer.applyPostProcessingRules(result);

        // Normalize contact names
        if (result.getEntity("contact_name") != null) {
            String contact = result.getEntity("contact_name");
            result.setEntity("contact_name", EgyptianNormalizer.normalizeContactName(contact));
        }

        // Normalize time expressions
        if (result.getEntity("time") != null) {
            String time = result.getEntity("time");
            result.setEntity("time", EgyptianNormalizer.normalizeTimeExpression(time));
        }

        // Boost confidence for high-priority intents
        IntentType intentType = result.getIntentType();
        if (intentType == IntentType.EMERGENCY && result.getConfidence() < 0.95f) {
            result.setConfidence(0.95f);
        }
    }

    // ========================================================================
    // Fallback Processing
    // ========================================================================

    /**
     * Fallback processing when FunctionGemma is unavailable.
     * Uses Whisper ASR + EgyptianNormalizer rule-based classification.
     */
    private IntentResult fallbackProcessing(String audioPath) {
        try {
            // Use Whisper ASR
            String egyptianText = whisperASR.transcribe(audioPath);
            Log.d(TAG, "Fallback ASR result: " + egyptianText);

            // Use rule-based classification
            IntentResult result = EgyptianNormalizer.classifyBasicIntent(egyptianText);
            result.setOriginalText(egyptianText);

            Log.d(TAG, "Fallback classification: " + result);
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error in fallback processing", e);

            // Return unknown intent as last resort
            IntentResult result = new IntentResult();
            result.setIntentType(IntentType.UNKNOWN);
            result.setConfidence(0.0f);
            return result;
        }
    }

    // ========================================================================
    // Status and Metrics
    // ========================================================================

    /**
     * Check if engine is ready.
     */
    public boolean isReady() {
        return isReady.get();
    }

    /**
     * Check if engine is currently loading.
     */
    public boolean isLoading() {
        return isLoading.get();
    }

    /**
     * Check if engine is destroyed.
     */
    public boolean isDestroyed() {
        return isDestroyed.get();
    }

    /**
     * Get model load time in milliseconds.
     */
    public long getModelLoadTimeMs() {
        return modelLoadTimeMs;
    }

    /**
     * Get last processing time in milliseconds.
     */
    public long getLastProcessingTimeMs() {
        return lastProcessingTimeMs;
    }

    /**
     * Get average processing time in milliseconds.
     */
    public long getAverageProcessingTimeMs() {
        if (totalProcessed == 0) return 0;
        return totalProcessingTimeMs / totalProcessed;
    }

    /**
     * Get total number of processed commands.
     */
    public int getTotalProcessed() {
        return totalProcessed;
    }

    /**
     * Get number of successful classifications.
     */
    public int getSuccessfulClassifications() {
        return successfulClassifications;
    }

    /**
     * Get number of fallback invocations.
     */
    public int getFallbackCount() {
        return fallbackCount;
    }

    /**
     * Get success rate.
     */
    public float getSuccessRate() {
        if (totalProcessed == 0) return 0;
        return (float) successfulClassifications / totalProcessed;
    }

    /**
     * Get fallback rate.
     */
    public float getFallbackRate() {
        if (totalProcessed == 0) return 0;
        return (float) fallbackCount / totalProcessed;
    }

    /**
     * Get estimated memory usage in MB.
     */
    public long getEstimatedMemoryUsageMB() {
        return functionGemmaEngine.getEstimatedMemoryUsageMB();
    }

    /**
     * Get performance summary.
     */
    public String getPerformanceSummary() {
        return String.format(
            "FunctionGemmaIntentEngine Performance:\n" +
            "  Ready: %s\n" +
            "  Model load time: %dms\n" +
            "  Total processed: %d\n" +
            "  Successful: %d (%.1f%%)\n" +
            "  Fallback: %d (%.1f%%)\n" +
            "  Avg processing: %dms\n" +
            "  Last processing: %dms\n" +
            "  Memory: ~%dMB",
            isReady() ? "Yes" : "No",
            modelLoadTimeMs,
            totalProcessed,
            successfulClassifications,
            getSuccessRate() * 100,
            fallbackCount,
            getFallbackRate() * 100,
            getAverageProcessingTimeMs(),
            lastProcessingTimeMs,
            getEstimatedMemoryUsageMB()
        );
    }

    // ========================================================================
    // Cleanup
    // ========================================================================

    /**
     * Clean up resources.
     */
    public void destroy() {
        if (isDestroyed.get()) return;

        Log.d(TAG, "Destroying FunctionGemmaIntentEngine...");

        if (functionGemmaEngine != null) {
            functionGemmaEngine.destroy();
        }

        if (whisperASR != null) {
            whisperASR.cleanup();
        }

        if (inferenceExecutor != null) {
            inferenceExecutor.shutdownNow();
            inferenceExecutor = null;
        }

        if (modelLoadExecutor != null) {
            modelLoadExecutor.shutdownNow();
            modelLoadExecutor = null;
        }

        isReady.set(false);
        isDestroyed.set(true);

        Log.d(TAG, "FunctionGemmaIntentEngine destroyed");
    }

    // ========================================================================
    // Builder Pattern
    // ========================================================================

    /**
     * Create a builder for FunctionGemmaIntentEngine.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for FunctionGemmaIntentEngine.
     */
    public static class Builder {
        private Context context;
        private FunctionGemmaConfig config;

        public Builder context(Context context) {
            this.context = context;
            return this;
        }

        public Builder config(FunctionGemmaConfig config) {
            this.config = config;
            return this;
        }

        public FunctionGemmaIntentEngine build() {
            if (context == null) {
                throw new IllegalStateException("Context is required");
            }
            if (config == null) {
                config = FunctionGemmaConfig.builder().build();
            }
            return new FunctionGemmaIntentEngine(context, config);
        }
    }
}
