package com.egyptian.agent.accessibility;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

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
        // In a real implementation, this would trigger appropriate actions
        // such as alerting emergency contacts or asking the user if they're OK
        
        Log.i(TAG, "Handling potential fall with acceleration: " + acceleration);
        
        // Example: Alert the user or trigger emergency protocols
        // This would involve calling methods from EmergencyHandler
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle accuracy changes if needed
    }
}