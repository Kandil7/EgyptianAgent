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
            TTSManager.speak(context, readMissedCalls(context));
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
    
    private static final String NO_MISSED_CALLS = "مافيش مكالمات فايتة";

    /**
     * Reads recent missed calls and returns a spoken-form summary.
     *
     * <p>This method does not speak; callers decide whether to route the text
     * to TTS. Returns a user-facing Arabic message on permission/query errors
     * rather than throwing.
     *
     * @param context android context
     * @return summary of up to 5 recent missed calls, never null
     */
    public static String readMissedCalls(Context context) {
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
                    return missedCalls.toString();
                }
                return NO_MISSED_CALLS;
            }

            if (cursor != null) {
                cursor.close();
            }
            return NO_MISSED_CALLS;
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for reading call log", e);
            CrashLogger.logError(context, e);
            return "مافيش صلاحية لقراءة سجل المكالمات";
        } catch (Exception e) {
            Log.e(TAG, "Error reading call log", e);
            CrashLogger.logError(context, e);
            return "حصل خطأ في قراءة سجل المكالمات";
        }
    }
}