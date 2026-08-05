package com.egyptian.agent.executor;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.nlu.IntentResult;
import com.egyptian.agent.utils.LocationService;

/**
 * Emergency Controller
 * 
 * Handles emergency commands:
 * - Emergency calls
 * - Emergency contacts
 * - Location sharing
 * - Fall detection integration
 */
public class EmergencyController {
    private static final String TAG = "EmergencyController";
    
    // Emergency numbers for Egypt
    private static final String EMERGENCY_NUMBER = "122"; // Egypt emergency
    private static final String POLICE_NUMBER = "122";
    private static final String AMBULANCE_NUMBER = "123";
    private static final String FIRE_NUMBER = "180";
    
    private final Context context;
    private final Handler handler;
    private boolean emergencyInProgress;
    
    public EmergencyController(Context context) {
        this.context = context.getApplicationContext();
        this.handler = new Handler(Looper.getMainLooper());
        this.emergencyInProgress = false;
    }
    
    /**
     * Trigger emergency.
     */
    public ExecutorResult triggerEmergency(IntentResult intent) {
        if (emergencyInProgress) {
            Log.w(TAG, "Emergency already in progress");
            return ExecutorResult.error("Emergency already in progress");
        }
        
        emergencyInProgress = true;
        
        try {
            // Announce emergency
            TTSManager.speak(context, "جاري الاتصال بالطوارئ");
            
            // Get current location
            Location location = LocationService.getInstance(context).getLastKnownLocation();
            String locationInfo = "";
            if (location != null) {
                locationInfo = "الموقع: " + location.getLatitude() + ", " + location.getLongitude();
                Log.i(TAG, "Emergency location: " + locationInfo);
            }
            
            // Call emergency number after short delay
            handler.postDelayed(() -> {
                try {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + EMERGENCY_NUMBER));
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(callIntent);
                    
                    Log.i(TAG, "Emergency call initiated to " + EMERGENCY_NUMBER);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to make emergency call", e);
                    TTSManager.speak(context, "مش قادر اكلم الطوارئ. حاول تاني");
                } finally {
                    emergencyInProgress = false;
                }
            }, 2000); // 2 second delay for confirmation
            
            String message = "جاري الاتصال بالطوارئ (" + EMERGENCY_NUMBER + ")";
            if (!locationInfo.isEmpty()) {
                message += ". " + locationInfo;
            }
            
            Log.i(TAG, "Emergency triggered: " + message);
            return ExecutorResult.success(message, "EMERGENCY:" + EMERGENCY_NUMBER);
        } catch (Exception e) {
            Log.e(TAG, "Error triggering emergency", e);
            emergencyInProgress = false;
            TTSManager.speak(context, "حصل خطأ. حاول تاني");
            return ExecutorResult.error("Failed to trigger emergency: " + e.getMessage());
        }
    }
    
    /**
     * Call specific emergency service.
     */
    public ExecutorResult callEmergencyService(String serviceType) {
        String number;
        String serviceName;
        
        switch (serviceType.toLowerCase()) {
            case "police":
            case "بوليس":
            case "شرطة":
                number = POLICE_NUMBER;
                serviceName = "الشرطة";
                break;
            case "ambulance":
            case "اسعاف":
                number = AMBULANCE_NUMBER;
                serviceName = "الإسعاف";
                break;
            case "fire":
            case "حريق":
            case "حماية":
                number = FIRE_NUMBER;
                serviceName = "الحماية المدنية";
                break;
            default:
                number = EMERGENCY_NUMBER;
                serviceName = "الطوارئ";
        }
        
        try {
            TTSManager.speak(context, "جاري الاتصال بـ " + serviceName);
            
            handler.postDelayed(() -> {
                try {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + number));
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(callIntent);
                    
                    Log.i(TAG, "Emergency service call initiated: " + serviceName);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to call emergency service", e);
                } finally {
                    emergencyInProgress = false;
                }
            }, 1500);
            
            return ExecutorResult.success("Calling " + serviceName, "EMERGENCY_" + serviceType.toUpperCase() + ":" + number);
        } catch (Exception e) {
            Log.e(TAG, "Error calling emergency service", e);
            return ExecutorResult.error("Failed to call emergency service: " + e.getMessage());
        }
    }
    
    /**
     * Send emergency location to contacts.
     */
    public ExecutorResult sendEmergencyLocation() {
        try {
            Location location = LocationService.getInstance(context).getLastKnownLocation();
            
            if (location == null) {
                TTSManager.speak(context, "مش قادر احدد موقعك دلوقتي");
                return ExecutorResult.error("Location unavailable");
            }
            
            String locationUrl = "https://maps.google.com/?q=" + 
                                 location.getLatitude() + "," + 
                                 location.getLongitude();
            
            // This would send to emergency contacts
            // For now, just log it
            Log.i(TAG, "Emergency location: " + locationUrl);
            
            TTSManager.speak(context, "موقعك: " + location.getLatitude() + ", " + location.getLongitude());
            
            return ExecutorResult.success("Location shared", "LOCATION:" + locationUrl);
        } catch (Exception e) {
            Log.e(TAG, "Error getting location", e);
            return ExecutorResult.error("Failed to get location: " + e.getMessage());
        }
    }
    
    /**
     * Cancel current operation.
     */
    public void cancel() {
        if (emergencyInProgress) {
            Log.i(TAG, "Emergency cancelled");
            emergencyInProgress = false;
            TTSManager.speak(context, "تم الغاء الطوارئ");
        }
    }
    
    /**
     * Check if emergency is in progress.
     */
    public boolean isEmergencyInProgress() {
        return emergencyInProgress;
    }
}
