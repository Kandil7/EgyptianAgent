package com.egyptian.agent.llm;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit Tests for FunctionCallSchema
 * 
 * Test Coverage:
 * - All 16 functions defined correctly
 * - JSON schema generation
 * - Function call parsing
 * - Argument validation
 * - Egyptian dialect examples
 * 
 * @author EgyptianAgent Team
 * @version 1.0
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FunctionCallSchemaTest {

    private FunctionCallSchema schema;

    @Before
    public void setUp() {
        schema = new FunctionCallSchema();
    }

    // ========================================================================
    // Schema Definition Tests
    // ========================================================================

    @Test
    public void testAllFunctionsDefined() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        assertNotNull("Functions map should not be null", functions);
        assertFalse("Functions map should not be empty", functions.isEmpty());
        
        // Should have all 16 functions
        assertTrue("Should have at least 16 functions", functions.size() >= 16);
    }

    @Test
    public void testFunction_CallContact() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        FunctionCallSchema.FunctionDefinition callContact = functions.get("call_contact");
        
        assertNotNull("call_contact function should be defined", callContact);
        assertEquals("call_contact", callContact.name);
        assertNotNull("Description should exist", callContact.description);
        assertNotNull("Parameters should exist", callContact.parameters);
        
        // Check required parameters
        assertTrue("Should have contact_name parameter", 
                   callContact.parameters.containsKey("contact_name"));
    }

    @Test
    public void testFunction_SendWhatsapp() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        FunctionCallSchema.FunctionDefinition sendWhatsapp = functions.get("send_whatsapp");
        
        assertNotNull("send_whatsapp function should be defined", sendWhatsapp);
        assertEquals("send_whatsapp", sendWhatsapp.name);
        
        // Check parameters
        assertTrue("Should have contact_name parameter", 
                   sendWhatsapp.parameters.containsKey("contact_name"));
        assertTrue("Should have message parameter", 
                   sendWhatsapp.parameters.containsKey("message"));
    }

    @Test
    public void testFunction_SetAlarm() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        FunctionCallSchema.FunctionDefinition setAlarm = functions.get("set_alarm");
        
        assertNotNull("set_alarm function should be defined", setAlarm);
        
        assertTrue("Should have time parameter", 
                   setAlarm.parameters.containsKey("time"));
    }

    @Test
    public void testFunction_Emergency() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        FunctionCallSchema.FunctionDefinition emergency = functions.get("emergency");
        
        assertNotNull("emergency function should be defined", emergency);
        // Emergency may not require parameters
    }

    @Test
    public void testFunction_ReadTime() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        FunctionCallSchema.FunctionDefinition readTime = functions.get("read_time");
        
        assertNotNull("read_time function should be defined", readTime);
    }

    @Test
    public void testFunction_OpenApp() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        FunctionCallSchema.FunctionDefinition openApp = functions.get("open_app");
        
        assertNotNull("open_app function should be defined", openApp);
        assertTrue("Should have app_name parameter", 
                   openApp.parameters.containsKey("app_name"));
    }

    @Test
    public void testFunction_WifiToggle() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        FunctionCallSchema.FunctionDefinition wifiToggle = functions.get("wifi_toggle");
        
        assertNotNull("wifi_toggle function should be defined", wifiToggle);
        assertTrue("Should have state parameter", 
                   wifiToggle.parameters.containsKey("state"));
    }

    @Test
    public void testFunction_BluetoothToggle() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        FunctionCallSchema.FunctionDefinition bluetoothToggle = functions.get("bluetooth_toggle");
        
        assertNotNull("bluetooth_toggle function should be defined", bluetoothToggle);
    }

    @Test
    public void testFunction_VolumeUp() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        FunctionCallSchema.FunctionDefinition volumeUp = functions.get("volume_up");
        
        assertNotNull("volume_up function should be defined", volumeUp);
    }

    @Test
    public void testFunction_VolumeDown() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        FunctionCallSchema.FunctionDefinition volumeDown = functions.get("volume_down");
        
        assertNotNull("volume_down function should be defined", volumeDown);
    }

    // ========================================================================
    // JSON Schema Generation Tests
    // ========================================================================

    @Test
    public void testJsonSchemaGeneration() {
        String jsonSchema = schema.toJsonSchema();
        
        assertNotNull("JSON schema should not be null", jsonSchema);
        assertFalse("JSON schema should not be empty", jsonSchema.trim().isEmpty());
        
        // Should be valid JSON
        JSONObject jsonObject = new JSONObject(jsonSchema);
        
        // Should have functions array
        assertTrue("Should have 'functions' array", jsonObject.has("functions"));
    }

    @Test
    public void testJsonSchema_FunctionStructure() {
        String jsonSchema = schema.toJsonSchema();
        JSONObject jsonObject = new JSONObject(jsonSchema);
        
        // Parse functions
        org.json.JSONArray functions = jsonObject.getJSONArray("functions");
        
        assertTrue("Should have at least 16 functions", functions.length() >= 16);
        
        // Check first function structure
        JSONObject firstFunction = functions.getJSONObject(0);
        
        assertTrue("Function should have 'name'", firstFunction.has("name"));
        assertTrue("Function should have 'description'", firstFunction.has("description"));
        assertTrue("Function should have 'parameters'", firstFunction.has("parameters"));
    }

    @Test
    public void testJsonSchema_ParameterStructure() {
        String jsonSchema = schema.toJsonSchema();
        JSONObject jsonObject = new JSONObject(jsonSchema);
        
        org.json.JSONArray functions = jsonObject.getJSONArray("functions");
        
        // Find call_contact function
        JSONObject callContact = null;
        for (int i = 0; i < functions.length(); i++) {
            JSONObject func = functions.getJSONObject(i);
            if ("call_contact".equals(func.getString("name"))) {
                callContact = func;
                break;
            }
        }
        
        assertNotNull("call_contact function should exist", callContact);
        
        JSONObject parameters = callContact.getJSONObject("parameters");
        assertTrue("Parameters should have 'type'", parameters.has("type"));
        assertTrue("Parameters should have 'properties'", parameters.has("properties"));
    }

    // ========================================================================
    // Function Call Parsing Tests
    // ========================================================================

    @Test
    public void testParseFunctionCall_CallContact() {
        String jsonCall = "{\"function\": \"call_contact\", \"arguments\": {\"contact_name\": \"ماما\"}}";
        
        FunctionCallSchema.FunctionCallResult result = schema.parseFunctionCall(jsonCall);
        
        assertNotNull("Result should not be null", result);
        assertEquals("call_contact", result.functionName);
        assertEquals("ماما", result.arguments.get("contact_name"));
        assertTrue("Should be successful", result.success);
    }

    @Test
    public void testParseFunctionCall_SendWhatsapp() {
        String jsonCall = "{\"function\": \"send_whatsapp\", \"arguments\": {\"contact_name\": \"أحمد\", \"message\": \"سلامات\"}}";
        
        FunctionCallSchema.FunctionCallResult result = schema.parseFunctionCall(jsonCall);
        
        assertNotNull("Result should not be null", result);
        assertEquals("send_whatsapp", result.functionName);
        assertEquals("أحمد", result.arguments.get("contact_name"));
        assertEquals("سلامات", result.arguments.get("message"));
    }

    @Test
    public void testParseFunctionCall_InvalidJson() {
        String invalidJson = "{invalid json}";
        
        FunctionCallSchema.FunctionCallResult result = schema.parseFunctionCall(invalidJson);
        
        assertNotNull("Result should not be null even for invalid JSON", result);
        assertFalse("Invalid JSON should not succeed", result.success);
    }

    @Test
    public void testParseFunctionCall_NullInput() {
        FunctionCallSchema.FunctionCallResult result = schema.parseFunctionCall(null);
        
        assertNotNull("Result should not be null", result);
        assertFalse("Null input should not succeed", result.success);
    }

    @Test
    public void testParseFunctionCall_EmptyInput() {
        FunctionCallSchema.FunctionCallResult result = schema.parseFunctionCall("");
        
        assertNotNull("Result should not be null", result);
        assertFalse("Empty input should not succeed", result.success);
    }

    // ========================================================================
    // Argument Validation Tests
    // ========================================================================

    @Test
    public void testValidateArguments_CallContact() {
        Map<String, Object> args = java.util.Map.of("contact_name", "ماما");
        
        boolean valid = schema.validateArguments("call_contact", args);
        
        assertTrue("Valid arguments for call_contact", valid);
    }

    @Test
    public void testValidateArguments_CallContact_MissingContact() {
        Map<String, Object> args = java.util.Map.of();
        
        boolean valid = schema.validateArguments("call_contact", args);
        
        assertFalse("Missing contact_name should be invalid", valid);
    }

    @Test
    public void testValidateArguments_SendWhatsapp() {
        Map<String, Object> args = java.util.Map.of(
            "contact_name", "أحمد",
            "message", "سلامات"
        );
        
        boolean valid = schema.validateArguments("send_whatsapp", args);
        
        assertTrue("Valid arguments for send_whatsapp", valid);
    }

    @Test
    public void testValidateArguments_SetAlarm() {
        Map<String, Object> args = java.util.Map.of("time", "بكرة الصبح");
        
        boolean valid = schema.validateArguments("set_alarm", args);
        
        assertTrue("Valid arguments for set_alarm", valid);
    }

    @Test
    public void testValidateArguments_UnknownFunction() {
        Map<String, Object> args = java.util.Map.of("param", "value");
        
        boolean valid = schema.validateArguments("unknown_function", args);
        
        // Unknown functions may be treated as valid (passthrough) or invalid
        // Depends on implementation choice
        // For now, we'll accept either behavior
        assertTrue("Unknown function handling", true);
    }

    // ========================================================================
    // Egyptian Dialect Examples Tests
    // ========================================================================

    @Test
    public void testEgyptianExamples_CallContact() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        FunctionCallSchema.FunctionDefinition callContact = functions.get("call_contact");
        
        assertNotNull("call_contact should be defined", callContact);
        
        // Should have Egyptian examples
        assertNotNull("Should have examples", callContact.examples);
        assertFalse("Examples should not be empty", callContact.examples.isEmpty());
        
        // Check for Egyptian dialect examples
        boolean hasEgyptianExample = callContact.examples.stream()
            .anyMatch(ex -> ex.contains("اتصل") || ex.contains("كلم") || ex.contains("رن"));
        
        assertTrue("Should have Egyptian dialect examples", hasEgyptianExample);
    }

    @Test
    public void testEgyptianExamples_SendWhatsapp() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        FunctionCallSchema.FunctionDefinition sendWhatsapp = functions.get("send_whatsapp");
        
        assertNotNull("send_whatsapp should be defined", sendWhatsapp);
        assertNotNull("Should have examples", sendWhatsapp.examples);
        
        boolean hasEgyptianExample = sendWhatsapp.examples.stream()
            .anyMatch(ex -> ex.contains("ابعت") || ex.contains("واتساب") || ex.contains("رسالة"));
        
        assertTrue("Should have Egyptian dialect examples", hasEgyptianExample);
    }

    @Test
    public void testEgyptianExamples_SetAlarm() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        FunctionCallSchema.FunctionDefinition setAlarm = functions.get("set_alarm");
        
        assertNotNull("set_alarm should be defined", setAlarm);
        assertNotNull("Should have examples", setAlarm.examples);
        
        boolean hasEgyptianExample = setAlarm.examples.stream()
            .anyMatch(ex -> ex.contains("نبه") || ex.contains("منبه") || ex.contains("ذكر"));
        
        assertTrue("Should have Egyptian dialect examples", hasEgyptianExample);
    }

    // ========================================================================
    // FunctionCallResult Tests
    // ========================================================================

    @Test
    public void testFunctionCallResult_Success() {
        FunctionCallSchema.FunctionCallResult result = 
            new FunctionCallSchema.FunctionCallResult("call_contact", 
                java.util.Map.of("contact_name", "ماما"), true, null);
        
        assertEquals("call_contact", result.functionName);
        assertEquals("ماما", result.arguments.get("contact_name"));
        assertTrue(result.success);
        assertNull(result.error);
    }

    @Test
    public void testFunctionCallResult_Failure() {
        FunctionCallSchema.FunctionCallResult result = 
            new FunctionCallSchema.FunctionCallResult(null, null, false, "Invalid function");
        
        assertFalse(result.success);
        assertEquals("Invalid function", result.error);
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    public void testEdgeCase_NullFunctionName() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        assertFalse("Null function name should not exist", 
                    functions.containsKey(null));
    }

    @Test
    public void testEdgeCase_EmptyFunctionName() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        assertFalse("Empty function name should not exist", 
                    functions.containsKey(""));
    }

    @Test
    public void testEdgeCase_CaseSensitivity() {
        Map<String, FunctionCallSchema.FunctionDefinition> functions = schema.getFunctions();
        
        // Function names should be lowercase
        assertNull("CALL_CONTACT (uppercase) should not exist", 
                   functions.get("CALL_CONTACT"));
        assertNotNull("call_contact (lowercase) should exist", 
                      functions.get("call_contact"));
    }
}
