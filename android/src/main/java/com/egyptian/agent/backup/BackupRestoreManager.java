package com.egyptian.agent.backup;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.egyptian.agent.security.DataEncryptionManager;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.CrashLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Backup and restore utility for user data and settings
 * Securely backs up sensitive information with encryption
 */
public class BackupRestoreManager {
    private static final String TAG = "BackupRestoreManager";
    private static final String BACKUP_DIRECTORY = "egyptian_agent_backup";
    private static final String BACKUP_FILE_PREFIX = "egyptian_agent_backup_";
    private static final String BACKUP_FILE_EXTENSION = ".zip";
    
    private static BackupRestoreManager instance;
    private Context context;
    private DataEncryptionManager encryptionManager;
    
    private BackupRestoreManager(Context context) {
        this.context = context.getApplicationContext();
        this.encryptionManager = DataEncryptionManager.getInstance(context);
    }
    
    public static synchronized BackupRestoreManager getInstance(Context context) {
        if (instance == null) {
            instance = new BackupRestoreManager(context);
        }
        return instance;
    }
    
    /**
     * Create a backup of user data and settings
     */
    public boolean createBackup() {
        try {
            // Create backup directory
            File backupDir = new File(context.getExternalFilesDir(null), BACKUP_DIRECTORY);
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            // Generate backup filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String backupFileName = BACKUP_FILE_PREFIX + timestamp + BACKUP_FILE_EXTENSION;
            File backupFile = new File(backupDir, backupFileName);
            
            // Create the backup file
            createBackupFile(backupFile);
            
            Log.i(TAG, "Backup created successfully: " + backupFile.getAbsolutePath());
            TTSManager.speak(context, "النسخة الاحتياطية اتمت بنجاح.");
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create backup", e);
            CrashLogger.logError(context, e);
            TTSManager.speak(context, "فشل في إنشاء النسخة الاحتياطية.");
            return false;
        }
    }
    
    /**
     * Create the actual backup file with encrypted data
     */
    private void createBackupFile(File backupFile) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(backupFile))) {
            // Add emergency contact info
            addEmergencyContactToBackup(zipOut);
            
            // Add guardian info
            addGuardianInfoToBackup(zipOut);
            
            // Add preferences
            addPreferencesToBackup(zipOut);
            
            // Add any other sensitive data that needs backup
            addOtherDataToBackup(zipOut);
        }
    }
    
    /**
     * Add emergency contact info to backup
     */
    private void addEmergencyContactToBackup(ZipOutputStream zipOut) throws IOException {
        String[] emergencyContact = encryptionManager.retrieveEmergencyContact();
        if (emergencyContact[0] != null && emergencyContact[1] != null) {
            String data = emergencyContact[0] + "|" + emergencyContact[1];
            addToZip(zipOut, "emergency_contact.txt", data.getBytes());
        }
    }
    
    /**
     * Add guardian info to backup
     */
    private void addGuardianInfoToBackup(ZipOutputStream zipOut) throws IOException {
        String[] guardianInfo = encryptionManager.retrieveGuardianInfo();
        if (guardianInfo[0] != null && guardianInfo[1] != null) {
            String data = guardianInfo[0] + "|" + guardianInfo[1];
            addToZip(zipOut, "guardian_info.txt", data.getBytes());
        }
    }
    
    /**
     * Add preferences to backup
     */
    private void addPreferencesToBackup(ZipOutputStream zipOut) throws IOException {
        // Add any other preferences that need to be backed up
        // This could include senior mode settings, wake word preferences, etc.
        android.content.SharedPreferences prefs = context.getSharedPreferences("egyptian_agent_prefs", Context.MODE_PRIVATE);
        StringBuilder prefsData = new StringBuilder();
        
        // Add senior mode setting
        boolean seniorMode = prefs.getBoolean("senior_mode_enabled", false);
        prefsData.append("senior_mode_enabled=").append(seniorMode).append("\n");
        
        // Add speech rate
        float speechRate = prefs.getFloat("speech_rate", 1.0f);
        prefsData.append("speech_rate=").append(speechRate).append("\n");
        
        // Add other preferences as needed
        addToZip(zipOut, "preferences.txt", prefsData.toString().getBytes());
    }
    
    /**
     * Add other data to backup
     */
    private void addOtherDataToBackup(ZipOutputStream zipOut) throws IOException {
        // Add any other data that needs to be backed up
        // For now, just add a timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String data = "backup_timestamp=" + timestamp + "\n";
        addToZip(zipOut, "metadata.txt", data.getBytes());
    }
    
    /**
     * Add data to zip file
     */
    private void addToZip(ZipOutputStream zipOut, String fileName, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(fileName);
        zipOut.putNextEntry(entry);
        zipOut.write(data);
        zipOut.closeEntry();
    }
    
    /**
     * Restore from a backup file
     */
    public boolean restoreFromBackup(String backupFilePath) {
        try {
            File backupFile = new File(backupFilePath);
            if (!backupFile.exists()) {
                Log.e(TAG, "Backup file does not exist: " + backupFilePath);
                TTSManager.speak(context, "ملف النسخة الاحتياطية مش موجود.");
                return false;
            }
            
            // Extract and restore the backup
            restoreFromBackupFile(backupFile);
            
            Log.i(TAG, "Backup restored successfully from: " + backupFilePath);
            TTSManager.speak(context, "النسخة الاحتياطية اترممت بنجاح.");
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore backup", e);
            CrashLogger.logError(context, e);
            TTSManager.speak(context, "فشل في استعادة النسخة الاحتياطية.");
            return false;
        }
    }
    
    /**
     * Restore from the actual backup file
     */
    private void restoreFromBackupFile(File backupFile) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(backupFile))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                String fileName = entry.getName();
                
                // Read the entry content
                byte[] buffer = new byte[1024];
                int bytesRead;
                StringBuilder content = new StringBuilder();
                
                while ((bytesRead = zipIn.read(buffer)) != -1) {
                    content.append(new String(buffer, 0, bytesRead));
                }
                
                // Process the entry based on its name
                processBackupEntry(fileName, content.toString());
                
                zipIn.closeEntry();
            }
        }
    }
    
    /**
     * Process a backup entry
     */
    private void processBackupEntry(String fileName, String content) {
        switch (fileName) {
            case "emergency_contact.txt":
                restoreEmergencyContact(content);
                break;
            case "guardian_info.txt":
                restoreGuardianInfo(content);
                break;
            case "preferences.txt":
                restorePreferences(content);
                break;
            default:
                // Handle other entries as needed
                break;
        }
    }
    
    /**
     * Restore emergency contact from backup content
     */
    private void restoreEmergencyContact(String content) {
        String[] parts = content.split("\\|");
        if (parts.length >= 2) {
            encryptionManager.storeEmergencyContact(parts[0].trim(), parts[1].trim());
            Log.i(TAG, "Emergency contact restored from backup");
        }
    }
    
    /**
     * Restore guardian info from backup content
     */
    private void restoreGuardianInfo(String content) {
        String[] parts = content.split("\\|");
        if (parts.length >= 2) {
            encryptionManager.storeGuardianInfo(parts[0].trim(), parts[1].trim());
            Log.i(TAG, "Guardian info restored from backup");
        }
    }
    
    /**
     * Restore preferences from backup content
     */
    private void restorePreferences(String content) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("egyptian_agent_prefs", Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("senior_mode_enabled=")) {
                boolean enabled = Boolean.parseBoolean(line.substring("senior_mode_enabled=".length()));
                editor.putBoolean("senior_mode_enabled", enabled);
            } else if (line.startsWith("speech_rate=")) {
                float rate = Float.parseFloat(line.substring("speech_rate=".length()));
                editor.putFloat("speech_rate", rate);
            }
        }
        
        editor.apply();
        Log.i(TAG, "Preferences restored from backup");
    }
    
    /**
     * Get list of available backups
     */
    public File[] getAvailableBackups() {
        File backupDir = new File(context.getExternalFilesDir(null), BACKUP_DIRECTORY);
        if (!backupDir.exists()) {
            return new File[0];
        }
        
        File[] files = backupDir.listFiles((dir, name) -> 
            name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION));
        
        if (files == null) {
            return new File[0];
        }
        
        // Sort by modification time (newest first)
        java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        
        return files;
    }
    
    /**
     * Delete a specific backup file
     */
    public boolean deleteBackup(String backupFilePath) {
        File backupFile = new File(backupFilePath);
        if (backupFile.exists()) {
            boolean deleted = backupFile.delete();
            if (deleted) {
                Log.i(TAG, "Backup file deleted: " + backupFilePath);
                TTSManager.speak(context, "النسخة الاحتياطية اتمسحت.");
            } else {
                Log.e(TAG, "Failed to delete backup file: " + backupFilePath);
                TTSManager.speak(context, "فشل في مسح النسخة الاحتياطية.");
            }
            return deleted;
        } else {
            Log.w(TAG, "Backup file does not exist: " + backupFilePath);
            TTSManager.speak(context, "النسخة الاحتياطية مش موجودة.");
            return false;
        }
    }
    
    /**
     * Get the size of a backup file in human-readable format
     */
    public String getBackupFileSize(String backupFilePath) {
        File backupFile = new File(backupFilePath);
        if (backupFile.exists()) {
            long sizeBytes = backupFile.length();
            return formatFileSize(sizeBytes);
        }
        return "N/A";
    }
    
    /**
     * Format file size in human-readable format
     */
    private String formatFileSize(long sizeBytes) {
        if (sizeBytes < 1024) return sizeBytes + " B";
        if (sizeBytes < 1024 * 1024) return String.format("%.2f KB", sizeBytes / 1024.0);
        if (sizeBytes < 1024 * 1024 * 1024) return String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0));
    }
}