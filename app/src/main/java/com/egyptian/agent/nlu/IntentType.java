package com.egyptian.agent.nlu;

/**
 * Intent Type Enumeration
 * 
 * Defines all supported intent types for the Egyptian Agent.
 */
public enum IntentType {
    // Communication intents
    CALL_CONTACT("CALL_CONTACT", "اتصال بجهة اتصال"),
    SEND_WHATSAPP("SEND_WHATSAPP", "إرسال واتساب"),
    SEND_VOICE_MESSAGE("SEND_VOICE_MESSAGE", "إرسال رسالة صوتية"),
    SEND_SMS("SEND_SMS", "إرسال رسالة نصية"),
    READ_MISSED_CALLS("READ_MISSED_CALLS", "قراءة المكالمات الفائتة"),
    
    // System control intents
    SET_ALARM("SET_ALARM", "ضبط منبه"),
    READ_TIME("READ_TIME", "قراءة الوقت"),
    TOGGLE_WIFI("TOGGLE_WIFI", "تشغيل/إيقاف الواي فاي"),
    TOGGLE_BLUETOOTH("TOGGLE_BLUETOOTH", "تشغيل/إيقاف البلوتوث"),
    TOGGLE_FLASHLIGHT("TOGGLE_FLASHLIGHT", "تشغيل/إيقاف الفلاش"),
    
    // App control intents
    OPEN_APP("OPEN_APP", "فتح تطبيق"),
    CLOSE_APP("CLOSE_APP", "إغلاق تطبيق"),
    
    // Information intents
    WEATHER_QUERY("WEATHER_QUERY", "استفسار عن الطقس"),
    NEWS_QUERY("NEWS_QUERY", "استفسار عن الأخبار"),
    GENERAL_QUERY("GENERAL_QUERY", "استفسار عام"),
    
    // Emergency intents
    EMERGENCY("EMERGENCY", "طوارئ"),
    SENIOR_ASSIST("SENIOR_ASSIST", "مساعدة كبار السن"),
    
    // Conversation intents
    GREETING("GREETING", "تحية"),
    THANK_YOU("THANK_YOU", "شكر"),
    GOODBYE("GOODBYE", "وداع"),
    
    // Unknown
    UNKNOWN("UNKNOWN", "غير معروف");
    
    private final String code;
    private final String arabicName;
    
    IntentType(String code, String arabicName) {
        this.code = code;
        this.arabicName = arabicName;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getArabicName() {
        return arabicName;
    }
    
    /**
     * Parse intent type from string code.
     */
    public static IntentType fromCode(String code) {
        if (code == null) return UNKNOWN;
        
        for (IntentType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
    
    /**
     * Parse intent type from OpenPhone string format.
     */
    public static IntentType fromOpenPhoneString(String openPhoneString) {
        if (openPhoneString == null) return UNKNOWN;
        
        String normalized = openPhoneString.toUpperCase().trim();
        
        // Map OpenPhone intent names to our enum
        switch (normalized) {
            case "CALL_PERSON":
            case "CALL_CONTACT":
                return CALL_CONTACT;
            case "SEND_WHATSAPP":
            case "SEND_MESSAGE":
                return SEND_WHATSAPP;
            case "SEND_VOICE_MESSAGE":
                return SEND_VOICE_MESSAGE;
            case "SET_ALARM":
            case "SET_REMINDER":
                return SET_ALARM;
            case "READ_TIME":
            case "GET_TIME":
                return READ_TIME;
            case "READ_MISSED_CALLS":
                return READ_MISSED_CALLS;
            case "EMERGENCY":
                return EMERGENCY;
            case "TOGGLE_WIFI":
            case "WIFI_ON":
            case "WIFI_OFF":
                return TOGGLE_WIFI;
            case "TOGGLE_BLUETOOTH":
            case "BLUETOOTH_ON":
            case "BLUETOOTH_OFF":
                return TOGGLE_BLUETOOTH;
            case "OPEN_APP":
                return OPEN_APP;
            default:
                return UNKNOWN;
        }
    }
}
