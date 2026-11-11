package io.github.calcplus.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// ---------- Генератор задач ----------

enum class MotionDifficulty(val title: String) {
    EASY("Простые"),
    MEDIUM("Средние"),
    HARD("Сложные")
}

data class MotionTask(val text: String, val answer: Double, val unit: String)

private fun Double.fmt(): String =
    if (this % 1.0 == 0.0) toLong().toString()
    else String.format(Locale.US, "%.1f", this).replace('.', ',')

private fun round1(x: Double): Double = (x * 10).roundToInt() / 10.0

private val easyMovers = listOf(
    "Пешеход" to 3..6,
    "Велосипедист" to 12..25,
    "Автомобиль" to 60..100,
    "Поезд" to 80..140
)

private fun easyTask(): MotionTask {
    val (name, range) = easyMovers.random()
    val v = range.random()
    val t = (2..8).random()
    val s = v * t
    return when ((0..2).random()) {
        0 -> MotionTask("$name двигался $t ч и преодолел $s км. Найди скорость.", v.toDouble(), "км/ч")
        1 -> MotionTask("$name двигался со скоростью $v км/ч в течение $t ч. Какой путь он преодолел?", s.toDouble(), "км")
        else -> MotionTask("$name преодолел $s км со скоростью $v км/ч. Сколько часов он был в пути?", t.toDouble(), "ч")
    }
}

private fun mediumTask(): MotionTask = when ((0..3).random()) {
    0 -> {
        val v = listOf(18, 36, 54, 72, 90, 108).random()
        MotionTask("Скорость автомобиля $v км/ч. Вырази её в метрах в секунду.", v / 3.6, "м/с")
    }
    1 -> {
        val m = (5..30).random()
        MotionTask("Скорость катера $m м/с. Вырази её в километрах в час.", m * 3.6, "км/ч")
    }
    2 -> {
        val v = (8..48 step 4).toList().random()
        val min = listOf(15, 30, 45, 90, 120, 150).random()
        MotionTask("Велосипедист ехал $min мин со скоростью $v км/ч. Какой путь он проехал?", v * min / 60.0, "км")
    }
    else -> {
        val v = listOf(12, 24, 36, 48, 60).random()
        val min = listOf(15, 20, 30, 40, 45).random()
        val s = v * min / 60.0
        MotionTask("Мотоциклист проехал ${s.fmt()} км за $min мин. Найди его скорость.", v.toDouble(), "км/ч")
    }
}

private fun hardTask(): MotionTask = when ((0..3).random()) {
    0 -> {
        val t = (2..6).random()
        val v1 = (5..9).random() * 10
        val v2 = (4..8).random() * 10
        val d = (v1 + v2) * t
        MotionTask(
            "Из двух городов, расстояние между которыми $d км, навстречу друг другу одновременно выехали два автомобиля со скоростями $v1 и $v2 км/ч. Через сколько часов они встретятся?",
            t.toDouble(), "ч"
        )
    }
    1 -> {
        val t = (2..6).random()
        val v1 = (5..9).random() * 10
        val v2 = (4..8).random() * 10
        val d = (v1 + v2) * t
        MotionTask(
            "Из двух городов навстречу друг другу одновременно выехали два автомобиля со скоростями $v1 и $v2 км/ч. Они встретились через $t ч. Найди расстояние между городами.",
            d.toDouble(), "км"
        )
    }
    2 -> {
        val dd = listOf(5, 10, 15).random()
        val k = (2..5).random()
        val lead = dd * k
        val t0 = listOf(1, 2, 3).filter { lead % it == 0 }.random()
        val v1 = lead / t0
        val v2 = v1 + dd
        MotionTask(
            "Из посёлка выехал велосипедист со скоростью $v1 км/ч. Через $t0 ч вслед за ним выехал мотоциклист со скоростью $v2 км/ч. Через сколько часов после своего выезда мотоциклист догонит велосипедиста?",
            k.toDouble(), "ч"
        )
    }
    else -> {
        val v1 = (4..9).random() * 10
        val v2 = (4..9).random() * 10
        val t1 = (1..4).random()
        val t2 = (1..4).random()
        val avg = (v1 * t1 + v2 * t2).toDouble() / (t1 + t2)
        MotionTask(
            "Поезд ехал $t1 ч со скоростью $v1 км/ч, затем $t2 ч со скоростью $v2 км/ч. Найди среднюю скорость на всём пути (округли до десятых).",
            round1(avg), "км/ч"
        )
    }
}

fun generateMotionTask(d: MotionDifficulty): MotionTask = when (d) {
    MotionDifficulty.EASY -> easyTask()
    MotionDifficulty.MEDIUM -> mediumTask()
    MotionDifficulty.HARD -> hardTask()
}

// ---------- Экран ----------

@Composable
fun MotionTasksScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("calcplus_motion", Context.MODE_PRIVATE) }

    var difficulty by rememberSaveable { mutableStateOf(MotionDifficulty.EASY) }
    var task by remember(difficulty) { mutableStateOf(generateMotionTask(difficulty)) }
    var input by remember(task) { mutableStateOf("") }
    var result by remember(task) { mutableStateOf<Boolean?>(null) }
    var seconds by remember(task) { mutableIntStateOf(0) }
    var streak by remember(difficulty) { mutableIntStateOf(0) }
    var best by remember(difficulty) { mutableIntStateOf(prefs.getInt("best_${difficulty.name}", 0)) }

    // Таймер идёт, пока ответа ещё нет
    LaunchedEffect(task, result) {
        while (result == null) {
            delay(1000)
            seconds++
        }
    }

    fun check() {
        val value = input.replace(',', '.').toDoubleOrNull() ?: return
        val ok = abs(value - task.answer) <= 0.05
        result = ok
        if (ok) {
            streak++
            if (streak > best) {
                best = streak
                prefs.edit().putInt("best_${difficulty.name}", best).apply()
            }
        } else {
            streak = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MotionDifficulty.entries.forEach { d ->
                FilterChip(
                    selected = difficulty == d,
                    onClick = { difficulty = d },
                    label = { Text(d.title) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Серия: $streak  •  Рекорд: $best", style = MaterialTheme.typography.titleMedium)
            Text(
                String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = task.text,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(20.dp)
            )
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = result == null,
            singleLine = true,
            label = { Text("Ответ, ${task.unit}") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        when (result) {
            true -> Text("Верно!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            false -> Text(
                "Неверно. Правильный ответ: ${task.answer.fmt()} ${task.unit}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            null -> {}
        }

        if (result == null) {
            Button(
                onClick = { check() },
                enabled = input.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Проверить") }
            OutlinedButton(
                onClick = { streak = 0; task = generateMotionTask(difficulty) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Пропустить") }
        } else {
            Button(
                onClick = { task = generateMotionTask(difficulty) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Следующая") }
        }
    }
}