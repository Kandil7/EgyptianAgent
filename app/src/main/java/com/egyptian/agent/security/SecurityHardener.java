package com.egyptian.agent.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SecurityHardener - Implements security hardening measures for Egyptian Agent
 * Provides app integrity verification, secure communication, and tamper detection
 */
public class SecurityHardener {
    private static final String TAG = "SecurityHardener";
    
    // Known good signatures for the app (in production, these would be the actual signatures)
    private static final List<String> KNOWN_SIGNATURES = Arrays.asList(
        // These would be the actual SHA-256 hashes of the signing certificates
        // In a real implementation, you'd include the actual hashes
    );
    
    private Context context;
    private boolean isIntegrityVerified = false;
    private boolean isRootedDevice = false;
    
    public SecurityHardener(Context context) {
        this.context = context.getApplicationContext();
        this.isRootedDevice = checkRootStatus();
    }
    
    /**
     * Performs comprehensive security checks
     */
    public SecurityReport performSecurityChecks() {
        Log.i(TAG, "Performing security checks...");
        
        SecurityReport report = new SecurityReport();
        
        // Check app integrity
        report.setIntegrityCheckPassed(verifyAppIntegrity());
        
        // Check for root
        report.setRootCheckPassed(!isRootedDevice);
        
        // Check for debug mode
        report.setDebugCheckPassed(!isDebugBuild());
        
        // Check for Xposed framework
        report.setXposedCheckPassed(!isXposedInstalled());
        
        // Check for known hooking frameworks
        report.setHookingCheckPassed(!isHookingFrameworkDetected());
        
        // Log security status
        if (!report.isAllChecksPassed()) {
            Log.w(TAG, "Security checks failed: " + report.getSecurityIssues());
        } else {
            Log.i(TAG, "All security checks passed");
        }
        
        return report;
    }
    
    /**
     * Verifies the app's integrity by checking signatures
     */
    public boolean verifyAppIntegrity() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                context.getPackageName(), 
                PackageManager.GET_SIGNATURES
            );
            
            for (Signature signature : packageInfo.signatures) {
                String signatureHash = getSignatureHash(signature);
                
                // Check if the signature matches any known good signatures
                if (KNOWN_SIGNATURES.contains(signatureHash)) {
                    isIntegrityVerified = true;
                    return true;
                }
            }
            
            Log.e(TAG, "App integrity verification failed - unknown signature");
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error verifying app integrity", e);
            return false;
        }
    }
    
    /**
     * Gets the SHA-256 hash of a signature
     */
    private String getSignatureHash(Signature signature) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA256");
            byte[] hash = md.digest(signature.toByteArray());
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 algorithm not available", e);
            return null;
        }
    }
    
    /**
     * Checks if the device is rooted
     */
    public boolean checkRootStatus() {
        // Check for common root indicators
        String[] rootIndicators = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        };
        
        for (String indicator : rootIndicators) {
            if (new java.io.File(indicator).exists()) {
                Log.w(TAG, "Root indicator found: " + indicator);
                return true;
            }
        }
        
        // Check for root management apps
        String[] rootApps = {
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su"
        };
        
        PackageManager pm = context.getPackageManager();
        for (String app : rootApps) {
            try {
                pm.getPackageInfo(app, 0);
                Log.w(TAG, "Root management app detected: " + app);
                return true;
            } catch (PackageManager.NameNotFoundException e) {
                // App not found, continue checking
            }
        }
        
        // Check for su binary
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            return process.waitFor() == 0; // If this succeeds, device is rooted
        } catch (Exception e) {
            // Unable to execute su command, likely not rooted
        }
        
        return false;
    }
    
    /**
     * Checks if this is a debug build
     */
    public boolean isDebugBuild() {
        return context.getApplicationInfo().flags == android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE;
    }
    
    /**
     * Checks if Xposed framework is installed
     */
    public boolean isXposedInstalled() {
        // Check for Xposed-specific classes
        try {
            Class.forName("de.robv.android.xposed.XposedBridge");
            Log.w(TAG, "Xposed framework detected");
            return true;
        } catch (ClassNotFoundException e) {
            // Xposed not found
        }
        
        // Check for Xposed app
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo("de.robv.android.xposed.installer", 0);
            Log.w(TAG, "Xposed installer detected");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            // Xposed installer not found
        }
        
        return false;
    }
    
    /**
     * Checks for common hooking frameworks
     */
    public boolean isHookingFrameworkDetected() {
        // Check for common hooking framework indicators
        String[] hookingIndicators = {
            "XposedBridge.jar",
            "substrate",
            "CydiaSubstrate"
        };
        
        // Check loaded classes
        try {
            ClassLoader classLoader = context.getClass().getClassLoader();
            for (String indicator : hookingIndicators) {
                try {
                    classLoader.loadClass(indicator);
                    Log.w(TAG, "Hooking framework indicator detected: " + indicator);
                    return true;
                } catch (ClassNotFoundException e) {
                    // Indicator not found, continue checking
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking for hooking frameworks", e);
        }
        
        return false;
    }
    
    /**
     * Checks if the app is running in an emulator
     */
    public boolean isEmulator() {
        String hardware = android.os.Build.HARDWARE;
        String model = android.os.Build.MODEL;
        
        // Common emulator indicators
        return hardware.contains("goldfish") || 
               hardware.contains("ranchu") || 
               model.contains("google_sdk") || 
               model.contains("Emulator") || 
               model.contains("Android SDK built for x86");
    }
    
    /**
     * Gets the root detection status
     */
    public boolean isRooted() {
        return isRootedDevice;
    }
    
    /**
     * Gets the integrity verification status
     */
    public boolean isIntegrityVerified() {
        return isIntegrityVerified;
    }
    
    /**
     * Securely hashes sensitive data
     */
    public String secureHash(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error hashing data", e);
            return null;
        }
    }
    
    /**
     * Represents the security report
     */
    public static class SecurityReport {
        private boolean integrityCheckPassed = false;
        private boolean rootCheckPassed = false;
        private boolean debugCheckPassed = false;
        private boolean xposedCheckPassed = false;
        private boolean hookingCheckPassed = false;
        
        public boolean isAllChecksPassed() {
            return integrityCheckPassed && 
                   rootCheckPassed && 
                   debugCheckPassed && 
                   xposedCheckPassed && 
                   hookingCheckPassed;
        }
        
        public String getSecurityIssues() {
            List<String> issues = new ArrayList<>();
            
            if (!integrityCheckPassed) issues.add("App integrity verification failed");
            if (!rootCheckPassed) issues.add("Device is rooted");
            if (!debugCheckPassed) issues.add("Debug build detected");
            if (!xposedCheckPassed) issues.add("Xposed framework detected");
            if (!hookingCheckPassed) issues.add("Hooking framework detected");
            
            return String.join(", ", issues);
        }
        
        // Getters and setters
        public boolean isIntegrityCheckPassed() { return integrityCheckPassed; }
        public void setIntegrityCheckPassed(boolean integrityCheckPassed) { 
            this.integrityCheckPassed = integrityCheckPassed; 
        }
        
        public boolean isRootCheckPassed() { return rootCheckPassed; }
        public void setRootCheckPassed(boolean rootCheckPassed) { 
            this.rootCheckPassed = rootCheckPassed; 
        }
        
        public boolean isDebugCheckPassed() { return debugCheckPassed; }
        public void setDebugCheckPassed(boolean debugCheckPassed) { 
            this.debugCheckPassed = debugCheckPassed; 
        }
        
        public boolean isXposedCheckPassed() { return xposedCheckPassed; }
        public void setXposedCheckPassed(boolean xposedCheckPassed) { 
            this.xposedCheckPassed = xposedCheckPassed; 
        }
        
        public boolean isHookingCheckPassed() { return hookingCheckPassed; }
        public void setHookingCheckPassed(boolean hookingCheckPassed) { 
            this.hookingCheckPassed = hookingCheckPassed; 
        }
    }
}