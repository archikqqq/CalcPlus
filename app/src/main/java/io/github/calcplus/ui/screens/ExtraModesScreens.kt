package io.github.calcplus.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.calcplus.domain.ConverterCategory
import io.github.calcplus.domain.FractionNumber
import io.github.calcplus.domain.SpeedPreset
import io.github.calcplus.domain.SvtDifficulty
import io.github.calcplus.domain.UnitConverter
import io.github.calcplus.model.HapticStrength
import io.github.calcplus.model.ThemeMode
import io.github.calcplus.ui.CalcUiState
import io.github.calcplus.ui.CalcViewModel
import io.github.calcplus.ui.components.CalcButtonRole
import io.github.calcplus.ui.components.ExpressiveCalcButton

@Composable
fun FractionsScreen(
    uiState: CalcUiState,
    viewModel: CalcViewModel,
    modifier: Modifier = Modifier
) {
    val calcResult = viewModel.computeFractionResult()
    val decToFracResult = FractionNumber.fromDecimal(uiState.decimalToFractionInput)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Калькулятор дробей (BigInteger)", style = MaterialTheme.typography.titleLarge)

                FractionInputRow(
                    label = "Дробь 1",
                    whole = uiState.frac1Whole,
                    num = uiState.frac1Num,
                    den = uiState.frac1Den,
                    onWholeChange = { viewModel.updateFractionFields(f1W = it) },
                    onNumChange = { viewModel.updateFractionFields(f1N = it) },
                    onDenChange = { viewModel.updateFractionFields(f1D = it) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("+", "−", "×", "÷").forEach { op ->
                        FilterChip(
                            selected = uiState.fracOp == op,
                            onClick = { viewModel.updateFractionFields(op = op) },
                            label = { Text(op, style = MaterialTheme.typography.titleLarge) }
                        )
                    }
                }

                FractionInputRow(
                    label = "Дробь 2",
                    whole = uiState.frac2Whole,
                    num = uiState.frac2Num,
                    den = uiState.frac2Den,
                    onWholeChange = { viewModel.updateFractionFields(f2W = it) },
                    onNumChange = { viewModel.updateFractionFields(f2N = it) },
                    onDenChange = { viewModel.updateFractionFields(f2D = it) }
                )

                calcResult.fold(
                    onSuccess = { frac ->
                        Text(
                            text = "Неправильная (сокращённая): ${frac.toImproperString()}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Смешанная: ${frac.toMixedString()}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Десятичная: ${frac.toDecimalString()}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onFailure = { err ->
                        Text(
                            text = "Ошибка: ${err.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }

        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Десятичная → Обыкновенная", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = uiState.decimalToFractionInput,
                    onValueChange = { viewModel.updateFractionFields(decInput = it) },
                    label = { Text("Введите десятичное число (например, 0.375)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                decToFracResult.onSuccess { frac ->
                    Text(
                        text = "= ${frac.toImproperString()}  (смешанная: ${frac.toMixedString()})",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun FractionInputRow(
    label: String,
    whole: String,
    num: String,
    den: String,
    onWholeChange: (String) -> Unit,
    onNumChange: (String) -> Unit,
    onDenChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = whole,
            onValueChange = onWholeChange,
            label = { Text("$label: Целая") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = num,
            onValueChange = onNumChange,
            label = { Text("Числитель") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = den,
            onValueChange = onDenChange,
            label = { Text("Знаменатель") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConverterScreen(
    uiState: CalcUiState,
    viewModel: CalcViewModel,
    modifier: Modifier = Modifier
) {
    val units = UnitConverter.getUnits(uiState.converterCategory)
    val convertedValue = UnitConverter.convert(
        valueStr = uiState.converterInput,
        category = uiState.converterCategory,
        fromUnit = uiState.converterFromUnit,
        toUnit = uiState.converterToUnit
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConverterCategory.entries.forEach { cat ->
                FilterChip(
                    selected = uiState.converterCategory == cat,
                    onClick = { viewModel.selectConverterCategory(cat) },
                    label = { Text(cat.title) }
                )
            }
        }

        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Из: ${uiState.converterFromUnit.name}", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    units.forEach { u ->
                        FilterChip(
                            selected = uiState.converterFromUnit == u,
                            onClick = { viewModel.setConverterUnits(u, uiState.converterToUnit) },
                            label = { Text(u.shortName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.converterInput,
                    onValueChange = viewModel::setConverterInput,
                    label = { Text("Значение (${uiState.converterFromUnit.shortName})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = viewModel::swapConverterUnits) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Поменять единицы местами"
                        )
                    }
                }

                Text("В: ${uiState.converterToUnit.name}", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    units.forEach { u ->
                        FilterChip(
                            selected = uiState.converterToUnit == u,
                            onClick = { viewModel.setConverterUnits(uiState.converterFromUnit, u) },
                            label = { Text(u.shortName) }
                        )
                    }
                }

                Text(
                    text = "$convertedValue ${uiState.converterToUnit.shortName}",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SpeedModeScreen(
    uiState: CalcUiState,
    viewModel: CalcViewModel,
    modifier: Modifier = Modifier
) {
    val isPhysics = uiState.speedPreset == SpeedPreset.PHYSICS_SVT
    val bestForCurrent = if (isPhysics) {
        uiState.svtBestScores[uiState.svtDifficulty] ?: 0
    } else {
        uiState.speedBestScores[uiState.speedPreset] ?: 0
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeedPreset.entries.forEach { preset ->
                FilterChip(
                    selected = uiState.speedPreset == preset,
                    onClick = { viewModel.selectSpeedPreset(preset) },
                    label = { Text(preset.title) }
                )
            }
        }

        if (isPhysics) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SvtDifficulty.entries.forEach { diff ->
                    FilterChip(
                        selected = uiState.svtDifficulty == diff,
                        onClick = { viewModel.selectSvtDifficulty(diff) },
                        label = { Text(diff.title) }
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Таймер: ${uiState.speedSecondsLeft} с",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (uiState.speedSecondsLeft <= 10 && uiState.isSpeedRunning) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "Серия: ${uiState.speedCurrentStreak}  |  Рекорд: $bestForCurrent",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (!uiState.isSpeedRunning) {
                    Button(
                        onClick = viewModel::startSpeedGame,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.speedSecondsLeft == 0) "Сыграть ещё раз (60 с)" else "Старт (60 секунд)")
                    }
                } else {
                    Text(
                        text = uiState.currentTask.question,
                        style = if (isPhysics) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Ответ: ${uiState.speedUserAnswer.ifBlank { "?" }}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        val bottomLeftKey = if (uiState.speedPreset == SpeedPreset.FRACTIONS) "/" else ","
        val padRows = listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
            listOf(bottomLeftKey, "0", "⌫")
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            padRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { key ->
                        ExpressiveCalcButton(
                            label = key,
                            onClick = { viewModel.onSpeedInput(key) },
                            buttonRole = if (key == "⌫" || key == "/" || key == ",") CalcButtonRole.OPERATOR else CalcButtonRole.DIGIT,
                            hapticStrength = uiState.hapticStrength,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiSolverScreen(
    uiState: CalcUiState,
    viewModel: CalcViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ИИ-решатель задач", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onOpenSettings) {
                        Text("Настроить ключ и модель")
                    }
                }

                Text(
                    text = if (uiState.apiKey.isBlank() || uiState.geminiModelId.isBlank()) {
                        "Статус: Ключ или ID модели не заполнены в настройках"
                    } else {
                        "Активная модель: ${uiState.geminiModelId}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (uiState.apiKey.isBlank() || uiState.geminiModelId.isBlank()) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )

                OutlinedTextField(
                    value = uiState.aiPrompt,
                    onValueChange = viewModel::updateAiPrompt,
                    label = { Text("Опиши задачу текстом или уравнением") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = viewModel::solveWithAi,
                    enabled = !uiState.isAiLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isAiLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(20.dp)
                                .width(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Решаю...")
                    } else {
                        Text("Получить пошаговое решение")
                    }
                }
            }
        }

        if (uiState.aiError != null) {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = uiState.aiError,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (uiState.aiResponse.isNotBlank()) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Text(
                    text = uiState.aiResponse,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBottomSheet(
    uiState: CalcUiState,
    viewModel: CalcViewModel,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var apiKeyDraft by remember(uiState.apiKey) { mutableStateOf(uiState.apiKey) }
    var modelIdDraft by remember(uiState.geminiModelId) { mutableStateOf(uiState.geminiModelId) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Настройки CalcPlus", style = MaterialTheme.typography.headlineMedium)

            Text("Тема оформления", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(mode.title) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Динамические цвета Monet", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = uiState.useDynamicColor,
                    onCheckedChange = viewModel::setDynamicColor
                )
            }

            Text("Сила виброотклика (Haptic)", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HapticStrength.entries.forEach { strength ->
                    FilterChip(
                        selected = uiState.hapticStrength == strength,
                        onClick = { viewModel.setHapticStrength(strength) },
                        label = { Text(strength.title) }
                    )
                }
            }

            Text("Google AI Studio (ИИ-режим)", style = MaterialTheme.typography.titleLarge)
            TextButton(
                onClick = { uriHandler.openUri("https://aistudio.google.com/apikey") }
            ) {
                Text("Получить бесплатный API-ключ: aistudio.google.com/apikey")
            }

            OutlinedTextField(
                value = apiKeyDraft,
                onValueChange = { apiKeyDraft = it },
                label = { Text("API-ключ (хранится зашифрованно AES-GCM)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = modelIdDraft,
                onValueChange = { modelIdDraft = it },
                label = { Text("ID модели из AI Studio") },
                placeholder = { Text("Например: gemini-2.5-flash-lite") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.saveAiSettings(apiKeyDraft, modelIdDraft)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить настройки")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}