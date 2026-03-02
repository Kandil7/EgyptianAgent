package com.egyptian.agent.service;

import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionService;
import android.service.voice.VoiceInteractionSession;
import android.service.voice.VoiceInteractionSessionService;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.nlu.IntentResult;
import com.egyptian.agent.nlu.NLUManager;
import com.egyptian.agent.wakeword.WakeWordManager;
import com.egyptian.agent.executor.CommandExecutor;

/**
 * Egyptian Agent Voice Interaction Service
 * 
 * System-level voice interaction service that integrates with Android's
 * voice assistant framework. Allows the agent to be set as the default
 * voice assistant and work with home button / gesture activation.
 * 
 * Features:
 * - System-level integration
 * - Home button activation
 * - VoiceInteractionSession support
 * - Background operation
 */
public class EgyptianAgentService extends VoiceInteractionService {
    private static final String TAG = "EgyptianAgentService";
    
    private NLUManager nluManager;
    private WakeWordManager wakeWordManager;
    private CommandExecutor commandExecutor;
    
    private boolean isReady;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "VoiceInteractionService created");
        
        initializeComponents();
    }
    
    /**
     * Initialize core components.
     */
    private void initializeComponents() {
        try {
            // Initialize NLU manager
            nluManager = NLUManager.getInstance(this);
            nluManager.initialize(true);
            
            // Initialize wake word manager
            wakeWordManager = WakeWordManager.getInstance(this);
            wakeWordManager.initialize();
            
            // Initialize command executor
            commandExecutor = CommandExecutor.getInstance(this);
            
            isReady = true;
            Log.i(TAG, "All components initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing components", e);
            isReady = false;
        }
    }
    
    @Override
    public void onReady() {
        super.onReady();
        Log.i(TAG, "VoiceInteractionService ready");
        
        // Start wake word detection
        if (wakeWordManager != null) {
            wakeWordManager.setCallback(wakeWordCallback);
            wakeWordManager.start();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "VoiceInteractionService destroyed");
        
        if (wakeWordManager != null) {
            wakeWordManager.destroy();
        }
        
        if (nluManager != null) {
            nluManager.destroy();
        }
        
        if (commandExecutor != null) {
            commandExecutor.destroy();
        }
    }
    
    @Override
    public VoiceInteractionSession onCreateNewSession() {
        Log.d(TAG, "Creating new voice interaction session");
        return new EgyptianAgentSession(this);
    }
    
    @Override
    public void onShow() {
        super.onShow();
        Log.d(TAG, "Voice interaction shown");
    }
    
    @Override
    public void onHide() {
        super.onHide();
        Log.d(TAG, "Voice interaction hidden");
    }
    
    /**
     * Handle wake word detection.
     */
    private final com.egyptian.agent.wakeword.WakeWordCallback wakeWordCallback = 
        new com.egyptian.agent.wakeword.WakeWordCallback() {
        @Override
        public void onWakeWordDetected(String wakeWord, float confidence) {
            Log.i(TAG, "Wake word detected: " + wakeWord + " (confidence: " + confidence + ")");
            
            // Show voice interaction UI
            showSession();
            
            // Speak confirmation
            if ("يا كبير".equals(wakeWord)) {
                TTSManager.speak(EgyptianAgentService.this, "أوامرك يا كبير");
            } else {
                TTSManager.speak(EgyptianAgentService.this, "أوامرك؟");
            }
        }
        
        @Override
        public void onError(Exception error) {
            Log.e(TAG, "Wake word error: " + error.getMessage());
        }
        
        @Override
        public void onStateChanged(boolean isListening) {
            Log.d(TAG, "Wake word state changed: " + isListening);
        }
    };
    
    /**
     * Process voice command.
     */
    public void processCommand(String command) {
        if (!isReady || nluManager == null) {
            Log.w(TAG, "Service not ready");
            return;
        }
        
        Log.d(TAG, "Processing command: " + command);
        
        // Classify intent
        IntentResult result = nluManager.classify(command);
        
        // Execute command
        if (commandExecutor != null) {
            commandExecutor.execute(result);
        }
    }
    
    /**
     * Check if service is ready.
     */
    public boolean isReady() {
        return isReady;
    }
}
