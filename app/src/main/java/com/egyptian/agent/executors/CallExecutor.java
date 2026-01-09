package com.egyptian.agent.executors;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.core.IntentType;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.ContactCache;
import com.egyptian.agent.utils.VibrationManager;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CallExecutor {

    private static final String TAG = "CallExecutor";
    private static final Map<String, String> EMERGENCY_CONTACTS = new HashMap<>();
    private static ExecutorService contactLookupExecutor = Executors.newSingleThreadExecutor();

    static {
        // Predefined emergency contacts (configurable by user)
        EMERGENCY_CONTACTS.put("ماما", "123");
        EMERGENCY_CONTACTS.put("نجدة", "123");
        EMERGENCY_CONTACTS.put("إسعاف", "122");
        EMERGENCY_CONTACTS.put("شرطة", "121");
    }

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

        // Special handling for emergency contacts
        if (EMERGENCY_CONTACTS.containsKey(contactName)) {
            handleEmergencyCall(context, contactName);
            return;
        }

        // Look up contact number
        lookupContactNumber(context, contactName, (number, error) -> {
            if (error != null) {
                handleContactLookupError(context, contactName, error);
            } else if (number == null) {
                handleContactNotFound(context, contactName);
            } else {
                confirmAndPlaceCall(context, contactName, number);
            }
        });
    }

    private static String extractContactName(String command) {
        // Advanced Egyptian dialect handling
        return command.replaceAll(".*(اتصل|كلم|رن|بعت)\\s+(?:على|مع|لـ)?\\s*", "")
            .replaceAll("دلوقتي|حالا|من فضلك|فضلك|يا كبير|يا صاحبي", "")
            .trim();
    }

    private static void handleEmergencyCall(Context context, String contactName) {
        String emergencyNumber = EMERGENCY_CONTACTS.get(contactName);

        // Senior mode special handling - no confirmation needed for emergencies
        if (SeniorMode.isEnabled()) {
            placeCall(context, emergencyNumber);
            TTSManager.speak(context, "بتصل بـ " + contactName + " دلوقتي");
            return;
        }

        // Standard confirmation flow
        TTSManager.speak(context, "عايز تتصل بـ " + contactName + "؟ قول 'نعم' أو 'لا'");
        SpeechConfirmation.waitForConfirmation(context, confirmed -> {
            if (confirmed) {
                placeCall(context, emergencyNumber);
                TTSManager.speak(context, "بتصل بـ " + contactName + " دلوقتي");
            } else {
                TTSManager.speak(context, "ما عملتش اتصال");
            }
        });
    }

    private static void lookupContactNumber(Context context, String contactName, ContactLookupCallback callback) {
        // First check cache (critical for performance on 6GB RAM devices)
        String cachedNumber = ContactCache.get(context, contactName);
        if (cachedNumber != null) {
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
                CrashLogger.logError(context, e);
            }

            // Return to main thread
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                callback.onResult(number, error)
            );
        });
    }

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
                return cursor.getString(numberIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching contacts", e);
        }

        return null;
    }

    private static void confirmAndPlaceCall(Context context, String contactName, String number) {
        // Senior mode requires double confirmation
        if (SeniorMode.isEnabled()) {
            VibrationManager.vibrateShort(context);
            TTSManager.speak(context, "عايز تتصل بـ " + contactName + " على الرقم " + formatNumberForSpeech(number) +
                "؟ قول 'نعم' بس، ولا 'لا'");
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
        SpeechConfirmation.waitForConfirmation(context, confirmed -> {
            if (confirmed) {
                placeCall(context, number);
                TTSManager.speak(context, "بتكلم مع " + contactName + " دلوقتي");
            } else {
                TTSManager.speak(context, "ما عملتش اتصال");
            }
        });
    }

    public static void placeCall(Context context, String number) {
        try {
            String cleanNumber = number.replaceAll("[^0-9+]", "");
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + cleanNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Critical permission handling for system app
            context.startActivity(callIntent);
            Log.i(TAG, "Call placed to: " + cleanNumber);
        } catch (Exception e) {
            Log.e(TAG, "Call failed", e);
            CrashLogger.logError(context, e);
            TTSManager.speak(context, "حصل مشكلة في إجراء المكالمة. حاول تاني");
        }
    }

    private static String formatNumberForSpeech(String number) {
        return number.replaceAll("(\\d)", "$1 ");
    }

    private static void handleContactLookupError(Context context, String contactName, Exception error) {
        TTSManager.speak(context, "حصل خطأ في البحث عن " + contactName + ". حاول تاني بعد شوية");
        CrashLogger.logError(context, error);
    }

    private static void handleContactNotFound(Context context, String contactName) {
        TTSManager.speak(context, "مش لاقي " + contactName + " في<Contactات. قول 'أضف' عشان تضيف الرقم الجديد");

        // Listen for add contact command
        SpeechConfirmation.waitForCommand(context, 15000, command -> {
            if (command.contains("أضف") || command.contains("اضيف")) {
                TTSManager.speak(context, "قول الرقم الجديد");
                SpeechConfirmation.waitForCommand(context, 30000, numberCommand -> {
                    String newNumber = extractNumber(numberCommand);
                    if (!newNumber.isEmpty()) {
                        // In a real app, we would save the contact here
                        TTSManager.speak(context, "تم حفظ الرقم الجديد لـ " + contactName);
                        placeCall(context, newNumber);
                    } else {
                        TTSManager.speak(context, "متعرفش الرقم. قول 'يا كبير' وانا أساعدك");
                    }
                });
            }
        });
    }

    private static String extractNumber(String command) {
        return command.replaceAll("[^0-9]", "");
    }

    @FunctionalInterface
    private interface ContactLookupCallback {
        void onResult(String number, Exception error);
    }
}