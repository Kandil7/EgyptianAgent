package com.egyptian.agent.stt;

import android.content.Context;
import android.util.Log;

import org.vosk.LibVosk;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Vosk Speech-to-Text Engine
 * Handles speech recognition using the Vosk library
 */
public class VoskSTTEngine {
    private static final String TAG = "VoskSTTEngine";
    
    private Context context;
    private Model model;
    private Recognizer recognizer;
    private ExecutorService executorService;
    private boolean isInitialized = false;
    private boolean isListening = false;
    private STTCallback callback;
    
    public VoskSTTEngine(Context context) {
        this(context, "models/vosk-model-small-ar.zip");
    }
    
    public VoskSTTEngine(Context context, String modelPath) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        
        initializeModel(modelPath);
    }
    
    /**
     * Initializes the Vosk model
     * @param modelPath Path to the model in assets
     */
    private void initializeModel(String modelPath) {
        executorService.execute(() -> {
            try {
                // Extract model from assets to internal storage
                String extractedModelPath = extractModelToInternalStorage(modelPath);
                
                if (extractedModelPath == null) {
                    Log.e(TAG, "Failed to extract model: " + modelPath);
                    return;
                }
                
                // Initialize the Vosk model
                model = new Model(extractedModelPath);
                
                // Create recognizer with sample rate of 16kHz
                recognizer = new Recognizer(model, 16000);
                
                isInitialized = true;
                Log.i(TAG, "Vosk STT Engine initialized successfully with model: " + modelPath);
                
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Vosk model: " + modelPath, e);
            }
        });
    }
    
    /**
     * Extracts the model file from assets to internal storage
     * @param modelPath Path to the model in assets
     * @return Path to the extracted model file, or null if failed
     */
    private String extractModelToInternalStorage(String modelPath) {
        try {
            // 1. Check if model already exists in internal storage
            String fileName = modelPath.substring(modelPath.lastIndexOf('/') + 1);
            File modelFile = new File(context.getFilesDir(), fileName);

            if (modelFile.exists()) {
                Log.d(TAG, "Model already exists: " + modelFile.getAbsolutePath());
                return modelFile.getAbsolutePath();
            }

            // 2. If not, extract from assets
            InputStream inputStream = context.getAssets().open(modelPath);

            // Handle compressed model files (common for Vosk models)
            if (modelPath.endsWith(".zip")) {
                // Extract the ZIP file to a directory
                String modelName = fileName.substring(0, fileName.lastIndexOf('.'));
                File modelDir = new File(context.getFilesDir(), modelName);

                if (!modelDir.exists()) {
                    modelDir.mkdirs();
                }

                // Extract ZIP contents to the model directory
                extractZip(inputStream, modelDir);

                // Return the directory path (Vosk expects a directory, not a file)
                Log.d(TAG, "Model extracted to: " + modelDir.getAbsolutePath());
                return modelDir.getAbsolutePath();
            } else {
                // Handle uncompressed model files
                FileOutputStream outputStream = new FileOutputStream(modelFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                inputStream.close();
                outputStream.close();

                // 3. Return the path to the extracted model
                Log.d(TAG, "Model extracted to: " + modelFile.getAbsolutePath());
                return modelFile.getAbsolutePath();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting model to internal storage", e);
            return null;
        }
    }

    /**
     * Extracts a ZIP file to the specified directory
     * @param inputStream Stream containing the ZIP file
     * @param outputDir Directory to extract to
     */
    private void extractZip(InputStream inputStream, File outputDir) throws IOException {
        java.util.zip.ZipInputStream zipInputStream = new java.util.zip.ZipInputStream(inputStream);
        java.util.zip.ZipEntry entry;

        while ((entry = zipInputStream.getNextEntry()) != null) {
            File outputFile = new File(outputDir, entry.getName());

            if (entry.isDirectory()) {
                outputFile.mkdirs();
            } else {
                // Ensure parent directories exist
                outputFile.getParentFile().mkdirs();

                // Extract file
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
                fileOutputStream.close();
            }

            zipInputStream.closeEntry();
        }

        zipInputStream.close();
        inputStream.close();
    }
    
    /**
     * Starts listening for speech
     * @param callback Callback to receive the recognition result
     */
    public void startListening(STTCallback callback) {
        if (!isInitialized) {
            Log.e(TAG, "Vosk STT Engine not initialized");
            return;
        }

        this.callback = callback;
        isListening = true;

        startAudioRecording();
    }

    /**
     * Starts audio recording and feeds to the Vosk recognizer
     */
    private void startAudioRecording() {
        executorService.execute(() -> {
            android.media.AudioRecord audioRecord = null;
            try {
                int bufferSize = android.media.AudioRecord.getMinBufferSize(
                    16000, // Sample rate
                    android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT
                );

                audioRecord = new android.media.AudioRecord(
                    android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    16000,
                    android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                );

                audioRecord.startRecording();

                byte[] buffer = new byte[bufferSize];
                while (isListening) {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);

                    if (bytesRead > 0) {
                        // Feed audio data to the recognizer
                        recognizer.acceptWaveForm(buffer, bytesRead);

                        // Check if we have a partial result
                        String partialResult = recognizer.getResult();
                        if (partialResult != null && !partialResult.isEmpty()) {
                            Log.d(TAG, "Partial result: " + partialResult);
                        }
                    }
                }

                // Get final result
                String finalResult = recognizer.getFinalResult();
                Log.d(TAG, "Final recognition result: " + finalResult);

                if (isListening && callback != null && finalResult != null) {
                    // Parse the result to extract the text
                    org.json.JSONObject jsonObject = new org.json.JSONObject(finalResult);
                    String text = jsonObject.getString("text");
                    callback.onResult(text);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error during audio recording and recognition", e);
                if (callback != null) {
                    callback.onResult(""); // Indicate error with empty result
                }
            } finally {
                if (audioRecord != null) {
                    audioRecord.stop();
                    audioRecord.release();
                }
            }
        });
    }
    
    /**
     * Simulates the recognition process
     */
    private void simulateRecognition() {
        executorService.execute(() -> {
            try {
                // Simulate recognition delay
                Thread.sleep(2000);
                
                // Simulate recognition result
                String result = "يا حكيم اتصل بماما"; // Example Egyptian dialect command
                
                if (isListening && callback != null) {
                    callback.onResult(result);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * Pauses listening
     */
    public void pauseListening() {
        isListening = false;
        Log.d(TAG, "STT listening paused");
    }
    
    /**
     * Resumes listening
     */
    public void resumeListening() {
        isListening = true;
        Log.d(TAG, "STT listening resumed");
    }
    
    /**
     * Checks if the engine is initialized
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Destroys the STT engine and cleans up resources
     */
    public void destroy() {
        isListening = false;
        
        if (recognizer != null) {
            recognizer.close();
        }
        
        if (model != null) {
            model.close();
        }
        
        if (executorService != null) {
            executorService.shutdownNow();
        }
        
        Log.i(TAG, "Vosk STT Engine destroyed");
    }
    
    /**
     * Recognizes audio from a byte buffer
     * @param audioBuffer The audio data buffer
     * @param bufferSize Size of the buffer
     * @return Recognized text or null if not available
     */
    public String recognizeAudio(byte[] audioBuffer, int bufferSize) {
        if (!isInitialized || recognizer == null) {
            Log.e(TAG, "Recognizer not initialized");
            return null;
        }

        try {
            // Feed audio data to the recognizer
            recognizer.acceptWaveForm(audioBuffer, bufferSize);

            // Get the result
            String result = recognizer.getResult();
            if (result != null && !result.isEmpty()) {
                try {
                    org.json.JSONObject jsonObject = new org.json.JSONObject(result);
                    return jsonObject.getString("text");
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error parsing recognition result", e);
                    return null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error recognizing audio", e);
        }

        return null;
    }

    /**
     * Recognizes audio from a file path
     * @param audioFilePath Path to the audio file
     * @return Recognized text or null if not available
     */
    public String recognizeFromFile(String audioFilePath) {
        if (!isInitialized || recognizer == null) {
            Log.e(TAG, "Recognizer not initialized");
            return null;
        }

        try {
            // Read audio file
            java.io.FileInputStream fis = new java.io.FileInputStream(audioFilePath);
            byte[] buffer = new byte[4096];
            int bytesRead;

            // Process audio in chunks
            while ((bytesRead = fis.read(buffer)) != -1) {
                recognizer.acceptWaveForm(buffer, bytesRead);
            }
            fis.close();

            // Get the final result
            String finalResult = recognizer.getFinalResult();
            if (finalResult != null && !finalResult.isEmpty()) {
                try {
                    org.json.JSONObject jsonObject = new org.json.JSONObject(finalResult);
                    return jsonObject.getString("text");
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error parsing final recognition result", e);
                    return null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error recognizing audio from file: " + audioFilePath, e);
        }

        return null;
    }

    /**
     * Callback interface for STT results
     */
    public interface STTCallback {
        void onResult(String result);
    }
}