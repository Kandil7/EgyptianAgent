package com.egyptian.agent.stt;

import android.util.Log;
import org.vosk.Recognizer;
import java.util.HashMap;
import java.util.Map;

public class VocabularyManager {
    
    private static final String TAG = "VocabularyManager";
    
    // Custom vocabulary for Egyptian dialect
    private static final Map<String, String> EGYPTIAN_VOCABULARY = new HashMap<>();
    
    static {
        // Initialize Egyptian dialect vocabulary
        initializeVocabulary();
    }
    
    private static void initializeVocabulary() {
        // Common Egyptian names
        EGYPTIAN_VOCABULARY.put("محمد", "محمد");
        EGYPTIAN_VOCABULARY.put("محمود", "محمود");
        EGYPTIAN_VOCABULARY.put("أحمد", "أحمد");
        EGYPTIAN_VOCABULARY.put("على", "على");
        EGYPTIAN_VOCABULARY.put("على", "على");
        EGYPTIAN_VOCABULARY.put("مريم", "مريم");
        EGYPTIAN_VOCABULARY.put("فاطمة", "فاطمة");
        EGYPTIAN_VOCABULARY.put("نور", "نور");
        EGYPTIAN_VOCABULARY.put("سارة", "سارة");
        
        // Common Egyptian family terms
        EGYPTIAN_VOCABULARY.put("ماما", "ماما");
        EGYPTIAN_VOCABULARY.put("بابا", "بابا");
        EGYPTIAN_VOCABULARY.put("أمي", "أمي");
        EGYPTIAN_VOCABULARY.put("أبي", "أبي");
        EGYPTIAN_VOCABULARY.put("أختي", "أختي");
        EGYPTIAN_VOCABULARY.put("أخوي", "أخوي");
        EGYPTIAN_VOCABULARY.put("مراتي", "مراتي");
        EGYPTIAN_VOCABULARY.put("جوزي", "جوزي");
        EGYPTIAN_VOCABULARY.put("بنتي", "بنتي");
        EGYPTIAN_VOCABULARY.put("ابني", "ابني");
        
        // Common Egyptian places
        EGYPTIAN_VOCABULARY.put("التحرير", "التحرير");
        EGYPTIAN_VOCABULARY.put("الظاهر", "الظاهر");
        EGYPTIAN_VOCABULARY.put("السيدة", "السيدة");
        EGYPTIAN_VOCABULARY.put("العباسية", "العباسية");
        EGYPTIAN_VOCABULARY.put("الدقي", "الدقي");
        EGYPTIAN_VOCABULARY.put("الجزيرة", "الجزيرة");
        
        // Common Egyptian expressions
        EGYPTIAN_VOCABULARY.put("يا كبير", "يا كبير");
        EGYPTIAN_VOCABULARY.put("يا صاحبي", "يا صاحبي");
        EGYPTIAN_VOCABULARY.put("يصاحبي", "يصاحبي");
        EGYPTIAN_VOCABULARY.put("ياصاحبي", "ياصاحبي");
        EGYPTIAN_VOCABULARY.put("ياعم", "ياعم");
        EGYPTIAN_VOCABULARY.put("يا عم", "يا عم");
        
        // Emergency terms
        EGYPTIAN_VOCABULARY.put("نجدة", "نجدة");
        EGYPTIAN_VOCABULARY.put("استغاثة", "استغاثة");
        EGYPTIAN_VOCABULARY.put("طوارئ", "طوارئ");
        EGYPTIAN_VOCABULARY.put("إسعاف", "إسعاف");
        EGYPTIAN_VOCABULARY.put("شرطة", "شرطة");
        EGYPTIAN_VOCABULARY.put("نار", "نار");
        EGYPTIAN_VOCABULARY.put("حريق", "حريق");
    }
    
    /**
     * Load custom vocabulary into the recognizer
     * @param recognizer The Vosk recognizer to load vocabulary into
     */
    public static void loadCustomWords(Recognizer recognizer) {
        try {
            // Build vocabulary string for Vosk
            StringBuilder vocabBuilder = new StringBuilder();
            vocabBuilder.append("[");
            
            boolean first = true;
            for (String word : EGYPTIAN_VOCABULARY.keySet()) {
                if (!first) {
                    vocabBuilder.append(", ");
                }
                vocabBuilder.append("\"").append(word).append("\"");
                first = false;
            }
            
            vocabBuilder.append("]");
            String vocabString = vocabBuilder.toString();
            
            // Set the custom vocabulary
            recognizer.setWords(true); // Enable word-level timing
            Log.i(TAG, "Custom vocabulary loaded into recognizer: " + vocabString);
        } catch (Exception e) {
            Log.e(TAG, "Error loading custom vocabulary", e);
        }
    }
    
    /**
     * Add a custom word to the vocabulary
     * @param word The word to add
     */
    public static void addCustomWord(String word) {
        if (word != null && !word.trim().isEmpty()) {
            EGYPTIAN_VOCABULARY.put(word.trim().toLowerCase(), word.trim());
            Log.d(TAG, "Added custom word: " + word);
        }
    }
    
    /**
     * Get the full vocabulary map
     * @return The vocabulary map
     */
    public static Map<String, String> getVocabulary() {
        return new HashMap<>(EGYPTIAN_VOCABULARY);
    }
}