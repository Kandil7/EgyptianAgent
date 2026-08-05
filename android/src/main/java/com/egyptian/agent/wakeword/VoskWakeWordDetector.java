package com.egyptian.agent.wakeword;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.egyptian.agent.stt.VoskSTTEngine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Vosk-based Wake Word Detector
 *
 * Fallback wake word detection using Vosk STT engine.
 * Less accurate than Porcupine but works without additional SDK.
 *
 * Features:
 * - Uses existing Vosk STT infrastructure
 * - No additional dependencies
 * - Higher battery usage than Porcupine
 */
public class VoskWakeWordDetector implements WakeWordDetectorInterface {
    private static final String TAG = "VoskWakeWord";
    
    // Audio configuration
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_MS = 500; // Process 500ms chunks
    private static final int BUFFER_SIZE = SAMPLE_RATE * BUFFER_SIZE_MS / 1000 * 2; // 16-bit samples
    
    // Wake word definitions
    private static final String[] WAKE_WORDS = {
        "يا صاحبي",
        "يا كبير",
        "يا حكيم",
        "يا معلم"
    };
    
    private final Context context;
    private WakeWordCallback callback;
    
    private ExecutorService executorService;
    private AtomicBoolean isListening;
    private AtomicBoolean isDestroyed;
    
    private AudioRecord audioRecord;
    private byte[] audioBuffer;
    private Thread detectionThread;
    
    private VoskSTTEngine sttEngine;
    private boolean sttInitialized;
    
    /**
     * Creates a new Vosk wake word detector.
     * @param context Android context
     */
    public VoskWakeWordDetector(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.isListening = new AtomicBoolean(false);
        this.isDestroyed = new AtomicBoolean(false);
        this.audioBuffer = new byte[BUFFER_SIZE];
        
        initializeSTT();
    }
    
    /**
     * Initialize Vosk STT engine for wake word detection.
     */
    private void initializeSTT() {
        executorService.execute(() -> {
            try {
                // Use small model for wake word detection (faster, less memory)
                sttEngine = new VoskSTTEngine(context, "models/vosk-model-small-ar");
                sttInitialized = true;
                Log.i(TAG, "Vosk STT initialized for wake word detection");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Vosk STT", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    @Override
    public void setCallback(WakeWordCallback callback) {
        this.callback = callback;
    }
    
    @Override
    public void start() {
        if (isListening.get() || isDestroyed.get()) {
            Log.w(TAG, "Already listening or destroyed");
            return;
        }
        
        if (!sttInitialized) {
            Log.w(TAG, "STT not initialized yet");
            return;
        }
        
        isListening.set(true);
        
        detectionThread = new Thread(this::detectionLoop, "VoskWakewordDetectionThread");
        detectionThread.setPriority(Thread.MIN_PRIORITY);
        detectionThread.start();
        
        Log.d(TAG, "Vosk wake word detection started");
        
        if (callback != null) {
            callback.onStateChanged(true);
        }
    }
    
    @Override
    public void stop() {
        isListening.set(false);
        
        if (detectionThread != null) {
            detectionThread.interrupt();
            try {
                detectionThread.join(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping detection thread", e);
            }
            detectionThread = null;
        }
        
        stopAudioRecord();
        
        Log.d(TAG, "Vosk wake word detection stopped");
        
        if (callback != null) {
            callback.onStateChanged(false);
        }
    }
    
    @Override
    public void restart() {
        stop();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        start();
    }
    
    @Override
    public boolean isListening() {
        return isListening.get() && sttInitialized;
    }
    
    @Override
    public void destroy() {
        isDestroyed.set(true);
        stop();
        
        if (sttEngine != null) {
            sttEngine.destroy();
            sttEngine = null;
            sttInitialized = false;
        }
        
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        
        Log.d(TAG, "Vosk wake word detector destroyed");
    }
    
    /**
     * Main detection loop using Vosk STT.
     */
    private void detectionLoop() {
        try {
            initializeAudioRecord();
            
            // Buffer for accumulating audio
            StringBuilder accumulatedText = new StringBuilder();
            long lastResetTime = System.currentTimeMillis();
            
            while (isListening.get() && !isDestroyed.get() && !Thread.interrupted()) {
                // Read audio chunk
                int bytesRead = audioRecord.read(audioBuffer, 0, BUFFER_SIZE);
                
                if (bytesRead > 0) {
                    // Process with Vosk STT
                    String recognizedText = sttEngine.recognizeAudio(audioBuffer, bytesRead);
                    
                    if (recognizedText != null && !recognizedText.isEmpty()) {
                        accumulatedText.append(" ").append(recognizedText);
                        
                        // Check for wake words
                        String fullText = accumulatedText.toString().toLowerCase();
                        for (String wakeWord : WAKE_WORDS) {
                            if (fullText.contains(wakeWord.toLowerCase())) {
                                Log.i(TAG, "Wake word detected: " + wakeWord);
                                
                                float confidence = calculateConfidence(fullText, wakeWord);
                                
                                if (callback != null) {
                                    callback.onWakeWordDetected(wakeWord, confidence);
                                }
                                
                                // Reset and pause
                                accumulatedText.setLength(0);
                                lastResetTime = System.currentTimeMillis();
                                Thread.sleep(2000);
                                break;
                            }
                        }
                        
                        // Reset buffer every 5 seconds to prevent memory growth
                        if (System.currentTimeMillis() - lastResetTime > 5000) {
                            accumulatedText.setLength(0);
                            lastResetTime = System.currentTimeMillis();
                        }
                    }
                } else if (bytesRead < 0) {
                    Log.e(TAG, "Error reading audio: " + bytesRead);
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in detection loop", e);
            if (callback != null) {
                callback.onError(e);
            }
        } finally {
            stopAudioRecord();
        }
    }
    
    /**
     * Calculate confidence score for wake word detection.
     */
    private float calculateConfidence(String text, String wakeWord) {
        float baseConfidence = 0.7f;
        
        // Boost confidence for exact match
        if (text.trim().equals(wakeWord)) {
            baseConfidence += 0.2f;
        }
        
        // Boost confidence if wake word appears at start
        if (text.trim().startsWith(wakeWord)) {
            baseConfidence += 0.1f;
        }
        
        return Math.min(baseConfidence, 0.98f);
    }
    
    /**
     * Initialize AudioRecord for audio capture.
     */
    private void initializeAudioRecord() throws Exception {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        bufferSize = Math.max(bufferSize, BUFFER_SIZE);
        
        audioRecord = new AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        );
        
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new Exception("Failed to initialize AudioRecord");
        }
        
        audioRecord.startRecording();
        Log.d(TAG, "AudioRecord initialized for Vosk wake word");
    }
    
    /**
     * Stop and release AudioRecord.
     */
    private void stopAudioRecord() {
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing AudioRecord", e);
            }
            audioRecord = null;
        }
    }
}
