package com.egyptian.agent.security;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Data encryption manager for sensitive information
 * Uses AndroidX Security Crypto library for AES-256 encryption
 */
public class DataEncryptionManager {
    private static final String TAG = "DataEncryptionManager";
    private static final String MASTER_KEY_ALIAS = "EgyptianAgentMasterKey";
    private static final String ENCRYPTED_SHARED_PREFS_NAME = "encrypted_shared_prefs";

    private static DataEncryptionManager instance;
    private Context context;
    private MasterKey masterKey;
    private EncryptedSharedPreferences encryptedSharedPreferences;

    private DataEncryptionManager(Context context) {
        this.context = context.getApplicationContext();
        initializeEncryption();
    }

    public static synchronized DataEncryptionManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataEncryptionManager(context);
        }
        return instance;
    }

    private void initializeEncryption() {
        try {
            // Create or retrieve the master key
            masterKey = new MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    .setKeyGenParameterSpec(
                            new KeyGenParameterSpec.Builder(
                                    MASTER_KEY_ALIAS,
                                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                    .setKeySize(256)
                                    .build())
                    .build();

            // Create encrypted shared preferences
            encryptedSharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences
                    .create(
                            context,
                            ENCRYPTED_SHARED_PREFS_NAME,
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    );

            Log.i(TAG, "Data encryption initialized successfully");
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to initialize data encryption", e);
        }
    }

    /**
     * Encrypt and store sensitive data
     */
    public void storeSensitiveData(String key, String value) {
        if (encryptedSharedPreferences != null) {
            encryptedSharedPreferences.edit().putString(key, value).apply();
        } else {
            Log.e(TAG, "Encryption not initialized, storing in plain text (not recommended)");
            // Fallback to regular shared preferences if encryption fails
            context.getSharedPreferences("fallback_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString(key, value)
                    .apply();
        }
    }

    /**
     * Retrieve and decrypt sensitive data
     */
    public String retrieveSensitiveData(String key) {
        if (encryptedSharedPreferences != null) {
            return encryptedSharedPreferences.getString(key, null);
        } else {
            Log.e(TAG, "Encryption not initialized, retrieving from plain text (not recommended)");
            // Fallback to regular shared preferences if encryption fails
            return context.getSharedPreferences("fallback_prefs", Context.MODE_PRIVATE)
                    .getString(key, null);
        }
    }

    /**
     * Store emergency contact information securely
     */
    public void storeEmergencyContact(String name, String number) {
        storeSensitiveData("emergency_contact_name", name);
        storeSensitiveData("emergency_contact_number", number);
        Log.i(TAG, "Emergency contact stored securely");
    }

    /**
     * Retrieve emergency contact information
     */
    public String[] retrieveEmergencyContact() {
        String name = retrieveSensitiveData("emergency_contact_name");
        String number = retrieveSensitiveData("emergency_contact_number");
        return new String[]{name, number};
    }

    /**
     * Store guardian information securely
     */
    public void storeGuardianInfo(String name, String number) {
        storeSensitiveData("guardian_name", name);
        storeSensitiveData("guardian_number", number);
        Log.i(TAG, "Guardian information stored securely");
    }

    /**
     * Retrieve guardian information
     */
    public String[] retrieveGuardianInfo() {
        String name = retrieveSensitiveData("guardian_name");
        String number = retrieveSensitiveData("guardian_number");
        return new String[]{name, number};
    }

    /**
     * Clear all encrypted data
     */
    public void clearAllEncryptedData() {
        if (encryptedSharedPreferences != null) {
            encryptedSharedPreferences.edit().clear().apply();
        }
        Log.i(TAG, "All encrypted data cleared");
    }

    /**
     * Check if encryption is properly initialized
     */
    public boolean isEncryptionReady() {
        return encryptedSharedPreferences != null;
    }
}