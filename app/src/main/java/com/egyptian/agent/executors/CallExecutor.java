package com.egyptian.agent.executors;

import android.content.Context;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.stt.EgyptianNormalizer;

/**
 * Executor for call-related commands
 * Handles making phone calls based on voice commands
 */
public class CallExecutor {
    private static final String TAG = "CallExecutor";

    /**
     * Handles a call command
     * @param context Context for the operation
     * @param command The command to process
     */
    public static void handleCommand(Context context, String command) {
        Log.d(TAG, "Handling call command: " + command);

        try {
            // Extract contact name from command
            String contactName = extractContactName(command);
            if (contactName.isEmpty()) {
                TTSManager.speak(context, "اسم الشخص مش واضح");
                return;
            }

            // Normalize the contact name
            String normalizedContact = EgyptianNormalizer.normalizeContactName(contactName);
            
            // Get phone number for the contact
            String phoneNumber = getPhoneNumberForContact(context, normalizedContact);
            
            if (phoneNumber.isEmpty()) {
                TTSManager.speak(context, "ملاقيش رقم " + normalizedContact + " في جهات الاتصال");
                return;
            }

            // Check for call permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
                != PackageManager.PERMISSION_GRANTED) {
                
                // Request permission or inform user
                TTSManager.speak(context, "التطبيق محتاج إذن الاتصال");
                
                // Redirect to settings if needed
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                context.startActivity(intent);
                return;
            }

            // Make the call
            makeCall(context, phoneNumber);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling call command: " + command, e);
            TTSManager.speak(context, "حصل مشكلة في الاتصال");
        }
    }

    /**
     * Extracts contact name from command
     * @param command The command to extract from
     * @return The extracted contact name
     */
    private static String extractContactName(String command) {
        // this would use more sophisticated NLP techniques
        String[] keywords = {"ب", "على", "لـ", "لى", "مع"};
        
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
     * Gets phone number for a contact
     * @param context Context for the operation
     * @param contactName The contact name to look up
     * @return The phone number, or empty string if not found
     */
    private static String getPhoneNumberForContact(Context context, String contactName) {
        try {
            // Query contacts for the given name
            android.database.Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                },
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + contactName + "%"},
                null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String phoneNumber = cursor.getString(numberIndex);
                cursor.close();
                
                // Clean the phone number
                if (phoneNumber != null) {
                    return phoneNumber.replaceAll("[^0-9+]", "");
                }
            }
            
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting phone number for contact: " + contactName, e);
        }
        
        return "";
    }

    /**
     * Makes a phone call
     * @param context Context for the operation
     * @param phoneNumber The phone number to call
     */
    private static void makeCall(Context context, String phoneNumber) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            
            // Add flags to ensure the call works from background service
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(callIntent);
            
            Log.d(TAG, "Initiating call to: " + phoneNumber);
            TTSManager.speak(context, "بتصل على " + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "Error making call to: " + phoneNumber, e);
            TTSManager.speak(context, "مقدرش أعمل الاتصال");
        }
    }
}