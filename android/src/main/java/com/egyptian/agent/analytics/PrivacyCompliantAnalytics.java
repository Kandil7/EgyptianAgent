package com.egyptian.agent.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Privacy-compliant analytics system for Egyptian Agent
 * Collects usage data while protecting user privacy
 */
public class PrivacyCompliantAnalytics {
    private static final String TAG = "Analytics";
    private static final String ANALYTICS_PREFS = "analytics_prefs";
    private static final String ANALYTICS_DATA_KEY = "analytics_data";
    private static final int MAX_EVENTS = 100; // Maximum number of events to store

    private static PrivacyCompliantAnalytics instance;
    private SharedPreferences prefs;
    private ExecutorService executor;
    private Context context;

    private PrivacyCompliantAnalytics(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(ANALYTICS_PREFS, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized PrivacyCompliantAnalytics getInstance(Context context) {
        if (instance == null) {
            instance = new PrivacyCompliantAnalytics(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Logs an event without collecting personal information
     */
    public void logEvent(String eventName, Map<String, Object> properties) {
        executor.execute(() -> {
            try {
                // Sanitize properties to ensure no PII is collected
                Map<String, Object> sanitizedProps = sanitizeProperties(properties);

                // Create event object
                Map<String, Object> event = new HashMap<>();
                event.put("name", eventName);
                event.put("timestamp", System.currentTimeMillis());
                event.put("properties", sanitizedProps);

                // Store event
                storeEvent(event);

                Log.d(TAG, "Logged event: " + eventName);
            } catch (Exception e) {
                Log.e(TAG, "Error logging event", e);
            }
        });
    }

    /**
     * Logs a simple event without properties
     */
    public void logEvent(String eventName) {
        logEvent(eventName, new HashMap<>());
    }

    /**
     * Sanitizes properties to ensure no personally identifiable information is collected
     */
    private Map<String, Object> sanitizeProperties(Map<String, Object> properties) {
        Map<String, Object> sanitized = new HashMap<>();

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Skip any properties that might contain PII
            if (isPIIProperty(key)) {
                continue;
            }

            // Sanitize the value if it's a string
            if (value instanceof String) {
                String stringValue = (String) value;
                if (containsPII(stringValue)) {
                    continue; // Skip this property entirely
                }
                sanitized.put(key, stringValue);
            } else {
                // For non-string values, add them directly if they're not PII-related
                sanitized.put(key, value);
            }
        }

        return sanitized;
    }

    /**
     * Checks if a property key might contain PII
     */
    private boolean isPIIProperty(String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("name") ||
               lowerKey.contains("phone") ||
               lowerKey.contains("number") ||
               lowerKey.contains("email") ||
               lowerKey.contains("address") ||
               lowerKey.contains("location") ||
               lowerKey.contains("contact") ||
               lowerKey.contains("message") ||
               lowerKey.contains("content");
    }

    /**
     * Checks if a string value contains potential PII
     */
    private boolean containsPII(String value) {
        // Basic check for phone number patterns (Egyptian numbers typically start with 01x)
        if (value.matches(".*01[0-9]{9}.*")) {
            return true;
        }

        // Check for email patterns
        if (value.matches(".*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*")) {
            return true;
        }

        // Check for potential Arabic names (basic heuristic)
        // This is a simplified check - in production, you'd want more sophisticated PII detection
        if (value.length() < 50 && value.matches(".*[\\u0600-\\u06FF\\s]+.*")) {
            // Contains Arabic characters, could be a name
            // For now, we'll allow short Arabic text but flag longer sequences
            String[] words = value.trim().split("\\s+");
            if (words.length > 5) {
                return true; // Likely contains personal information
            }
        }

        return false;
    }

    /**
     * Stores an event in shared preferences
     */
    private void storeEvent(Map<String, Object> event) {
        try {
            // Retrieve existing events
            String existingJson = prefs.getString(ANALYTICS_DATA_KEY, "[]");
            JSONArray jsonArray = new JSONArray(existingJson);

            // Add new event
            JSONObject eventJson = new JSONObject(event);
            jsonArray.put(eventJson);

            // Limit the number of stored events
            while (jsonArray.length() > MAX_EVENTS) {
                // Remove oldest event (at index 0)
                jsonArray = removeJsonAtIndex(jsonArray, 0);
            }

            // Save back to preferences
            prefs.edit().putString(ANALYTICS_DATA_KEY, jsonArray.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error storing event", e);
        }
    }

    /**
     * Removes an item at a specific index from JSONArray (helper method)
     */
    private JSONArray removeJsonAtIndex(JSONArray jsonArray, int index) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < jsonArray.length(); i++) {
            if (i != index) {
                try {
                    result.put(jsonArray.get(i));
                } catch (Exception e) {
                    Log.e(TAG, "Error removing JSON at index", e);
                }
            }
        }
        return result;
    }

    /**
     * Gets analytics data for reporting (without PII)
     */
    public String getAnalyticsReport() {
        try {
            String jsonStr = prefs.getString(ANALYTICS_DATA_KEY, "[]");
            JSONArray jsonArray = new JSONArray(jsonStr);

            StringBuilder report = new StringBuilder();
            report.append("Egyptian Agent Analytics Report\n");
            report.append("Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
            report.append("Total Events: ").append(jsonArray.length()).append("\n\n");

            // Count event types
            Map<String, Integer> eventTypeCounts = new HashMap<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject event = jsonArray.getJSONObject(i);
                    String eventName = event.getString("name");
                    eventTypeCounts.put(eventName, eventTypeCounts.getOrDefault(eventName, 0) + 1);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing event for report", e);
                }
            }

            // Add counts to report
            for (Map.Entry<String, Integer> entry : eventTypeCounts.entrySet()) {
                report.append(entry.getKey()).append(": ").append(entry.getValue()).append(" times\n");
            }

            return report.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error generating analytics report", e);
            return "Error generating report: " + e.getMessage();
        }
    }

    /**
     * Clears all analytics data
     */
    public void clearAnalyticsData() {
        executor.execute(() -> {
            prefs.edit().remove(ANALYTICS_DATA_KEY).apply();
            Log.d(TAG, "Analytics data cleared");
        });
    }

    /**
     * Gets the count of specific event type
     */
    public int getEventCount(String eventName) {
        try {
            String jsonStr = prefs.getString(ANALYTICS_DATA_KEY, "[]");
            JSONArray jsonArray = new JSONArray(jsonStr);

            int count = 0;
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject event = jsonArray.getJSONObject(i);
                    if (eventName.equals(event.getString("name"))) {
                        count++;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error counting events", e);
                }
            }

            return count;
        } catch (Exception e) {
            Log.e(TAG, "Error getting event count", e);
            return 0;
        }
    }
}