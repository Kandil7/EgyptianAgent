package com.egyptian.agent.core;

import android.content.Context;

/**
 * Interface for TTS Engine as defined in the SRD
 */
public interface TTSEngineInterface {
    void initialize(Context context);
    void speak(String text, TTSEngine.SpeechParams params, SpeechCallback callback);
    void stopSpeaking();
    boolean isSpeaking();
    void setLanguage(String languageCode);
    void setVoiceType(TTSEngine.VoiceType voiceType);

    class SpeechParams {
        private float rate = 1.0f;
        private float pitch = 1.0f;
        private float volume = 0.9f;
        private boolean priority = false; // for emergencies

        public float getRate() {
            return rate;
        }

        public void setRate(float rate) {
            this.rate = rate;
        }

        public float getPitch() {
            return pitch;
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }

        public float getVolume() {
            return volume;
        }

        public void setVolume(float volume) {
            this.volume = volume;
        }

        public boolean isPriority() {
            return priority;
        }

        public void setPriority(boolean priority) {
            this.priority = priority;
        }
    }

    interface SpeechCallback {
        void onCompleted();
        void onError(String errorMessage);
    }
}