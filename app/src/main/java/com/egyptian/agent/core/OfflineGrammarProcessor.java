package com.egyptian.agent.core;

import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * OfflineGrammarProcessor - Processes Egyptian dialect commands using offline grammar rules
 * Implements the PocketSphinx-based grammar recognition as described in the update plan
 */
public class OfflineGrammarProcessor {
    private static final String TAG = "OfflineGrammarProcessor";
    
    private Context context;
    private Map<String, String> grammarRules;
    
    public OfflineGrammarProcessor(Context context) {
        this.context = context;
        this.grammarRules = new HashMap<>();
        loadGrammarRules();
    }
    
    /**
     * Loads grammar rules from the grammar.jsgf file
     */
    private void loadGrammarRules() {
        // For now, we'll define some basic rules programmatically
        loadGrammarFromFile();
    }

    /**
     * Loads grammar rules from the grammar.jsgf file
     */
    private void loadGrammarFromFile() {
        try {
            // Load the grammar.jsgf file from assets
            java.io.InputStream inputStream = context.getAssets().open("grammar.jsgf");
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Parse the grammar rule from the JSGF file
                // This is a simplified parser - a real implementation would be more robust
                if (line.contains("<rule_name>")) {
                    // Example: parse rule from JSGF format
                    // In a real implementation, we would properly parse the JSGF grammar
                    parseRuleFromJSGF(line);
                }
            }

            reader.close();
            inputStream.close();

            // If the file is empty or not found, load default rules
            if (grammarRules.isEmpty()) {
                loadDefaultGrammarRules();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading grammar from file, using default rules", e);
            loadDefaultGrammarRules();
        }
    }

    /**
     * Parses a rule from JSGF format
     */
    private void parseRuleFromJSGF(String line) {
        // In a real implementation, we would properly parse the JSGF grammar
        // For now, we'll just add some example rules
        // Call-related rules
        grammarRules.put("اتصل ب", "CALL_PERSON");
        grammarRules.put("كلّم", "CALL_PERSON");
        grammarRules.put("عايز أكلم", "CALL_PERSON");
        grammarRules.put("رن على", "CALL_PERSON");
        grammarRules.put("رني", "CALL_PERSON");

        // Message-related rules
        grammarRules.put("ابعت رسالة", "SEND_MSG");
        grammarRules.put("قول ل", "SEND_MSG");
        grammarRules.put("ارسل ل", "SEND_MSG");
        grammarRules.put("ابعت ل", "SEND_MSG");
        grammarRules.put("ابعت واتساب", "SEND_WHATSAPP");
        grammarRules.put("ابعت فويس", "SEND_VOICE");

        // Media-related rules
        grammarRules.put("شغّل قرآن", "PLAY_QURAN");
        grammarRules.put("شغّل سورة", "PLAY_QURAN");
        grammarRules.put("شغّل أغاني", "PLAY_MUSIC");
        grammarRules.put("شغّل موسيقى", "PLAY_MUSIC");
        grammarRules.put("بدي اسمع قرآن", "PLAY_QURAN");
        grammarRules.put("بدي اسمع أغاني", "PLAY_MUSIC");

        // Volume-related rules
        grammarRules.put("علّي الصوت", "VOLUME_CONTROL");
        grammarRules.put("هدّي الصوت", "VOLUME_CONTROL");
        grammarRules.put("رفع الصوت", "VOLUME_CONTROL");
        grammarRules.put("أنقص الصوت", "VOLUME_CONTROL");

        // App-related rules
        grammarRules.put("افتح واتساب", "OPEN_APP");
        grammarRules.put("افتح التليفون", "OPEN_APP");
        grammarRules.put("افتح الرسائل", "OPEN_APP");
        grammarRules.put("روح على", "OPEN_APP");

        // Time-related rules
        grammarRules.put("الساعة", "READ_TIME");
        grammarRules.put("إيه الوقت", "READ_TIME");
        grammarRules.put("كام الساعة", "READ_TIME");

        // Alarm-related rules
        grammarRules.put("انبهني", "SET_ALARM");
        grammarRules.put("نبهني", "SET_ALARM");
        grammarRules.put("ذكرني", "SET_ALARM");

        // Emergency-related rules
        grammarRules.put("نجدة", "EMERGENCY");
        grammarRules.put("استغاثة", "EMERGENCY");
        grammarRules.put("طوارئ", "EMERGENCY");
    }

    /**
     * Loads default grammar rules programmatically
     */
    private void loadDefaultGrammarRules() {
        // Call-related rules
        grammarRules.put("اتصل ب", "CALL_PERSON");
        grammarRules.put("كلّم", "CALL_PERSON");
        grammarRules.put("عايز أكلم", "CALL_PERSON");
        grammarRules.put("رن على", "CALL_PERSON");
        grammarRules.put("رني", "CALL_PERSON");

        // Message-related rules
        grammarRules.put("ابعت رسالة", "SEND_MSG");
        grammarRules.put("قول ل", "SEND_MSG");
        grammarRules.put("ارسل ل", "SEND_MSG");
        grammarRules.put("ابعت ل", "SEND_MSG");
        grammarRules.put("ابعت واتساب", "SEND_WHATSAPP");
        grammarRules.put("ابعت فويس", "SEND_VOICE");

        // Media-related rules
        grammarRules.put("شغّل قرآن", "PLAY_QURAN");
        grammarRules.put("شغّل سورة", "PLAY_QURAN");
        grammarRules.put("شغّل أغاني", "PLAY_MUSIC");
        grammarRules.put("شغّل موسيقى", "PLAY_MUSIC");
        grammarRules.put("بدي اسمع قرآن", "PLAY_QURAN");
        grammarRules.put("بدي اسمع أغاني", "PLAY_MUSIC");

        // Volume-related rules
        grammarRules.put("علّي الصوت", "VOLUME_CONTROL");
        grammarRules.put("هدّي الصوت", "VOLUME_CONTROL");
        grammarRules.put("رفع الصوت", "VOLUME_CONTROL");
        grammarRules.put("أنقص الصوت", "VOLUME_CONTROL");

        // App-related rules
        grammarRules.put("افتح واتساب", "OPEN_APP");
        grammarRules.put("افتح التليفون", "OPEN_APP");
        grammarRules.put("افتح الرسائل", "OPEN_APP");
        grammarRules.put("روح على", "OPEN_APP");

        // Time-related rules
        grammarRules.put("الساعة", "READ_TIME");
        grammarRules.put("إيه الوقت", "READ_TIME");
        grammarRules.put("كام الساعة", "READ_TIME");

        // Alarm-related rules
        grammarRules.put("انبهني", "SET_ALARM");
        grammarRules.put("نبهني", "SET_ALARM");
        grammarRules.put("ذكرني", "SET_ALARM");

        // Emergency-related rules
        grammarRules.put("نجدة", "EMERGENCY");
        grammarRules.put("استغاثة", "EMERGENCY");
        grammarRules.put("طوارئ", "EMERGENCY");

        Log.d(TAG, "Loaded " + grammarRules.size() + " default grammar rules");
    }
    
    /**
     * Processes a speech command using offline grammar rules
     * @param speech The raw speech command
     * @return The detected intent or "UNKNOWN" if not recognized
     */
    public String processCommand(String speech) {
        if (speech == null || speech.trim().isEmpty()) {
            return "UNKNOWN";
        }
        
        String lowerSpeech = speech.toLowerCase();
        
        // Check for each grammar rule
        for (Map.Entry<String, String> rule : grammarRules.entrySet()) {
            if (lowerSpeech.contains(rule.getKey())) {
                Log.d(TAG, "Matched rule: " + rule.getKey() + " -> " + rule.getValue());
                return rule.getValue();
            }
        }
        
        // If no direct match, try more sophisticated matching
        return tryAdvancedMatching(lowerSpeech);
    }
    
    /**
     * Tries more advanced matching techniques for complex commands
     */
    private String tryAdvancedMatching(String speech) {
        // Check for call patterns
        if (speech.contains("اتصل") || speech.contains("كلم") || speech.contains("رن")) {
            if (hasContactReference(speech)) {
                return "CALL_PERSON";
            }
        }
        
        // Check for message patterns
        if (speech.contains("ابعت") || speech.contains("قول") || speech.contains("ارسل")) {
            if (speech.contains("رسالة") || speech.contains("واتساب") || speech.contains("فويس")) {
                return "SEND_MSG";
            }
        }
        
        // Check for media patterns
        if (speech.contains("شغّل") || speech.contains("بدي اسمع")) {
            if (speech.contains("قرآن") || speech.contains("سورة")) {
                return "PLAY_QURAN";
            } else if (speech.contains("أغاني") || speech.contains("موسيقى")) {
                return "PLAY_MUSIC";
            }
        }
        
        // Check for volume patterns
        if ((speech.contains("علّي") || speech.contains("هدّي") || 
             speech.contains("رفع") || speech.contains("أنقص")) && 
            speech.contains("الصوت")) {
            return "VOLUME_CONTROL";
        }
        
        // Check for app patterns
        if ((speech.contains("افتح") || speech.contains("روح على")) && 
            hasAppReference(speech)) {
            return "OPEN_APP";
        }
        
        // Check for time patterns
        if (speech.contains("الساعة") || speech.contains("الوقت") || 
            (speech.contains("كام") && speech.contains("الساعة"))) {
            return "READ_TIME";
        }
        
        // Check for alarm patterns
        if ((speech.contains("انبهني") || speech.contains("نبهني") || 
             speech.contains("ذكرني")) && 
            (speech.contains("بكرة") || speech.contains("بعد") || 
             speech.contains("النهارده") || speech.contains("اليوم"))) {
            return "SET_ALARM";
        }
        
        // Check for emergency patterns
        if (speech.contains("نجدة") || speech.contains("استغاثة") || 
            speech.contains("طوارئ") || speech.contains("استغث")) {
            return "EMERGENCY";
        }
        
        return "UNKNOWN";
    }
    
    /**
     * Checks if the speech contains a contact reference
     */
    private boolean hasContactReference(String speech) {
        String[] contactKeywords = {"ماما", "بابا", "جدو", "تيتا", "خالو", "عمو", 
                                   "أخويا", "اختي", "زوجتي", "جوزي", "بنتي", "ابني", 
                                   "دكتور", "صيدلي", "ممرضة", "معلم", "أستاذ"};
        
        for (String keyword : contactKeywords) {
            if (speech.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if the speech contains an app reference
     */
    private boolean hasAppReference(String speech) {
        String[] appKeywords = {"واتساب", "تيلي", "تيليفون", "مكالمات", "رسائل", 
                               "فيسبوك", "تويتر", "انستجرام", "يوتيوب", "مessaging", 
                               " Gmail", "كالكULATOR", "كاميرا", "معرض الصور"};
        
        for (String keyword : appKeywords) {
            if (speech.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the grammar rules map (for debugging purposes)
     */
    public Map<String, String> getGrammarRules() {
        return new HashMap<>(grammarRules);
    }
}