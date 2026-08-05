package com.egyptian.agent.service;

import android.content.Context;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.egyptian.agent.R;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.nlu.IntentResult;
import com.egyptian.agent.nlu.NLUManager;
import com.egyptian.agent.executor.CommandExecutor;

/**
 * Egyptian Agent Voice Interaction Session
 *
 * Handles individual voice interaction sessions.
 * Manages the UI and interaction flow for each voice command.
 */
public class EgyptianAgentSession extends VoiceInteractionSession {
    private static final String TAG = "EgyptianAgentSession";

    private NLUManager nluManager;
    private CommandExecutor commandExecutor;
    private View contentView;

    private final android.os.Handler mainHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private boolean isListening;
    private boolean isProcessing;

    public EgyptianAgentSession(Context context) {
        super(context);
        Log.d(TAG, "Session created");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Session onCreate");

        nluManager = NLUManager.getInstance(getContext());
        commandExecutor = CommandExecutor.getInstance(getContext());
    }

    @Override
    public View onCreateContentView() {
        Log.d(TAG, "Creating content view");

        LayoutInflater inflater = LayoutInflater.from(getContext());
        contentView = inflater.inflate(R.layout.voice_session_overlay, null);

        return contentView;
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        Log.d(TAG, "Session onShow, flags=" + showFlags);

        // Start listening as soon as the session is shown.
        startListening();
    }

    @Override
    public void onHide() {
        super.onHide();
        Log.d(TAG, "Session onHide");

        // Stop listening
        stopListening();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Session destroyed");
    }
    
    /**
     * Start listening for voice input.
     */
    public void startListening() {
        if (isListening) return;
        
        isListening = true;
        isProcessing = false;
        
        Log.d(TAG, "Started listening");
        
        // Update UI to show listening state
        updateListeningState(true);
        
        // Speak prompt
        TTSManager.speak(getContext(), "أوامرك؟");
    }
    
    /**
     * Stop listening.
     */
    public void stopListening() {
        isListening = false;
        
        Log.d(TAG, "Stopped listening");
        
        // Update UI
        updateListeningState(false);
    }
    
    /**
     * Process voice input.
     */
    public void processVoiceInput(String text) {
        if (isProcessing) return;
        
        isProcessing = true;
        isListening = false;
        
        Log.d(TAG, "Processing voice input: " + text);
        
        // Update UI
        updateProcessingState(true);
        
        // Classify intent
        IntentResult result = nluManager.classify(text);
        
        // Execute command
        commandExecutor.execute(result);
        
        // Update UI
        updateProcessingState(false);
        
        // Hide session after delay
        hideDelayed(2000);
    }
    
    /**
     * Handle error.
     */
    public void handleError(String message) {
        Log.e(TAG, "Error: " + message);
        
        TTSManager.speak(getContext(), message);
        
        hideDelayed(3000);
    }
    
    /**
     * Update UI for listening state.
     */
    private void updateListeningState(boolean listening) {
        if (contentView == null) return;
        
        // Update UI elements based on state
        // This would be implemented with actual UI components
    }
    
    /**
     * Update UI for processing state.
     */
    private void updateProcessingState(boolean processing) {
        if (contentView == null) return;
        
        // Update UI elements based on state
    }
    
    /**
     * Hide session after delay.
     */
    private void hideDelayed(long delayMs) {
        // VoiceInteractionSession exposes no getHandler(); use our own.
        mainHandler.postDelayed(this::hide, delayMs);
    }
}
