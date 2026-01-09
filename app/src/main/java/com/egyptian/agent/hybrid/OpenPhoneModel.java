package com.egyptian.agent.hybrid;

import android.content.res.AssetManager;
import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper class for the OpenPhone-3B model
 * This implementation integrates with the actual OpenPhone model
 */
public class OpenPhoneModel {
    private static final String TAG = "OpenPhoneModel";

    private final String modelPath;
    private boolean isLoaded = false;
    private ai.openphone.OpenPhone openPhoneInstance;

    public OpenPhoneModel(AssetManager assets, String modelPath) throws Exception {
        this.modelPath = modelPath;

        Log.i(TAG, "Initializing OpenPhone-3B model from: " + modelPath);

        // Initialize the actual OpenPhone model
        initializeModel(assets);
    }

    private void initializeModel(AssetManager assets) throws Exception {
        try {
            // Initialize the OpenPhone model with Egyptian dialect configuration
            this.openPhoneInstance = new ai.openphone.OpenPhone.Builder()
                .setModelPath("asset://" + modelPath)
                .setAssetsManager(assets)
                .setConfig(getModelConfig())
                .build();

            isLoaded = true;
            Log.i(TAG, "OpenPhone-3B model loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize OpenPhone model", e);
            throw e;
        }
    }

    private ai.openphone.OpenPhoneConfig getModelConfig() {
        // Configure the model for Egyptian dialect processing
        return new ai.openphone.OpenPhoneConfig.Builder()
            .setLanguage("ar-eg") // Egyptian Arabic
            .setDialect("egyptian")
            .setMaxTokens(512)
            .setTemperature(0.7f) // Balanced creativity and accuracy
            .setTopK(50)
            .setTopP(0.95f)
            .build();
    }

    /**
     * Analyzes text using the OpenPhone model
     * @param text The input text to analyze
     * @return JSONObject containing the analysis results
     */
    public JSONObject analyze(String text) {
        if (!isLoaded || openPhoneInstance == null) {
            Log.e(TAG, "Model not loaded!");
            return createFallbackResult(text);
        }

        Log.d(TAG, "Analyzing text: " + text);

        try {
            // Prepare the prompt for the OpenPhone model
            String prompt = createAnalysisPrompt(text);

            // Call the actual OpenPhone model
            ai.openphone.OpenPhoneResult result = openPhoneInstance.process(prompt);

            // Convert the result to our expected format
            return convertToStandardResult(result);
        } catch (Exception e) {
            Log.e(TAG, "Error during analysis", e);
            return createFallbackResult(text);
        }
    }

    /**
     * Creates a prompt for the OpenPhone model to analyze the text
     */
    private String createAnalysisPrompt(String text) {
        // Create a structured prompt for the OpenPhone model
        return String.format(
            "You are an Egyptian Arabic language understanding system. Analyze the following Egyptian dialect command and respond in JSON format.\n\n" +
            "Command: \"%s\"\n\n" +
            "Respond with JSON containing: {\"intent\": \"<intent_type>\", \"entities\": {\"<entity_type>\": \"<entity_value>\"}, \"confidence\": <0.0-1.0>}\n\n" +
            "Intent types: CALL_CONTACT, SEND_WHATSAPP, SET_ALARM, READ_TIME, READ_MISSED_CALLS, EMERGENCY, UNKNOWN\n\n" +
            "Example response: {\"intent\": \"CALL_CONTACT\", \"entities\": {\"contact\": \"أمي\"}, \"confidence\": 0.92}",
            text
        );
    }

    /**
     * Converts the OpenPhone result to our standard format
     */
    private JSONObject convertToStandardResult(ai.openphone.OpenPhoneResult result) {
        try {
            // The OpenPhone result should contain the JSON response
            String rawResponse = result.getResponse();

            // Parse the JSON response
            JSONObject parsedResponse = new JSONObject(rawResponse);

            // Validate that it has the expected structure
            if (!parsedResponse.has("intent") || !parsedResponse.has("entities") || !parsedResponse.has("confidence")) {
                Log.w(TAG, "OpenPhone response missing required fields, using fallback");
                return createFallbackResult("validation_error");
            }

            return parsedResponse;
        } catch (Exception e) {
            Log.e(TAG, "Error converting OpenPhone result", e);
            return createFallbackResult("conversion_error");
        }
    }

    /**
     * Determines the intent of the given text
     */
    private String determineIntent(String text) {
        text = text.toLowerCase();
        
        // Check for emergency keywords
        if (text.contains("نجدة") || text.contains("استغاثة") || text.contains("طوارئ") || 
            text.contains("إسعاف") || text.contains("شرطة") || text.contains("مساعدة")) {
            return "EMERGENCY";
        }
        
        // Check for call keywords
        if (text.contains("اتصل") || text.contains("كلم") || text.contains("رن") || 
            text.contains("اتكلم")) {
            return "CALL_CONTACT";
        }
        
        // Check for WhatsApp keywords
        if (text.contains("واتساب") || text.contains("ابعت") || text.contains("رساله") || 
            text.contains("قول ل")) {
            return "SEND_WHATSAPP";
        }
        
        // Check for alarm/time keywords
        if (text.contains("انبهني") || text.contains("نبهني") || text.contains("ذكرني") || 
            text.contains("الساعه") || text.contains("الوقت")) {
            return "SET_ALARM";
        }
        
        // Check for time reading
        if (text.contains("الوقت") || text.contains("الساعه") || text.contains("كام")) {
            return "READ_TIME";
        }
        
        // Default to unknown
        return "UNKNOWN";
    }

    /**
     * Extracts entities from the text
     */
    private JSONObject extractEntities(String text) {
        JSONObject entities = new JSONObject();
        
        try {
            // Extract contact name
            String contactName = extractContactName(text);
            if (!contactName.isEmpty()) {
                entities.put("contact", contactName);
            }
            
            // Extract time expression
            String timeExpr = extractTimeExpression(text);
            if (!timeExpr.isEmpty()) {
                entities.put("time", timeExpr);
            }
            
            // Extract message content
            String message = extractMessage(text);
            if (!message.isEmpty()) {
                entities.put("message", message);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting entities", e);
        }
        
        return entities;
    }

    /**
     * Extracts contact name from text
     */
    private String extractContactName(String text) {
        // This would use more sophisticated NLP in a real implementation
        // For now, we'll use simple pattern matching
        
        // Look for patterns like "اتصل بـ [name]" or "كلم [name]"
        if (text.contains("اتصل ب")) {
            int startIndex = text.indexOf("اتصل ب") + 5;
            if (startIndex < text.length()) {
                String remaining = text.substring(startIndex).trim();
                // Extract first word after the connector
                String[] words = remaining.split("\\s+");
                if (words.length > 0) {
                    return words[0].replaceAll("[^\\p{L}\\s]", ""); // Keep only letters and spaces
                }
            }
        } else if (text.contains("كلم ")) {
            int startIndex = text.indexOf("كلم ") + 4;
            if (startIndex < text.length()) {
                String remaining = text.substring(startIndex).trim();
                String[] words = remaining.split("\\s+");
                if (words.length > 0) {
                    return words[0].replaceAll("[^\\p{L}\\s]", "");
                }
            }
        } else if (text.contains("رن على ")) {
            int startIndex = text.indexOf("رن على ") + 7;
            if (startIndex < text.length()) {
                String remaining = text.substring(startIndex).trim();
                String[] words = remaining.split("\\s+");
                if (words.length > 0) {
                    return words[0].replaceAll("[^\\p{L}\\s]", "");
                }
            }
        }
        
        return "";
    }

    /**
     * Extracts time expression from text
     */
    private String extractTimeExpression(String text) {
        // Simple extraction of time-related phrases
        if (text.contains("الصبح") || text.contains("الصباح")) return "الصباح";
        if (text.contains("الظهر")) return "الظهر";
        if (text.contains("المساء")) return "المساء";
        if (text.contains("الليل")) return "الليل";
        if (text.contains("بكرة") || text.contains("غدا")) return "غدا";
        if (text.contains("بعد شوية") || text.contains("بعد قليل")) return "لاحقا";
        
        // Look for specific times like "الساعة 8"
        if (text.contains("الساعة")) {
            int startIndex = text.indexOf("الساعة") + 6;
            if (startIndex < text.length()) {
                String remaining = text.substring(startIndex).trim();
                String[] words = remaining.split("\\s+");
                if (words.length > 0) {
                    // Extract the time number
                    String timeNum = words[0].replaceAll("[^0-9]", "");
                    if (!timeNum.isEmpty()) {
                        return "الساعة " + timeNum;
                    }
                }
            }
        }
        
        return "";
    }

    /**
     * Extracts message content from text
     */
    private String extractMessage(String text) {
        // Look for patterns like "قول لـ [name] إن [message]"
        if (text.contains("قول ل")) {
            int startIndex = text.indexOf("إن ");
            if (startIndex != -1) {
                startIndex += 2; // Skip "إن "
                if (startIndex < text.length()) {
                    return text.substring(startIndex).trim();
                }
            }
        } else if (text.contains("ابعت ") && text.contains("قال")) {
            int startIndex = text.indexOf("قال");
            if (startIndex != -1) {
                startIndex += 3; // Skip "قال"
                if (startIndex < text.length()) {
                    return text.substring(startIndex).trim();
                }
            }
        }
        
        return "";
    }

    /**
     * Calculates confidence based on text analysis
     */
    private float calculateConfidence(String text, String intent) {
        float baseConfidence = 0.5f; // Base confidence
        
        // Increase confidence if we found a clear intent
        if (!"UNKNOWN".equals(intent)) {
            baseConfidence += 0.3f;
        }
        
        // Increase confidence if text contains known Egyptian dialect patterns
        if (text.contains("يا كبير") || text.contains("يا صاحبي") || 
            text.contains("دلوقتي") || text.contains("بكرة") || 
            text.contains("امبارح") || text.contains("النهارده")) {
            baseConfidence += 0.1f;
        }
        
        // Cap confidence at 0.95
        return Math.min(baseConfidence, 0.95f);
    }

    /**
     * Creates a fallback result when analysis fails
     */
    private JSONObject createFallbackResult(String text) {
        JSONObject result = new JSONObject();

        try {
            result.put("intent", "UNKNOWN");
            result.put("entities", new JSONObject());
            result.put("confidence", 0.1f); // Low confidence for fallback
        } catch (Exception e) {
            Log.e(TAG, "Error creating fallback result", e);
        }

        return result;
    }

    /**
     * Unloads the model and frees resources
     */
    public void unload() {
        Log.i(TAG, "Unloading OpenPhone model");
        if (openPhoneInstance != null) {
            openPhoneInstance.unload();
            openPhoneInstance = null;
        }
        isLoaded = false;
    }
}