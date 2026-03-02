package com.egyptian.agent.service;

import android.service.voice.VoiceInteractionSessionService;
import android.util.Log;

/**
 * Voice Interaction Session Service
 * 
 * Service that creates voice interaction sessions.
 * Required for VoiceInteractionService integration.
 */
public class EgyptianAgentSessionService extends VoiceInteractionSessionService {
    private static final String TAG = "EgyptianAgentSessionService";
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "SessionService created");
    }
    
    @Override
    public EgyptianAgentSession onNewSession() {
        Log.d(TAG, "Creating new session");
        return new EgyptianAgentSession(this);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "SessionService destroyed");
    }
}
