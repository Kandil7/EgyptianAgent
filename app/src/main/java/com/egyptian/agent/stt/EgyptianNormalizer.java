package com.egyptian.agent.stt;

import android.util.Log;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EgyptianNormalizer {

    private static final String TAG = "EgyptianNormalizer";

    // Mapping of Egyptian dialect words to standard Arabic or command keywords
    private static final Map<String, String> EGYPTIAN_TO_STANDARD = new HashMap<>();

    // Time expressions mapping
    private static final Map<String, String> TIME_EXPRESSIONS = new HashMap<>();

    // Contact references mapping
    private static final Map<String, String> CONTACT_REFERENCES = new HashMap<>();

    static {
        // Initialize Egyptian dialect mappings
        initializeEgyptianMappings();

        // Initialize time expressions
        initializeTimeExpressions();

        // Initialize contact references
        initializeContactReferences();
    }

    private static void initializeEgyptianMappings() {
        // Common Egyptian expressions
        EGYPTIAN_TO_STANDARD.put("عايز", "أريد");
        EGYPTIAN_TO_STANDARD.put("عاوز", "أريد");
        EGYPTIAN_TO_STANDARD.put("عايزين", "نريد");
        EGYPTIAN_TO_STANDARD.put("عاوزين", "نريد");
        EGYPTIAN_TO_STANDARD.put("فايتة", "فاتت");
        EGYPTIAN_TO_STANDARD.put("فايتات", "فاتت");
        EGYPTIAN_TO_STANDARD.put("رني", "اتصل");
        EGYPTIAN_TO_STANDARD.put("كلمني", "اتصل");
        EGYPTIAN_TO_STANDARD.put("ابعتلي", "أرسل");
        EGYPTIAN_TO_STANDARD.put("قوللي", "أخبرني");
        EGYPTIAN_TO_STANDARD.put("فين", "أين");
        EGYPTIAN_TO_STANDARD.put("دلوقتي", "الآن");
        EGYPTIAN_TO_STANDARD.put("بكرة", "غداً");
        EGYPTIAN_TO_STANDARD.put("امبارح", "أمس");
        EGYPTIAN_TO_STANDARD.put("النهارده", "اليوم");
        EGYPTIAN_TO_STANDARD.put("بعد كدة", "بعد ذلك");
        EGYPTIAN_TO_STANDARD.put("عايز اعمل", "أريد أن أفعل");
        EGYPTIAN_TO_STANDARD.put("عايز اتصل", "أريد أن أتصل");
        EGYPTIAN_TO_STANDARD.put("عايز ابعت", "أريد أن أرسل");
        EGYPTIAN_TO_STANDARD.put("من فضلك", "رجاءً");
        EGYPTIAN_TO_STANDARD.put("من فضل", "رجاءً");
        EGYPTIAN_TO_STANDARD.put("يا فندم", "سيدي");
        EGYPTIAN_TO_STANDARD.put("يا معلم", "صديقي");

        // Command keywords
        EGYPTIAN_TO_STANDARD.put("رن", "اتصل");
        EGYPTIAN_TO_STANDARD.put("كلم", "اتصل");
        EGYPTIAN_TO_STANDARD.put("اتكلم", "اتصل");
        EGYPTIAN_TO_STANDARD.put("ابعت", "أرسل");
        EGYPTIAN_TO_STANDARD.put("رسالة", "رسالة");
        EGYPTIAN_TO_STANDARD.put("واتساب", "واتساب");
        EGYPTIAN_TO_STANDARD.put("انبهني", "ذكرني");
        EGYPTIAN_TO_STANDARD.put("نبهني", "ذكرني");
        EGYPTIAN_TO_STANDARD.put("فكرني", "ذكرني");
        EGYPTIAN_TO_STANDARD.put("الساعه", "الوقت");
        EGYPTIAN_TO_STANDARD.put("الوقت", "الوقت");
        EGYPTIAN_TO_STANDARD.put("كام", "كم");

        // Emergency keywords
        EGYPTIAN_TO_STANDARD.put("نجدة", "طوارئ");
        EGYPTIAN_TO_STANDARD.put("استغاثة", "طوارئ");
        EGYPTIAN_TO_STANDARD.put("مش قادر", "طوارئ");
        EGYPTIAN_TO_STANDARD.put("حد يجي", "طوارئ");
        EGYPTIAN_TO_STANDARD.put("إسعاف", "إسعاف");
        EGYPTIAN_TO_STANDARD.put("حرقان", "طوارئ");
    }

    private static void initializeTimeExpressions() {
        // Time expressions mapping
        TIME_EXPRESSIONS.put("الصبح", "الصباح");
        TIME_EXPRESSIONS.put("الفجر", "الصباح الباكر");
        TIME_EXPRESSIONS.put("الضهر", "الظهر");
        TIME_EXPRESSIONS.put("العشية", "المساء");
        TIME_EXPRESSIONS.put("الليل", "الليل");
        TIME_EXPRESSIONS.put("بعد ساعة", "بعد ساعة");
        TIME_EXPRESSIONS.put("بعد ساعتين", "بعد ساعتين");
        TIME_EXPRESSIONS.put("بكرة الصبح", "غداً الصباح");
        TIME_EXPRESSIONS.put("بكرة الظهر", "غداً الظهر");
        TIME_EXPRESSIONS.put("بكرة العشا", "غداً المساء");
        TIME_EXPRESSIONS.put("بعد ما", "بعد");
    }

    private static void initializeContactReferences() {
        // Contact references
        CONTACT_REFERENCES.put("ماما", "الأم");
        CONTACT_REFERENCES.put("أمي", "الأم");
        CONTACT_REFERENCES.put("بابا", "الأب");
        CONTACT_REFERENCES.put("أبي", "الأب");
        CONTACT_REFERENCES.put("أختي", "الشقيقة");
        CONTACT_REFERENCES.put("أخوي", "الأخ");
        CONTACT_REFERENCES.put("مراتي", "الزوجة");
        CONTACT_REFERENCES.put("جوزي", "الزوج");
        CONTACT_REFERENCES.put("بنتي", "الابنة");
        CONTACT_REFERENCES.put("ابني", "الابن");
        CONTACT_REFERENCES.put("دكتور", "الطبيب");
        CONTACT_REFERENCES.put("صيدلية", "الصيدلية");
        CONTACT_REFERENCES.put("جار", "الجار");
        CONTACT_REFERENCES.put("عمو", "العم");
        CONTACT_REFERENCES.put("خالو", "الخال");
    }

    /**
     * Normalize Egyptian dialect text to standard form with command keywords
     * @param input Raw speech recognition result
     * @return Normalized text ready for intent detection
     */
    public static String normalize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        String normalized = input.trim().toLowerCase();

        Log.d(TAG, "Original text: " + normalized);

        // Remove diacritics and special characters
        normalized = removeDiacritics(normalized);

        // Normalize Egyptian letters
        normalized = normalizeEgyptianLetters(normalized);

        // Apply Egyptian dialect mappings
        normalized = applyEgyptianMappings(normalized);

        // Extract and normalize time expressions
        normalized = normalizeTimeExpressions(normalized);

        // Handle contact references
        normalized = normalizeContactReferences(normalized);

        // Handle numerical expressions
        normalized = normalizeNumericalExpressions(normalized);

        // Final cleanup
        normalized = normalized.replaceAll("\\s+", " ").trim();

        Log.i(TAG, "Normalized text: " + normalized);
        return normalized;
    }

    private static String removeDiacritics(String text) {
        // Remove Arabic diacritics (tashkeel)
        return text.replaceAll("[\\u064B-\\u065F\\u0670]", "");
    }

    private static String normalizeEgyptianLetters(String text) {
        // Normalize Egyptian letter variations
        return text
            .replaceAll("[إأآا]", "ا")
            .replaceAll("[ةه]", "ه")
            .replaceAll("[ئي]", "ي")
            .replaceAll("[ؤو]", "و")
            .replaceAll("ج", "ج")
            .replaceAll("چ", "ج")
            .replaceAll("چ", "ج")
            .replaceAll("ڤ", "ف")
            .replaceAll("گ", "ج");
    }

    private static String applyEgyptianMappings(String text) {
        String result = text;

        // Apply all Egyptian dialect mappings
        for (Map.Entry<String, String> entry : EGYPTIAN_TO_STANDARD.entrySet()) {
            String pattern = "\\b" + entry.getKey() + "\\b";
            result = result.replaceAll(pattern, entry.getValue());
        }

        return result;
    }

    private static String normalizeTimeExpressions(String text) {
        String result = text;

        // Handle time expressions
        for (Map.Entry<String, String> entry : TIME_EXPRESSIONS.entrySet()) {
            String pattern = "\\b" + entry.getKey() + "\\b";
            result = result.replaceAll(pattern, entry.getValue());
        }

        // Handle "after X hours/minutes" patterns
        result = result.replaceAll("بعد\\s+(\\d+)\\s+ساعات?", "بعد $1 ساعة");
        result = result.replaceAll("بعد\\s+(\\d+)\\s+دقايق?", "بعد $1 دقيقة");
        result = result.replaceAll("الساعة\\s+(\\d+)", "الساعة $1");

        return result;
    }

    private static String normalizeContactReferences(String text) {
        String result = text;

        // Handle contact references
        for (Map.Entry<String, String> entry : CONTACT_REFERENCES.entrySet()) {
            String pattern = "\\b" + entry.getKey() + "\\b";
            if (result.matches(".*" + pattern + ".*")) {
                result = result.replaceAll(pattern, entry.getValue());
            }
        }

        // Handle "call mom/dad" patterns
        result = result.replaceAll("اتصل\\s+ب\\s*ام[يي]", "اتصل بالأم");
        result = result.replaceAll("اتصل\\s+ب\\s*اب[وي]", "اتصل بالأب");
        result = result.replaceAll("كلم\\s+ام[يي]", "اتصل بالأم");
        result = result.replaceAll("كلم\\s+اب[وي]", "اتصل بالأب");

        return result;
    }

    private static String normalizeNumericalExpressions(String text) {
        // Convert Arabic-Indic numerals to Western numerals
        String result = text;

        // Arabic-Indic numerals
        String[] arabicNumerals = {"٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩"};
        String[] westernNumerals = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

        for (int i = 0; i < arabicNumerals.length; i++) {
            result = result.replace(arabicNumerals[i], westernNumerals[i]);
        }

        // Handle "number" patterns
        result = result.replaceAll("رقم\\s+(\\d+)", "الرقم $1");

        return result;
    }

    /**
     * Extract contact name from normalized text
     * @param text Normalized command text
     * @return Extracted contact name or empty string
     */
    public static String extractContactName(String text) {
        // Patterns for extracting contact names
        List<String> patterns = Arrays.asList(
            "اتصل\\s+ب\\s*([\\w\\s]+?)(?:\\s+|$)",
            "كلم\\s+([\\w\\s]+?)(?:\\s+|$)",
            "رن\\s+على\\s+([\\w\\s]+?)(?:\\s+|$)",
            "ابعت\\s+رساله\\s+ل\\s*([\\w\\s]+?)(?:\\s+|$)"
        );

        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find() && matcher.groupCount() >= 1) {
                String contactName = matcher.group(1).trim();
                // Remove common prefixes/suffixes
                contactName = contactName.replaceAll("^(ال|دكتور|دكتوره|استاذ|استاذه)\\s*", "");
                contactName = contactName.replaceAll("\\s+(صاحب|اللي|اللي هو)$", "");
                return contactName;
            }
        }

        return "";
    }

    /**
     * Extract time expression from normalized text
     * @param text Normalized command text
     * @return Extracted time expression or empty string
     */
    public static String extractTimeExpression(String text) {
        // Patterns for extracting time expressions
        List<String> patterns = Arrays.asList(
            "الساعة\\s+(\\d+\\s*(?:و\\s*\\d+\\s*(?:دقيقه|دقايق))?)",
            "بعد\\s+(\\d+\\s*(?:ساعه|ساعات|دقيقه|دقايق))",
            "(بكرة|امبارح|النهارده)\\s+(الصبح|الضهر|العشيه|الليل)",
            "(الصبح|الضهر|العشيه|الليل)"
        );

        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find() && matcher.groupCount() >= 1) {
                return matcher.group(1).trim();
            }
        }

        return "";
    }

    /**
     * Enhances text with Egyptian dialect context before sending to model
     * @param text Input text
     * @return Enhanced text with Egyptian context
     */
    public static String enhanceWithEgyptianContext(String text) {
        // Add Egyptian dialect specific enhancements
        String enhanced = text;

        // Map common Egyptian expressions to more standard forms for the model
        enhanced = enhanced.replaceAll("بكره", "بكرة");
        enhanced = enhanced.replaceAll("عنده", "عندده");
        enhanced = enhanced.replaceAll("فيه", "فيه");
        enhanced = enhanced.replaceAll("ليه", "ليه");
        enhanced = enhanced.replaceAll("إيه", "إيه");

        // Add context markers for Egyptian dialect
        if (text.contains("يا كبير") || text.contains("يا صاحبي")) {
            enhanced = "[EGYPTIAN_DIALECT_CONTEXT] " + enhanced;
        }

        return enhanced;
    }

    /**
     * Applies post-processing rules to the result
     * @param result IntentResult to apply rules to
     */
    public static void applyPostProcessingRules(IntentResult result) {
        // Apply Egyptian-specific post-processing
        String contact = result.getEntity("contact", "");
        if (!contact.isEmpty()) {
            // Normalize Egyptian contact names
            result.setEntity("contact", normalizeContactName(contact));
        }

        // Apply other post-processing rules as needed
    }

    /**
     * Normalizes Egyptian contact names
     * @param contactName Original contact name
     * @return Normalized contact name
     */
    public static String normalizeContactName(String contactName) {
        // Common Egyptian nicknames and their normalized forms
        Map<String, String> nicknameMap = new HashMap<>();
        nicknameMap.put("ماما", "أمّي");
        nicknameMap.put("بابا", "أبي");
        nicknameMap.put("مama", "أمّي");
        nicknameMap.put("baba", "أبي");
        nicknameMap.put("دكتور", "الدكتور");
        nicknameMap.put("دكتورة", "الدكتورة");

        String normalized = contactName.toLowerCase();
        if (nicknameMap.containsKey(normalized)) {
            return nicknameMap.get(normalized);
        }

        // Add "ال" prefix to common titles
        if (normalized.startsWith("دكتور") || normalized.startsWith("استاذ") ||
            normalized.startsWith("مهندس")) {
            return "ال" + contactName;
        }

        return contactName;
    }

    /**
     * Checks if a word is known to the system
     * @param word Word to check
     * @return True if known, false otherwise
     */
    public static boolean isKnownWord(String word) {
        // Check against known Egyptian dialect words
        Set<String> knownWords = new HashSet<>();
        knownWords.add("بكرة");
        knownWords.add("امبارح");
        knownWords.add("النهارده");
        knownWords.add("دلوقتي");
        knownWords.add("فين");
        knownWords.add("ازاي");
        knownWords.add("يا كبير");
        knownWords.add("يا صاحبي");
        knownWords.add("اتصل");
        knownWords.add("كلم");
        knownWords.add("رن");
        knownWords.add("ابعت");
        knownWords.add("واتساب");
        knownWords.add("انبهني");
        knownWords.add("نبهني");
        knownWords.add(" thinker");

        return knownWords.contains(word.toLowerCase());
    }
}