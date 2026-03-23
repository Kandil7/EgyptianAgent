package com.egyptian.agent.ui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.content.Intent;

/**
 * Main Activity - Standalone version without core dependencies
 * Entry point for the Egyptian Agent application
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Simple UI
        TextView textView = new TextView(this);
        textView.setText("Egyptian Agent\n\nApp is running!\n\nTest Commands:\n- Call someone\n- Send SMS\n- Set alarm\n- Open app");
        textView.setTextSize(16);
        textView.setPadding(50, 50, 50, 50);
        
        Button startServiceBtn = new Button(this);
        startServiceBtn.setText("Start Voice Service");
        startServiceBtn.setPadding(50, 20, 50, 20);
        
        Button testBtn = new Button(this);
        testBtn.setText("Test TTS");
        testBtn.setPadding(50, 20, 50, 20);
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 100, 50, 50);
        layout.addView(textView);
        layout.addView(startServiceBtn);
        layout.addView(testBtn);
        
        setContentView(layout);
        
        startServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Try to start voice service
                try {
                    Intent intent = new Intent("com.egyptian.agent.action.START_VOICE_SERVICE");
                    startService(intent);
                } catch (Exception e) {
                    textView.setText("Service not available\n(" + e.getMessage() + ")");
                }
            }
        });
        
        testBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setText("TTS Test\n\nSay a command in Egyptian Arabic:\n- يا صاحبي (Ya Sahbi)\n- يا كبير (Ya Kabeer)");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}