package com.egyptian.agent.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;

/**
 * Wrapper for Android SensorManager with additional functionality
 * for the Egyptian Agent project
 */
public class SensorManager implements SensorEventListener {
    private static final String TAG = "SensorManager";
    private android.hardware.SensorManager androidSensorManager;
    private Sensor accelerometer;
    private Context context;

    public interface FallDetectionCallback {
        void onFallDetected(Location location);
        void onFallConfirmationTimeout();
        void onFalseAlarm();
    }

    private FallDetectionCallback fallDetectionCallback;

    public SensorManager(Context context) {
        this.context = context;
        this.androidSensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = androidSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void registerAccelerometer(SensorEventListener listener) {
        androidSensorManager.registerListener(listener, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregisterAccelerometer(SensorEventListener listener) {
        androidSensorManager.unregisterListener(listener);
    }

    public void enableFallDetection(FallDetectionCallback callback) {
        this.fallDetectionCallback = callback;
        androidSensorManager.registerListener(this, accelerometer, android.hardware.SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void disableFallDetection() {
        androidSensorManager.unregisterListener(this);
        this.fallDetectionCallback = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Implementation will be handled by the specific fall detection service
        if (fallDetectionCallback != null) {
            // Forward the sensor data to the fall detection algorithm
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not implemented
    }
}