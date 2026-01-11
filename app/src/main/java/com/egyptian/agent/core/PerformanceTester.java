package com.egyptian.agent.core;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Performance testing class for Honor X6c target device
 * Tests various components and measures performance metrics
 */
public class PerformanceTester {
    private static final String TAG = "PerformanceTester";
    
    private final Context context;
    private final DeviceClassDetector.DeviceClass deviceClass;
    
    // Performance metrics
    private final Map<String, Object> performanceMetrics = new HashMap<>();
    
    public PerformanceTester(Context context) {
        this.context = context;
        this.deviceClass = ((MainApplication) context.getApplicationContext()).getDeviceClass();
        
        Log.i(TAG, "PerformanceTester initialized for device class: " + deviceClass.name() + 
              " on device: " + Build.MODEL);
    }
    
    /**
     * Runs comprehensive performance tests
     */
    public PerformanceResults runComprehensiveTests() {
        Log.i(TAG, "Starting comprehensive performance tests for device class: " + deviceClass.name());
        
        PerformanceResults results = new PerformanceResults();
        
        // Test device class detection
        testDeviceClassDetection(results);
        
        // Test model loading performance
        testModelLoadingPerformance(results);
        
        // Test wake word detection performance
        testWakeWordDetectionPerformance(results);
        
        // Test ASR performance
        testASRPerformance(results);
        
        // Test NLU processing performance
        testNLUProcessingPerformance(results);
        
        // Test TTS performance
        testTTSPeformance(results);
        
        // Test NNAPI delegation
        testNNAPIDelegation(results);
        
        // Test background processing
        testBackgroundProcessing(results);
        
        // Test caching mechanisms
        testCachingMechanisms(results);
        
        Log.i(TAG, "Comprehensive performance tests completed");
        return results;
    }
    
    /**
     * Tests device class detection performance
     */
    private void testDeviceClassDetection(PerformanceResults results) {
        long startTime = System.currentTimeMillis();
        DeviceClassDetector.DeviceClass detectedClass = DeviceClassDetector.detectDevice(context);
        long endTime = System.currentTimeMillis();
        
        results.deviceClassDetectionTime = endTime - startTime;
        results.detectedDeviceClass = detectedClass;
        
        Log.d(TAG, String.format("Device class detection: %s in %d ms", 
                detectedClass.name(), results.deviceClassDetectionTime));
    }
    
    /**
     * Tests model loading performance
     */
    private void testModelLoadingPerformance(PerformanceResults results) {
        long startTime = System.currentTimeMillis();
        
        ModelManager modelManager = new ModelManager(context);
        CountDownLatch latch = new CountDownLatch(1);
        
        modelManager.initializeModels(new ModelManager.ModelInitializationCallback() {
            @Override
            public void onComplete(boolean success) {
                long endTime = System.currentTimeMillis();
                results.modelLoadingTime = endTime - startTime;
                results.modelLoadingSuccess = success;
                
                Log.d(TAG, String.format("Model loading: %s in %d ms", 
                        success ? "SUCCESS" : "FAILED", results.modelLoadingTime));
                
                // Clean up
                modelManager.cleanup();
                latch.countDown();
            }

            @Override
            public void onError(Exception error) {
                long endTime = System.currentTimeMillis();
                results.modelLoadingTime = endTime - startTime;
                results.modelLoadingSuccess = false;
                results.modelLoadingError = error.getMessage();
                
                Log.e(TAG, "Model loading failed: " + error.getMessage());
                
                // Clean up
                modelManager.cleanup();
                latch.countDown();
            }
        });
        
        try {
            latch.await(30, TimeUnit.SECONDS); // Wait up to 30 seconds
        } catch (InterruptedException e) {
            Log.e(TAG, "Model loading test interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Tests wake word detection performance
     */
    private void testWakeWordDetectionPerformance(PerformanceResults results) {
        long startTime = System.currentTimeMillis();
        
        try {
            WakeWordDetector wakeDetector = new WakeWordDetector(context, new WakeWordDetector.WakeWordCallback() {
                @Override
                public void onWakeWordDetected() {
                    long endTime = System.currentTimeMillis();
                    results.wakeWordDetectionTime = endTime - startTime;
                    
                    Log.d(TAG, String.format("Wake word detection completed in %d ms", 
                            results.wakeWordDetectionTime));
                    
                    // Stop the detector
                    wakeDetector.stopListening();
                }
            });
            
            // Start listening for a short time
            wakeDetector.startListening();
            
            // Wait for a reasonable time for wake word detection
            Thread.sleep(5000); // 5 seconds
            
            // Stop if not detected
            wakeDetector.stopListening();
            wakeDetector.destroy();
            
            if (results.wakeWordDetectionTime == 0) {
                results.wakeWordDetectionTime = System.currentTimeMillis() - startTime;
                Log.d(TAG, "Wake word detection timeout after " + results.wakeWordDetectionTime + " ms");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during wake word detection test", e);
            results.wakeWordDetectionError = e.getMessage();
        }
    }
    
    /**
     * Tests ASR performance
     */
    private void testASRPerformance(PerformanceResults results) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Get the model path from the model manager
            ModelManager modelManager = new ModelManager(context);
            String asrModelPath = modelManager.getAsrModelPath();
            
            if (asrModelPath != null) {
                VoskSTTEngine asrEngine = new VoskSTTEngine(context, asrModelPath);
                
                // For this test, we'll just initialize and measure that
                // In a real test, we would provide audio input
                long initTime = System.currentTimeMillis() - startTime;
                
                Log.d(TAG, String.format("ASR engine initialization completed in %d ms", initTime));
                
                asrEngine.destroy();
            } else {
                Log.w(TAG, "No ASR model path available for testing");
                results.asrError = "No ASR model available";
            }
            
            results.asrInitializationTime = System.currentTimeMillis() - startTime;
        } catch (Exception e) {
            Log.e(TAG, "Error during ASR performance test", e);
            results.asrError = e.getMessage();
        }
    }
    
    /**
     * Tests NLU processing performance
     */
    private void testNLUProcessingPerformance(PerformanceResults results) {
        long startTime = System.currentTimeMillis();
        
        try {
            ModelManager modelManager = new ModelManager(context);
            String llmModelPath = modelManager.getLlmModelPath();
            
            if (llmModelPath != null) {
                Gemma2NLUProcessor nluProcessor = new Gemma2NLUProcessor(context, llmModelPath);
                nluProcessor.initialize();
                
                // Test with a sample input
                String testInput = "اتصل بأمي";
                
                CountDownLatch latch = new CountDownLatch(1);
                nluProcessor.processText(testInput, new Gemma2NLUProcessor.NLUResultCallback() {
                    @Override
                    public void onResult(Gemma2NLUProcessor.NLUResult result) {
                        long endTime = System.currentTimeMillis();
                        results.nluProcessingTime = endTime - startTime;
                        results.nluResult = result;
                        
                        Log.d(TAG, String.format("NLU processing completed in %d ms: %s", 
                                results.nluProcessingTime, result.toString()));
                        
                        nluProcessor.destroy();
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception error) {
                        long endTime = System.currentTimeMillis();
                        results.nluProcessingTime = endTime - startTime;
                        results.nluError = error.getMessage();
                        
                        Log.e(TAG, "NLU processing failed: " + error.getMessage());
                        
                        nluProcessor.destroy();
                        latch.countDown();
                    }
                });
                
                latch.await(15, TimeUnit.SECONDS); // Wait up to 15 seconds
            } else {
                Log.w(TAG, "No LLM model path available for NLU testing");
                results.nluError = "No LLM model available";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during NLU performance test", e);
            results.nluError = e.getMessage();
        }
    }
    
    /**
     * Tests TTS performance
     */
    private void testTTSPeformance(PerformanceResults results) {
        long startTime = System.currentTimeMillis();
        
        try {
            ModelManager modelManager = new ModelManager(context);
            String ttsModelPath = modelManager.getTtsModelPath();
            
            if (ttsModelPath != null) {
                PiperTTSEngine ttsEngine = new PiperTTSEngine(context, ttsModelPath);
                ttsEngine.initialize();
                
                // Test with a sample text
                String testText = "السلام عليكم ورحمة الله وبركاته";
                
                CountDownLatch latch = new CountDownLatch(1);
                ttsEngine.speak(testText, new PiperTTSEngine.TTSCompletionCallback() {
                    @Override
                    public void onComplete() {
                        long endTime = System.currentTimeMillis();
                        results.ttsProcessingTime = endTime - startTime;
                        
                        Log.d(TAG, String.format("TTS processing completed in %d ms", 
                                results.ttsProcessingTime));
                        
                        ttsEngine.destroy();
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception error) {
                        long endTime = System.currentTimeMillis();
                        results.ttsProcessingTime = endTime - startTime;
                        results.ttsError = error.getMessage();
                        
                        Log.e(TAG, "TTS processing failed: " + error.getMessage());
                        
                        ttsEngine.destroy();
                        latch.countDown();
                    }
                });
                
                latch.await(10, TimeUnit.SECONDS); // Wait up to 10 seconds
            } else {
                Log.w(TAG, "No TTS model path available for testing");
                results.ttsError = "No TTS model available";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during TTS performance test", e);
            results.ttsError = e.getMessage();
        }
    }
    
    /**
     * Tests NNAPI delegation
     */
    private void testNNAPIDelegation(PerformanceResults results) {
        long startTime = System.currentTimeMillis();
        
        try {
            NNAPIDelegator nnapiDelegator = new NNAPIDelegator(context);
            nnapiDelegator.initialize();
            
            long endTime = System.currentTimeMillis();
            results.nnapiInitializationTime = endTime - startTime;
            results.nnapiAvailable = nnapiDelegator.isAvailable();
            results.nnapiMetrics = nnapiDelegator.getPerformanceMetrics();
            
            Log.d(TAG, String.format("NNAPI delegation test completed in %d ms, available: %b", 
                    results.nnapiInitializationTime, results.nnapiAvailable));
            
            nnapiDelegator.destroy();
        } catch (Exception e) {
            Log.e(TAG, "Error during NNAPI delegation test", e);
            results.nnapiError = e.getMessage();
        }
    }
    
    /**
     * Tests background processing
     */
    private void testBackgroundProcessing(PerformanceResults results) {
        long startTime = System.currentTimeMillis();
        
        try {
            BackgroundTaskManager taskManager = new BackgroundTaskManager(context);
            
            // Execute a simple task to test the manager
            CountDownLatch latch = new CountDownLatch(1);
            
            taskManager.executeGeneralTask(() -> {
                // Simulate some work
                try {
                    Thread.sleep(100); // 100ms of work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                long endTime = System.currentTimeMillis();
                results.backgroundProcessingTime = endTime - startTime;
                results.backgroundTaskMetrics = taskManager.getMetrics();
                
                Log.d(TAG, String.format("Background processing test completed in %d ms", 
                        results.backgroundProcessingTime));
                
                latch.countDown();
            });
            
            latch.await(5, TimeUnit.SECONDS); // Wait up to 5 seconds
            taskManager.destroy();
        } catch (Exception e) {
            Log.e(TAG, "Error during background processing test", e);
            results.backgroundProcessingError = e.getMessage();
        }
    }
    
    /**
     * Tests caching mechanisms
     */
    private void testCachingMechanisms(PerformanceResults results) {
        long startTime = System.currentTimeMillis();
        
        try {
            CacheManager cacheManager = new CacheManager(context);
            
            // Test contact caching
            cacheManager.loadContactsToCache();
            
            // Test result caching
            cacheManager.cacheResult("test_key", "test_value");
            Object cachedValue = cacheManager.getCachedResult("test_key");
            
            long endTime = System.currentTimeMillis();
            results.cachingTime = endTime - startTime;
            results.cacheStats = cacheManager.getCacheStats();
            results.cachingSuccess = cachedValue != null && "test_value".equals(cachedValue);
            
            Log.d(TAG, String.format("Caching mechanisms test completed in %d ms, success: %b", 
                    results.cachingTime, results.cachingSuccess));
            
            cacheManager.destroy();
        } catch (Exception e) {
            Log.e(TAG, "Error during caching mechanisms test", e);
            results.cachingError = e.getMessage();
        }
    }
    
    /**
     * Performance results class
     */
    public static class PerformanceResults {
        // Device detection
        public long deviceClassDetectionTime = 0;
        public DeviceClassDetector.DeviceClass detectedDeviceClass;
        
        // Model loading
        public long modelLoadingTime = 0;
        public boolean modelLoadingSuccess = false;
        public String modelLoadingError;
        
        // Wake word detection
        public long wakeWordDetectionTime = 0;
        public String wakeWordDetectionError;
        
        // ASR performance
        public long asrInitializationTime = 0;
        public String asrError;
        
        // NLU processing
        public long nluProcessingTime = 0;
        public Gemma2NLUProcessor.NLUResult nluResult;
        public String nluError;
        
        // TTS performance
        public long ttsProcessingTime = 0;
        public String ttsError;
        
        // NNAPI delegation
        public long nnapiInitializationTime = 0;
        public boolean nnapiAvailable = false;
        public NNAPIDelegator.NNAPIPerformanceMetrics nnapiMetrics;
        public String nnapiError;
        
        // Background processing
        public long backgroundProcessingTime = 0;
        public BackgroundTaskManager.TaskManagerMetrics backgroundTaskMetrics;
        public String backgroundProcessingError;
        
        // Caching mechanisms
        public long cachingTime = 0;
        public boolean cachingSuccess = false;
        public CacheManager.CacheStats cacheStats;
        public String cachingError;
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("PerformanceResults{\n");
            sb.append("  deviceClassDetectionTime=").append(deviceClassDetectionTime).append("ms\n");
            sb.append("  modelLoadingTime=").append(modelLoadingTime).append("ms, success=").append(modelLoadingSuccess).append("\n");
            sb.append("  wakeWordDetectionTime=").append(wakeWordDetectionTime).append("ms\n");
            sb.append("  asrInitializationTime=").append(asrInitializationTime).append("ms\n");
            sb.append("  nluProcessingTime=").append(nluProcessingTime).append("ms\n");
            sb.append("  ttsProcessingTime=").append(ttsProcessingTime).append("ms\n");
            sb.append("  nnapiInitializationTime=").append(nnapiInitializationTime).append("ms, available=").append(nnapiAvailable).append("\n");
            sb.append("  backgroundProcessingTime=").append(backgroundProcessingTime).append("ms\n");
            sb.append("  cachingTime=").append(cachingTime).append("ms, success=").append(cachingSuccess).append("\n");
            sb.append("}");
            
            return sb.toString();
        }
        
        /**
         * Gets a summary of the performance results
         */
        public String getSummary() {
            return String.format(
                "Performance Summary:\n" +
                "- Device Detection: %d ms\n" +
                "- Model Loading: %d ms (%s)\n" +
                "- Wake Word: %d ms\n" +
                "- ASR Init: %d ms\n" +
                "- NLU Processing: %d ms\n" +
                "- TTS Processing: %d ms\n" +
                "- NNAPI Available: %b\n" +
                "- Background Processing: %d ms\n" +
                "- Caching: %d ms (%s)",
                deviceClassDetectionTime,
                modelLoadingTime, modelLoadingSuccess ? "SUCCESS" : "FAILED",
                wakeWordDetectionTime,
                asrInitializationTime,
                nluProcessingTime,
                ttsProcessingTime,
                nnapiAvailable,
                backgroundProcessingTime,
                cachingTime, cachingSuccess ? "SUCCESS" : "FAILED"
            );
        }
    }
}