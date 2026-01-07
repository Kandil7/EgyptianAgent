package com.egyptian.agent.core;

import com.egyptian.agent.stt.EgyptianNormalizer;
import java.util.regex.Pattern;

public class IntentRouter {
    
    public static IntentType detectIntent(String command) {
        // Normalize the command first
        String normalized = EgyptianNormalizer.normalize(command.toLowerCase());
        
        // Check for emergency keywords first
        if (isEmergency(normalized)) {
            return IntentType.EMERGENCY;
        }
        
        // Check for call-related commands
        if (isCallIntent(normalized)) {
            return IntentType.CALL_CONTACT;
        }
        
        // Check for WhatsApp commands
        if (isWhatsAppIntent(normalized)) {
            return IntentType.SEND_WHATSAPP;
        }
        
        // Check for alarm/set reminder commands
        if (isAlarmIntent(normalized)) {
            return IntentType.SET_ALARM;
        }
        
        // Check for reading missed calls
        if (isReadMissedCallsIntent(normalized)) {
            return IntentType.READ_MISSED_CALLS;
        }
        
        // Check for time reading
        if (isReadTimeIntent(normalized)) {
            return IntentType.READ_TIME;
        }
        
        // Default to unknown if no specific intent is detected
        return IntentType.UNKNOWN;
    }
    
    private static boolean isEmergency(String command) {
        return command.contains("نجدة") || 
               command.contains("استغاثة") || 
               command.contains("طوارئ") || 
               command.contains("مش قادر") || 
               command.contains("حد يجي") || 
               command.contains("إسعاف") || 
               command.contains("شرطة") || 
               command.contains("حرقان") || 
               command.contains("طلق ناري");
    }
    
    private static boolean isCallIntent(String command) {
        return command.contains("اتصل") || 
               command.contains("كلم") || 
               command.contains("رن") || 
               command.contains("بعت") || // Egyptian dialect for "call"
               command.contains("اتكلم");
    }
    
    private static boolean isWhatsAppIntent(String command) {
        return command.contains("واتساب") || 
               command.contains("رساله") || 
               command.contains("ابعت") || 
               command.contains("ارسل");
    }
    
    private static boolean isAlarmIntent(String command) {
        return command.contains("انبهني") || 
               command.contains("نبهني") || 
               command.contains("فكرني") || 
               command.contains("ذكرني") || 
               command.contains("المنبه") || 
               command.contains("التنبيه");
    }
    
    private static boolean isReadMissedCallsIntent(String command) {
        return command.contains("فايتة") || 
               command.contains("فايتات") || 
               command.contains("المكالمات") || 
               command.contains("اللي فاتت");
    }
    
    private static boolean isReadTimeIntent(String command) {
        return command.contains("الساعه") || 
               command.contains("الوقت") || 
               command.contains("كام") || 
               command.contains("الساعة");
    }
}