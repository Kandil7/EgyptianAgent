package com.egyptian.agent.nlu;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Egyptian Dialect Normalizer
 * 
 * Normalizes Egyptian Arabic dialect to Modern Standard Arabic (MSA)
 * for better intent classification.
 * 
 * Features:
 * - Egyptian to MSA conversion
 * - Contact name normalization
 * - Time expression normalization
 * - Entity extraction
 */
public class EgyptianNormalizer {
    private static final String TAG = "EgyptianNormalizer";
    
    // Egyptian dialect to MSA mappings
    private static final Map<String, String> EGYPTIAN_TO_MSA = new HashMap<>();
    
    // Contact name mappings
    private static final Map<String, String> CONTACT_MAPPINGS = new HashMap<>();
    
    // Time expression mappings
    private static final Map<String, String> TIME_MAPPINGS = new HashMap<>();
    
    // Regex patterns for entity extraction
    private static final Pattern CALL_PATTERN = Pattern.compile("(اتصل|كلم|رن|نادي)\\s+(?:بـ)?(.+?)(?:\\s|$)");
    private static final Pattern WHATSAPP_PATTERN = Pattern.compile("(ابعت|ارسل|قول)\\s+(?:واتساب|رسالة)\\s+(?:لـ)?(.+?)(?:\\s|$)");
    private static final Pattern ALARM_PATTERN = Pattern.compile("(نبهني|ذكرني|انبهني|اضبط منبه)\\s+(.+?)(?:\\s|$)");
    private static final Pattern TIME_PATTERN = Pattern.compile("(الساعة|الوقت|كام الساعة)");
    
    static {
        // Initialize Egyptian to MSA mappings
        initEgyptianToMSA();
        initContactMappings();
        initTimeMappings();
    }
    
    /**
     * Initialize Egyptian to MSA mappings.
     */
    private static void initEgyptianToMSA() {
        // Verbs
        EGYPTIAN_TO_MSA.put("اتصل", "اتصل");
        EGYPTIAN_TO_MSA.put("كلم", "اتصل");
        EGYPTIAN_TO_MSA.put("رن على", "اتصل بـ");
        EGYPTIAN_TO_MSA.put("نادي", "اتصل بـ");
        EGYPTIAN_TO_MSA.put("ابعت", "أرسل");
        EGYPTIAN_TO_MSA.put("قول", "قل");
        EGYPTIAN_TO_MSA.put("هات", "أحضر");
        EGYPTIAN_TO_MSA.put("اعمل", "افعل");
        EGYPTIAN_TO_MSA.put("افتح", "افتح");
        EGYPTIAN_TO_MSA.put("اقفل", "أغلق");
        EGYPTIAN_TO_MSA.put("شغل", "شغّل");
        
        // Common expressions
        EGYPTIAN_TO_MSA.put("ازيك", "كيف حالك");
        EGYPTIAN_TO_MSA.put("عامل ايه", "كيف حالك");
        EGYPTIAN_TO_MSA.put("تمام", "جيد");
        EGYPTIAN_TO_MSA.put("مش قادر", "لا أستطيع");
        EGYPTIAN_TO_MSA.put("مفيش", "لا يوجد");
        EGYPTIAN_TO_MSA.put("فيه", "يوجد");
        EGYPTIAN_TO_MSA.put("دلوقتي", "الآن");
        EGYPTIAN_TO_MSA.put("بعدين", "لاحقاً");
        EGYPTIAN_TO_MSA.put("بكرة", "غداً");
        EGYPTIAN_TO_MSA.put("النهاردة", "اليوم");
        
        // Time expressions
        EGYPTIAN_TO_MSA.put("الصبح", "الصباح");
        EGYPTIAN_TO_MSA.put("الظهر", "الظهر");
        EGYPTIAN_TO_MSA.put("العصر", "العصر");
        EGYPTIAN_TO_MSA.put("المغرب", "المغرب");
        EGYPTIAN_TO_MSA.put("العشا", "العشاء");
        EGYPTIAN_TO_MSA.put("الليل", "الليل");
        EGYPTIAN_TO_MSA.put("كام ساعة", "بعد كم ساعة");
        EGYPTIAN_TO_MSA.put("بعد شوية", "بعد قليل");
        
        // Emergency expressions
        EGYPTIAN_TO_MSA.put("يا نجدة", "نجدة");
        EGYPTIAN_TO_MSA.put("استغاثة", "استغاثة");
        EGYPTIAN_TO_MSA.put("حاجة طارئة", "حالة طارئة");
        EGYPTIAN_TO_MSA.put("مش قادر", "أحتاج مساعدة");
    }
    
    /**
     * Initialize contact name mappings.
     */
    private static void initContactMappings() {
        // Common Egyptian contact nicknames
        CONTACT_MAPPINGS.put("ماما", "أمي");
        CONTACT_MAPPINGS.put("ماما", "الأم");
        CONTACT_MAPPINGS.put("بابا", "أبي");
        CONTACT_MAPPINGS.put("بابا", "الأب");
        CONTACT_MAPPINGS.put("يما", "أمي");
        CONTACT_MAPPINGS.put("يبا", "أبي");
        CONTACT_MAPPINGS.put("ست الحبايب", "أمي");
        CONTACT_MAPPINGS.put("الحاج", "الأب");
        CONTACT_MAPPINGS.put("عمو", "عمي");
        CONTACT_MAPPINGS.put("خالو", "خالي");
        CONTACT_MAPPINGS.put("تيتا", "جدتي");
        CONTACT_MAPPINGS.put("تيتو", "جدي");
        CONTACT_MAPPINGS.put("نينا", "جدتي");
        CONTACT_MAPPINGS.put("سيد", "السيد");
        CONTACT_MAPPINGS.put("مدام", "السيدة");
    }
    
    /**
     * Initialize time mappings.
     */
    private static void initTimeMappings() {
        TIME_MAPPINGS.put("الصبح", "08:00");
        TIME_MAPPINGS.put("الصبح بدري", "07:00");
        TIME_MAPPINGS.put("الظهر", "12:00");
        TIME_MAPPINGS.put("بعد الظهر", "14:00");
        TIME_MAPPINGS.put("العصر", "16:00");
        TIME_MAPPINGS.put("المغرب", "18:00");
        TIME_MAPPINGS.put("العشا", "20:00");
        TIME_MAPPINGS.put("الليل", "21:00");
        TIME_MAPPINGS.put("بالليل", "21:00");
        TIME_MAPPINGS.put("نص الليل", "00:00");
        TIME_MAPPINGS.put("نص النهار", "12:00");
    }
    
    /**
     * Normalize Egyptian dialect text to MSA.
     */
    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        String normalized = text.trim();
        
        // Apply word-by-word normalization
        String[] words = normalized.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            String normalizedWord = EGYPTIAN_TO_MSA.get(word);
            result.append(normalizedWord != null ? normalizedWord : word).append(" ");
        }
        
        return result.toString().trim();
    }
    
    /**
     * Normalize contact name.
     */
    public static String normalizeContactName(String contactName) {
        if (contactName == null || contactName.isEmpty()) {
            return "";
        }
        
        String normalized = contactName.trim();
        
        // Check mappings
        String mapped = CONTACT_MAPPINGS.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        
        // Check partial matches
        for (Map.Entry<String, String> entry : CONTACT_MAPPINGS.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return normalized.replace(entry.getKey(), entry.getValue());
            }
        }
        
        return normalized;
    }
    
    /**
     * Normalize time expression.
     */
    public static String normalizeTimeExpression(String timeExpr) {
        if (timeExpr == null || timeExpr.isEmpty()) {
            return "";
        }
        
        String normalized = timeExpr.trim();
        
        // Check mappings
        String mapped = TIME_MAPPINGS.get(normalized);
        if (mapped != null) {
            return mapped;
        }
        
        return normalized;
    }
    
    /**
     * Classify basic intent using pattern matching.
     */
    public static IntentResult classifyBasicIntent(String text) {
        IntentResult result = new IntentResult();
        result.setOriginalText(text);
        
        if (text == null || text.isEmpty()) {
            return result;
        }
        
        String normalized = normalize(text);
        String lowerText = normalized.toLowerCase();
        
        // Check call pattern
        Matcher callMatcher = CALL_PATTERN.matcher(normalized);
        if (callMatcher.find()) {
            result.setIntentType(IntentType.CALL_CONTACT);
            result.setEntity("contact", normalizeContactName(callMatcher.group(2).trim()));
            result.setConfidence(0.9f);
            return result;
        }
        
        // Check WhatsApp pattern
        Matcher whatsappMatcher = WHATSAPP_PATTERN.matcher(normalized);
        if (whatsappMatcher.find()) {
            result.setIntentType(IntentType.SEND_WHATSAPP);
            result.setEntity("contact", normalizeContactName(whatsappMatcher.group(2).trim()));
            result.setConfidence(0.85f);
            return result;
        }
        
        // Check alarm pattern
        Matcher alarmMatcher = ALARM_PATTERN.matcher(normalized);
        if (alarmMatcher.find()) {
            result.setIntentType(IntentType.SET_ALARM);
            result.setEntity("time", normalizeTimeExpression(alarmMatcher.group(2).trim()));
            result.setConfidence(0.85f);
            return result;
        }
        
        // Check time query
        if (TIME_PATTERN.matcher(normalized).find() || 
            lowerText.contains("الساعة") || 
            lowerText.contains("الوقت") ||
            lowerText.contains("كام الساعة")) {
            result.setIntentType(IntentType.READ_TIME);
            result.setConfidence(0.95f);
            return result;
        }
        
        // Check emergency
        if (lowerText.contains("نجدة") || 
            lowerText.contains("استغاثة") || 
            lowerText.contains("طارئة") ||
            lowerText.contains("مش قادر")) {
            result.setIntentType(IntentType.EMERGENCY);
            result.setConfidence(0.95f);
            return result;
        }
        
        // Check WiFi toggle
        if (lowerText.contains("واي فاي") || lowerText.contains("wifi")) {
            result.setIntentType(IntentType.TOGGLE_WIFI);
            result.setConfidence(0.8f);
            return result;
        }
        
        // Check Bluetooth toggle
        if (lowerText.contains("بلوتوث") || lowerText.contains("bluetooth")) {
            result.setIntentType(IntentType.TOGGLE_BLUETOOTH);
            result.setConfidence(0.8f);
            return result;
        }
        
        return result;
    }
    
    /**
     * Apply post-processing rules to intent result.
     */
    public static void applyPostProcessingRules(IntentResult result) {
        if (result == null) return;
        
        // Normalize contact names
        if (result.getEntity("contact") != null) {
            String contact = result.getEntity("contact");
            result.setEntity("contact", normalizeContactName(contact));
        }
        
        // Normalize time expressions
        if (result.getEntity("time") != null) {
            String time = result.getEntity("time");
            result.setEntity("time", normalizeTimeExpression(time));
        }
    }
}
