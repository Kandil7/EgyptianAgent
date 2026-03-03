package com.egyptian.agent.llm;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * FunctionGemma Configuration
 *
 * Configuration class for FunctionGemma-270M-IT model settings.
 * Optimized for Egyptian Arabic dialect function calling on mobile devices.
 *
 * Performance Targets (Honor X6c - Helio G81 Ultra):
 * - Model load time: <5 seconds (vs 30s for Llama 3.2 3B)
 * - Inference time: <500ms per command
 * - Memory usage: ~550MB (vs 2GB for Llama)
 * - Context window: 2048 tokens (optimized for mobile)
 *
 * @author Egyptian Agent Team
 * @version 1.0.0
 */
public class FunctionGemmaConfig {

    private static final String TAG = "FunctionGemmaConfig";

    // ========================================================================
    // Default Configuration Constants
    // ========================================================================

    /** Model file name (GGUF quantized format) */
    public static final String DEFAULT_MODEL_FILE = "functiongemma-270m-it-Q4_K_M.gguf";

    /** Model size in MB (Q4_K_M quantization) */
    public static final int MODEL_SIZE_MB = 288;

    /** Context window size - optimized for mobile (FunctionGemma supports 32K but we use less for performance) */
    public static final int DEFAULT_CONTEXT_SIZE = 2048;

    /** Number of CPU threads - smaller model needs fewer threads */
    public static final int DEFAULT_NUM_THREADS = 2;

    /** Maximum tokens to generate for function calls */
    public static final int DEFAULT_MAX_TOKENS = 256;

    /** Minimum memory required in MB */
    public static final int MIN_MEMORY_MB = 550;

    /** Optimal memory available in MB */
    public static final int OPTIMAL_MEMORY_MB = 1000;

    /** Model load timeout in milliseconds */
    public static final long MODEL_LOAD_TIMEOUT_MS = 5000;

    // ========================================================================
    // Inference Parameters (Optimized for Function Calling)
    // ========================================================================

    /** Temperature - low for deterministic function calling output */
    public static final float TEMPERATURE = 0.1f;

    /** Top-K sampling - limited for focused output */
    public static final int TOP_K = 40;

    /** Top-P (nucleus) sampling */
    public static final float TOP_P = 0.9f;

    /** Repetition penalty to avoid loops */
    public static final float REPETITION_PENALTY = 1.1f;

    /** Keep special tokens in context */
    public static final int N_KEEP = 48;

    /** Confidence threshold for intent classification */
    public static final float CONFIDENCE_THRESHOLD = 0.85f;

    // ========================================================================
    // FunctionGemma Special Tokens
    // ========================================================================

    /** Start header token */
    public static final String START_HEADER = "<|start_header_id|>";

    /** End header token */
    public static final String END_HEADER = "<|end_header_id|>";

    /** End of turn token */
    public static final String EOT_TOKEN = "<|eot_id|>";

    // ========================================================================
    // Instance Configuration
    // ========================================================================

    private String modelFile;
    private int contextSize;
    private int numThreads;
    private int maxTokens;
    private float temperature;
    private int topK;
    private float topP;
    private float repetitionPenalty;
    private float confidenceThreshold;
    private boolean useStreaming;
    private boolean enableLogging;
    private Map<String, FunctionSchema> functionSchemas;

    /**
     * Default configuration optimized for Egyptian Arabic function calling.
     */
    public FunctionGemmaConfig() {
        this.modelFile = DEFAULT_MODEL_FILE;
        this.contextSize = DEFAULT_CONTEXT_SIZE;
        this.numThreads = DEFAULT_NUM_THREADS;
        this.maxTokens = DEFAULT_MAX_TOKENS;
        this.temperature = TEMPERATURE;
        this.topK = TOP_K;
        this.topP = TOP_P;
        this.repetitionPenalty = REPETITION_PENALTY;
        this.confidenceThreshold = CONFIDENCE_THRESHOLD;
        this.useStreaming = false;
        this.enableLogging = true;
        this.functionSchemas = new HashMap<>();

        // Initialize default function schemas
        initializeDefaultFunctionSchemas();
    }

    /**
     * Initialize default function schemas for Egyptian Agent.
     */
    private void initializeDefaultFunctionSchemas() {
        // CALL_CONTACT
        functionSchemas.put("CALL_CONTACT", new FunctionSchema(
            "call_contact",
            "Make a phone call to a contact by name or number",
            Map.of(
                "contact_name", new FunctionParameter("string", "Contact name in Arabic (e.g., ماما، بابا، أحمد)", true),
                "phone_number", new FunctionParameter("string", "Phone number (optional if contact name provided)", false)
            )
        ));

        // SEND_WHATSAPP
        functionSchemas.put("SEND_WHATSAPP", new FunctionSchema(
            "send_whatsapp",
            "Send a WhatsApp message to a contact",
            Map.of(
                "contact_name", new FunctionParameter("string", "Contact name in Arabic", true),
                "message", new FunctionParameter("string", "Message content in Arabic", true)
            )
        ));

        // SEND_VOICE_MESSAGE
        functionSchemas.put("SEND_VOICE_MESSAGE", new FunctionSchema(
            "send_voice_message",
            "Send a voice message via WhatsApp",
            Map.of(
                "contact_name", new FunctionParameter("string", "Contact name in Arabic", true)
            )
        ));

        // SET_ALARM
        functionSchemas.put("SET_ALARM", new FunctionSchema(
            "set_alarm",
            "Set an alarm or reminder for a specific time",
            Map.of(
                "time", new FunctionParameter("string", "Time in Egyptian Arabic (e.g., بكرة الصبح، الساعة 8)", true),
                "label", new FunctionParameter("string", "Alarm label/description", false)
            )
        ));

        // READ_TIME
        functionSchemas.put("READ_TIME", new FunctionSchema(
            "read_time",
            "Read the current time aloud",
            Map.of()
        ));

        // EMERGENCY
        functionSchemas.put("EMERGENCY", new FunctionSchema(
            "emergency",
            "Trigger emergency protocol - call emergency contacts",
            Map.of(
                "type", new FunctionParameter("string", "Type of emergency (medical, security, general)", false)
            )
        ));

        // OPEN_APP
        functionSchemas.put("OPEN_APP", new FunctionSchema(
            "open_app",
            "Open a specific application",
            Map.of(
                "app_name", new FunctionParameter("string", "Application name (واتساب، فيسبوك، يوتيوب)", true)
            )
        ));

        // WIFI_TOGGLE
        functionSchemas.put("WIFI_TOGGLE", new FunctionSchema(
            "toggle_wifi",
            "Turn WiFi on or off",
            Map.of(
                "state", new FunctionParameter("string", "Desired state: on, off, or toggle", true)
            )
        ));

        // BLUETOOTH_TOGGLE
        functionSchemas.put("BLUETOOTH_TOGGLE", new FunctionSchema(
            "toggle_bluetooth",
            "Turn Bluetooth on or off",
            Map.of(
                "state", new FunctionParameter("string", "Desired state: on, off, or toggle", true)
            )
        ));

        // TOGGLE_FLASHLIGHT
        functionSchemas.put("TOGGLE_FLASHLIGHT", new FunctionSchema(
            "toggle_flashlight",
            "Turn the flashlight/torch on or off",
            Map.of(
                "state", new FunctionParameter("string", "Desired state: on, off, or toggle", true)
            )
        ));

        // READ_MISSED_CALLS
        functionSchemas.put("READ_MISSED_CALLS", new FunctionSchema(
            "read_missed_calls",
            "Read missed calls from the call log",
            Map.of()
        ));

        // SEND_SMS
        functionSchemas.put("SEND_SMS", new FunctionSchema(
            "send_sms",
            "Send an SMS message",
            Map.of(
                "contact_name", new FunctionParameter("string", "Contact name or phone number", true),
                "message", new FunctionParameter("string", "SMS message content", true)
            )
        ));

        // WEATHER_QUERY
        functionSchemas.put("WEATHER_QUERY", new FunctionSchema(
            "weather_query",
            "Get weather information",
            Map.of(
                "location", new FunctionParameter("string", "Location name (default: current location)", false),
                "time", new FunctionParameter("string", "Time reference: today, tomorrow, weekend", false)
            )
        ));

        // GREETING
        functionSchemas.put("GREETING", new FunctionSchema(
            "greeting",
            "Respond to greetings",
            Map.of()
        ));

        // THANK_YOU
        functionSchemas.put("THANK_YOU", new FunctionSchema(
            "thank_you",
            "Respond to thank you messages",
            Map.of()
        ));

        // GOODBYE
        functionSchemas.put("GOODBYE", new FunctionSchema(
            "goodbye",
            "Respond to goodbye messages",
            Map.of()
        ));
    }

    // ========================================================================
    // Getters and Setters
    // ========================================================================

    public String getModelFile() {
        return modelFile;
    }

    public void setModelFile(String modelFile) {
        this.modelFile = modelFile;
    }

    public int getContextSize() {
        return contextSize;
    }

    public void setContextSize(int contextSize) {
        this.contextSize = contextSize;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public float getTopP() {
        return topP;
    }

    public void setTopP(float topP) {
        this.topP = topP;
    }

    public float getRepetitionPenalty() {
        return repetitionPenalty;
    }

    public void setRepetitionPenalty(float repetitionPenalty) {
        this.repetitionPenalty = repetitionPenalty;
    }

    public float getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(float confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public boolean isUseStreaming() {
        return useStreaming;
    }

    public void setUseStreaming(boolean useStreaming) {
        this.useStreaming = useStreaming;
    }

    public boolean isEnableLogging() {
        return enableLogging;
    }

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    public Map<String, FunctionSchema> getFunctionSchemas() {
        return functionSchemas;
    }

    public void setFunctionSchemas(Map<String, FunctionSchema> functionSchemas) {
        this.functionSchemas = functionSchemas;
    }

    /**
     * Get a specific function schema by name.
     */
    public FunctionSchema getFunctionSchema(String functionName) {
        return functionSchemas.get(functionName.toUpperCase());
    }

    /**
     * Add or update a function schema.
     */
    public void addFunctionSchema(FunctionSchema schema) {
        functionSchemas.put(schema.getName().toUpperCase(), schema);
    }

    // ========================================================================
    // Prompt Building Utilities
    // ========================================================================

    /**
     * Build the system prompt for FunctionGemma with Egyptian dialect support.
     */
    public String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();

        prompt.append(START_HEADER).append("system").append(END_HEADER).append("\n");
        prompt.append("أنت مساعد صوتي مصري متخصص في مساعدة كبار السن والمكفوفين.\n");
        prompt.append("فهمك ممتاز للعامية المصرية واللهجة المصرية.\n");
        prompt.append("مهمتك هي فهم أوامر المستخدم وتحويلها لاستدعاءات دوال (function calls).\n");
        prompt.append("استخدم فقط الوظائف المتاحة لك ولا تخترع وظائف جديدة.\n\n");

        prompt.append("الوظائف المتاحة:\n");
        for (FunctionSchema schema : functionSchemas.values()) {
            prompt.append("- ").append(schema.toFunctionGemmaFormat()).append("\n");
        }

        prompt.append("\n");
        prompt.append("أمثلة:\n");
        prompt.append("- \"اتصل بماما\" → call_contact(contact_name=\"ماما\")\n");
        prompt.append("- \"ابعت واتساب لأحمد\" → send_whatsapp(contact_name=\"أحمد\", message=\"...\")\n");
        prompt.append("- \"نبهني بكرة الصبح\" → set_alarm(time=\"بكرة الصبح\")\n");
        prompt.append("- \"الساعة كام\" → read_time()\n");
        prompt.append("- \"يا نجدة\" → emergency(type=\"general\")\n");
        prompt.append("- \"افتح الواي فاي\" → toggle_wifi(state=\"on\")\n");

        prompt.append(EOT_TOKEN).append("\n");

        return prompt.toString();
    }

    /**
     * Build user prompt for inference.
     */
    public String buildUserPrompt(String userInput) {
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
    public String buildFullPrompt(String userInput) {
        return buildSystemPrompt() + buildUserPrompt(userInput);
    }

    // ========================================================================
    // JSON Schema Generation
    // ========================================================================

    /**
     * Generate JSON schema for all functions (compatible with FunctionGemma format).
     */
    public String generateFunctionSchemaJson() {
        try {
            JSONArray functions = new JSONArray();

            for (FunctionSchema schema : functionSchemas.values()) {
                JSONObject funcObj = new JSONObject();
                funcObj.put("type", "function");

                JSONObject function = new JSONObject();
                function.put("name", schema.getName());
                function.put("description", schema.getDescription());

                JSONObject parameters = new JSONObject();
                parameters.put("type", "object");

                JSONObject properties = new JSONObject();
                JSONArray required = new JSONArray();

                for (Map.Entry<String, FunctionParameter> entry : schema.getParameters().entrySet()) {
                    FunctionParameter param = entry.getValue();
                    JSONObject paramObj = new JSONObject();
                    paramObj.put("type", param.getType());
                    paramObj.put("description", param.getDescription());

                    if (param.getEnumValues() != null && !param.getEnumValues().isEmpty()) {
                        JSONArray enumArray = new JSONArray(param.getEnumValues());
                        paramObj.put("enum", enumArray);
                    }

                    properties.put(entry.getKey(), paramObj);

                    if (param.isRequired()) {
                        required.put(entry.getKey());
                    }
                }

                parameters.put("properties", properties);
                parameters.put("required", required);
                function.put("parameters", parameters);

                funcObj.put("function", function);
                functions.put(funcObj);
            }

            return functions.toString();

        } catch (JSONException e) {
            Log.e(TAG, "Error generating function schema JSON", e);
            return "[]";
        }
    }

    // ========================================================================
    // Builder Pattern
    // ========================================================================

    /**
     * Create a builder for FunctionGemmaConfig.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for FunctionGemmaConfig.
     */
    public static class Builder {
        private final FunctionGemmaConfig config;

        public Builder() {
            config = new FunctionGemmaConfig();
        }

        public Builder modelFile(String modelFile) {
            config.setModelFile(modelFile);
            return this;
        }

        public Builder contextSize(int contextSize) {
            config.setContextSize(contextSize);
            return this;
        }

        public Builder numThreads(int numThreads) {
            config.setNumThreads(numThreads);
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            config.setMaxTokens(maxTokens);
            return this;
        }

        public Builder temperature(float temperature) {
            config.setTemperature(temperature);
            return this;
        }

        public Builder topK(int topK) {
            config.setTopK(topK);
            return this;
        }

        public Builder topP(float topP) {
            config.setTopP(topP);
            return this;
        }

        public Builder repetitionPenalty(float repetitionPenalty) {
            config.setRepetitionPenalty(repetitionPenalty);
            return this;
        }

        public Builder confidenceThreshold(float threshold) {
            config.setConfidenceThreshold(threshold);
            return this;
        }

        public Builder useStreaming(boolean useStreaming) {
            config.setUseStreaming(useStreaming);
            return this;
        }

        public Builder enableLogging(boolean enableLogging) {
            config.setEnableLogging(enableLogging);
            return this;
        }

        public FunctionGemmaConfig build() {
            return config;
        }
    }

    // ========================================================================
    // Function Schema Inner Classes
    // ========================================================================

    /**
     * Function Schema Definition
     * Defines a callable function with its parameters.
     */
    public static class FunctionSchema {
        private final String name;
        private final String description;
        private final Map<String, FunctionParameter> parameters;

        public FunctionSchema(String name, String description, Map<String, FunctionParameter> parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Map<String, FunctionParameter> getParameters() {
            return parameters;
        }

        /**
         * Convert to FunctionGemma format string.
         */
        public String toFunctionGemmaFormat() {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append("(");

            boolean first = true;
            for (Map.Entry<String, FunctionParameter> entry : parameters.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append("=").append("\"").append(entry.getValue().getDescription().split("\\(")[0].trim()).append("\"");
                first = false;
            }

            sb.append(")");
            return sb.toString();
        }

        @Override
        public String toString() {
            return "FunctionSchema{name='" + name + "', description='" + description + "'}";
        }
    }

    /**
     * Function Parameter Definition
     * Defines a parameter for a function.
     */
    public static class FunctionParameter {
        private final String type;
        private final String description;
        private final boolean required;
        private final String[] enumValues;

        public FunctionParameter(String type, String description, boolean required) {
            this(type, description, required, null);
        }

        public FunctionParameter(String type, String description, boolean required, String[] enumValues) {
            this.type = type;
            this.description = description;
            this.required = required;
            this.enumValues = enumValues;
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public boolean isRequired() {
            return required;
        }

        public String[] getEnumValues() {
            return enumValues;
        }
    }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    /**
     * Load configuration from assets JSON file.
     */
    public static FunctionGemmaConfig loadFromAssets(Context context, String assetPath) {
        try {
            InputStream inputStream = context.getAssets().open(assetPath);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONObject configJson = new JSONObject(json);
            FunctionGemmaConfig config = new FunctionGemmaConfig();

            if (configJson.has("model_file")) {
                config.setModelFile(configJson.getString("model_file"));
            }
            if (configJson.has("context_size")) {
                config.setContextSize(configJson.getInt("context_size"));
            }
            if (configJson.has("num_threads")) {
                config.setNumThreads(configJson.getInt("num_threads"));
            }
            if (configJson.has("temperature")) {
                config.setTemperature((float) configJson.getDouble("temperature"));
            }
            if (configJson.has("confidence_threshold")) {
                config.setConfidenceThreshold((float) configJson.getDouble("confidence_threshold"));
            }

            return config;

        } catch (Exception e) {
            Log.e(TAG, "Error loading config from assets", e);
            return new FunctionGemmaConfig();
        }
    }
}
