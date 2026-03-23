package com.egyptian.agent.nlu;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TFLite-based Intent Classifier
 * 
 * Machine learning classifier using TensorFlow Lite for Egyptian Arabic
 * intent classification. Provides fallback when rule-based classification
 * has low confidence.
 * 
 * Features:
 * - On-device inference (no network required)
 * - Supports 10+ intent types
 * - Returns confidence scores
 * - Memory-optimized for mobile
 * 
 * Supported Intents:
 * - CALL_CONTACT
 * - SEND_WHATSAPP
 * - SET_ALARM
 * - TOGGLE_WIFI
 * - TOGGLE_BLUETOOTH
 * - OPEN_APP
 * - SEND_SMS
 * - READ_TIME
 * - GREETING
 * - EMERGENCY
 * - CONVERSATION
 * - UNKNOWN
 */
public class TFLiteIntentClassifier {
    
    private static final String TAG = "TFLiteIntentClassifier";
    private static final String MODEL_FILE = "egyptian_intent_classifier.tflite";
    
    // Intent labels matching the model output
    private static final String[] INTENT_LABELS = {
        "CALL_CONTACT",
        "SEND_WHATSAPP", 
        "SET_ALARM",
        "TOGGLE_WIFI",
        "TOGGLE_BLUETOOTH",
        "OPEN_APP",
        "SEND_SMS",
        "READ_TIME",
        "GREETING",
        "EMERGENCY",
        "CONVERSATION",
        "THANK_YOU",
        "GOODBYE",
        "UNKNOWN"
    };
    
    private final Interpreter interpreter;
    private final Context context;
    private final boolean isLoaded;
    
    // Tokenizer vocabulary (simplified - would be loaded from assets in production)
    private final Map<String, Integer> vocabulary;
    
    /**
     * Creates a new TFLiteIntentClassifier
     * @param context Application context
     */
    public TFLiteIntentClassifier(Context context) {
        this.context = context.getApplicationContext();
        this.vocabulary = buildVocabulary();
        
        Interpreter tempInterpreter = null;
        boolean loaded = false;
        
        try {
            MappedByteBuffer modelBuffer = loadModelFile(context, MODEL_FILE);
            if (modelBuffer != null) {
                Interpreter.Options options = new Interpreter.Options();
                options.setNumThreads(4);
                options.setUseNNAPI(true);  // Use Android NN API for acceleration
                
                tempInterpreter = new Interpreter(modelBuffer, options);
                loaded = true;
                Log.i(TAG, "TFLite model loaded successfully");
            }
        } catch (IOException e) {
            Log.w(TAG, "TFLite model not found, using fallback: " + e.getMessage());
        }
        
        this.interpreter = tempInterpreter;
        this.isLoaded = loaded;
    }
    
    /**
     * Loads the TFLite model from assets
     */
    private MappedByteBuffer loadModelFile(Context context, String modelFile) throws IOException {
        AssetFileDescriptor fileDescriptor = null;
        try {
            fileDescriptor = context.getAssets().openFd(modelFile);
            return FileUtil.loadMappedFile(context, fileDescriptor);
        } catch (IOException e) {
            Log.e(TAG, "Error loading model file: " + modelFile, e);
            return null;
        } finally {
            if (fileDescriptor != null) {
                try {
                    fileDescriptor.close();
                } catch (IOException e) {
                    Log.w(TAG, "Error closing file descriptor", e);
                }
            }
        }
    }
    
    /**
     * Builds vocabulary for tokenization
     * In production, this would be loaded from a vocabulary file
     */
    private Map<String, Integer> buildVocabulary() {
        Map<String, Integer> vocab = new HashMap<>();
        
        // Common Arabic words for intents
        String[] callWords = {"اتصل", "كلم", "رن", "تصل", "لمن", "اتصل ب", "كلم"};
        String[] whatsappWords = {"واتساب", "رسالة", "ابعت", "ارسال", "قول"};
        String[] alarmWords = {"منبه", "نبه", "ذكر", "تنبيه", "انبه", "المنبه"};
        String[] wifiWords = {"واي", "wifi", "شغال", "قفل"};
        String[] bluetoothWords = {"بلوتوث", "بلوت"};
        String[] appWords = {"افتح", "شغل", "فتح"};
        String[] smsWords = {"رسالة", "sms", "بعث"};
        String[] timeWords = {"الساعة", "الوقت", "كام"};
        String[] greetingWords = {"اهلا", "مرحبا", "صباح", "مساء"};
        String[] emergencyWords = {"نجدة", "استغاثة", "طوارئ", "ساعد"};
        
        int index = 1;  // Start from 1 (0 is reserved for padding)
        for (String word : callWords) vocab.put(word, index++);
        for (String word : whatsappWords) vocab.put(word, index++);
        for (String word : alarmWords) vocab.put(word, index++);
        for (String word : wifiWords) vocab.put(word, index++);
        for (String word : bluetoothWords) vocab.put(word, index++);
        for (String word : appWords) vocab.put(word, index++);
        for (String word : smsWords) vocab.put(word, index++);
        for (String word : timeWords) vocab.put(word, index++);
        for (String word : greetingWords) vocab.put(word, index++);
        for (String word : emergencyWords) vocab.put(word, index++);
        
        return vocab;
    }
    
    /**
     * Classifies the intent of the given text
     * @param text Input text in Egyptian Arabic
     * @return ClassificationResult with intent and confidence
     */
    public ClassificationResult classify(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ClassificationResult(IntentType.UNKNOWN, 0.0f);
        }
        
        // If model not loaded, use fallback heuristic
        if (!isLoaded || interpreter == null) {
            return classifyWithHeuristics(text);
        }
        
        try {
            // Tokenize input
            int[] inputTokens = tokenize(text);
            
            // Create input tensor (assuming model expects [1, seq_length])
            float[][][] input = new float[1][1][inputTokens.length];
            input[0][0] = convertTokensToFloats(inputTokens);
            
            // Create output tensor
            float[][] output = new float[1][INTENT_LABELS.length];
            
            // Run inference
            interpreter.run(input, output);
            
            // Get prediction
            return parseOutput(output[0]);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during classification", e);
            return classifyWithHeuristics(text);
        }
    }
    
    /**
     * Fallback heuristic-based classification when model unavailable
     */
    private ClassificationResult classifyWithHeuristics(String text) {
        String lowerText = text.toLowerCase();
        
        // Emergency keywords (highest priority)
        if (containsAny(lowerText, "نجدة", "استغاثة", "طوارئ", "مش قادر", "انقذ")) {
            return new ClassificationResult(IntentType.EMERGENCY, 0.95f);
        }
        
        // Call keywords
        if (containsAny(lowerText, "اتصل", "كلم", "رن", "تصل")) {
            return new ClassificationResult(IntentType.CALL_CONTACT, 0.85f);
        }
        
        // WhatsApp keywords
        if (containsAny(lowerText, "واتساب", "ابعت", "رسالة")) {
            return new ClassificationResult(IntentType.SEND_WHATSAPP, 0.85f);
        }
        
        // Alarm keywords
        if (containsAny(lowerText, "منبه", "نبه", "ذكر", "تنبيه")) {
            return new ClassificationResult(IntentType.SET_ALARM, 0.85f);
        }
        
        // WiFi keywords
        if (containsAny(lowerText, "واي", "wifi")) {
            return new ClassificationResult(IntentType.TOGGLE_WIFI, 0.85f);
        }
        
        // Bluetooth keywords
        if (containsAny(lowerText, "بلوتوث", "بلوت")) {
            return new ClassificationResult(IntentType.TOGGLE_BLUETOOTH, 0.85f);
        }
        
        // Time keywords
        if (containsAny(lowerText, "الساعة", "الوقت", "كام")) {
            return new ClassificationResult(IntentType.READ_TIME, 0.85f);
        }
        
        // Greeting keywords
        if (containsAny(lowerText, "اهلا", "مرحبا", "صباح", "مساء", "ازيك", "عامل")) {
            return new ClassificationResult(IntentType.GREETING, 0.85f);
        }
        
        // App opening
        if (containsAny(lowerText, "افتح", "شغل", "فتح")) {
            return new ClassificationResult(IntentType.OPEN_APP, 0.75f);
        }
        
        // Thank you
        if (containsAny(lowerText, "شكرا", "متشكر", "تسلم")) {
            return new ClassificationResult(IntentType.THANK_YOU, 0.85f);
        }
        
        return new ClassificationResult(IntentType.UNKNOWN, 0.0f);
    }
    
    /**
     * Tokenizes input text
     */
    private int[] tokenize(String text) {
        // Simple character-based tokenization for Arabic
        // In production, would use proper Arabic tokenizer
        List<Integer> tokens = new ArrayList<>();
        
        for (char c : text.toCharArray()) {
            String charStr = String.valueOf(c);
            if (vocabulary.containsKey(charStr)) {
                tokens.add(vocabulary.get(charStr));
            } else {
                tokens.add(0);  // Unknown token
            }
        }
        
        // Pad or truncate to fixed length
        int maxLength = 50;
        int[] result = new int[maxLength];
        for (int i = 0; i < maxLength; i++) {
            if (i < tokens.size()) {
                result[i] = tokens.get(i);
            } else {
                result[i] = 0;  // Padding
            }
        }
        
        return result;
    }
    
    /**
     * Converts token array to float array for model input
     */
    private float[] convertTokensToFloats(int[] tokens) {
        float[] result = new float[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            result[i] = (float) tokens[i];
        }
        return result;
    }
    
    /**
     * Parses model output to get intent and confidence
     */
    private ClassificationResult parseOutput(float[] output) {
        int maxIndex = 0;
        float maxValue = Float.MIN_VALUE;
        
        for (int i = 0; i < output.length; i++) {
            if (output[i] > maxValue) {
                maxValue = output[i];
                maxIndex = i;
            }
        }
        
        // Apply softmax-like normalization
        float sum = 0;
        for (float v : output) {
            sum += Math.exp(v);
        }
        float confidence = (float) Math.exp(maxValue) / sum;
        
        if (maxIndex >= INTENT_LABELS.length) {
            return new ClassificationResult(IntentType.UNKNOWN, confidence);
        }
        
        IntentType intent = mapLabelToIntent(INTENT_LABELS[maxIndex]);
        return new ClassificationResult(intent, confidence);
    }
    
    /**
     * Maps model label to IntentType enum
     */
    private IntentType mapLabelToIntent(String label) {
        switch (label) {
            case "CALL_CONTACT": return IntentType.CALL_CONTACT;
            case "SEND_WHATSAPP": return IntentType.SEND_WHATSAPP;
            case "SET_ALARM": return IntentType.SET_ALARM;
            case "TOGGLE_WIFI": return IntentType.TOGGLE_WIFI;
            case "TOGGLE_BLUETOOTH": return IntentType.TOGGLE_BLUETOOTH;
            case "OPEN_APP": return IntentType.OPEN_APP;
            case "SEND_SMS": return IntentType.SEND_SMS;
            case "READ_TIME": return IntentType.READ_TIME;
            case "GREETING": return IntentType.GREETING;
            case "EMERGENCY": return IntentType.EMERGENCY;
            case "CONVERSATION": return IntentType.CONVERSATION;
            case "THANK_YOU": return IntentType.THANK_YOU;
            case "GOODBYE": return IntentType.GOODBYE;
            default: return IntentType.UNKNOWN;
        }
    }
    
    /**
     * Helper to check if text contains any of the keywords
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if classifier is loaded
     */
    public boolean isLoaded() {
        return isLoaded;
    }
    
    /**
     * Closes the interpreter and releases resources
     */
    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
    
    /**
     * Classification result class
     */
    public static class ClassificationResult {
        public final IntentType intent;
        public final float confidence;
        
        public ClassificationResult(IntentType intent, float confidence) {
            this.intent = intent;
            this.confidence = confidence;
        }
        
        @Override
        public String toString() {
            return "ClassificationResult{" +
                    "intent=" + intent +
                    ", confidence=" + confidence +
                    '}';
        }
    }
}
