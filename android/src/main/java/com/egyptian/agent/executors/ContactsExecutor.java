package com.egyptian.agent.executors;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Executor for reading contacts
 */
public class ContactsExecutor {
    private static final String TAG = "ContactsExecutor";

    public static void handleCommand(Context context) {
        Log.i(TAG, "Handling contacts command");

        try {
            List<String> contactNames = getContactNames(context);
            
            if (contactNames.isEmpty()) {
                TTSManager.speak(context, "مافيش جهات اتصال");
                return;
            }
            
            // Limit to first 10 contacts to avoid overwhelming the user
            int limit = Math.min(contactNames.size(), 10);
            StringBuilder contactsList = new StringBuilder("جهات الاتصال: ");
            
            for (int i = 0; i < limit; i++) {
                contactsList.append(contactNames.get(i));
                if (i < limit - 1) {
                    contactsList.append(", ");
                }
            }
            
            if (contactNames.size() > limit) {
                contactsList.append(" و ").append(contactNames.size() - limit).append(" تانية");
            }
            
            TTSManager.speak(context, contactsList.toString());
            
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for reading contacts", e);
            TTSManager.speak(context, "مافيش إذن لقراءة جهات الاتصال");
        } catch (Exception e) {
            Log.e(TAG, "Error reading contacts", e);
            TTSManager.speak(context, "مقدرش أقرا جهات الاتصال");
        }
    }

    public static void addContact(Context context, String name, String number) {
        Log.i(TAG, "Adding contact: " + name + " with number: " + number);

        // In a real implementation, this would use Android's ContactsContract API
        // to add a new contact to the device
        TTSManager.speak(context, "إضافة جهة اتصال " + name + " قيد التطوير");
    }

    private static List<String> getContactNames(Context context) {
        List<String> contactNames = new ArrayList<>();
        
        try {
            Cursor cursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
            );

            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME
                    ));
                    
                    if (name != null && !name.isEmpty()) {
                        contactNames.add(name);
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact names", e);
        }

        return contactNames;
    }
}