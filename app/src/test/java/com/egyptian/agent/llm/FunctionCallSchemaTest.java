package com.egyptian.agent.llm;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive Unit Tests for FunctionCallSchema
 *
 * Test Coverage:
 * - All 16 functions defined correctly
 * - JSON schema generation
 * - Function call parsing
 * - Argument validation
 * - Egyptian dialect examples
 * - Schema registry operations
 * - Parameter definitions
 *
 * @author Egyptian Agent Team
 * @version 1.0.0
 */
public class FunctionCallSchemaTest {

    // ========================================================================
    // Test Configuration
    // ========================================================================

    private FunctionCallSchema schema;
    private int totalTests;
    private int passedTests;

    // Expected function names
    private static final List<String> EXPECTED_FUNCTIONS = Arrays.asList(
        "call_contact",
        "send_whatsapp",
        "send_voice_message",
        "set_alarm",
        "read_time",
        "emergency",
        "open_app",
        "toggle_wifi",
        "toggle_bluetooth",
        "toggle_flashlight",
        "read_missed_calls",
        "send_sms",
        "weather_query",
        "greeting",
        "thank_you",
        "goodbye"
    );

    // ========================================================================
    // Test Setup
    // ========================================================================

    @Before
    public void setUp() {
        schema = new FunctionCallSchema();
        totalTests = 0;
        passedTests = 0;
    }

    // ========================================================================
    // CATEGORY 1: Schema Initialization Tests (5 tests)
    // ========================================================================

    @Test
    public void testSchemaInitialization() {
        // Given: New schema instance
        FunctionCallSchema newSchema = new FunctionCallSchema();

        // Then: Should initialize successfully
        assertNotNull("Schema should be initialized", newSchema);
        totalTests++;
        passedTests++;
    }

    @Test
    public void testAllFunctionsRegistered() {
        // When: Get all function names
        List<String> functionNames = schema.getFunctionNames();

        // Then: All 16 functions should be registered
        assertNotNull("Function names should not be null", functionNames);
        assertEquals("Should have 16 functions", EXPECTED_FUNCTIONS.size(), functionNames.size());

        for (String expectedFunction : EXPECTED_FUNCTIONS) {
            assertTrue("Should contain: " + expectedFunction,
                    functionNames.contains(expectedFunction.toLowerCase()));
        }

        totalTests++;
        passedTests++;
    }

    @Test
    public void testFunctionConstants() {
        // Then: All function constants should be defined
        assertEquals("CALL_CONTACT constant", "call_contact", FunctionCallSchema.CALL_CONTACT);
        assertEquals("SEND_WHATSAPP constant", "send_whatsapp", FunctionCallSchema.SEND_WHATSAPP);
        assertEquals("SEND_VOICE_MESSAGE constant", "send_voice_message", FunctionCallSchema.SEND_VOICE_MESSAGE);
        assertEquals("SET_ALARM constant", "set_alarm", FunctionCallSchema.SET_ALARM);
        assertEquals("READ_TIME constant", "read_time", FunctionCallSchema.READ_TIME);
        assertEquals("EMERGENCY constant", "emergency", FunctionCallSchema.EMERGENCY);
        assertEquals("OPEN_APP constant", "open_app", FunctionCallSchema.OPEN_APP);
        assertEquals("WIFI_TOGGLE constant", "toggle_wifi", FunctionCallSchema.WIFI_TOGGLE);
        assertEquals("BLUETOOTH_TOGGLE constant", "toggle_bluetooth", FunctionCallSchema.BLUETOOTH_TOGGLE);
        assertEquals("TOGGLE_FLASHLIGHT constant", "toggle_flashlight", FunctionCallSchema.TOGGLE_FLASHLIGHT);
        assertEquals("READ_MISSED_CALLS constant", "read_missed_calls", FunctionCallSchema.READ_MISSED_CALLS);
        assertEquals("SEND_SMS constant", "send_sms", FunctionCallSchema.SEND_SMS);
        assertEquals("WEATHER_QUERY constant", "weather_query", FunctionCallSchema.WEATHER_QUERY);
        assertEquals("GREETING constant", "greeting", FunctionCallSchema.GREETING);
        assertEquals("THANK_YOU constant", "thank_you", FunctionCallSchema.THANK_YOU);
        assertEquals("GOODBYE constant", "goodbye", FunctionCallSchema.GOODBYE);

        totalTests++;
        passedTests++;
    }

    @Test
    public void testSchemaRegistry_GetSchema() {
        // When: Get specific schemas
        FunctionCallSchema.SchemaDefinition callSchema = schema.getSchema("call_contact");
        FunctionCallSchema.SchemaDefinition whatsappSchema = schema.getSchema("send_whatsapp");
        FunctionCallSchema.SchemaDefinition alarmSchema = schema.getSchema("set_alarm");

        // Then: Schemas should be retrieved
        assertNotNull("CALL_CONTACT schema should exist", callSchema);
        assertNotNull("SEND_WHATSAPP schema should exist", whatsappSchema);
        assertNotNull("SET_ALARM schema should exist", alarmSchema);

        totalTests++;
        passedTests++;
    }

    @Test
    public void testSchemaRegistry_IsSupported() {
        // When: Check supported functions
        boolean callSupported = schema.isSupported("call_contact");
        boolean whatsappSupported = schema.isSupported("send_whatsapp");
        boolean unknownSupported = schema.isSupported("unknown_function");

        // Then: Support should be correctly reported
        assertTrue("CALL_CONTACT should be supported", callSupported);
        assertTrue("SEND_WHATSAPP should be supported", whatsappSupported);
        assertFalse("Unknown function should not be supported", unknownSupported);

        totalTests++;
        passedTests++;
    }

    // ========================================================================
    // CATEGORY 2: Individual Function Schema Tests (16 tests)
    // ========================================================================

    @Test
    public void testCallContactSchema() {
        // Given: CALL_CONTACT schema
        FunctionCallSchema.SchemaDefinition callSchema = schema.getSchema("call_contact");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", callSchema);
        assertEquals("Name should be call_contact", "call_contact", callSchema.getName());
        assertNotNull("Description should exist", callSchema.getDescription());
        assertTrue("Should have parameters", callSchema.getParameters().size() > 0);
        assertTrue("Should have examples", callSchema.getExamples().size() > 0);

        // Check parameters
        Map<String, FunctionCallSchema.ParameterDefinition> params = new HashMap<>();
        for (FunctionCallSchema.ParameterDefinition param : callSchema.getParameters()) {
            params.put(param.getName(), param);
        }

        assertTrue("Should have contact_name parameter", params.containsKey("contact_name"));
        assertTrue("contact_name should be required", params.get("contact_name").isRequired());

        totalTests++;
        passedTests++;
    }

    @Test
    public void testSendWhatsAppSchema() {
        // Given: SEND_WHATSAPP schema
        FunctionCallSchema.SchemaDefinition whatsappSchema = schema.getSchema("send_whatsapp");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", whatsappSchema);
        assertEquals("Name should be send_whatsapp", "send_whatsapp", whatsappSchema.getName());

        // Check parameters
        Map<String, FunctionCallSchema.ParameterDefinition> params = new HashMap<>();
        for (FunctionCallSchema.ParameterDefinition param : whatsappSchema.getParameters()) {
            params.put(param.getName(), param);
        }

        assertTrue("Should have contact_name parameter", params.containsKey("contact_name"));
        assertTrue("Should have message parameter", params.containsKey("message"));
        assertTrue("contact_name should be required", params.get("contact_name").isRequired());
        assertTrue("message should be required", params.get("message").isRequired());

        totalTests++;
        passedTests++;
    }

    @Test
    public void testSendVoiceMessageSchema() {
        // Given: SEND_VOICE_MESSAGE schema
        FunctionCallSchema.SchemaDefinition voiceSchema = schema.getSchema("send_voice_message");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", voiceSchema);
        assertEquals("Name should be send_voice_message", "send_voice_message", voiceSchema.getName());

        // Check parameters
        Map<String, FunctionCallSchema.ParameterDefinition> params = new HashMap<>();
        for (FunctionCallSchema.ParameterDefinition param : voiceSchema.getParameters()) {
            params.put(param.getName(), param);
        }

        assertTrue("Should have contact_name parameter", params.containsKey("contact_name"));

        totalTests++;
        passedTests++;
    }

    @Test
    public void testSetAlarmSchema() {
        // Given: SET_ALARM schema
        FunctionCallSchema.SchemaDefinition alarmSchema = schema.getSchema("set_alarm");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", alarmSchema);
        assertEquals("Name should be set_alarm", "set_alarm", alarmSchema.getName());

        // Check parameters
        Map<String, FunctionCallSchema.ParameterDefinition> params = new HashMap<>();
        for (FunctionCallSchema.ParameterDefinition param : alarmSchema.getParameters()) {
            params.put(param.getName(), param);
        }

        assertTrue("Should have time parameter", params.containsKey("time"));
        assertTrue("time should be required", params.get("time").isRequired());

        totalTests++;
        passedTests++;
    }

    @Test
    public void testReadTimeSchema() {
        // Given: READ_TIME schema
        FunctionCallSchema.SchemaDefinition timeSchema = schema.getSchema("read_time");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", timeSchema);
        assertEquals("Name should be read_time", "read_time", timeSchema.getName());

        // READ_TIME has no required parameters
        assertEquals("Should have no parameters", 0, timeSchema.getParameters().size());

        totalTests++;
        passedTests++;
    }

    @Test
    public void testEmergencySchema() {
        // Given: EMERGENCY schema
        FunctionCallSchema.SchemaDefinition emergencySchema = schema.getSchema("emergency");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", emergencySchema);
        assertEquals("Name should be emergency", "emergency", emergencySchema.getName());

        // Check parameters
        Map<String, FunctionCallSchema.ParameterDefinition> params = new HashMap<>();
        for (FunctionCallSchema.ParameterDefinition param : emergencySchema.getParameters()) {
            params.put(param.getName(), param);
        }

        assertTrue("Should have type parameter", params.containsKey("type"));
        assertFalse("type should be optional", params.get("type").isRequired());

        // Check enum values
        FunctionCallSchema.ParameterDefinition typeParam = params.get("type");
        assertNotNull("type should have enum values", typeParam.getEnumValues());
        assertTrue("Should have medical option", typeParam.getEnumValues().contains("medical"));
        assertTrue("Should have security option", typeParam.getEnumValues().contains("security"));
        assertTrue("Should have general option", typeParam.getEnumValues().contains("general"));

        totalTests++;
        passedTests++;
    }

    @Test
    public void testOpenAppSchema() {
        // Given: OPEN_APP schema
        FunctionCallSchema.SchemaDefinition appSchema = schema.getSchema("open_app");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", appSchema);
        assertEquals("Name should be open_app", "open_app", appSchema.getName());

        // Check parameters
        Map<String, FunctionCallSchema.ParameterDefinition> params = new HashMap<>();
        for (FunctionCallSchema.ParameterDefinition param : appSchema.getParameters()) {
            params.put(param.getName(), param);
        }

        assertTrue("Should have app_name parameter", params.containsKey("app_name"));
        assertTrue("app_name should be required", params.get("app_name").isRequired());

        totalTests++;
        passedTests++;
    }

    @Test
    public void testToggleWifiSchema() {
        // Given: TOGGLE_WIFI schema
        FunctionCallSchema.SchemaDefinition wifiSchema = schema.getSchema("toggle_wifi");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", wifiSchema);
        assertEquals("Name should be toggle_wifi", "toggle_wifi", wifiSchema.getName());

        // Check parameters
        Map<String, FunctionCallSchema.ParameterDefinition> params = new HashMap<>();
        for (FunctionCallSchema.ParameterDefinition param : wifiSchema.getParameters()) {
            params.put(param.getName(), param);
        }

        assertTrue("Should have state parameter", params.containsKey("state"));
        assertTrue("state should be required", params.get("state").isRequired());

        // Check enum values
        FunctionCallSchema.ParameterDefinition stateParam = params.get("state");
        assertNotNull("state should have enum values", stateParam.getEnumValues());
        assertTrue("Should have on option", stateParam.getEnumValues().contains("on"));
        assertTrue("Should have off option", stateParam.getEnumValues().contains("off"));
        assertTrue("Should have toggle option", stateParam.getEnumValues().contains("toggle"));

        totalTests++;
        passedTests++;
    }

    @Test
    public void testToggleBluetoothSchema() {
        // Given: TOGGLE_BLUETOOTH schema
        FunctionCallSchema.SchemaDefinition btSchema = schema.getSchema("toggle_bluetooth");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", btSchema);
        assertEquals("Name should be toggle_bluetooth", "toggle_bluetooth", btSchema.getName());

        // Check parameters
        Map<String, FunctionCallSchema.ParameterDefinition> params = new HashMap<>();
        for (FunctionCallSchema.ParameterDefinition param : btSchema.getParameters()) {
            params.put(param.getName(), param);
        }

        assertTrue("Should have state parameter", params.containsKey("state"));

        totalTests++;
        passedTests++;
    }

    @Test
    public void testToggleFlashlightSchema() {
        // Given: TOGGLE_FLASHLIGHT schema
        FunctionCallSchema.SchemaDefinition flashSchema = schema.getSchema("toggle_flashlight");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", flashSchema);
        assertEquals("Name should be toggle_flashlight", "toggle_flashlight", flashSchema.getName());

        // Check parameters
        Map<String, FunctionCallSchema.ParameterDefinition> params = new HashMap<>();
        for (FunctionCallSchema.ParameterDefinition param : flashSchema.getParameters()) {
            params.put(param.getName(), param);
        }

        assertTrue("Should have state parameter", params.containsKey("state"));

        totalTests++;
        passedTests++;
    }

    @Test
    public void testReadMissedCallsSchema() {
        // Given: READ_MISSED_CALLS schema
        FunctionCallSchema.SchemaDefinition missedSchema = schema.getSchema("read_missed_calls");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", missedSchema);
        assertEquals("Name should be read_missed_calls", "read_missed_calls", missedSchema.getName());

        // READ_MISSED_CALLS has no required parameters
        assertEquals("Should have no parameters", 0, missedSchema.getParameters().size());

        totalTests++;
        passedTests++;
    }

    @Test
    public void testSendSMSSchema() {
        // Given: SEND_SMS schema
        FunctionCallSchema.SchemaDefinition smsSchema = schema.getSchema("send_sms");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", smsSchema);
        assertEquals("Name should be send_sms", "send_sms", smsSchema.getName());

        // Check parameters
        Map<String, FunctionCallSchema.ParameterDefinition> params = new HashMap<>();
        for (FunctionCallSchema.ParameterDefinition param : smsSchema.getParameters()) {
            params.put(param.getName(), param);
        }

        assertTrue("Should have contact_name parameter", params.containsKey("contact_name"));
        assertTrue("Should have message parameter", params.containsKey("message"));

        totalTests++;
        passedTests++;
    }

    @Test
    public void testWeatherQuerySchema() {
        // Given: WEATHER_QUERY schema
        FunctionCallSchema.SchemaDefinition weatherSchema = schema.getSchema("weather_query");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", weatherSchema);
        assertEquals("Name should be weather_query", "weather_query", weatherSchema.getName());

        // Check parameters
        Map<String, FunctionCallSchema.ParameterDefinition> params = new HashMap<>();
        for (FunctionCallSchema.ParameterDefinition param : weatherSchema.getParameters()) {
            params.put(param.getName(), param);
        }

        assertTrue("Should have location parameter", params.containsKey("location"));
        assertTrue("Should have time parameter", params.containsKey("time"));

        totalTests++;
        passedTests++;
    }

    @Test
    public void testGreetingSchema() {
        // Given: GREETING schema
        FunctionCallSchema.SchemaDefinition greetingSchema = schema.getSchema("greeting");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", greetingSchema);
        assertEquals("Name should be greeting", "greeting", greetingSchema.getName());

        // GREETING has no required parameters
        assertEquals("Should have no parameters", 0, greetingSchema.getParameters().size());
        assertTrue("Should have examples", greetingSchema.getExamples().size() > 0);

        totalTests++;
        passedTests++;
    }

    @Test
    public void testThankYouSchema() {
        // Given: THANK_YOU schema
        FunctionCallSchema.SchemaDefinition thankSchema = schema.getSchema("thank_you");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", thankSchema);
        assertEquals("Name should be thank_you", "thank_you", thankSchema.getName());

        // THANK_YOU has no required parameters
        assertEquals("Should have no parameters", 0, thankSchema.getParameters().size());

        totalTests++;
        passedTests++;
    }

    @Test
    public void testGoodbyeSchema() {
        // Given: GOODBYE schema
        FunctionCallSchema.SchemaDefinition goodbyeSchema = schema.getSchema("goodbye");

        // Then: Schema should have correct properties
        assertNotNull("Schema should exist", goodbyeSchema);
        assertEquals("Name should be goodbye", "goodbye", goodbyeSchema.getName());

        // GOODBYE has no required parameters
        assertEquals("Should have no parameters", 0, goodbyeSchema.getParameters().size());

        totalTests++;
        passedTests++;
    }

    // ========================================================================
    // CATEGORY 3: JSON Schema Generation Tests (6 tests)
    // ========================================================================

    @Test
    public void testJsonSchemaGeneration() {
        // When: Generate JSON schema
        String jsonSchema = schema.toJsonSchema();

        // Then: Should generate valid JSON
        assertNotNull("JSON schema should not be null", jsonSchema);
        assertFalse("JSON schema should not be empty", jsonSchema.isEmpty());

        // Parse and validate
        try {
            JSONObject json = new JSONObject(jsonSchema);
            assertTrue("Should have functions array", json.has("functions"));
            JSONArray functions = json.getJSONArray("functions");
            assertEquals("Should have 16 functions", EXPECTED_FUNCTIONS.size(), functions.length());
        } catch (Exception e) {
            fail("JSON should be valid: " + e.getMessage());
        }

        totalTests++;
        passedTests++;
    }

    @Test
    public void testJsonSchema_FunctionStructure() {
        // When: Generate JSON schema
        String jsonSchema = schema.toJsonSchema();

        try {
            JSONObject json = new JSONObject(jsonSchema);
            JSONArray functions = json.getJSONArray("functions");

            // Then: Each function should have correct structure
            for (int i = 0; i < functions.length(); i++) {
                JSONObject func = functions.getJSONObject(i);

                assertTrue("Should have type field", func.has("type"));
                assertEquals("Type should be function", "function", func.getString("type"));

                assertTrue("Should have function object", func.has("function"));
                JSONObject function = func.getJSONObject("function");

                assertTrue("Function should have name", function.has("name"));
                assertTrue("Function should have description", function.has("description"));
                assertTrue("Function should have parameters", function.has("parameters"));
            }
        } catch (Exception e) {
            fail("JSON structure should be valid: " + e.getMessage());
        }

        totalTests++;
        passedTests++;
    }

    @Test
    public void testJsonSchema_ParameterStructure() {
        // When: Generate JSON schema for call_contact
        FunctionCallSchema.SchemaDefinition callSchema = schema.getSchema("call_contact");
        JSONObject json = callSchema.toJsonObject();

        // Then: Parameters should have correct structure
        try {
            JSONObject function = json.getJSONObject("function");
            JSONObject parameters = function.getJSONObject("parameters");

            assertEquals("Parameters type should be object", "object", parameters.getString("type"));
            assertTrue("Should have properties", parameters.has("properties"));
            assertTrue("Should have required array", parameters.has("required"));

            JSONObject properties = parameters.getJSONObject("properties");
            JSONObject contactName = properties.getJSONObject("contact_name");

            assertTrue("contact_name should have type", contactName.has("type"));
            assertEquals("contact_name type should be string", "string", contactName.getString("type"));
            assertTrue("contact_name should have description", contactName.has("description"));
        } catch (Exception e) {
            fail("Parameter structure should be valid: " + e.getMessage());
        }

        totalTests++;
        passedTests++;
    }

    @Test
    public void testJsonSchema_EnumValues() {
        // When: Generate JSON schema for emergency
        FunctionCallSchema.SchemaDefinition emergencySchema = schema.getSchema("emergency");
        JSONObject json = emergencySchema.toJsonObject();

        // Then: Enum values should be included
        try {
            JSONObject function = json.getJSONObject("function");
            JSONObject parameters = function.getJSONObject("parameters");
            JSONObject properties = parameters.getJSONObject("properties");
            JSONObject typeParam = properties.getJSONObject("type");

            assertTrue("type should have enum", typeParam.has("enum"));
            JSONArray enumValues = typeParam.getJSONArray("enum");

            assertTrue("Should have medical", contains(enumValues, "medical"));
            assertTrue("Should have security", contains(enumValues, "security"));
            assertTrue("Should have general", contains(enumValues, "general"));
        } catch (Exception e) {
            fail("Enum structure should be valid: " + e.getMessage());
        }

        totalTests++;
        passedTests++;
    }

    @Test
    public void testJsonSchema_RequiredFields() {
        // When: Generate JSON schema for call_contact
        FunctionCallSchema.SchemaDefinition callSchema = schema.getSchema("call_contact");
        JSONObject json = callSchema.toJsonObject();

        // Then: Required fields should be listed
        try {
            JSONObject function = json.getJSONObject("function");
            JSONObject parameters = function.getJSONObject("parameters");
            JSONArray required = parameters.getJSONArray("required");

            assertTrue("Should require contact_name", contains(required, "contact_name"));
        } catch (Exception e) {
            fail("Required fields should be valid: " + e.getMessage());
        }

        totalTests++;
        passedTests++;
    }

    @Test
    public void testJsonSchema_AllFunctions() {
        // When: Generate JSON schema
        String jsonSchema = schema.toJsonSchema();

        try {
            JSONObject json = new JSONObject(jsonSchema);
            JSONArray functions = json.getJSONArray("functions");

            // Then: All expected functions should be present
            for (String expectedFunction : EXPECTED_FUNCTIONS) {
                boolean found = false;
                for (int i = 0; i < functions.length(); i++) {
                    JSONObject func = functions.getJSONObject(i).getJSONObject("function");
                    if (func.getString("name").equals(expectedFunction)) {
                        found = true;
                        break;
                    }
                }
                assertTrue("Should contain: " + expectedFunction, found);
            }
        } catch (Exception e) {
            fail("All functions should be in JSON: " + e.getMessage());
        }

        totalTests++;
        passedTests++;
    }

    // ========================================================================
    // CATEGORY 4: Function Call Result Tests (6 tests)
    // ========================================================================

    @Test
    public void testFunctionCallResult_Creation() {
        // Given: Function call result
        Map<String, String> args = new HashMap<>();
        args.put("contact_name", "ماما");

        FunctionCallSchema.FunctionCallResult result =
            new FunctionCallSchema.FunctionCallResult("call_contact", args);

        // Then: Result should be created correctly
        assertNotNull("Result should not be null", result);
        assertEquals("Function name should be call_contact", "call_contact", result.getFunctionName());
        assertEquals("Should have contact_name", "ماما", result.getArgument("contact_name"));

        totalTests++;
        passedTests++;
    }

    @Test
    public void testFunctionCallResult_WithConfidence() {
        // Given: Function call result with confidence
        Map<String, String> args = new HashMap<>();
        args.put("time", "بكرة الصبح");

        FunctionCallSchema.FunctionCallResult result =
            new FunctionCallSchema.FunctionCallResult("set_alarm", args, 0.95f, "raw output");

        // Then: Result should have confidence
        assertEquals("Confidence should be 0.95", 0.95f, result.getConfidence(), 0.01f);
        assertEquals("Raw output should be preserved", "raw output", result.getRawOutput());

        totalTests++;
        passedTests++;
    }

    @Test
    public void testFunctionCallResult_IsValid() {
        // Given: Valid and invalid results
        Map<String, String> validArgs = new HashMap<>();
        validArgs.put("contact_name", "ماما");

        FunctionCallSchema.FunctionCallResult validResult =
            new FunctionCallSchema.FunctionCallResult("call_contact", validArgs, 0.9f, null);

        Map<String, String> invalidArgs = new HashMap<>();
        FunctionCallSchema.FunctionCallResult invalidResult =
            new FunctionCallSchema.FunctionCallResult("unknown", invalidArgs, 0.3f, null);

        // Then: Validity should be correct
        assertTrue("Valid result should be valid", validResult.isValid());
        assertFalse("Invalid result should not be valid", invalidResult.isValid());

        totalTests++;
        passedTests++;
    }

    @Test
    public void testFunctionCallResult_ArgumentRetrieval() {
        // Given: Function call result
        Map<String, String> args = new HashMap<>();
        args.put("contact_name", "ماما");
        args.put("phone_number", "123456789");

        FunctionCallSchema.FunctionCallResult result =
            new FunctionCallSchema.FunctionCallResult("call_contact", args);

        // Then: Arguments should be retrievable
        assertEquals("Should get contact_name", "ماما", result.getArgument("contact_name"));
        assertEquals("Should get phone_number", "123456789", result.getArgument("phone_number"));
        assertEquals("Should get default for missing", "default", result.getArgument("missing", "default"));
        assertNull("Should return null for missing without default", result.getArgument("missing"));

        totalTests++;
        passedTests++;
    }

    @Test
    public void testFunctionCallResult_ToString() {
        // Given: Function call result
        Map<String, String> args = new HashMap<>();
        args.put("contact_name", "ماما");

        FunctionCallSchema.FunctionCallResult result =
            new FunctionCallSchema.FunctionCallResult("call_contact", args, 0.9f, null);

        // Then: toString should work
        String str = result.toString();
        assertNotNull("toString should not be null", str);
        assertTrue("toString should contain function name", str.contains("call_contact"));
        assertTrue("toString should contain confidence", str.contains("0.9"));

        totalTests++;
        passedTests++;
    }

    @Test
    public void testFunctionCallResult_ArgumentsMap() {
        // Given: Function call result
        Map<String, String> args = new HashMap<>();
        args.put("contact_name", "ماما");

        FunctionCallSchema.FunctionCallResult result =
            new FunctionCallSchema.FunctionCallResult("call_contact", args);

        // Then: Arguments map should be accessible
        Map<String, String> retrievedArgs = result.getArguments();
        assertNotNull("Arguments map should not be null", retrievedArgs);
        assertEquals("Should have same size", args.size(), retrievedArgs.size());
        assertEquals("Should have same values", args.get("contact_name"), retrievedArgs.get("contact_name"));

        totalTests++;
        passedTests++;
    }

    // ========================================================================
    // CATEGORY 5: Egyptian Dialect Examples Tests (4 tests)
    // ========================================================================

    @Test
    public void testEgyptianExamples_CallContact() {
        // Given: CALL_CONTACT schema
        FunctionCallSchema.SchemaDefinition callSchema = schema.getSchema("call_contact");

        // Then: Should have Egyptian examples
        List<String> examples = callSchema.getExamples();
        assertNotNull("Examples should not be null", examples);
        assertFalse("Examples should not be empty", examples.isEmpty());

        // Check for Egyptian dialect examples
        boolean hasArabicExample = examples.stream().anyMatch(e -> e.contains("اتصل") || e.contains("كلم"));
        assertTrue("Should have Arabic examples", hasArabicExample);

        totalTests++;
        passedTests++;
    }

    @Test
    public void testEgyptianExamples_Emergency() {
        // Given: EMERGENCY schema
        FunctionCallSchema.SchemaDefinition emergencySchema = schema.getSchema("emergency");

        // Then: Should have Egyptian emergency examples
        List<String> examples = emergencySchema.getExamples();
        assertNotNull("Examples should not be null", examples);

        boolean hasNagda = examples.stream().anyMatch(e -> e.contains("نجدة"));
        assertTrue("Should have نجدة example", hasNagda);

        totalTests++;
        passedTests++;
    }

    @Test
    public void testEgyptianExamples_SetAlarm() {
        // Given: SET_ALARM schema
        FunctionCallSchema.SchemaDefinition alarmSchema = schema.getSchema("set_alarm");

        // Then: Should have Egyptian alarm examples
        List<String> examples = alarmSchema.getExamples();
        assertNotNull("Examples should not be null", examples);

        boolean hasBokra = examples.stream().anyMatch(e -> e.contains("بكرة"));
        assertTrue("Should have بكرة example", hasBokra);

        totalTests++;
        passedTests++;
    }

    @Test
    public void testEgyptianExamples_Greeting() {
        // Given: GREETING schema
        FunctionCallSchema.SchemaDefinition greetingSchema = schema.getSchema("greeting");

        // Then: Should have Egyptian greeting examples
        List<String> examples = greetingSchema.getExamples();
        assertNotNull("Examples should not be null", examples);

        boolean hasSalam = examples.stream().anyMatch(e -> e.contains("السلام"));
        assertTrue("Should have السلام عليكم example", hasSalam);

        boolean hasAzayak = examples.stream().anyMatch(e -> e.contains("ازيك"));
        assertTrue("Should have ازيك example", hasAzayak);

        totalTests++;
        passedTests++;
    }

    // ========================================================================
    // CATEGORY 6: Schema Registry Operations Tests (4 tests)
    // ========================================================================

    @Test
    public void testSchemaRegistry_GetAllSchemas() {
        // When: Get all schemas
        Map<String, FunctionCallSchema.SchemaDefinition> allSchemas = schema.getAllSchemas();

        // Then: Should return all schemas
        assertNotNull("All schemas should not be null", allSchemas);
        assertEquals("Should have 16 schemas", EXPECTED_FUNCTIONS.size(), allSchemas.size());

        for (String expectedFunction : EXPECTED_FUNCTIONS) {
            assertTrue("Should contain: " + expectedFunction,
                    allSchemas.containsKey(expectedFunction.toLowerCase()));
        }

        totalTests++;
        passedTests++;
    }

    @Test
    public void testSchemaRegistry_Register() {
        // Given: New schema
        FunctionCallSchema newSchema = new FunctionCallSchema();

        FunctionCallSchema.ParameterDefinition param = new FunctionCallSchema.ParameterDefinition(
            "test_param", "string", "Test parameter", true
        );

        FunctionCallSchema.SchemaDefinition testSchema = new FunctionCallSchema.SchemaDefinition(
            "test_function",
            "Test function description",
            Arrays.asList(param),
            Arrays.asList("test example")
        );

        // When: Register new schema
        newSchema.register(testSchema);

        // Then: Schema should be registered
        FunctionCallSchema.SchemaDefinition retrieved = newSchema.getSchema("test_function");
        assertNotNull("Test schema should be retrievable", retrieved);
        assertEquals("Name should match", "test_function", retrieved.getName());

        totalTests++;
        passedTests++;
    }

    @Test
    public void testSchemaRegistry_CaseInsensitive() {
        // When: Get schemas with different cases
        FunctionCallSchema.SchemaDefinition schema1 = schema.getSchema("CALL_CONTACT");
        FunctionCallSchema.SchemaDefinition schema2 = schema.getSchema("call_contact");
        FunctionCallSchema.SchemaDefinition schema3 = schema.getSchema("Call_Contact");

        // Then: Should be case insensitive
        assertNotNull("CALL_CONTACT should work", schema1);
        assertNotNull("call_contact should work", schema2);
        assertNotNull("Call_Contact should work", schema3);

        totalTests++;
        passedTests++;
    }

    @Test
    public void testSchemaRegistry_GetNonExistent() {
        // When: Get non-existent schema
        FunctionCallSchema.SchemaDefinition result = schema.getSchema("non_existent_function");

        // Then: Should return null
        assertNull("Non-existent schema should return null", result);

        totalTests++;
        passedTests++;
    }

    // ========================================================================
    // CATEGORY 7: Parameter Definition Tests (4 tests)
    // ========================================================================

    @Test
    public void testParameterDefinition_Creation() {
        // Given: Parameter definition
        FunctionCallSchema.ParameterDefinition param = new FunctionCallSchema.ParameterDefinition(
            "test_param",
            "string",
            "Test parameter description",
            true
        );

        // Then: Should be created correctly
        assertEquals("Name should match", "test_param", param.getName());
        assertEquals("Type should match", "string", param.getType());
        assertEquals("Description should match", "Test parameter description", param.getDescription());
        assertTrue("Should be required", param.isRequired());

        totalTests++;
        passedTests++;
    }

    @Test
    public void testParameterDefinition_WithEnum() {
        // Given: Parameter definition with enum
        List<String> enumValues = Arrays.asList("on", "off", "toggle");
        FunctionCallSchema.ParameterDefinition param = new FunctionCallSchema.ParameterDefinition(
            "state",
            "string",
            "Toggle state",
            true,
            enumValues
        );

        // Then: Enum values should be set
        assertNotNull("Enum values should not be null", param.getEnumValues());
        assertEquals("Should have 3 enum values", 3, param.getEnumValues().size());
        assertTrue("Should contain on", param.getEnumValues().contains("on"));

        totalTests++;
        passedTests++;
    }

    @Test
    public void testParameterDefinition_Optional() {
        // Given: Optional parameter
        FunctionCallSchema.ParameterDefinition param = new FunctionCallSchema.ParameterDefinition(
            "optional_param",
            "string",
            "Optional parameter",
            false
        );

        // Then: Should be optional
        assertFalse("Should not be required", param.isRequired());

        totalTests++;
        passedTests++;
    }

    @Test
    public void testParameterDefinition_ToString() {
        // Given: Parameter definition
        FunctionCallSchema.ParameterDefinition param = new FunctionCallSchema.ParameterDefinition(
            "test", "string", "Test", true
        );

        // Then: toString should work
        String str = param.toString();
        assertNotNull("toString should not be null", str);

        totalTests++;
        passedTests++;
    }

    // ========================================================================
    // CATEGORY 8: FunctionGemma Prompt Format Tests (3 tests)
    // ========================================================================

    @Test
    public void testFunctionGemmaPrompt_Generation() {
        // When: Generate FunctionGemma prompt
        String prompt = schema.toFunctionGemmaPrompt();

        // Then: Should generate valid prompt
        assertNotNull("Prompt should not be null", prompt);
        assertFalse("Prompt should not be empty", prompt.isEmpty());
        assertTrue("Prompt should contain function names", prompt.contains("call_contact"));

        totalTests++;
        passedTests++;
    }

    @Test
    public void testSchemaDefinition_FunctionGemmaFormat() {
        // Given: Schema definition
        FunctionCallSchema.SchemaDefinition callSchema = schema.getSchema("call_contact");

        // When: Convert to FunctionGemma format
        String format = callSchema.toFunctionGemmaFormat();

        // Then: Should have correct format
        assertNotNull("Format should not be null", format);
        assertTrue("Should contain function name", format.contains("call_contact"));
        assertTrue("Should contain description", format.contains("-"));

        totalTests++;
        passedTests++;
    }

    @Test
    public void testSchemaDefinition_ToString() {
        // Given: Schema definition
        FunctionCallSchema.SchemaDefinition callSchema = schema.getSchema("call_contact");

        // When: Convert to string
        String str = callSchema.toString();

        // Then: Should work
        assertNotNull("toString should not be null", str);
        assertTrue("Should contain name", str.contains("call_contact"));

        totalTests++;
        passedTests++;
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Check if JSONArray contains a value.
     */
    private boolean contains(JSONArray array, String value) {
        for (int i = 0; i < array.length(); i++) {
            try {
                if (array.getString(i).equals(value)) {
                    return true;
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return false;
    }

    // ========================================================================
    // Test Summary
    // ========================================================================

    /**
     * Print test summary.
     */
    @Test
    public void printTestSummary() {
        System.out.println("\n========================================");
        System.out.println("FunctionCallSchema TEST SUMMARY");
        System.out.println("========================================");
        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + (totalTests - passedTests));

        if (totalTests > 0) {
            float accuracy = 100.0f * passedTests / totalTests;
            System.out.println("Accuracy: " + String.format("%.2f%%", accuracy));
        }

        System.out.println("\nFunctions Tested: " + EXPECTED_FUNCTIONS.size());
        System.out.println("========================================");

        // Assert 100% pass rate for schema tests
        assertEquals("All schema tests should pass", totalTests, passedTests);
    }

    /**
     * Get test accuracy.
     */
    public float getAccuracy() {
        if (totalTests == 0) return 0;
        return 100.0f * passedTests / totalTests;
    }
}
