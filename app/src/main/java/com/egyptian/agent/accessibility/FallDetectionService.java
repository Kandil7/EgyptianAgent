package com.egyptian.agent.accessibility;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.util.Log;
import com.egyptian.agent.utils.CrashLogger;

public class FallDetectionService extends Service implements SensorEventListener {

    private static final String TAG = "FallDetectionService";
    
    private SensorManager sensorManager;
    private Sensor accelerometer;
    
    // Fall detection parameters
    private static final int FALL_THRESHOLD = 15; // Adjust based on testing
    private static final int VIBRATION_THRESHOLD = 5;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Fall Detection Service created");
        
        // Initialize sensor manager and accelerometer
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Fall Detection Service started");
        
        // Register the accelerometer sensor
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.e(TAG, "Accelerometer not available on this device");
            CrashLogger.logError(this, new Exception("Accelerometer not available on this device"));
        }
        
        // Start foreground service to keep it running
        startForeground(1, new android.app.Notification());
        
        return START_STICKY; // Restart if killed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // No binding for this service
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Fall Detection Service destroyed");
        
        // Unregister the sensor listener
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            
            // Calculate the magnitude of acceleration
            double acceleration = Math.sqrt(x * x + y * y + z * z);
            
            // Check if acceleration exceeds fall threshold
            if (acceleration > FALL_THRESHOLD) {
                Log.w(TAG, "Potential fall detected with acceleration: " + acceleration);
                
                // Here you could implement more sophisticated fall detection logic
                // For example, checking for a sudden impact followed by lack of movement
                
                // For now, just log the potential fall
                handlePotentialFall(acceleration);
            }
        }
    }

    private void handlePotentialFall(double acceleration) {
        // Trigger appropriate actions for potential fall
        Log.i(TAG, "Handling potential fall with acceleration: " + acceleration);

        // Vibrate to alert the user
        VibrationManager.vibrateEmergency(this);

        // Speak an alert to the user
        TTSManager.speak(this, "يا كبير، لقيت إنك وقعت؟ لو مفيش رد هيتم الاتصال بجهات الطوارئ خلال 10 ثواني");

        // Start a countdown timer to trigger emergency if no response
        new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check if user responded (this would be implemented with voice recognition)
            boolean userResponded = checkUserResponse();

            if (!userResponded) {
                Log.e(TAG, "No response after potential fall - triggering emergency");
                // Trigger emergency response
                com.egyptian.agent.executors.EmergencyHandler.trigger(this, true); // true indicates fall-triggered
            } else {
                Log.i(TAG, "User responded after potential fall - no emergency needed");
            }
        }, 10000); // 10 seconds to respond
    }

    /**
     * Checks if the user has responded to the fall alert
     * Uses voice recognition to detect "I'm fine" or similar
     */
    private boolean checkUserResponse() {
        // For now, return false to simulate no response
        // This would involve starting a voice recognition session to listen for
        // phrases like "I'm fine", "I'm okay", "ana ta2ib", "ana mokay", etc.
        return false;
    }

    /**
     * Starts voice recognition to listen for user response after a fall
     */
    private void startVoiceRecognitionForResponse() {
        // Create an intent for voice recognition
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG"); // Egyptian Arabic
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "قولي إنت بخير لو محتاجش مساعدة");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        // Start voice recognition activity to listen for user response
        // and handle the response in onActivityResult
        // For now, we'll just log that this would happen
        Log.d(TAG, "Voice recognition would start to listen for user response");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }
}