package com.egyptian.agent.accessibility;

import java.time.LocalTime;

/**
 * Class representing a medication reminder as specified in the SRD
 */
public class MedicationReminder {
    private String medicationName;
    private LocalTime time;
    private String instructions;
    private boolean recurring = true; // Daily by default
    
    // Constructors
    public MedicationReminder() {}
    
    public MedicationReminder(String medicationName, LocalTime time, String instructions) {
        this.medicationName = medicationName;
        this.time = time;
        this.instructions = instructions;
    }
    
    public MedicationReminder(String medicationName, LocalTime time, String instructions, boolean recurring) {
        this.medicationName = medicationName;
        this.time = time;
        this.instructions = instructions;
        this.recurring = recurring;
    }
    
    // Getters and setters
    public String getMedicationName() {
        return medicationName;
    }
    
    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }
    
    public LocalTime getTime() {
        return time;
    }
    
    public void setTime(LocalTime time) {
        this.time = time;
    }
    
    public String getInstructions() {
        return instructions;
    }
    
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    
    public boolean isRecurring() {
        return recurring;
    }
    
    public void setRecurring(boolean recurring) {
        this.recurring = recurring;
    }
    
    @Override
    public String toString() {
        return "MedicationReminder{" +
                "medicationName='" + medicationName + '\'' +
                ", time=" + time +
                ", instructions='" + instructions + '\'' +
                ", recurring=" + recurring +
                '}';
    }
}