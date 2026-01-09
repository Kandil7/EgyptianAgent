package com.egyptian.agent.executors;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.WhatsAppService;
import com.egyptian.agent.utils.TelephonyService;
import com.egyptian.agent.utils.LocationService;
import com.egyptian.agent.utils.CrashLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced Emergency Handler implementing the complete emergency protocol
 * as defined in the SRD
 */
public class EmergencyHandler implements EmergencyHandlerInterface {
    private static final String TAG = "EmergencyHandler";
    private Context context;
    private boolean isActive = false;
    private long startTime;
    private List<String> contactedNumbers = new ArrayList<>();
    private Location lastLocation;
    private String audioAlertSent;
    private String emergencyId;
    private String activationMethod;
    private boolean isEmergencyConfirmed = false;

    // Emergency contacts configuration
    private static final List<String> EMERGENCY_NUMBERS = List.of("123", "122", "180"); // Police, Ambulance, Fire

    public EmergencyHandler(Context context) {
        this.context = context;
    }

    @Override
    public void trigger(Context context, String reason) {
        Log.i(TAG, "Emergency triggered with reason: " + reason);
        
        if (isActive) {
            Log.w(TAG, "Emergency already active, ignoring new trigger");
            return;
        }

        // Generate unique emergency ID
        this.emergencyId = "EMG-" + System.currentTimeMillis();
        this.startTime = System.currentTimeMillis();
        this.activationMethod = reason;
        this.isActive = true;
        this.isEmergencyConfirmed = true;

        // Announce emergency
        TTSManager.speak(context, "تم تفعيل وضع الطوارئ! جاري الاتصال بجهات الطوارئ");

        // Get current location
        LocationService locationService = new LocationService();
        this.lastLocation = locationService.getLastKnownLocation(context);

        // Execute emergency protocol
        executeEmergencyProtocol(context);
    }

    private void executeEmergencyProtocol(Context context) {
        // 1. Start audio alert
        startEmergencyAudioAlert();

        // 2. Send WhatsApp emergency messages to family
        sendEmergencyWhatsAppMessages(context);

        // 3. Call emergency numbers
        callEmergencyNumbers(context);

        // 4. Call family members
        callFamilyMembers(context);
    }

    private void startEmergencyAudioAlert() {
        // In a real implementation, this would play an emergency alert sound
        Log.d(TAG, "Emergency audio alert started");
        this.audioAlertSent = "emergency_alert.mp3";
    }

    private void sendEmergencyWhatsAppMessages(Context context) {
        WhatsAppService whatsAppService = new WhatsAppService();
        
        // Get family contacts from user settings
        List<String> familyContacts = getUserFamilyContacts();
        
        for (String contact : familyContacts) {
            String message = "🚨 تنبيه طوارئ! 🚨\n" +
                           "تم تفعيل وضع الطوارئ في تطبيق الوكيل المصري.\n" +
                           "الوقت: " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()) + "\n" +
                           "الموقع: " + (lastLocation != null ? 
                                 lastLocation.getLatitude() + "," + lastLocation.getLongitude() : 
                                 "غير متاح") + "\n" +
                           "السبب: تم اكتشاف حالة طوارئ تلقائيًا.";
            
            if (whatsAppService.isWhatsAppInstalled(context)) {
                whatsAppService.sendEmergencyWhatsApp(context, contact, message);
                contactedNumbers.add(contact);
            }
        }
    }

    private void callEmergencyNumbers(Context context) {
        TelephonyService telephonyService = new TelephonyService();
        
        for (String number : EMERGENCY_NUMBERS) {
            try {
                telephonyService.placeCall(context, number);
                contactedNumbers.add(number);
                
                // Wait a bit between calls
                Thread.sleep(5000); // 5 seconds
                
                // Check if call was answered
                if (telephonyService.isCallActive()) {
                    Log.d(TAG, "Emergency call answered for: " + number);
                    break; // Stop if someone answers
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to call emergency number: " + number, e);
                CrashLogger.logError(context, e);
            }
        }
    }

    private void callFamilyMembers(Context context) {
        TelephonyService telephonyService = new TelephonyService();
        
        // Get family contacts from user settings
        List<String> familyContacts = getUserFamilyContacts();
        
        for (String contact : familyContacts) {
            try {
                telephonyService.placeCall(context, contact);
                contactedNumbers.add(contact);
                
                // Wait a bit between calls
                Thread.sleep(3000); // 3 seconds
            } catch (Exception e) {
                Log.e(TAG, "Failed to call family member: " + contact, e);
                CrashLogger.logError(context, e);
            }
        }
    }

    private List<String> getUserFamilyContacts() {
        // In a real implementation, this would retrieve user-configured family contacts
        // from shared preferences or a database
        List<String> contacts = new ArrayList<>();
        contacts.add("01000000000"); // Placeholder
        contacts.add("01111111111"); // Placeholder
        return contacts;
    }

    @Override
    public boolean isEmergencyActive() {
        return isActive;
    }

    @Override
    public void cancelEmergency() {
        Log.i(TAG, "Emergency cancelled");
        isActive = false;
        isEmergencyConfirmed = false;
        
        // Stop any ongoing audio alerts
        stopEmergencyAudioAlert();
        
        // Announce cancellation
        TTSManager.speak(context, "تم إلغاء وضع الطوارئ");
    }

    private void stopEmergencyAudioAlert() {
        // In a real implementation, this would stop the emergency audio alert
        Log.d(TAG, "Emergency audio alert stopped");
    }

    @Override
    public void enableSeniorMode() {
        Log.i(TAG, "Senior mode enabled for emergency handler");
        // In senior mode, we might adjust the emergency response behavior
        // For example, reducing the confirmation time or skipping certain steps
    }

    @Override
    public void disableSeniorMode() {
        Log.i(TAG, "Senior mode disabled for emergency handler");
    }

    // Getters for emergency state
    public String getEmergencyId() {
        return emergencyId;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getActivationMethod() {
        return activationMethod;
    }

    public List<String> getContactedNumbers() {
        return new ArrayList<>(contactedNumbers);
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public String getAudioAlertSent() {
        return audioAlertSent;
    }

    public boolean isEmergencyConfirmed() {
        return isEmergencyConfirmed;
    }
}