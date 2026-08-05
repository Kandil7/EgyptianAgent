package com.egyptian.agent.llm;

import org.json.JSONArray;
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
 * - JSON schema generation (OpenAI function-calling format)
 * - Schema registry lookups (getSchema / isSupported / getFunctionNames)
 * - Required parameter definitions
 * - FunctionCallResult validity
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
    // Helpers
    // ========================================================================

    private FunctionCallSchema.SchemaDefinition def(String functionName) {
        return schema.getAllSchemas().get(functionName);
    }

    private boolean hasParameter(FunctionCallSchema.SchemaDefinition def, String paramName) {
        if (def == null || def.getParameters() == null) {
            return false;
        }
        for (FunctionCallSchema.ParameterDefinition param : def.getParameters()) {
            if (paramName.equals(param.getName())) {
                return true;
            }
        }
        return false;
    }

    // ========================================================================
    // Schema Definition Tests
    // ========================================================================

    @Test
    public void testAllFunctionsDefined() {
        Map<String, FunctionCallSchema.SchemaDefinition> functions = schema.getAllSchemas();
        
        assertNotNull("Functions map should not be null", functions);
        assertFalse("Functions map should not be empty", functions.isEmpty());
        
        // Should have all 16 functions
        assertTrue("Should have at least 16 functions", functions.size() >= 16);
    }

    @Test
    public void testFunction_CallContact() {
        FunctionCallSchema.SchemaDefinition callContact = def("call_contact");
        
        assertNotNull("call_contact function should be defined", callContact);
        assertEquals("call_contact", callContact.getName());
        assertNotNull("Description should exist", callContact.getDescription());
        assertNotNull("Parameters should exist", callContact.getParameters());
        
        // Check required parameters
        assertTrue("Should have contact_name parameter", 
                   hasParameter(callContact, "contact_name"));
    }

    @Test
    public void testFunction_SendWhatsapp() {
        FunctionCallSchema.SchemaDefinition sendWhatsapp = def("send_whatsapp");
        
        assertNotNull("send_whatsapp function should be defined", sendWhatsapp);
        assertEquals("send_whatsapp", sendWhatsapp.getName());
        
        // Check parameters
        assertTrue("Should have contact_name parameter", 
                   hasParameter(sendWhatsapp, "contact_name"));
        assertTrue("Should have message parameter", 
                   hasParameter(sendWhatsapp, "message"));
    }

    @Test
    public void testFunction_SetAlarm() {
        FunctionCallSchema.SchemaDefinition setAlarm = def("set_alarm");
        
        assertNotNull("set_alarm function should be defined", setAlarm);
        
        assertTrue("Should have time parameter", 
                   hasParameter(setAlarm, "time"));
    }

    @Test
    public void testFunction_Emergency() {
        FunctionCallSchema.SchemaDefinition emergency = def("emergency");
        
        assertNotNull("emergency function should be defined", emergency);
        // Emergency may not require parameters
    }

    @Test
    public void testFunction_ReadTime() {
        FunctionCallSchema.SchemaDefinition readTime = def("read_time");
        
        assertNotNull("read_time function should be defined", readTime);
    }

    @Test
    public void testFunction_OpenApp() {
        FunctionCallSchema.SchemaDefinition openApp = def("open_app");
        
        assertNotNull("open_app function should be defined", openApp);
        assertTrue("Should have app_name parameter", 
                   hasParameter(openApp, "app_name"));
    }

    @Test
    public void testFunction_WifiToggle() {
        FunctionCallSchema.SchemaDefinition wifiToggle = def("toggle_wifi");
        
        assertNotNull("toggle_wifi function should be defined", wifiToggle);
        assertTrue("Should have state parameter", 
                   hasParameter(wifiToggle, "state"));
    }

    @Test
    public void testFunction_BluetoothToggle() {
        FunctionCallSchema.SchemaDefinition bluetoothToggle = def("toggle_bluetooth");
        
        assertNotNull("toggle_bluetooth function should be defined", bluetoothToggle);
        assertTrue("Should have state parameter", 
                   hasParameter(bluetoothToggle, "state"));
    }

    @Test
    public void testFunction_NoVolumeFunctions() {
        // The schema registry intentionally has no volume functions
        assertNull("volume_up should not be defined", def("volume_up"));
        assertNull("volume_down should not be defined", def("volume_down"));
    }

    // ========================================================================
    // JSON Schema Generation Tests
    // ========================================================================

    @Test
    public void testJsonSchemaGeneration() throws Exception {
        String jsonSchema = schema.toJsonSchema();
        
        assertNotNull("JSON schema should not be null", jsonSchema);
        assertFalse("JSON schema should not be empty", jsonSchema.trim().isEmpty());
        
        // Should be valid JSON
        JSONObject jsonObject = new JSONObject(jsonSchema);
        
        // Should have functions array
        assertTrue("Should have 'functions' array", jsonObject.has("functions"));
    }

    @Test
    public void testJsonSchema_FunctionStructure() throws Exception {
        String jsonSchema = schema.toJsonSchema();
        JSONObject jsonObject = new JSONObject(jsonSchema);
        
        // Parse functions
        JSONArray functions = jsonObject.getJSONArray("functions");
        
        assertTrue("Should have at least 16 functions", functions.length() >= 16);
        
        // Check first function structure (OpenAI format: {type, function:{...}})
        JSONObject firstFunction = functions.getJSONObject(0);
        
        assertTrue("Function should have 'type'", firstFunction.has("type"));
        assertEquals("function", firstFunction.getString("type"));
        
        JSONObject innerFunction = firstFunction.getJSONObject("function");
        assertTrue("Function should have 'name'", innerFunction.has("name"));
        assertTrue("Function should have 'description'", innerFunction.has("description"));
        assertTrue("Function should have 'parameters'", innerFunction.has("parameters"));
    }

    @Test
    public void testJsonSchema_ParameterStructure() throws Exception {
        String jsonSchema = schema.toJsonSchema();
        JSONObject jsonObject = new JSONObject(jsonSchema);
        
        JSONArray functions = jsonObject.getJSONArray("functions");
        
        // Find call_contact function
        JSONObject callContact = null;
        for (int i = 0; i < functions.length(); i++) {
            JSONObject func = functions.getJSONObject(i);
            if ("call_contact".equals(func.getJSONObject("function").getString("name"))) {
                callContact = func;
                break;
            }
        }
        
        assertNotNull("call_contact function should exist", callContact);
        
        JSONObject parameters = callContact.getJSONObject("function").getJSONObject("parameters");
        assertTrue("Parameters should have 'type'", parameters.has("type"));
        assertEquals("object", parameters.getString("type"));
        assertTrue("Parameters should have 'properties'", parameters.has("properties"));
        assertTrue("Parameters should have 'required'", parameters.has("required"));
        
        // contact_name is a required property
        JSONObject properties = parameters.getJSONObject("properties");
        assertTrue("Properties should contain contact_name", properties.has("contact_name"));
    }

    // ========================================================================
    // Schema Registry Tests
    // ========================================================================

    @Test
    public void testGetSchema() {
        assertNotNull("getSchema(call_contact) should return schema", 
                      schema.getSchema("call_contact"));
        assertNull("getSchema(unknown) should return null", 
                   schema.getSchema("unknown_function"));
    }

    @Test
    public void testIsSupported() {
        assertTrue("call_contact should be supported", schema.isSupported("call_contact"));
        assertFalse("unknown_function should not be supported", schema.isSupported("unknown_function"));
    }

    @Test
    public void testGetFunctionNames() {
        assertTrue("Function names should include call_contact", 
                   schema.getFunctionNames().contains("call_contact"));
        assertEquals("Function names should match registry size", 
                     schema.getAllSchemas().size(), schema.getFunctionNames().size());
    }

    // ========================================================================
    // Required Parameter Tests
    // ========================================================================

    @Test
    public void testRequiredParameters_CallContact() {
        FunctionCallSchema.SchemaDefinition callContact = def("call_contact");
        boolean contactRequired = false;
        boolean phoneRequired = false;
        for (FunctionCallSchema.ParameterDefinition param : callContact.getParameters()) {
            if ("contact_name".equals(param.getName())) {
                contactRequired = param.isRequired();
            } else if ("phone_number".equals(param.getName())) {
                phoneRequired = param.isRequired();
            }
        }
        assertTrue("contact_name should be required", contactRequired);
        assertFalse("phone_number should be optional", phoneRequired);
    }

    @Test
    public void testRequiredParameters_SendWhatsapp() {
        FunctionCallSchema.SchemaDefinition sendWhatsapp = def("send_whatsapp");
        for (FunctionCallSchema.ParameterDefinition param : sendWhatsapp.getParameters()) {
            if ("contact_name".equals(param.getName()) || "message".equals(param.getName())) {
                assertTrue("send_whatsapp parameters should be required", param.isRequired());
            }
        }
    }

    @Test
    public void testRequiredParameters_SetAlarm() {
        FunctionCallSchema.SchemaDefinition setAlarm = def("set_alarm");
        for (FunctionCallSchema.ParameterDefinition param : setAlarm.getParameters()) {
            if ("time".equals(param.getName())) {
                assertTrue("time should be required", param.isRequired());
            }
        }
    }

    // ========================================================================
    // FunctionCallResult Tests
    // ========================================================================

    @Test
    public void testFunctionCallResult_Success() {
        FunctionCallSchema.FunctionCallResult result = 
            new FunctionCallSchema.FunctionCallResult("call_contact", 
                java.util.Map.of("contact_name", "ماما"));
        
        assertEquals("call_contact", result.getFunctionName());
        assertEquals("ماما", result.getArguments().get("contact_name"));
        assertEquals("ماما", result.getArgument("contact_name"));
        assertTrue("Result should be valid", result.isValid());
    }

    @Test
    public void testFunctionCallResult_UnknownFunction() {
        FunctionCallSchema.FunctionCallResult result = 
            new FunctionCallSchema.FunctionCallResult("unknown", null, 0.9f, null);
        
        assertFalse("Unknown function should not be valid", result.isValid());
    }

    @Test
    public void testFunctionCallResult_LowConfidence() {
        FunctionCallSchema.FunctionCallResult result = 
            new FunctionCallSchema.FunctionCallResult("call_contact", null, 0.2f, null);
        
        assertFalse("Low confidence result should not be valid", result.isValid());
    }

    @Test
    public void testFunctionCallResult_NullFunctionName() {
        FunctionCallSchema.FunctionCallResult result = 
            new FunctionCallSchema.FunctionCallResult(null, null, 1.0f, null);
        
        assertFalse("Null function name should not be valid", result.isValid());
    }

    // ========================================================================
    // Egyptian Dialect Examples Tests
    // ========================================================================

    @Test
    public void testEgyptianExamples_CallContact() {
        FunctionCallSchema.SchemaDefinition callContact = def("call_contact");
        
        assertNotNull("call_contact should be defined", callContact);
        
        // Should have Egyptian examples
        assertNotNull("Should have examples", callContact.getExamples());
        assertFalse("Examples should not be empty", callContact.getExamples().isEmpty());
        
        // Check for Egyptian dialect examples
        boolean hasEgyptianExample = callContact.getExamples().stream()
            .anyMatch(ex -> ex.contains("اتصل") || ex.contains("كلم") || ex.contains("رن"));
        
        assertTrue("Should have Egyptian dialect examples", hasEgyptianExample);
    }

    @Test
    public void testEgyptianExamples_SendWhatsapp() {
        FunctionCallSchema.SchemaDefinition sendWhatsapp = def("send_whatsapp");
        
        assertNotNull("send_whatsapp should be defined", sendWhatsapp);
        assertNotNull("Should have examples", sendWhatsapp.getExamples());
        
        boolean hasEgyptianExample = sendWhatsapp.getExamples().stream()
            .anyMatch(ex -> ex.contains("ابعت") || ex.contains("واتساب") || ex.contains("رسالة"));
        
        assertTrue("Should have Egyptian dialect examples", hasEgyptianExample);
    }

    @Test
    public void testEgyptianExamples_SetAlarm() {
        FunctionCallSchema.SchemaDefinition setAlarm = def("set_alarm");
        
        assertNotNull("set_alarm should be defined", setAlarm);
        assertNotNull("Should have examples", setAlarm.getExamples());
        
        boolean hasEgyptianExample = setAlarm.getExamples().stream()
            .anyMatch(ex -> ex.contains("نبه") || ex.contains("منبه") || ex.contains("ذكر"));
        
        assertTrue("Should have Egyptian dialect examples", hasEgyptianExample);
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    public void testEdgeCase_NullFunctionName() {
        assertFalse("Null function name should not exist", 
                    schema.getAllSchemas().containsKey(null));
    }

    @Test
    public void testEdgeCase_EmptyFunctionName() {
        assertFalse("Empty function name should not exist", 
                    schema.getAllSchemas().containsKey(""));
    }

    @Test
    public void testEdgeCase_CaseSensitivity() {
        // Function names are normalized to lowercase
        assertNull("CALL_CONTACT (uppercase) should not exist", 
                   schema.getAllSchemas().get("CALL_CONTACT"));
        assertNotNull("call_contact (lowercase) should exist", 
                      schema.getAllSchemas().get("call_contact"));
    }
}
