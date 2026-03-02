package com.egyptian.agent.executor;

import android.content.Context;
import android.util.Log;

import com.egyptian.agent.nlu.IntentResult;
import com.egyptian.agent.nlu.IntentType;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Command Executor
 * 
 * Main executor that routes intents to appropriate controllers.
 * Manages all command execution for the voice assistant.
 * 
 * Features:
 * - Intent routing
 * - Controller management
 * - Async execution
 * - Error handling
 */
public class CommandExecutor {
    private static final String TAG = "CommandExecutor";
    
    // Singleton instance
    private static CommandExecutor instance;
    
    private final Context context;
    private final ExecutorService executorService;
    
    private CommunicationController communicationController;
    private SettingsController settingsController;
    private AppsController appsController;
    private AlarmController alarmController;
    private EmergencyController emergencyController;
    
    /**
     * Private constructor for singleton pattern.
     */
    private CommandExecutor(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        
        initializeControllers();
    }
    
    /**
     * Get singleton instance.
     */
    public static synchronized CommandExecutor getInstance(Context context) {
        if (instance == null) {
            instance = new CommandExecutor(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Initialize all controllers.
     */
    private void initializeControllers() {
        communicationController = new CommunicationController(context);
        settingsController = new SettingsController(context);
        appsController = new AppsController(context);
        alarmController = new AlarmController(context);
        emergencyController = new EmergencyController(context);
        
        Log.i(TAG, "All controllers initialized");
    }
    
    /**
     * Execute intent result.
     */
    public void execute(IntentResult intent) {
        if (intent == null) {
            Log.w(TAG, "Null intent received");
            return;
        }
        
        Log.d(TAG, "Executing intent: " + intent.getIntentType());
        
        executorService.execute(() -> {
            try {
                ExecutorResult result = executeSync(intent);
                Log.d(TAG, "Execution result: " + result);
            } catch (Exception e) {
                Log.e(TAG, "Error executing intent", e);
            }
        });
    }
    
    /**
     * Execute intent synchronously.
     */
    public ExecutorResult executeSync(IntentResult intent) {
        if (intent == null) {
            return ExecutorResult.error("Null intent");
        }
        
        IntentType type = intent.getIntentType();
        
        switch (type) {
            case CALL_CONTACT:
                return communicationController.makeCall(intent);
                
            case SEND_WHATSAPP:
            case SEND_VOICE_MESSAGE:
                return communicationController.sendWhatsApp(intent);
                
            case SEND_SMS:
                return communicationController.sendSms(intent);
                
            case READ_MISSED_CALLS:
                return communicationController.readMissedCalls(intent);
                
            case SET_ALARM:
                return alarmController.setAlarm(intent);
                
            case READ_TIME:
                return alarmController.readTime(intent);
                
            case TOGGLE_WIFI:
                return settingsController.toggleWifi(intent);
                
            case TOGGLE_BLUETOOTH:
                return settingsController.toggleBluetooth(intent);
                
            case TOGGLE_FLASHLIGHT:
                return settingsController.toggleFlashlight(intent);
                
            case OPEN_APP:
                return appsController.openApp(intent);
                
            case EMERGENCY:
                return emergencyController.triggerEmergency(intent);
                
            case SENIOR_ASSIST:
                return communicationController.seniorAssist(intent);
                
            case UNKNOWN:
            default:
                return ExecutorResult.error("Unknown intent: " + type);
        }
    }
    
    /**
     * Cancel current execution.
     */
    public void cancel() {
        Log.d(TAG, "Cancelling current execution");
        
        if (communicationController != null) {
            communicationController.cancel();
        }
        if (alarmController != null) {
            alarmController.cancel();
        }
        if (emergencyController != null) {
            emergencyController.cancel();
        }
    }
    
    /**
     * Clean up resources.
     */
    public void destroy() {
        Log.d(TAG, "Destroying command executor");
        
        cancel();
        
        if (executorService != null) {
            executorService.shutdownNow();
        }
        
        communicationController = null;
        settingsController = null;
        appsController = null;
        alarmController = null;
        emergencyController = null;
        
        instance = null;
    }
}
