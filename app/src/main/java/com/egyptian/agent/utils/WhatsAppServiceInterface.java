package com.egyptian.agent.utils;

import android.content.Context;

/**
 * Interface for WhatsApp Service as defined in the SRD
 */
public interface WhatsAppServiceInterface {
    boolean isWhatsAppInstalled(Context context);
    boolean sendWhatsAppMessage(Context context, String phoneNumber, String message);
    boolean sendEmergencyWhatsApp(Context context, String phoneNumber, String emergencyMessage);
}