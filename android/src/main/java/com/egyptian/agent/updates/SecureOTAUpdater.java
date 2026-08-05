package com.egyptian.agent.updates;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HttpsURLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SecureOTAUpdater - Handles secure over-the-air updates for the Egyptian Agent
 * Implements secure download, verification, and installation of updates
 */
public class SecureOTAUpdater {
    private static final String TAG = "SecureOTAUpdater";
    private static final String UPDATE_URL = "https://egyptianagent.example.com/api/update"; // Placeholder URL
    private static final String UPDATE_CHANNEL = "stable"; // Options: stable, beta, alpha
    
    private Context context;
    private ExecutorService executor;
    private UpdateListener listener;
    
    public SecureOTAUpdater(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Sets the update listener to receive update events
     */
    public void setUpdateListener(UpdateListener listener) {
        this.listener = listener;
    }
    
    /**
     * Checks for available updates
     */
    public void checkForUpdates() {
        executor.execute(() -> {
            try {
                UpdateInfo latestUpdate = fetchLatestUpdateInfo();
                
                if (latestUpdate != null && isNewerVersion(latestUpdate.getVersionCode())) {
                    if (listener != null) {
                        listener.onUpdateAvailable(latestUpdate);
                    }
                } else {
                    if (listener != null) {
                        listener.onNoUpdateAvailable();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking for updates", e);
                if (listener != null) {
                    listener.onError("Error checking for updates: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Downloads and installs an update
     */
    public void downloadAndInstallUpdate(UpdateInfo updateInfo) {
        executor.execute(() -> {
            try {
                if (listener != null) {
                    listener.onDownloadStarted();
                }
                
                // Download the update
                File downloadedFile = downloadUpdate(updateInfo.getDownloadUrl(), updateInfo.getVersionCode());
                
                // Verify the download
                if (!verifyDownload(downloadedFile, updateInfo.getChecksum())) {
                    throw new SecurityException("Download verification failed");
                }
                
                if (listener != null) {
                    listener.onDownloadCompleted();
                }
                
                // Install the update
                installUpdate(downloadedFile);
                
                if (listener != null) {
                    listener.onUpdateInstalled();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error downloading or installing update", e);
                if (listener != null) {
                    listener.onError("Error with update: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Fetches information about the latest available update
     */
    private UpdateInfo fetchLatestUpdateInfo() throws IOException {
        URL url = new URL(UPDATE_URL + "?channel=" + UPDATE_CHANNEL + "&current_version=" + getCurrentVersionCode());
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "EgyptianAgent/" + getCurrentVersionCode());
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                String response = convertStreamToString(inputStream);
                
                // Parse the response (simplified - in real implementation, use JSON parsing)
                // This is a simplified implementation - real implementation would parse JSON properly
                return new UpdateInfo("1.1.0", 2, "https://example.com/update.apk", "checksum123");
            } else {
                throw new IOException("HTTP error code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Downloads the update APK
     */
    private File downloadUpdate(String downloadUrl, int versionCode) throws IOException {
        URL url = new URL(downloadUrl);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "EgyptianAgent/" + getCurrentVersionCode());
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String fileName = "update_" + versionCode + ".apk";
                File outputFile = new File(context.getCacheDir(), fileName);
                
                InputStream inputStream = connection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(outputFile);
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesRead = 0;
                long fileSize = connection.getContentLength();
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    // Notify progress
                    if (fileSize > 0 && listener != null) {
                        int progress = (int) ((totalBytesRead * 100) / fileSize);
                        listener.onDownloadProgress(progress);
                    }
                }
                
                outputStream.close();
                inputStream.close();
                
                return outputFile;
            } else {
                throw new IOException("Download failed with HTTP code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Verifies the downloaded file using checksum
     */
    private boolean verifyDownload(File downloadedFile, String expectedChecksum) throws NoSuchAlgorithmException, IOException {
        String actualChecksum = calculateMD5Checksum(downloadedFile);
        return expectedChecksum.equalsIgnoreCase(actualChecksum);
    }
    
    /**
     * Calculates MD5 checksum of a file
     */
    private String calculateMD5Checksum(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream inputStream = java.nio.file.Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        
        return sb.toString();
    }
    
    /**
     * Installs the downloaded update
     */
    private void installUpdate(File updateFile) {
        // For system apps, installation requires special handling
        // This is a simplified implementation - real implementation would use proper system update mechanisms
        
        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setDataAndType(android.net.Uri.fromFile(updateFile), "application/vnd.android.package-archive");
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        context.startActivity(installIntent);
    }
    
    /**
     * Gets the current app version code
     */
    private int getCurrentVersionCode() {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get current version code", e);
            return 1; // Default to version 1 if unable to determine
        }
    }
    
    /**
     * Checks if the provided version code is newer than current
     */
    private boolean isNewerVersion(int newVersionCode) {
        return newVersionCode > getCurrentVersionCode();
    }
    
    /**
     * Converts input stream to string
     */
    private String convertStreamToString(InputStream is) throws IOException {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
    
    /**
     * Interface for receiving update events
     */
    public interface UpdateListener {
        void onUpdateAvailable(UpdateInfo updateInfo);
        void onNoUpdateAvailable();
        void onDownloadStarted();
        void onDownloadProgress(int progress);
        void onDownloadCompleted();
        void onUpdateInstalled();
        void onError(String errorMessage);
    }
    
    /**
     * Holds information about an update
     */
    public static class UpdateInfo {
        private String versionName;
        private int versionCode;
        private String downloadUrl;
        private String checksum;
        private String changelog;
        
        public UpdateInfo(String versionName, int versionCode, String downloadUrl, String checksum) {
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.downloadUrl = downloadUrl;
            this.checksum = checksum;
        }
        
        // Getters
        public String getVersionName() { return versionName; }
        public int getVersionCode() { return versionCode; }
        public String getDownloadUrl() { return downloadUrl; }
        public String getChecksum() { return checksum; }
        public String getChangelog() { return changelog; }
        
        // Setters
        public void setChangelog(String changelog) { this.changelog = changelog; }
    }
}