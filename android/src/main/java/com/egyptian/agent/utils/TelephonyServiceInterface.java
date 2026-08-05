package com.egyptian.agent.utils;

import android.content.Context;
import java.util.List;

/**
 * Interface for Telephony Service as defined in the SRD
 */
public interface TelephonyServiceInterface {
    void placeCall(Context context, String phoneNumber) throws SecurityException;
    List<TelephonyService.CallLogEntry> getMissedCalls(Context context, int limit);
    void setCallStateListener(CallStateListener listener);

    interface CallStateListener {
        void onCallStarted(String phoneNumber);
        void onCallAnswered();
        void onCallEnded();
    }
}