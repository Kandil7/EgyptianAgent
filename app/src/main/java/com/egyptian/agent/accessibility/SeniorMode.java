package com.egyptian.agent.accessibility;

import android.content.Context;
import android.util.Log;
import com.egyptian.agent.core.IntentType;
import com.egyptian.agent.executors.EmergencyHandler;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.PreferencesHelper;
import java.util.*;

public class SeniorMode {

    private static final String TAG = "SeniorMode";
    private static Boolean isEnabled = null;

    // Simplified command set for seniors
    private static final Set<String> ALLOWED_COMMANDS = new HashSet<>(Arrays.asList(
        "اتصل", "كلم", "رن", "يا نجدة", "استغاثة", "الوقت كام",
        "الساعة كام", "نبهني", "انبهني", "واتساب", "رسالة"
    ));

    private static final Set<IntentType> ALLOWED_INTENTS = new HashSet<>(Arrays.asList(
        IntentType.CALL_CONTACT,
        IntentType.EMERGENCY,
        IntentType.READ_TIME,
        IntentType.SET_ALARM,
        IntentType.SEND_WHATSAPP,
        IntentType.READ_MISSED_CALLS
    ));

    private static final float SENIOR_TTS_RATE = 0.75f; // Slower speech
    private static final float SENIOR_TTS_VOLUME = 1.0f; // Maximum volume

    public static void enable(Context context) {
        if (isEnabled(context)) return;

        Log.i(TAG, "Enabling Senior Mode");
        isEnabled = true;
        PreferencesHelper.getInstance(context).setSeniorModeEnabled(true);

        // Apply senior-specific settings
        applySeniorTtsSettings(context);
        startFallDetection(context);
        EmergencyHandler.enableSeniorMode();

        // Special greeting for seniors
        TTSManager.speak(context, "تم تفعيل وضع كبار السن. قول 'يا كبير' لأي حاجة.");

        // Vibration confirmation (placeholder)
        // VibrationManager.vibratePattern(context, new long[]{0, 100, 200, 100});
    }

    public static void disable(Context context) {
        if (!isEnabled(context)) return;

        Log.i(TAG, "Disabling Senior Mode");
        isEnabled = false;
        PreferencesHelper.getInstance(context).setSeniorModeEnabled(false);

        // Restore normal settings
        restoreNormalTtsSettings(context);
        stopFallDetection(context);
        EmergencyHandler.disableSeniorMode();

        TTSManager.speak(context, "تم إيقاف وضع كبار السن");
        // VibrationManager.vibrateShort(context);
    }

    public static boolean isEnabled(Context context) {
        if (isEnabled == null) {
            // Initialize from preferences
            isEnabled = PreferencesHelper.getInstance(context).isSeniorModeEnabled();
        }
        return isEnabled;
    }

    // Overload for backward compatibility
    public static boolean isEnabled() {
        return isEnabled != null ? isEnabled : false;
    }

    // Method to initialize from context
    public static void initialize(Context context) {
        isEnabled = PreferencesHelper.getInstance(context).isSeniorModeEnabled();
    }

    public static boolean isCommandAllowed(Context context, String command) {
        if (!isEnabled(context)) return true;

        // Always allow emergency commands
        if (EmergencyHandler.isEmergency(command)) {
            return true;
        }

        // Check against allowed commands
        for (String allowedCommand : ALLOWED_COMMANDS) {
            if (command.contains(allowedCommand)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isIntentAllowed(Context context, IntentType intent) {
        if (!isEnabled(context)) return true;
        return ALLOWED_INTENTS.contains(intent);
    }

    public static void applySeniorTtsSettings(Context context) {
        TTSManager.setSpeechRate(context, SENIOR_TTS_RATE);
        TTSManager.setVolume(context, SENIOR_TTS_VOLUME);
        TTSManager.setPitch(context, 0.9f); // Slightly lower pitch for clarity
    }

    public static void restoreNormalTtsSettings(Context context) {
        TTSManager.setSpeechRate(context, 1.0f);
        TTSManager.setVolume(context, 0.9f);
        TTSManager.setPitch(context, 1.0f);
    }

    private static void startFallDetection(Context context) {
        FallDetector.start(context);
    }

    private static void stopFallDetection(Context context) {
        FallDetector.stop(context);
    }

    public static void handleRestrictedCommand(Context context, String command) {
        Log.w(TAG, "Blocked restricted command in senior mode: " + command);

        // Vibrate to alert user (placeholder)
        // VibrationManager.vibrateShort(context);

        // Explain limitations clearly
        TTSManager.speak(context, "في وضع كبار السن، أنا بس أعرف أوامر بسيطة. قول 'يا كبير' وأنا أعلمك إياهم.");

        // Offer to disable senior mode
        TTSManager.speak(context, "عايز تخرج من وضع كبار السن؟ قول 'نعم'");
        SpeechConfirmation.waitForConfirmation(context, 15000, confirmed -> {
            if (confirmed) {
                disable(context);
            } else {
                // In a real app, we would explain how to exit senior mode later
                TTSManager.speak(context, "مفيش مشكلة. قول 'يا كبير، خرج من وضع كبار السن' في أي وقت");
            }
        });
    }

    public static void handleEmergency(Context context) {
        EmergencyHandler.trigger(context, true);

        // Vibrate continuously until acknowledged (placeholder)
        // VibrationManager.vibrateEmergency(context);

        // Clear, simple instructions
        TTSManager.speak(context, "يا كبير! لقيت إنك وقعت. بيتصل بالإسعاف دلوقتي! إتقعد مكانك ومتتحركش.");
    }
}