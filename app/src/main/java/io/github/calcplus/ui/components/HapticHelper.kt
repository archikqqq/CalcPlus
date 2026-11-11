package io.github.calcplus.ui.components

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import io.github.calcplus.model.HapticStrength

enum class CalcHapticType {
    DIGIT,
    OPERATOR,
    EQUALS,
    ERROR
}

object HapticHelper {

    fun perform(
        context: Context,
        composeHaptic: HapticFeedback,
        type: CalcHapticType,
        strength: HapticStrength
    ) {
        if (strength == HapticStrength.OFF) return

        val vibratorManager = context.getSystemService(VibratorManager::class.java)
        val vibrator = vibratorManager?.defaultVibrator

        if (vibrator == null || !vibrator.hasVibrator()) {
            val fallbackType = when (type) {
                CalcHapticType.DIGIT -> HapticFeedbackType.TextHandleMove
                CalcHapticType.OPERATOR -> HapticFeedbackType.LongPress
                CalcHapticType.EQUALS, CalcHapticType.ERROR -> HapticFeedbackType.LongPress
            }
            composeHaptic.performHapticFeedback(fallbackType)
            return
        }

        val effectId = when (type) {
            CalcHapticType.DIGIT -> when (strength) {
                HapticStrength.LIGHT -> VibrationEffect.EFFECT_TICK
                HapticStrength.MEDIUM -> VibrationEffect.EFFECT_TICK
                HapticStrength.STRONG -> VibrationEffect.EFFECT_CLICK
                HapticStrength.OFF -> return
            }
            CalcHapticType.OPERATOR -> when (strength) {
                HapticStrength.LIGHT -> VibrationEffect.EFFECT_TICK
                HapticStrength.MEDIUM -> VibrationEffect.EFFECT_CLICK
                HapticStrength.STRONG -> VibrationEffect.EFFECT_HEAVY_CLICK
                HapticStrength.OFF -> return
            }
            CalcHapticType.EQUALS -> when (strength) {
                HapticStrength.LIGHT -> VibrationEffect.EFFECT_CLICK
                HapticStrength.MEDIUM, HapticStrength.STRONG -> VibrationEffect.EFFECT_HEAVY_CLICK
                HapticStrength.OFF -> return
            }
            CalcHapticType.ERROR -> VibrationEffect.EFFECT_DOUBLE_CLICK
        }

        runCatching {
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        }
    }
}