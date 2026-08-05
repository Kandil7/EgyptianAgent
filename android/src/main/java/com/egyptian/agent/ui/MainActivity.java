package com.egyptian.agent.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.egyptian.agent.R;
import com.egyptian.agent.service.EgyptianAgentService;

import java.util.Locale;

/**
 * Main Activity - Egyptian Agent with reliable voice commands
 */
public class MainActivity extends Activity {
    private TextView statusText;
    private TextView lastCommandText;
    private Button micButton;
    private Button startServiceBtn;
    private Button stopServiceBtn;
    private Button ttsTestBtn;
    private Button callBtn;
    private Button smsBtn;
    private Button alarmBtn;
    private Button settingsBtn;
    private Button textCommandBtn;
    
    private boolean serviceRunning = false;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("ar", "EG"));
                speak("مرحبا! المساعد المصري جاهز");
            }
        });
        
        buildUI();
    }
    
    private void buildUI() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(20, 40, 20, 40);
        
        // Title
        TextView title = new TextView(this);
        title.setText("🏺 Egyptian Agent");
        title.setTextSize(30);
        title.setGravity(android.view.Gravity.CENTER);
        mainLayout.addView(title);
        
        // Status
        statusText = new TextView(this);
        statusText.setText("Ready");
        statusText.setTextSize(16);
        statusText.setGravity(android.view.Gravity.CENTER);
        statusText.setPadding(0, 20, 0, 20);
        mainLayout.addView(statusText);
        
        // Voice input button
        micButton = new Button(this);
        micButton.setText("🎤 VOICE INPUT");
        micButton.setTextSize(24);
        micButton.setBackgroundColor(0xFF1565C0);
        micButton.setTextColor(0xFFFFFFFF);
        micButton.setPadding(40, 50, 40, 50);
        micButton.setOnClickListener(v -> showVoiceInputDialog());
        mainLayout.addView(micButton);
        
        // Text command button
        textCommandBtn = new Button(this);
        textCommandBtn.setText("⌨️ Type Command");
        textCommandBtn.setOnClickListener(v -> showTextInputDialog());
        mainLayout.addView(textCommandBtn);
        
        // Last command
        lastCommandText = new TextView(this);
        lastCommandText.setText("Last: none");
        lastCommandText.setTextSize(14);
        lastCommandText.setPadding(0, 15, 0, 15);
        mainLayout.addView(lastCommandText);
        
        // Service buttons
        LinearLayout serviceRow = new LinearLayout(this);
        startServiceBtn = new Button(this);
        startServiceBtn.setText("Start Svc");
        startServiceBtn.setOnClickListener(v -> startVoiceService());
        
        stopServiceBtn = new Button(this);
        stopServiceBtn.setText("Stop Svc");
        stopServiceBtn.setEnabled(false);
        stopServiceBtn.setOnClickListener(v -> stopVoiceService());
        
        serviceRow.addView(startServiceBtn);
        serviceRow.addView(stopServiceBtn);
        mainLayout.addView(serviceRow);
        
        // TTS test
        ttsTestBtn = new Button(this);
        ttsTestBtn.setText("🔊 Test Voice");
        ttsTestBtn.setOnClickListener(v -> testVoice());
        mainLayout.addView(ttsTestBtn);
        
        // Quick actions
        TextView divider = new TextView(this);
        divider.setText("\n── Quick Actions ──\n");
        divider.setGravity(android.view.Gravity.CENTER);
        mainLayout.addView(divider);
        
        LinearLayout btnRow = new LinearLayout(this);
        
        callBtn = new Button(this);
        callBtn.setText("📞 Call");
        callBtn.setOnClickListener(v -> makeCall());
        
        smsBtn = new Button(this);
        smsBtn.setText("💬 SMS");
        smsBtn.setOnClickListener(v -> sendSms());
        
        alarmBtn = new Button(this);
        alarmBtn.setText("⏰ Alarm");
        alarmBtn.setOnClickListener(v -> setAlarm());
        
        btnRow.addView(callBtn);
        btnRow.addView(smsBtn);
        btnRow.addView(alarmBtn);
        mainLayout.addView(btnRow);
        
        settingsBtn = new Button(this);
        settingsBtn.setText("⚙️ Settings");
        settingsBtn.setOnClickListener(v -> openSettings());
        mainLayout.addView(settingsBtn);
        
        // Help
        TextView help = new TextView(this);
        help.setText("\nCommands:\n• اتصل / call → Call\n• رسالة / sms → SMS\n• منبه / alarm → Alarm");
        help.setTextSize(12);
        mainLayout.addView(help);
        
        setContentView(mainLayout);
    }
    
    private void showVoiceInputDialog() {
        // Use standard Android speech recognition
        try {
            Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ar-EG");
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            startActivityForResult(intent, 100);
            statusText.setText("🎙️ Speak...");
        } catch (Exception e) {
            // Fallback to text input
            showTextInputDialog();
        }
    }
    
    private void showTextInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Type Command");
        
        final EditText input = new EditText(this);
        input.setHint("Type command in Arabic or English");
        builder.setView(input);
        
        builder.setPositiveButton("OK", (dialog, which) -> {
            String cmd = input.getText().toString();
            if (!cmd.isEmpty()) {
                lastCommandText.setText("Text: " + cmd);
                processCommand(cmd);
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            java.util.ArrayList<String> results = data.getStringArrayListExtra("results");
            
            if (results != null && !results.isEmpty()) {
                String command = results.get(0);
                lastCommandText.setText("Voice: " + command);
                processCommand(command);
            }
        }
    }
    
    private void processCommand(String cmd) {
        String c = cmd.toLowerCase(Locale.ROOT);
        
        if (c.contains("اتصل") || c.contains("call")) {
            makeCall();
            speak("جاري الاتصال");
            statusText.setText("📞 Calling...");
        } else if (c.contains("رسالة") || c.contains("sms")) {
            sendSms();
            speak("جاري فتح الرسائل");
            statusText.setText("💬 SMS...");
        } else if (c.contains("منبه") || c.contains("alarm")) {
            setAlarm();
            speak("جاري ضبط المنبه");
            statusText.setText("⏰ Alarm...");
        } else if (c.contains("اعدادات") || c.contains("settings")) {
            openSettings();
            speak("جاري الاعدادات");
            statusText.setText("⚙️ Settings...");
        } else if (c.contains("مرحبا") || c.contains("hello") || c.contains("اهلا")) {
            speak("اهلا بيك!");
            statusText.setText("👋 Hello!");
        } else {
            speak("لم افهم");
            statusText.setText("❓: " + cmd);
        }
    }
    
    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    
    private void testVoice() {
        speak("مرحبا! اختبار الصوت");
        statusText.setText("🔊 Testing...");
    }
    
    private void startVoiceService() {
        try {
            startService(new Intent(this, EgyptianAgentService.class));
            serviceRunning = true;
            startServiceBtn.setEnabled(false);
            stopServiceBtn.setEnabled(true);
            speak("تم التشغيل");
            statusText.setText("✅ Service started");
        } catch (Exception e) {
            statusText.setText("Error: " + e.getMessage());
        }
    }
    
    private void stopVoiceService() {
        try {
            Intent i = new Intent(this, EgyptianAgentService.class);
            i.setAction("STOP");
            startService(i);
            serviceRunning = false;
            startServiceBtn.setEnabled(true);
            stopServiceBtn.setEnabled(false);
            speak("تم الايقاف");
            statusText.setText("Stopped");
        } catch (Exception e) {
            statusText.setText("Error: " + e.getMessage());
        }
    }
    
    private void makeCall() {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+201234567890")));
        statusText.setText("📞 Phone");
    }
    
    private void sendSms() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("sms:+201234567890")));
        statusText.setText("💬 SMS");
    }
    
    private void setAlarm() {
        Intent i = new Intent(android.provider.AlarmClock.ACTION_SET_ALARM);
        i.putExtra(android.provider.AlarmClock.EXTRA_HOUR, 8);
        i.putExtra(android.provider.AlarmClock.EXTRA_MINUTES, 0);
        startActivity(i);
        statusText.setText("⏰ Alarm");
    }
    
    private void openSettings() {
        Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.setData(Uri.parse("package:" + getPackageName()));
        startActivity(i);
        statusText.setText("⚙️ Settings");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        statusText.setText("Ready");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.shutdown();
        }
    }
}