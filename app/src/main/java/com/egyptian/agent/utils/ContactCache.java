package com.egyptian.agent.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import com.egyptian.agent.utils.CrashLogger;

public class ContactCache {

    private static final String TAG = "ContactCache";
    private static final String PREF_NAME = "contact_cache";
    private static Map<String, String> cache = new HashMap<>();

    public static void initialize(Context context) {
        try {
            // Load cached contacts from SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            Map<String, ?> allEntries = prefs.getAll();

            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                if (entry.getValue() instanceof String) {
                    cache.put(entry.getKey(), (String) entry.getValue());
                }
            }

            Log.d(TAG, "Loaded " + cache.size() + " contacts from cache");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing contact cache", e);
            CrashLogger.logError(context, e);
        }
    }

    public static String get(Context context, String contactName) {
        try {
            String number = cache.get(contactName.toLowerCase());
            if (number != null) {
                Log.d(TAG, "Cache hit for: " + contactName);
            } else {
                Log.d(TAG, "Cache miss for: " + contactName);
            }
            return number;
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact from cache: " + contactName, e);
            CrashLogger.logError(context, e);
            return null;
        }
    }

    public static void put(Context context, String contactName, String number) {
        try {
            String key = contactName.toLowerCase();
            cache.put(key, number);

            // Persist to SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(key, number);
            editor.apply();

            Log.d(TAG, "Cached contact: " + contactName + " -> " + number);
        } catch (Exception e) {
            Log.e(TAG, "Error putting contact in cache: " + contactName, e);
            CrashLogger.logError(context, e);
        }
    }

    public static void remove(Context context, String contactName) {
        try {
            String key = contactName.toLowerCase();
            cache.remove(key);

            // Remove from SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(key);
            editor.apply();

            Log.d(TAG, "Removed from cache: " + contactName);
        } catch (Exception e) {
            Log.e(TAG, "Error removing contact from cache: " + contactName, e);
            CrashLogger.logError(context, e);
        }
    }

    public static void clear(Context context) {
        try {
            cache.clear();

            // Clear SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            Log.d(TAG, "Cleared contact cache");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing contact cache", e);
            CrashLogger.logError(context, e);
        }
    }
}