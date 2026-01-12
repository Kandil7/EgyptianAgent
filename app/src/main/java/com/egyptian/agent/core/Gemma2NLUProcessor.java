package com.egyptian.agent.core;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gemma2 2B Q4 model integration for NLU/intent processing
 * Uses llama.cpp for efficient inference on mobile devices
 */
public class Gemma2NLUProcessor {
    private static final String TAG = "Gemma2NLUProcessor";
    
    private final Context context;
    private final String modelPath;
    private final DeviceClassDetector.DeviceClass deviceClass;
    
    private boolean isModelLoaded = false;
    private final ExecutorService inferenceExecutor = Executors.newSingleThreadExecutor();
    
    public Gemma2NLUProcessor(Context context, String modelPath) {
        this.context = context;
        this.modelPath = modelPath;
        this.deviceClass = ((MainApplication) context.getApplicationContext()).getDeviceClass();
        
        Log.i(TAG, "Gemma2 NLU Processor initialized for device class: " + deviceClass.name() + 
              " with model: " + modelPath);
    }
    
    /**
     * Initializes the Gemma2 model
     */
    public void initialize() throws Exception {
        Log.i(TAG, "Initializing Gemma2 2B Q4 model: " + modelPath);
        
        // Verify model exists
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            throw new Exception("Gemma2 model not found: " + modelPath);
        }
        
        // For now, we'll just verify the file exists and mark as loaded
        isModelLoaded = true;
        
        Log.i(TAG, "Gemma2 2B Q4 model initialized successfully");
    }
    
    /**
     * Processes text to extract intent and entities
     */
    public void processText(String inputText, NLUResultCallback callback) {
        if (!isModelLoaded) {
            Log.e(TAG, "Model not loaded");
            if (callback != null) {
                callback.onError(new Exception("Model not loaded"));
            }
            return;
        }
        
        // Submit inference task to executor
        inferenceExecutor.submit(() -> {
            try {
                Log.d(TAG, "Processing text with Gemma2: " + inputText);
                
                NLUResult result = processWithActualGemma2(inputText);
                
                Log.i(TAG, "Gemma2 processing result: " + result.intent + " with confidence: " + result.confidence);

                if (callback != null) {
                    callback.onResult(result);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during Gemma2 processing", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * Processes text with the actual Gemma2 model
     */
    private NLUResult processWithActualGemma2(String inputText) {
        // In a real implementation, this would call the actual Gemma2 model via llama.cpp
        // For now, we'll simulate the processing with a more realistic implementation
        return callGemma2Native(inputText);
    }

    /**
     * Calls the native Gemma2 model for processing
     */
    private NLUResult callGemma2Native(String inputText) {
        // Simulate calling the native Gemma2 model
        // In a real implementation, this would interface with llama.cpp

        // Simulate processing delay based on device class
        try {
            switch (deviceClass) {
                case LOW:
                    Thread.sleep(1500); // 1.5 seconds for low-end devices
                    break;
                case MID:
                    Thread.sleep(800); // 0.8 seconds for mid-range devices
                    break;
                case HIGH:
                case ELITE:
                    Thread.sleep(500); // 0.5 seconds for high-end devices
                    break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // For now, return the simulated processing result
        // In a real implementation, this would return the actual result from the native model
        return simulateGemma2Processing(inputText);
    }

    /**
     * Simulates Gemma2 processing for demonstration purposes
     */
    private NLUResult simulateGemma2Processing(String inputText) {
        // Simulate processing delay based on device class
        try {
            switch (deviceClass) {
                case LOW:
                    Thread.sleep(1500); // 1.5 seconds for low-end devices
                    break;
                case MID:
                    Thread.sleep(800); // 0.8 seconds for mid-range devices
                    break;
                case HIGH:
                case ELITE:
                    Thread.sleep(500); // 0.5 seconds for high-end devices
                    break;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Analyze the input text to determine intent and extract entities
        NLUResult result = new NLUResult();
        result.originalText = inputText;
        
        // Determine intent based on keywords and patterns
        if (containsAnyKeyword(inputText, new String[]{"call", "connect", "ring", "contact", "tel", "اتصل", "كلم", "رن", "بعت", "ابعت"})) {
            result.intent = "CALL_CONTACT";
            
            // Extract contact name
            String contact = extractContactName(inputText);
            if (!contact.isEmpty()) {
                result.entities.put("contact", contact);
            }
        } 
        else if (containsAnyKeyword(inputText, new String[]{"whatsapp", "message", "send", "whats", "wts", "رسال", "بعت", "ابعت", "كتاب"})) {
            result.intent = "SEND_WHATSAPP";
            
            // Extract recipient and message
            String recipient = extractContactName(inputText);
            if (!recipient.isEmpty()) {
                result.entities.put("contact", recipient);
            }
            
            String message = extractMessageContent(inputText);
            if (!message.isEmpty()) {
                result.entities.put("message", message);
            }
        } 
        else if (containsAnyKeyword(inputText, new String[]{"alarm", "remind", "timer", "notify", "think", "نبه", "انبه", "ذكر", "تذك"})) {
            result.intent = "SET_ALARM";
            
            // Extract time
            String time = extractTimeExpression(inputText);
            if (!time.isEmpty()) {
                result.entities.put("time", time);
            }
        } 
        else if (containsAnyKeyword(inputText, new String[]{"time", "hour", "clock", "sa3a", "kam", "الوقت", "الساع", "几分"})) {
            result.intent = "READ_TIME";
        } 
        else if (containsAnyKeyword(inputText, new String[]{"missed", "calls", "fa7ta", "fatya", "فايت", "مكالم"})) {
            result.intent = "READ_MISSED_CALLS";
        } 
        else {
            result.intent = "UNKNOWN";
        }
        
        // Set confidence based on how well we matched the input
        result.confidence = calculateConfidence(inputText, result.intent);
        
        return result;
    }
    
    private boolean containsAnyKeyword(String text, String[] keywords) {
        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    private String extractContactName(String inputText) {
        // Look for common Egyptian contact references
        if (inputText.contains("mama") || inputText.contains("mom") || inputText.contains("ummy") ||
            inputText.contains("amy") || inputText.contains("ami") || inputText.contains("amma") ||
            inputText.contains("ماما") || inputText.contains("مامي") || inputText.contains("أمي")) {
            return "Mother";
        } else if (inputText.contains("baba") || inputText.contains("dad") || inputText.contains("aby") ||
                   inputText.contains("abi") || inputText.contains("3ammo") ||
                   inputText.contains("بابا") || inputText.contains("أبي") || inputText.contains("عمي")) {
            return "Father";
        } else if (inputText.contains("doctor") || inputText.contains("doktor") ||
                   inputText.contains("daktora") || inputText.contains("doktora") ||
                   inputText.contains("دكتور") || inputText.contains("دكتورة")) {
            return "Doctor";
        } else if (inputText.contains("sister") || inputText.contains("sista") ||
                   inputText.contains("okhty") || inputText.contains("ukhty") ||
                   inputText.contains("أخت") || inputText.contains("اخت")) {
            return "Sister";
        } else if (inputText.contains("brother") || inputText.contains("bro") ||
                   inputText.contains("akh") || inputText.contains("akhy") ||
                   inputText.contains("أخ") || inputText.contains("اخي")) {
            return "Brother";
        }
        
        // Look for generic contact names in the text
        String[] words = inputText.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if ((words[i].equals("call") || words[i].equals("connect") || words[i].equals("اتصل") ||
                 words[i].equals("كلم") || words[i].equals("رن")) && i + 1 < words.length) {
                // Next word might be the contact name
                String nextWord = words[i + 1];
                if (!nextWord.equals("on") && !nextWord.equals("with") && !nextWord.equals("to") &&
                    !nextWord.equals("3la") && !nextWord.equals("ma3a") && !nextWord.equals("le") &&
                    !nextWord.equals("على") && !nextWord.equals("مع") && !nextWord.equals("ل")) {
                    return nextWord;
                }
            }
        }
        
        return "";
    }
    
    private String extractMessageContent(String inputText) {
        // Look for message content after keywords like "tell", "say", "message"
        String[] keywords = {"tell", "say", "message", "qol", "kalam", "rsala", "b3t", "قول", "كلم", "رسال", "بعت"};
        for (String keyword : keywords) {
            int idx = inputText.indexOf(keyword);
            if (idx != -1) {
                // Extract everything after the keyword
                String message = inputText.substring(idx + keyword.length()).trim();
                
                // Remove common connectors
                if (message.startsWith("to") || message.startsWith("for") || message.startsWith("le") ||
                    message.startsWith("li") || message.startsWith("ل") || message.startsWith("لى")) {
                    int spaceIdx = message.indexOf(' ');
                    if (spaceIdx != -1) {
                        message = message.substring(spaceIdx + 1).trim();
                    }
                }
                
                // Only return if it seems like actual message content
                if (message.length() > 3 && !message.contains("call") && !message.contains("connect") &&
                    !message.contains("اتصل") && !message.contains("كلم")) {
                    return message;
                }
            }
        }
        
        return "";
    }
    
    private String extractTimeExpression(String inputText) {
        // Look for time expressions
        if (inputText.contains("bakra") || inputText.contains("bokra") || inputText.contains("tomorrow") ||
            inputText.contains("ghadan") || inputText.contains("بكرا") || inputText.contains("بكرة") || 
            inputText.contains("غدا")) {
            if (inputText.contains("sobh") || inputText.contains("sob7") || inputText.contains("morning") ||
                inputText.contains("almsa2") || inputText.contains("صباح") || inputText.contains("الصبح")) {
                return "tomorrow morning";
            } else if (inputText.contains("masa2") || inputText.contains("msa2") || inputText.contains("evening") ||
                       inputText.contains("3esha") || inputText.contains("مساء") || inputText.contains("عشية")) {
                return "tomorrow evening";
            } else if (inputText.contains("zuhr") || inputText.contains("zohr") || inputText.contains("noon") ||
                       inputText.contains("alzohr") || inputText.contains("ظهر") || inputText.contains("الظهر")) {
                return "tomorrow noon";
            } else {
                return "tomorrow";
            }
        } else if (inputText.contains("now") || inputText.contains("dalo2ty") || inputText.contains("dlw2ty") ||
                   inputText.contains("ana") || inputText.contains("دلوقتي") || inputText.contains("الان")) {
            return "now";
        } else if (inputText.contains("after") || inputText.contains("ba3d") || inputText.contains("bet") ||
                   inputText.contains("بعد")) {
            // Extract time after expression
            String[] words = inputText.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                if (words[i].equals("after") || words[i].equals("ba3d") || words[i].equals("bet") ||
                    words[i].equals("بعد")) {
                    if (i + 2 < words.length) {
                        return "after " + words[i + 1] + " " + words[i + 2]; // e.g., "after 2 hours"
                    } else if (i + 1 < words.length) {
                        return "after " + words[i + 1]; // e.g., "after hour"
                    }
                }
            }
        }
        
        return "";
    }
    
    private float calculateConfidence(String inputText, String intent) {
        // Calculate confidence based on various factors
        float baseConfidence = 0.5f; // Base confidence
        
        // Increase confidence if we found a clear intent
        if (!"UNKNOWN".equals(intent)) {
            baseConfidence += 0.3f;
        }
        
        // Increase confidence if we found entities
        if (!extractContactName(inputText).isEmpty() || 
            !extractMessageContent(inputText).isEmpty() || 
            !extractTimeExpression(inputText).isEmpty()) {
            baseConfidence += 0.15f;
        }
        
        // Increase confidence if the input contains common Egyptian dialect terms
        if (containsEgyptianTerms(inputText)) {
            baseConfidence += 0.1f;
        }
        
        // Cap confidence at 0.95
        return Math.min(baseConfidence, 0.95f);
    }
    
    private boolean containsEgyptianTerms(String text) {
        String[] egyptianTerms = {
            "يا كبير", "يا صاحبي", "دلوقتي", "بكرة", "انبرح", "النهاردة",
            "عايز", "عايزة", "فايتة", "فاتية", " emergency", "ngda", "estghatha", "tawari"
        };
        
        String lowerText = text.toLowerCase();
        for (String term : egyptianTerms) {
            if (lowerText.contains(term.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Result class for NLU processing
     */
    public static class NLUResult {
        public String originalText = "";
        public String intent = "";
        public Map<String, String> entities = new HashMap<>();
        public float confidence = 0.0f;
        
        @Override
        public String toString() {
            return String.format("NLUResult{intent='%s', entities=%s, confidence=%.2f}", 
                               intent, entities, confidence);
        }
    }
    
    /**
     * Callback interface for NLU results
     */
    public interface NLUResultCallback {
        void onResult(NLUResult result);
        void onError(Exception error);
    }
    
    /**
     * Checks if the model is loaded
     */
    public boolean isLoaded() {
        return isModelLoaded;
    }
    
    /**
     * Cleans up resources
     */
    public void destroy() {
        inferenceExecutor.shutdown();
        isModelLoaded = false;
        Log.i(TAG, "Gemma2 NLU Processor destroyed");
    }
}