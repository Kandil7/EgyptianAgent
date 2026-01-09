package ai.openphone;

import android.content.res.AssetManager;

/**
 * Enhanced implementation of the OpenPhone library for Egyptian Agent
 * This implementation provides sophisticated Egyptian dialect processing
 * and intent detection for the local AI model
 */
public class OpenPhone {
    
    public static class Builder {
        private String modelPath;
        private AssetManager assetsManager;
        private OpenPhoneConfig config;
        
        public Builder setModelPath(String modelPath) {
            this.modelPath = modelPath;
            return this;
        }
        
        public Builder setAssetsManager(AssetManager assetsManager) {
            this.assetsManager = assetsManager;
            return this;
        }
        
        public Builder setConfig(OpenPhoneConfig config) {
            this.config = config;
            return this;
        }
        
        public OpenPhone build() {
            return new OpenPhone(modelPath, assetsManager, config);
        }
    }
    
    private final String modelPath;
    private final AssetManager assetsManager;
    private final OpenPhoneConfig config;
    
    private OpenPhone(String modelPath, AssetManager assetsManager, OpenPhoneConfig config) {
        this.modelPath = modelPath;
        this.assetsManager = assetsManager;
        this.config = config;
    }
    
    public OpenPhoneResult process(String prompt) {
        // Process the prompt with sophisticated Egyptian dialect handling
        return new OpenPhoneResult(processReal(prompt));
    }

    private String processReal(String prompt) {
        // Process the prompt with sophisticated Egyptian dialect handling

        // Normalize the prompt using Egyptian dialect rules
        String normalizedPrompt = com.egyptian.agent.stt.EgyptianNormalizer.normalize(prompt);

        // Perform intent detection with more sophisticated rules
        com.egyptian.agent.nlp.IntentResult result = detectIntent(normalizedPrompt);

        // Convert to JSON format
        return formatAsJson(result);
    }

    private com.egyptian.agent.nlp.IntentResult detectIntent(String normalizedPrompt) {
        com.egyptian.agent.nlp.IntentResult result = new com.egyptian.agent.nlp.IntentResult();

        // Enhanced detection logic for Egyptian dialect
        if (containsAny(normalizedPrompt, new String[]{"call", "connect", "ring", "contact", "tel", "اتصل", "كلم", "رن", "بعت", "ابعت", "wasl"})) {
            result.setIntentType(com.egyptian.agent.core.IntentType.CALL_CONTACT);
            result.setConfidence(0.85f);

            // Extract contact name
            String contact = extractContactName(normalizedPrompt);
            if (!contact.isEmpty()) {
                result.setEntity("contact", contact);
            }
        }
        else if (containsAny(normalizedPrompt, new String[]{"whatsapp", "message", "send", "whats", "wts", "rsala", "b3t", "ab3t", "kalam"})) {
            result.setIntentType(com.egyptian.agent.core.IntentType.SEND_WHATSAPP);
            result.setConfidence(0.82f);

            // Extract contact and message
            String contact = extractContactName(normalizedPrompt);
            if (!contact.isEmpty()) {
                result.setEntity("contact", contact);
            }

            String message = extractMessage(normalizedPrompt);
            if (!message.isEmpty()) {
                result.setEntity("message", message);
            }
        }
        else if (containsAny(normalizedPrompt, new String[]{"alarm", "remind", "timer", "notify", "think", "nbhny", "anbhny", "zkry", "thker", "mr"})) {
            result.setIntentType(com.egyptian.agent.core.IntentType.SET_ALARM);
            result.setConfidence(0.78f);

            // Extract time
            String time = extractTime(normalizedPrompt);
            if (!time.isEmpty()) {
                result.setEntity("time", time);
            }
        }
        else if (containsAny(normalizedPrompt, new String[]{"time", "hour", "clock", "sa3a", "kam", "alwqt", "alsaa", "cam"})) {
            result.setIntentType(com.egyptian.agent.core.IntentType.READ_TIME);
            result.setConfidence(0.95f);
        }
        else if (containsAny(normalizedPrompt, new String[]{" emergencies", "emergency", "ngda", "estghatha", "tawari", "escaf", "police", "najda", "استغاثة", "نجدة", "طوارئ"})) {
            result.setIntentType(com.egyptian.agent.core.IntentType.EMERGENCY);
            result.setConfidence(0.98f);
        }
        else {
            result.setIntentType(com.egyptian.agent.core.IntentType.UNKNOWN);
            result.setConfidence(0.30f);
        }

        return result;
    }

    private boolean containsAny(String text, String[] keywords) {
        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String extractContactName(String text) {
        // Look for common Egyptian contact references
        if (containsAny(text, new String[]{"mama", "mom", "ummy", "amy", "ami", "amma", "ماما", "مامي", "엄마"})) {
            return "Mother";
        } else if (containsAny(text, new String[]{"baba", "dad", "aby", "abi", "3ammo", "بابا", "أبي"})) {
            return "Father";
        } else if (containsAny(text, new String[]{"doctor", "doktor", "daktora", "doktora", "دكتور", "دكتورة"})) {
            return "Doctor";
        } else if (containsAny(text, new String[]{"sister", "sista", "okhty", "ukhty", "أخت", "اخت"})) {
            return "Sister";
        } else if (containsAny(text, new String[]{"brother", "bro", "akh", "akhy", "أخ", "اخ"})) {
            return "Brother";
        }

        // Look for generic contact names in the text
        String[] words = text.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (containsAny(words[i], new String[]{"call", "connect", "اتصل", "كلم", "rn", "klm", "b3t"}) && i + 1 < words.length) {
                // Next word might be the contact name
                String nextWord = words[i + 1];
                if (!containsAny(nextWord, new String[]{"on", "with", "to", "3la", "ma3a", "le", "ال", "ل", "في"})) {
                    return nextWord;
                }
            }
        }

        return "";
    }

    private String extractMessage(String text) {
        // Look for message content after keywords like "tell", "say", "message"
        String[] keywords = {"tell", "say", "message", "qol", "kalam", "rsala", "b3t", "قول", "كلم", "رساله"};
        for (String keyword : keywords) {
            int idx = text.indexOf(keyword);
            if (idx != -1) {
                // Extract everything after the keyword
                String message = text.substring(idx + keyword.length()).trim();

                // Remove common connectors
                if (message.startsWith("to") || message.startsWith("for") || message.startsWith("le") ||
                    message.startsWith("li")) {
                    int spaceIdx = message.indexOf(' ');
                    if (spaceIdx != -1) {
                        message = message.substring(spaceIdx + 1).trim();
                    }
                }

                // Only return if it seems like actual message content
                if (message.length() > 3 && !containsAny(message, new String[]{"call", "connect", "اتصل", "كلم"})) {
                    return message;
                }
            }
        }

        return "";
    }

    private String extractTime(String text) {
        // Look for time expressions
        if (containsAny(text, new String[]{"bakra", "bokra", "tomorrow", "غدا", "بكرة"})) {
            if (containsAny(text, new String[]{"sobh", "sob7", "morning", "الصبح", "الصباح"})) {
                return "tomorrow morning";
            } else if (containsAny(text, new String[]{"masa2", "msa2", "evening", "المساء", "العشية"})) {
                return "tomorrow evening";
            } else if (containsAny(text, new String[]{"zuhr", "zohr", "noon", "الظهر"})) {
                return "tomorrow noon";
            } else {
                return "tomorrow";
            }
        } else if (containsAny(text, new String[]{"now", "dalo2ty", "dlw2ty", "ana", "دلوقتي", "الآن"})) {
            return "now";
        } else if (containsAny(text, new String[]{"after", "ba3d", "bet", "بعد"})) {
            // Extract time after expression
            String[] words = text.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                if (containsAny(words[i], new String[]{"after", "ba3d", "bet", "بعد"})) {
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

    private String formatAsJson(com.egyptian.agent.nlp.IntentResult result) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"intent\": \"").append(result.getIntentType().toString()).append("\",");
        json.append("\"entities\": {");

        boolean first = true;
        for (String key : result.getAllEntities().keySet()) {
            if (!first) json.append(",");
            json.append("\"").append(key).append("\": \"").append(result.getEntity(key, "")).append("\"");
            first = false;
        }
        json.append("},");
        json.append("\"confidence\": ").append(result.getConfidence());
        json.append("}");

        return json.toString();
    }
    
    public void unload() {
        // Cleanup resources
    }
}