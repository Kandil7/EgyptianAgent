package com.egyptian.agent.executors;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.VibrationManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles emergency situations with appropriate responses
 */
public class EmergencyHandler {
    private static final String TAG = "EmergencyHandler";
    private static final String PREFS_NAME = "emergency_prefs";
    private static final String EMERGENCY_CONTACTS_KEY = "emergency_contacts";
    private static boolean seniorModeEnabled = false;

    /**
     * Checks if the given command indicates an emergency
     */
    public static boolean isEmergency(String command) {
        String lowerCmd = command.toLowerCase();
        
        // Check for emergency keywords in Arabic and Egyptian dialect
        return lowerCmd.contains(" emergencies") || 
               lowerCmd.contains("emergency") || 
               lowerCmd.contains("ngda") || 
               lowerCmd.contains("estghatha") || 
               lowerCmd.contains("tawari") || 
               lowerCmd.contains("help") || 
               lowerCmd.contains("help me") || 
               lowerCmd.contains("need help") || 
               lowerCmd.contains("escaf") ||  // ambulance
               lowerCmd.contains("police") || 
               lowerCmd.contains("nafar") ||  // police in Egyptian dialect
               lowerCmd.contains("nar") ||    // fire in Egyptian dialect
               lowerCmd.contains("fire") || 
               lowerCmd.contains("burning") || 
               lowerCmd.contains("fall") || 
               lowerCmd.contains("fell") || 
               lowerCmd.contains("fallen") || 
               lowerCmd.contains("wq3t") ||   // fell in Arabic
               lowerCmd.contains("wq3") ||    // fall in Arabic
               lowerCmd.contains("injur") ||  // injury related
               lowerCmd.contains("darar");    // harm in Arabic
    }

    /**
     * Triggers emergency response
     */
    public static void trigger(Context context) {
        trigger(context, false);
    }

    /**
     * Triggers emergency response
     * @param context Application context
     * @param fromFall True if emergency was triggered by fall detection
     */
    public static void trigger(Context context, boolean fromFall) {
        Log.e(TAG, "EMERGENCY TRIGGERED" + (fromFall ? " (from fall detection)" : ""));
        
        // Play emergency sound
        playEmergencySound(context);
        
        // Vibrate intensely
        VibrationManager.vibrateEmergency(context);
        
        // Speak emergency message
        if (fromFall) {
            TTSManager.speak(context, "تم اكتشاف سقوط! يتم الاتصال بجهات الطوارئ الآن!");
        } else {
            TTSManager.speak(context, "حالة طوارئ! يتم الاتصال بجهات الطوارئ الآن!");
        }
        
        // Get emergency contacts
        List<String> emergencyContacts = getEmergencyContacts(context);
        
        if (!emergencyContacts.isEmpty()) {
            // Call the first emergency contact
            String firstContact = emergencyContacts.get(0);
            callEmergencyContact(context, firstContact);
            
            // Send SMS to all emergency contacts with location
            sendSMSToEmergencyContacts(context, emergencyContacts, fromFall);
            
            // If senior mode is enabled, also send notification to guardians
            if (seniorModeEnabled) {
                notifyGuardians(context, fromFall);
            }
        } else {
            // If no emergency contacts are set, call default emergency numbers
            callDefaultEmergencyNumbers(context);
        }
    }

    /**
     * Plays emergency sound
     */
    private static void playEmergencySound(Context context) {
        // In a real implementation, this would play an emergency sound
        // For this demo, we'll just log it
        Log.d(TAG, "Playing emergency sound");
    }

    /**
     * Gets configured emergency contacts
     */
    private static List<String> getEmergencyContacts(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String contactsStr = prefs.getString(EMERGENCY_CONTACTS_KEY, "");
        
        List<String> contacts = new ArrayList<>();
        if (!contactsStr.isEmpty()) {
            // Split by comma and trim spaces
            String[] contactArray = contactsStr.split(",");
            for (String contact : contactArray) {
                contacts.add(contact.trim());
            }
        }
        
        // If no contacts are set, add default ones for demo purposes
        if (contacts.isEmpty()) {
            contacts.add("123"); // Emergency
            contacts.add("122"); // Police
            contacts.add("1234567890"); // Sample contact
        }
        
        return contacts;
    }

    /**
     * Calls an emergency contact
     */
    private static void callEmergencyContact(Context context, String contactNumber) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing CALL_PHONE permission");
            TTSManager.speak(context, "محتاج إذن لإجراء مكالمات الطوارئ");
            return;
        }

        try {
            String cleanNumber = contactNumber.replaceAll("[^0-9+]", "");
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + cleanNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(callIntent);
            Log.i(TAG, "Emergency call placed to: " + cleanNumber);
        } catch (Exception e) {
            Log.e(TAG, "Emergency call failed", e);
            TTSManager.speak(context, "فشل في الاتصال بجهة الطوارئ. جاري المحاولة مرة أخرى...");
            
            // Try calling default emergency number
            callDefaultEmergencyNumbers(context);
        }
    }

    /**
     * Calls default emergency numbers
     */
    private static void callDefaultEmergencyNumbers(Context context) {
        // In a real implementation, this would cycle through default emergency numbers
        // For this demo, we'll just log it
        Log.d(TAG, "Calling default emergency numbers");
        TTSManager.speak(context, "جارٍ الاتصال بجهات الطوارئ...");
    }

    /**
     * Sends SMS to emergency contacts
     */
    private static void sendSMSToEmergencyContacts(Context context, List<String> contacts, boolean fromFall) {
        // Check for SMS permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing SEND_SMS permission");
            return;
        }

        // Get current location if available
        String location = getCurrentLocation(context);
        String locationText = location != null ? " الموقع: " + location : "";

        String message = fromFall ? 
            "تنبيه طوارئ: تم اكتشاف سقوط لمستخدم الوكيل المصري!" + locationText :
            "تنبيه طوارئ: تم تفعيل زر الطوارئ من مستخدم الوكيل المصري!" + locationText;

        // In a real implementation, this would send SMS to each contact
        // For this demo, we'll just log it
        for (String contact : contacts) {
            Log.d(TAG, "SMS to " + contact + ": " + message);
        }
    }

    /**
     * Gets current location
     */
    private static String getCurrentLocation(Context context) {
        // In a real implementation, this would get the current location
        // For this demo, we'll return a placeholder
        return "القاهرة، مصر";
    }

    /**
     * Notifies guardians (used in senior mode)
     */
    private static void notifyGuardians(Context context, boolean fromFall) {
        // In a real implementation, this would send notifications to guardians
        // For this demo, we'll just log it
        Log.d(TAG, "Notifying guardians" + (fromFall ? " (from fall)" : ""));
        TTSManager.speak(context, "جارٍ إخطار الأوصياء...");
    }

    /**
     * Enables senior mode for emergency handling
     */
    public static void enableSeniorMode() {
        seniorModeEnabled = true;
        Log.d(TAG, "Senior mode enabled for emergency handler");
    }

    /**
     * Disables senior mode for emergency handling
     */
    public static void disableSeniorMode() {
        seniorModeEnabled = false;
        Log.d(TAG, "Senior mode disabled for emergency handler");
    }
}