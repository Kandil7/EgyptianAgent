package com.egyptian.agent.executors;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import com.egyptian.agent.core.TTSManager;
import com.egyptian.agent.stt.EgyptianNormalizer;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WhatsAppExecutor {

    private static final String TAG = "WhatsAppExecutor";
    private static final String WHATSAPP_PACKAGE_NAME = "com.whatsapp";
    private static final String WHATSAPP_BUSINESS_PACKAGE_NAME = "com.whatsapp.w4b";

    /**
     * Handle WhatsApp command from user
     */
    public static void handleCommand(Context context, String command) {
        Log.i(TAG, "Handling WhatsApp command: " + command);

        // Normalize Egyptian dialect
        String normalizedCommand = EgyptianNormalizer.normalize(command);
        Log.d(TAG, "Normalized command: " + normalizedCommand);

        // Extract contact name and message
        String contactName = EgyptianNormalizer.extractContactName(normalizedCommand);
        String message = extractMessage(normalizedCommand);

        Log.i(TAG, "Extracted contact: '" + contactName + "', message: '" + message + "'");

        if (contactName.isEmpty()) {
            TTSManager.speak(context, "لمين عايز تبعت الرسالة؟ قول الاسم");
            // In a real app, we would wait for the next voice input
            SpeechConfirmation.waitForCommand(context, 15000, nextCommand -> {
                if (!nextCommand.isEmpty()) {
                    String extractedName = EgyptianNormalizer.extractContactName(EgyptianNormalizer.normalize(nextCommand));
                    if (!extractedName.isEmpty()) {
                        String number = getContactNumber(context, extractedName);
                        if (number != null) {
                            handleWhatsAppSend(context, number, extractedName, message.isEmpty() ? " " : message);
                        } else {
                            TTSManager.speak(context, "مش لاقي " + extractedName + " في>Contactات. قول الرقم مباشرة");
                            // Wait for number input
                        }
                    } else {
                        TTSManager.speak(context, "متعرفش الاسم. قول 'يا كبير' وأنا أساعدك");
                    }
                } else {
                    TTSManager.speak(context, "ما سمعتش حاجة. قول 'يا كبير' وأنا أساعدك");
                }
            });
            return;
        }

        // Get contact number (simplified for this example)
        String contactNumber = getContactNumber(context, contactName);

        if (contactNumber == null) {
            TTSManager.speak(context, "مش لاقي " + contactName + " في>Contactات. قول الرقم مباشرة");
            // In a real app, we would wait for number input
            SpeechConfirmation.waitForCommand(context, 20000, numberCommand -> {
                String extractedNumber = extractNumberFromVoice(EgyptianNormalizer.normalize(numberCommand));
                if (!extractedNumber.isEmpty()) {
                    handleWhatsAppSend(context, extractedNumber, contactName, message.isEmpty() ? " " : message);
                } else {
                    TTSManager.speak(context, "متعرفش الرقم. قول 'يا كبير' وانا أساعدك");
                }
            });
            return;
        }

        handleWhatsAppSend(context, contactNumber, contactName, message);
    }

    private static void handleWhatsAppSend(Context context, String number, String contactName, String message) {
        // Check if WhatsApp is installed
        if (!isWhatsAppInstalled(context)) {
            TTSManager.speak(context, "واتساب مش مثبت على الموبايل. ثبته الأول");
            return;
        }

        // Use default message if none provided
        if (message.isEmpty()) {
            TTSManager.speak(context, "قول الرسالة اللي عايز تبعتها لـ " + contactName);
            // In a real app, we would wait for message input
            SpeechConfirmation.waitForCommand(context, 30000, messageCommand -> {
                if (!messageCommand.isEmpty()) {
                    String normalizedMessage = EgyptianNormalizer.normalize(messageCommand);
                    // Remove command words and keep the actual message
                    String actualMessage = normalizedMessage.replaceAll(".*(قول|ارسل|ابعت)\\s+(?:رساله)?\\s*(?:ليه|له|ليها)?", "").trim();

                    if (!actualMessage.isEmpty()) {
                        sendWhatsAppMessage(context, number, actualMessage);
                        TTSManager.speak(context, "الرسالة اتبعتت لـ " + contactName);
                    } else {
                        TTSManager.speak(context, "متعرفش المحتوى. قول 'يا كبير' وانا أساعدك");
                    }
                } else {
                    TTSManager.speak(context, "ما سمعتش المحتوى. قول 'يا كبير' وأنا أساعدك");
                }
            });
            return;
        }

        // Senior mode requires double confirmation
        // if (SeniorMode.isEnabled()) {
        //     VibrationManager.vibrateShort(context);
        //     TTSManager.speak(context, "عايز تبعت الرسالة دي لـ " + contactName + "؟ قول 'نعم' بس، ولا 'لا'");
        //     SpeechConfirmation.waitForConfirmation(context, 10000, confirmed -> {
        //         if (confirmed) {
        //             sendWhatsAppMessage(context, number, message);
        //             TTSManager.speak(context, "الرسالة اتبعتت لـ " + contactName);
        //         } else {
        //             TTSManager.speak(context, "ما بعتش الرسالة");
        //         }
        //     });
        //     return;
        // }

        // Standard confirmation for normal mode
        TTSManager.speak(context, "عايز تبعت الرسالة دي لـ " + contactName + "؟ قول 'نعم' أو 'لا'");
        // SpeechConfirmation.waitForConfirmation(context, confirmed -> {
        //     if (confirmed) {
        //         sendWhatsAppMessage(context, number, message);
        //         TTSManager.speak(context, "الرسالة اتبعتت لـ " + contactName);
        //     } else {
        //         TTSManager.speak(context, "ما بعتش الرسالة");
        //     }
        // });
    }

    /**
     * Extract message content from normalized command
     */
    private static String extractMessage(String command) {
        // Look for message patterns
        String[] patterns = {
            "رساله\\s*(?:يقول|بيقول|قول|انه)?\\s*(.+)",
            "ابعت\\s*(?:ليه|له|ليها)?\\s*(?:رساله)?\\s*(?:يقول|بيقول|قول|انه)?\\s*(.+)",
            "قول\\s*له\\s*(?:ان)?\\s*(.+)",
            "رساله\\s*ل\\s*\\w+\\s*(?:يقول|بيقول|قول|انه)?\\s*(.+)"
        };

        for (String pattern : patterns) {
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = regex.matcher(command);
            if (matcher.find() && matcher.groupCount() >= 1) {
                return matcher.group(1).trim();
            }
        }

        // If no specific message pattern found, try to extract after keywords
        if (command.contains("رساله")) {
            int msgIndex = command.indexOf("رساله");
            if (msgIndex + 6 < command.length()) {
                return command.substring(msgIndex + 6).trim();
            }
        }

        return ""; // Return empty if no message found
    }

    /**
     * Send WhatsApp message to specified number
     */
    public static void sendWhatsAppMessage(Context context, String number, String message) {
        try {
            // Format number for WhatsApp API (should start with country code without +)
            String formattedNumber = number.startsWith("+") ? number.substring(1) : number;

            // URL encode the message
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.name());

            // Try WhatsApp Business first, then regular WhatsApp
            boolean sent = trySendWhatsAppBusiness(context, formattedNumber, encodedMessage);

            if (!sent) {
                sent = trySendWhatsApp(context, formattedNumber, encodedMessage);
            }

            if (!sent) {
                // Fallback to sharing intent
                sendWhatsAppFallback(context, number, message);
            }

            Log.i(TAG, "WhatsApp message sent to: " + number);
        } catch (Exception e) {
            Log.e(TAG, "Error sending WhatsApp message", e);
            // CrashLogger.logError(context, e);
            TTSManager.speak(context, "حصل خطأ في إرسال الرسالة. حاول تاني");
        }
    }

    private static boolean trySendWhatsAppBusiness(Context context, String number, String encodedMessage) {
        if (!isPackageInstalled(context, WHATSAPP_BUSINESS_PACKAGE_NAME)) {
            return false;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage(WHATSAPP_BUSINESS_PACKAGE_NAME);
            intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + number + "&text=" + encodedMessage));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to send via WhatsApp Business", e);
            return false;
        }
    }

    private static boolean trySendWhatsApp(Context context, String number, String encodedMessage) {
        if (!isPackageInstalled(context, WHATSAPP_PACKAGE_NAME)) {
            return false;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setPackage(WHATSAPP_PACKAGE_NAME);
            intent.setData(Uri.parse("https://api.whatsapp.com/send?phone=" + number + "&text=" + encodedMessage));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to send via WhatsApp", e);
            return false;
        }
    }

    private static void sendWhatsAppFallback(Context context, String number, String message) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, message);

            // Create chooser that includes WhatsApp options
            Intent chooser = Intent.createChooser(intent, "ابعت الرسالة عن طريق");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        } catch (Exception e) {
            Log.e(TAG, "Fallback sharing failed", e);
            // CrashLogger.logError(context, e);
        }
    }

    private static boolean isWhatsAppInstalled(Context context) {
        return isPackageInstalled(context, WHATSAPP_PACKAGE_NAME) ||
               isPackageInstalled(context, WHATSAPP_BUSINESS_PACKAGE_NAME);
    }

    private static boolean isPackageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // New method to extract number with Egyptian dialect support
    private String extractNumberFromVoice(String command) {
        // Handle Egyptian ways of saying numbers
        String normalized = EgyptianNormalizer.normalize(command);

        // Extract digits (including Egyptian Arabic numerals)
        String digitsOnly = normalized.replaceAll("[^0-9٠-٩]", "");

        // Convert Arabic-Indic numerals to Western numerals
        StringBuilder westernDigits = new StringBuilder();
        for (char c : digitsOnly.toCharArray()) {
            if (c >= '٠' && c <= '٩') {
                westernDigits.append((char) (c - '٠' + '0'));
            } else {
                westernDigits.append(c);
            }
        }

        String result = westernDigits.toString();
        Log.d(TAG, "Extracted number from voice: " + result);
        return result;
    }

    /**
     * Send emergency WhatsApp message with location and alert
     */
    public static void sendEmergencyWhatsApp(Context context, String number, String emergencyType) {
        try {
            String message = "🚨 طوارئ! 🚨\n" +
                            "مستخدم الوكيل المصري في حالة طوارئ: " + emergencyType + "\n" +
                            "الوقت: " + new java.text.SimpleDateFormat("HH:mm dd/MM/yyyy").format(new java.util.Date()) + "\n" +
                            "هذا إشعار تلقائي من نظام المساعدة.";

            // For emergency messages, don't check if WhatsApp is installed first
            // Try to send anyway and handle failure gracefully
            sendWhatsAppMessage(context, number, message);

            Log.i(TAG, "Emergency WhatsApp sent to: " + number);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send emergency WhatsApp", e);
            // CrashLogger.logError(context, e);
            // Don't speak error in emergency situations
        }
    }

    /**
     * Send message to guardian for log sharing
     */
    public static void sendLogsToGuardian(Context context, String logs) {
        try {
            String guardianNumber = getGuardianNumber(context); // In real app, this would be stored securely
            if (guardianNumber == null || guardianNumber.isEmpty()) {
                Log.w(TAG, "No guardian number set for log sharing");
                return;
            }

            String message = "📋 سجلات الأخطاء من الوكيل المصري\n" +
                            "تم إنشاؤها في: " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()) + "\n\n" +
                            "المستخدم يحتاج مساعدة في إصلاح هذه المشاكل.\n" +
                            "يرجى التواصل مع المستخدم لإصلاحها.";

            sendWhatsAppMessage(context, guardianNumber, message);

            // In a real app, we would attach the actual logs file
            Log.i(TAG, "Logs sent to guardian via WhatsApp");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send logs to guardian", e);
            // CrashLogger.logError(context, e);
        }
    }

    private static String getGuardianNumber(Context context) {
        // In a real app, this would retrieve from secure storage
        // For now, return a placeholder
        return "01000000000";
    }

    // Placeholder method for getting contact number
    public static String getContactNumber(Context context, String contactName) {
        // In a real implementation, this would look up the contact in the phone's contacts
        // For this example, we'll return a placeholder
        return "01000000000"; // Placeholder number
    }
}