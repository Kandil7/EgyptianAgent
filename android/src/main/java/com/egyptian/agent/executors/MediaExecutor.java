package com.egyptian.agent.executors;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.utils.CrashLogger;

/**
 * Handles media playback execution with Egyptian dialect support
 */
public class MediaExecutor {
    private static final String TAG = "MediaExecutor";

    /**
     * Handles a media command from the user
     * @param context Application context
     * @param command The raw command from speech recognition
     */
    public static void handleCommand(Context context, String command) {
        Log.i(TAG, "Handling media command: " + command);

        // Determine media type based on command
        if (isQuranCommand(command)) {
            playQuran(context, command);
        } else if (isMusicCommand(command)) {
            playMusic(context, command);
        } else {
            TTSManager.speak(context, "مافيش محتوى مطابق. ممكن تقولها تاني؟");
        }
    }

    /**
     * Checks if the command is for Quran playback
     */
    private static boolean isQuranCommand(String command) {
        return command.contains("قرآن") || 
               command.contains("القرآن") || 
               command.contains("قُرآن") || 
               command.contains("سورة") ||
               command.contains("الصلاة") ||
               command.contains("أذان");
    }

    /**
     * Checks if the command is for music playback
     */
    private static boolean isMusicCommand(String command) {
        return command.contains("موسيقى") || 
               command.contains("موسيقي") || 
               command.contains("أغاني") || 
               command.contains("اغاني") ||
               command.contains("أغنية") ||
               command.contains("اغنية") ||
               command.contains("طرب") ||
               command.contains("طرب مصري");
    }

    /**
     * Plays Quran based on the command
     */
    private static void playQuran(Context context, String command) {
        Log.i(TAG, "Playing Quran: " + command);

        // Determine specific sura if mentioned
        String sura = extractSura(command);
        if (!sura.isEmpty()) {
            TTSManager.speak(context, "بتشغيل سورة " + sura);
        } else {
            TTSManager.speak(context, "بتشغيل القرآن الكريم");
        }

        // Intent to open Quran app
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("quran://" + sura));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Try to open a Quran app if available
        if (isAppInstalled(context, "com.quran.labs.androidquran")) {
            intent.setPackage("com.quran.labs.androidquran");
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening Quran app", e);
                CrashLogger.logError(context, e);
                TTSManager.speak(context, "مافيش تطبيق قرآن مثبت. ممكن تثبته؟");
            }
        } else {
            // Fallback: try to open with any available app
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening Quran content", e);
                TTSManager.speak(context, "مافيش تطبيق يقدر يشغل المحتوى ده.");
            }
        }
    }

    /**
     * Plays music based on the command
     */
    private static void playMusic(Context context, String command) {
        Log.i(TAG, "Playing music: " + command);

        // Extract artist or song if mentioned
        String artist = extractArtist(command);
        String song = extractSong(command);

        if (!artist.isEmpty()) {
            TTSManager.speak(context, "بتشغيل أغاني " + artist);
        } else if (!song.isEmpty()) {
            TTSManager.speak(context, "بتشغيل " + song);
        } else {
            TTSManager.speak(context, "بتشغيل الأغاني");
        }

        // Intent to open music app
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Try to open YouTube Music if available
        if (isAppInstalled(context, "com.google.android.apps.youtube.music")) {
            intent.setPackage("com.google.android.apps.youtube.music");
            intent.setData(Uri.parse("https://music.youtube.com/search?q=" + 
                Uri.encode(artist + " " + song)));
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening YouTube Music", e);
                CrashLogger.logError(context, e);
                TTSManager.speak(context, "مافيش تطبيق موسيقى مثبت. ممكن تثبته؟");
            }
        } else {
            // Fallback: try to open with any available music app
            intent.setData(Uri.parse("https://music.youtube.com/search?q=" + 
                Uri.encode(artist + " " + song)));
            try {
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening music content", e);
                TTSManager.speak(context, "مافيش تطبيق يقدر يشغل المحتوى ده.");
            }
        }
    }

    /**
     * Extracts sura name from command
     */
    private static String extractSura(String command) {
        // Look for patterns like "سورة الفاتحة", "سورة البقرة", etc.
        if (command.contains("سورة ")) {
            String[] parts = command.split("سورة ");
            if (parts.length > 1) {
                return parts[1].split(" ")[0]; // Take first word after "سورة"
            }
        }
        return "";
    }

    /**
     * Extracts artist name from command
     */
    private static String extractArtist(String command) {
        // Look for patterns like "أغاني محمد منير", "موسيقى عمرو دياب", etc.
        if (command.contains("أغاني ")) {
            String[] parts = command.split("أغاني ");
            if (parts.length > 1) {
                return parts[1].split(" ")[0];
            }
        } else if (command.contains("موسيقى ")) {
            String[] parts = command.split("موسيقى ");
            if (parts.length > 1) {
                return parts[1].split(" ")[0];
            }
        } else if (command.contains("موسيقي ")) {
            String[] parts = command.split("موسيقي ");
            if (parts.length > 1) {
                return parts[1].split(" ")[0];
            }
        }
        return "";
    }

    /**
     * Extracts song name from command
     */
    private static String extractSong(String command) {
        // Look for patterns like "أغنية انا مش ندمان", etc.
        if (command.contains("أغنية ")) {
            String[] parts = command.split("أغنية ");
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }
        return "";
    }

    /**
     * Checks if an app is installed
     */
    private static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}