package com.egyptian.agent.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.RequiresApi;

public class SystemAppHelper {
    
    private static final String TAG = "SystemAppHelper";
    
    /**
     * Keep the app alive on Honor devices with aggressive battery optimization
     */
    public static void keepAlive(Context context) {
        Log.i(TAG, "Applying system-level optimizations for Honor device");
        
        // Apply battery optimization exemptions
        applyBatteryOptimizationExemptions(context);
        
        // Apply doze mode exemptions
        applyDozeModeExemptions(context);
        
        // Set up foreground services
        setupForegroundServices(context);
    }
    
    private static void applyBatteryOptimizationExemptions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                
                // Try to request exemption (this will show a system dialog)
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    // In a service context, we can't directly start activities
                    // This would typically be done from an activity
                    Log.d(TAG, "Battery optimization exemption intent created");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to request battery optimization exemption", e);
            }
        }
        
        // Add to whitelist programmatically where possible
        try {
            // Use dumpsys command to add to whitelist (requires system-level permissions)
            Runtime.getRuntime().exec("su -c 'dumpsys deviceidle whitelist +" + context.getPackageName() + "'");
            Log.i(TAG, "Added app to device idle whitelist");
        } catch (Exception e) {
            Log.w(TAG, "Could not add to device idle whitelist (requires root): " + e.getMessage());
        }
    }
    
    private static void applyDozeModeExemptions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // Try to request exemption from doze mode
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                
                // This would typically be called from an activity
                Log.d(TAG, "Doze mode exemption intent created");
            } catch (Exception e) {
                Log.e(TAG, "Failed to request doze mode exemption", e);
            }
        }
    }
    
    private static void setupForegroundServices(Context context) {
        // Ensure our services are set up as foreground services
        // This is handled in the service declaration in AndroidManifest.xml
        Log.d(TAG, "Foreground services configured in manifest");
    }
    
    /**
     * Check if the app has system-level permissions
     */
    public static boolean hasSystemPermissions(Context context) {
        // Check if the app has system UID (sharedUserId="android.uid.system")
        try {
            // This is a basic check - in a real system app, this would be set in the manifest
            return context.getApplicationInfo().uid < 10000; // System apps typically have lower UIDs
        } catch (Exception e) {
            Log.e(TAG, "Error checking system permissions", e);
            return false;
        }
    }
    
    /**
     * Request system-level permissions if not already granted
     */
    public static void requestSystemPermissions(Context context) {
        // In a real implementation, this would involve flashing the app as a system app
        // This typically requires root access and flashing to system partition
        Log.i(TAG, "System permissions should be granted by installing as system app");
    }
}