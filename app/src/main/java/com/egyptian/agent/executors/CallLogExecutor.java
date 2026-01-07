package com.egyptian.agent.executors;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.CrashLogger;

public class CallLogExecutor {
    
    private static final String TAG = "CallLogExecutor";
    
    public static void handleCommand(Context context, String command) {
        Log.i(TAG, "Handling call log command: " + command);
        
        // Check if command is related to missed calls
        if (isReadMissedCallsCommand(command)) {
            readMissedCalls(context);
        } else {
            TTSManager.speak(context, "الأمر غير مدعوم");
        }
    }
    
    private static boolean isReadMissedCallsCommand(String command) {
        // Check for keywords related to reading missed calls
        String lowerCommand = command.toLowerCase();
        return lowerCommand.contains("فايتة") || 
               lowerCommand.contains("فايتات") ||
               lowerCommand.contains("المكالمات") ||
               lowerCommand.contains("اللي فاتت");
    }
    
    private static void readMissedCalls(Context context) {
        try {
            // Query for missed calls
            String[] projection = {CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME};
            String selection = CallLog.Calls.TYPE + " = " + CallLog.Calls.MISSED_TYPE;
            String sortOrder = CallLog.Calls.DATE + " DESC LIMIT 5"; // Get last 5 missed calls
            
            Cursor cursor = context.getContentResolver().query(
                CallLog.CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            );
            
            if (cursor != null && cursor.getCount() > 0) {
                StringBuilder missedCalls = new StringBuilder();
                int count = 0;
                
                while (cursor.moveToNext() && count < 5) {
                    String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                    String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
                    
                    if (name != null && !name.isEmpty()) {
                        missedCalls.append("مكالمة فايتة من ").append(name);
                    } else {
                        missedCalls.append("مكالمة فايتة من ").append(number);
                    }
                    
                    if (cursor.moveToNext()) {
                        missedCalls.append(". ");
                        cursor.moveToPrevious(); // Move back to continue the loop properly
                    }
                    
                    count++;
                }
                
                cursor.close();
                
                if (count > 0) {
                    TTSManager.speak(context, missedCalls.toString());
                } else {
                    TTSManager.speak(context, "مافيش مكالمات فايتة");
                }
            } else {
                TTSManager.speak(context, "مافيش مكالمات فايتة");
            }
            
            if (cursor != null) {
                cursor.close();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for reading call log", e);
            TTSManager.speak(context, "مافيش صلاحية لقراءة سجل المكالمات");
            CrashLogger.logError(context, e);
        } catch (Exception e) {
            Log.e(TAG, "Error reading call log", e);
            TTSManager.speak(context, "حصل خطأ في قراءة سجل المكالمات");
            CrashLogger.logError(context, e);
        }
    }
}