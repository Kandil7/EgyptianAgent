package com.egyptian.agent.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Telephony service implementation as defined in the SRD
 */
public class TelephonyService implements TelephonyServiceInterface {
    private static final String TAG = "TelephonyService";
    private Context context;

    public TelephonyService() {
        // Default constructor
    }

    @Override
    public void placeCall(Context context, String phoneNumber) throws SecurityException {
        this.context = context;

        // Check for CALL_PHONE permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("CALL_PHONE permission not granted");
        }

        // Create call intent
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Start the call
        context.startActivity(callIntent);
    }

    @Override
    public List<CallLogEntry> getMissedCalls(Context context, int limit) {
        this.context = context;
        List<CallLogEntry> missedCalls = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "READ_CALL_LOG permission not granted");
            return missedCalls;
        }

        // Query call log for missed calls
        String[] projection = {
            CallLog.Calls.NUMBER,
            CallLog.Calls.NAME,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
        };

        String selection = CallLog.Calls.TYPE + " = ?";
        String[] selectionArgs = {String.valueOf(CallLog.Calls.MISSED_TYPE)};
        String sortOrder = CallLog.Calls.DATE + " DESC LIMIT " + limit;

        Cursor cursor = context.getContentResolver().query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        );

        if (cursor != null) {
            while (cursor.moveToNext() && missedCalls.size() < limit) {
                String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NAME));
                long date = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
                int duration = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DURATION));

                // Get contact name if not available in call log
                if (name == null || name.isEmpty()) {
                    name = getContactNameFromNumber(context, number);
                }

                CallLogEntry entry = new CallLogEntry(number, name, date, duration);
                missedCalls.add(entry);
            }
            cursor.close();
        }

        return missedCalls;
    }

    private String getContactNameFromNumber(Context context, String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};

        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        String name = number; // Default to number if no contact name found

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
            cursor.close();
        }

        return name;
    }

    @Override
    public void setCallStateListener(CallStateListener listener) {
        // In a real implementation, this would register a listener for call state changes
        // This typically involves using a PhoneStateListener with TelephonyManager
        Log.d(TAG, "Call state listener set (implementation would register PhoneStateListener)");
    }

    // Inner class for call log entries
    public static class CallLogEntry {
        private String phoneNumber;
        private String contactName;
        private long timestamp;
        private int duration;

        public CallLogEntry(String phoneNumber, String contactName, long timestamp, int duration) {
            this.phoneNumber = phoneNumber;
            this.contactName = contactName;
            this.timestamp = timestamp;
            this.duration = duration;
        }

        // Getters
        public String getPhoneNumber() { return phoneNumber; }
        public String getContactName() { return contactName; }
        public long getTimestamp() { return timestamp; }
        public int getDuration() { return duration; }

        @Override
        public String toString() {
            return "CallLogEntry{" +
                    "phoneNumber='" + phoneNumber + '\'' +
                    ", contactName='" + contactName + '\'' +
                    ", timestamp=" + timestamp +
                    ", duration=" + duration +
                    '}';
        }
    }
}