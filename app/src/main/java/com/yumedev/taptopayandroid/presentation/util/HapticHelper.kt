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

    /**
     * Plays a distinct failure/decline vibration pattern (triple buzz reject haptic).
     */
    fun playFailureVibration(context: Context, hapticFeedback: HapticFeedback? = null) {
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
                        // Triple reject pulse: 0ms wait, 60ms buzz, 50ms pause, 60ms buzz, 50ms pause, 80ms buzz
                        val timings = longArrayOf(0, 60, 50, 60, 50, 80)
                        val amplitudes = intArrayOf(0, 200, 0, 200, 0, 255)
                        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                        vibrator.vibrate(effect)
                    } else {
                        val effect = VibrationEffect.createWaveform(longArrayOf(0, 60, 50, 60, 50, 80), -1)
                        vibrator.vibrate(effect)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 60, 50, 60, 50, 80), -1)
                }
            }
        } catch (_: Exception) {
            // Gracefully ignore
        }
    }

    /**
     * Plays Mastercard's signature 6-pulse sensory haptic rhythm synced to the sonic brand melody.
     */
    fun playMastercardSonicHaptic(context: Context, hapticFeedback: HapticFeedback? = null) {
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
                        // 6-pulse rhythm matching D5, A4, D5, E5, A5, D6:
                        val timings = longArrayOf(0, 30, 90, 30, 90, 35, 105, 35, 125, 50, 150, 75)
                        val amplitudes = intArrayOf(0, 110, 0, 90, 0, 120, 0, 140, 0, 180, 0, 240)
                        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                        vibrator.vibrate(effect)
                    } else {
                        val effect = VibrationEffect.createWaveform(
                            longArrayOf(0, 30, 90, 30, 90, 35, 105, 35, 125, 50, 150, 75),
                            -1
                        )
                        vibrator.vibrate(effect)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(
                        longArrayOf(0, 30, 90, 30, 90, 35, 105, 35, 125, 50, 150, 75),
                        -1
                    )
                }
            }
        } catch (_: Exception) {
            // Gracefully ignore
        }
    }
}
