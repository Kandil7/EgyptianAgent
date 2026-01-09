package com.egyptian.agent.executors;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.egyptian.agent.core.TTSManager;

import java.util.List;

public class EmergencyFollowupWorker extends Worker {
    private static final String TAG = "EmergencyFollowupWorker";

    public EmergencyFollowupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        List<String> contacts = getInputData().getStringArrayList("contacts");
        if (contacts == null || contacts.isEmpty()) {
            Log.w(TAG, "No contacts provided for emergency follow-up");
            return Result.failure();
        }

        // Get application context
        Context context = getApplicationContext();

        // Check if emergency is still active
        if (!EmergencyHandler.isEmergencyActive()) {
            Log.i(TAG, "Emergency no longer active, stopping follow-up");
            return Result.success();
        }

        // Speak notification
        TTSManager.speak(context, "بتحاول نتصل بأرقام الطوارئ تاني");

        // Execute follow-up calls
        EmergencyHandler.executeEmergencyCalls(context, contacts);

        Log.i(TAG, "Completed emergency follow-up cycle for " + contacts.size() + " contacts");
        return Result.success();
    }
}