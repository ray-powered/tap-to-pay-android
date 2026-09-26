package com.yumedev.taptopayandroid.presentation.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object HapticHelper {
    /**
     * Plays a gentle, crisp success vibration (subtle double-pulse haptic, like Apple Pay / Google Wallet).
     */
    fun playSuccessVibration(context: Context, hapticFeedback: HapticFeedback? = null) {
        try {
            hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (vibrator.hasAmplitudeControl()) {
                        // Gentle double-pulse: 0ms wait, 35ms pulse (soft), 50ms pause, 45ms pulse (crisp)
                        val timings = longArrayOf(0, 35, 50, 45)
                        val amplitudes = intArrayOf(0, 140, 0, 200)
                        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                        vibrator.vibrate(effect)
                    } else {
                        val effect = VibrationEffect.createWaveform(longArrayOf(0, 35, 50, 45), -1)
                        vibrator.vibrate(effect)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 35, 50, 45), -1)
                }
            }
        } catch (_: Exception) {
            // Gracefully ignore if vibrator is unavailable or restricted
        }
    }
}
