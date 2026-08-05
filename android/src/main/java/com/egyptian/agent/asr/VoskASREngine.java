package com.egyptian.agent.asr;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.egyptian.agent.stt.VoskSTTEngine;
import com.egyptian.agent.utils.CrashLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Vosk ASR Engine
 * 
 * Fallback speech-to-text engine using Vosk.
 * Lighter weight than Whisper but lower accuracy.
 * 
 * Features:
 * - Lower memory footprint
 * - Faster initialization
 * - Good for simple commands
 * - Egyptian Arabic support
 */
public class VoskASREngine implements ASREngineInterface {
    private static final String TAG = "VoskASR";
    
    // Audio configuration
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    private final Context context;
    private final String modelPath;
    
    private ExecutorService executorService;
    private AtomicBoolean isInitialized;
    private AtomicBoolean isListening;
    private AtomicBoolean isDestroyed;
    
    private VoskSTTEngine voskEngine;
    private RecognitionCallback callback;
    
    private AudioRecord audioRecord;
    private byte[] audioBuffer;
    private Thread listeningThread;
    
    /**
     * Create Vosk ASR engine with default model.
     */
    public VoskASREngine(Context context) {
        this(context, "models/vosk-model-small-ar");
    }
    
    /**
     * Create Vosk ASR engine with specified model.
     */
    public VoskASREngine(Context context, String modelPath) {
        this.context = context.getApplicationContext();
        this.modelPath = modelPath;
        this.executorService = Executors.newSingleThreadExecutor();
        this.isInitialized = new AtomicBoolean(false);
        this.isListening = new AtomicBoolean(false);
        this.isDestroyed = new AtomicBoolean(false);
        this.audioBuffer = new byte[SAMPLE_RATE]; // 1 second buffer
        
        Log.d(TAG, "VoskASREngine created with model: " + modelPath);
    }
    
    @Override
    public boolean initialize() {
        if (isInitialized.get()) {
            Log.w(TAG, "Already initialized");
            return true;
        }
        
        executorService.execute(() -> {
            try {
                voskEngine = new VoskSTTEngine(context, modelPath);
                isInitialized.set(true);
                Log.i(TAG, "Vosk ASR initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Vosk ASR", e);
                CrashLogger.logError(context, e);
            }
        });
        
        return true;
    }
    
    @Override
    public ASRResult transcribe(String audioPath) {
        if (!isInitialized.get() || voskEngine == null) {
            Log.w(TAG, "Not initialized");
            return new ASRResult("", 0.0f, 0, false);
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Vosk doesn't support file transcription directly
            // This would require loading the file and processing it
            // For now, return empty result
            Log.w(TAG, "File transcription not supported in Vosk fallback");
            
            return new ASRResult("", 0.0f, System.currentTimeMillis() - startTime, false);
        } catch (Exception e) {
            Log.e(TAG, "Error transcribing file", e);
            return new ASRResult("", 0.0f, 0, false);
        }
    }
    
    @Override
    public ASRResult transcribeStream(short[] audioData) {
        if (!isInitialized.get() || voskEngine == null) {
            return new ASRResult("", 0.0f, 0, false);
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Convert short[] to byte[]
            byte[] byteData = new byte[audioData.length * 2];
            for (int i = 0; i < audioData.length; i++) {
                byteData[i * 2] = (byte) (audioData[i] & 0xff);
                byteData[i * 2 + 1] = (byte) ((audioData[i] >> 8) & 0xff);
            }
            
            // Use Vosk for recognition
            String text = voskEngine.recognizeAudio(byteData, byteData.length);
            
            long duration = System.currentTimeMillis() - startTime;
            
            return new ASRResult(text != null ? text : "", 0.7f, duration, false);
        } catch (Exception e) {
            Log.e(TAG, "Error in streaming transcription", e);
            return new ASRResult("", 0.0f, 0, false);
        }
    }
    
    @Override
    public void startListening(RecognitionCallback callback) {
        if (!isInitialized.get()) {
            Log.w(TAG, "Not initialized, cannot start listening");
            return;
        }
        
        if (isListening.get()) {
            Log.w(TAG, "Already listening");
            return;
        }
        
        this.callback = callback;
        isListening.set(true);
        
        listeningThread = new Thread(this::listeningLoop, "VoskListeningThread");
        listeningThread.setPriority(Thread.NORM_PRIORITY);
        listeningThread.start();
        
        Log.d(TAG, "Vosk listening started");
    }
    
    @Override
    public void stopListening() {
        isListening.set(false);
        
        if (listeningThread != null) {
            listeningThread.interrupt();
            try {
                listeningThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            listeningThread = null;
        }
        
        stopAudioRecord();
        
        Log.d(TAG, "Vosk listening stopped");
    }
    
    @Override
    public boolean isReady() {
        return isInitialized.get() && voskEngine != null;
    }
    
    @Override
    public void destroy() {
        isDestroyed.set(true);
        stopListening();
        
        if (voskEngine != null) {
            voskEngine.destroy();
            voskEngine = null;
        }
        
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        
        Log.d(TAG, "Vosk ASR destroyed");
    }
    
    /**
     * Main listening loop.
     */
    private void listeningLoop() {
        try {
            initializeAudioRecord();
            
            while (isListening.get() && !isDestroyed.get() && !Thread.interrupted()) {
                int bytesRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                
                if (bytesRead > 0) {
                    // Process with Vosk
                    String text = voskEngine != null ? voskEngine.recognizeAudio(audioBuffer, bytesRead) : null;
                    
                    if (text != null && !text.isEmpty() && callback != null) {
                        ASRResult result = new ASRResult(text, 0.7f, bytesRead / SAMPLE_RATE * 1000, true);
                        callback.onResult(result);
                    }
                } else if (bytesRead < 0) {
                    Log.e(TAG, "Error reading audio: " + bytesRead);
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in listening loop", e);
            if (callback != null) {
                callback.onError(e);
            }
        } finally {
            stopAudioRecord();
        }
    }
    
    /**
     * Initialize AudioRecord.
     */
    private void initializeAudioRecord() throws Exception {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        bufferSize = Math.max(bufferSize, audioBuffer.length * 2);
        
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
        Log.d(TAG, "AudioRecord initialized for Vosk");
    }
    
    /**
     * Stop AudioRecord.
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
