package com.egyptian.agent.nlp;

/**
 * Intent types for Egyptian Agent
 * Defines the different types of commands the agent can understand
 */
public enum IntentType {
    UNKNOWN,
    CALL_CONTACT,
    SEND_WHATSAPP,
    SET_ALARM,
    READ_TIME,
    READ_MISSED_CALLS,
    EMERGENCY,
    SEND_VOICE_MESSAGE,
    READ_CONTACTS,
    ADD_CONTACT,
    READ_SMS,
    SEND_SMS,
    OPEN_APP,
    CLOSE_APP,
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN,
    VOLUME_UP,
    VOLUME_DOWN,
    AIRPLANE_MODE,
    WIFI_TOGGLE,
    BLUETOOTH_TOGGLE,
    LOCATION_TOGGLE;

    /**
     * Converts from OpenPhone string representation
     * @param openPhoneString The OpenPhone string representation
     * @return The corresponding IntentType
     */
    public static IntentType fromOpenPhoneString(String openPhoneString) {
        if (openPhoneString == null) {
            return UNKNOWN;
        }

        switch (openPhoneString.toUpperCase()) {
            case "CALL_CONTACT":
            case "CALL_PERSON":
                return CALL_CONTACT;
            case "SEND_WHATSAPP":
            case "SEND_MESSAGE":
                return SEND_WHATSAPP;
            case "SET_ALARM":
            case "REMIND":
                return SET_ALARM;
            case "READ_TIME":
            case "WHAT_TIME":
                return READ_TIME;
            case "READ_MISSED_CALLS":
            case "CHECK_CALLS":
                return READ_MISSED_CALLS;
            case "EMERGENCY":
            case "HELP":
                return EMERGENCY;
            case "SEND_VOICE_MESSAGE":
                return SEND_VOICE_MESSAGE;
            case "READ_CONTACTS":
                return READ_CONTACTS;
            case "ADD_CONTACT":
                return ADD_CONTACT;
            case "READ_SMS":
                return READ_SMS;
            case "SEND_SMS":
                return SEND_SMS;
            case "OPEN_APP":
                return OPEN_APP;
            case "CLOSE_APP":
                return CLOSE_APP;
            case "BRIGHTNESS_UP":
                return BRIGHTNESS_UP;
            case "BRIGHTNESS_DOWN":
                return BRIGHTNESS_DOWN;
            case "VOLUME_UP":
                return VOLUME_UP;
            case "VOLUME_DOWN":
                return VOLUME_DOWN;
            case "AIRPLANE_MODE":
                return AIRPLANE_MODE;
            case "WIFI_TOGGLE":
                return WIFI_TOGGLE;
            case "BLUETOOTH_TOGGLE":
                return BLUETOOTH_TOGGLE;
            case "LOCATION_TOGGLE":
                return LOCATION_TOGGLE;
            default:
                return UNKNOWN;
        }
    }

    /**
     * Gets the string representation for OpenPhone compatibility
     * @return The OpenPhone string representation
     */
    public String toOpenPhoneString() {
        switch (this) {
            case CALL_CONTACT:
                return "CALL_CONTACT";
            case SEND_WHATSAPP:
                return "SEND_WHATSAPP";
            case SET_ALARM:
                return "SET_ALARM";
            case READ_TIME:
                return "READ_TIME";
            case READ_MISSED_CALLS:
                return "READ_MISSED_CALLS";
            case EMERGENCY:
                return "EMERGENCY";
            case SEND_VOICE_MESSAGE:
                return "SEND_VOICE_MESSAGE";
            case READ_CONTACTS:
                return "READ_CONTACTS";
            case ADD_CONTACT:
                return "ADD_CONTACT";
            case READ_SMS:
                return "READ_SMS";
            case SEND_SMS:
                return "SEND_SMS";
            case OPEN_APP:
                return "OPEN_APP";
            case CLOSE_APP:
                return "CLOSE_APP";
            case BRIGHTNESS_UP:
                return "BRIGHTNESS_UP";
            case BRIGHTNESS_DOWN:
                return "BRIGHTNESS_DOWN";
            case VOLUME_UP:
                return "VOLUME_UP";
            case VOLUME_DOWN:
                return "VOLUME_DOWN";
            case AIRPLANE_MODE:
                return "AIRPLANE_MODE";
            case WIFI_TOGGLE:
                return "WIFI_TOGGLE";
            case BLUETOOTH_TOGGLE:
                return "BLUETOOTH_TOGGLE";
            case LOCATION_TOGGLE:
                return "LOCATION_TOGGLE";
            default:
                return "UNKNOWN";
        }
    }
}