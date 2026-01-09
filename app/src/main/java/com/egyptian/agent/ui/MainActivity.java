package com.egyptian.agent.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.egyptian.agent.R;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.core.VoiceService;
import com.egyptian.agent.utils.CrashLogger;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.VIBRATE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.WAKE_LOCK,
        Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    };

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private TextView statusTextView;
    private Button startServiceButton;
    private Button seniorModeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set up the UI
        setContentView(R.layout.activity_main);
        
        initializeViews();
        checkAndRequestPermissions();
        initializeComponents();
    }

    private void initializeViews() {
        statusTextView = findViewById(R.id.statusTextView);
        startServiceButton = findViewById(R.id.startServiceButton);
        seniorModeButton = findViewById(R.id.seniorModeButton);

        if (startServiceButton != null) {
            startServiceButton.setOnClickListener(v -> startVoiceService());
        }

        if (seniorModeButton != null) {
            seniorModeButton.setOnClickListener(v -> toggleSeniorMode());
        }
    }

    private void initializeComponents() {
        // Initialize TTS manager
        TTSManager.initialize(this);

        // Initialize crash logger
        CrashLogger.registerGlobalExceptionHandler(this);

        // Update UI based on service status
        updateServiceStatus();
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toArray(new String[0]),
                PERMISSION_REQUEST_CODE
            );
        } else {
            // All permissions granted, proceed with initialization
            onPermissionsGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                onPermissionsGranted();
            } else {
                onPermissionsDenied();
            }
        }
    }

    private void onPermissionsGranted() {
        runOnUiThread(() -> {
            if (statusTextView != null) {
                statusTextView.setText("جميع الأذونات ممنوحة. جاهز للعمل.");
            }
            Toast.makeText(this, "جميع الأذونات ممنوحة", Toast.LENGTH_SHORT).show();
        });
    }

    private void onPermissionsDenied() {
        runOnUiThread(() -> {
            if (statusTextView != null) {
                statusTextView.setText("بعض الأذونات مرفوضة. قد لا يعمل التطبيق بشكل صحيح.");
            }
            Toast.makeText(this, "بعض الأذونات مطلوبة ليعمل التطبيق", Toast.LENGTH_LONG).show();
        });
    }

    private void startVoiceService() {
        try {
            Intent serviceIntent = new Intent(this, VoiceService.class);
            startForegroundService(serviceIntent);
            
            Toast.makeText(this, "تم تشغيل خدمة الصوت", Toast.LENGTH_SHORT).show();
            updateServiceStatus();
        } catch (Exception e) {
            Toast.makeText(this, "خطأ في بدء الخدمة: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void toggleSeniorMode() {
        if (SeniorMode.isEnabled()) {
            SeniorMode.disable(this);
            seniorModeButton.setText("تشغيل وضع كبار السن");
        } else {
            SeniorMode.enable(this);
            seniorModeButton.setText("إيقاف وضع كبار السن");
        }
        
        Toast.makeText(this, 
            SeniorMode.isEnabled() ? "تم تفعيل وضع كبار السن" : "تم إيقاف وضع كبار السن", 
            Toast.LENGTH_SHORT).show();
    }

    private void updateServiceStatus() {
        // Check if the voice service is actually running
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean isServiceRunning = false;
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if ("com.egyptian.agent.core.VoiceService".equals(service.service.getClassName())) {
                    isServiceRunning = true;
                    break;
                }
            }
        }

        if (statusTextView != null) {
            if (isServiceRunning) {
                statusTextView.setText("الوكيل المصري جاهز. قول \"يا كبير\" أو \"يا صاحبي\" لتفعيله.");
                statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                statusTextView.setText("الوكيل المصري غير مفعل. يرجى التأكد من تشغيل الخدمة.");
                statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));

                // Try to start the service
                Intent serviceIntent = new Intent(this, VoiceService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        TTSManager.shutdown();
    }
}