package com.egyptian.agent.asr;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Audio Recorder Utility
 * 
 * Utility class for recording audio from microphone.
 * Optimized for speech recognition (16kHz, mono, 16-bit PCM).
 */
public class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    
    // Audio configuration
    public static final int SAMPLE_RATE = 16000;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    private AudioRecord audioRecord;
    private short[] audioBuffer;
    private FileOutputStream outputStream;
    private AtomicBoolean isRecording;
    private Thread recordingThread;
    
    /**
     * Create audio recorder.
     */
    public AudioRecorder() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        this.audioBuffer = new short[bufferSize / 2];
        this.isRecording = new AtomicBoolean(false);
    }
    
    /**
     * Initialize AudioRecord.
     */
    public boolean initialize() {
        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed to initialize AudioRecord");
                return false;
            }
            
            Log.d(TAG, "AudioRecord initialized");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AudioRecord", e);
            return false;
        }
    }
    
    /**
     * Start recording to file.
     */
    public void startRecording(File outputFile) {
        if (isRecording.get()) {
            Log.w(TAG, "Already recording");
            return;
        }
        
        try {
            outputStream = new FileOutputStream(outputFile);
            
            audioRecord.startRecording();
            isRecording.set(true);
            
            recordingThread = new Thread(this::recordingLoop, "AudioRecordingThread");
            recordingThread.start();
            
            Log.d(TAG, "Recording started: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
        }
    }
    
    /**
     * Start recording to buffer (for streaming).
     */
    public void startRecording(RecordingCallback callback) {
        if (isRecording.get()) {
            Log.w(TAG, "Already recording");
            return;
        }
        
        try {
            audioRecord.startRecording();
            isRecording.set(true);
            
            recordingThread = new Thread(() -> recordingLoop(callback), "AudioStreamingThread");
            recordingThread.start();
            
            Log.d(TAG, "Streaming recording started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting streaming recording", e);
        }
    }
    
    /**
     * Stop recording.
     */
    public void stopRecording() {
        isRecording.set(false);
        
        if (recordingThread != null) {
            recordingThread.interrupt();
            try {
                recordingThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            recordingThread = null;
        }
        
        if (audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioRecord", e);
            }
        }
        
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing output stream", e);
            }
            outputStream = null;
        }
        
        Log.d(TAG, "Recording stopped");
    }
    
    /**
     * Check if currently recording.
     */
    public boolean isRecording() {
        return isRecording.get();
    }
    
    /**
     * Release resources.
     */
    public void release() {
        stopRecording();
        
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
        
        Log.d(TAG, "AudioRecorder released");
    }
    
    /**
     * Recording loop for file output.
     */
    private void recordingLoop() {
        try {
            while (isRecording.get() && !Thread.interrupted()) {
                int bytesRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                
                if (bytesRead > 0 && outputStream != null) {
                    byte[] byteBuffer = new byte[bytesRead * 2];
                    for (int i = 0; i < bytesRead; i++) {
                        byteBuffer[i * 2] = (byte) (audioBuffer[i] & 0xff);
                        byteBuffer[i * 2 + 1] = (byte) ((audioBuffer[i] >> 8) & 0xff);
                    }
                    outputStream.write(byteBuffer);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in recording loop", e);
        }
    }
    
    /**
     * Recording loop for streaming callback.
     */
    private void recordingLoop(RecordingCallback callback) {
        try {
            while (isRecording.get() && !Thread.interrupted()) {
                int bytesRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                
                if (bytesRead > 0 && callback != null) {
                    // Copy to new array for callback
                    short[] bufferCopy = new short[bytesRead];
                    System.arraycopy(audioBuffer, 0, bufferCopy, 0, bytesRead);
                    callback.onAudioData(bufferCopy);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in streaming recording loop", e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    /**
     * Callback for streaming audio data.
     */
    public interface RecordingCallback {
        void onAudioData(short[] audioData);
        void onError(Exception error);
    }
}
