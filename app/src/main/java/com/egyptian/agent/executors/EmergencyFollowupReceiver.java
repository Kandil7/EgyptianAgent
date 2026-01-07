package com.egyptian.agent.executors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;

import java.util.ArrayList;

public class EmergencyFollowupReceiver extends BroadcastReceiver {
    private static final String TAG = "EmergencyFollowupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.egyptian.agent.action.EMERGENCY_FOLLOWUP".equals(intent.getAction())) {
            ArrayList<String> contacts = intent.getStringArrayListExtra("contacts");
            if (contacts != null && !contacts.isEmpty() && EmergencyHandler.isEmergencyActive()) {
                TTSManager.speak(context, "بتحاول نتصل بأرقام الطوارئ تاني");
                EmergencyHandler.executeEmergencyCalls(context, contacts);
                Log.i(TAG, "Emergency follow-up received and executed for " + contacts.size() + " contacts");
            }
        }
    }
}