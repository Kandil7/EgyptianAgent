package com.egyptian.agent.core;

import android.app.Application;
import android.util.Log;
import com.egyptian.agent.utils.CrashLogger;
import com.egyptian.agent.system.SystemPrivilegeManager;

public class MainApplication extends Application {

    private static final String TAG = "MainApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Egyptian Agent Application starting");

        // Initialize crash logger
        CrashLogger.registerGlobalExceptionHandler(this);

        // Initialize system privileges via Shizuku
        SystemPrivilegeManager.initialize(this);

        // Initialize TTS engine
        TTSManager.initialize(this);

        // Additional initialization can go here
        initializeServices();
    }

    private void initializeServices() {
        // Initialize any background services
        Log.i(TAG, "All services initialized");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Cleanup resources
        TTSManager.shutdown();
        SystemPrivilegeManager.cleanup();
        Log.i(TAG, "Egyptian Agent Application terminated");
    }
}