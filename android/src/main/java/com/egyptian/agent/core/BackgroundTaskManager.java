package com.egyptian.agent.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Background task manager for smooth UI performance
 * Handles all computationally intensive operations off the main thread
 */
public class BackgroundTaskManager {
    private static final String TAG = "BackgroundTaskManager";
    
    private final Context context;
    private final DeviceClassDetector.DeviceClass deviceClass;
    
    // Thread pools for different types of tasks
    private final ExecutorService inferenceExecutor;      // For AI model inference
    private final ExecutorService audioProcessingExecutor; // For audio processing
    private final ExecutorService networkExecutor;         // For network operations
    private final ExecutorService fileIOExecutor;          // For file I/O operations
    private final ExecutorService generalExecutor;         // For general background tasks
    
    // Main thread handler for UI updates
    private final Handler mainHandler;
    
    public BackgroundTaskManager(Context context) {
        this.context = context;
        this.deviceClass = ((MainApplication) context.getApplicationContext()).getDeviceClass();
        
        // Create thread pools based on device class
        this.inferenceExecutor = createExecutorForDeviceClass("inference");
        this.audioProcessingExecutor = createExecutorForDeviceClass("audio");
        this.networkExecutor = Executors.newFixedThreadPool(getThreadPoolSize(2));
        this.fileIOExecutor = Executors.newFixedThreadPool(getThreadPoolSize(3));
        this.generalExecutor = createExecutorForDeviceClass("general");
        
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        Log.i(TAG, "BackgroundTaskManager initialized for device class: " + deviceClass.name());
    }
    
    /**
     * Creates an executor service based on device class
     */
    private ExecutorService createExecutorForDeviceClass(String name) {
        int poolSize = getThreadPoolSize(1); // Default to 1 for most tasks
        switch (deviceClass) {
            case LOW:
                // For low-end devices, use minimal threads to conserve resources
                poolSize = 1;
                break;
            case MID:
                // For mid-range devices (like Honor X6c), use moderate threading
                poolSize = 2;
                break;
            case HIGH:
                // For high-end devices, use more threads
                poolSize = 3;
                break;
            case ELITE:
                // For elite devices, use maximum threads
                poolSize = 4;
                break;
        }
        
        Log.d(TAG, "Creating " + name + " executor with pool size: " + poolSize);
        return Executors.newFixedThreadPool(poolSize);
    }
    
    /**
     * Gets appropriate thread pool size based on device class
     */
    private int getThreadPoolSize(int defaultSize) {
        switch (deviceClass) {
            case LOW:
                return Math.min(defaultSize, 1);
            case MID:
                return Math.min(defaultSize, 2);
            case HIGH:
                return Math.min(defaultSize, 3);
            case ELITE:
                return Math.min(defaultSize, 4);
            default:
                return defaultSize;
        }
    }
    
    /**
     * Executes an inference task (AI model processing)
     */
    public Future<?> executeInferenceTask(Runnable task) {
        Log.d(TAG, "Executing inference task on background thread");
        return inferenceExecutor.submit(task);
    }
    
    /**
     * Executes an audio processing task
     */
    public Future<?> executeAudioProcessingTask(Runnable task) {
        Log.d(TAG, "Executing audio processing task on background thread");
        return audioProcessingExecutor.submit(task);
    }
    
    /**
     * Executes a network task
     */
    public Future<?> executeNetworkTask(Runnable task) {
        Log.d(TAG, "Executing network task on background thread");
        return networkExecutor.submit(task);
    }
    
    /**
     * Executes a file I/O task
     */
    public Future<?> executeFileIOTask(Runnable task) {
        Log.d(TAG, "Executing file I/O task on background thread");
        return fileIOExecutor.submit(task);
    }
    
    /**
     * Executes a general background task
     */
    public Future<?> executeGeneralTask(Runnable task) {
        Log.d(TAG, "Executing general task on background thread");
        return generalExecutor.submit(task);
    }
    
    /**
     * Posts a task to the main UI thread
     */
    public void postToUIThread(Runnable task) {
        mainHandler.post(task);
    }
    
    /**
     * Posts a task to the main UI thread after a delay
     */
    public void postToUIThreadDelayed(Runnable task, long delayMillis) {
        mainHandler.postDelayed(task, delayMillis);
    }
    
    /**
     * Executes a task with proper threading based on its type
     */
    public Future<?> executeTask(TaskType taskType, Runnable task) {
        switch (taskType) {
            case INFERENCE:
                return executeInferenceTask(task);
            case AUDIO_PROCESSING:
                return executeAudioProcessingTask(task);
            case NETWORK:
                return executeNetworkTask(task);
            case FILE_IO:
                return executeFileIOTask(task);
            case GENERAL:
            default:
                return executeGeneralTask(task);
        }
    }
    
    /**
     * Task type enumeration
     */
    public enum TaskType {
        INFERENCE,
        AUDIO_PROCESSING,
        NETWORK,
        FILE_IO,
        GENERAL
    }
    
    /**
     * Gets performance metrics for the background task manager
     */
    public TaskManagerMetrics getMetrics() {
        return new TaskManagerMetrics(
            deviceClass,
            inferenceExecutor,
            audioProcessingExecutor,
            networkExecutor,
            fileIOExecutor,
            generalExecutor
        );
    }
    
    /**
     * Metrics class for task manager performance
     */
    public static class TaskManagerMetrics {
        public final DeviceClassDetector.DeviceClass deviceClass;
        public final int inferencePoolSize;
        public final int audioPoolSize;
        public final int networkPoolSize;
        public final int fileIOPoolSize;
        public final int generalPoolSize;
        
        public TaskManagerMetrics(
            DeviceClassDetector.DeviceClass deviceClass,
            ExecutorService inferenceExecutor,
            ExecutorService audioExecutor,
            ExecutorService networkExecutor,
            ExecutorService fileIOExecutor,
            ExecutorService generalExecutor) {
            
            this.deviceClass = deviceClass;
            this.inferencePoolSize = getPoolSize(inferenceExecutor);
            this.audioPoolSize = getPoolSize(audioExecutor);
            this.networkPoolSize = getPoolSize(networkExecutor);
            this.fileIOPoolSize = getPoolSize(fileIOExecutor);
            this.generalPoolSize = getPoolSize(generalExecutor);
        }
        
        private static int getPoolSize(ExecutorService executor) {
            if (executor instanceof java.util.concurrent.ThreadPoolExecutor) {
                return ((java.util.concurrent.ThreadPoolExecutor) executor).getCorePoolSize();
            }
            return 1; // Default fallback
        }
        
        @Override
        public String toString() {
            return String.format(
                "TaskManagerMetrics{deviceClass=%s, inferencePool=%d, audioPool=%d, networkPool=%d, fileIOPool=%d, generalPool=%d}",
                deviceClass.name(), inferencePoolSize, audioPoolSize, networkPoolSize, fileIOPoolSize, generalPoolSize
            );
        }
    }
    
    /**
     * Cleans up resources
     */
    public void destroy() {
        Log.i(TAG, "Shutting down BackgroundTaskManager");
        
        // Shutdown all executors gracefully
        shutdownExecutor(inferenceExecutor, "inference");
        shutdownExecutor(audioProcessingExecutor, "audio");
        shutdownExecutor(networkExecutor, "network");
        shutdownExecutor(fileIOExecutor, "fileIO");
        shutdownExecutor(generalExecutor, "general");
        
        Log.i(TAG, "BackgroundTaskManager destroyed");
    }
    
    /**
     * Shuts down an executor service gracefully
     */
    private void shutdownExecutor(ExecutorService executor, String name) {
        try {
            executor.shutdown(); // Disable new tasks from being submitted
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                Log.w(TAG, name + " executor did not terminate in time, forcing shutdown");
                executor.shutdownNow(); // Cancel currently executing tasks
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    Log.e(TAG, name + " executor did not terminate after force shutdown");
                }
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while shutting down " + name + " executor", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}