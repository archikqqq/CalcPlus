package io.github.calcplus.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.calcplus.model.CalcMode
import io.github.calcplus.model.HapticStrength
import io.github.calcplus.ui.CalcUiState
import io.github.calcplus.ui.CalcViewModel
import io.github.calcplus.ui.components.CalcButtonRole
import io.github.calcplus.ui.components.CalcHapticType
import io.github.calcplus.ui.components.ExpressiveCalcButton
import io.github.calcplus.ui.components.HapticHelper
import kotlin.math.abs

@Composable
fun CalculatorScreen(
    uiState: CalcUiState,
    viewModel: CalcViewModel,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isScientific = uiState.currentMode == CalcMode.SCIENTIFIC

    // Пропорции экрана (сумма весов в каждом режиме = 1.0). Меняй числа здесь.
    val displayWeight = when {
        isLandscape -> 0.36f
        isScientific -> 0.24f
        else -> 0.48f
    }
    val memoryWeight = 0.07f     // ряд MC/MR/M+/M−/Ans (портрет, инженерный)
    val sciWeight = 0.26f        // 4 ряда sin/cos/... (портрет, инженерный)
    val basicWeight = when {
        isLandscape -> 0.64f
        isScientific -> 0.43f
        else -> 0.52f
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CalculatorDisplay(
            expression = uiState.expression,
            previousExpression = uiState.previousExpression,
            livePreview = uiState.livePreview,
            isError = uiState.isError,
            isDegrees = uiState.isDegrees,
            isScientific = isScientific,
            compact = isScientific || isLandscape,
            memoryValue = uiState.memoryValue,
            ansValue = uiState.ansValue,
            resultAnimationTick = uiState.resultAnimationTick,
            hapticStrength = uiState.hapticStrength,
            onInsertAns = { viewModel.appendToken("Ans") },
            onOpenSettings = { viewModel.toggleSettings(true) },
            onSwipeDelete = viewModel::deleteLast,
            onToggleDegRad = viewModel::toggleDegRad,
            modifier = Modifier
                .fillMaxWidth()
                .weight(displayWeight)
        )

        if (isLandscape) {
            if (isScientific) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(basicWeight)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                    ) {
                        MemoryRow(
                            hapticStrength = uiState.hapticStrength,
                            onMc = viewModel::memoryClear,
                            onMr = viewModel::memoryRecall,
                            onMPlus = viewModel::memoryAdd,
                            onMMinus = viewModel::memorySubtract,
                            onAns = { viewModel.appendToken("Ans") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.20f)
                        )
                        ScientificPad(
                            isDegrees = uiState.isDegrees,
                            hapticStrength = uiState.hapticStrength,
                            onToken = viewModel::appendToken,
                            onToggleDegRad = viewModel::toggleDegRad,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.80f)
                        )
                    }
                    BasicPad(
                        hapticStrength = uiState.hapticStrength,
                        onToken = viewModel::appendToken,
                        onBracket = viewModel::appendSmartBracket,
                        onBackspace = viewModel::deleteLast,
                        onClear = viewModel::clearAll,
                        onEquals = viewModel::evaluateFinal,
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                    )
                }
            } else {
                BasicPad(
                    hapticStrength = uiState.hapticStrength,
                    onToken = viewModel::appendToken,
                    onBracket = viewModel::appendSmartBracket,
                    onBackspace = viewModel::deleteLast,
                    onClear = viewModel::clearAll,
                    onEquals = viewModel::evaluateFinal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(basicWeight)
                )
            }
        } else {
            if (isScientific) {
                MemoryRow(
                    hapticStrength = uiState.hapticStrength,
                    onMc = viewModel::memoryClear,
                    onMr = viewModel::memoryRecall,
                    onMPlus = viewModel::memoryAdd,
                    onMMinus = viewModel::memorySubtract,
                    onAns = { viewModel.appendToken("Ans") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(memoryWeight)
                )
                ScientificPad(
                    isDegrees = uiState.isDegrees,
                    hapticStrength = uiState.hapticStrength,
                    onToken = viewModel::appendToken,
                    onToggleDegRad = viewModel::toggleDegRad,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(sciWeight)
                )
            }
            BasicPad(
                hapticStrength = uiState.hapticStrength,
                onToken = viewModel::appendToken,
                onBracket = viewModel::appendSmartBracket,
                onBackspace = viewModel::deleteLast,
                onClear = viewModel::clearAll,
                onEquals = viewModel::evaluateFinal,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(basicWeight)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalculatorDisplay(
    expression: String,
    previousExpression: String,
    livePreview: String,
    isError: Boolean,
    isDegrees: Boolean,
    isScientific: Boolean,
    compact: Boolean,
    memoryValue: Double,
    ansValue: String,
    resultAnimationTick: Int,
    hapticStrength: HapticStrength,
    onInsertAns: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwipeDelete: () -> Unit,
    onToggleDegRad: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    val animationsEnabled = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        }.getOrDefault(true)
    }

    val flyProgress = remember { Animatable(1f) }
    LaunchedEffect(resultAnimationTick) {
        if (resultAnimationTick > 0) {
            HapticHelper.perform(context, haptic, CalcHapticType.DIGIT, hapticStrength)
            if (animationsEnabled) {
                flyProgress.snapTo(0f)
                flyProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            } else {
                flyProgress.snapTo(1f)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cursorBlink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (animationsEnabled) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 530),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    // В компактном режиме (инженерный/ландшафт) шрифт меньше, чтобы всё влезло
    val dynamicFontSize = if (compact) {
        when {
            expression.length > 20 -> 24.sp
            expression.length > 13 -> 32.sp
            expression.length > 8 -> 40.sp
            else -> 48.sp
        }
    } else {
        when {
            expression.length > 20 -> 34.sp
            expression.length > 13 -> 44.sp
            expression.length > 8 -> 56.sp
            else -> 68.sp
        }
    }

    Card(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                    },
                    onDragEnd = {
                        if (abs(dragAccumulator) > 60f) {
                            HapticHelper.perform(context, haptic, CalcHapticType.DIGIT, hapticStrength)
                            onSwipeDelete()
                        }
                    }
                )
            }
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    val textToCopy = livePreview.ifBlank { expression }
                    if (textToCopy.isNotBlank() && textToCopy != "Ошибка") {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("CalcPlus Result", textToCopy))
                        Toast.makeText(context, "Скопировано: $textToCopy", Toast.LENGTH_SHORT).show()
                    }
                }
            ),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = if (compact) 4.dp else 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Верхняя строка: история слева, DEG/RAD по центру (только инженерный), меню справа
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onInsertAns) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Вставить прошлый ответ Ans ($ansValue)",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (isScientific) {
                    Surface(
                        onClick = onToggleDegRad,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (isDegrees) "DEG" else "RAD",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Меню и настройки",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Выражение, результат, курсор
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (previousExpression.isNotBlank() && livePreview.isBlank()) {
                    Text(
                        text = previousExpression,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer {
                            translationY = (1f - flyProgress.value) * 36f
                            val s = 1f - 0.15f * flyProgress.value
                            scaleX = s
                            scaleY = s
                            alpha = flyProgress.value
                            transformOrigin = TransformOrigin(1f, 0.5f)
                        }
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = (1f - flyProgress.value) * 72f
                            val s = 0.65f + 0.35f * flyProgress.value
                            scaleX = s
                            scaleY = s
                            transformOrigin = TransformOrigin(1f, 1f)
                        }
                ) {
                    Text(
                        text = expression,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = dynamicFontSize,
                            lineHeight = dynamicFontSize * 1.08f
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .width(3.dp)
                            .height(with(density) { (dynamicFontSize * 0.85f).toDp() })
                            .graphicsLayer { alpha = cursorAlpha }
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }

                if (livePreview.isNotBlank()) {
                    Text(
                        text = livePreview,
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }
            }

            // Нижняя строка: индикатор памяти (только если что-то записано)
            if (memoryValue != 0.0) {
                Text(
                    text = "M = $memoryValue",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Box(modifier = Modifier.height(1.dp))
            }
        }
    }
}

@Composable
private fun MemoryRow(
    hapticStrength: HapticStrength,
    onMc: () -> Unit,
    onMr: () -> Unit,
    onMPlus: () -> Unit,
    onMMinus: () -> Unit,
    onAns: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        "MC" to onMc,
        "MR" to onMr,
        "M+" to onMPlus,
        "M−" to onMMinus,
        "Ans" to onAns
    )
    // Зазоры между клавишами даёт сама кнопка (внутренний padding), поэтому spacing не нужен
    Row(modifier = modifier) {
        items.forEach { (label, action) ->
            ExpressiveCalcButton(
                label = label,
                onClick = action,
                buttonRole = CalcButtonRole.ACTION,
                hapticStrength = hapticStrength,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun ScientificPad(
    isDegrees: Boolean,
    hapticStrength: HapticStrength,
    onToken: (String) -> Unit,
    onToggleDegRad: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("sin(" to "sin", "cos(" to "cos", "tan(" to "tan", if (isDegrees) "DEG" to "DEG" else "RAD" to "RAD"),
        listOf("asin(" to "asin", "acos(" to "acos", "atan(" to "atan", "!" to "n!"),
        listOf("ln(" to "ln", "log(" to "log", "√(" to "√", "^" to "xʸ"),
        listOf("π" to "π", "e" to "e", "(" to "(", ")" to ")")
    )

    Column(modifier = modifier) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                row.forEach { (token, title) ->
                    ExpressiveCalcButton(
                        label = title,
                        onClick = {
                            if (token == "DEG" || token == "RAD") onToggleDegRad()
                            else onToken(token)
                        },
                        buttonRole = CalcButtonRole.ACTION,
                        hapticStrength = hapticStrength,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun BasicPad(
    hapticStrength: HapticStrength,
    onToken: (String) -> Unit,
    onBracket: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    modifier: Modifier = Modifier
) {
    data class KeySpec(
        val label: String,
        val role: CalcButtonRole,
        val desc: String = label,
        val icon: ImageVector? = null,
        val action: () -> Unit
    )

    val rows = listOf(
        listOf(
            KeySpec("AC", CalcButtonRole.EQUALS, "Очистить всё", action = onClear),
            KeySpec("( )", CalcButtonRole.OPERATOR, "Скобки", action = onBracket),
            KeySpec("%", CalcButtonRole.OPERATOR, "Процент") { onToken("%") },
            KeySpec("÷", CalcButtonRole.OPERATOR, "Разделить") { onToken("÷") }
        ),
        listOf(
            KeySpec("7", CalcButtonRole.DIGIT) { onToken("7") },
            KeySpec("8", CalcButtonRole.DIGIT) { onToken("8") },
            KeySpec("9", CalcButtonRole.DIGIT) { onToken("9") },
            KeySpec("×", CalcButtonRole.OPERATOR, "Умножить") { onToken("×") }
        ),
        listOf(
            KeySpec("4", CalcButtonRole.DIGIT) { onToken("4") },
            KeySpec("5", CalcButtonRole.DIGIT) { onToken("5") },
            KeySpec("6", CalcButtonRole.DIGIT) { onToken("6") },
            KeySpec("−", CalcButtonRole.OPERATOR, "Минус") { onToken("−") }
        ),
        listOf(
            KeySpec("1", CalcButtonRole.DIGIT) { onToken("1") },
            KeySpec("2", CalcButtonRole.DIGIT) { onToken("2") },
            KeySpec("3", CalcButtonRole.DIGIT) { onToken("3") },
            KeySpec("+", CalcButtonRole.OPERATOR, "Плюс") { onToken("+") }
        ),
        listOf(
            KeySpec("0", CalcButtonRole.DIGIT) { onToken("0") },
            KeySpec(",", CalcButtonRole.DIGIT, "Запятая") { onToken(",") },
            KeySpec(
                label = "⌫",
                role = CalcButtonRole.DIGIT,
                desc = "Удалить символ",
                icon = Icons.AutoMirrored.Outlined.Backspace,
                action = onBackspace
            ),
            KeySpec("=", CalcButtonRole.EQUALS, "Равно", action = onEquals)
        )
    )

    Column(modifier = modifier) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                row.forEach { key ->
                    ExpressiveCalcButton(
                        label = key.label,
                        icon = key.icon,
                        onClick = key.action,
                        buttonRole = key.role,
                        hapticStrength = hapticStrength,
                        accessibilityLabel = key.desc,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}