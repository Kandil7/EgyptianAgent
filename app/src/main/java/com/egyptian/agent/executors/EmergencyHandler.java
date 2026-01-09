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
        try {
            // Play emergency sound using MediaPlayer
            android.media.MediaPlayer mediaPlayer = android.media.MediaPlayer.create(context, R.raw.emergency_alert);
            if (mediaPlayer != null) {
                mediaPlayer.start();
            } else {
                // If resource not available, use system alert
                android.media.AudioManager audioManager = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                int maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM);
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxVolume, 0);

                // Create a short beep as emergency signal
                android.media.ToneGenerator toneG = new android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, maxVolume);
                toneG.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2000); // 2 sec
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing emergency sound", e);
        }
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
        // Cycle through default emergency numbers
        String[] defaultNumbers = {"122", "123", "180"}; // Police, Ambulance, Fire

        for (String number : defaultNumbers) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
                try {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + number));
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(callIntent);
                    Log.i(TAG, "Called emergency number: " + number);
                    break; // Call first available number
                } catch (Exception e) {
                    Log.e(TAG, "Failed to call emergency number: " + number, e);
                    continue; // Try next number
                }
            }
        }

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

        // Send SMS to each contact
        for (String contact : contacts) {
            try {
                android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
                smsManager.sendTextMessage(contact, null, message, null, null);
                Log.d(TAG, "SMS sent to " + contact + ": " + message);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send SMS to " + contact, e);
                CrashLogger.logError(context, e);
            }
        }
    }

    /**
     * Gets current location
     */
    private static String getCurrentLocation(Context context) {
        // Check for location permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted");
            return null;
        }

        try {
            android.location.LocationManager locationManager =
                (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            // Get last known location
            android.location.Location location = locationManager.getLastKnownLocation(
                android.location.LocationManager.GPS_PROVIDER);

            if (location != null) {
                return String.format("%.4f, %.4f", location.getLatitude(), location.getLongitude());
            } else {
                // Try network provider as fallback
                location = locationManager.getLastKnownLocation(
                    android.location.LocationManager.NETWORK_PROVIDER);

                if (location != null) {
                    return String.format("%.4f, %.4f", location.getLatitude(), location.getLongitude());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting location", e);
            CrashLogger.logError(context, e);
        }

        return null;
    }

    /**
     * Notifies guardians (used in senior mode)
     */
    private static void notifyGuardians(Context context, boolean fromFall) {
        // Get guardian phone number from senior mode config
        String guardianNumber = com.egyptian.agent.accessibility.SeniorModeManager.getInstance(context).getGuardianPhoneNumber();

        if (guardianNumber == null || guardianNumber.isEmpty()) {
            Log.w(TAG, "No guardian phone number configured");
            return;
        }

        // Check for SMS permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing SEND_SMS permission for guardian notification");
            return;
        }

        // Get current location if available
        String location = getCurrentLocation(context);
        String locationText = location != null ? " الموقع: " + location : "";

        String message = fromFall ?
            "تنبيه طوارئ: تم اكتشاف سقوط لوالدك/والدتك!" + locationText :
            "تنبيه طوارئ: تم تفعيل زر الطوارئ من والدك/والدتك!" + locationText;

        try {
            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
            smsManager.sendTextMessage(guardianNumber, null, message, null, null);
            Log.d(TAG, "Guardian notification sent to " + guardianNumber + ": " + message);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send guardian notification to " + guardianNumber, e);
            CrashLogger.logError(context, e);
        }
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