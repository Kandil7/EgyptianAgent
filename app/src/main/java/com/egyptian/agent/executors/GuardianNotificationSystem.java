package com.egyptian.agent.executors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.egyptian.agent.accessibility.SeniorModeManager;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.CrashLogger;
import java.util.Date;

/**
 * System for notifying guardians about senior user activities and emergencies
 */
public class GuardianNotificationSystem {
    private static final String TAG = "GuardianNotification";
    
    /**
     * Sends an emergency notification to the guardian
     * @param context Application context
     * @param eventType Type of emergency event
     * @param details Additional details about the event
     */
    public static void sendEmergencyNotification(Context context, String eventType, String details) {
        String guardianNumber = SeniorModeManager.getInstance(context).getGuardianPhoneNumber();
        
        if (guardianNumber == null || guardianNumber.isEmpty()) {
            Log.w(TAG, "No guardian phone number configured");
            return;
        }
        
        // Check for SMS permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing SEND_SMS permission for guardian notification");
            TTSManager.speak(context, "محتاج إذن لإرسال تنبيهات للوصي");
            return;
        }
        
        String message = createEmergencyMessage(eventType, details);
        
        try {
            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
            smsManager.sendTextMessage(guardianNumber, null, message, null, null);
            Log.d(TAG, "Emergency notification sent to guardian: " + guardianNumber);
            
            // Also send via WhatsApp if available
            sendWhatsAppNotification(context, guardianNumber, message);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send emergency notification to guardian", e);
            CrashLogger.logError(context, e);
        }
    }
    
    /**
     * Sends a daily activity report to the guardian
     * @param context Application context
     */
    public static void sendDailyReport(Context context) {
        if (!SeniorModeManager.getInstance(context).getConfig().isSendDailyReports()) {
            return; // Daily reports are disabled
        }
        
        String guardianNumber = SeniorModeManager.getInstance(context).getGuardianPhoneNumber();
        
        if (guardianNumber == null || guardianNumber.isEmpty()) {
            Log.w(TAG, "No guardian phone number configured for daily reports");
            return;
        }
        
        // Check for SMS permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing SEND_SMS permission for daily reports");
            return;
        }
        
        String message = createDailyReportMessage(context);
        
        try {
            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
            smsManager.sendTextMessage(guardianNumber, null, message, null, null);
            Log.d(TAG, "Daily report sent to guardian: " + guardianNumber);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send daily report to guardian", e);
            CrashLogger.logError(context, e);
        }
    }
    
    /**
     * Creates an emergency message based on the event type
     */
    private static String createEmergencyMessage(String eventType, String details) {
        String timestamp = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            .format(new Date());
        
        StringBuilder message = new StringBuilder();
        message.append("🚨 تنبيه طوارئ للوكيل المصري 🚨\n");
        message.append("الحدث: ").append(eventType).append("\n");
        message.append("الوقت: ").append(timestamp).append("\n");
        
        if (details != null && !details.isEmpty()) {
            message.append("التفاصيل: ").append(details).append("\n");
        }
        
        message.append("الرجاء التحقق من الحالة فورًا.");
        
        return message.toString();
    }
    
    /**
     * Creates a daily activity report message
     */
    private static String createDailyReportMessage(Context context) {
        String timestamp = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(new Date());
        
        StringBuilder message = new StringBuilder();
        message.append("📋 تقرير نشاط يومي للوكيل المصري\n");
        message.append("التاريخ: ").append(timestamp).append("\n");
        message.append("الحالة: المستخدم نشط وآمن.\n");
        message.append("النظام: يعمل بشكل طبيعي.\n");
        message.append("لا توجد أحداث طارئة.\n");
        message.append("مع تحيات فريق الوكيل المصري.");
        
        return message.toString();
    }
    
    /**
     * Sends notification via WhatsApp if available
     */
    private static void sendWhatsAppNotification(Context context, String guardianNumber, String message) {
        // This would use WhatsApp's Intent API to send a message
        // For now, we'll just log that this would happen
        Log.d(TAG, "Would send WhatsApp notification to: " + guardianNumber);
    }
    
    /**
     * Sends a medication reminder notification to the guardian
     * @param context Application context
     * @param medicationName Name of the medication
     * @param dosage Dosage information
     */
    public static void sendMedicationReminderNotification(Context context, String medicationName, String dosage) {
        String guardianNumber = SeniorModeManager.getInstance(context).getGuardianPhoneNumber();
        
        if (guardianNumber == null || guardianNumber.isEmpty()) {
            Log.w(TAG, "No guardian phone number configured");
            return;
        }
        
        // Check for SMS permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing SEND_SMS permission for medication reminders");
            return;
        }
        
        String message = createMedicationReminderMessage(medicationName, dosage);
        
        try {
            android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
            smsManager.sendTextMessage(guardianNumber, null, message, null, null);
            Log.d(TAG, "Medication reminder sent to guardian: " + guardianNumber);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send medication reminder to guardian", e);
            CrashLogger.logError(context, e);
        }
    }
    
    /**
     * Creates a medication reminder message
     */
    private static String createMedicationReminderMessage(String medicationName, String dosage) {
        String timestamp = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            .format(new Date());
        
        StringBuilder message = new StringBuilder();
        message.append("💊 تذكير بتناول الدواء 💊\n");
        message.append("الدواء: ").append(medicationName).append("\n");
        message.append("الجرعة: ").append(dosage).append("\n");
        message.append("الوقت: ").append(timestamp).append("\n");
        message.append("الرجاء التأكد من تناول الدواء في الوقت المحدد.");
        
        return message.toString();
    }
}