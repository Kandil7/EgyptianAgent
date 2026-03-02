package com.egyptian.agent.executor;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;

import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.nlu.IntentResult;
import com.egyptian.agent.nlu.IntentType;

import java.util.List;

/**
 * Communication Controller
 * 
 * Handles communication-related commands:
 * - Phone calls
 * - WhatsApp messages
 * - SMS messages
 * - Call log reading
 */
public class CommunicationController {
    private static final String TAG = "CommunicationController";
    
    private final Context context;
    
    public CommunicationController(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Make phone call.
     */
    public ExecutorResult makeCall(IntentResult intent) {
        String contact = intent.getEntity("contact", "");
        
        if (contact.isEmpty()) {
            TTSManager.speak(context, "مين اللي عايز تتصل بيه؟");
            return ExecutorResult.error("No contact specified");
        }
        
        // Find contact phone number
        String phoneNumber = findContactPhone(contact);
        
        if (phoneNumber == null) {
            TTSManager.speak(context, "مش لاقي رقم " + contact);
            return ExecutorResult.error("Contact not found: " + contact);
        }
        
        try {
            // Make call
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(callIntent);
            
            TTSManager.speak(context, "بتتصل بـ " + contact);
            Log.i(TAG, "Call initiated to: " + contact + " (" + phoneNumber + ")");
            
            return ExecutorResult.success("Calling " + contact, "CALL:" + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "Error making call", e);
            TTSManager.speak(context, "مش قادر اكلم دلوقتي");
            return ExecutorResult.error("Failed to make call: " + e.getMessage());
        }
    }
    
    /**
     * Send WhatsApp message.
     */
    public ExecutorResult sendWhatsApp(IntentResult intent) {
        String contact = intent.getEntity("contact", "");
        String message = intent.getEntity("message", "");
        
        if (contact.isEmpty()) {
            TTSManager.speak(context, "عايز تبعت رسالة لحد معين؟");
            return ExecutorResult.error("No contact specified");
        }
        
        // Find contact phone number
        String phoneNumber = findContactPhone(contact);
        
        if (phoneNumber == null) {
            TTSManager.speak(context, "مش لاقي رقم " + contact);
            return ExecutorResult.error("Contact not found: " + contact);
        }
        
        try {
            // Check if WhatsApp is installed
            if (!isWhatsAppInstalled()) {
                TTSManager.speak(context, "واتساب مش مثبت على الجهاز");
                return ExecutorResult.error("WhatsApp not installed");
            }
            
            // Send WhatsApp message
            Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
            whatsappIntent.setData(Uri.parse("https://wa.me/" + phoneNumber));
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (message.isEmpty()) {
                // Just open chat
                context.startActivity(whatsappIntent);
                TTSManager.speak(context, "فاتح واتساب لـ " + contact);
            } else {
                // Open with message (requires WhatsApp Business API or user to send)
                whatsappIntent.putExtra(Intent.EXTRA_TEXT, message);
                context.startActivity(whatsappIntent);
                TTSManager.speak(context, "فاتح واتساب لـ " + contact + ". اكتب الرسالة وقول ابعت");
            }
            
            Log.i(TAG, "WhatsApp opened for: " + contact);
            return ExecutorResult.success("Opening WhatsApp for " + contact, "WHATSAPP:" + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "Error sending WhatsApp", e);
            TTSManager.speak(context, "مش قادر افتح واتساب");
            return ExecutorResult.error("Failed to open WhatsApp: " + e.getMessage());
        }
    }
    
    /**
     * Send SMS message.
     */
    public ExecutorResult sendSms(IntentResult intent) {
        String contact = intent.getEntity("contact", "");
        String message = intent.getEntity("message", "");
        
        if (contact.isEmpty()) {
            TTSManager.speak(context, "عايز تبعت رسالة لحد معين؟");
            return ExecutorResult.error("No contact specified");
        }
        
        String phoneNumber = findContactPhone(contact);
        
        if (phoneNumber == null) {
            TTSManager.speak(context, "مش لاقي رقم " + contact);
            return ExecutorResult.error("Contact not found: " + contact);
        }
        
        try {
            Intent smsIntent = new Intent(Intent.ACTION_VIEW);
            smsIntent.setData(Uri.parse("sms:" + phoneNumber));
            smsIntent.putExtra("sms_body", message);
            smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            context.startActivity(smsIntent);
            
            TTSManager.speak(context, "فاتح رسائل لـ " + contact);
            Log.i(TAG, "SMS opened for: " + contact);
            
            return ExecutorResult.success("Opening SMS for " + contact, "SMS:" + phoneNumber);
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS", e);
            TTSManager.speak(context, "مش قادر افتح رسائل");
            return ExecutorResult.error("Failed to open SMS: " + e.getMessage());
        }
    }
    
    /**
     * Read missed calls.
     */
    public ExecutorResult readMissedCalls(IntentResult intent) {
        try {
            // Query missed calls from call log
            Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.DATE},
                CallLog.Calls.TYPE + " = " + CallLog.Calls.MISSED_TYPE,
                null,
                CallLog.Calls.DATE + " DESC LIMIT 5"
            );
            
            if (cursor == null || cursor.getCount() == 0) {
                TTSManager.speak(context, "مفيش مكالمات فايته");
                if (cursor != null) cursor.close();
                return ExecutorResult.success("No missed calls");
            }
            
            StringBuilder missedCalls = new StringBuilder("عندك ");
            missedCalls.append(cursor.getCount()).append(" مكالمات فايته. ");
            
            int count = 0;
            while (cursor.moveToNext() && count < 3) {
                String name = cursor.getString(0);
                String number = cursor.getString(1);
                
                if (name != null) {
                    missedCalls.append(name).append(". ");
                } else if (number != null) {
                    missedCalls.append(number).append(". ");
                }
                count++;
            }
            
            cursor.close();
            
            TTSManager.speak(context, missedCalls.toString());
            Log.i(TAG, "Read " + cursor.getCount() + " missed calls");
            
            return ExecutorResult.success("Read " + cursor.getCount() + " missed calls");
        } catch (Exception e) {
            Log.e(TAG, "Error reading missed calls", e);
            TTSManager.speak(context, "مش قادر اقرا المكالمات الفايته");
            return ExecutorResult.error("Failed to read missed calls: " + e.getMessage());
        }
    }
    
    /**
     * Senior assist - simplified communication help.
     */
    public ExecutorResult seniorAssist(IntentResult intent) {
        TTSManager.speak(context, "أنا هنا للمساعدة. عايز تتصل بحد ولا تبعت رسالة؟");
        return ExecutorResult.success("Senior assist activated");
    }
    
    /**
     * Find contact phone number by name.
     */
    private String findContactPhone(String contactName) {
        if (contactName == null || contactName.isEmpty()) {
            return null;
        }
        
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
        String[] selectionArgs = new String[]{"%" + contactName + "%"};
        
        try {
            Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                String phoneNumber = cursor.getString(0);
                cursor.close();
                return phoneNumber;
            }
            
            if (cursor != null) cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error finding contact", e);
        }
        
        return null;
    }
    
    /**
     * Check if WhatsApp is installed.
     */
    private boolean isWhatsAppInstalled() {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Cancel current operation.
     */
    public void cancel() {
        Log.d(TAG, "Cancelling communication operation");
    }
}
