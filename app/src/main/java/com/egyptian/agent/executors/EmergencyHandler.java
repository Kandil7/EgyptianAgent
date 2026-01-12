package com.egyptian.agent.executors;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.egyptian.agent.core.TTSManager;

import java.util.Arrays;
import java.util.List;

/**
 * Emergency Handler
 * Handles emergency situations and triggers appropriate responses
 */
public class EmergencyHandler {
    private static final String TAG = "EmergencyHandler";
    
    // Common emergency keywords in Egyptian dialect
    private static final List<String> EMERGENCY_KEYWORDS = Arrays.asList(
        "emergency", "emergencies", "ngda", "استغاثة", "استغث", "طوارئ", "危", "危",
        "emergency services", "ngda 3amalya", "estghatha", "tawari", "medical emergency",
        "medical help", "help", "help me", "sos", "s.o.s", "s o s", "救命", "救救我"
    );
    
    /**
     * Checks if a command is an emergency command
     * @param command The command to check
     * @return true if emergency command, false otherwise
     */
    public static boolean isEmergency(String command) {
        if (command == null) {
            return false;
        }
        
        String lowerCommand = command.toLowerCase();
        
        // Check for emergency keywords
        for (String keyword : EMERGENCY_KEYWORDS) {
            if (lowerCommand.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Triggers emergency response
     * @param context Context for the operation
     */
    public static void trigger(Context context) {
        Log.i(TAG, "Emergency triggered!");
        
        // Speak emergency notification
        TTSManager.speak(context, "حالة طوارئ! ببدأ الإجراءات الطارئة");
        
        // Try to call emergency services
        callEmergencyServices(context);
        
        // Share location if available
        shareLocation(context);
        
        // Notify guardians if configured
        notifyGuardians(context);
    }
    
    /**
     * Calls emergency services
     * @param context Context for the operation
     */
    private static void callEmergencyServices(Context context) {
        // Emergency numbers for Egypt
        String[] emergencyNumbers = {"122", "123", "126", "180"}; // Police, Ambulance, Fire, Civil Defense
        
        // Check for call permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            
            // Request permission or inform user
            TTSManager.speak(context, "التطبيق محتاج إذن الاتصال بالطوارئ");
            
            // Redirect to settings if needed
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
            context.startActivity(intent);
            return;
        }
        
        // Try to call the first available emergency number
        for (String number : emergencyNumbers) {
            if (attemptCall(context, number)) {
                TTSManager.speak(context, "بحاول الاتصال بخدمة الطوارئ");
                return;
            }
        }
        
        TTSManager.speak(context, "مقدرش أتصل بخدمات الطوارئ");
    }
    
    /**
     * Attempts to make a call to the specified number
     * @param context Context for the operation
     * @param number The number to call
     * @return true if call initiated, false otherwise
     */
    private static boolean attemptCall(Context context, String number) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + number));
            
            // Add flags to ensure the call works from background service
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(callIntent);
            
            Log.d(TAG, "Initiating emergency call to: " + number);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error making emergency call to: " + number, e);
            return false;
        }
    }
    
    /**
     * Shares current location in emergency
     * @param context Context for the operation
     */
    private static void shareLocation(Context context) {
        // Check for location permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted for emergency location sharing");
            return;
        }
        
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            
            // Get last known location
            Location location = null;
            if (locationManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (locationManager.isLocationEnabled()) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                } else {
                    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }
            }
            
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                
                // Create location URL
                String locationUrl = "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;
                
                Log.d(TAG, "Emergency location: " + locationUrl);
                
                shareLocationWithEmergencyContacts(context, latitude, longitude);
            } else {
                Log.w(TAG, "Could not retrieve location for emergency");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting location for emergency", e);
        }
    }

    /**
     * Shares location with configured emergency contacts
     * @param context Context for the operation
     * @param latitude Latitude of current position
     * @param longitude Longitude of current position
     */
    private static void shareLocationWithEmergencyContacts(Context context, double latitude, double longitude) {
        // Create location URL
        String locationUrl = "https://www.google.com/maps/search/?api=1&query=" + latitude + "," + longitude;

        Log.d(TAG, "Sharing emergency location: " + locationUrl);

        // 1. Get emergency contacts from app settings
        // 2. Send SMS or make calls to these contacts with the location
        // 3. Possibly send via WhatsApp or other messaging apps
        // 4. Log the emergency event

        // For now, we'll simulate sending to configured emergency contacts
        String[] emergencyContacts = getEmergencyContacts(context);
        if (emergencyContacts != null) {
            for (String contactNumber : emergencyContacts) {
                sendLocationSms(context, contactNumber, locationUrl);
            }
        }
    }

    /**
     * Gets configured emergency contacts from app settings
     * @param context Context for the operation
     * @return Array of emergency contact numbers
     */
    private static String[] getEmergencyContacts(Context context) {
        // Retrieve emergency contacts from shared preferences
        // where the user has configured their emergency contacts
        android.content.SharedPreferences prefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE);
        String contactsStr = prefs.getString("contacts", "");

        if (!contactsStr.isEmpty()) {
            return contactsStr.split(",");
        }

        // Default emergency contacts could be retrieved from settings
        return new String[]{"122", "123"}; // Police and ambulance as examples
    }

    /**
     * Sends location via SMS to a contact
     * @param context Context for the operation
     * @param phoneNumber Phone number to send to
     * @param locationUrl Location URL to send
     */
    private static void sendLocationSms(Context context, String phoneNumber, String locationUrl) {
        try {
            String message = "Egyptian Agent Emergency: Location during emergency situation: " + locationUrl;

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "SMS permission not granted for emergency location sharing");
                return;
            }

            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

            Log.d(TAG, "Emergency location sent to: " + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "Error sending emergency location SMS to: " + phoneNumber, e);
        }
    }
    
    /**
     * Notifies configured guardians in emergency
     * @param context Context for the operation
     */
    private static void notifyGuardians(Context context) {
        Log.d(TAG, "Notifying emergency guardians");

        // Get emergency contacts from app settings
        String[] emergencyContacts = getEmergencyContacts(context);

        if (emergencyContacts != null && emergencyContacts.length > 0) {
            // Send SMS or make calls to these contacts
            for (String contactNumber : emergencyContacts) {
                sendEmergencyNotification(context, contactNumber);
            }
        } else {
            Log.w(TAG, "No emergency contacts configured");
        }

        // Share location with them
        shareLocation(context);

        // Log the emergency event
        logEmergencyEvent(context);

        TTSManager.speak(context, "تم إخطار جهات الاتصال الطارئة");
    }

    /**
     * Sends an emergency notification to a contact
     * @param context Context for the operation
     * @param contactNumber The contact number to notify
     */
    private static void sendEmergencyNotification(Context context, String contactNumber) {
        try {
            String message = "Egyptian Agent Emergency Alert: Emergency situation detected. Location and assistance required.";

            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "SMS permission not granted for emergency notification");
                return;
            }

            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
            smsManager.sendTextMessage(contactNumber, null, message, null, null);

            Log.d(TAG, "Emergency notification sent to: " + contactNumber);
        } catch (Exception e) {
            Log.e(TAG, "Error sending emergency notification to: " + contactNumber, e);
        }
    }

    /**
     * Logs the emergency event
     * @param context Context for the operation
     */
    private static void logEmergencyEvent(Context context) {
        // Log the emergency event to a local file or database
        try {
            java.io.FileOutputStream fos = context.openFileOutput("emergency_log.txt", Context.MODE_APPEND);
            java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(fos);

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            String timestamp = sdf.format(new java.util.Date());

            writer.write("Emergency event at: " + timestamp + "\n");
            writer.close();
            fos.close();

            Log.d(TAG, "Emergency event logged");
        } catch (Exception e) {
            Log.e(TAG, "Error logging emergency event", e);
        }
    }
}