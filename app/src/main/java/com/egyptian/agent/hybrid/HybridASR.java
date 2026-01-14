package com.egyptian.agent.hybrid;

import android.content.Context;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.egyptian.agent.stt.VoskSTTEngine;

/**
 * Hybrid ASR Engine for EgyptianAgent
 * Combines Whisper offline, Live Transcribe cloud, and PocketSphinx fallback
 */
public class HybridASR {
    private static final String TAG = "HybridASR";
    
    private WhisperEngine whisper;
    private LiveTranscribeEngine liveTranscribe;
    private VoskSTTEngine voskEngine; // Using existing Vosk as fallback
    private Context context;
    
    public HybridASR(Context ctx) {
        this.context = ctx;
        
        // Initialize Whisper offline engine
        try {
            this.whisper = new WhisperEngine(ctx, "whisper-small-egyptian.bin");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Whisper engine", e);
        }
        
        // Initialize Live Transcribe cloud engine
        try {
            this.liveTranscribe = new LiveTranscribeEngine(ctx);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Live Transcribe engine", e);
        }
        
        // Initialize Vosk as additional fallback
        try {
            this.voskEngine = new VoskSTTEngine(ctx, "models/vosk-model-small-ar.zip");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Vosk engine", e);
        }
    }
    
    /**
     * Main transcription method that implements the hybrid approach
     * @param audioPath Path to the audio file to transcribe
     * @return Transcribed text
     */
    public String transcribeAudio(String audioPath) {
        // 1. Check offline first (priority)
        if (isOfflineCapable()) {
            String whisperResult = whisper.transcribe(audioPath);
            if (whisperResult != null && whisperResult.length() > 5 && 
                whisper.confidence(whisperResult) > 0.85) {
                Log.d(TAG, "Whisper offline result: " + whisperResult);
                return whisperResult;  // Egyptian offline success!
            }
        }
        
        // 2. Cloud fallback (Live Transcribe)
        if (liveTranscribe != null && liveTranscribe.isOnline()) {
            String cloudResult = liveTranscribe.streamTranscribe(audioPath);
            if (cloudResult != null && !cloudResult.isEmpty()) {
                Log.d(TAG, "Live Transcribe cloud result: " + cloudResult);
                return cloudResult;
            }
        }
        
        // 3. Vosk fallback (existing offline solution)
        return voskFallback(audioPath);
    }
    
    /**
     * Checks if the device has sufficient resources for offline processing
     * @return true if offline capable, false otherwise
     */
    private boolean isOfflineCapable() {
        // Check available memory (requires at least 1GB free)
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = maxMemory - totalMemory + runtime.freeMemory();
        
        // Also check if Whisper engine is initialized
        return freeMemory > 1_000_000_000L && whisper != null;
    }
    
    /**
     * Fallback to Vosk engine when other methods fail
     * @param audioPath Path to the audio file
     * @return Transcribed text from Vosk
     */
    private String voskFallback(String audioPath) {
        if (voskEngine != null && voskEngine.isInitialized()) {
            String result = voskEngine.recognizeFromFile(audioPath);
            if (result != null) {
                Log.d(TAG, "Vosk fallback result: " + result);
                return result;
            }
        }

        Log.w(TAG, "All ASR engines failed, returning empty result");
        return "";
    }
    
    /**
     * Checks if the device is online
     * @return true if online, false otherwise
     */
    public boolean isOnline() {
        if (liveTranscribe != null) {
            return liveTranscribe.isOnline();
        }
        
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
    
    /**
     * Cleans up resources
     */
    public void destroy() {
        if (whisper != null) {
            // No specific cleanup needed for Whisper in this implementation
        }
        
        if (liveTranscribe != null) {
            liveTranscribe.destroy();
        }
        
        if (voskEngine != null) {
            voskEngine.destroy();
        }
    }
}