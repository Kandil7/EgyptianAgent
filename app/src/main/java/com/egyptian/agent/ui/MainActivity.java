package com.egyptian.agent.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.egyptian.agent.R;
import com.egyptian.agent.accessibility.SeniorMode;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.core.VoiceService;
import com.egyptian.agent.utils.SystemAppHelper;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private TextView statusTextView;
    private Button activateSeniorModeButton;
    private Button startListeningButton;
    private Button testEmergencyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        initUIComponents();

        // Initialize TTS
        TTSManager.initialize(this);

        // Request critical permissions
        requestCriticalPermissions();

        // Apply Honor-specific battery optimizations fix
        SystemAppHelper.keepAlive(this);

        // Handle intent actions (like enabling senior mode from external trigger)
        handleIntentActions();

        // Update UI status
        updateStatus("التطبيق شغال. قول 'يا صاحبي' لتفعيل الـ assistant");

        // Start voice service if not already running
        startVoiceService();
    }

    private void initUIComponents() {
        statusTextView = findViewById(R.id.statusTextView);
        activateSeniorModeButton = findViewById(R.id.activateSeniorModeButton);
        startListeningButton = findViewById(R.id.startListeningButton);
        testEmergencyButton = findViewById(R.id.testEmergencyButton);

        // Set up button listeners
        activateSeniorModeButton.setOnClickListener(v -> toggleSeniorMode());
        startListeningButton.setOnClickListener(v -> startListening());
        testEmergencyButton.setOnClickListener(v -> triggerTestEmergency());

        // Hide UI elements in senior mode (as it's voice-first)
        if (SeniorMode.isEnabled()) {
            hideUIElements();
        }
    }

    private void requestCriticalPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Critical permissions for the app to function
        String[] criticalPermissions = {
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.VIBRATE
        };

        for (String permission : criticalPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        // Request permissions if any are missing
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                permissionsNeeded.toArray(new String[0]),
                PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                updateStatus("كل الصلاحيات متاحة");
                startVoiceService();
            } else {
                updateStatus("مفيش كل الصلاحيات. بعض المميزات مش هتشتغل");
                // In a real app, we would guide the user to enable permissions
            }
        }
    }

    private void handleIntentActions() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra("enable_senior_mode", false)) {
                enableSeniorMode();
            }
        }
    }

    private void startVoiceService() {
        Intent serviceIntent = new Intent(this, VoiceService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void toggleSeniorMode() {
        if (SeniorMode.isEnabled()) {
            disableSeniorMode();
        } else {
            enableSeniorMode();
        }
    }

    private void enableSeniorMode() {
        SeniorMode.enable(this);
        updateStatus("وضع كبار السن تم تفعيله");
        activateSeniorModeButton.setText("تعطيل وضع كبار السن");

        // Hide UI in senior mode as it's voice-first
        hideUIElements();

        // Speak confirmation
        TTSManager.speak(this, "وضع كبار السن نشط. قول 'يا كبير' لأي حاجة");
    }

    private void disableSeniorMode() {
        SeniorMode.disable(this);
        updateStatus("وضع كبار السن تم تعطيله");
        activateSeniorModeButton.setText("تفعيل وضع كبار السن");

        // Show UI elements again
        showUIElements();

        TTSManager.speak(this, "وضع كبار السن متوقف");
    }

    private void hideUIElements() {
        activateSeniorModeButton.setVisibility(View.GONE);
        startListeningButton.setVisibility(View.GONE);
        testEmergencyButton.setVisibility(View.GONE);
    }

    private void showUIElements() {
        activateSeniorModeButton.setVisibility(View.VISIBLE);
        startListeningButton.setVisibility(View.VISIBLE);
        testEmergencyButton.setVisibility(View.VISIBLE);
    }

    private void startListening() {
        // Start voice service with listening command
        Intent serviceIntent = new Intent(this, VoiceService.class);
        serviceIntent.setAction("com.egyptian.agent.action.START_LISTENING");
        startService(serviceIntent);

        updateStatus("بيسمع... قول أوامرك");
    }

    private void triggerTestEmergency() {
        updateStatus("جاري اختبار وضع الطوارئ...");

        // Trigger emergency handler
        EmergencyHandler emergencyHandler = new EmergencyHandler(this);
        emergencyHandler.trigger(this, true);

        updateStatus("وضع الطوارئ تم تنفيذه بنجاح");
    }

    private void updateStatus(String status) {
        runOnUiThread(() -> {
            if (statusTextView != null) {
                statusTextView.setText(status);
            }
        });
        Log.i(TAG, status);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update UI based on current mode
        if (SeniorMode.isEnabled()) {
            hideUIElements();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup TTS
        TTSManager.shutdown();
    }
}