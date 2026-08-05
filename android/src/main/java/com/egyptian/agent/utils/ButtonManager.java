package com.egyptian.agent.utils;

import android.content.Context;
import android.media.AudioManager;
import android.os.Vibrator;
import android.util.Log;

/**
 * Button manager to handle volume button presses for emergency functionality
 */
public class ButtonManager {
    private static final String TAG = "ButtonManager";
    private Context context;
    private AudioManager audioManager;
    private Vibrator vibrator;

    public interface VolumeButtonListener {
        void onVolumeDownPress(long timestamp);
        void onVolumeUpPress(long timestamp);
        void onTripleClickDetected();
        void onDoubleClickDetected();
    }

    private VolumeButtonListener volumeButtonListener;

    public ButtonManager(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void registerVolumeButtonListener(VolumeButtonListener listener) {
        this.volumeButtonListener = listener;
    }

    public void unregisterVolumeButtonListener() {
        this.volumeButtonListener = null;
    }

    /**
     * This method would be called from the VolumeButtonReceiver when a volume button event occurs
     */
    public void handleVolumeButtonEvent(int keyCode, long timestamp) {
        if (volumeButtonListener == null) return;

        switch (keyCode) {
            case android.view.KeyEvent.KEYCODE_VOLUME_DOWN:
                volumeButtonListener.onVolumeDownPress(timestamp);
                break;
            case android.view.KeyEvent.KEYCODE_VOLUME_UP:
                volumeButtonListener.onVolumeUpPress(timestamp);
                break;
        }
    }
}