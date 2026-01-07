package com.egyptian.agent.core;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.utils.CrashLogger;

public class WakeWordDetector {

    private static final String TAG = "WakeWordDetector";
    private static final int SAMPLE_RATE = 16000;
    private static final int BUFFER_SIZE = 2048;
    private static final String WAKE_WORD_MODEL_PATH = "wake_word_model";

    private final Context context;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRunning = false;
    private boolean isDetecting = false;
    private final WakeWordCallback callback;

    // Wake word variations for Egyptian dialect
    private static final String[] WAKE_WORDS = {
        "يا صاحبي", "يصاحبي", "ياصاحبي", "يا كبير", "ياعم", "يا عم"
    };

    // Confidence threshold for wake word detection
    private static final float WAKE_WORD_CONFIDENCE_THRESHOLD = 0.75f;

    public interface WakeWordCallback {
        void onWakeWordDetected();
    }

    public WakeWordDetector(Context context, WakeWordCallback callback) {
        this.context = context;
        this.callback = callback;
        initialize();
    }

    private void initialize() {
        try {
            Log.i(TAG, "Initializing wake word detector with model: " + WAKE_WORD_MODEL_PATH);

            // Configure audio recording
            int minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            );

            int bufferSize = Math.max(minBufferSize, BUFFER_SIZE * 2);

            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            );

            Log.i(TAG, "Wake word detector initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize wake word detector", e);
            CrashLogger.logError(context, e);
            TTSManager.speak(context, "حصل خطأ في تهيئة كاشف الكلمة التنشيطية");
        }
    }

    public void startListening() {
        if (isRunning || audioRecord == null) return;

        Log.i(TAG, "Starting wake word detection");
        isRunning = true;
        isDetecting = false;

        audioRecord.startRecording();

        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            short[] audioData = new short[BUFFER_SIZE / 2]; // 16-bit audio

            while (isRunning) {
                try {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);

                    if (bytesRead > 0) {
                        // Convert byte array to short array for processing
                        ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audioData);

                        // Detect silence or noise to optimize processing
                        if (isSoundPresent(audioData)) {
                            processAudio(buffer, bytesRead);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error during audio recording", e);
                    CrashLogger.logError(context, e);
                }
            }

            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping audio record", e);
            }
        });

        recordingThread.start();
    }

    private boolean isSoundPresent(short[] audioData) {
        double sum = 0;
        for (short sample : audioData) {
            sum += sample * sample;
        }
        double rms = Math.sqrt(sum / audioData.length);
        return rms > 100; // Threshold for detecting sound
    }

    private void processAudio(byte[] buffer, int bytesRead) {
        if (isDetecting) return;

        isDetecting = true;

        try {
            // Simple keyword spotting algorithm (placeholder)
            // In a real implementation, this would use a trained model
            String detectedText = simpleKeywordSpotting(buffer, bytesRead);
            if (detectedText != null && isWakeWord(detectedText)) {
                Log.i(TAG, "Wake word detected: " + detectedText);
                if (callback != null) {
                    callback.onWakeWordDetected();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing audio", e);
            CrashLogger.logError(context, e);
        } finally {
            isDetecting = false;
        }
    }

    private String simpleKeywordSpotting(byte[] buffer, int bytesRead) {
        // This is a very simplified placeholder for keyword spotting
        // In reality, this would use a machine learning model
        // For now, we'll just return a dummy result occasionally
        
        // Simulate occasional detection for testing purposes
        if (Math.random() > 0.99) { // Very rare chance to simulate detection
            return "يا صاحبي";
        }
        
        return null;
    }

    private boolean isWakeWord(String detectedText) {
        if (detectedText == null) return false;

        // Normalize the detected text
        String normalizedText = detectedText.toLowerCase(Locale.getDefault())
            .replaceAll("[إأآا]", "ا")
            .replaceAll("[ةه]", "ه")
            .replaceAll("[^\\w\\s]", "")
            .trim();

        Log.i(TAG, "Detected text: '" + normalizedText + "'");

        // Check if wake word was detected with sufficient confidence
        boolean isWakeWord = false;
        String detectedWakeWord = "";

        for (String wakeWord : WAKE_WORDS) {
            String normalizedWakeWord = wakeWord.toLowerCase(Locale.getDefault())
                .replaceAll("[إأآا]", "ا")
                .replaceAll("[ةه]", "ه");

            if (normalizedText.contains(normalizedWakeWord)) {
                isWakeWord = true;
                detectedWakeWord = wakeWord;
                break;
            }
        }

        // Special handling for senior mode (lower threshold)
        if (SeniorMode.isEnabled() && !isWakeWord) {
            for (String wakeWord : new String[]{"يا كبير", "ياعم", "يا عم"}) {
                String normalizedWakeWord = wakeWord.toLowerCase(Locale.getDefault())
                    .replaceAll("[إأآا]", "ا")
                    .replaceAll("[ةه]", "ه");

                if (normalizedText.contains(normalizedWakeWord)) {
                    isWakeWord = true;
                    detectedWakeWord = wakeWord;
                    break;
                }
            }
        }

        return isWakeWord;
    }

    public void stopListening() {
        isRunning = false;
        try {
            if (recordingThread != null && recordingThread.isAlive()) {
                recordingThread.join(1000); // Wait up to 1 second
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop();
                audioRecord.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping audio record", e);
        }
    }

    public void restartListening() {
        stopListening();
        initialize();
        startListening();
    }

    public void destroy() {
        stopListening();
    }
}