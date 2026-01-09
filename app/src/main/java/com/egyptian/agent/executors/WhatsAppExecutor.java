package com.egyptian.agent.executors;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.ContactCache;
import com.egyptian.agent.utils.VibrationManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles WhatsApp message execution with Egyptian dialect support
 */
public class WhatsAppExecutor {
    private static final String TAG = "WhatsAppExecutor";
    private static final String WHATSAPP_PACKAGE = "com.whatsapp";
    private static final String WHATSAPP_CLASS = "com.whatsapp.Conversation";
    
    // Thread pool for contact lookups
    private static final ExecutorService contactLookupExecutor = Executors.newSingleThreadExecutor();

    /**
     * Handles a WhatsApp command from the user
     * @param context Application context
     * @param command The raw command from speech recognition
     */
    public static void handleCommand(Context context, String command) {
        Log.i(TAG, "Handling WhatsApp command: " + command);

        // Normalize Egyptian dialect
        String normalizedCommand = EgyptianNormalizer.normalize(command);
        Log.d(TAG, "Normalized command: " + normalizedCommand);

        // Extract recipient and message
        WhatsAppCommand parsedCommand = parseWhatsAppCommand(normalizedCommand);
        
        if (parsedCommand.recipient.isEmpty()) {
            TTSManager.speak(context, "من اللي عايز تبعتله الرسالة؟ قول اسم الشخص");
            return;
        }

        if (parsedCommand.message.isEmpty()) {
            TTSManager.speak(context, "قول الرسالة اللي عايز تبعتها");
            return;
        }

        // Look up contact number
        lookupContactNumber(context, parsedCommand.recipient, (number, error) -> {
            if (error != null) {
                Log.e(TAG, "Contact lookup error", error);
                TTSManager.speak(context, "حصل مشكلة في البحث عن " + parsedCommand.recipient);
            } else if (number == null) {
                handleContactNotFound(context, parsedCommand.recipient);
            } else {
                confirmAndSendMessage(context, parsedCommand.recipient, number, parsedCommand.message);
            }
        });
    }

    /**
     * Parses the WhatsApp command to extract recipient and message
     */
    private static WhatsAppCommand parseWhatsAppCommand(String command) {
        // Look for patterns like "send message to X saying Y"
        String[] parts = command.split(" saying | أن | إن | ب | قول | رسالة ");
        
        String recipient = "";
        String message = "";
        
        if (parts.length >= 2) {
            // Extract recipient from first part
            String firstPart = parts[0];
            recipient = extractRecipient(firstPart);
            
            // Extract message from remaining parts
            StringBuilder msgBuilder = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                if (msgBuilder.length() > 0) {
                    msgBuilder.append(" ");
                }
                msgBuilder.append(parts[i]);
            }
            message = msgBuilder.toString().trim();
        } else {
            // Alternative parsing for simpler commands
            recipient = extractRecipient(command);
            message = extractMessage(command, recipient);
        }
        
        return new WhatsAppCommand(recipient, message);
    }

    /**
     * Extracts recipient from command
     */
    private static String extractRecipient(String command) {
        // Look for patterns like "send message to X" or "message X"
        String[] patterns = {
            "send message to ",
            "send whatsapp to ",
            "whatsapp to ",
            "message to ",
            "tell ",
            "say to ",
            "to "
        };
        
        for (String pattern : patterns) {
            int index = command.indexOf(pattern);
            if (index != -1) {
                String remainder = command.substring(index + pattern.length()).trim();
                // Extract first word or phrase as recipient
                String[] words = remainder.split("\\s+");
                if (words.length > 0) {
                    // Take first 1-3 words as recipient name
                    StringBuilder name = new StringBuilder();
                    for (int i = 0; i < Math.min(3, words.length); i++) {
                        if (name.length() > 0) {
                            name.append(" ");
                        }
                        name.append(words[i]);
                        
                        // Stop if we hit a conjunction or preposition
                        if (words[i].equals("that") || words[i].equals("أن") || 
                            words[i].equals("إن") || words[i].equals(" saying ") ||
                            words[i].equals(" ب ")) {
                            break;
                        }
                    }
                    return name.toString().trim();
                }
            }
        }
        
        // If no pattern matched, try to extract based on Egyptian dialect
        return EgyptianNormalizer.extractContactName(command);
    }

    /**
     * Extracts message content from command
     */
    private static String extractMessage(String command, String recipient) {
        // Remove recipient from command to get message
        String message = command.replaceFirst("(?i)send message to " + recipient, "")
                                .replaceFirst("(?i)send whatsapp to " + recipient, "")
                                .replaceFirst("(?i)whatsapp to " + recipient, "")
                                .replaceFirst("(?i)message to " + recipient, "")
                                .replaceFirst("(?i)tell " + recipient, "")
                                .replaceFirst("(?i)say to " + recipient, "")
                                .replaceFirst("(?i)to " + recipient, "")
                                .trim();
        
        // Remove common connectors
        if (message.startsWith("that") || message.startsWith("أن") || 
            message.startsWith("إن") || message.startsWith(" saying ") ||
            message.startsWith(" ب ")) {
            message = message.substring(message.indexOf(' ') + 1).trim();
        }
        
        return message;
    }

    /**
     * Looks up contact number asynchronously
     */
    private static void lookupContactNumber(Context context, String contactName, ContactLookupCallback callback) {
        // First check cache (critical for performance on 6GB RAM devices)
        String cachedNumber = ContactCache.get(context, contactName);
        if (cachedNumber != null) {
            Log.d(TAG, "Contact found in cache: " + contactName);
            callback.onResult(cachedNumber, null);
            return;
        }

        // Use executor to avoid blocking main thread
        contactLookupExecutor.execute(() -> {
            String number = null;
            Exception error = null;

            try {
                // Look up the contact's phone number and then convert it to WhatsApp ID format
                number = searchContacts(context, contactName);
                if (number != null) {
                    // Format number for WhatsApp (remove leading zeros, add country code)
                    number = formatNumberForWhatsApp(number);
                    ContactCache.put(context, contactName, number);
                }
            } catch (Exception e) {
                Log.e(TAG, "Contact lookup error", e);
                error = e;
            }

            // Return to main thread
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                callback.onResult(number, error)
            );
        });
    }

    /**
     * Searches for contact in device contacts
     */
    private static String searchContacts(Context context, String partialName) {
        String[] projection = new String[] {
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        // More flexible search to handle Egyptian dialect variations
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ? OR " +
                          ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ? OR " +
                          ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ? OR " +
                          ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
        String[] selectionArgs = new String[] {
            "%" + partialName + "%",
            "%" + EgyptianNormalizer.normalize(partialName) + "%",
            "%" + partialName.toLowerCase() + "%",
            "%" + partialName.toUpperCase() + "%"
        };

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                if (numberIndex != -1) {
                    String foundNumber = cursor.getString(numberIndex);
                    String foundName = cursor.getString(nameIndex);

                    Log.d(TAG, "Found contact: " + foundName + " -> " + foundNumber);
                    return foundNumber;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching contacts", e);
            CrashLogger.logError(context, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    /**
     * Formats phone number for WhatsApp (removes leading zeros, adds country code)
     */
    private static String formatNumberForWhatsApp(String phoneNumber) {
        // Remove all non-digit characters
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        
        // Ensure it starts with country code (Egypt: +20)
        if (digitsOnly.length() >= 10 && !digitsOnly.startsWith("20")) {
            if (digitsOnly.startsWith("0")) {
                digitsOnly = "20" + digitsOnly.substring(1);
            } else {
                digitsOnly = "20" + digitsOnly;
            }
        }
        
        return digitsOnly;
    }

    /**
     * Confirms and sends the WhatsApp message
     */
    private static void confirmAndSendMessage(Context context, String recipient, String number, String message) {
        // Senior mode requires double confirmation
        if (SeniorMode.isEnabled()) {
            VibrationManager.vibrateShort(context);
            TTSManager.speak(context, "عايز تبعت رسالة لـ " + recipient + 
                " بعنوان \"" + message + "\"؟ قول 'نعم' بس، ولا 'لا'");
            
            // Wait for user confirmation using speech recognition
            SpeechConfirmation.waitForConfirmation(context, 10000, confirmed -> {
                if (confirmed) {
                    VibrationManager.vibrateLong(context);
                    sendMessage(context, number, message);
                    TTSManager.speak(context, "ببعت الرسالة لـ " + recipient + " دلوقتي");
                } else {
                    TTSManager.speak(context, "ما بعتتش رسالة. قول 'يا كبير' لو عايز تبعت رسالة تانية");
                }
            });
            
            return;
        }

        // Standard confirmation
        TTSManager.speak(context, "عايز تبعت رسالة لـ " + recipient + 
            " بعنوان \"" + message + "\"؟ قول 'نعم' أو 'لا'");
        
        // For this demo, we'll proceed directly
        sendMessage(context, number, message);
    }

    /**
     * Sends the actual WhatsApp message
     */
    private static void sendMessage(Context context, String number, String message) {
        // Check if WhatsApp is installed
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(WHATSAPP_PACKAGE, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "WhatsApp not installed");
            TTSManager.speak(context, "واتساب مش مثبت على الجهاز");
            return;
        }

        try {
            // Create WhatsApp intent
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage(WHATSAPP_PACKAGE);
            intent.putExtra(Intent.EXTRA_TEXT, message);
            
            // In newer versions of WhatsApp, we can't directly specify the recipient
            // So we'll open the app and let the user select the contact
            intent.putExtra("jid", number + "@s.whatsapp.net"); // WhatsApp ID format
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(intent);
            Log.i(TAG, "WhatsApp message sent to: " + number);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send WhatsApp message", e);
            TTSManager.speak(context, "حصل مشكلة في إرسال الرسالة. حاول تاني");
        }
    }

    /**
     * Handles case when contact is not found
     */
    private static void handleContactNotFound(Context context, String contactName) {
        TTSManager.speak(context, "مش لاقي " + contactName + " في جهات الاتصال. قول 'أضف' عشان تضيف الرقم الجديد");

        // Listen for add contact command
        SpeechConfirmation.waitForCommand(context, 15000, command -> {
            if (command.contains("أضف") || command.contains("اضيف") || command.contains("add")) {
                TTSManager.speak(context, "قول الرقم الجديد");
                SpeechConfirmation.waitForCommand(context, 30000, numberCommand -> {
                    String newNumber = extractNumber(numberCommand);
                    if (!newNumber.isEmpty()) {
                        // Add the new contact to the device
                        boolean success = addContactToPhone(context, contactName, newNumber);
                        if (success) {
                            TTSManager.speak(context, "تم حفظ الرقم الجديد لـ " + contactName);

                            // Format number for WhatsApp and send message
                            String formattedNumber = formatNumberForWhatsApp(newNumber);
                            sendMessage(context, formattedNumber, "تمت إضافة جهة الاتصال الجديدة");
                        } else {
                            TTSManager.speak(context, "حصل مشكلة في حفظ الرقم. حاول تاني");
                        }
                    } else {
                        TTSManager.speak(context, "متعرفش الرقم. قول 'يا كبير' وانا أساعدك");
                    }
                });
            } else {
                TTSManager.speak(context, "ما فهمتش. قول 'يا كبير' لو عايز تبعت رسالة لحد تاني");
            }
        });
    }

    /**
     * Adds a new contact to the phone
     */
    private static boolean addContactToPhone(Context context, String name, String number) {
        try {
            // Check for contacts permission
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
                TTSManager.speak(context, "محتاج إذن لحفظ جهات الاتصال");
                return false;
            }

            // Create a new contact
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

            // Name
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build());

            // Phone number
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
                .build());

            // Execute the operations
            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error adding contact", e);
            CrashLogger.logError(context, e);
            return false;
        }
    }

    /**
     * Data class to hold parsed WhatsApp command
     */
    private static class WhatsAppCommand {
        final String recipient;
        final String message;
        
        WhatsAppCommand(String recipient, String message) {
            this.recipient = recipient;
            this.message = message;
        }
    }

    /**
     * Callback interface for contact lookup
     */
    interface ContactLookupCallback {
        void onResult(String number, Exception error);
    }
}