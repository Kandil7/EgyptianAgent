package com.egyptian.agent.core;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import org.vosk.Recognizer;
import org.vosk.Model;
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
    private static final String WAKE_WORD_MODEL_PATH = "model";

    private final Context context;
    private final DeviceClassDetector.DeviceClass deviceClass;
    private Model voskModel;
    private Recognizer voskRecognizer;
    private PocketSphinxWakeWordDetector pocketSphinxDetector;
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
        this.deviceClass = ((MainApplication) context.getApplicationContext()).getDeviceClass();
        initialize();
    }

    private void initialize() {
        try {
            Log.i(TAG, "Initializing wake word detector for device class: " + deviceClass.name());

            // Choose implementation based on device class
            if (deviceClass == DeviceClassDetector.DeviceClass.LOW) {
                // Use PocketSphinx for low-end devices (more efficient)
                initializePocketSphinx();
            } else {
                // Use Vosk for higher-end devices (more accurate)
                initializeVosk();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize wake word detector", e);
            CrashLogger.logError(context, e);
            TTSManager.speak(context, "حصل خطأ في تهيئة كاشف الكلمة التنشيطية");
        }
    }

    private void initializePocketSphinx() throws IOException {
        Log.i(TAG, "Initializing PocketSphinx wake word detector");

        pocketSphinxDetector = new PocketSphinxWakeWordDetector(context, new PocketSphinxWakeWordDetector.WakeWordCallback() {
            @Override
            public void onWakeWordDetected() {
                Log.i(TAG, "PocketSphinx detected wake word");
                if (callback != null) {
                    callback.onWakeWordDetected();
                }
            }
        });

        pocketSphinxDetector.initialize();
        Log.i(TAG, "PocketSphinx wake word detector initialized successfully");
    }

    private void initializeVosk() throws Exception {
        Log.i(TAG, "Initializing Vosk wake word detector");

        // Load the Vosk model
        voskModel = new Model(WAKE_WORD_MODEL_PATH);
        voskRecognizer = new Recognizer(voskModel, SAMPLE_RATE);

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

        Log.i(TAG, "Vosk wake word detector initialized successfully");
    }

    public void startListening() {
        if (isRunning) return;

        Log.i(TAG, "Starting wake word detection for device class: " + deviceClass.name());

        if (deviceClass == DeviceClassDetector.DeviceClass.LOW) {
            // Use PocketSphinx for low-end devices
            if (pocketSphinxDetector != null) {
                pocketSphinxDetector.startListening();
                isRunning = true;
                Log.i(TAG, "PocketSphinx wake word detection started");
            }
        } else {
            // Use Vosk for higher-end devices
            if (audioRecord == null) {
                Log.e(TAG, "AudioRecord not initialized");
                return;
            }

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
            if (voskRecognizer.acceptWaveForm(buffer, bytesRead)) {
                String result = voskRecognizer.getResult();
                processRecognitionResult(result);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing audio", e);
            CrashLogger.logError(context, e);
        } finally {
            isDetecting = false;
        }
    }

    private void processRecognitionResult(String jsonResult) {
        try {
            Log.d(TAG, "Wake word recognition result: " + jsonResult);

            // Parse JSON result
            float confidence = 0.0f;
            String text = "";

            if (jsonResult.contains("\"text\"")) {
                int textStart = jsonResult.indexOf("\"text\":\"") + 8;
                int textEnd = jsonResult.indexOf("\"", textStart);
                text = jsonResult.substring(textStart, textEnd);
            }

            if (jsonResult.contains("\"confidence\"")) {
                int confStart = jsonResult.indexOf("\"confidence\":") + 13;
                int confEnd = jsonResult.indexOf(",", confStart);
                if (confEnd == -1) confEnd = jsonResult.indexOf("}", confStart);
                String confStr = jsonResult.substring(confStart, confEnd).trim();
                try {
                    confidence = Float.parseFloat(confStr);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Failed to parse confidence value: " + confStr);
                }
            }

            // Normalize the detected text
            String normalizedText = text.toLowerCase(Locale.getDefault())
                .replaceAll("[إأآا]", "ا")
                .replaceAll("[ةه]", "ه")
                .replaceAll("[^\\w\\s]", "")
                .trim();

            Log.i(TAG, "Detected text: '" + normalizedText + "' with confidence: " + confidence);

            // Check if wake word was detected with sufficient confidence
            boolean isWakeWord = false;
            String detectedWakeWord = "";

            for (String wakeWord : WAKE_WORDS) {
                String normalizedWakeWord = wakeWord.toLowerCase(Locale.getDefault())
                    .replaceAll("[إأآا]", "ا")
                    .replaceAll("[ةه]", "ه");

                if (normalizedText.contains(normalizedWakeWord) && confidence >= WAKE_WORD_CONFIDENCE_THRESHOLD) {
                    isWakeWord = true;
                    detectedWakeWord = wakeWord;
                    break;
                }
            }

            // Special handling for senior mode (lower threshold)
            if (SeniorMode.isEnabled() && !isWakeWord && confidence >= 0.65f) {
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

            if (isWakeWord) {
                Log.i(TAG, "Wake word detected: " + detectedWakeWord);
                if (callback != null) {
                    callback.onWakeWordDetected();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing recognition result", e);
            CrashLogger.logError(context, e);
        }
    }

    public void stopListening() {
        isRunning = false;

        if (deviceClass == DeviceClassDetector.DeviceClass.LOW) {
            // Use PocketSphinx for low-end devices
            if (pocketSphinxDetector != null) {
                pocketSphinxDetector.stopListening();
            }
        } else {
            // Use Vosk for higher-end devices
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

            try {
                if (voskRecognizer != null) {
                    voskRecognizer.shutdown();
                }
                if (voskModel != null) {
                    voskModel.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error closing Vosk resources", e);
            }
        }
    }

    public void restartListening() {
        stopListening();
        initialize();
        startListening();
    }

    public void destroy() {
        stopListening();

        if (pocketSphinxDetector != null) {
            pocketSphinxDetector.destroy();
        }
    }
}