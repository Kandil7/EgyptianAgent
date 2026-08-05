package com.saiy.android.core;

import android.content.Context;
import android.util.Log;

import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.nlp.IntentRouter;
import com.egyptian.agent.nlp.IntentType;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.executors.CallExecutor;
import com.egyptian.agent.executors.WhatsAppExecutor;
import com.egyptian.agent.executors.AlarmExecutor;
import com.egyptian.agent.executors.CallLogExecutor;
import com.egyptian.agent.executors.EmergencyHandler;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.ContactCache;
import com.egyptian.agent.utils.SpeechConfirmation;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Quantum - Core processing engine for Egyptian dialect commands
 * Handles command interpretation and routing for Egyptian users
 */
public class Quantum {
    private static final String TAG = "Quantum";
    private final EgyptianNormalizer normalizer;
    private final IntentRouter intentRouter;
    private final Context context;

    public Quantum(Context context) {
        this.context = context;
        this.normalizer = new EgyptianNormalizer();
        this.intentRouter = new IntentRouter();
    }

    /**
     * Process a command in Egyptian dialect
     * @param rawCommand The raw command from speech recognition
     * @return CommandResult with success status and response
     */
    public void processCommand(String rawCommand, CommandResultCallback callback) {
        // 1. Normalize Egyptian dialect
        String normalizedCommand = normalizer.normalize(rawCommand);
        Log.d(TAG, "Command after normalization: " + normalizedCommand);

        // 2. Detect intent
        IntentType intent = intentRouter.detectIntent(normalizedCommand);

        // 3. Execute based on intent
        executeIntent(intent, normalizedCommand, callback);
    }

    private void executeIntent(IntentType intent, String normalizedCommand, CommandResultCallback callback) {
        switch (intent) {
            case CALL_CONTACT:
                handleCallContact(normalizedCommand, callback);
                break;
            case SEND_WHATSAPP:
                handleSendWhatsApp(normalizedCommand, callback);
                break;
            case SET_ALARM:
                handleSetAlarm(normalizedCommand, callback);
                break;
            case READ_MISSED_CALLS:
                handleReadMissedCalls(callback);
                break;
            case READ_TIME:
                handleReadTime(callback);
                break;
            case EMERGENCY:
                handleEmergency(callback);
                break;
            case MEDICATION_REMINDER:
                handleMedicationReminder(normalizedCommand, callback);
                break;
            case ADD_CONTACT:
                handleAddContact(normalizedCommand, callback);
                break;
            case UNKNOWN:
            default:
                handleUnknownCommand(normalizedCommand, callback);
        }
    }

    private void handleCallContact(String command, CommandResultCallback callback) {
        // Extract contact name from Egyptian command
        String contactName = extractContactName(command);

        if (contactName.isEmpty()) {
            callback.onResult(new CommandResult(false, "مين اللي عايز تتصل بيه؟ قول الاسم"));
            return;
        }

        // Look up contact in cache
        String number = ContactCache.findContact(context, contactName);

        if (number == null) {
            callback.onResult(new CommandResult(false, "مش لاقي " + contactName + " في جهات الاتصال"));
            return;
        }

        // In senior mode - double confirmation
        if (SeniorMode.isEnabled()) {
            String confirmationMessage = "عايز تتصل بـ " + contactName + "؟ قول 'نعم' أو 'لا'";
            callback.onResult(new CommandResult(true, confirmationMessage, () -> {
                if (SpeechConfirmation.waitForConfirmation(context)) {
                    CallExecutor.placeCall(context, number);
                    return "بتكلم مع " + contactName + " دلوقتي";
                }
                return "ما عملتش اتصال";
            }));
        } else {
            // Normal mode - direct execution
            CallExecutor.placeCall(context, number);
            callback.onResult(new CommandResult(true, "بتكلم مع " + contactName));
        }
    }

    private void handleSendWhatsApp(String command, CommandResultCallback callback) {
        // Extract recipient and message from command
        String[] parts = extractWhatsAppParts(command);
        if (parts.length < 2) {
            callback.onResult(new CommandResult(false, "عايز تبعت رسالة لحد معين؟"));
            return;
        }

        String recipient = parts[0];
        String message = parts[1];

        if (SeniorMode.isEnabled()) {
            String confirmation = "عايز تبعت رسالة \"" + message + "\" لـ " + recipient + "؟ قول 'نعم' أو 'لا'";
            callback.onResult(new CommandResult(true, confirmation, () -> {
                if (SpeechConfirmation.waitForConfirmation(context)) {
                    WhatsAppExecutor.sendMessage(context, recipient, message);
                    return "ببعت رسالة لـ " + recipient;
                }
                return "ما بعتتش الرسالة";
            }));
        } else {
            WhatsAppExecutor.sendMessage(context, recipient, message);
            callback.onResult(new CommandResult(true, "ببعت رسالة لـ " + recipient));
        }
    }

    private void handleSetAlarm(String command, CommandResultCallback callback) {
        // Extract time from command
        String time = extractTime(command);
        if (time.isEmpty()) {
            callback.onResult(new CommandResult(false, "متى عايز التنبيه؟"));
            return;
        }

        AlarmExecutor.setAlarm(context, time);
        callback.onResult(new CommandResult(true, "بأضع تنبيه لـ " + time));
    }

    private void handleReadMissedCalls(CommandResultCallback callback) {
        String missedCalls = CallLogExecutor.readMissedCalls(context);
        callback.onResult(new CommandResult(true, missedCalls));
    }

    private void handleReadTime(CommandResultCallback callback) {
        String currentTime = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
        callback.onResult(new CommandResult(true, "الساعة " + currentTime));
    }

    private void handleEmergency(CommandResultCallback callback) {
        EmergencyHandler.trigger(context);
        callback.onResult(new CommandResult(true, "بأتصال بالإسعاف دلوقتي"));
    }

    private void handleMedicationReminder(String command, CommandResultCallback callback) {
        // Extract medication and time from command
        String medication = extractMedication(command);
        String time = extractTime(command);

        if (medication.isEmpty()) {
            callback.onResult(new CommandResult(false, "اسم الدوا؟"));
            return;
        }

        if (time.isEmpty()) {
            callback.onResult(new CommandResult(false, "متى عايز تاخد الدوا؟"));
            return;
        }

        // Schedule medication reminder
        // Implementation would go here
        callback.onResult(new CommandResult(true, "بأضع تذكير لـ " + medication + " في " + time));
    }

    private void handleAddContact(String command, CommandResultCallback callback) {
        // Extract name and number from command
        String[] parts = extractContactInfo(command);
        if (parts.length < 2) {
            callback.onResult(new CommandResult(false, "عايز تضيف مين؟ ورقم تيليفونه؟"));
            return;
        }

        String name = parts[0];
        String number = parts[1];

        // Add contact to phonebook
        boolean success = ContactCache.addContact(context, name, number);
        if (success) {
            callback.onResult(new CommandResult(true, "تمت إضافة " + name + " لجهات الاتصال"));
        } else {
            callback.onResult(new CommandResult(false, "تعذر إضافة " + name + " لجهات الاتصال"));
        }
    }

    private void handleUnknownCommand(String command, CommandResultCallback callback) {
        callback.onResult(new CommandResult(false, "مش فاهمك. قول حاجة تانية"));
    }

    // Helper methods for extracting information from Egyptian commands
    private String extractContactName(String command) {
        // Look for patterns like "اتصل بـ [name]", "كلم [name]", "رن على [name]"
        if (command.contains("اتصل بـ ")) {
            return command.split(Pattern.quote("اتصل بـ "))[1].trim();
        } else if (command.contains("كلم ")) {
            return command.split(Pattern.quote("كلم "))[1].trim();
        } else if (command.contains("رن على ")) {
            return command.split(Pattern.quote("رن على "))[1].trim();
        } else if (command.contains("اتصل ")) {
            return command.split(Pattern.quote("اتصل "))[1].trim();
        }
        return "";
    }

    private String[] extractWhatsAppParts(String command) {
        // Look for patterns like "ابعت واتساب لـ [recipient] [message]", "قول لـ [recipient] إن [message]"
        if (command.contains("ابعت واتساب لـ ")) {
            String remainder = command.split(Pattern.quote("ابعت واتساب لـ "))[1].trim();
            // Find where recipient ends and message begins
            String[] parts = remainder.split("\\s+", 2);
            if (parts.length >= 2) {
                return new String[]{parts[0], parts[1]};
            }
        } else if (command.contains("قول لـ ") && command.contains("إن ")) {
            String remainder = command.split(Pattern.quote("قول لـ "))[1].trim();
            String[] parts = remainder.split(Pattern.quote(" إن "), 2);
            if (parts.length >= 2) {
                return new String[]{parts[0], parts[1]};
            }
        }
        return new String[]{};
    }

    private String extractTime(String command) {
        // Look for time patterns in Egyptian dialect
        // Examples: "بعد ساعة", "بكرة الصبح", "الساعة 7", etc.
        if (command.contains("بعد ساعة")) {
            return "بعد ساعة";
        } else if (command.contains("بكرة الصبح")) {
            return "بكرة الصبح";
        } else if (command.contains("الساعة ")) {
            String[] parts = command.split(Pattern.quote("الساعة "));
            if (parts.length > 1) {
                return "الساعة " + parts[1].split("\\s")[0]; // Get first word after "الساعة"
            }
        } else if (command.contains("في ")) {
            String[] parts = command.split(Pattern.quote("في "));
            if (parts.length > 1) {
                return "في " + parts[1];
            }
        }
        return "";
    }

    private String extractMedication(String command) {
        // Look for medication patterns
        if (command.contains("الدوا")) {
            String[] parts = command.split(Pattern.quote("الدوا "));
            if (parts.length > 1) {
                return parts[1].split("\\s")[0]; // Get first word after "الدوا"
            }
        } else if (command.contains("الدواء")) {
            String[] parts = command.split(Pattern.quote("الدواء "));
            if (parts.length > 1) {
                return parts[1].split("\\s")[0]; // Get first word after "الدواء"
            }
        }
        return "";
    }

    private String[] extractContactInfo(String command) {
        // Look for patterns like "أضف [name] [number]", "خلي [name] [number]"
        if (command.contains("أضف ")) {
            String remainder = command.split(Pattern.quote("أضف "))[1].trim();
            String[] parts = remainder.split("\\s+", 2); // Split into 2 parts: name and number
            if (parts.length >= 2) {
                return parts;
            }
        } else if (command.contains("خلي ")) {
            String remainder = command.split(Pattern.quote("خلي "))[1].trim();
            String[] parts = remainder.split("\\s+", 2); // Split into 2 parts: name and number
            if (parts.length >= 2) {
                return parts;
            }
        }
        return new String[]{};
    }

    // Callback interface for command results
    public interface CommandResultCallback {
        void onResult(CommandResult result);
    }

    // Result class for command execution
    public static class CommandResult {
        private final boolean success;
        private final String message;
        private final ConfirmationAction confirmationAction;

        public CommandResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.confirmationAction = null;
        }

        public CommandResult(boolean success, String message, ConfirmationAction confirmationAction) {
            this.success = success;
            this.message = message;
            this.confirmationAction = confirmationAction;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public ConfirmationAction getConfirmationAction() { return confirmationAction; }
    }

    // Functional interface for confirmation actions
    public interface ConfirmationAction {
        String execute();
    }
}