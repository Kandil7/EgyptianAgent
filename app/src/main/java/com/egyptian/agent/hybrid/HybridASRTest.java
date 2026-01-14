package com.egyptian.agent.hybrid;

import android.content.Context;
import android.util.Log;

/**
 * Test class for HybridASR functionality
 */
public class HybridASRTest {
    private static final String TAG = "HybridASRTest";
    
    public static void testHybridASR(Context context) {
        Log.d(TAG, "Starting HybridASR test...");
        
        try {
            // Initialize HybridASR
            HybridASR hybridASR = new HybridASR(context);
            
            // Test with a mock audio file path (this would be an actual file in real usage)
            String testAudioPath = "/data/local/tmp/test_audio.wav";
            
            // Perform transcription
            String result = hybridASR.transcribeAudio(testAudioPath);
            
            Log.d(TAG, "HybridASR test result: " + result);
            
            // Clean up
            hybridASR.destroy();
            
            Log.d(TAG, "HybridASR test completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during HybridASR test", e);
        }
    }
}