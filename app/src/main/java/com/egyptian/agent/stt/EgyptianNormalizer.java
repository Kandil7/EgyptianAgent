package com.egyptian.agent.stt;

import android.util.Log;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.nlp.IntentType;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Egyptian dialect normalizer
 * Handles normalization of Egyptian Arabic dialect for processing
 */
public class EgyptianNormalizer {
    private static final String TAG = "EgyptianNormalizer";
    
    // Common Egyptian dialect to standard Arabic mappings
    private static final Map<String, String> DIALECT_MAP = new HashMap<>();
    
    // Common Egyptian names and their variations
    private static final Map<String, String> NAME_VARIATIONS = new HashMap<>();
    
    static {
        // Initialize dialect mappings
        initializeDialectMap();
        initializeNameVariations();
    }
    
    /**
     * Initializes the dialect mapping
     */
    private static void initializeDialectMap() {
        // Common Egyptian dialect words to standard Arabic
        DIALECT_MAP.put("اتفضل", "تفضل");
        DIALECT_MAP.put("تعال", "تعال");
        DIALECT_MAP.put("فين", "أين");
        DIALECT_MAP.put("إمتى", "متى");
        DIALECT_MAP.put("أزاى", "كيف");
        DIALECT_MAP.put("إزاى", "كيف");
        DIALECT_MAP.put("ليه", "لماذا");
        DIALECT_MAP.put("ليه", "لماذا");
        DIALECT_MAP.put("إحنا", "نحن");
        DIALECT_MAP.put("إنت", "أنت");
        DIALECT_MAP.put("إنتي", "أنت");
        DIALECT_MAP.put("إيه", "ماذا");
        DIALECT_MAP.put("أية", "أي");
        DIALECT_MAP.put("أيه", "أي");
        DIALECT_MAP.put("أكيد", "بالتأكيد");
        DIALECT_MAP.put("أكيد", "بالطبع");
        DIALECT_MAP.put("ممكن", "يمكن");
        DIALECT_MAP.put("مفيش", "لا يوجد");
        DIALECT_MAP.put("مافيش", "لا يوجد");
        DIALECT_MAP.put("ماعملتش", "لم أعمل");
        DIALECT_MAP.put("مش", "ليس");
        DIALECT_MAP.put("متش", "لا");
        DIALECT_MAP.put("ميه", "لا");
        DIALECT_MAP.put("أكيد", "نعم");
        DIALECT_MAP.put("أكيد", "صحيح");
        DIALECT_MAP.put("أكيد", "بالتأكيد");
        DIALECT_MAP.put("أكيد", "بالطبع");
    }
    
    /**
     * Initializes name variations
     */
    private static void initializeNameVariations() {
        // Common Egyptian name variations
        NAME_VARIATIONS.put("ماما", "أمي");
        NAME_VARIATIONS.put("بابا", "أبي");
        NAME_VARIATIONS.put("مama", "أمي");
        NAME_VARIATIONS.put("baba", "أبي");
        NAME_VARIATIONS.put("ماماها", "أمي");
        NAME_VARIATIONS.put("باباه", "أبي");
        NAME_VARIATIONS.put("الست", "أم");
        NAME_VARIATIONS.put("العم", "أب");
        NAME_VARIATIONS.put("الدكتور", "دكتور");
        NAME_VARIATIONS.put("الدكتورة", "دكتورة");
        NAME_VARIATIONS.put("الاستاذ", "أستاذ");
        NAME_VARIATIONS.put("الاستاذة", "أستاذة");
        NAME_VARIATIONS.put("الدكتوره", "دكتورة");
    }
    
    /**
     * Normalizes Egyptian dialect text to standard form
     * @param text The text to normalize
     * @return The normalized text
     */
    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String normalized = text;
        
        // Apply dialect mappings
        for (Map.Entry<String, String> entry : DIALECT_MAP.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }
        
        // Additional normalization rules
        normalized = applyNormalizationRules(normalized);
        
        return normalized;
    }
    
    /**
     * Applies additional normalization rules
     * @param text The text to normalize
     * @return The normalized text
     */
    private static String applyNormalizationRules(String text) {
        // Remove extra spaces
        String normalized = text.replaceAll("\\s+", " ").trim();
        
        // Standardize common expressions
        normalized = normalized.replaceAll("ب\\s+([\\u0627-\\u064a]+)", "ل$1"); // Replace "ب" with "ل" in some contexts
        normalized = normalized.replaceAll("عنده\\s+([\\u0627-\\u064a]+)", "لديه $1"); // "عنده" to "لديه"
        
        return normalized;
    }
    
    /**
     * Normalizes a contact name to standard form
     * @param contactName The contact name to normalize
     * @return The normalized contact name
     */
    public static String normalizeContactName(String contactName) {
        if (contactName == null || contactName.isEmpty()) {
            return contactName;
        }
        
        String normalized = contactName;
        
        // Apply name variations
        for (Map.Entry<String, String> entry : NAME_VARIATIONS.entrySet()) {
            if (normalized.equalsIgnoreCase(entry.getKey())) {
                normalized = entry.getValue();
                break;
            }
        }
        
        // Additional contact name normalization
        normalized = normalized.trim();
        
        return normalized;
    }
    
    /**
     * Enhances text with Egyptian context
     * @param text The input text
     * @return Enhanced text with Egyptian context
     */
    public static String enhanceWithEgyptianContext(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // Add Egyptian context enhancements
        String enhanced = text;
        
        // Identify Egyptian-specific patterns and enhance them
        if (enhanced.contains("اتصل")) {
            enhanced = enhanced.replaceAll("اتصل\\s+(?:ب|على|مع)?\\s*([\\u0627-\\u064a]+)", "اتصل ب $1");
        }
        
        if (enhanced.contains("كلم")) {
            enhanced = enhanced.replaceAll("كلم\\s+([\\u0627-\\u064a]+)", "اتصل ب $1");
        }
        
        if (enhanced.contains("رن على")) {
            enhanced = enhanced.replaceAll("رن\\s+على\\s+([\\u0627-\\u064a]+)", "اتصل ب $1");
        }
        
        return enhanced;
    }
    
    /**
     * Applies post-processing rules to the intent result
     * @param result The intent result to modify
     */
    public static void applyPostProcessingRules(IntentResult result) {
        // Apply Egyptian-specific post-processing rules
        if (result.getIntentType() == IntentType.CALL_CONTACT || 
            result.getIntentType() == IntentType.SEND_WHATSAPP) {
            
            String contact = result.getEntity("contact");
            if (!contact.isEmpty()) {
                result.setEntity("contact", normalizeContactName(contact));
            }
            
            // Additional post-processing for Egyptian dialect
            String message = result.getEntity("message");
            if (!message.isEmpty()) {
                result.setEntity("message", normalize(message));
            }
        }
    }
    
    /**
     * Classifies a basic intent from Egyptian text
     * @param text The Egyptian text to classify
     * @return An IntentResult with the classified intent
     */
    public static IntentResult classifyBasicIntent(String text) {
        IntentResult result = new IntentResult();
        
        // Basic pattern matching for Egyptian dialect
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("اتصل") || lowerText.contains("كلم") || lowerText.contains("رن على")) {
            result.setIntentType(IntentType.CALL_CONTACT);
            result.setConfidence(0.7f);
        } else if (lowerText.contains("واتساب") || lowerText.contains("رسالة") || lowerText.contains("ابعت")) {
            result.setIntentType(IntentType.SEND_WHATSAPP);
            result.setConfidence(0.7f);
        } else if (lowerText.contains("نبهني") || lowerText.contains("ذكرني") || lowerText.contains("المنبه")) {
            result.setIntentType(IntentType.SET_ALARM);
            result.setConfidence(0.7f);
        } else if (lowerText.contains(" emergencies") != -1 || lowerText.contains("نجدة") != -1 || lowerText.contains("استغاثة") != -1) {
            result.setIntentType(IntentType.EMERGENCY);
            result.setConfidence(0.9f);
        } else {
            result.setIntentType(IntentType.UNKNOWN);
            result.setConfidence(0.3f);
        }
        
        return result;
    }
}