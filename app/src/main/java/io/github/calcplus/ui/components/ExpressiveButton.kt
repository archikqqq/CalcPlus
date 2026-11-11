package io.github.calcplus.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.calcplus.model.HapticStrength

enum class CalcButtonRole {
    DIGIT,
    OPERATOR,
    ACTION,
    EQUALS
}

@Composable
fun ExpressiveCalcButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    buttonRole: CalcButtonRole = CalcButtonRole.DIGIT,
    hapticStrength: HapticStrength = HapticStrength.MEDIUM,
    accessibilityLabel: String = label,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val hapticType = when (buttonRole) {
        CalcButtonRole.DIGIT -> CalcHapticType.DIGIT
        CalcButtonRole.OPERATOR, CalcButtonRole.ACTION -> CalcHapticType.OPERATOR
        CalcButtonRole.EQUALS -> CalcHapticType.EQUALS
    }

    // Вибро в момент касания, а не при отпускании
    LaunchedEffect(isPressed) {
        if (isPressed) HapticHelper.perform(context, haptic, hapticType, hapticStrength)
    }

    // 0.5 = полностью скруглённая «таблетка», 0.30 = скруглённый квадрат
    val cornerFraction by animateFloatAsState(
        targetValue = if (isPressed) 0.30f else 0.50f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cornerMorph"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scalePress"
    )

    val (containerColor, contentColor) = when (buttonRole) {
        CalcButtonRole.DIGIT ->
            MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
        CalcButtonRole.OPERATOR, CalcButtonRole.ACTION ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        CalcButtonRole.EQUALS ->
            MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
    }

    // Кнопка занимает ВСЮ ячейку, которую ей выдал родитель (минус маленький зазор)
    BoxWithConstraints(modifier = modifier.padding(2.dp)) {
        val minSide: Dp = minOf(maxWidth, maxHeight).let { if (it == Dp.Infinity) 64.dp else it }
        val shape = RoundedCornerShape(minSide * cornerFraction)

        val iconSize = minSide * 0.44f
        val textFraction = when (label.length) {
            1 -> 0.46f
            2 -> 0.36f
            3 -> 0.30f
            else -> 0.26f
        }
        val fontSize = with(density) { (minSide * textFraction).toSp() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .background(containerColor)
                .semantics {
                    contentDescription = accessibilityLabel
                    role = Role.Button
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(iconSize)
                    )
                } else {
                    Text(
                        text = label,
                        style = textStyle.copy(
                            fontSize = fontSize,
                            lineHeight = fontSize * 1.1f,
                            fontWeight = FontWeight.Medium
                        ),
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}