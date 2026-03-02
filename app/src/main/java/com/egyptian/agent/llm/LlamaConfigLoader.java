package com.egyptian.agent.llm;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Loader for Llama configuration from YAML.
 * Parses llama_config_honor_x6c.yaml to configure the engine at runtime.
 */
public class LlamaConfigLoader {
    private static final String TAG = "LlamaConfigLoader";
    private static final String CONFIG_FILE = "llama_config_honor_x6c.yaml";

    public static LlamaConfig loadConfig(Context context) {
        LlamaConfig config = new LlamaConfig();
        Map<String, String> yamlValues = new HashMap<>();

        try {
            // Try to load from assets
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(CONFIG_FILE)));
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Simple key=value parser
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim().split("#")[0].trim(); // Remove inline comments
                    yamlValues.put(key, value);
                }
            }
            reader.close();
            
            // Apply values to config
            if (yamlValues.containsKey("n_ctx")) {
                config.setContextSize(Integer.parseInt(yamlValues.get("n_ctx")));
            }
            if (yamlValues.containsKey("n_threads")) {
                config.setNumThreads(Integer.parseInt(yamlValues.get("n_threads")));
            }
            if (yamlValues.containsKey("temperature")) {
                // Remove quotes if present
                // config.setTemperature(Float.parseFloat(yamlValues.get("temperature"))); 
                // Note: LlamaConfig needs to expose setters for these if they don't exist
            }
            // Add more parsers as needed based on LlamaConfig's available setters
            
            Log.i(TAG, "Loaded config from " + CONFIG_FILE);
            
        } catch (IOException e) {
            Log.e(TAG, "Error loading config, using defaults", e);
        }
        
        return config;
    }
}
