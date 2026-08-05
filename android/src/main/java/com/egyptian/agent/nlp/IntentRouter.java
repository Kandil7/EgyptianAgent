package com.egyptian.agent.nlp;

import android.util.Log;
import com.egyptian.agent.stt.EgyptianNormalizer;

public class IntentRouter {
    private static final String TAG = "IntentRouter";

    /**
     * Detects the intent from the given text
     * @param text The input text to analyze
     * @return The detected intent type
     */
    public static IntentType detectIntent(String text) {
        Log.d(TAG, "Detecting intent for text: " + text);
        
        // Normalize the text first
        String normalizedText = EgyptianNormalizer.normalize(text);
        
        // Check for emergency keywords first (highest priority)
        if (isEmergencyIntent(normalizedText)) {
            return IntentType.EMERGENCY;
        }
        
        // Check for other intents
        if (isCallIntent(normalizedText)) {
            return IntentType.CALL_CONTACT;
        } else if (isWhatsAppIntent(normalizedText)) {
            return IntentType.SEND_WHATSAPP;
        } else if (isAlarmIntent(normalizedText)) {
            return IntentType.SET_ALARM;
        } else if (isReadTimeIntent(normalizedText)) {
            return IntentType.READ_TIME;
        } else if (isReadMissedCallsIntent(normalizedText)) {
            return IntentType.READ_MISSED_CALLS;
        } else {
            return IntentType.UNKNOWN;
        }
    }
    
    private static boolean isEmergencyIntent(String text) {
        String lowerText = text.toLowerCase();
        
        // Check for emergency keywords
        return lowerText.contains("نجدة") || 
               lowerText.contains("استغاثة") || 
               lowerText.contains("طوارئ") || 
               lowerText.contains("إسعاف") || 
               lowerText.contains("救急") || 
               lowerText.contains("help") || 
               lowerText.contains("danger") ||
               lowerText.contains("urgent") ||
               lowerText.contains("emergency");
    }
    
    private static boolean isCallIntent(String text) {
        String lowerText = text.toLowerCase();
        
        // Check for call-related keywords
        return lowerText.contains("اتصل") || 
               lowerText.contains("كلم") || 
               lowerText.contains("رن") || 
               lowerText.contains("اتكلم") ||
               lowerText.contains("call") ||
               lowerText.contains("ring") ||
               lowerText.contains("dial");
    }
    
    private static boolean isWhatsAppIntent(String text) {
        String lowerText = text.toLowerCase();
        
        // Check for WhatsApp-related keywords
        return lowerText.contains("واتساب") || 
               lowerText.contains("whatsapp") || 
               lowerText.contains("ابعت") || 
               lowerText.contains("رساله") || 
               lowerText.contains("message") ||
               lowerText.contains("send") ||
               lowerText.contains("text");
    }
    
    private static boolean isAlarmIntent(String text) {
        String lowerText = text.toLowerCase();
        
        // Check for alarm/remind-related keywords
        return lowerText.contains("انبهني") || 
               lowerText.contains("نبهني") || 
               lowerText.contains("ذكرني") || 
               lowerText.contains("ال提醒") || 
               lowerText.contains("remind") || 
               lowerText.contains("alarm") ||
               lowerText.contains("set") ||
               lowerText.contains("alert");
    }
    
    private static boolean isReadTimeIntent(String text) {
        String lowerText = text.toLowerCase();
        
        // Check for time-related keywords
        return lowerText.contains("الوقت") || 
               lowerText.contains("الساعه") || 
               lowerText.contains("كام") || 
               lowerText.contains("time") || 
               lowerText.contains("hour") ||
               lowerText.contains("what time");
    }
    
    private static boolean isReadMissedCallsIntent(String text) {
        String lowerText = text.toLowerCase();
        
        // Check for missed calls keywords
        return lowerText.contains("فايتة") || 
               lowerText.contains("فايتات") || 
               lowerText.contains("المكالمات") || 
               lowerText.contains("missed") || 
               lowerText.contains("calls") ||
               lowerText.contains("look") ||
               lowerText.contains("check");
    }
}