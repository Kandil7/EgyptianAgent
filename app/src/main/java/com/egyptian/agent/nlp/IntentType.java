package com.egyptian.agent.nlp;

public enum IntentType {
    UNKNOWN,
    CALL_CONTACT,
    SEND_WHATSAPP,
    SET_ALARM,
    READ_MISSED_CALLS,
    READ_TIME,
    EMERGENCY,
    ENABLE_SENIOR_MODE,
    DISABLE_SENIOR_MODE,
    FALL_DETECTION,
    MEDICATION_REMINDER,
    WEATHER_INFO,
    NEWS_BRIEFING,
    CALENDAR_EVENT,
    LOCATION_INFO;

    public static IntentType fromOpenPhoneString(String intentStr) {
        if (intentStr == null) {
            return UNKNOWN;
        }

        switch (intentStr.toUpperCase()) {
            case "CALL_CONTACT":
            case "CALL":
                return CALL_CONTACT;
            case "SEND_WHATSAPP":
            case "WHATSAPP":
                return SEND_WHATSAPP;
            case "SET_ALARM":
            case "ALARM":
                return SET_ALARM;
            case "READ_MISSED_CALLS":
            case "MISSED_CALLS":
                return READ_MISSED_CALLS;
            case "READ_TIME":
            case "TIME":
            case "WHAT_TIME":
                return READ_TIME;
            case "EMERGENCY":
            case "EMERGENCY_CALL":
                return EMERGENCY;
            case "ENABLE_SENIOR_MODE":
            case "SENIOR_MODE_ON":
                return ENABLE_SENIOR_MODE;
            case "DISABLE_SENIOR_MODE":
            case "SENIOR_MODE_OFF":
                return DISABLE_SENIOR_MODE;
            case "FALL_DETECTION":
            case "FALL_ALERT":
                return FALL_DETECTION;
            case "MEDICATION_REMINDER":
            case "MEDICATION":
                return MEDICATION_REMINDER;
            case "WEATHER_INFO":
            case "WEATHER":
                return WEATHER_INFO;
            case "NEWS_BRIEFING":
            case "NEWS":
                return NEWS_BRIEFING;
            case "CALENDAR_EVENT":
            case "CALENDAR":
                return CALENDAR_EVENT;
            case "LOCATION_INFO":
            case "LOCATION":
                return LOCATION_INFO;
            default:
                return UNKNOWN;
        }
    }
}