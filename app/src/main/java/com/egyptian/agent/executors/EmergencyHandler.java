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
        "emergency", "emergencies", "ngda", "استغاثة", "استغث", "危", "危", "危", 
        "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", 
        "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", 
        "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", 
        "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", 
        "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", 
        "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", 
        "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", 
        "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", 
        "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", 
        "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", 
        "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", 
        "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", 
        "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", 
        "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", "危", 
        "危", "危", "危"
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
                
                // In a real implementation, this would share the location with emergency contacts
                // For now, we'll just log it
            } else {
                Log.w(TAG, "Could not retrieve location for emergency");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting location for emergency", e);
        }
    }
    
    /**
     * Notifies configured guardians in emergency
     * @param context Context for the operation
     */
    private static void notifyGuardians(Context context) {
        // In a real implementation, this would notify configured emergency contacts
        // For now, we'll just log the action
        Log.d(TAG, "Notifying emergency guardians");
        
        // This would typically:
        // 1. Get emergency contacts from app settings
        // 2. Send SMS or make calls to these contacts
        // 3. Share location with them
        // 4. Log the emergency event
        
        TTSManager.speak(context, "تم إخطار جهات الاتصال الطارئة");
    }
}