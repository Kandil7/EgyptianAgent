package com.egyptian.agent.executors;

import android.Manifest;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.ContactCache;
import com.egyptian.agent.utils.CrashLogger;
import com.egyptian.agent.utils.VibrationManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles phone call execution with Egyptian dialect support
 */
public class CallExecutor {
    private static final String TAG = "CallExecutor";
    
    // Predefined emergency contacts
    private static final Map<String, String> EMERGENCY_CONTACTS = new HashMap<>();
    
    // Thread pool for contact lookups
    private static final ExecutorService contactLookupExecutor = Executors.newSingleThreadExecutor();

    static {
        EMERGENCY_CONTACTS.put("ngda", "123");      // Emergency
        EMERGENCY_CONTACTS.put("escaf", "123");     // Ambulance
        EMERGENCY_CONTACTS.put("police", "122");    // Police
        EMERGENCY_CONTACTS.put("fire", "180");      // Fire department
    }

    /**
     * Handles a call command from the user
     * @param context Application context
     * @param command The raw command from speech recognition
     */
    public static void handleCommand(Context context, String command) {
        Log.i(TAG, "Handling call command: " + command);

        // Normalize Egyptian dialect
        String normalizedCommand = EgyptianNormalizer.normalize(command);
        Log.d(TAG, "Normalized command: " + normalizedCommand);

        // Extract contact name
        String contactName = extractContactName(normalizedCommand);
        Log.i(TAG, "Extracted contact name: " + contactName);

        if (contactName.isEmpty()) {
            TTSManager.speak(context, "مين اللي عايز تتصل بيه؟ قول الاسم");
            return;
        }

        // Check for emergency contacts
        if (isEmergencyContact(contactName)) {
            handleEmergencyCall(context, contactName);
            return;
        }

        // Look up contact number
        lookupContactNumber(context, contactName, (number, error) -> {
            if (error != null) {
                Log.e(TAG, "Contact lookup error", error);
                TTSManager.speak(context, "حصل مشكلة في البحث عن " + contactName);
            } else if (number == null) {
                handleContactNotFound(context, contactName);
            } else {
                confirmAndPlaceCall(context, contactName, number);
            }
        });
    }

    /**
     * Extracts contact name from command
     */
    private static String extractContactName(String command) {
        // Advanced Egyptian dialect handling
        return command.replaceAll(".*(اتصل|كلم|رن|بعت)\\s+(?:على|مع|لـ)?\\s*", "")
                .replaceAll("دلوقتي|حالا|من فضلك|فضلك|يا كبير|يا صاحبي", "")
                .trim();
    }

    /**
     * Checks if the contact is an emergency contact
     */
    private static boolean isEmergencyContact(String contactName) {
        return EMERGENCY_CONTACTS.containsKey(contactName.toLowerCase());
    }

    /**
     * Handles emergency call
     */
    private static void handleEmergencyCall(Context context, String contactName) {
        String emergencyNumber = EMERGENCY_CONTACTS.get(contactName.toLowerCase());

        // Senior mode special handling - no confirmation needed for emergencies
        if (SeniorMode.isEnabled()) {
            placeCall(context, emergencyNumber);
            TTSManager.speak(context, "بتصل بـ " + contactName + " دلوقتي");
            return;
        }

        // Standard confirmation flow
        TTSManager.speak(context, "عايز تتصل بـ " + contactName + "؟ قول 'نعم' أو 'لا'");
        // Wait for user confirmation using speech recognition
        SpeechConfirmation.waitForConfirmation(context, confirmed -> {
            if (confirmed) {
                placeCall(context, emergencyNumber);
                TTSManager.speak(context, "بتصل بـ " + contactName + " دلوقتي");
            } else {
                TTSManager.speak(context, "ما عملتش اتصال");
            }
        });
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
                number = searchContacts(context, contactName);
                if (number != null) {
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
     * Extracts phone number from voice command
     */
    private static String extractNumber(String command) {
        if (command == null) return "";

        // Remove non-digits to extract phone number
        return command.replaceAll("[^0-9+]", "");
    }

    /**
     * Confirms and places the call
     */
    private static void confirmAndPlaceCall(Context context, String contactName, String number) {
        // Senior mode requires double confirmation
        if (SeniorMode.isEnabled()) {
            VibrationManager.vibrateShort(context);
            TTSManager.speak(context, "عايز تتصل بـ " + contactName + " على الرقم " + 
                formatNumberForSpeech(number) + "؟ قول 'نعم' بس، ولا 'لا'");
            
            // Wait for user confirmation using speech recognition
            SpeechConfirmation.waitForConfirmation(context, 10000, confirmed -> {
                if (confirmed) {
                    VibrationManager.vibrateLong(context);
                    placeCall(context, number);
                    TTSManager.speak(context, "اتصلت بـ " + contactName + ". هو بيرد عليكم دلوقتي");
                } else {
                    TTSManager.speak(context, "ما عملتش اتصال. قول 'يا كبير' لو عايز حاجة تانية");
                }
            });
            
            return;
        }

        // Standard confirmation
        TTSManager.speak(context, "عايز تتصل بـ " + contactName + "؟ قول 'نعم' أو 'لا'");
        
        // For this demo, we'll proceed directly
        placeCall(context, number);
    }

    /**
     * Places the actual phone call
     */
    private static void placeCall(Context context, String number) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing CALL_PHONE permission");
            TTSManager.speak(context, "محتاج إذن لإجراء المكالمات");
            return;
        }

        try {
            String cleanNumber = number.replaceAll("[^0-9+]", "");
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + cleanNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(callIntent);
            Log.i(TAG, "Call placed to: " + cleanNumber);
        } catch (Exception e) {
            Log.e(TAG, "Call failed", e);
            TTSManager.speak(context, "حصل مشكلة في إجراء المكالمة. حاول تاني");
        }
    }

    /**
     * Formats number for speech output
     */
    private static String formatNumberForSpeech(String number) {
        StringBuilder formatted = new StringBuilder();
        for (char c : number.toCharArray()) {
            if (Character.isDigit(c)) {
                formatted.append(c).append(" ");
            }
        }
        return formatted.toString().trim();
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
                            // Now place the call
                            placeCall(context, newNumber);
                        } else {
                            TTSManager.speak(context, "حصل مشكلة في حفظ الرقم. حاول تاني");
                        }
                    } else {
                        TTSManager.speak(context, "متعرفش الرقم. قول 'يا كبير' وانا أساعدك");
                    }
                });
            } else {
                TTSManager.speak(context, "ما فهمتش. قول 'يا كبير' لو عايز تكلم حد تاني");
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
     * Callback interface for contact lookup
     */
    interface ContactLookupCallback {
        void onResult(String number, Exception error);
    }
}