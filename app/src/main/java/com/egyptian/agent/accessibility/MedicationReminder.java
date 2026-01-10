package com.egyptian.agent.accessibility;

import java.util.Date;

/**
 * Represents a medication reminder for the senior mode
 */
public class MedicationReminder {
    private String medicationName;
    private String dosage;
    private long timeInMillis;
    private String frequency; // daily, weekly, etc.
    private boolean isActive;

    public MedicationReminder(String medicationName, String dosage, long timeInMillis) {
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.timeInMillis = timeInMillis;
        this.frequency = "daily"; // default
        this.isActive = true;
    }

    public MedicationReminder(String medicationName, String dosage, long timeInMillis, String frequency) {
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.timeInMillis = timeInMillis;
        this.frequency = frequency;
        this.isActive = true;
    }

    // Getters and setters
    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    public void setTimeInMillis(long timeInMillis) {
        this.timeInMillis = timeInMillis;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return "MedicationReminder{" +
                "medicationName='" + medicationName + '\'' +
                ", dosage='" + dosage + '\'' +
                ", timeInMillis=" + timeInMillis +
                ", frequency='" + frequency + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}