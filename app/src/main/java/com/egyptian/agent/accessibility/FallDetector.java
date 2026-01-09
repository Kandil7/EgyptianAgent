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
import com.egyptian.agent.utils.CrashLogger;

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
            CrashLogger.logWarning(context, "Accelerometer not available on this device");
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
        // Check if emergency is still active
        if (!isEmergencyActive()) {
            return; // Emergency was already cancelled
        }

        TTSManager.speak(context, "يا كبير، إيه الأخبار؟ قول 'أنا كويس' لو جات سليمه من السقوط");

        SpeechConfirmation.waitForConfirmation(context, 30000, userIsOk -> {
            if (userIsOk) {
                TTSManager.speak(context, "الحمد لله. هسيب الإتصالات دي شغالة لحد ما المساعدة تيجي");
                // Cancel the emergency state
                cancelEmergency();
            } else {
                TTSManager.speak(context, "خلاص، بعت إشارة تاني للنجدة. ركز معايا، قوللي فين  المكان بتاعك بالظبط");
                // Gather more location information here
                requestLocationDetails();
            }
        });
    }

    /**
     * Checks if an emergency is currently active
     */
    private boolean isEmergencyActive() {
        // Check the current emergency state by checking if the emergency service is running
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if ("com.egyptian.agent.executors.EmergencyHandler".equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Cancels the current emergency state
     */
    private void cancelEmergency() {
        // Update the emergency state
        Log.i(TAG, "Emergency cancelled by user confirmation");

        // Cancel any pending emergency actions
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null); // Remove all pending callbacks
        }
    }

    /**
     * Requests more location details from the user
     */
    private void requestLocationDetails() {
        TTSManager.speak(context, "قوللي فينك؟ فين الشارع؟ اسم العمارة؟ ممكن تقولي الكود البريدي؟");

        // Wait for user to provide location details
        SpeechConfirmation.waitForCommand(context, 45000, locationDetails -> {
            Log.i(TAG, "User provided location details: " + locationDetails);
            TTSManager.speak(context, "تم تسجيل التفاصيل. بعتها لجهات الطوارئ.");

            // Send the details to emergency services
            sendLocationDetailsToEmergencyServices(locationDetails);
        });
    }

    /**
     * Sends location details to emergency services
     */
    private void sendLocationDetailsToEmergencyServices(String locationDetails) {
        // For now, we'll log it and send to configured emergency contacts
        Log.i(TAG, "Sending location details to emergency services: " + locationDetails);

        // Get current location if available
        String currentLocation = getCurrentLocation();
        String fullDetails = locationDetails + (currentLocation != null ? " - Current location: " + currentLocation : "");

        // Send to emergency contacts
        com.egyptian.agent.executors.EmergencyHandler.sendLocationDetailsToEmergencyContacts(context, fullDetails);
    }

    /**
     * Gets current location if available
     */
    private String getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted");
            return null;
        }

        try {
            android.location.LocationManager locationManager =
                (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            // Get last known location
            android.location.Location location = locationManager.getLastKnownLocation(
                android.location.LocationManager.GPS_PROVIDER);

            if (location != null) {
                return String.format("%.4f, %.4f", location.getLatitude(), location.getLongitude());
            } else {
                // Try network provider as fallback
                location = locationManager.getLastKnownLocation(
                    android.location.LocationManager.NETWORK_PROVIDER);

                if (location != null) {
                    return String.format("%.4f, %.4f", location.getLatitude(), location.getLongitude());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting location", e);
            com.egyptian.agent.utils.CrashLogger.logError(context, e);
        }

        return null;
    }
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