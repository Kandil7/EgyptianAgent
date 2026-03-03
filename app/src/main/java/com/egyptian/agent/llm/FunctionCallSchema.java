package com.egyptian.agent.llm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Function Call Schema Definitions
 *
 * Defines all supported function calling schemas for Egyptian Agent.
 * Each schema includes function name, description, parameters, and Egyptian dialect examples.
 *
 * Supported Functions:
 * - CALL_CONTACT: Make phone calls
 * - SEND_WHATSAPP: Send WhatsApp messages
 * - SEND_VOICE_MESSAGE: Send voice messages
 * - SET_ALARM: Set alarms and reminders
 * - READ_TIME: Read current time
 * - EMERGENCY: Trigger emergency protocol
 * - OPEN_APP: Open applications
 * - WIFI_TOGGLE: Toggle WiFi
 * - BLUETOOTH_TOGGLE: Toggle Bluetooth
 * - TOGGLE_FLASHLIGHT: Toggle flashlight
 * - READ_MISSED_CALLS: Read missed calls
 * - SEND_SMS: Send SMS messages
 * - WEATHER_QUERY: Get weather information
 * - GREETING: Handle greetings
 * - THANK_YOU: Handle thank you messages
 * - GOODBYE: Handle goodbye messages
 *
 * @author Egyptian Agent Team
 * @version 1.0.0
 */
public class FunctionCallSchema {

    private static final String TAG = "FunctionCallSchema";

    // ========================================================================
    // Function Name Constants
    // ========================================================================

    /** Call a contact by name or number */
    public static final String CALL_CONTACT = "call_contact";

    /** Send WhatsApp message */
    public static final String SEND_WHATSAPP = "send_whatsapp";

    /** Send voice message via WhatsApp */
    public static final String SEND_VOICE_MESSAGE = "send_voice_message";

    /** Set alarm or reminder */
    public static final String SET_ALARM = "set_alarm";

    /** Read current time */
    public static final String READ_TIME = "read_time";

    /** Trigger emergency protocol */
    public static final String EMERGENCY = "emergency";

    /** Open an application */
    public static final String OPEN_APP = "open_app";

    /** Toggle WiFi */
    public static final String WIFI_TOGGLE = "toggle_wifi";

    /** Toggle Bluetooth */
    public static final String BLUETOOTH_TOGGLE = "toggle_bluetooth";

    /** Toggle flashlight */
    public static final String TOGGLE_FLASHLIGHT = "toggle_flashlight";

    /** Read missed calls */
    public static final String READ_MISSED_CALLS = "read_missed_calls";

    /** Send SMS message */
    public static final String SEND_SMS = "send_sms";

    /** Get weather information */
    public static final String WEATHER_QUERY = "weather_query";

    /** Handle greeting */
    public static final String GREETING = "greeting";

    /** Handle thank you */
    public static final String THANK_YOU = "thank_you";

    /** Handle goodbye */
    public static final String GOODBYE = "goodbye";

    // ========================================================================
    // Egyptian Dialect Examples
    // ========================================================================

    /** Egyptian dialect examples for each function */
    private static final Map<String, List<String>> EGYPTIAN_EXAMPLES = new HashMap<>();

    static {
        // CALL_CONTACT examples
        EGYPTIAN_EXAMPLES.put(CALL_CONTACT, Arrays.asList(
            "اتصل بماما",
            "كلم بابا",
            "رن على أحمد",
            "نادي مريم",
            "عايز أتكلّم مع خالو",
            "حطني في مكالمة مع تيتا"
        ));

        // SEND_WHATSAPP examples
        EGYPTIAN_EXAMPLES.put(SEND_WHATSAPP, Arrays.asList(
            "ابعت واتساب لأحمد",
            "أرسل رسالة واتساب لماما",
            "قول لأني هتأخر",
            "ابعث رسالة لمحمد",
            "راسل بابا على واتساب"
        ));

        // SEND_VOICE_MESSAGE examples
        EGYPTIAN_EXAMPLES.put(SEND_VOICE_MESSAGE, Arrays.asList(
            "ابعت رسالة صوتية",
            "سجّل صوت وابعثه",
            "أرسل ميمو لماما"
        ));

        // SET_ALARM examples
        EGYPTIAN_EXAMPLES.put(SET_ALARM, Arrays.asList(
            "نبهني بكرة الصبح",
            "اضبط منبه الساعة 8",
            "ذكرني بعد ساعة",
            "حطلي منبه للعصر",
            "نبهني قبل المغرب"
        ));

        // READ_TIME examples
        EGYPTIAN_EXAMPLES.put(READ_TIME, Arrays.asList(
            "الساعة كام",
            "كام الوقت",
            "إيه الوقت دلوقتي",
            "قولي الساعة"
        ));

        // EMERGENCY examples
        EGYPTIAN_EXAMPLES.put(EMERGENCY, Arrays.asList(
            "يا نجدة",
            "استغاثة",
            "محتاج مساعدة حالاً",
            "في حد يجي",
            "مش قادر",
            "طوارئ"
        ));

        // OPEN_APP examples
        EGYPTIAN_EXAMPLES.put(OPEN_APP, Arrays.asList(
            "افتح واتساب",
            "شغل يوتيوب",
            "افتح الفيسبوك",
            "شغل التطبيق بتاع الأخبار"
        ));

        // WIFI_TOGGLE examples
        EGYPTIAN_EXAMPLES.put(WIFI_TOGGLE, Arrays.asList(
            "افتح الواي فاي",
            "قفل الواي فاي",
            "شغل الواي فاي",
            "اطفئ الواي فاي"
        ));

        // BLUETOOTH_TOGGLE examples
        EGYPTIAN_EXAMPLES.put(BLUETOOTH_TOGGLE, Arrays.asList(
            "افتح البلوتوث",
            "قفل البلوتوث",
            "شغل البلوتوث"
        ));

        // TOGGLE_FLASHLIGHT examples
        EGYPTIAN_EXAMPLES.put(TOGGLE_FLASHLIGHT, Arrays.asList(
            "افتح الفلاش",
            "شغل الكشاف",
            "قفل الفلاش"
        ));

        // READ_MISSED_CALLS examples
        EGYPTIAN_EXAMPLES.put(READ_MISSED_CALLS, Arrays.asList(
            "إيه المكالمات اللي فاتت",
            "قولي مين اتصل بيا",
            "عندي مكالمات مفقودة"
        ));

        // SEND_SMS examples
        EGYPTIAN_EXAMPLES.put(SEND_SMS, Arrays.asList(
            "ابعت رسالة نصية",
            "أرسل SMS لماما",
            "سيب رسالة لمحمد"
        ));

        // WEATHER_QUERY examples
        EGYPTIAN_EXAMPLES.put(WEATHER_QUERY, Arrays.asList(
            "الجو إيه النهاردة",
            "إيه حالة الطقس",
            "هيمطر بكرة"
        ));

        // GREETING examples
        EGYPTIAN_EXAMPLES.put(GREETING, Arrays.asList(
            "السلام عليكم",
            "أهلاً",
            "ازيك",
            "عامل ايه",
            "صباح الخير",
            "مساء النور"
        ));

        // THANK_YOU examples
        EGYPTIAN_EXAMPLES.put(THANK_YOU, Arrays.asList(
            "شكرا",
            "متشكر",
            "تسلم إيديك",
            "يسلمو"
        ));

        // GOODBYE examples
        EGYPTIAN_EXAMPLES.put(GOODBYE, Arrays.asList(
            "مع السلامة",
            "باي",
            "سلام",
            "في أمان الله",
            "أشوفك بعدين"
        ));
    }

    // ========================================================================
    // Schema Registry
    // ========================================================================

    private final Map<String, SchemaDefinition> schemas;

    /**
     * Create schema registry with all default functions.
     */
    public FunctionCallSchema() {
        this.schemas = new HashMap<>();
        initializeDefaultSchemas();
    }

    /**
     * Initialize all default function schemas.
     */
    private void initializeDefaultSchemas() {
        // CALL_CONTACT
        register(new SchemaDefinition(
            CALL_CONTACT,
            "Make a phone call to a contact by name or phone number",
            Arrays.asList(
                new ParameterDefinition("contact_name", "string",
                    "Contact name in Arabic (e.g., ماما، بابا، أحمد) or phone number",
                    true),
                new ParameterDefinition("phone_number", "string",
                    "Phone number (optional if contact name provided)",
                    false)
            ),
            EGYPTIAN_EXAMPLES.get(CALL_CONTACT)
        ));

        // SEND_WHATSAPP
        register(new SchemaDefinition(
            SEND_WHATSAPP,
            "Send a WhatsApp message to a contact",
            Arrays.asList(
                new ParameterDefinition("contact_name", "string",
                    "Contact name in Arabic",
                    true),
                new ParameterDefinition("message", "string",
                    "Message content in Arabic",
                    true)
            ),
            EGYPTIAN_EXAMPLES.get(SEND_WHATSAPP)
        ));

        // SEND_VOICE_MESSAGE
        register(new SchemaDefinition(
            SEND_VOICE_MESSAGE,
            "Send a voice message via WhatsApp",
            Arrays.asList(
                new ParameterDefinition("contact_name", "string",
                    "Contact name in Arabic",
                    true)
            ),
            EGYPTIAN_EXAMPLES.get(SEND_VOICE_MESSAGE)
        ));

        // SET_ALARM
        register(new SchemaDefinition(
            SET_ALARM,
            "Set an alarm or reminder for a specific time",
            Arrays.asList(
                new ParameterDefinition("time", "string",
                    "Time in Egyptian Arabic (e.g., بكرة الصبح، الساعة 8، بعد ساعة)",
                    true),
                new ParameterDefinition("label", "string",
                    "Alarm label or description",
                    false)
            ),
            EGYPTIAN_EXAMPLES.get(SET_ALARM)
        ));

        // READ_TIME
        register(new SchemaDefinition(
            READ_TIME,
            "Read the current time aloud",
            new ArrayList<>(),
            EGYPTIAN_EXAMPLES.get(READ_TIME)
        ));

        // EMERGENCY
        register(new SchemaDefinition(
            EMERGENCY,
            "Trigger emergency protocol - call emergency contacts and alert family",
            Arrays.asList(
                new ParameterDefinition("type", "string",
                    "Type of emergency: medical, security, general",
                    false,
                    Arrays.asList("medical", "security", "general"))
            ),
            EGYPTIAN_EXAMPLES.get(EMERGENCY)
        ));

        // OPEN_APP
        register(new SchemaDefinition(
            OPEN_APP,
            "Open a specific application",
            Arrays.asList(
                new ParameterDefinition("app_name", "string",
                    "Application name in Arabic (واتساب، فيسبوك، يوتيوب، etc.)",
                    true)
            ),
            EGYPTIAN_EXAMPLES.get(OPEN_APP)
        ));

        // WIFI_TOGGLE
        register(new SchemaDefinition(
            WIFI_TOGGLE,
            "Turn WiFi on or off",
            Arrays.asList(
                new ParameterDefinition("state", "string",
                    "Desired state: on, off, or toggle",
                    true,
                    Arrays.asList("on", "off", "toggle"))
            ),
            EGYPTIAN_EXAMPLES.get(WIFI_TOGGLE)
        ));

        // BLUETOOTH_TOGGLE
        register(new SchemaDefinition(
            BLUETOOTH_TOGGLE,
            "Turn Bluetooth on or off",
            Arrays.asList(
                new ParameterDefinition("state", "string",
                    "Desired state: on, off, or toggle",
                    true,
                    Arrays.asList("on", "off", "toggle"))
            ),
            EGYPTIAN_EXAMPLES.get(BLUETOOTH_TOGGLE)
        ));

        // TOGGLE_FLASHLIGHT
        register(new SchemaDefinition(
            TOGGLE_FLASHLIGHT,
            "Turn the flashlight/torch on or off",
            Arrays.asList(
                new ParameterDefinition("state", "string",
                    "Desired state: on, off, or toggle",
                    true,
                    Arrays.asList("on", "off", "toggle"))
            ),
            EGYPTIAN_EXAMPLES.get(TOGGLE_FLASHLIGHT)
        ));

        // READ_MISSED_CALLS
        register(new SchemaDefinition(
            READ_MISSED_CALLS,
            "Read missed calls from the call log",
            new ArrayList<>(),
            EGYPTIAN_EXAMPLES.get(READ_MISSED_CALLS)
        ));

        // SEND_SMS
        register(new SchemaDefinition(
            SEND_SMS,
            "Send an SMS message to a contact",
            Arrays.asList(
                new ParameterDefinition("contact_name", "string",
                    "Contact name or phone number",
                    true),
                new ParameterDefinition("message", "string",
                    "SMS message content in Arabic",
                    true)
            ),
            EGYPTIAN_EXAMPLES.get(SEND_SMS)
        ));

        // WEATHER_QUERY
        register(new SchemaDefinition(
            WEATHER_QUERY,
            "Get weather information for a location",
            Arrays.asList(
                new ParameterDefinition("location", "string",
                    "Location name (default: current location)",
                    false),
                new ParameterDefinition("time", "string",
                    "Time reference: today, tomorrow, weekend",
                    false,
                    Arrays.asList("today", "tomorrow", "weekend"))
            ),
            EGYPTIAN_EXAMPLES.get(WEATHER_QUERY)
        ));

        // GREETING
        register(new SchemaDefinition(
            GREETING,
            "Respond to greetings appropriately",
            new ArrayList<>(),
            EGYPTIAN_EXAMPLES.get(GREETING)
        ));

        // THANK_YOU
        register(new SchemaDefinition(
            THANK_YOU,
            "Respond to thank you messages",
            new ArrayList<>(),
            EGYPTIAN_EXAMPLES.get(THANK_YOU)
        ));

        // GOODBYE
        register(new SchemaDefinition(
            GOODBYE,
            "Respond to goodbye messages",
            new ArrayList<>(),
            EGYPTIAN_EXAMPLES.get(GOODBYE)
        ));
    }

    /**
     * Register a function schema.
     */
    public void register(SchemaDefinition schema) {
        schemas.put(schema.getName().toLowerCase(), schema);
    }

    /**
     * Get schema by function name.
     */
    public SchemaDefinition getSchema(String functionName) {
        return schemas.get(functionName.toLowerCase());
    }

    /**
     * Check if function is supported.
     */
    public boolean isSupported(String functionName) {
        return schemas.containsKey(functionName.toLowerCase());
    }

    /**
     * Get all registered schemas.
     */
    public Map<String, SchemaDefinition> getAllSchemas() {
        return new HashMap<>(schemas);
    }

    /**
     * Get all function names.
     */
    public List<String> getFunctionNames() {
        return new ArrayList<>(schemas.keySet());
    }

    // ========================================================================
    // JSON Schema Generation
    // ========================================================================

    /**
     * Generate JSON schema for all functions.
     * Compatible with FunctionGemma and OpenAI function calling format.
     */
    public String toJsonSchema() {
        try {
            JSONArray functions = new JSONArray();

            for (SchemaDefinition schema : schemas.values()) {
                functions.put(schema.toJsonObject());
            }

            JSONObject result = new JSONObject();
            result.put("functions", functions);

            return result.toString(2);

        } catch (JSONException e) {
            return "{\"error\": \"Failed to generate JSON schema\"}";
        }
    }

    /**
     * Generate FunctionGemma-specific prompt format.
     */
    public String toFunctionGemmaPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("Available functions:\n");

        for (SchemaDefinition schema : schemas.values()) {
            sb.append("- ").append(schema.toFunctionGemmaFormat()).append("\n");
        }

        return sb.toString();
    }

    // ========================================================================
    // Schema Definition Inner Class
    // ========================================================================

    /**
     * Schema Definition
     * Defines a single function schema with name, description, and parameters.
     */
    public static class SchemaDefinition {
        private final String name;
        private final String description;
        private final List<ParameterDefinition> parameters;
        private final List<String> examples;

        public SchemaDefinition(String name, String description,
                                List<ParameterDefinition> parameters,
                                List<String> examples) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
            this.examples = examples;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public List<ParameterDefinition> getParameters() {
            return parameters;
        }

        public List<String> getExamples() {
            return examples;
        }

        /**
         * Convert to JSON object.
         */
        public JSONObject toJsonObject() {
            try {
                JSONObject function = new JSONObject();
                function.put("name", name);
                function.put("description", description);

                JSONObject parametersObj = new JSONObject();
                parametersObj.put("type", "object");

                JSONObject properties = new JSONObject();
                JSONArray required = new JSONArray();

                for (ParameterDefinition param : parameters) {
                    JSONObject paramObj = new JSONObject();
                    paramObj.put("type", param.getType());
                    paramObj.put("description", param.getDescription());

                    if (param.getEnumValues() != null && !param.getEnumValues().isEmpty()) {
                        JSONArray enumArray = new JSONArray(param.getEnumValues());
                        paramObj.put("enum", enumArray);
                    }

                    properties.put(param.getName(), paramObj);

                    if (param.isRequired()) {
                        required.put(param.getName());
                    }
                }

                parametersObj.put("properties", properties);
                parametersObj.put("required", required);
                function.put("parameters", parametersObj);

                JSONObject result = new JSONObject();
                result.put("type", "function");
                result.put("function", function);

                return result;

            } catch (JSONException e) {
                return new JSONObject();
            }
        }

        /**
         * Convert to FunctionGemma format string.
         */
        public String toFunctionGemmaFormat() {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append("(");

            boolean first = true;
            for (ParameterDefinition param : parameters) {
                if (!first) sb.append(", ");
                sb.append(param.getName());
                if (param.getEnumValues() != null && !param.getEnumValues().isEmpty()) {
                    sb.append(": ").append(String.join("|", param.getEnumValues()));
                }
                first = false;
            }

            sb.append(") - ").append(description);
            return sb.toString();
        }

        @Override
        public String toString() {
            return "SchemaDefinition{name='" + name + "', description='" + description + "'}";
        }
    }

    // ========================================================================
    // Parameter Definition Inner Class
    // ========================================================================

    /**
     * Parameter Definition
     * Defines a single parameter for a function.
     */
    public static class ParameterDefinition {
        private final String name;
        private final String type;
        private final String description;
        private final boolean required;
        private final List<String> enumValues;

        public ParameterDefinition(String name, String type, String description, boolean required) {
            this(name, type, description, required, null);
        }

        public ParameterDefinition(String name, String type, String description, boolean required,
                                   List<String> enumValues) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.required = required;
            this.enumValues = enumValues;
        }

        public String getName() {
            return name;
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

        public List<String> getEnumValues() {
            return enumValues;
        }
    }

    // ========================================================================
    // Function Call Result
    // ========================================================================

    /**
     * Function Call Result
     * Represents a parsed function call from model output.
     */
    public static class FunctionCallResult {
        private final String functionName;
        private final Map<String, String> arguments;
        private final float confidence;
        private final String rawOutput;

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
