package com.egyptian.agent.core;

import android.util.Log;
import java.util.regex.Pattern;

public class IntentRouter {

    private static final String TAG = "IntentRouter";

    public static IntentType detectIntent(String command) {
        if (command == null || command.trim().isEmpty()) {
            return IntentType.UNKNOWN;
        }

        String normalizedCommand = command.toLowerCase();

        Log.d(TAG, "Detecting intent for command: " + normalizedCommand);

        // Check for emergency keywords first
        if (isEmergencyCommand(normalizedCommand)) {
            return IntentType.EMERGENCY;
        }

        // Check for call commands
        if (isCallCommand(normalizedCommand)) {
            return IntentType.CALL_CONTACT;
        }

        // Check for WhatsApp commands
        if (isWhatsAppCommand(normalizedCommand)) {
            return IntentType.SEND_WHATSAPP;
        }

        // Check for alarm/set reminder commands
        if (isAlarmCommand(normalizedCommand)) {
            return IntentType.SET_ALARM;
        }

        // Check for missed calls
        if (isMissedCallsCommand(normalizedCommand)) {
            return IntentType.READ_MISSED_CALLS;
        }

        // Check for time commands
        if (isTimeCommand(normalizedCommand)) {
            return IntentType.READ_TIME;
        }

        // Default to unknown if no pattern matches
        return IntentType.UNKNOWN;
    }

    private static boolean isEmergencyCommand(String command) {
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

    private static boolean isCallCommand(String command) {
        return command.contains("اتصل") ||
               command.contains("كلم") ||
               command.contains("رن") ||
               command.contains("شيل") ||
               command.contains("اتكلم");
    }

    private static boolean isWhatsAppCommand(String command) {
        return command.contains("واتساب") ||
               command.contains("ابعت") ||
               command.contains("رساله") ||
               command.contains("رسالة");
    }

    private static boolean isAlarmCommand(String command) {
        return command.contains("نبهني") ||
               command.contains("انبهني") ||
               command.contains("ذكرني") ||
               command.contains("منبه") ||
               command.contains("تذكير");
    }

    private static boolean isMissedCallsCommand(String command) {
        return command.contains("فايتة") ||
               command.contains("فايتات") ||
               command.contains("المكالمات") ||
               command.contains("شوفلي");
    }

    private static boolean isTimeCommand(String command) {
        return command.contains("الساعه") ||
               command.contains("الساعة") ||
               command.contains("الوقت") ||
               command.contains("كام");
    }
}