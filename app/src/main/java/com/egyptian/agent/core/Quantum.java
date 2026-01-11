package com.egyptian.agent.core;

import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.media.AudioManager;
import android.provider.ContactsContract;
import android.database.Cursor;
import android.util.Log;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import androidx.core.content.FileProvider;
import com.egyptian.agent.BuildConfig;

/**
 * Quantum.java - Core Intent Classification and Execution Engine
 * Implements the intent classification system as described in the update plan
 * Handles Egyptian dialect commands and executes corresponding Android actions
 */
public class Quantum {
    private static final String TAG = "Quantum";
    
    private Context context;
    private ContextMemory contextMemory;
    private MediaRecorder recorder;
    private File recordedFile;
    
    // Constructor
    public Quantum(Context context) {
        this.context = context;
        this.contextMemory = ContextMemory.getInstance(context);
    }
    
    /**
     * Classifies the user's speech command into specific intents
     * @param speech The raw speech command from user
     * @return The classified intent as a string
     */
    public String classifyIntent(String speech) {
        speech = speech.toLowerCase();

        if (containsAny(speech, "اتصل", "كلّم", "كلم", "رن", "رني")) return "CALL_PERSON";
        if (containsAny(speech, "رجّع", "فاتت", "فاتتني", "الفايتة", "فايتة")) return "CALL_MISSED";
        if (containsAny(speech, "ابعت رسالة", "قول ل", "ارسل ل", "ابعت ل")) return "SEND_MSG";
        if (containsAny(speech, "فويس", "سجّل", "سجّل فويس", "ابعت فويس")) return "SEND_VOICE";
        if (containsAny(speech, "اقرا", "آخر رسالة", "شوف الرسائل", "رسائل")) return "READ_MSG";
        if (containsAny(speech, "شغّل", "عايز أغاني", "اغاني", "موسيقى", "موسيقي")) return "PLAY_MUSIC";
        if (containsAny(speech, "قرآن", "سورة", "القرآن", "القُرآن")) return "PLAY_QURAN";
        if (containsAny(speech, "علّي", "صوت", ".Volume", "هدّي", "أقلع", "أقلع الصوت", ".Volume up", ".Volume down")) return "VOLUME_CONTROL";
        if (containsAny(speech, "افتح", "روح على", "شغّل التطبيق", "التطبيق")) return "OPEN_APP";

        return "UNKNOWN";
    }
    
    /**
     * Helper method to check if speech contains any of the given keywords
     */
    private boolean containsAny(String speech, String... keywords) {
        for (String keyword : keywords) {
            if (speech.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Extracts the contact name from the speech command
     */
    public String extractName(String speech) {
        // Look for patterns like "اتصل بماما", "كلم بابا", etc.
        if (speech.contains("اتصل ب")) {
            return speech.split("اتصل ب")[1].trim();
        } else if (speech.contains("كلم ")) {
            return speech.split("كلم ")[1].trim();
        } else if (speech.contains("ابعت ل")) {
            return speech.split("ابعت ل")[1].trim();
        } else if (speech.contains("قول ل")) {
            return speech.split("قول ل")[1].trim();
        } else if (speech.contains("رن على")) {
            return speech.split("رن على")[1].trim();
        }
        
        return "";
    }
    
    /**
     * Finds the contact number based on the name
     */
    public String findContactNumber(String name) {
        String[] projection = new String[] {
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
        String[] selectionArgs = new String[] { "%" + name + "%" };
        
        Cursor cursor = context.getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            
            if (numberIndex != -1) {
                String foundNumber = cursor.getString(numberIndex);
                cursor.close();
                return foundNumber;
            }
            cursor.close();
        }
        
        return null;
    }
    
    /**
     * Executes the CALL_PERSON intent
     */
    public void executeCallPerson(String speech) {
        String name = extractName(speech);
        if (name.isEmpty()) {
            name = contextMemory.getLastContact(); // Use last mentioned contact if none specified
        }

        String number = findContactNumber(name);

        if (number == null) {
            TTSManager.speak(context, "مش لاقي رقم " + name + "، عايز أدور في الشغل ولا البيت؟");
            return;
        }

        // Store last contact for context memory
        contextMemory.setLastContact(name);
        contextMemory.updateContext("call_person", "contact", name);

        // Make the call
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + number));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(callIntent);

        TTSManager.speak(context, "باتصل بـ " + name + " دلوقتي");
    }
    
    /**
     * Executes the SEND_VOICE intent
     */
    public void executeSendVoice(String speech) {
        String target = extractName(speech);
        
        try {
            // Initialize recorder
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            
            // Create temporary file for recording
            recordedFile = new File(context.getExternalFilesDir(null), "voice_note_" + System.currentTimeMillis() + ".3gp");
            recorder.setOutputFile(recordedFile.getAbsolutePath());
            
            recorder.prepare();
            recorder.start();
            
            TTSManager.speak(context, "سجّل فويسك لـ " + target + "، هيروح بعد 30 ثانية");
            
            // Schedule sending after 30 seconds
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                    recorder = null;
                    
                    sendWhatsAppVoice(target, recordedFile);
                }
            }, 30000); // 30 seconds
            
        } catch (Exception e) {
            Log.e(TAG, "Error recording voice note", e);
            TTSManager.speak(context, "حصل مشكلة في تسجيل الفويس");
        }
    }
    
    /**
     * Sends voice note via WhatsApp
     */
    private void sendWhatsAppVoice(String contact, File voiceFile) {
        try {
            Uri voiceUri = FileProvider.getUriForFile(
                context, 
                BuildConfig.APPLICATION_ID + ".fileprovider", 
                voiceFile
            );
            
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setType("audio/*");
            sendIntent.putExtra(Intent.EXTRA_STREAM, voiceUri);
            sendIntent.setPackage("com.whatsapp");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Find WhatsApp contact
            String whatsappNumber = findWhatsAppNumber(contact);
            if (whatsappNumber != null) {
                // Add recipient info if possible
                sendIntent.putExtra("jid", whatsappNumber + "@s.whatsapp.net");
            }
            
            context.startActivity(sendIntent);
            TTSManager.speak(context, "الفويس اتباع لـ " + contact);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending voice note", e);
            TTSManager.speak(context, "حصل مشكلة في إرسال الفويس");
        }
    }
    
    /**
     * Finds WhatsApp number for a contact
     */
    private String findWhatsAppNumber(String contactName) {
        // This is a simplified implementation
        // In a real app, you'd need to query WhatsApp's contact database
        String number = findContactNumber(contactName);
        if (number != null) {
            // Format number for WhatsApp (remove leading zeros, add country code)
            number = number.replaceAll("[^0-9]", "");
            if (number.length() >= 10 && !number.startsWith("20")) {
                if (number.startsWith("0")) {
                    number = "20" + number.substring(1);
                } else {
                    number = "20" + number;
                }
            }
        }
        return number;
    }
    
    /**
     * Executes volume control
     */
    public void executeVolumeControl(String command) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (isInCall()) {
            // Adjust call volume
            if (command.contains("علّي") || command.contains("up")) {
                audio.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_RAISE, 0);
            } else if (command.contains("هدّي") || command.contains("down")) {
                audio.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_LOWER, 0);
            }
        } else {
            // Adjust media volume
            if (command.contains("علّي") || command.contains("up")) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
            } else if (command.contains("هدّي") || command.contains("down")) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
            }
        }
    }

    /**
     * Executes media playback (Quran)
     */
    public void executePlayQuran(String speech) {
        // Delegate to MediaExecutor
        MediaExecutor.handleCommand(context, speech);
    }

    /**
     * Executes media playback (Music)
     */
    public void executePlayMusic(String speech) {
        // Delegate to MediaExecutor
        MediaExecutor.handleCommand(context, speech);
    }
    
    /**
     * Checks if device is currently in a call
     */
    private boolean isInCall() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getCallState() != TelephonyManager.CALL_STATE_IDLE;
    }
    
    /**
     * Processes the user command using context memory and intent classification
     */
    public void processCommand(String speech) {
        // Context memory - if user says "عايز أكلمه" without specifying who,
        // use the last mentioned contact
        if (speech.contains("كلّمه") || speech.contains("أكلمه") || speech.contains("كلمه")) {
            String lastContact = contextMemory.getLastContact();
            if (lastContact != null && !lastContact.isEmpty()) {
                speech = "اتصل ب" + lastContact; // Transform to call command
            }
        }

        String intent = classifyIntent(speech);

        switch(intent) {
            case "CALL_PERSON":
                executeCallPerson(speech);
                break;
            case "SEND_VOICE":
                executeSendVoice(speech);
                break;
            case "VOLUME_CONTROL":
                executeVolumeControl(speech);
                break;
            case "PLAY_MUSIC":
                executePlayMusic(speech);
                break;
            case "PLAY_QURAN":
                executePlayQuran(speech);
                break;
            case "UNKNOWN":
                TTSManager.speak(context, "مش فاهمك، ممكن تقولها تاني؟");
                break;
            default:
                TTSManager.speak(context, "الコマンد دي مش مدعومة دلوقتي");
        }
    }
    
    /**
     * Finds multiple contact matches for smart resolution
     */
    public List<String> findContactMatches(String name) {
        List<String> matches = new ArrayList<>();
        
        String[] projection = new String[] {
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
        String[] selectionArgs = new String[] { "%" + name + "%" };
        
        Cursor cursor = context.getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        );
        
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            
            do {
                if (nameIndex != -1 && numberIndex != -1) {
                    String contactName = cursor.getString(nameIndex);
                    String contactNumber = cursor.getString(numberIndex);
                    matches.add(contactName + ": " + contactNumber);
                }
            } while (cursor.moveToNext());
            
            cursor.close();
        }
        
        return matches;
    }
    
    /**
     * Performs fuzzy matching for contact names
     */
    public double fuzzyMatch(String a, String b) {
        // Simple implementation of Levenshtein distance
        int n = a.length();
        int m = b.length();
        
        if (n == 0) return m;
        if (m == 0) return n;
        
        int[][] d = new int[n + 1][m + 1];
        
        for (int i = 0; i <= n; i++) {
            d[i][0] = i;
        }
        
        for (int j = 0; j <= m; j++) {
            d[0][j] = j;
        }
        
        for (int i = 1; i <= n; i++) {
            char s_i = a.charAt(i - 1);
            
            for (int j = 1; j <= m; j++) {
                char t_j = b.charAt(j - 1);
                
                int cost = (s_i == t_j) ? 0 : 1;
                
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
            }
        }
        
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 1.0;
        
        return (double) d[n][m] / maxLen;
    }
    
    /**
     * Gets the last mentioned contact for context memory
     */
    public String getLastContact() {
        return contextMemory.getLastContact();
    }

    /**
     * Sets the last mentioned contact for context memory
     */
    public void setLastContact(String contact) {
        contextMemory.setLastContact(contact);
    }
}