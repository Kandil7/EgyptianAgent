package com.egyptian.agent.accessibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class for senior mode settings
 */
public class SeniorModeConfig {
    private float speechRate;
    private float pitch;
    private float volume;
    private String guardianPhoneNumber;
    private boolean sendDailyReports;
    private List<MedicationReminder> medicationReminders;

    public SeniorModeConfig() {
        // Default values optimized for seniors
        this.speechRate = 0.75f;  // Slower speech for clarity
        this.pitch = 0.9f;        // Slightly lower pitch for clarity
        this.volume = 1.0f;       // Maximum volume
        this.guardianPhoneNumber = "";
        this.sendDailyReports = true;
        this.medicationReminders = new ArrayList<>();
    }

    // Getters and setters
    public float getSpeechRate() {
        return speechRate;
    }

    public void setSpeechRate(float speechRate) {
        this.speechRate = speechRate;
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

    public String getGuardianPhoneNumber() {
        return guardianPhoneNumber;
    }

    public void setGuardianPhoneNumber(String guardianPhoneNumber) {
        this.guardianPhoneNumber = guardianPhoneNumber;
    }

    public boolean isSendDailyReports() {
        return sendDailyReports;
    }

    public void setSendDailyReports(boolean sendDailyReports) {
        this.sendDailyReports = sendDailyReports;
    }

    public List<MedicationReminder> getMedicationReminders() {
        return medicationReminders;
    }

    public void setMedicationReminders(List<MedicationReminder> medicationReminders) {
        this.medicationReminders = medicationReminders;
    }
}