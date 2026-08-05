package com.egyptian.agent.asr;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.egyptian.agent.utils.CrashLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Whisper ASR Engine
 * 
 * Primary speech-to-text engine using whisper.cpp.
 * Optimized for Egyptian Arabic dialect recognition.
 * 
 * Features:
 * - High accuracy for Egyptian dialect
 * - Offline processing
 * - Real-time streaming support
 * - Multiple model size support (tiny, base, small)
 */
public class WhisperASREngine implements ASREngineInterface {
    private static final String TAG = "WhisperASR";
    
    // Audio configuration
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    // Model configurations
    public static final String MODEL_TINY = "ggml-tiny.bin";
    public static final String MODEL_BASE = "ggml-base.bin";
    public static final String MODEL_SMALL = "ggml-small.bin";
    public static final String MODEL_MEDIUM = "ggml-medium.bin";
    
    // Default model for Honor X6c (balance of speed/accuracy)
    private static final String DEFAULT_MODEL = MODEL_BASE;
    
    private final Context context;
    private final String modelPath;
    
    private ExecutorService executorService;
    private AtomicBoolean isInitialized;
    private AtomicBoolean isListening;
    private AtomicBoolean isDestroyed;
    
    private long whisperContext;
    private RecognitionCallback callback;
    
    private AudioRecord audioRecord;
    private short[] audioBuffer;
    private Thread listeningThread;
    
    // Performance tracking
    private long lastInferenceTime;
    private int totalInferences;
    
    /**
     * Create Whisper ASR engine with default model.
     */
    public WhisperASREngine(Context context) {
        this(context, DEFAULT_MODEL);
    }
    
    /**
     * Create Whisper ASR engine with specified model.
     */
    public WhisperASREngine(Context context, String modelFile) {
        this.context = context.getApplicationContext();
        this.modelPath = modelFile;
        this.executorService = Executors.newSingleThreadExecutor();
        this.isInitialized = new AtomicBoolean(false);
        this.isListening = new AtomicBoolean(false);
        this.isDestroyed = new AtomicBoolean(false);
        this.audioBuffer = new short[SAMPLE_RATE * 5]; // 5 second buffer
        
        Log.d(TAG, "WhisperASREngine created with model: " + modelFile);
    }
    
    @Override
    public boolean initialize() {
        if (isInitialized.get()) {
            Log.w(TAG, "Already initialized");
            return true;
        }
        
        executorService.execute(() -> {
            try {
                // Extract model from assets if needed
                File modelFile = extractModelFromAssets(modelPath);
                
                // Initialize whisper.cpp native library
                whisperContext = initWhisperNative(modelFile.getAbsolutePath());
                
                if (whisperContext != 0) {
                    isInitialized.set(true);
                    Log.i(TAG, "Whisper initialized successfully");
                } else {
                    Log.e(TAG, "Failed to initialize Whisper");
                    CrashLogger.logError(context, new RuntimeException("Whisper init failed"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Whisper", e);
                CrashLogger.logError(context, e);
            }
        });
        
        return true;
    }
    
    /**
     * Extract model file from assets.
     */
    private File extractModelFromAssets(String modelName) throws Exception {
        File outputFile = new File(context.getFilesDir(), "models/" + modelName);
        
        if (!outputFile.exists()) {
            outputFile.getParentFile().mkdirs();
            
            try (InputStream input = context.getAssets().open("models/" + modelName);
                 FileOutputStream output = new FileOutputStream(outputFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
        }
        
        Log.d(TAG, "Model file: " + outputFile.getAbsolutePath());
        return outputFile;
    }
    
    @Override
    public ASRResult transcribe(String audioPath) {
        if (!isInitialized.get()) {
            Log.w(TAG, "Not initialized");
            return new ASRResult("", 0.0f, 0, false);
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Call native transcription
            String text = transcribeFileNative(whisperContext, audioPath);
            
            long duration = System.currentTimeMillis() - startTime;
            lastInferenceTime = duration;
            totalInferences++;
            
            Log.d(TAG, "Transcription completed in " + duration + "ms: " + text);
            
            return new ASRResult(text, 0.9f, duration, true);
        } catch (Exception e) {
            Log.e(TAG, "Error transcribing file", e);
            return new ASRResult("", 0.0f, 0, false);
        }
    }
    
    @Override
    public ASRResult transcribeStream(short[] audioData) {
        if (!isInitialized.get()) {
            return new ASRResult("", 0.0f, 0, false);
        }
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Convert short[] to float[] for whisper.cpp
            float[] floatData = new float[audioData.length];
            for (int i = 0; i < audioData.length; i++) {
                floatData[i] = audioData[i] / 32768.0f;
            }
            
            // Call native streaming transcription
            String text = transcribeStreamNative(whisperContext, floatData);
            
            long duration = System.currentTimeMillis() - startTime;
            
            return new ASRResult(text, 0.8f, duration, false);
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
        
        listeningThread = new Thread(this::listeningLoop, "WhisperListeningThread");
        listeningThread.setPriority(Thread.NORM_PRIORITY);
        listeningThread.start();
        
        Log.d(TAG, "Whisper listening started");
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
        
        Log.d(TAG, "Whisper listening stopped");
    }
    
    @Override
    public boolean isReady() {
        return isInitialized.get() && whisperContext != 0;
    }
    
    @Override
    public void destroy() {
        isDestroyed.set(true);
        stopListening();
        
        if (whisperContext != 0) {
            unloadWhisperNative(whisperContext);
            whisperContext = 0;
        }
        
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        
        Log.d(TAG, "Whisper ASR destroyed");
    }
    
    /**
     * Main listening loop for continuous recognition.
     */
    private void listeningLoop() {
        try {
            initializeAudioRecord();
            
            // Buffer for accumulating audio
            short[] accumulatedBuffer = new short[SAMPLE_RATE * 10]; // 10 seconds
            int accumulatedSamples = 0;
            
            while (isListening.get() && !isDestroyed.get() && !Thread.interrupted()) {
                int bytesRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                
                if (bytesRead > 0) {
                    // Add to accumulated buffer
                    int samplesToCopy = Math.min(bytesRead / 2, accumulatedBuffer.length - accumulatedSamples);
                    System.arraycopy(audioBuffer, 0, accumulatedBuffer, accumulatedSamples, samplesToCopy);
                    accumulatedSamples += samplesToCopy;
                    
                    // Process when we have enough audio (5 seconds)
                    if (accumulatedSamples >= SAMPLE_RATE * 5) {
                        // Create buffer for processing
                        short[] processBuffer = new short[accumulatedSamples];
                        System.arraycopy(accumulatedBuffer, 0, processBuffer, 0, accumulatedSamples);
                        
                        // Transcribe
                        ASRResult result = transcribeStream(processBuffer);
                        
                        if (!result.isEmpty() && callback != null) {
                            if (result.isFinal()) {
                                callback.onResult(result);
                            } else {
                                callback.onPartialResult(result);
                            }
                        }
                        
                        // Reset accumulator
                        accumulatedSamples = 0;
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
        Log.d(TAG, "AudioRecord initialized for Whisper");
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
    
    /**
     * Get performance statistics.
     */
    public long getLastInferenceTime() {
        return lastInferenceTime;
    }
    
    public int getTotalInferences() {
        return totalInferences;
    }
    
    public double getAverageInferenceTime() {
        return totalInferences > 0 ? (double) lastInferenceTime / totalInferences : 0;
    }
    
    // Native methods for whisper.cpp
    private native long initWhisperNative(String modelPath);
    private native String transcribeFileNative(long context, String audioPath);
    private native String transcribeStreamNative(long context, float[] audioData);
    private native void unloadWhisperNative(long context);
}
