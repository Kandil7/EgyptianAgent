package com.egyptian.agent.executors;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;

/**
 * Executor for SMS-related commands
 */
public class SmsExecutor {
    private static final String TAG = "SmsExecutor";

    public static void handleReadSmsCommand(Context context) {
        Log.i(TAG, "Handling read SMS command");

        try {
            // Query for recent SMS messages
            Uri uri = Telephony.Sms.Inbox.CONTENT_URI;
            String[] projection = {Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.PERSON};
            String selection = null;
            String[] selectionArgs = null;
            String sortOrder = Telephony.Sms.DEFAULT_SORT_ORDER + " LIMIT 5";

            Cursor cursor = context.getContentResolver().query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            );

            if (cursor != null && cursor.getCount() > 0) {
                StringBuilder smsSummary = new StringBuilder("الرسائل: ");
                int count = 0;

                while (cursor.moveToNext() && count < 3) { // Read first 3 messages
                    String address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));

                    // Limit body length to avoid long readings
                    if (body != null && body.length() > 50) {
                        body = body.substring(0, 50) + "...";
                    }

                    smsSummary.append("من ").append(address).append(": ").append(body);
                    if (cursor.moveToNext()) {
                        smsSummary.append(". ");
                        cursor.moveToPrevious(); // Move back to continue the loop properly
                    }

                    count++;
                }

                cursor.close();

                if (count > 0) {
                    TTSManager.speak(context, smsSummary.toString());
                } else {
                    TTSManager.speak(context, "مافيش رسائل");
                }
            } else {
                TTSManager.speak(context, "مافيش رسائل");
            }

            if (cursor != null) {
                cursor.close();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for reading SMS", e);
            TTSManager.speak(context, "مافيش إذن لقراءة الرسائل");
        } catch (Exception e) {
            Log.e(TAG, "Error reading SMS", e);
            TTSManager.speak(context, "مقدرش أقرا الرسائل");
        }
    }

    public static void handleSendSmsCommand(Context context, String command) {
        Log.i(TAG, "Handling send SMS command: " + command);

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

        // In a real implementation, this would send an SMS
        // For now, we'll just acknowledge the command
        TTSManager.speak(context, "ببعت رسالة لـ " + recipient + " تقول " + message);
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
        String[] messageIndicators = {"أن", "إني", "إنه", "قول", "إنه", "رسالة"};

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
        // Look for keywords that indicate a recipient
        String[] keywords = {"لـ", "لى", "إلى", "ل"};

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
}