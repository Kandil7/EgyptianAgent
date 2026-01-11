package com.egyptian.agent.hybrid;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.egyptian.agent.R;
import com.egyptian.agent.ai.LlamaIntentEngine;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.stt.EgyptianNormalizer;

/**
 * Integration Test Activity
 * Allows testing of the hybrid AI integration
 */
public class IntegrationTestActivity extends Activity {
    private static final String TAG = "IntegrationTest";
    
    private EditText inputEditText;
    private Button testButton;
    private TextView resultTextView;
    private LlamaIntentEngine llamaIntentEngine;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_integration_test);
        
        // Initialize views
        inputEditText = findViewById(R.id.inputEditText);
        testButton = findViewById(R.id.testButton);
        resultTextView = findViewById(R.id.resultTextView);
        
        // Initialize Llama Intent Engine
        llamaIntentEngine = new LlamaIntentEngine(this);
        
        // Set click listener for test button
        testButton.setOnClickListener(v -> {
            String input = inputEditText.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "Please enter a command", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Process the input through Llama Intent Engine
            processInput(input);
        });
    }
    
    private void processInput(String input) {
        // Show loading indicator
        resultTextView.setText("Processing...");
        
        // Run in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                // Simulate the processing pipeline:
                // 1. Whisper ASR -> Llama Intent Classification
                // 2. For testing, we'll simulate the Whisper ASR result
                
                // Simulate Whisper ASR result (in real scenario, this comes from audio)
                String egyptianText = input; // In real scenario, this would be from Whisper
                
                // 2. Llama 3.2 3B Intent Classification
                // Instead of calling the actual Llama model (which may not be available in test environment),
                // we'll simulate the response based on the input
                String simulatedResponse = simulateLlamaResponse(egyptianText);
                
                // Parse the simulated response
                IntentResult result = parseSimulatedResponse(simulatedResponse, egyptianText);
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    String resultText = "Input: " + input + "\n" +
                                      "Intent: " + result.getIntentType() + "\n" +
                                      "Confidence: " + result.getConfidence() + "\n" +
                                      "Entities: " + result.getEntities().toString();
                    resultTextView.setText(resultText);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing input", e);
                runOnUiThread(() -> {
                    resultTextView.setText("Error: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private String simulateLlamaResponse(String text) {
        // Simulate Llama response based on input
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("اتصل") || lowerText.contains("كلم") || lowerText.contains("رن على")) {
            return "{\"intent\":\"CALL_PERSON\", \"entities\":{\"person_name\":\"ماما\"}, \"confidence\":0.95}";
        } else if (lowerText.contains("واتساب") || lowerText.contains("رسالة")) {
            return "{\"intent\":\"SEND_WHATSAPP\", \"entities\":{\"person_name\":\"بابا\", \"message\":\"مرحبا\"}, \"confidence\":0.92}";
        } else if (lowerText.contains("نبهني") || lowerText.contains("ذكرني")) {
            return "{\"intent\":\"SET_ALARM\", \"entities\":{\"time\":\"الساعة 8\"}, \"confidence\":0.89}";
        } else if (lowerText.contains(" emergencies") != -1 || lowerText.contains("njda") != -1 || lowerText.contains("استغاثة") != -1) {
            return "{\"intent\":\"EMERGENCY\", \"entities\":{}, \"confidence\":0.98}";
        } else {
            return "{\"intent\":\"UNKNOWN\", \"entities\":{}, \"confidence\":0.3}";
        }
    }
    
    private IntentResult parseSimulatedResponse(String response, String originalText) {
        IntentResult result = new IntentResult();

        // In a real implementation, we would parse the actual JSON response
        // For simulation, we'll create a result based on our simulated response
        try {
            org.json.JSONObject jsonResponse = new org.json.JSONObject(response);

            String intentStr = jsonResponse.optString("intent", "UNKNOWN");
            org.json.JSONObject entitiesObj = jsonResponse.optJSONObject("entities");
            float confidence = (float) jsonResponse.optDouble("confidence", 0.0);

            // Map string intent to enum
            com.egyptian.agent.nlp.IntentType intentType;
            switch (intentStr) {
                case "CALL_PERSON":
                    intentType = com.egyptian.agent.nlp.IntentType.CALL_CONTACT;
                    break;
                case "SEND_WHATSAPP":
                    intentType = com.egyptian.agent.nlp.IntentType.SEND_WHATSAPP;
                    break;
                case "SET_ALARM":
                    intentType = com.egyptian.agent.nlp.IntentType.SET_ALARM;
                    break;
                case "EMERGENCY":
                    intentType = com.egyptian.agent.nlp.IntentType.EMERGENCY;
                    break;
                default:
                    intentType = com.egyptian.agent.nlp.IntentType.UNKNOWN;
                    break;
            }

            result.setIntentType(intentType);
            result.setConfidence(confidence);

            // Add entities if present
            if (entitiesObj != null) {
                java.util.Iterator<String> keys = entitiesObj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = entitiesObj.optString(key);
                    result.setEntity(key, value);
                }
            }
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Error parsing JSON response", e);

            // Fallback to original approach
            if (response.contains("CALL_PERSON")) {
                result.setIntentType(com.egyptian.agent.nlp.IntentType.CALL_CONTACT);
                result.setEntity("person_name", "ماما");
                result.setConfidence(0.95f);
            } else if (response.contains("SEND_WHATSAPP")) {
                result.setIntentType(com.egyptian.agent.nlp.IntentType.SEND_WHATSAPP);
                result.setEntity("person_name", "بابا");
                result.setEntity("message", "مرحبا");
                result.setConfidence(0.92f);
            } else if (response.contains("SET_ALARM")) {
                result.setIntentType(com.egyptian.agent.nlp.IntentType.SET_ALARM);
                result.setEntity("time", "الساعة 8");
                result.setConfidence(0.89f);
            } else if (response.contains("EMERGENCY")) {
                result.setIntentType(com.egyptian.agent.nlp.IntentType.EMERGENCY);
                result.setConfidence(0.98f);
            } else {
                result.setIntentType(com.egyptian.agent.nlp.IntentType.UNKNOWN);
                result.setConfidence(0.3f);
            }
        }

        return result;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up resources
        if (llamaIntentEngine != null) {
            llamaIntentEngine.destroy();
        }
    }
}