package com.egyptian.agent.executors;

import android.Manifest;
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
import com.egyptian.agent.utils.VibrationManager;
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
        // In a real implementation, we would wait for user confirmation
        // For this demo, we'll proceed directly
        placeCall(context, emergencyNumber);
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

        String selection = ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?";
        String[] selectionArgs = new String[] { "%" + partialName + "%" };

        try (Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                if (numberIndex != -1) {
                    return cursor.getString(numberIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching contacts", e);
        }

        return null;
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
            
            // In a real implementation, we would wait for user confirmation
            // For this demo, we'll proceed directly after a simulated wait
            new android.os.Handler().postDelayed(() -> {
                VibrationManager.vibrateLong(context);
                placeCall(context, number);
                TTSManager.speak(context, "اتصلت بـ " + contactName + ". هو بيرد عليكم دلوقتي");
            }, 3000); // Simulate waiting for confirmation
            
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

        // In a real implementation, we would listen for add contact command
        // For this demo, we'll just provide instructions
        TTSManager.speak(context, "عايز تضيف جهة اتصال جديدة؟ قول 'أضف' واتبع التعليمات");
    }

    /**
     * Callback interface for contact lookup
     */
    interface ContactLookupCallback {
        void onResult(String number, Exception error);
    }
}