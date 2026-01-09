package com.egyptian.agent.hybrid;

import android.content.Context;
import android.util.Log;
import com.egyptian.agent.core.IntentType;
import com.egyptian.agent.nlp.IntentResult;
import com.egyptian.agent.stt.EgyptianNormalizer;
import com.egyptian.agent.utils.CrashLogger;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles fallback to cloud-based processing when local model fails
 * Provides cloud-based NLP processing with Egyptian dialect support
 */
public class CloudFallback {
    private static final String TAG = "CloudFallback";
    private static final String CLOUD_ENDPOINT = "https://api.egyptian-agent.com/process";
    private static final int TIMEOUT_SECONDS = 15;
    
    private final Context context;
    private final OkHttpClient httpClient;
    private boolean isCloudAvailable = true;
    private int fallbackCount = 0;
    private long lastFallbackTime = 0;

    public CloudFallback(Context context) {
        this.context = context;
        
        // Initialize HTTP client with appropriate timeouts
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();
    }

    /**
     * Analyzes text using cloud-based processing
     * @param normalizedText The text to analyze
     * @param callback Callback for the result
     */
    public void analyzeText(String normalizedText, HybridOrchestrator.IntentCallback callback) {
        // Check if we should rate limit cloud requests
        if (shouldRateLimit()) {
            Log.w(TAG, "Cloud request rate limited");
            callback.onIntentDetected(createFallbackResult(normalizedText));
            return;
        }

        // Create request to cloud service
        RequestBody body = RequestBody.create(
            MediaType.get("application/json; charset=utf-8"),
            createRequestJson(normalizedText)
        );

        Request request = new Request.Builder()
            .url(CLOUD_ENDPOINT)
            .post(body)
            .build();

        // Execute request asynchronously
        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e(TAG, "Cloud request failed", e);
                CrashLogger.logError(context, e);
                
                // Update availability status
                isCloudAvailable = false;
                
                // Fallback to local processing or rule-based system
                callback.onIntentDetected(createFallbackResult(normalizedText));
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        IntentResult result = parseCloudResponse(responseBody);
                        
                        // Update availability status
                        isCloudAvailable = true;
                        lastFallbackTime = System.currentTimeMillis();
                        fallbackCount++;
                        
                        Log.d(TAG, "Cloud processing successful: " + result);
                        callback.onIntentDetected(result);
                    } else {
                        Log.e(TAG, "Cloud request failed with code: " + response.code());
                        callback.onIntentDetected(createFallbackResult(normalizedText));
                    }
                } finally {
                    response.close();
                }
            }
        });
    }

    private String createRequestJson(String text) {
        try {
            JSONObject request = new JSONObject();
            request.put("text", text);
            request.put("language", "ar-eg"); // Egyptian Arabic
            request.put("dialect", "egyptian");
            request.put("device_info", getDeviceInfo());
            request.put("timestamp", System.currentTimeMillis());
            
            return request.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error creating request JSON", e);
            // Fallback to minimal request
            return "{\"text\":\"" + text.replace("\"", "\\\"") + "\",\"language\":\"ar-eg\"}";
        }
    }

    private String getDeviceInfo() {
        // In a real implementation, this would gather device information
        // for model optimization and analytics
        return "honor_x6c_android12";
    }

    private IntentResult parseCloudResponse(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            
            IntentResult result = new IntentResult();
            
            // Parse intent
            String intentStr = jsonResponse.optString("intent", "UNKNOWN");
            IntentType intentType = IntentType.valueOf(intentStr.toUpperCase());
            result.setIntentType(intentType);
            
            // Parse entities
            JSONObject entities = jsonResponse.optJSONObject("entities");
            if (entities != null) {
                for (String key : entities.keySet()) {
                    result.setEntity(key, entities.optString(key, ""));
                }
            }
            
            // Parse confidence
            float confidence = (float) jsonResponse.optDouble("confidence", 0.7);
            result.setConfidence(confidence);
            
            // Apply Egyptian dialect post-processing
            EgyptianNormalizer.applyPostProcessingRules(result);
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing cloud response", e);
            return createFallbackResult("Error parsing response");
        }
    }

    private IntentResult createFallbackResult(String text) {
        // Rule-based fallback when cloud is unavailable
        // This uses simple pattern matching similar to the original IntentRouter
        
        IntentResult result = new IntentResult();
        
        // Determine intent based on keywords
        if (text.contains(" emergencies") || text.contains("emergency") || 
            text.contains("ngda") || text.contains("estghatha") || text.contains("tawari")) {
            result.setIntentType(IntentType.EMERGENCY);
            result.setConfidence(0.9f);
        } else if (text.contains("call") || text.contains("connect") || 
                   text.contains("tel") || text.contains("etasel") || 
                   text.contains("klm") || text.contains("rn")) {
            result.setIntentType(IntentType.CALL_CONTACT);
            result.setConfidence(0.8f);
            
            // Extract contact name
            String contact = EgyptianNormalizer.extractContactName(text);
            if (!contact.isEmpty()) {
                result.setEntity("contact", contact);
            }
        } else if (text.contains("whatsapp") || text.contains("message") || 
                   text.contains("wts") || text.contains("rsala") || 
                   text.contains("b3t")) {
            result.setIntentType(IntentType.SEND_WHATSAPP);
            result.setConfidence(0.75f);
        } else if (text.contains("alarm") || text.contains("remind") || 
                   text.contains("nbhny") || text.contains("anbhny") || 
                   text.contains("zkry")) {
            result.setIntentType(IntentType.SET_ALARM);
            result.setConfidence(0.7f);
            
            // Extract time
            String time = EgyptianNormalizer.extractTimeExpression(text);
            if (!time.isEmpty()) {
                result.setEntity("time", time);
            }
        } else if (text.contains("time") || text.contains("hour") || 
                   text.contains("sa3a") || text.contains("kam") || 
                   text.contains("alwqt")) {
            result.setIntentType(IntentType.READ_TIME);
            result.setConfidence(0.95f);
        } else if (text.contains("missed") || text.contains("fa7ta") || 
                   text.contains("fatya")) {
            result.setIntentType(IntentType.READ_MISSED_CALLS);
            result.setConfidence(0.85f);
        } else {
            result.setIntentType(IntentType.UNKNOWN);
            result.setConfidence(0.3f);
        }
        
        return result;
    }

    private boolean shouldRateLimit() {
        // Simple rate limiting: max 10 requests per minute
        long timeSinceLast = System.currentTimeMillis() - lastFallbackTime;
        return fallbackCount > 10 && timeSinceLast < 60000; // 60 seconds
    }

    public boolean isCloudAvailable() {
        return isCloudAvailable;
    }

    public void destroy() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}