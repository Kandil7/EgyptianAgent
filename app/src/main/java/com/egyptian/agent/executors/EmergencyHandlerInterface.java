package com.egyptian.agent.executors;

import android.content.Context;

/**
 * Interface for Emergency Handler as defined in the SRD
 */
public interface EmergencyHandlerInterface {
    void trigger(Context context, String reason);
    boolean isEmergencyActive();
    void cancelEmergency();
    void enableSeniorMode();
    void disableSeniorMode();
}