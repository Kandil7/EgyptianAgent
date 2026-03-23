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
     * Expanded for 97.8% accuracy target.
     */
    private static void initEgyptianToMSA() {
        // === Core Verbs ===
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
        
        // === Additional Egyptian Verbs ===
        EGYPTIAN_TO_MSA.put("خده", "خذه");
        EGYPTIAN_TO_MSA.put("جيب", "أحضر");
        EGYPTIAN_TO_MSA.put("روح", "اذهب");
        EGYPTIAN_TO_MSA.put("استنى", "انتظر");
        EGYPTIAN_TO_MSA.put("فهم", "افهم");
        EGYPTIAN_TO_MSA.put("عرف", "اعرف");
        EGYPTIAN_TO_MSA.put("لاقى", "وجد");
        EGYPTIAN_TO_MSA.put("مشى", "ذهب");
        EGYPTIAN_TO_MSA.put("رجع", "عاد");
        EGYPTIAN_TO_MSA.put("سيب", "اترك");
        EGYPTIAN_TO_MSA.put("خد", "خذ");
        EGYPTIAN_TO_MSA.put("حط", "ضع");
        EGYPTIAN_TO_MSA.put("حطني", "ضعني");
        EGYPTIAN_TO_MSA.put("حطلي", "ضع لي");

        // === Common Expressions ===
        EGYPTIAN_TO_MSA.put("ازيك", "كيف حالك");
        EGYPTIAN_TO_MSA.put("عامل ايه", "كيف حالك");
        EGYPTIAN_TO_MSA.put("عامل إيه", "كيف حالك");
        EGYPTIAN_TO_MSA.put("تمام", "جيد");
        EGYPTIAN_TO_MSA.put("مش قادر", "لا أستطيع");
        EGYPTIAN_TO_MSA.put("مفيش", "لا يوجد");
        EGYPTIAN_TO_MSA.put("فيه", "يوجد");
        EGYPTIAN_TO_MSA.put("دلوقتي", "الآن");
        EGYPTIAN_TO_MSA.put("بعدين", "لاحقاً");
        EGYPTIAN_TO_MSA.put("بكرة", "غداً");
        EGYPTIAN_TO_MSA.put("النهاردة", "اليوم");
        
        // === Egyptian Negation ===
        EGYPTIAN_TO_MSA.put("مش", "ليس");
        EGYPTIAN_TO_MSA.put("مقدرش", "لا أستطيع");
        EGYPTIAN_TO_MSA.put("معاياش", "ليس معي");
        EGYPTIAN_TO_MSA.put("معرش", "لا أعرف");
        EGYPTIAN_TO_MSA.put("مفيش حاجة", "لا شيء");

        // === Time Expressions ===
        EGYPTIAN_TO_MSA.put("الصبح", "الصباح");
        EGYPTIAN_TO_MSA.put("الظهر", "الظهر");
        EGYPTIAN_TO_MSA.put("العصر", "العصر");
        EGYPTIAN_TO_MSA.put("المغرب", "المغرب");
        EGYPTIAN_TO_MSA.put("العشا", "العشاء");
        EGYPTIAN_TO_MSA.put("الليل", "الليل");
        EGYPTIAN_TO_MSA.put("كام ساعة", "بعد كم ساعة");
        EGYPTIAN_TO_MSA.put("بعد شوية", "بعد قليل");
        EGYPTIAN_TO_MSA.put("بدري", "مبكراً");
        EGYPTIAN_TO_MSA.put("متأخر", "متأخراً");
        EGYPTIAN_TO_MSA.put("حالاً", "فوراً");
        EGYPTIAN_TO_MSA.put("بعد نص ساعة", "بعد 30 دقيقة");
        EGYPTIAN_TO_MSA.put("بعد ساعة", "بعد ساعة");
        EGYPTIAN_TO_MSA.put("دلوقتي حالا", "الآن فوراً");

        // === Emergency Expressions ===
        EGYPTIAN_TO_MSA.put("يا نجدة", "نجدة");
        EGYPTIAN_TO_MSA.put("استغاثة", "استغاثة");
        EGYPTIAN_TO_MSA.put("حاجة طارئة", "حالة طارئة");
        EGYPTIAN_TO_MSA.put("مش قادر", "أحتاج مساعدة");
        EGYPTIAN_TO_MSA.put("محتاج مساعدة", "أحتاج مساعدة");
        EGYPTIAN_TO_MSA.put("ساعدني", "ساعدني");
        EGYPTIAN_TO_MSA.put("انقذني", "أنقذني");
        EGYPTIAN_TO_MSA.put("في حد يجي", "أحتاج مساعدة");
        
        // === Question Words ===
        EGYPTIAN_TO_MSA.put("إيه", "ماذا");
        EGYPTIAN_TO_MSA.put("إزاي", "كيف");
        EGYPTIAN_TO_MSA.put("إمتى", "متى");
        EGYPTIAN_TO_MSA.put("فين", "أين");
        EGYPTIAN_TO_MSA.put("ليه", "لماذا");
        EGYPTIAN_TO_MSA.put("كام", "كم");
        EGYPTIAN_TO_MSA.put("مين", "من");
        
        // === Affirmations/Negations ===
        EGYPTIAN_TO_MSA.put("أه", "نعم");
        EGYPTIAN_TO_MSA.put("أيوة", "نعم");
        EGYPTIAN_TO_MSA.put("لا", "لا");
        EGYPTIAN_TO_MSA.put("معلش", "عفواً");
        EGYPTIAN_TO_MSA.put("خلاص", "حسناً");
        EGYPTIAN_TO_MSA.put("زي الفل", "ممتاز");
        EGYPTIAN_TO_MSA.put("بلاش", "لا شكراً");
        
        // === Common Phrases ===
        EGYPTIAN_TO_MSA.put("أخبار إيه", "ما الأخبار");
        EGYPTIAN_TO_MSA.put("إيه الأخبار", "ما الأخبار");
        EGYPTIAN_TO_MSA.put("كله تمام", "كل شيء جيد");
        EGYPTIAN_TO_MSA.put("الحمد لله", "الحمد لله");
    }
    
    /**
     * Initialize contact name mappings.
     * Expanded for 97.8% accuracy target.
     */
    private static void initContactMappings() {
        // === Core Family Terms ===
        CONTACT_MAPPINGS.put("ماما", "أمي");
        CONTACT_MAPPINGS.put("بابا", "أبي");
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
        
        // === Extended Family Terms ===
        CONTACT_MAPPINGS.put("حبيبي", "زوجي");
        CONTACT_MAPPINGS.put("حبيبتي", "زوجتي");
        CONTACT_MAPPINGS.put("ابني", "ولدي");
        CONTACT_MAPPINGS.put("بنتي", "ابنتي");
        CONTACT_MAPPINGS.put("أختي", "أختي");
        CONTACT_MAPPINGS.put("أخويا", "أخي");
        CONTACT_MAPPINGS.put("خويا", "أخي");
        CONTACT_MAPPINGS.put("ختي", "أختي");
        CONTACT_MAPPINGS.put("خالة", "خالتي");
        CONTACT_MAPPINGS.put("عمة", "عمتي");
        
        // === Professional Titles ===
        CONTACT_MAPPINGS.put("ريّس", "الرئيس");
        CONTACT_MAPPINGS.put("أستاذ", "الأستاذ");
        CONTACT_MAPPINGS.put("دكتور", "الدكتور");
        CONTACT_MAPPINGS.put("مهندس", "المهندس");
        CONTACT_MAPPINGS.put("باشا", "السيد");
        CONTACT_MAPPINGS.put("هانم", "السيدة");
        CONTACT_MAPPINGS.put("بيك", "السيد");
        CONTACT_MAPPINGS.put("كابتن", "الكابتن");
        
        // === Endearment Terms ===
        CONTACT_MAPPINGS.put("يا روحي", "حبيبي");
        CONTACT_MAPPINGS.put("يا عمري", "حبيبي");
        CONTACT_MAPPINGS.put("يا قمر", "حبيبي");
        CONTACT_MAPPINGS.put("يا غالي", "حبيبي");
        
        // === Name Aliases ===
        CONTACT_MAPPINGS.put("أحمدو", "أحمد");
        CONTACT_MAPPINGS.put("محمودو", "محمود");
        CONTACT_MAPPINGS.put("سوسو", "سوزان");
        CONTACT_MAPPINGS.put("ميمي", "مريم");
    }
    
    /**
     * Initialize time mappings.
     * Expanded for 97.8% accuracy target.
     */
    private static void initTimeMappings() {
        // === Standard Time Mappings ===
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
        
        // === Expanded Time Mappings ===
        TIME_MAPPINGS.put("الفجر", "05:00");
        TIME_MAPPINGS.put("الشروق", "06:00");
        TIME_MAPPINGS.put("بدري الصبح", "07:00");
        TIME_MAPPINGS.put("قبل الضهر", "11:00");
        TIME_MAPPINGS.put("بعد الضهر", "14:00");
        TIME_MAPPINGS.put("قبل المغرب", "17:00");
        TIME_MAPPINGS.put("بعد المغرب", "19:00");
        TIME_MAPPINGS.put("بعد العشا", "21:00");
        TIME_MAPPINGS.put("آخر الليل", "23:00");
        
        // === Relative Time Expressions ===
        TIME_MAPPINGS.put("بعد نص ساعة", "30 دقيقة");
        TIME_MAPPINGS.put("بعد ساعة", "60 دقيقة");
        TIME_MAPPINGS.put("بعد ساعتين", "120 دقيقة");
        TIME_MAPPINGS.put("بعد تلت ساعة", "20 دقيقة");
        TIME_MAPPINGS.put("بعد ربع ساعة", "15 دقيقة");
        TIME_MAPPINGS.put("بعد شوية", "بعد قليل");
        
        // === Day References ===
        TIME_MAPPINGS.put("النهاردة الصبح", "اليوم الصباح");
        TIME_MAPPINGS.put("النهاردة الضهر", "اليوم الظهر");
        TIME_MAPPINGS.put("بكرة بدري", "غداً مبكراً");
        TIME_MAPPINGS.put("بكرة الصبح", "غداً الصباح");
        TIME_MAPPINGS.put("بكرة الضهر", "غداً الظهر");
        TIME_MAPPINGS.put("بكرة العصر", "غداً العصر");
        TIME_MAPPINGS.put("بكرة المغرب", "غداً المغرب");
        TIME_MAPPINGS.put("بكرة العشا", "غداً العشاء");
        TIME_MAPPINGS.put("بكرة الليل", "غداً الليل");
        
        // === Weekend References ===
        TIME_MAPPINGS.put("يوم الجمعة", "الجمعة");
        TIME_MAPPINGS.put("الجمعة الجاي", "الجمعة القادمة");
        TIME_MAPPINGS.put("يوم الاتنين", "الاثنين");
        TIME_MAPPINGS.put("الاتنين الجاي", "الاثنين القادم");
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
        
        // Use advanced parser for complex expressions
        return parseTimeExpressionAdvanced(normalized);
    }
    
    /**
     * Advanced time expression parser with regex support.
     * Handles numeric times and relative expressions.
     */
    public static String parseTimeExpressionAdvanced(String timeExpr) {
        if (timeExpr == null || timeExpr.isEmpty()) {
            return "";
        }
        
        String normalized = timeExpr.trim();
        
        // Parse numeric time expressions (e.g., "الساعة 7", "3 العصر")
        Pattern numericPattern = Pattern.compile("(\\d+)(?:\\s*(?:الصبح|الضهر|العصر|المغرب|العشا|الليل))?");
        Matcher matcher = numericPattern.matcher(normalized);
        
        if (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            String period = "";
            
            if (normalized.contains("الصبح")) period = " الصباح";
            else if (normalized.contains("الضهر")) period = " الظهر";
            else if (normalized.contains("العصر")) period = " العصر";
            else if (normalized.contains("المغرب")) period = " المغرب";
            else if (normalized.contains("العشا") || normalized.contains("العشاء")) period = " العشاء";
            else if (normalized.contains("الليل")) period = " الليل";
            
            if (!period.isEmpty()) {
                return hour + period;
            }
            return String.valueOf(hour);
        }
        
        // Parse relative time expressions
        if (normalized.contains("بعد")) {
            if (normalized.contains("ساعة")) return "بعد ساعة";
            if (normalized.contains("نص ساعة")) return "بعد 30 دقيقة";
            if (normalized.contains("ربع ساعة")) return "بعد 15 دقيقة";
            if (normalized.contains("دقيقة")) return "بعد دقائق";
            if (normalized.contains("شوية")) return "بعد قليل";
        }
        
        return normalized;
    }
    
    /**
     * Classify basic intent using pattern matching.
     * Enhanced for 97.8% accuracy target.
     */
    public static IntentResult classifyBasicIntent(String text) {
        IntentResult result = new IntentResult();
        result.setOriginalText(text);

        if (text == null || text.isEmpty()) {
            return result;
        }

        String normalized = normalize(text);
        String lowerText = normalized.toLowerCase();

        // === EMERGENCY (Highest Priority) ===
        if (lowerText.contains("نجدة") ||
            lowerText.contains("استغاثة") ||
            lowerText.contains("طوارئ") ||
            lowerText.contains("ساعدني") ||
            lowerText.contains("انقذني") ||
            lowerText.contains("مش قادر") ||
            lowerText.contains("محتاج مساعدة") ||
            lowerText.contains("حاجة طارئة") ||
            lowerText.contains("في حد يجي")) {
            result.setIntentType(IntentType.EMERGENCY);
            result.setConfidence(calculateConfidenceScore(text, IntentType.EMERGENCY, false));
            return result;
        }

        // === CALL CONTACT ===
        Matcher callMatcher = CALL_PATTERN.matcher(normalized);
        if (callMatcher.find()) {
            result.setIntentType(IntentType.CALL_CONTACT);
            String contact = callMatcher.group(2).trim();
            // Remove time modifiers from contact name
            contact = contact.replaceAll("\\s+(دلوقتي|حالاً|بكرة|الصبح|الضهر|العصر|المغرب|العشا).*$", "");
            result.setEntity("contact", normalizeContactName(contact));
            result.setConfidence(calculateConfidenceScore(text, IntentType.CALL_CONTACT, true));
            return result;
        }
        
        // Additional call patterns
        if (lowerText.contains("خده على تليفون") ||
            lowerText.contains("حطني في مكالمة") ||
            lowerText.contains("عايز أتتكلم مع") ||
            lowerText.contains("ممكن تكلم")) {
            result.setIntentType(IntentType.CALL_CONTACT);
            result.setConfidence(0.85f);
            return result;
        }

        // === SEND WHATSAPP ===
        Matcher whatsappMatcher = WHATSAPP_PATTERN.matcher(normalized);
        if (whatsappMatcher.find()) {
            result.setIntentType(IntentType.SEND_WHATSAPP);
            String contact = whatsappMatcher.group(2).trim();
            result.setEntity("contact", normalizeContactName(contact));
            result.setConfidence(calculateConfidenceScore(text, IntentType.SEND_WHATSAPP, true));
            return result;
        }
        
        // Additional WhatsApp patterns
        if (lowerText.contains("قول ل") ||
            lowerText.contains("راسل") ||
            lowerText.contains("اكتب ل")) {
            result.setIntentType(IntentType.SEND_WHATSAPP);
            result.setConfidence(0.8f);
            return result;
        }

        // === SET ALARM ===
        Matcher alarmMatcher = ALARM_PATTERN.matcher(normalized);
        if (alarmMatcher.find()) {
            result.setIntentType(IntentType.SET_ALARM);
            result.setEntity("time", normalizeTimeExpression(alarmMatcher.group(2).trim()));
            result.setConfidence(calculateConfidenceScore(text, IntentType.SET_ALARM, true));
            return result;
        }
        
        // Additional alarm patterns
        if (lowerText.contains("اضبط") ||
            lowerText.contains("حطلي منبه") ||
            lowerText.contains("حطلي تذكير")) {
            result.setIntentType(IntentType.SET_ALARM);
            result.setConfidence(0.8f);
            return result;
        }

        // === READ TIME ===
        if (TIME_PATTERN.matcher(normalized).find() ||
            lowerText.contains("الساعة") ||
            lowerText.contains("الوقت") ||
            lowerText.contains("كام الساعة") ||
            lowerText.contains("وقت إيه")) {
            result.setIntentType(IntentType.READ_TIME);
            result.setConfidence(calculateConfidenceScore(text, IntentType.READ_TIME, false));
            return result;
        }

        // === GREETING ===
        if (lowerText.contains("السلام عليكم") ||
            lowerText.contains("أهلاً") ||
            lowerText.contains("مرحبا") ||
            lowerText.contains("ازيك") ||
            lowerText.contains("عامل ايه") ||
            lowerText.contains("صباح الخير") ||
            lowerText.contains("مساء الخير") ||
            lowerText.contains("ألو")) {
            result.setIntentType(IntentType.GREETING);
            result.setConfidence(0.9f);
            return result;
        }

        // === THANK YOU ===
        if (lowerText.contains("شكرا") ||
            lowerText.contains("متشكر") ||
            lowerText.contains("تسلم") ||
            lowerText.contains("يسلمو")) {
            result.setIntentType(IntentType.THANK_YOU);
            result.setConfidence(0.9f);
            return result;
        }

        // === GOODBYE ===
        if (lowerText.contains("مع السلامة") ||
            lowerText.contains("باي") ||
            lowerText.contains("سلام") ||
            lowerText.contains("في أمان الله") ||
            lowerText.contains("أشوفك بعدين")) {
            result.setIntentType(IntentType.GOODBYE);
            result.setConfidence(0.9f);
            return result;
        }

        // === TOGGLE WIFI ===
        if (lowerText.contains("واي فاي") || 
            lowerText.contains("wifi") ||
            lowerText.contains("شغل الواي فاي") ||
            lowerText.contains("افتح الواي فاي") ||
            lowerText.contains("اقفل الواي فاي")) {
            result.setIntentType(IntentType.TOGGLE_WIFI);
            result.setConfidence(0.85f);
            return result;
        }

        // === TOGGLE BLUETOOTH ===
        if (lowerText.contains("بلوتوث") || 
            lowerText.contains("bluetooth") ||
            lowerText.contains("شغل البلوتوث") ||
            lowerText.contains("افتح البلوتوث") ||
            lowerText.contains("اقفل البلوتوث")) {
            result.setIntentType(IntentType.TOGGLE_BLUETOOTH);
            result.setConfidence(0.85f);
            return result;
        }

        // === OPEN APP ===
        if (lowerText.contains("افتح") || lowerText.contains("شغل")) {
            // Check if it's followed by an app name
            if (lowerText.contains("واتساب") || lowerText.contains("فيسبوك") || 
                lowerText.contains("يوتيوب") || lowerText.contains("whatsapp") ||
                lowerText.contains("facebook") || lowerText.contains("youtube")) {
                result.setIntentType(IntentType.OPEN_APP);
                result.setConfidence(0.85f);
                return result;
            }
        }

        // === UNKNOWN ===
        result.setIntentType(IntentType.UNKNOWN);
        result.setConfidence(0.3f);
        return result;
    }
    
    /**
     * Calculate confidence score based on pattern match strength.
     */
    public static float calculateConfidenceScore(String text, IntentType intent, boolean hasEntity) {
        float baseConfidence = 0.75f;
        
        // Boost for having extracted entity
        if (hasEntity) {
            baseConfidence += 0.12f;
        }
        
        // Boost for exact pattern match with high confidence keywords
        String lowerText = text.toLowerCase();
        
        // High confidence keywords
        String[] highConfidenceKeywords = {"نجدة", "استغاثة", "الساعة كام", "كام الساعة", "السلام عليكم"};
        for (String keyword : highConfidenceKeywords) {
            if (lowerText.contains(keyword)) {
                baseConfidence += 0.13f;
                break;
            }
        }
        
        // Medium confidence keywords
        String[] mediumConfidenceKeywords = {"اتصل", "كلم", "ابعت", "نبهني", "ذكرني", "ازيك"};
        for (String keyword : mediumConfidenceKeywords) {
            if (lowerText.contains(keyword)) {
                baseConfidence += 0.08f;
                break;
            }
        }
        
        // Cap at 0.98
        return Math.min(baseConfidence, 0.98f);
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

    /**
     * Extract contact name from command.
     * Stub implementation for compatibility.
     * @param command The command to extract contact from
     * @return Contact name or null
     */
    public static String extractContactName(String command) {
        // Stub implementation
        return null;
    }
}
