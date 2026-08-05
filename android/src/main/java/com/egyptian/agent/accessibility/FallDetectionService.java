package com.egyptian.agent.accessibility;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.core.VibrationManager;
import com.egyptian.agent.executors.EmergencyHandler;
import com.egyptian.agent.utils.CrashLogger;
import java.util.ArrayList;

public class FallDetectionService extends Service implements SensorEventListener {

    private static final String TAG = "FallDetectionService";
    private static final String CHANNEL_ID = "FallDetectionChannel";
    
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SpeechRecognizer speechRecognizer;
    private Handler mainHandler;
    
    // Fall detection parameters
    private static final int FALL_THRESHOLD = 15; // G-force > 1.5g (approx)
    private boolean isCheckingStatus = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Fall Detection Service created");
        
        mainHandler = new Handler(Looper.getMainLooper());
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Fall Detection Service",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Fall Detection Service started");
        
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.e(TAG, "Accelerometer not available on this device");
        }
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("مراقب السقوط")
                .setContentText("أنا بتابع حركتك عشان أطمن عليك")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        
        startForeground(2, notification);
        
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; 
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isCheckingStatus) return; // Don't detect new falls while checking status

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            
            double acceleration = Math.sqrt(x * x + y * y + z * z);
            
            // Threshold for fall
            if (acceleration > FALL_THRESHOLD) {
                Log.w(TAG, "Potential fall detected: " + acceleration);
                handlePotentialFall(acceleration);
            }
        }
    }

    private void handlePotentialFall(double acceleration) {
        isCheckingStatus = true;
        Log.i(TAG, "Handling potential fall sequence...");

        // 1. Vibrate hard
        VibrationManager.vibrateEmergency(this);

        // 2. Speak alert
        TTSManager.speak(this, "يا كبير، لقيت إنك وقعت؟ لو أنت كويس قول أنا تمام. لو مفيش رد هطلب النجدة.");

        // 3. Listen for response after TTS finishes (approx 5s)
        mainHandler.postDelayed(this::startVoiceCheck, 5000);
    }

    private void startVoiceCheck() {
        mainHandler.post(() -> {
            if (speechRecognizer == null) {
                if (SpeechRecognizer.isRecognitionAvailable(this)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                } else {
                    Log.e(TAG, "SpeechRecognizer not available");
                    // Fallback: Just trigger emergency after timeout if no interaction?
                    // For now, let's assume we proceed to emergency if we can't listen
                    mainHandler.postDelayed(this::triggerEmergencySequence, 5000);
                    return;
                }
            }
            
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) { Log.d(TAG, "Listening for 'I'm OK'..."); }
                @Override
                public void onBeginningOfSpeech() {}
                @Override
                public void onRmsChanged(float rmsdB) {}
                @Override
                public void onBufferReceived(byte[] buffer) {}
                @Override
                public void onEndOfSpeech() {}
                @Override
                public void onError(int error) {
                    Log.e(TAG, "Speech error: " + error);
                    // If error (e.g. no match/timeout), assume no response -> trigger emergency
                    // But give a little grace period or retry? For MVP, trigger.
                    triggerEmergencySequence();
                }
                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0).toLowerCase();
                        if (text.contains("تمام") || text.contains("بخير") || text.contains("كويس") || text.contains("ok")) {
                            Log.i(TAG, "User confirmed they are OK.");
                            TTSManager.speak(FallDetectionService.this, "الحمد لله على سلامتك.");
                            isCheckingStatus = false; // Reset
                        } else {
                            // User said something else (maybe "Help!"), trigger emergency
                            triggerEmergencySequence();
                        }
                    } else {
                        triggerEmergencySequence();
                    }
                }
                @Override
                public void onPartialResults(Bundle partialResults) {}
                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
            
            try {
                speechRecognizer.startListening(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting listener", e);
                triggerEmergencySequence();
            }
        });
    }

    private void triggerEmergencySequence() {
        Log.w(TAG, "Triggering emergency sequence!");
        EmergencyHandler.trigger(this, true); // true = automated trigger
        isCheckingStatus = false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
