package com.egyptian.agent.hybrid;

import android.content.Context;
import android.util.Log;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.CrashLogger;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Fallback implementation that uses cloud-based processing when local model is unavailable
 */
public class CloudFallback {
    private static final String TAG = "CloudFallback";
    private static final String CLOUD_ENDPOINT = "https://api.egyptian-agent.dev/process";
    private static final int TIMEOUT_MS = 10000; // 10 seconds
    
    private final Context context;
    private final OkHttpClient httpClient;
    
    public CloudFallback(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .writeTimeout(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build();
    }

    /**
     * Analyzes text using cloud-based processing
     * @param text The input text to analyze
     * @param callback Callback to receive the result
     */
    public void analyzeText(String text, HybridOrchestrator.IntentCallback callback) {
        Log.d(TAG, "Using cloud fallback for text: " + text);
        
        // In senior mode, warn about cloud processing
        if (com.egyptian.agent.accessibility.SeniorMode.isEnabled()) {
            com.egyptian.agent.core.TTSManager.speak(context, "بيستخدم الإنترنت لمعالجة الأمر");
        }
        
        // Process in background thread
        CompletableFuture.supplyAsync(() -> {
            try {
                return callCloudService(text);
            } catch (Exception e) {
                Log.e(TAG, "Cloud service call failed", e);
                CrashLogger.logError(context, e);
                return createFallbackResult(text);
            }
        }).thenAccept(result -> {
            // Return to main thread for callback
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                callback.onIntentDetected(result);
            });
        });
    }

    /**
     * Makes the actual call to the cloud service
     */
    private IntentResult callCloudService(String text) throws Exception {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        // Prepare request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("text", text);
        requestBody.put("dialect", "egyptian");
        requestBody.put("version", "3.0");
        requestBody.put("timestamp", System.currentTimeMillis());
        requestBody.put("device_info", getDeviceInfo());

        RequestBody body = RequestBody.create(requestBody.toString(), JSON);
        Request request = new Request.Builder()
            .url(CLOUD_ENDPOINT)
            .post(body)
            .addHeader("Authorization", "Bearer " + getApiToken()) // In production, use proper auth
            .addHeader("Content-Type", "application/json")
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "Cloud service returned error: " + response.code());
                // Try with a backup endpoint if the primary fails
                return callBackupService(requestBody);
            }

            String responseBody = response.body().string();
            return parseCloudResponse(responseBody);
        } catch (IOException e) {
            Log.e(TAG, "Network error during cloud service call", e);
            // Try with backup service on network errors
            return callBackupService(requestBody);
        }
    }

    /**
     * Calls a backup service endpoint if the primary fails
     */
    private IntentResult callBackupService(JSONObject requestBody) {
        try {
            String backupEndpoint = CLOUD_ENDPOINT.replace("api.", "backup-api.");
            MediaType JSON = MediaType.get("application/json; charset=utf-8");

            RequestBody body = RequestBody.create(requestBody.toString(), JSON);
            Request request = new Request.Builder()
                .url(backupEndpoint)
                .post(body)
                .addHeader("Authorization", "Bearer " + getApiToken())
                .addHeader("Content-Type", "application/json")
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Backup cloud service also failed: " + response.code());
                    return createFallbackResult(requestBody.optString("text", "fallback"));
                }

                String responseBody = response.body().string();
                return parseCloudResponse(responseBody);
            }
        } catch (Exception e) {
            Log.e(TAG, "Backup service also failed", e);
            return createFallbackResult(requestBody.optString("text", "fallback"));
        }
    }

    /**
     * Gets device information for the request
     */
    private JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();
        try {
            deviceInfo.put("model", android.os.Build.MODEL);
            deviceInfo.put("manufacturer", android.os.Build.MANUFACTURER);
            deviceInfo.put("sdk_version", android.os.Build.VERSION.SDK_INT);
            deviceInfo.put("device_id", android.provider.Settings.Secure.getString(
                context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID
            ));
        } catch (Exception e) {
            Log.e(TAG, "Error getting device info", e);
        }
        return deviceInfo;
    }

    /**
     * Gets API token for authentication (in production, this would be securely stored)
     */
    private String getApiToken() {
        // In a real implementation, this would retrieve a securely stored token
        // For now, return a placeholder
        return "placeholder_token";
    }

    /**
     * Parses the response from the cloud service
     */
    private IntentResult parseCloudResponse(String jsonResponse) {
        try {
            JSONObject responseObj = new JSONObject(jsonResponse);
            
            IntentResult result = new IntentResult();
            
            // Parse intent
            String intentStr = responseObj.optString("intent", "UNKNOWN");
            result.setIntentType(IntentType.fromOpenPhoneString(intentStr));
            
            // Parse entities
            JSONObject entities = responseObj.optJSONObject("entities");
            if (entities != null) {
                for (String key : entities.keySet()) {
                    String value = entities.optString(key, "");
                    result.setEntity(key, value);
                }
            }
            
            // Parse confidence
            float confidence = responseObj.optFloat("confidence", 0.7f);
            result.setConfidence(confidence);
            
            // Apply Egyptian post-processing
            EgyptianNormalizer.applyPostProcessingRules(result);
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing cloud response", e);
            return createFallbackResult("fallback");
        }
    }

    /**
     * Creates a fallback result when cloud processing fails
     */
    private IntentResult createFallbackResult(String text) {
        Log.w(TAG, "Creating fallback result for: " + text);
        
        IntentResult result = new IntentResult();
        result.setIntentType(IntentType.UNKNOWN);
        result.setConfidence(0.3f); // Lower confidence for fallback
        
        // Try basic local processing as a last resort
        if (text.contains("اتصل") || text.contains("كلم") || text.contains("رن")) {
            result.setIntentType(IntentType.CALL_CONTACT);
            result.setConfidence(0.5f);
        } else if (text.contains("واتساب") || text.contains("ابعت") || text.contains("رساله")) {
            result.setIntentType(IntentType.SEND_WHATSAPP);
            result.setConfidence(0.5f);
        } else if (text.contains("انبهني") || text.contains("نبهني") || text.contains("ذكرني")) {
            result.setIntentType(IntentType.SET_ALARM);
            result.setConfidence(0.5f);
        } else if (text.contains("الوقت") || text.contains("الساعه") || text.contains("كام")) {
            result.setIntentType(IntentType.READ_TIME);
            result.setConfidence(0.7f);
        }
        
        return result;
    }

    /**
     * Cleans up resources
     */
    public void destroy() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}