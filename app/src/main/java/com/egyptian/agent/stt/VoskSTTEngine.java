package com.egyptian.agent.stt;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.CrashLogger;

public class VoskSTTEngine {

    private static final String TAG = "VoskSTTEngine";
    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = 4096;
    private static final String STT_MODEL_PATH = "model";

    private final Context context;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isListening = false;
    private boolean isProcessing = false;
    private final STTCallback callback;

    // Background thread pool for processing
    private final ExecutorService processingExecutor = Executors.newSingleThreadExecutor();

    public interface STTCallback {
        void onResult(String text);
        void onError(Exception error);
    }

    public VoskSTTEngine(Context context) throws Exception {
        this(context, null);
    }

    public VoskSTTEngine(Context context, STTCallback callback) throws Exception {
        this.context = context;
        this.callback = callback;
        initialize();
    }

    private void initialize() throws Exception {
        Log.i(TAG, "Initializing Vosk STT Engine");

        try {
            // Configure audio recording
            int minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            );

            int bufferSize = Math.max(minBufferSize, BUFFER_SIZE);

            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            );

            Log.i(TAG, "STT engine initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize STT engine", e);
            CrashLogger.logError(context, e);
            throw new Exception("Failed to initialize speech recognition engine", e);
        }
    }

    public void startListening() {
        startListening(null);
    }

    public void startListening(STTCallback customCallback) {
        if (isListening) return;

        Log.i(TAG, "Starting STT listening");
        isListening = true;
        isProcessing = false;

        // Use custom callback if provided, otherwise use the default one
        final STTCallback effectiveCallback = customCallback != null ? customCallback : this.callback;

        try {
            audioRecord.startRecording();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start audio recording", e);
            // CrashLogger.logError(context, e);
            isListening = false;
            if (effectiveCallback != null) {
                effectiveCallback.onError(e);
            }
            return;
        }

        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (isListening) {
                try {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);

                    if (bytesRead > 0 && !isProcessing) {
                        isProcessing = true;
                        byte[] audioChunk = new byte[bytesRead];
                        System.arraycopy(buffer, 0, audioChunk, 0, bytesRead);

                        // Process audio on background thread
                        processingExecutor.execute(() -> {
                            try {
                                processAudio(audioChunk, bytesRead, effectiveCallback);
                            } finally {
                                isProcessing = false;
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error during audio recording", e);
                    CrashLogger.logError(context, e);
                    isListening = false;
                }
            }

            cleanupRecording();
        });

        recordingThread.start();
    }

    private void processAudio(byte[] buffer, int bytesRead, STTCallback callback) {
        try {
            // Simple speech recognition simulation (placeholder)
            // In a real implementation, this would use the Vosk library
            String result = simpleSpeechRecognition(buffer, bytesRead);
            
            if (result != null && !result.isEmpty()) {
                handleRecognitionResult(result, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing audio", e);
            CrashLogger.logError(context, e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    private String simpleSpeechRecognition(byte[] buffer, int bytesRead) {
        // This is a placeholder for actual speech recognition
        // In a real implementation, this would use the Vosk library
        
        // For demonstration purposes, we'll return a random phrase occasionally
        if (Math.random() > 0.95) { // 5% chance to simulate recognition
            String[] phrases = {
                "اتصل بأمي",
                "يا صاحبي",
                "يا كبير",
                "بكرة الصبح",
                "ابعت واتساب لبنتي"
            };
            return phrases[(int)(Math.random() * phrases.length)];
        }
        
        return null;
    }

    private void handleRecognitionResult(String result, STTCallback callback) {
        try {
            Log.d(TAG, "Recognition result: " + result);

            if (result == null || result.isEmpty()) {
                return;
            }

            Log.i(TAG, "Recognized text: '" + result + "'");

            if (!result.isEmpty() && callback != null) {
                // Apply Egyptian dialect normalization
                String normalizedText = EgyptianNormalizer.normalize(result);
                callback.onResult(normalizedText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling recognition result", e);
            // CrashLogger.logError(context, e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    public void stopListening() {
        isListening = false;
        try {
            if (recordingThread != null && recordingThread.isAlive()) {
                recordingThread.join(2000); // Wait up to 2 seconds
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        cleanupRecording();
    }

    private void cleanupRecording() {
        try {
            if (audioRecord != null) {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.release();
                }
                audioRecord = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up audio recording", e);
        }
    }

    public void pauseListening() {
        if (isListening && audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (Exception e) {
                Log.e(TAG, "Error pausing audio recording", e);
            }
        }
    }

    public void resumeListening() {
        if (isListening && audioRecord != null) {
            try {
                audioRecord.startRecording();
            } catch (Exception e) {
                Log.e(TAG, "Error resuming audio recording", e);
            }
        }
    }

    public void destroy() {
        stopListening();
        processingExecutor.shutdown();

        try {
            // Clean up resources if needed
        } catch (Exception e) {
            Log.e(TAG, "Error closing STT resources", e);
        }

        System.gc(); // Request garbage collection to free memory
    }
}