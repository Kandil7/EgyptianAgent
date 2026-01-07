package com.egyptian.agent.core;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import com.egyptian.agent.utils.ContactCache;
import com.egyptian.agent.utils.CrashLogger;
import com.egyptian.agent.utils.VibrationManager;

public class MainApplication extends Application {
    
    private static final String TAG = "MainApplication";
    private static Context applicationContext;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        applicationContext = getApplicationContext();
        
        Log.i(TAG, "Egyptian Agent Application starting up");
        
        // Initialize core components
        initializeCoreComponents();
        
        // Register global exception handler
        CrashLogger.registerGlobalExceptionHandler(this);
        
        Log.i(TAG, "Egyptian Agent Application initialized successfully");
    }
    
    private void initializeCoreComponents() {
        // Initialize TTS
        TTSManager.initialize(this);
        
        // Initialize contact cache
        ContactCache.initialize(this);
        
        // Initialize vibration manager
        VibrationManager.initialize(this);
        
        // Initialize crash logger
        CrashLogger.getInstance(this);
        
        Log.i(TAG, "Core components initialized");
    }
    
    public static Context getAppContext() {
        return applicationContext;
    }
}