package com.egyptian.agent.ui;

import android.app.Application;
import android.util.Log;

/**
 * Egyptian Agent Application
 * Standalone version without core dependencies
 */
public class EgyptianAgentApplication extends Application {
    private static final String TAG = "EgyptianAgent";
    private static EgyptianAgentApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "Egyptian Agent Application starting");
        
        // Initialize any required components here
        initializeApp();
    }

    private void initializeApp() {
        Log.i(TAG, "App initialized successfully");
    }

    public static EgyptianAgentApplication getInstance() {
        return instance;
    }
}