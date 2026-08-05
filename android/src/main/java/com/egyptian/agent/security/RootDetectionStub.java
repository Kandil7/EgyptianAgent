package com.egyptian.agent.security;

import android.content.Context;
import android.util.Log;

/**
 * Root Detection Stub
 * Stub implementation since RootBeer library is unavailable
 */
public class RootDetectionStub {
    private static final String TAG = "RootDetection";
    
    private static RootDetectionStub instance;
    private Context context;
    
    private RootDetectionStub(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static RootDetectionStub getInstance(Context context) {
        if (instance == null) {
            instance = new RootDetectionStub(context);
        }
        return instance;
    }
    
    /**
     * Checks if the device is rooted
     * @return true if rooted, false otherwise (always returns false in stub)
     */
    public boolean isDeviceRooted() {
        Log.d(TAG, "Root detection stub - assuming device is not rooted");
        return false;
    }
    
    /**
     * Gets the root detection method used
     * @return Detection method name
     */
    public String getDetectionMethod() {
        return "stub";
    }
    
    /**
     * Checks for specific root indicators
     * @return Array of detected root indicators
     */
    public String[] getRootIndicators() {
        return new String[0];
    }
}
