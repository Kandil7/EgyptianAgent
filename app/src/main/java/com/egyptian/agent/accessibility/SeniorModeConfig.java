package com.egyptian.agent.accessibility;

import com.egyptian.agent.core.IntentType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for Senior Mode as specified in the SRD
 */
public class SeniorModeConfig {
    // Audio settings
    private float speechRate = 0.75f;  // 75% of normal speed
    private float pitch = 0.9f;        // Lower pitch for clarity
    private float volume = 1.0f;       // 100% volume
    
    // Allowed intents in senior mode
    private List<IntentType> allowedIntents = Arrays.asList(
        IntentType.CALL_CONTACT,
        IntentType.EMERGENCY,
        IntentType.READ_TIME,
        IntentType.SET_ALARM,
        IntentType.READ_MISSED_CALLS
    );
    
    // Fall detection settings
    private boolean fallDetectionEnabled = true;
    private float fallThreshold = 20.0f; // m/s²
    private int confirmationTime = 5000; // 5 seconds
    
    // Guardian information
    private String guardianPhoneNumber;
    private boolean sendDailyReports = true;
    
    // Medication reminders
    private List<MedicationReminder> medicationReminders = new ArrayList<>();
    
    // Constructors
    public SeniorModeConfig() {}
    
    public SeniorModeConfig(float speechRate, float pitch, float volume) {
        this.speechRate = speechRate;
        this.pitch = pitch;
        this.volume = volume;
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
    
    public List<IntentType> getAllowedIntents() {
        return new ArrayList<>(allowedIntents);
    }
    
    public void setAllowedIntents(List<IntentType> allowedIntents) {
        this.allowedIntents = new ArrayList<>(allowedIntents);
    }
    
    public boolean isFallDetectionEnabled() {
        return fallDetectionEnabled;
    }
    
    public void setFallDetectionEnabled(boolean fallDetectionEnabled) {
        this.fallDetectionEnabled = fallDetectionEnabled;
    }
    
    public float getFallThreshold() {
        return fallThreshold;
    }
    
    public void setFallThreshold(float fallThreshold) {
        this.fallThreshold = fallThreshold;
    }
    
    public int getConfirmationTime() {
        return confirmationTime;
    }
    
    public void setConfirmationTime(int confirmationTime) {
        this.confirmationTime = confirmationTime;
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
        return new ArrayList<>(medicationReminders);
    }
    
    public void setMedicationReminders(List<MedicationReminder> medicationReminders) {
        this.medicationReminders = new ArrayList<>(medicationReminders);
    }
    
    public void addMedicationReminder(MedicationReminder reminder) {
        this.medicationReminders.add(reminder);
    }
}