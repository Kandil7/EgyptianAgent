package com.egyptian.agent.stt;

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
    private final DeviceClassDetector.DeviceClass deviceClass;
    private Model voskModel;
    private Recognizer voskRecognizer;
    private WhisperASREngine whisperEngine;
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
        this.context = context;
        this.deviceClass = ((MainApplication) context.getApplicationContext()).getDeviceClass();
        this.callback = null;
        initialize();
    }

    public VoskSTTEngine(Context context, STTCallback callback) throws Exception {
        this.context = context;
        this.deviceClass = ((MainApplication) context.getApplicationContext()).getDeviceClass();
        this.callback = callback;
        initialize();
    }

    public VoskSTTEngine(Context context, String modelPath) throws Exception {
        this.context = context;
        this.deviceClass = ((MainApplication) context.getApplicationContext()).getDeviceClass();
        this.callback = null;
        initialize(modelPath);
    }

    public VoskSTTEngine(Context context, String modelPath, STTCallback callback) throws Exception {
        this.context = context;
        this.deviceClass = ((MainApplication) context.getApplicationContext()).getDeviceClass();
        this.callback = callback;
        initialize(modelPath);
    }

    private void initialize() throws Exception {
        initialize(STT_MODEL_PATH);
    }

    private void initialize(String modelPath) throws Exception {
        Log.i(TAG, "Initializing STT Engine for device class: " + deviceClass.name() +
              " with model: " + modelPath);

        try {
            if (deviceClass == DeviceClassDetector.DeviceClass.LOW) {
                // For low-end devices, use Whisper which is more efficient
                whisperEngine = new WhisperASREngine(context, modelPath);
                whisperEngine.initialize();
                Log.i(TAG, "Whisper ASR engine initialized for low-end device");
            } else {
                // For higher-end devices, use Vosk
                voskModel = new Model(modelPath);
                voskRecognizer = new Recognizer(voskModel, SAMPLE_RATE);

                Log.i(TAG, "Vosk model loaded successfully from: " + modelPath);

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

                // Load custom vocabulary
                VocabularyManager.loadCustomWords(voskRecognizer);
            }
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
        Log.i(TAG, "Starting STT listening for device class: " + deviceClass.name());

        // Use custom callback if provided, otherwise use the default one
        final STTCallback effectiveCallback = customCallback != null ? customCallback : this.callback;

        if (deviceClass == DeviceClassDetector.DeviceClass.LOW) {
            // Use Whisper for low-end devices
            if (whisperEngine != null) {
                whisperEngine.startListening(new WhisperASREngine.ASRCallback() {
                    @Override
                    public void onResult(String text) {
                        if (effectiveCallback != null) {
                            effectiveCallback.onResult(text);
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        if (effectiveCallback != null) {
                            effectiveCallback.onError(error);
                        }
                    }
                });
            }
        } else {
            // Use Vosk for higher-end devices
            if (isListening) return;

            isListening = true;
            isProcessing = false;

            try {
                audioRecord.startRecording();
            } catch (Exception e) {
                Log.e(TAG, "Failed to start audio recording", e);
                CrashLogger.logError(context, e);
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
    }

    private void processAudio(byte[] buffer, int bytesRead, STTCallback callback) {
        try {
            if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                String result = recognizer.getResult();
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

    private void handleRecognitionResult(String jsonResult, STTCallback callback) {
        try {
            Log.d(TAG, "Recognition result: " + jsonResult);

            if (jsonResult == null || jsonResult.isEmpty()) {
                return;
            }

            String text = "";
            float confidence = 0.0f;

            if (jsonResult.contains("\"text\"")) {
                int textStart = jsonResult.indexOf("\"text\":\"") + 8;
                int textEnd = jsonResult.indexOf("\"", textStart);
                if (textEnd > textStart) {
                    text = jsonResult.substring(textStart, textEnd);
                }
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

            Log.i(TAG, "Recognized text: '" + text + "' with confidence: " + confidence);

            // Filter out low-confidence results
            if (confidence < 0.3f && !text.isEmpty()) {
                Log.w(TAG, "Low confidence result filtered out: " + text);
                return;
            }

            if (!text.isEmpty() && callback != null) {
                // Apply Egyptian dialect normalization
                String normalizedText = EgyptianNormalizer.normalize(text);
                callback.onResult(normalizedText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling recognition result", e);
            CrashLogger.logError(context, e);
            if (callback != null) {
                callback.onError(e);
            }
        }
    }

    public void stopListening() {
        if (deviceClass == DeviceClassDetector.DeviceClass.LOW) {
            // Use Whisper for low-end devices
            if (whisperEngine != null) {
                whisperEngine.stopListening();
            }
        } else {
            // Use Vosk for higher-end devices
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
            if (deviceClass == DeviceClassDetector.DeviceClass.LOW) {
                // Clean up Whisper resources for low-end devices
                if (whisperEngine != null) {
                    whisperEngine.destroy();
                    whisperEngine = null;
                }
            } else {
                // Clean up Vosk resources for higher-end devices
                if (voskRecognizer != null) {
                    voskRecognizer.shutdown();
                    voskRecognizer = null;
                }
                if (voskModel != null) {
                    voskModel.close();
                    voskModel = null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing ASR resources", e);
        }

        System.gc(); // Request garbage collection to free memory
    }
}