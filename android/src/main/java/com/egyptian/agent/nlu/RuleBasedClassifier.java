package com.egyptian.agent.nlu;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-Based Intent Classifier
 * 
 * Fast, lightweight intent classifier using regex patterns and keyword matching.
 * Used as fallback when ML models are unavailable.
 * 
 * Features:
 * - Egyptian dialect pattern matching
 * - Entity extraction
 * - High speed (<10ms)
 * - No ML dependencies
 */
public class RuleBasedClassifier {
    private static final String TAG = "RuleBasedClassifier";
    
    // Intent patterns with regex
    private static final Map<IntentType, Pattern[]> INTENT_PATTERNS = new HashMap<>();
    
    // Keywords for each intent
    private static final Map<IntentType, String[]> INTENT_KEYWORDS = new HashMap<>();
    
    // Entity extraction patterns
    private static final Pattern CONTACT_PATTERN = Pattern.compile("(?:بـ|لـ|على)?\\s*([\\u0600-\\u06FF]+(?:\\s+[\\u0600-\\u06FF]+)*)");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+:\\d+|\\d+\\s*(?:ساعة|دقيقة|س)|الصبح|الظهر|العصر|المغرب|العشا|الليل|بكرة|النهاردة)");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("(?:إن|ان|بـ)\\s*(.+?)(?:\\s*$)");
    
    static {
        initPatterns();
        initKeywords();
    }
    
    /**
     * Initialize intent patterns.
     */
    private static void initPatterns() {
        // Call patterns
        INTENT_PATTERNS.put(IntentType.CALL_CONTACT, new Pattern[]{
            Pattern.compile("(اتصل|كلم|رن|نادي)\\s+(?:بـ)?(.+?)"),
            Pattern.compile("عايز\\s+اكلم\\s+(.+)"),
            Pattern.compile("ممكن\\s+تكلم\\s+(.+)")
        });
        
        // WhatsApp patterns
        INTENT_PATTERNS.put(IntentType.SEND_WHATSAPP, new Pattern[]{
            Pattern.compile("(ابعت|ارسل)\\s+(?:واتساب|رسالة)\\s+(?:لـ)?(.+?)"),
            Pattern.compile("قول\\s+(?:لـ)?(.+?)\\s+(?:إن|ان)\\s+(.+)"),
            Pattern.compile("عايز\\s+ابعت\\s+واتساب\\s+(.+)")
        });
        
        // Alarm patterns
        INTENT_PATTERNS.put(IntentType.SET_ALARM, new Pattern[]{
            Pattern.compile("(نبهني|ذكرني|انبهني|اضبط منبه)\\s+(.+)"),
            Pattern.compile("عايز\\s+منبه\\s+(.+)"),
            Pattern.compile("اضبطلي\\s+تنبيه\\s+(.+)")
        });
        
        // Time patterns
        INTENT_PATTERNS.put(IntentType.READ_TIME, new Pattern[]{
            Pattern.compile("(الساعة|الوقت|كام الساعة)"),
            Pattern.compile("وقت\\s+إيه\\s+دلوقتي"),
            Pattern.compile("الساعة\\s+كام")
        });
        
        // Emergency patterns
        INTENT_PATTERNS.put(IntentType.EMERGENCY, new Pattern[]{
            Pattern.compile("(يا نجدة|استغاثة|طوارئ)"),
            Pattern.compile("(مش قادر|محتاج مساعدة|حاجة طارئة)"),
            Pattern.compile("انقذني|ساعدني")
        });
        
        // WiFi patterns
        INTENT_PATTERNS.put(IntentType.TOGGLE_WIFI, new Pattern[]{
            Pattern.compile("(شغل|افتح|قفل|اقفل|اطفئ)\\s+(?:الـ)?(?:واي فاي|wifi)"),
            Pattern.compile("الواي فاي\\s+(شغال|واقف)")
        });
        
        // Bluetooth patterns
        INTENT_PATTERNS.put(IntentType.TOGGLE_BLUETOOTH, new Pattern[]{
            Pattern.compile("(شغل|افتح|قفل|اقفل|اطفئ)\\s+(?:الـ)?(?:بلوتوث|bluetooth)"),
            Pattern.compile("البلوتوث\\s+(شغال|واقف)")
        });
        
        // Greeting patterns
        INTENT_PATTERNS.put(IntentType.GREETING, new Pattern[]{
            Pattern.compile("(السلام عليكم|صباح الخير|مساء الخير|ازيك|عامل ايه)"),
            Pattern.compile("اهلا|مرحبا|هلا")
        });
        
        // Thank you patterns
        INTENT_PATTERNS.put(IntentType.THANK_YOU, new Pattern[]{
            Pattern.compile("(شكرا|متشكر|تسلم|تسلم ايدك)"),
            Pattern.compile("جزاك الله خيرا")
        });
        
        // Goodbye patterns
        INTENT_PATTERNS.put(IntentType.GOODBYE, new Pattern[]{
            Pattern.compile("(مع السلامة|باي|بايباي|سلام|في امان الله)"),
            Pattern.compile("يلا باي|أشوفك بعدين")
        });
    }
    
    /**
     * Initialize intent keywords.
     */
    private static void initKeywords() {
        INTENT_KEYWORDS.put(IntentType.CALL_CONTACT, new String[]{
            "اتصل", "كلم", "رن", "نادي", "مكالمة", "تليفون", "هاتف"
        });
        
        INTENT_KEYWORDS.put(IntentType.SEND_WHATSAPP, new String[]{
            "واتساب", "رسالة", "ابعت", "ارسل", "مسنج", "فيسبوك"
        });
        
        INTENT_KEYWORDS.put(IntentType.SET_ALARM, new String[]{
            "نبهني", "ذكرني", "منبه", "تنبيه", "موعد", "ميعاد"
        });
        
        INTENT_KEYWORDS.put(IntentType.READ_TIME, new String[]{
            "الساعة", "الوقت", "كام", "وقت"
        });
        
        INTENT_KEYWORDS.put(IntentType.EMERGENCY, new String[]{
            "نجدة", "استغاثة", "طوارئ", "مساعدة", "انقذ", "خطر"
        });
        
        INTENT_KEYWORDS.put(IntentType.TOGGLE_WIFI, new String[]{
            "واي فاي", "wifi", "إنترنت", "نت", "شبكة"
        });
        
        INTENT_KEYWORDS.put(IntentType.TOGGLE_BLUETOOTH, new String[]{
            "بلوتوث", "bluetooth", "سماعة", "سماعات"
        });
        
        INTENT_KEYWORDS.put(IntentType.GREETING, new String[]{
            "سلام", "صباح", "مساء", "ازيك", "اهلا", "مرحبا"
        });
        
        INTENT_KEYWORDS.put(IntentType.THANK_YOU, new String[]{
            "شكرا", "متشكر", "تسلم", "شكر"
        });
        
        INTENT_KEYWORDS.put(IntentType.GOODBYE, new String[]{
            "سلام", "مع السلامة", "باي", "وداع"
        });
    }
    
    /**
     * Classify intent from text.
     */
    public IntentResult classify(String text) {
        IntentResult result = new IntentResult();
        result.setOriginalText(text);
        
        if (text == null || text.isEmpty()) {
            return result;
        }
        
        String normalizedText = EgyptianNormalizer.normalize(text);
        String lowerText = normalizedText.toLowerCase();
        
        long startTime = System.currentTimeMillis();
        
        // Try pattern matching first (most accurate)
        for (Map.Entry<IntentType, Pattern[]> entry : INTENT_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                Matcher matcher = pattern.matcher(normalizedText);
                if (matcher.find()) {
                    result.setIntentType(entry.getKey());
                    extractEntities(result, normalizedText, matcher);
                    result.setConfidence(0.9f);
                    result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
                    Log.d(TAG, "Pattern match: " + entry.getKey() + " in " + result.getProcessingTimeMs() + "ms");
                    return result;
                }
            }
        }
        
        // Fall back to keyword matching
        IntentType bestMatch = IntentType.UNKNOWN;
        int bestScore = 0;
        
        for (Map.Entry<IntentType, String[]> entry : INTENT_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (lowerText.contains(keyword)) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestMatch = entry.getKey();
            }
        }
        
        if (bestMatch != IntentType.UNKNOWN && bestScore > 0) {
            result.setIntentType(bestMatch);
            result.setConfidence(0.5f + (bestScore * 0.1f));
            extractEntities(result, normalizedText, null);
        }
        
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        Log.d(TAG, "Classification completed in " + result.getProcessingTimeMs() + "ms");
        
        return result;
    }
    
    /**
     * Extract entities from matched text.
     */
    private void extractEntities(IntentResult result, String text, Matcher matcher) {
        IntentType type = result.getIntentType();
        
        switch (type) {
            case CALL_CONTACT:
                extractContact(result, text, matcher);
                break;
            case SEND_WHATSAPP:
                extractContact(result, text, matcher);
                extractMessage(result, text, matcher);
                break;
            case SET_ALARM:
                extractTime(result, text, matcher);
                break;
            default:
                break;
        }
    }
    
    /**
     * Extract contact entity.
     */
    private void extractContact(IntentResult result, String text, Matcher matcher) {
        if (matcher != null && matcher.groupCount() >= 2) {
            String contact = matcher.group(2).trim();
            result.setEntity("contact", EgyptianNormalizer.normalizeContactName(contact));
        } else {
            Matcher contactMatcher = CONTACT_PATTERN.matcher(text);
            if (contactMatcher.find()) {
                result.setEntity("contact", EgyptianNormalizer.normalizeContactName(contactMatcher.group(1).trim()));
            }
        }
    }
    
    /**
     * Extract time entity.
     */
    private void extractTime(IntentResult result, String text, Matcher matcher) {
        if (matcher != null && matcher.groupCount() >= 2) {
            String time = matcher.group(2).trim();
            result.setEntity("time", EgyptianNormalizer.normalizeTimeExpression(time));
        } else {
            Matcher timeMatcher = TIME_PATTERN.matcher(text);
            if (timeMatcher.find()) {
                result.setEntity("time", EgyptianNormalizer.normalizeTimeExpression(timeMatcher.group(1).trim()));
            }
        }
    }
    
    /**
     * Extract message entity.
     */
    private void extractMessage(IntentResult result, String text, Matcher matcher) {
        if (matcher != null && matcher.groupCount() >= 3) {
            String message = matcher.group(3).trim();
            result.setEntity("message", message);
        } else {
            Matcher messageMatcher = MESSAGE_PATTERN.matcher(text);
            if (messageMatcher.find()) {
                result.setEntity("message", messageMatcher.group(1).trim());
            }
        }
    }
    
    /**
     * Check if classifier is ready.
     */
    public boolean isReady() {
        return true; // Rule-based is always ready
    }
}
