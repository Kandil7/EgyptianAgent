package com.egyptian.agent.core;

import android.content.Context;
import android.util.Log;
import com.egyptian.agent.executors.CallExecutor;
import com.egyptian.agent.executors.WhatsAppExecutor;
import com.egyptian.agent.executors.AlarmExecutor;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.CrashLogger;

/**
 * Quantum class for advanced command processing
 * Handles complex command patterns and fallback processing
 */
public class Quantum {
    private static final String TAG = "Quantum";
    private Context context;
    private String lastContact = "";

    public Quantum(Context context) {
        this.context = context;
    }

    /**
     * Processes a command using advanced pattern matching and fallback mechanisms
     * @param command The command to process
     */
    public void processCommand(String command) {
        Log.d(TAG, "Processing command: " + command);
        
        try {
            // Normalize the command using Egyptian dialect processing
            String normalizedCommand = EgyptianNormalizer.normalize(command);
            
            // Try different processing strategies
            if (processCallCommand(normalizedCommand)) {
                return;
            }
            
            if (processWhatsAppCommand(normalizedCommand)) {
                return;
            }
            
            if (processAlarmCommand(normalizedCommand)) {
                return;
            }
            
            // If no specific command matched, try general processing
            processGeneralCommand(normalizedCommand);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing command: " + command, e);
            CrashLogger.logError(context, e);
        }
    }

    /**
     * Processes call-related commands
     * @param command The normalized command
     * @return true if command was processed, false otherwise
     */
    private boolean processCallCommand(String command) {
        // Check for call-related keywords
        if (command.contains("اتصل") || command.contains("كلم") || command.contains("رن على")) {
            // Extract contact name
            String contactName = extractContactName(command);
            if (!contactName.isEmpty()) {
                lastContact = contactName;
                CallExecutor.handleCommand(context, command);
                TTSManager.speak(context, "بتتصل بـ " + contactName);
                return true;
            }
        }
        return false;
    }

    /**
     * Processes WhatsApp-related commands
     * @param command The normalized command
     * @return true if command was processed, false otherwise
     */
    private boolean processWhatsAppCommand(String command) {
        // Check for WhatsApp-related keywords
        if (command.contains("واتساب") || command.contains("رسالة") || command.contains("ابعت")) {
            // Extract recipient and message
            String recipient = extractContactName(command);
            String message = extractMessage(command);
            
            if (!recipient.isEmpty()) {
                lastContact = recipient;
                WhatsAppExecutor.handleCommand(context, command);
                TTSManager.speak(context, "ببعت رسالة لـ " + recipient);
                return true;
            }
        }
        return false;
    }

    /**
     * Processes alarm-related commands
     * @param command The normalized command
     * @return true if command was processed, false otherwise
     */
    private boolean processAlarmCommand(String command) {
        // Check for alarm-related keywords
        if (command.contains("نبهني") || command.contains("ذكرني") || command.contains("المنبه")) {
            AlarmExecutor.handleCommand(context, command);
            TTSManager.speak(context, "تم ضبط المنبه");
            return true;
        }
        return false;
    }

    /**
     * Processes general commands
     * @param command The normalized command
     */
    private void processGeneralCommand(String command) {
        // Handle general commands that don't fit specific categories
        if (command.contains("الوقت") || command.contains("الساعة")) {
            String currentTime = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
            TTSManager.speak(context, "الساعة " + currentTime);
            return;
        }
        
        if (command.contains("المكالمات") || command.contains("الفايتة")) {
            // Read missed calls
            TTSManager.speak(context, "بتشوف المكالمات الفايتة");
            return;
        }
        
        // If no pattern matched, inform user
        TTSManager.speak(context, "مش فاهمك. قول الأمر تاني");
    }

    /**
     * Extracts contact name from command
     * @param command The command to extract from
     * @return The extracted contact name, or empty string if not found
     */
    private String extractContactName(String command) {
        // Use more sophisticated NLP techniques
        String[] keywords = {"ب", "على", "لـ", "لى"};

        for (String keyword : keywords) {
            int index = command.indexOf(keyword);
            if (index != -1) {
                String afterKeyword = command.substring(index + keyword.length()).trim();

                // Extract the first word after the keyword
                String[] words = afterKeyword.split("\\s+");
                if (words.length > 0) {
                    String contact = words[0];

                    // Remove punctuation
                    contact = contact.replaceAll("[^\\p{L}\\p{N}\\s]", "");

                    // Common Egyptian names normalization
                    return EgyptianNormalizer.normalizeContactName(contact);
                }
            }
        }

        return "";
    }

    /**
     * Extracts message content from command
     * @param command The command to extract from
     * @return The extracted message, or empty string if not found
     */
    private String extractMessage(String command) {
        // Look for message indicators
        String[] messageIndicators = {"أن", "إني", "إنه", "قول"};
        
        for (String indicator : messageIndicators) {
            int index = command.indexOf(indicator);
            if (index != -1) {
                return command.substring(index + indicator.length()).trim();
            }
        }
        
        return "";
    }

    /**
     * Gets the last processed contact name
     * @return The last contact name, or empty string if none
     */
    public String getLastContact() {
        return lastContact;
    }
}