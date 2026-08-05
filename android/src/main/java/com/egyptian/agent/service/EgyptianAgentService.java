package com.egyptian.agent.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.egyptian.agent.R;
import com.egyptian.agent.ui.MainActivity;

import java.util.Locale;

/**
 * Egyptian Agent Voice Service
 * Handles voice commands and speech output
 */
public class EgyptianAgentService extends Service {
    private static final String TAG = "EgyptianAgentService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "egyptian_voice_channel";
    
    private boolean isRunning = false;
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Egyptian Agent Voice Service created");
        createNotificationChannel();
        initializeTTS();
    }

    private void initializeTTS() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("ar", "EG"));
                if (result != TextToSpeech.LANG_MISSING_DATA && 
                    result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsReady = true;
                    Log.i(TAG, "TTS initialized for Egyptian Arabic");
                    speak("مساعدك الصوتي جاهز");
                }
            }
        });
    }

    private void speak(String text) {
        if (ttsReady && textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "egyptian_tts");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Voice service started");
        
        if (intent != null && "STOP".equals(intent.getAction())) {
            speak("تم إيقاف المساعد الصوتي");
            stopSelf();
            return START_NOT_STICKY;
        }
        
        if (intent != null && "SPEAK".equals(intent.getAction())) {
            String message = intent.getStringExtra("message");
            if (message != null) {
                speak(message);
            }
        }
        
        startForeground(NOTIFICATION_ID, createNotification("جاري العمل..."));
        isRunning = true;
        
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Egyptian Agent Voice",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("مساعد صوتي للمصريين");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, EgyptianAgentService.class);
        stopIntent.setAction("STOP");
        PendingIntent stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
            .setContentTitle("Egyptian Agent 🎤")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "إيقاف", stopPendingIntent)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        Log.i(TAG, "Voice service stopped");
    }

    public boolean isRunning() {
        return isRunning;
    }
}