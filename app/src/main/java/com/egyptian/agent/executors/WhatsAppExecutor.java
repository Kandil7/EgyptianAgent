package com.egyptian.agent.executors;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.stt.EgyptianNormalizer;

/**
 * Executor for WhatsApp-related commands
 * Handles sending WhatsApp messages based on voice commands
 */
public class WhatsAppExecutor {
    private static final String TAG = "WhatsAppExecutor";

    /**
     * Handles a WhatsApp command
     * @param context Context for the operation
     * @param command The command to process
     */
    public static void handleCommand(Context context, String command) {
        Log.d(TAG, "Handling WhatsApp command: " + command);

        try {
            // Extract recipient and message from command
            String[] parts = extractRecipientAndMessage(command);
            String recipient = parts[0];
            String message = parts[1];
            
            if (recipient.isEmpty()) {
                TTSManager.speak(context, "اسم الشخص مش واضح");
                return;
            }
            
            if (message.isEmpty()) {
                TTSManager.speak(context, "الرسالة مش واضحة");
                return;
            }

            // Normalize the recipient name
            String normalizedRecipient = EgyptianNormalizer.normalizeContactName(recipient);
            
            // Check if WhatsApp is installed
            if (!isWhatsAppInstalled(context)) {
                TTSManager.speak(context, "وتس أب مش مثبّت");
                return;
            }

            // Send WhatsApp message
            sendWhatsAppMessage(context, normalizedRecipient, message);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling WhatsApp command: " + command, e);
            TTSManager.speak(context, "حصل مشكلة في إرسال الرسالة");
        }
    }

    /**
     * Extracts recipient and message from command
     * @param command The command to extract from
     * @return Array with [recipient, message]
     */
    private static String[] extractRecipientAndMessage(String command) {
        String recipient = "";
        String message = "";
        
        // Look for message indicators
        String[] messageIndicators = {"أن", "إني", "إنه", "قول", "إنه"};
        
        for (String indicator : messageIndicators) {
            int index = command.indexOf(indicator);
            if (index != -1) {
                // Extract recipient before the indicator
                String beforeIndicator = command.substring(0, index).trim();
                recipient = extractContactName(beforeIndicator);
                
                // Extract message after the indicator
                message = command.substring(index + indicator.length()).trim();
                break;
            }
        }
        
        // If no indicator found, try to extract recipient from the whole command
        if (recipient.isEmpty()) {
            recipient = extractContactName(command);
        }
        
        return new String[]{recipient, message};
    }

    /**
     * Extracts contact name from command
     * @param command The command to extract from
     * @return The extracted contact name
     */
    private static String extractContactName(String command) {
        // this would use more sophisticated NLP techniques
        String[] keywords = {"لـ", "لى", "مع", "إلى"};
        
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
                    
                    return contact;
                }
            }
        }
        
        return "";
    }

    /**
     * Checks if WhatsApp is installed
     * @param context Context for the operation
     * @return true if WhatsApp is installed, false otherwise
     */
    private static boolean isWhatsAppInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Sends a WhatsApp message
     * @param context Context for the operation
     * @param recipient The recipient name
     * @param message The message to send
     */
    private static void sendWhatsAppMessage(Context context, String recipient, String message) {
        try {
            // First, try to get the contact's phone number from the contacts
            String phoneNumber = getPhoneNumberForContact(context, recipient);

            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                // Use the direct WhatsApp API with phone number
                String url = "https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + Uri.encode(message);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                intent.setPackage("com.whatsapp");

                // Add flags to ensure it works from background service
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);

                    Log.d(TAG, "Sending WhatsApp message to: " + recipient + " (" + phoneNumber + "), message: " + message);
                    TTSManager.speak(context, "ببعت رسالة لـ " + recipient);
                } else {
                    // If WhatsApp isn't installed or can't be opened, try alternative
                    TTSManager.speak(context, "مقدرش أفتح وتس أب");
                }
            } else {
                // If we can't find the phone number, try to open WhatsApp Web with just the message
                String url = "https://api.whatsapp.com/send?text=" + Uri.encode(message);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                intent.setPackage("com.whatsapp");

                // Add flags to ensure it works from background service
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);

                    Log.d(TAG, "Sending WhatsApp message to: " + recipient + ", message: " + message);
                    TTSManager.speak(context, "ببعت رسالة لـ " + recipient);
                } else {
                    // If WhatsApp isn't installed or can't be opened, try alternative
                    TTSManager.speak(context, "مقدرش أفتح وتس أب");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending WhatsApp message to: " + recipient, e);
            TTSManager.speak(context, "مقدرش أبعت الرسالة");
        }
    }

    /**
     * Gets the phone number for a contact name
     * @param context Context for the operation
     * @param contactName The name of the contact
     * @return The phone number, or null if not found
     */
    private static String getPhoneNumberForContact(Context context, String contactName) {
        // First, try to get the contact's phone number from the contacts
        try {
            // Query the contacts provider for the contact name
            android.database.Cursor cursor = context.getContentResolver().query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                    android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                },
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + contactName + "%"},
                null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(
                    android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                );

                String phoneNumber = cursor.getString(numberIndex);

                // Clean the phone number (remove spaces, parentheses, hyphens, etc.)
                if (phoneNumber != null) {
                    phoneNumber = phoneNumber.replaceAll("[^\\d+]", "");

                    // If the number doesn't start with +, prepend the country code for Egypt (+20)
                    if (!phoneNumber.startsWith("+") && phoneNumber.length() > 0) {
                        if (phoneNumber.startsWith("0")) {
                            phoneNumber = "+20" + phoneNumber.substring(1);
                        } else {
                            phoneNumber = "+20" + phoneNumber;
                        }
                    }
                }

                cursor.close();
                return phoneNumber;
            }

            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting phone number for contact: " + contactName, e);
        }

        return null; // Contact not found or error occurred
    }
}