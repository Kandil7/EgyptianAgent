package com.egyptian.agent.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.egyptian.agent.R;

public class ForegroundServiceDelegate {
    
    private static final String TAG = "ForegroundServiceDelegate";
    private static final String CHANNEL_ID = "VoiceServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private final Service service;
    private final Context context;
    
    public ForegroundServiceDelegate(Service service) {
        this.service = service;
        this.context = service.getApplicationContext();
    }
    
    public void startForegroundService() {
        createNotificationChannel();
        Notification notification = buildNotification();
        
        service.startForeground(NOTIFICATION_ID, notification);
        Log.i(TAG, "Foreground service started");
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Egyptian Agent Service Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Channel for Egyptian Agent voice service");
            serviceChannel.setSound(null, null); // No sound for this service
            
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
    
    private Notification buildNotification() {
        return new NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("الوكيل المصري")
            .setContentText("المساعد الصوتي شغال في الخلفية")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using a standard Android icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build();
    }
}