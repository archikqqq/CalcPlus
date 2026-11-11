package io.github.calcplus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.calcplus.data.GeminiClient
import io.github.calcplus.data.PreferencesRepository
import io.github.calcplus.domain.ConverterCategory
import io.github.calcplus.domain.FractionNumber
import io.github.calcplus.domain.MathEvaluator
import io.github.calcplus.domain.SpeedGenerator
import io.github.calcplus.domain.SpeedPreset
import io.github.calcplus.domain.SpeedTask
import io.github.calcplus.domain.SvtDifficulty
import io.github.calcplus.domain.UnitConverter
import io.github.calcplus.domain.UnitItem
import io.github.calcplus.model.CalcMode
import io.github.calcplus.model.HapticStrength
import io.github.calcplus.model.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalcUiState(
    val currentMode: CalcMode = CalcMode.BASIC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val hapticStrength: HapticStrength = HapticStrength.MEDIUM,
    val isSettingsOpen: Boolean = false,

    // Обычный и Инженерный калькулятор
    val expression: String = "",
    val previousExpression: String = "",
    val livePreview: String = "",
    val isError: Boolean = false,
    val isDegrees: Boolean = true,
    val memoryValue: Double = 0.0,
    val ansValue: String = "0",
    val resultAnimationTick: Int = 0,

    // Дроби
    val frac1Whole: String = "",
    val frac1Num: String = "1",
    val frac1Den: String = "2",
    val frac2Whole: String = "",
    val frac2Num: String = "1",
    val frac2Den: String = "3",
    val fracOp: String = "+",
    val decimalToFractionInput: String = "0.75",

    // Конвертер
    val converterCategory: ConverterCategory = ConverterCategory.LENGTH,
    val converterFromUnit: UnitItem = UnitConverter.getUnits(ConverterCategory.LENGTH)[0],
    val converterToUnit: UnitItem = UnitConverter.getUnits(ConverterCategory.LENGTH)[1],
    val converterInput: String = "1",

    // Режим скорости
    val speedPreset: SpeedPreset = SpeedPreset.ARITHMETIC,
    val svtDifficulty: SvtDifficulty = SvtDifficulty.EASY,
    val isSpeedRunning: Boolean = false,
    val speedSecondsLeft: Int = 60,
    val currentTask: SpeedTask = SpeedGenerator.nextTask(SpeedPreset.ARITHMETIC),
    val speedUserAnswer: String = "",
    val speedCurrentStreak: Int = 0,
    val speedBestScores: Map<SpeedPreset, Int> = emptyMap(),
    val svtBestScores: Map<SvtDifficulty, Int> = emptyMap(),

    // ИИ-режим
    val apiKey: String = "",
    val geminiModelId: String = "",
    val aiPrompt: String = "",
    val aiResponse: String = "",
    val isAiLoading: Boolean = false,
    val aiError: String? = null
)

class CalcViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val geminiClient: GeminiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalcUiState())
    val uiState: StateFlow<CalcUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            preferencesRepository.settingsFlow.collect { stored ->
                _uiState.update {
                    it.copy(
                        themeMode = stored.themeMode,
                        useDynamicColor = stored.useDynamicColor,
                        hapticStrength = stored.hapticStrength,
                        apiKey = stored.decryptedApiKey,
                        geminiModelId = stored.geminiModelId,
                        speedBestScores = stored.highScores,
                        svtBestScores = stored.svtHighScores
                    )
                }
            }
        }
    }

    fun selectMode(mode: CalcMode) {
        _uiState.update { it.copy(currentMode = mode) }
    }

    fun toggleSettings(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(themeMode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }
    }

    fun setHapticStrength(strength: HapticStrength) {
        viewModelScope.launch { preferencesRepository.setHapticStrength(strength) }
    }

    fun saveAiSettings(apiKey: String, modelId: String) {
        viewModelScope.launch {
            preferencesRepository.saveAiCredentials(apiKey, modelId)
        }
    }

    fun appendToken(token: String) {
        _uiState.update { state ->
            val newExpr = state.expression + token
            recalcPreview(state.copy(expression = newExpr, isError = false))
        }
    }

    fun appendSmartBracket() {
        _uiState.update { state ->
            val expr = state.expression
            val open = expr.count { it == '(' }
            val close = expr.count { it == ')' }
            val lastChar = expr.lastOrNull()
            val bracket = if (open > close && (lastChar?.isDigit() == true || lastChar == ')' || lastChar == 'π' || lastChar == 'e')) {
                ")"
            } else {
                "("
            }
            recalcPreview(state.copy(expression = expr + bracket, isError = false))
        }
    }

    fun deleteLast() {
        _uiState.update { state ->
            if (state.expression.isEmpty()) return@update state
            val dropCount = when {
                state.expression.endsWith("Ans") -> 3
                state.expression.endsWith("asin(") || state.expression.endsWith("acos(") ||
                        state.expression.endsWith("atan(") -> 5
                state.expression.endsWith("sin(") || state.expression.endsWith("cos(") ||
                        state.expression.endsWith("tan(") || state.expression.endsWith("log(") -> 4
                state.expression.endsWith("ln(") -> 3
                else -> 1
            }
            val newExpr = state.expression.dropLast(dropCount.coerceAtMost(state.expression.length))
            recalcPreview(state.copy(expression = newExpr, isError = false))
        }
    }

    fun clearAll() {
        _uiState.update {
            it.copy(
                expression = "",
                previousExpression = "",
                livePreview = "",
                isError = false
            )
        }
    }

    fun evaluateFinal() {
        _uiState.update { state ->
            if (state.expression.isBlank()) return@update state
            val res = MathEvaluator.evaluate(state.expression, state.isDegrees, state.ansValue)
            res.fold(
                onSuccess = { value ->
                    if (value.isBlank()) state
                    else state.copy(
                        previousExpression = state.expression,
                        expression = value,
                        livePreview = "",
                        ansValue = value,
                        isError = false,
                        resultAnimationTick = state.resultAnimationTick + 1
                    )
                },
                onFailure = {
                    state.copy(livePreview = "Ошибка", isError = true)
                }
            )
        }
    }

    fun toggleDegRad() {
        _uiState.update { state ->
            recalcPreview(state.copy(isDegrees = !state.isDegrees))
        }
    }

    fun memoryAdd() {
        val currentVal = getCurrentNumericValue() ?: return
        _uiState.update { it.copy(memoryValue = it.memoryValue + currentVal) }
    }

    fun memorySubtract() {
        val currentVal = getCurrentNumericValue() ?: return
        _uiState.update { it.copy(memoryValue = it.memoryValue - currentVal) }
    }

    fun memoryRecall() {
        val mem = _uiState.value.memoryValue
        val formatted = MathEvaluator.evaluate(mem.toString()).getOrDefault("0")
        appendToken(formatted)
    }

    fun memoryClear() {
        _uiState.update { it.copy(memoryValue = 0.0) }
    }

    private fun getCurrentNumericValue(): Double? {
        val state = _uiState.value
        val source = state.livePreview.ifBlank { state.expression }
        return MathEvaluator.evaluate(source, state.isDegrees, state.ansValue)
            .getOrNull()
            ?.toDoubleOrNull()
    }

    private fun recalcPreview(state: CalcUiState): CalcUiState {
        if (state.expression.isBlank()) return state.copy(livePreview = "", isError = false)
        val preview = MathEvaluator.evaluate(state.expression, state.isDegrees, state.ansValue)
            .getOrDefault("")
        return state.copy(livePreview = if (preview == state.expression) "" else preview)
    }

    fun updateFractionFields(
        f1W: String = _uiState.value.frac1Whole,
        f1N: String = _uiState.value.frac1Num,
        f1D: String = _uiState.value.frac1Den,
        f2W: String = _uiState.value.frac2Whole,
        f2N: String = _uiState.value.frac2Num,
        f2D: String = _uiState.value.frac2Den,
        op: String = _uiState.value.fracOp,
        decInput: String = _uiState.value.decimalToFractionInput
    ) {
        _uiState.update {
            it.copy(
                frac1Whole = f1W,
                frac1Num = f1N,
                frac1Den = f1D,
                frac2Whole = f2W,
                frac2Num = f2N,
                frac2Den = f2D,
                fracOp = op,
                decimalToFractionInput = decInput
            )
        }
    }

    fun computeFractionResult(): Result<FractionNumber> {
        val s = _uiState.value
        return runCatching {
            val f1 = FractionNumber.fromMixed(s.frac1Whole, s.frac1Num, s.frac1Den).getOrThrow()
            val f2 = FractionNumber.fromMixed(s.frac2Whole, s.frac2Num, s.frac2Den).getOrThrow()
            when (s.fracOp) {
                "+" -> f1 + f2
                "−" -> f1 - f2
                "×" -> f1 * f2
                "÷" -> f1 / f2
                else -> f1 + f2
            }
        }
    }

    fun selectConverterCategory(category: ConverterCategory) {
        val units = UnitConverter.getUnits(category)
        _uiState.update {
            it.copy(
                converterCategory = category,
                converterFromUnit = units[0],
                converterToUnit = units.getOrElse(1) { units[0] }
            )
        }
    }

    fun setConverterUnits(from: UnitItem, to: UnitItem) {
        _uiState.update { it.copy(converterFromUnit = from, converterToUnit = to) }
    }

    fun swapConverterUnits() {
        _uiState.update {
            it.copy(
                converterFromUnit = it.converterToUnit,
                converterToUnit = it.converterFromUnit
            )
        }
    }

    fun setConverterInput(value: String) {
        _uiState.update { it.copy(converterInput = value) }
    }

    fun selectSpeedPreset(preset: SpeedPreset) {
        stopSpeedGame()
        _uiState.update {
            it.copy(
                speedPreset = preset,
                currentTask = SpeedGenerator.nextTask(preset, it.svtDifficulty),
                speedUserAnswer = "",
                speedCurrentStreak = 0
            )
        }
    }

    fun selectSvtDifficulty(difficulty: SvtDifficulty) {
        stopSpeedGame()
        _uiState.update {
            it.copy(
                svtDifficulty = difficulty,
                currentTask = SpeedGenerator.nextTask(it.speedPreset, difficulty),
                speedUserAnswer = "",
                speedCurrentStreak = 0
            )
        }
    }

    fun startSpeedGame() {
        timerJob?.cancel()
        val preset = _uiState.value.speedPreset
        val diff = _uiState.value.svtDifficulty
        _uiState.update {
            it.copy(
                isSpeedRunning = true,
                speedSecondsLeft = 60,
                speedCurrentStreak = 0,
                speedUserAnswer = "",
                currentTask = SpeedGenerator.nextTask(preset, diff)
            )
        }
        timerJob = viewModelScope.launch {
            while (_uiState.value.speedSecondsLeft > 0 && _uiState.value.isSpeedRunning) {
                delay(1000L)
                _uiState.update { state ->
                    val nextSec = state.speedSecondsLeft - 1
                    if (nextSec <= 0) {
                        state.copy(isSpeedRunning = false, speedSecondsLeft = 0)
                    } else {
                        state.copy(speedSecondsLeft = nextSec)
                    }
                }
            }
        }
    }

    fun stopSpeedGame() {
        timerJob?.cancel()
        _uiState.update { it.copy(isSpeedRunning = false) }
    }

    fun onSpeedInput(char: String) {
        if (!_uiState.value.isSpeedRunning) return
        _uiState.update { state ->
            val updatedInput = when (char) {
                "⌫" -> state.speedUserAnswer.dropLast(1)
                "C" -> ""
                else -> state.speedUserAnswer + char
            }
            if (SpeedGenerator.isCorrect(updatedInput, state.currentTask)) {
                val newStreak = state.speedCurrentStreak + 1
                viewModelScope.launch {
                    if (state.speedPreset == SpeedPreset.PHYSICS_SVT) {
                        preferencesRepository.updateSvtHighScoreIfBetter(state.svtDifficulty, newStreak)
                    } else {
                        preferencesRepository.updateHighScoreIfBetter(state.speedPreset, newStreak)
                    }
                }
                state.copy(
                    speedCurrentStreak = newStreak,
                    speedUserAnswer = "",
                    currentTask = SpeedGenerator.nextTask(state.speedPreset, state.svtDifficulty)
                )
            } else {
                state.copy(speedUserAnswer = updatedInput)
            }
        }
    }

    fun updateAiPrompt(text: String) {
        _uiState.update { it.copy(aiPrompt = text, aiError = null) }
    }

    fun solveWithAi() {
        val state = _uiState.value
        if (state.isAiLoading) return
        _uiState.update { it.copy(isAiLoading = true, aiError = null, aiResponse = "") }

        viewModelScope.launch {
            val result = geminiClient.solveMathProblem(
                apiKey = state.apiKey,
                modelId = state.geminiModelId,
                problemText = state.aiPrompt
            )
            result.fold(
                onSuccess = { answer ->
                    _uiState.update { it.copy(isAiLoading = false, aiResponse = answer, aiError = null) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isAiLoading = false, aiError = err.message ?: "Неизвестная ошибка") }
                }
            )
        }
    }

    class Factory(
        private val preferencesRepository: PreferencesRepository,
        private val geminiClient: GeminiClient
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalcViewModel(preferencesRepository, geminiClient) as T
        }
    }
}