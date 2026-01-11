package com.egyptian.agent.ui;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.egyptian.agent.R;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.core.VoiceService;
import com.egyptian.agent.core.TTSManager;

/**
 * Main Activity
 * Entry point for the Egyptian Agent application
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    
    private TextView statusTextView;
    private Button startServiceButton;
    private Button stopServiceButton;
    private Button seniorModeButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize views
        statusTextView = findViewById(R.id.statusTextView);
        startServiceButton = findViewById(R.id.startServiceButton);
        stopServiceButton = findViewById(R.id.stopServiceButton);
        seniorModeButton = findViewById(R.id.seniorModeButton);
        
        // Initialize TTS
        TTSManager.initialize(this);
        
        // Initialize Senior Mode
        SeniorMode.initialize(this);
        
        // Set up button listeners
        setupButtonListeners();
        
        // Update UI based on service status
        updateServiceStatus();
    }
    
    /**
     * Sets up button listeners
     */
    private void setupButtonListeners() {
        startServiceButton.setOnClickListener(v -> startVoiceService());
        stopServiceButton.setOnClickListener(v -> stopVoiceService());
        seniorModeButton.setOnClickListener(v -> toggleSeniorMode());
    }
    
    /**
     * Starts the voice service
     */
    private void startVoiceService() {
        try {
            Intent serviceIntent = new Intent(this, VoiceService.class);
            startService(serviceIntent);
            Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show();
            updateServiceStatus();
        } catch (Exception e) {
            Log.e(TAG, "Error starting voice service", e);
            Toast.makeText(this, "Error starting service", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Stops the voice service
     */
    private void stopVoiceService() {
        try {
            Intent serviceIntent = new Intent(this, VoiceService.class);
            stopService(serviceIntent);
            Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show();
            updateServiceStatus();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping voice service", e);
            Toast.makeText(this, "Error stopping service", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Toggles senior mode
     */
    private void toggleSeniorMode() {
        if (SeniorMode.isEnabled()) {
            SeniorMode.disable(this);
            Toast.makeText(this, "Senior mode disabled", Toast.LENGTH_SHORT).show();
        } else {
            SeniorMode.enable(this);
            Toast.makeText(this, "Senior mode enabled", Toast.LENGTH_SHORT).show();
        }
        
        // Update button text
        updateSeniorModeButtonText();
    }
    
    /**
     * Updates the service status display
     */
    private void updateServiceStatus() {
        boolean isServiceRunning = isMyServiceRunning(VoiceService.class);
        if (isServiceRunning) {
            statusTextView.setText("Egyptian Agent is running\nService is active");
        } else {
            statusTextView.setText("Egyptian Agent is ready\nService is not running");
        }
    }

    /**
     * Checks if a service is currently running
     * @param serviceClass The class of the service to check
     * @return true if the service is running, false otherwise
     */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        android.app.ActivityManager manager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Updates the senior mode button text
     */
    private void updateSeniorModeButtonText() {
        if (SeniorMode.isEnabled()) {
            seniorModeButton.setText("Disable Senior Mode");
        } else {
            seniorModeButton.setText("Enable Senior Mode");
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
        updateSeniorModeButtonText();
    }
}