package com.egyptian.agent.stt;

import android.util.Log;
import org.vosk.Recognizer;

import java.util.HashMap;
import java.util.Map;

public class VocabularyManager {
    private static final String TAG = "VocabularyManager";
    
    // Egyptian dialect specific vocabulary
    private static final Map<String, String> EGYPTIAN_VOCABULARY = new HashMap<>();
    
    static {
        // Initialize Egyptian dialect vocabulary
        initializeEgyptianVocabulary();
    }
    
    private static void initializeEgyptianVocabulary() {
        // Common Egyptian dialect words and phrases
        EGYPTIAN_VOCABULARY.put("بكره", "بكرة");
        EGYPTIAN_VOCABULARY.put("امبارح", "أمس");
        EGYPTIAN_VOCABULARY.put("النهارده", "اليوم");
        EGYPTIAN_VOCABULARY.put("دلوقتي", "الآن");
        EGYPTIAN_VOCABULARY.put("فين", "أين");
        EGYPTIAN_VOCABULARY.put("ازاي", "كيف");
        EGYPTIAN_VOCABULARY.put("أكيد", "بالتأكيد");
        EGYPTIAN_VOCABULARY.put("مفيش", "لا يوجد");
        EGYPTIAN_VOCABULARY.put("زي الفل", "تمامًا");
        EGYPTIAN_VOCABULARY.put("بلاش", "لا");
        EGYPTIAN_VOCABULARY.put("إيه", "ماذا");
        EGYPTIAN_VOCABULARY.put("إيه الاخبار", "ما الأخبار");
        EGYPTIAN_VOCABULARY.put("عايز", "أريد");
        EGYPTIAN_VOCABULARY.put("عاوز", "أريد");
        EGYPTIAN_VOCABULARY.put("معلش", "عفوًا");
        EGYPTIAN_VOCABULARY.put(" ana ", "أنا ");
        EGYPTIAN_VOCABULARY.put(" ana ", "أنا ");
        EGYPTIAN_VOCABULARY.put("yesta2n", "يتأكد");
        EGYPTIAN_VOCABULARY.put("yeghyr", "يغير");
        EGYPTIAN_VOCABULARY.put("yefhem", "يفهم");
        EGYPTIAN_VOCABULARY.put("yeghayer", "يغير");
        EGYPTIAN_VOCABULARY.put("yesta2n", "يتأكد");
        EGYPTIAN_VOCABULARY.put("yeghyr", "يغير");
        EGYPTIAN_VOCABULARY.put("yefhem", "يفهم");
        EGYPTIAN_VOCABULARY.put("yeghayer", "يغير");
    }
    
    /**
     * Loads custom vocabulary into the recognizer
     * @param recognizer The Vosk recognizer to load vocabulary into
     */
    public static void loadCustomWords(Recognizer recognizer) {
        try {
            // Build a grammar string for Vosk
            StringBuilder grammar = new StringBuilder("[");
            
            boolean first = true;
            for (String word : EGYPTIAN_VOCABULARY.keySet()) {
                if (!first) {
                    grammar.append(", ");
                }
                grammar.append("\"").append(word).append("\"");
                first = false;
            }
            
            // Add common Egyptian phrases
            String[] commonPhrases = {
                "يا كبير", "يا صاحبي", "اتصل بأمي", "اتصل بأبي", 
                "ابعت واتساب", "قول لأحمد", "قول لباسم", "قول لمحمد",
                "نّبهني", "انبهني", "الساعة كام", "الوقت كام",
                "كلم ماما", "كلم بابا", "рен على", "ابعتلي رسالة"
            };
            
            for (String phrase : commonPhrases) {
                grammar.append(", \"").append(phrase).append("\"");
            }
            
            grammar.append("]");
            
            // Set the grammar for the recognizer
            recognizer.setGrammar(grammar.toString());
            
            Log.i(TAG, "Custom Egyptian vocabulary loaded into recognizer");
        } catch (Exception e) {
            Log.e(TAG, "Error loading custom vocabulary", e);
        }
    }
    
    /**
     * Adds a new word to the vocabulary
     * @param dialectWord The Egyptian dialect word
     * @param standardWord The standard Arabic equivalent
     */
    public static void addWord(String dialectWord, String standardWord) {
        EGYPTIAN_VOCABULARY.put(dialectWord, standardWord);
    }
    
    /**
     * Gets the standard Arabic equivalent of an Egyptian dialect word
     * @param dialectWord The Egyptian dialect word
     * @return The standard Arabic equivalent, or the original word if not found
     */
    public static String getStandardWord(String dialectWord) {
        return EGYPTIAN_VOCABULARY.getOrDefault(dialectWord, dialectWord);
    }
    
    /**
     * Checks if a word is in the Egyptian vocabulary
     * @param word The word to check
     * @return true if the word is in the vocabulary, false otherwise
     */
    public static boolean isEgyptianWord(String word) {
        return EGYPTIAN_VOCABULARY.containsKey(word);
    }
    
    /**
     * Gets all Egyptian vocabulary
     * @return A copy of the Egyptian vocabulary map
     */
    public static Map<String, String> getAllVocabulary() {
        return new HashMap<>(EGYPTIAN_VOCABULARY);
    }
}