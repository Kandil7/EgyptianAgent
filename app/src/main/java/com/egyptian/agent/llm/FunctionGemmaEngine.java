package com.egyptian.agent.llm;

import android.content.Context;
import android.util.Log;

import com.egyptian.agent.utils.CrashLogger;
import com.egyptian.agent.utils.MemoryOptimizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FunctionGemma-270M-IT Core Inference Engine
 *
 * Production-grade AI inference engine for Egyptian Agent.
 * Implements function calling for Egyptian Arabic voice commands.
 *
 * Model: google/functiongemma-270m-it (Q4_K_M quantized)
 * Size: ~288MB
 * Context: 2048 tokens (optimized for mobile)
 * Memory: ~550MB RAM
 *
 * Performance Targets (Honor X6c - Helio G81 Ultra):
 * - Model load time: <5 seconds (vs 30s for Llama 3.2 3B)
 * - Inference time: <500ms per command
 * - Memory usage: ~550MB (vs 2GB for Llama)
 * - Accuracy: 95%+ on Egyptian dialect commands
 *
 * Features:
 * - Native function calling support
 * - Egyptian dialect optimization
 * - Streaming token callbacks
 * - JSON-only output contract
 * - Thread-safe operations
 * - Memory optimization
 * - Performance logging
 *
 * @author Egyptian Agent Team
 * @version 1.0.0
 */
public class FunctionGemmaEngine {

    private static final String TAG = "FunctionGemmaEngine";

    // ========================================================================
    // Static Initialization
    // ========================================================================

    static {
        try {
            // Load native library for llama.cpp backend
            System.loadLibrary("llama");
            Log.i(TAG, "Native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library", e);
            // Will use fallback mode
        }
    }

    // ========================================================================
    // Configuration Constants
    // ========================================================================

    /** Model file name */
    private static final String MODEL_FILE = "functiongemma-270m-it-Q4_K_M.gguf";

    /** Model size in MB */
    private static final int MODEL_SIZE_MB = 288;

    /** Context window size (optimized for mobile) */
    private static final int CONTEXT_SIZE = 2048;

    /** Number of CPU threads (smaller model needs fewer threads) */
    private static final int NUM_THREADS = 2;

    /** Temperature for deterministic function calling */
    private static final float TEMPERATURE = 0.1f;

    /** Top-K sampling */
    private static final int TOP_K = 40;

    /** Top-P (nucleus) sampling */
    private static final float TOP_P = 0.9f;

    /** Minimum memory required in MB */
    private static final int MIN_MEMORY_MB = 550;

    /** Model load timeout in milliseconds */
    private static final long MODEL_LOAD_TIMEOUT_MS = 5000;

    // ========================================================================
    // FunctionGemma Special Tokens
    // ========================================================================

    private static final String START_HEADER = "<|start_header_id|>";
    private static final String END_HEADER = "<|end_header_id|>";
    private static final String EOT_TOKEN = "<|eot_id|>";

    // ========================================================================
    // Instance Variables
    // ========================================================================

    private final Context context;
    private final FunctionGemmaConfig config;
    private final FunctionCallSchema functionSchema;

    private ExecutorService inferenceExecutor;
    private ExecutorService modelLoadExecutor;
    private AtomicBoolean isModelLoaded;
    private AtomicBoolean isDestroyed;
    private AtomicBoolean isLoading;

    private long nativeContext;
    private String systemPrompt;

    // Performance metrics
    private long modelLoadTimeMs;
    private long lastInferenceTimeMs;
    private int totalInferences;
    private long totalInferenceTimeMs;
    private int successfulInferences;

    // Callback interface for streaming tokens
    public interface TokenCallback {
        void onToken(String token);
        void onComplete(String fullResponse);
        void onError(Exception error);
    }

    // ========================================================================
    // Constructors
    // ========================================================================

    /**
     * Create FunctionGemma engine with default configuration.
     */
    public FunctionGemmaEngine(Context context) {
        this(context, FunctionGemmaConfig.builder().build());
    }

    /**
     * Create FunctionGemma engine with custom configuration.
     */
    public FunctionGemmaEngine(Context context, FunctionGemmaConfig config) {
        this.context = context.getApplicationContext();
        this.config = config;
        this.functionSchema = new FunctionCallSchema();

        this.inferenceExecutor = Executors.newSingleThreadExecutor();
        this.modelLoadExecutor = Executors.newSingleThreadExecutor();
        this.isModelLoaded = new AtomicBoolean(false);
        this.isDestroyed = new AtomicBoolean(false);
        this.isLoading = new AtomicBoolean(false);
        this.nativeContext = 0;
        this.modelLoadTimeMs = 0;
        this.lastInferenceTimeMs = 0;
        this.totalInferences = 0;
        this.totalInferenceTimeMs = 0;
        this.successfulInferences = 0;

        // Build system prompt
        this.systemPrompt = buildSystemPrompt();

        // Load model asynchronously
        loadModelAsync();
    }

    // ========================================================================
    // Model Loading
    // ========================================================================

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
            long startTime = System.currentTimeMillis();

            try {
                Log.i(TAG, "Loading FunctionGemma-270M-IT...");
                Log.i(TAG, "  Model: " + MODEL_FILE);
                Log.i(TAG, "  Size: " + MODEL_SIZE_MB + "MB");
                Log.i(TAG, "  Context: " + CONTEXT_SIZE + " tokens");
                Log.i(TAG, "  Threads: " + NUM_THREADS);

                // Check memory availability
                if (!MemoryOptimizer.hasEnoughMemory(MIN_MEMORY_MB)) {
                    Log.w(TAG, "Insufficient memory for FunctionGemma model (need " + MIN_MEMORY_MB + "MB+)");
                    CrashLogger.logError(context, new RuntimeException("Insufficient memory for FunctionGemma"));
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

                Log.i(TAG, "Initializing native FunctionGemma...");
                Log.i(TAG, "  Path: " + modelFile.getAbsolutePath());

                // Initialize native context with optimized parameters
                nativeContext = initFunctionGemmaNative(
                    modelFile.getAbsolutePath(),
                    CONTEXT_SIZE,
                    NUM_THREADS,
                    TEMPERATURE,
                    TOP_K,
                    TOP_P
                );

                if (nativeContext != 0) {
                    isModelLoaded.set(true);
                    modelLoadTimeMs = System.currentTimeMillis() - startTime;

                    Log.i(TAG, "✓ FunctionGemma-270M-IT loaded successfully");
                    Log.i(TAG, "  Load time: " + modelLoadTimeMs + "ms");
                    Log.i(TAG, "  Context pointer: 0x" + Long.toHexString(nativeContext));
                    Log.i(TAG, "  Memory estimate: ~" + getEstimatedMemoryUsageMB() + "MB");

                    // Warm up the model
                    warmUpModel();
                } else {
                    Log.e(TAG, "✗ Failed to load FunctionGemma - native init returned 0");
                    CrashLogger.logError(context, new RuntimeException("FunctionGemma native init failed"));
                }

            } catch (Exception e) {
                Log.e(TAG, "✗ Error loading FunctionGemma model", e);
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
            long lastProgressTime = System.currentTimeMillis();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                // Log progress every 50MB
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastProgressTime > 1000) {
                    Log.i(TAG, "Extracting model: " + (totalBytes / (1024 * 1024)) + "MB / " + MODEL_SIZE_MB + "MB...");
                    lastProgressTime = currentTime;
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
        if (!isModelLoaded.get() || nativeContext == 0) return;

        try {
            Log.d(TAG, "Warming up model...");
            String warmupPrompt = buildUserPrompt("الساعة كام");
            String result = inferNative(nativeContext, warmupPrompt, 64);
            Log.d(TAG, "Model warmed up: " + result);
        } catch (Exception e) {
            Log.e(TAG, "Error warming up model", e);
        }
    }

    // ========================================================================
    // Prompt Building
    // ========================================================================

    /**
     * Build system prompt for FunctionGemma with Egyptian dialect support.
     */
    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();

        prompt.append(START_HEADER).append("system").append(END_HEADER).append("\n");
        prompt.append("أنت مساعد صوتي مصري متخصص في مساعدة كبار السن والمكفوفين.\n");
        prompt.append("فهمك ممتاز للعامية المصرية واللهجة المصرية.\n");
        prompt.append("مهمتك هي فهم أوامر المستخدم وتحويلها لاستدعاءات دوال (function calls).\n");
        prompt.append("استخدم فقط الوظائف المتاحة لك ولا تخترع وظائف جديدة.\n");
        prompt.append("أجب بصيغة JSON فقط.\n\n");

        prompt.append("الوظائف المتاحة:\n");
        prompt.append("- call_contact(contact_name, phone_number) - Make a phone call\n");
        prompt.append("- send_whatsapp(contact_name, message) - Send WhatsApp message\n");
        prompt.append("- send_voice_message(contact_name) - Send voice message\n");
        prompt.append("- set_alarm(time, label) - Set alarm or reminder\n");
        prompt.append("- read_time() - Read current time\n");
        prompt.append("- emergency(type) - Trigger emergency protocol\n");
        prompt.append("- open_app(app_name) - Open application\n");
        prompt.append("- toggle_wifi(state) - Toggle WiFi\n");
        prompt.append("- toggle_bluetooth(state) - Toggle Bluetooth\n");
        prompt.append("- toggle_flashlight(state) - Toggle flashlight\n");
        prompt.append("- read_missed_calls() - Read missed calls\n");
        prompt.append("- send_sms(contact_name, message) - Send SMS\n");
        prompt.append("- weather_query(location, time) - Get weather\n");
        prompt.append("- greeting() - Handle greetings\n");
        prompt.append("- thank_you() - Handle thank you\n");
        prompt.append("- goodbye() - Handle goodbye\n");

        prompt.append("\nأمثلة:\n");
        prompt.append("- \"اتصل بماما\" → {\"function\": \"call_contact\", \"arguments\": {\"contact_name\": \"ماما\"}}\n");
        prompt.append("- \"ابعت واتساب لأحمد\" → {\"function\": \"send_whatsapp\", \"arguments\": {\"contact_name\": \"أحمد\", \"message\": \"...\"}}\n");
        prompt.append("- \"نبهني بكرة الصبح\" → {\"function\": \"set_alarm\", \"arguments\": {\"time\": \"بكرة الصبح\"}}\n");
        prompt.append("- \"الساعة كام\" → {\"function\": \"read_time\", \"arguments\": {}}\n");
        prompt.append("- \"يا نجدة\" → {\"function\": \"emergency\", \"arguments\": {\"type\": \"general\"}}\n");
        prompt.append("- \"افتح الواي فاي\" → {\"function\": \"toggle_wifi\", \"arguments\": {\"state\": \"on\"}}\n");

        prompt.append(EOT_TOKEN).append("\n");

        return prompt.toString();
    }

    /**
     * Build user prompt for inference.
     */
    private String buildUserPrompt(String userInput) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(START_HEADER).append("user").append(END_HEADER).append("\n");
        prompt.append(userInput).append("\n");
        prompt.append(EOT_TOKEN).append("\n");
        prompt.append(START_HEADER).append("assistant").append(END_HEADER).append("\n");
        return prompt.toString();
    }

    /**
     * Build full prompt with system context and user input.
     */
    private String buildFullPrompt(String userInput) {
        return systemPrompt + buildUserPrompt(userInput);
    }

    // ========================================================================
    // Inference Methods (Blocking)
    // ========================================================================

    /**
     * Process a voice command and get function call (blocking).
     */
    public FunctionCallResult processCommand(String userInput) {
        return processCommand(userInput, config.getMaxTokens());
    }

    /**
     * Process a voice command with token limit (blocking).
     */
    public FunctionCallResult processCommand(String userInput, int maxTokens) {
        if (!isModelLoaded.get()) {
            Log.w(TAG, "Model not loaded, using fallback");
            return processWithFallback(userInput);
        }

        long startTime = System.currentTimeMillis();

        try {
            // Build full prompt
            String prompt = buildFullPrompt(userInput);

            // Run inference
            String response = inferNative(nativeContext, prompt, maxTokens);

            // Parse function call
            FunctionCallResult result = parseFunctionCall(response, userInput);

            // Record metrics
            lastInferenceTimeMs = System.currentTimeMillis() - startTime;
            totalInferences++;
            totalInferenceTimeMs += lastInferenceTimeMs;

            if (result.isValid()) {
                successfulInferences++;
            }

            Log.d(TAG, "Processed command in " + lastInferenceTimeMs + "ms: " + result);
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error processing command", e);
            CrashLogger.logError(context, e);
            return processWithFallback(userInput);
        }
    }

    /**
     * Process command and return raw JSON response (blocking).
     */
    public String processCommandRaw(String userInput) {
        if (!isModelLoaded.get()) {
            return getFallbackJson(userInput);
        }

        try {
            String prompt = buildFullPrompt(userInput);
            return inferNative(nativeContext, prompt, config.getMaxTokens());
        } catch (Exception e) {
            Log.e(TAG, "Error in raw processing", e);
            return getFallbackJson(userInput);
        }
    }

    // ========================================================================
    // Inference Methods (Streaming)
    // ========================================================================

    /**
     * Process a voice command with streaming callback (async).
     */
    public void processCommandAsync(String userInput, TokenCallback callback) {
        processCommandAsync(userInput, config.getMaxTokens(), callback);
    }

    /**
     * Process a voice command with streaming callback and token limit (async).
     */
    public void processCommandAsync(String userInput, int maxTokens, TokenCallback callback) {
        if (!isModelLoaded.get()) {
            if (callback != null) {
                callback.onComplete(getFallbackJson(userInput));
            }
            return;
        }

        inferenceExecutor.execute(() -> {
            try {
                String prompt = buildFullPrompt(userInput);
                StringBuilder fullResponse = new StringBuilder();

                // Use streaming inference
                inferNativeStreaming(nativeContext, prompt, maxTokens, new TokenCallback() {
                    @Override
                    public void onToken(String token) {
                        fullResponse.append(token);
                        if (callback != null) {
                            callback.onToken(token);
                        }
                    }

                    @Override
                    public void onComplete(String response) {
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

            } catch (Exception e) {
                Log.e(TAG, "Error in async processing", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    // ========================================================================
    // Function Call Parsing
    // ========================================================================

    /**
     * Parse function call from model response.
     */
    private FunctionCallResult parseFunctionCall(String response, String originalInput) {
        try {
            // Clean response - extract JSON if present
            String jsonStr = extractJsonFromResponse(response);

            if (jsonStr != null && !jsonStr.isEmpty()) {
                // Parse JSON response
                return parseJsonFunctionCall(jsonStr, response);
            } else {
                // Try to parse function call from plain text
                return parseTextFunctionCall(response, originalInput);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing function call", e);
            return new FunctionCallResult("unknown", new HashMap<>(), 0.3f, response);
        }
    }

    /**
     * Extract JSON from model response.
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.isEmpty()) return null;

        // Look for JSON object
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }

        return null;
    }

    /**
     * Parse JSON function call.
     */
    private FunctionCallResult parseJsonFunctionCall(String jsonStr, String rawResponse) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(jsonStr);

            String functionName = json.optString("function", json.optString("name", "unknown"));
            Map<String, String> arguments = new HashMap<>();

            // Parse arguments
            if (json.has("arguments")) {
                org.json.JSONObject args = json.getJSONObject("arguments");
                java.util.Iterator<String> keys = args.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    arguments.put(key, args.getString(key));
                }
            } else if (json.has("parameters")) {
                org.json.JSONObject params = json.getJSONObject("parameters");
                java.util.Iterator<String> keys = params.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    arguments.put(key, params.getString(key));
                }
            }

            // Extract confidence if present
            float confidence = (float) json.optDouble("confidence", 0.9f);

            return new FunctionCallResult(functionName, arguments, confidence, rawResponse);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON function call", e);
            return new FunctionCallResult("unknown", new HashMap<>(), 0.3f, rawResponse);
        }
    }

    /**
     * Parse function call from plain text response.
     */
    private FunctionCallResult parseTextFunctionCall(String response, String originalInput) {
        String lowerResponse = response.toLowerCase();
        String lowerInput = originalInput.toLowerCase();
        Map<String, String> arguments = new HashMap<>();

        // Emergency detection (highest priority)
        if (lowerInput.contains("نجدة") || lowerInput.contains("استغاثة") ||
            lowerInput.contains("طوارئ") || lowerInput.contains("محتاج مساعدة")) {
            return new FunctionCallResult("emergency",
                Map.of("type", "general"), 0.95f, response);
        }

        // Call contact
        if (lowerInput.contains("اتصل") || lowerInput.contains("كلم") ||
            lowerInput.contains("رن") || lowerInput.contains("نادي")) {
            String contact = extractContactName(originalInput);
            arguments.put("contact_name", contact);
            return new FunctionCallResult("call_contact", arguments, 0.9f, response);
        }

        // WhatsApp
        if (lowerInput.contains("واتساب") || lowerInput.contains("ابعث") ||
            lowerInput.contains("رسالة واتساب")) {
            String contact = extractContactName(originalInput);
            String message = extractMessage(originalInput);
            arguments.put("contact_name", contact);
            arguments.put("message", message);
            return new FunctionCallResult("send_whatsapp", arguments, 0.9f, response);
        }

        // Voice message
        if (lowerInput.contains("رسالة صوتية") || lowerInput.contains("صوت")) {
            String contact = extractContactName(originalInput);
            arguments.put("contact_name", contact);
            return new FunctionCallResult("send_voice_message", arguments, 0.85f, response);
        }

        // Alarm
        if (lowerInput.contains("نبه") || lowerInput.contains("منبه") ||
            lowerInput.contains("ذكر") || lowerInput.contains("اضبط")) {
            String time = extractTime(originalInput);
            arguments.put("time", time);
            return new FunctionCallResult("set_alarm", arguments, 0.9f, response);
        }

        // Read time
        if (lowerInput.contains("الساعة") || lowerInput.contains("كام الوقت") ||
            lowerInput.contains("الوقت")) {
            return new FunctionCallResult("read_time", arguments, 0.95f, response);
        }

        // WiFi
        if (lowerInput.contains("واي فاي") || lowerInput.contains("wifi") ||
            lowerInput.contains("انترنت")) {
            String state = extractToggleState(originalInput);
            arguments.put("state", state);
            return new FunctionCallResult("toggle_wifi", arguments, 0.9f, response);
        }

        // Bluetooth
        if (lowerInput.contains("بلوتوث")) {
            String state = extractToggleState(originalInput);
            arguments.put("state", state);
            return new FunctionCallResult("toggle_bluetooth", arguments, 0.9f, response);
        }

        // Flashlight
        if (lowerInput.contains("فلاش") || lowerInput.contains("كشاف")) {
            String state = extractToggleState(originalInput);
            arguments.put("state", state);
            return new FunctionCallResult("toggle_flashlight", arguments, 0.9f, response);
        }

        // Open app
        if (lowerInput.contains("افتح") || lowerInput.contains("شغل")) {
            String appName = extractAppName(originalInput);
            arguments.put("app_name", appName);
            return new FunctionCallResult("open_app", arguments, 0.85f, response);
        }

        // Greeting
        if (lowerInput.contains("السلام") || lowerInput.contains("أهلاً") ||
            lowerInput.contains("ازيك") || lowerInput.contains("صباح")) {
            return new FunctionCallResult("greeting", arguments, 0.95f, response);
        }

        // Thank you
        if (lowerInput.contains("شكرا") || lowerInput.contains("متشكر") ||
            lowerInput.contains("تسلم")) {
            return new FunctionCallResult("thank_you", arguments, 0.95f, response);
        }

        // Goodbye
        if (lowerInput.contains("مع السلامة") || lowerInput.contains("باي") ||
            lowerInput.contains("سلام")) {
            return new FunctionCallResult("goodbye", arguments, 0.95f, response);
        }

        return new FunctionCallResult("unknown", arguments, 0.3f, response);
    }

    // ========================================================================
    // Fallback Processing
    // ========================================================================

    /**
     * Fallback processing when model is unavailable.
     */
    private FunctionCallResult processWithFallback(String userInput) {
        return parseTextFunctionCall("", userInput);
    }

    /**
     * Get fallback JSON response.
     */
    private String getFallbackJson(String userInput) {
        FunctionCallResult result = processWithFallback(userInput);
        try {
            org.json.JSONObject json = new org.json.JSONObject();
            json.put("function", result.functionName);

            org.json.JSONObject args = new org.json.JSONObject();
            for (Map.Entry<String, String> entry : result.arguments.entrySet()) {
                args.put(entry.getKey(), entry.getValue());
            }
            json.put("arguments", args);
            json.put("confidence", result.confidence);
            json.put("fallback", true);

            return json.toString();
        } catch (Exception e) {
            return "{\"function\": \"unknown\", \"arguments\": {}, \"fallback\": true}";
        }
    }

    // ========================================================================
    // Entity Extraction Helpers
    // ========================================================================

    /**
     * Extract contact name from input.
     */
    private String extractContactName(String input) {
        // Common Egyptian contact names
        String[] contactKeywords = {"ماما", "بابا", "أحمد", "محمد", "مريم", "فاطمة",
            "علي", "حسن", "حسين", "نور", "سارة", "خالد", "سامي", "هاني",
            "تيتا", "تيتو", "خالو", "عمو", "حبيبي", "حبيبتي"};

        for (String keyword : contactKeywords) {
            if (input.contains(keyword)) {
                return keyword;
            }
        }

        // Extract last word as contact name
        String[] words = input.split("\\s+");
        if (words.length > 0) {
            return words[words.length - 1].replaceAll("[^\\u0600-\\u06FF\\s]", "");
        }

        return "unknown";
    }

    /**
     * Extract message content from input.
     */
    private String extractMessage(String input) {
        int idx = input.indexOf("إن");
        if (idx < 0) idx = input.indexOf("ان");
        if (idx < 0) idx = input.indexOf("بإن");
        if (idx < 0) return "";

        return input.substring(idx + 2).trim();
    }

    /**
     * Extract time expression from input.
     */
    private String extractTime(String input) {
        if (input.contains("بكرة")) return "بكرة";
        if (input.contains("النهاردة")) return "النهاردة";
        if (input.contains("بعد ساعة")) return "بعد ساعة";
        if (input.contains("بعد نص ساعة")) return "بعد نص ساعة";
        if (input.contains("الصبح")) return "الصبح";
        if (input.contains("الضهر")) return "الضهر";
        if (input.contains("العصر")) return "العصر";
        if (input.contains("المغرب")) return "المغرب";
        if (input.contains("العشا")) return "العشا";

        // Try to extract numeric time
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1) + ":00";
        }

        return "unknown";
    }

    /**
     * Extract toggle state from input.
     */
    private String extractToggleState(String input) {
        if (input.contains("افتح") || input.contains("شغل") ||
            input.contains("على") || input.contains("نشط")) {
            return "on";
        }
        if (input.contains("اقفل") || input.contains("قفل") ||
            input.contains("اطفئ") || input.contains("وقف")) {
            return "off";
        }
        return "toggle";
    }

    /**
     * Extract app name from input.
     */
    private String extractAppName(String input) {
        String[] apps = {"واتساب", "فيسبوك", "يوتيوب", "انستغرام", "تيك توك",
            "تويتر", "سناب شات", "جوجل", "خرائط", "كلمات"};

        for (String app : apps) {
            if (input.contains(app)) {
                return app;
            }
        }

        // Check English app names
        if (input.toLowerCase().contains("whatsapp")) return "واتساب";
        if (input.toLowerCase().contains("facebook")) return "فيسبوك";
        if (input.toLowerCase().contains("youtube")) return "يوتيوب";

        return "unknown";
    }

    // ========================================================================
    // Status and Metrics
    // ========================================================================

    /**
     * Check if model is ready.
     */
    public boolean isReady() {
        return isModelLoaded.get() && nativeContext != 0;
    }

    /**
     * Check if model is currently loading.
     */
    public boolean isLoading() {
        return isLoading.get();
    }

    /**
     * Get model load time in milliseconds.
     */
    public long getModelLoadTimeMs() {
        return modelLoadTimeMs;
    }

    /**
     * Get last inference time in milliseconds.
     */
    public long getLastInferenceTimeMs() {
        return lastInferenceTimeMs;
    }

    /**
     * Get average inference time in milliseconds.
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
     * Get number of successful inferences.
     */
    public int getSuccessfulInferences() {
        return successfulInferences;
    }

    /**
     * Get success rate.
     */
    public float getSuccessRate() {
        if (totalInferences == 0) return 0;
        return (float) successfulInferences / totalInferences;
    }

    /**
     * Get estimated memory usage in MB.
     */
    public long getEstimatedMemoryUsageMB() {
        if (!isModelLoaded.get()) return 0;
        // FunctionGemma 270M Q4_K_M: ~288MB model + ~250MB runtime = ~550MB
        return 550;
    }

    /**
     * Get performance summary.
     */
    public String getPerformanceSummary() {
        return String.format(
            "FunctionGemma Performance:\n" +
            "  Model loaded: %s\n" +
            "  Load time: %dms\n" +
            "  Total inferences: %d\n" +
            "  Successful: %d (%.1f%%)\n" +
            "  Avg inference: %dms\n" +
            "  Last inference: %dms\n" +
            "  Memory: ~%dMB",
            isReady() ? "Yes" : "No",
            modelLoadTimeMs,
            totalInferences,
            successfulInferences,
            getSuccessRate() * 100,
            getAverageInferenceTimeMs(),
            lastInferenceTimeMs,
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

        Log.d(TAG, "Destroying FunctionGemma engine...");

        if (nativeContext != 0) {
            unloadFunctionGemmaNative(nativeContext);
            nativeContext = 0;
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

        Log.d(TAG, "FunctionGemma engine destroyed");
    }

    // ========================================================================
    // Native Methods (llama.cpp backend)
    // ========================================================================

    /**
     * Initialize native FunctionGemma context.
     *
     * @param modelPath Path to GGUF model file
     * @param contextSize Context window size (tokens)
     * @param numThreads Number of CPU threads
     * @param temperature Sampling temperature
     * @param topK Top-K sampling
     * @param topP Nucleus sampling (top-P)
     * @return Native context pointer (0 if failed)
     */
    private native long initFunctionGemmaNative(
        String modelPath,
        int contextSize,
        int numThreads,
        float temperature,
        int topK,
        float topP
    );

    /**
     * Run inference and return complete response.
     *
     * @param context Native context pointer
     * @param prompt Input prompt
     * @param maxTokens Maximum tokens to generate
     * @return Generated response text
     */
    private native String inferNative(long context, String prompt, int maxTokens);

    /**
     * Run inference with streaming token callback.
     *
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
     * Unload native FunctionGemma context and free resources.
     *
     * @param context Native context pointer
     */
    private native void unloadFunctionGemmaNative(long context);

    // ========================================================================
    // Inner Classes
    // ========================================================================

    /**
     * Function Call Result
     * Represents the result of processing a command.
     */
    public static class FunctionCallResult {
        public final String functionName;
        public final Map<String, String> arguments;
        public final float confidence;
        public final String rawOutput;

        public FunctionCallResult(String functionName, Map<String, String> arguments) {
            this(functionName, arguments, 1.0f, null);
        }

        public FunctionCallResult(String functionName, Map<String, String> arguments,
                                  float confidence, String rawOutput) {
            this.functionName = functionName;
            this.arguments = arguments;
            this.confidence = confidence;
            this.rawOutput = rawOutput;
        }

        public String getFunctionName() {
            return functionName;
        }

        public Map<String, String> getArguments() {
            return arguments;
        }

        public String getArgument(String key) {
            return arguments.get(key);
        }

        public String getArgument(String key, String defaultValue) {
            return arguments.getOrDefault(key, defaultValue);
        }

        public float getConfidence() {
            return confidence;
        }

        public String getRawOutput() {
            return rawOutput;
        }

        public boolean isValid() {
            return functionName != null && !functionName.isEmpty() &&
                   !functionName.equals("unknown") && confidence >= 0.5f;
        }

        @Override
        public String toString() {
            return "FunctionCallResult{function='" + functionName + "', args=" + arguments +
                   ", confidence=" + confidence + "}";
        }
    }
}
