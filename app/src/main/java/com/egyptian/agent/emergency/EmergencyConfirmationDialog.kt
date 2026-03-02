package com.egyptian.agent.emergency

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import com.egyptian.agent.R
import java.util.Locale

/**
 * Emergency Confirmation Dialog
 * 
 * Safety-critical component that provides a 10-second countdown
 * before initiating emergency calls, allowing users to cancel
 * accidental emergency triggers.
 * 
 * Features:
 * - 10-second countdown with visual feedback
 * - Arabic voice warning: "هيتم الاتصال بالطوارئ خلال 10 ثواني"
 * - Cancel button to abort emergency call
 * - Auto-call after countdown if not cancelled
 */
class EmergencyConfirmationDialog(
    private val context: Context,
    private val onConfirmCallback: () -> Unit,
    private val onCancelCallback: () -> Unit
) {
    companion object {
        private const val TAG = "EmergencyConfirmationDialog"
        private const val COUNTDOWN_SECONDS = 10L
        private const val COUNTDOWN_INTERVAL = 1000L
    }

    private var alertDialog: AlertDialog? = null
    private var countDownTimer: CountDownTimer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isCancelled = false

    /**
     * Shows the emergency confirmation dialog with countdown
     */
    fun show() {
        try {
            // Initialize TextToSpeech for voice warning
            initializeTextToSpeech()

            // Build the dialog
            val builder = AlertDialog.Builder(context)
            builder.setTitle("تنبيه طوارئ")
            builder.setMessage("هيتم الاتصال بالطوارئ خلال 10 ثواني...\n\nاضغط إلغاء لوقف العملية")

            // Set custom view with countdown timer
            val view = LayoutInflater.from(context)
                .inflate(R.layout.dialog_emergency_countdown, null)
            val countdownText = view.findViewById<TextView>(R.id.countdown_text)
            builder.setView(view)

            // Cancel button
            builder.setNegativeButton("إلغاء") { dialog, _ ->
                cancelEmergency()
                dialog.dismiss()
            }

            // Make dialog non-cancelable by back button
            builder.setCancelable(false)

            alertDialog = builder.create()
            alertDialog?.setCanceledOnTouchOutside(false)

            // Show dialog
            alertDialog?.show()

            // Start countdown timer
            startCountdown(countdownText)

            Log.i(TAG, "Emergency confirmation dialog shown")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing emergency confirmation dialog", e)
            // If dialog fails, proceed with emergency call directly
            onConfirmCallback()
        }
    }

    /**
     * Initializes TextToSpeech for voice warnings
     */
    private fun initializeTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = textToSpeech?.setLanguage(Locale("ar", "EG"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || 
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Arabic TTS not supported, using default")
                        textToSpeech?.setLanguage(Locale.getDefault())
                    }
                } else {
                    Log.e(TAG, "TextToSpeech initialization failed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TextToSpeech", e)
        }
    }

    /**
     * Speaks the emergency warning
     */
    private fun speakWarning() {
        try {
            textToSpeech?.speak(
                "هيتم الاتصال بالطوارئ خلال 10 ثواني",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "emergency_warning"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking warning", e)
        }
    }

    /**
     * Starts the countdown timer with visual feedback
     */
    private fun startCountdown(countdownText: TextView) {
        // Speak warning immediately
        speakWarning()

        countDownTimer = object : CountDownTimer(COUNTDOWN_SECONDS * 1000, COUNTDOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                if (isCancelled) return

                val secondsRemaining = millisUntilFinished / 1000
                countdownText.text = "$secondsRemaining"

                // Update dialog message with remaining time
                alertDialog?.setMessage(
                    "هيتم الاتصال بالطوارئ خلال $secondsRemaining ثواني...\n\nاضغط إلغاء لوقف العملية"
                )

                Log.d(TAG, "Countdown tick: $secondsRemaining seconds remaining")
            }

            override fun onFinish() {
                if (isCancelled) return

                Log.i(TAG, "Countdown finished, confirming emergency call")
                
                // Speak final warning
                textToSpeech?.speak(
                    "جاري الاتصال بالطوارئ الآن",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "emergency_calling"
                )

                // Call the confirm callback to initiate emergency call
                onConfirmCallback()
                
                // Dismiss dialog
                alertDialog?.dismiss()
            }
        }

        countDownTimer?.start()
    }

    /**
     * Cancels the emergency call
     */
    private fun cancelEmergency() {
        isCancelled = true
        
        Log.i(TAG, "Emergency call cancelled by user")

        // Stop countdown
        countDownTimer?.cancel()
        countDownTimer = null

        // Speak cancellation message
        try {
            textToSpeech?.speak(
                "تم إلغاء الاتصال بالطوارئ",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "emergency_cancelled"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking cancellation", e)
        }

        // Call the cancel callback
        onCancelCallback()
    }

    /**
     * Dismisses the dialog and cleans up resources
     */
    fun dismiss() {
        isCancelled = true

        // Stop countdown
        countDownTimer?.cancel()
        countDownTimer = null

        // Dismiss dialog
        alertDialog?.dismiss()
        alertDialog = null

        // Shutdown TextToSpeech
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null

        Log.d(TAG, "Emergency confirmation dialog dismissed")
    }

    /**
     * Checks if the dialog is currently showing
     */
    fun isShowing(): Boolean {
        return alertDialog?.isShowing == true
    }
}
