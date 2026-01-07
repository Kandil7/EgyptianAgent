package com.egyptian.agent.accessibility;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.executors.EmergencyHandler;
import com.egyptian.agent.core.VibrationManager;

public class FallDetector implements SensorEventListener {

    private static final String TAG = "FallDetector";
    private static final int FALL_ACCELERATION_THRESHOLD = 20; // m/s²
    private static final int POST_FALL_MOVEMENT_THRESHOLD = 2;  // m/s² (low movement after fall)
    private static final int FALL_CONFIRMATION_TIME = 5000;     // 5 seconds
    private static final int MOVEMENT_CHECK_INTERVAL = 1000;    // 1 second

    private Context context;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isFallSuspected = false;
    private long fallStartTime = 0;
    private float lastX, lastY, lastZ;
    private boolean isMonitoring = false;
    private Handler mainHandler;

    private static FallDetector instance;

    private FallDetector(Context context) {
        this.context = context;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized void start(Context context) {
        if (instance == null) {
            instance = new FallDetector(context.getApplicationContext());
        }
        instance.startMonitoring();
    }

    public static synchronized void stop(Context context) {
        if (instance != null) {
            instance.stopMonitoring();
            instance = null;
        }
    }

    public static synchronized boolean isMonitoring() {
        return instance != null && instance.isMonitoring;
    }

    private void startMonitoring() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            isMonitoring = true;
            Log.i(TAG, "Fall detection monitoring started");
        } else {
            Log.w(TAG, "Accelerometer not available on this device");
            TTSManager.speak(context, "كاشف السقوط مش متاح على الموبايل ده");
        }
    }

    private void stopMonitoring() {
        sensorManager.unregisterListener(this);
        isMonitoring = false;
        isFallSuspected = false;
        Log.i(TAG, "Fall detection monitoring stopped");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Calculate acceleration magnitude ignoring gravity
            double acceleration = Math.sqrt(x*x + y*y + z*z) - SensorManager.GRAVITY_EARTH;
            acceleration = Math.abs(acceleration);

            // For debugging
            if (acceleration > 10) {
                Log.d(TAG, "Acceleration: " + acceleration);
            }

            if (!isFallSuspected) {
                // Check for initial fall impact (high acceleration)
                if (acceleration > FALL_ACCELERATION_THRESHOLD) {
                    Log.w(TAG, "Potential fall detected! Initial acceleration: " + acceleration);
                    isFallSuspected = true;
                    fallStartTime = System.currentTimeMillis();
                    lastX = x;
                    lastY = y;
                    lastZ = z;

                    // Start confirmation process
                    startFallConfirmationProcess();
                }
            } else {
                // Track movement during confirmation period
                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }
    }

    private void startFallConfirmationProcess() {
        // Schedule the confirmation check
        mainHandler.postDelayed(this::checkFallConfirmation, FALL_CONFIRMATION_TIME);
    }

    private void checkFallConfirmation() {
        if (!isFallSuspected) return;

        // Check if the confirmation period has elapsed
        if (System.currentTimeMillis() - fallStartTime < FALL_CONFIRMATION_TIME) {
            // Reschedule if not enough time has passed
            mainHandler.postDelayed(this::checkFallConfirmation, 100);
            return;
        }

        // Check for minimal movement after the fall (person is down and not moving much)
        double movement = Math.sqrt(
            Math.pow(lastX, 2) +
            Math.pow(lastY, 2) +
            Math.pow(lastZ, 2)
        );

        Log.i(TAG, "Post-fall movement level: " + movement);

        if (movement < POST_FALL_MOVEMENT_THRESHOLD) {
            // Fall confirmed - trigger emergency
            confirmFall();
        } else {
            // False alarm - person got up or was just moving suddenly
            cancelFallSuspicion();
        }
    }

    private void confirmFall() {
        Log.e(TAG, "FALL CONFIRMED! Triggering emergency response");
        isFallSuspected = false;

        // Trigger emergency without confirmation for falls
        EmergencyHandler.trigger(context, true);

        // Special announcement for fall detection
        TTSManager.speak(context, "يا كبير! لقيت إنك وقعت. بيتصل بالإسعاف دلوقتي! إتقعد مكانك ومتتحركش.");

        // Strong emergency vibration pattern (placeholder)
        // VibrationManager.vibratePattern(context, new long[]{0, 500, 200, 500, 200, 500});

        // Schedule a follow-up check
        mainHandler.postDelayed(this::checkIfUserIsOk, 60000); // Check after 1 minute
    }

    private void checkIfUserIsOk() {
        // In a real implementation, we would check if emergency is still active
        // if (!EmergencyHandler.isEmergencyActive()) {
        //     return; // Emergency was already cancelled
        // }

        TTSManager.speak(context, "يا كبير، إيه الأخبار؟ قول 'أنا كويس' لو اتكنت من السقوط");

        // SpeechConfirmation.waitForConfirmation(context, 30000, userIsOk -> {
        //     if (userIsOk) {
        //         TTSManager.speak(context, "الحمد لله. هسيب الإتصالات دي شغالة لحد ما المساعدة تيجي");
        //     } else {
        //         TTSManager.speak(context, "خلاص، بعت إشارة تاني للنجدة. ركز معايا، قوللي فين بتاعك بالظبط");
        //         // In a real app, we would gather more location information here
        //     }
        // });
    }

    private void cancelFallSuspicion() {
        Log.i(TAG, "Fall suspicion cancelled - false alarm");
        isFallSuspected = false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    /**
     * Cleanup resources
     */
    public static void destroy() {
        if (instance != null) {
            instance.stopMonitoring();
            instance = null;
        }
    }
}