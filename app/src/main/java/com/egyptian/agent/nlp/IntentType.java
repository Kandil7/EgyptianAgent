package com.egyptian.agent.nlp;

public enum IntentType {
    UNKNOWN,
    CALL_CONTACT,
    SEND_WHATSAPP,
    SET_ALARM,
    READ_TIME,
    READ_MISSED_CALLS,
    EMERGENCY;

    public static IntentType fromOpenPhoneString(String openPhoneIntent) {
        switch (openPhoneIntent.toLowerCase()) {
            case "call_contact":
            case "make_call":
                return CALL_CONTACT;
            case "send_whatsapp":
            case "whatsapp_message":
                return SEND_WHATSAPP;
            case "set_alarm":
            case "set_reminder":
                return SET_ALARM;
            case "read_time":
            case "tell_time":
                return READ_TIME;
            case "read_missed_calls":
                return READ_MISSED_CALLS;
            case "emergency":
            case "help":
                return EMERGENCY;
            default:
                return UNKNOWN;
        }
    }
}