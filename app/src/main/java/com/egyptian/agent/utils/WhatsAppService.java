package com.egyptian.agent.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

/**
 * WhatsApp service implementation as defined in the SRD
 */
public class WhatsAppService implements WhatsAppServiceInterface {
    private static final String TAG = "WhatsAppService";
    private static final String WHATSAPP_PACKAGE = "com.whatsapp";
    private static final String WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b";

    @Override
    public boolean isWhatsAppInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(WHATSAPP_PACKAGE, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            try {
                pm.getPackageInfo(WHATSAPP_BUSINESS_PACKAGE, PackageManager.GET_ACTIVITIES);
                return true;
            } catch (PackageManager.NameNotFoundException ex) {
                return false;
            }
        }
    }

    @Override
    public boolean sendWhatsAppMessage(Context context, String phoneNumber, String message) {
        if (!isWhatsAppInstalled(context)) {
            Log.e(TAG, "WhatsApp is not installed");
            return false;
        }

        try {
            // Format phone number (remove any leading + and add 91 for India, or adjust as needed)
            String formattedNumber = phoneNumber.replaceAll("[^0-9]", "");
            if (formattedNumber.startsWith("0")) {
                // Remove leading zero if present
                formattedNumber = formattedNumber.substring(1);
            }

            // Create WhatsApp intent
            String url = "https://api.whatsapp.com/send?phone=" + formattedNumber + "&text=" + Uri.encode(message);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setPackage(getWhatsAppPackageName(context));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            Log.d(TAG, "WhatsApp message sent to: " + phoneNumber);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending WhatsApp message", e);
            CrashLogger.logError(context, e);
            return false;
        }
    }

    @Override
    public boolean sendEmergencyWhatsApp(Context context, String phoneNumber, String emergencyMessage) {
        // Add emergency indicators to the message
        String formattedMessage = "🚨 تنبيه طوارئ! 🚨\n\n" + emergencyMessage + 
                                 "\n\nتم إرسال هذا التنبيه من تطبيق الوكيل المصري." +
                                 "\nالوقت: " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()) +
                                 "\nالموقع: سيتم مشاركته قريبًا.";

        return sendWhatsAppMessage(context, phoneNumber, formattedMessage);
    }

    private String getWhatsAppPackageName(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(WHATSAPP_PACKAGE, PackageManager.GET_ACTIVITIES);
            return WHATSAPP_PACKAGE;
        } catch (PackageManager.NameNotFoundException e) {
            try {
                pm.getPackageInfo(WHATSAPP_BUSINESS_PACKAGE, PackageManager.GET_ACTIVITIES);
                return WHATSAPP_BUSINESS_PACKAGE;
            } catch (PackageManager.NameNotFoundException ex) {
                return null;
            }
        }
    }
}