package io.github.calcplus.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.random.Random

enum class SpeedPreset(val title: String) {
    ARITHMETIC("Арифметика"),
    PERCENTAGES("Проценты"),
    POWERS("Степени и корни"),
    FRACTIONS("Дроби"),
    PHYSICS_SVT("Путь S = V·t")
}

enum class SvtDifficulty(val title: String) {
    EASY("Лёгкая"),
    MEDIUM("Средняя"),
    HARD("Сложная")
}

data class SpeedTask(
    val question: String,
    val expectedAnswer: String,
    val numericAnswer: Double? = null,
    val tolerance: Double = 0.05
)

object SpeedGenerator {

    fun nextTask(
        preset: SpeedPreset,
        svtDifficulty: SvtDifficulty = SvtDifficulty.EASY
    ): SpeedTask {
        return when (preset) {
            SpeedPreset.ARITHMETIC -> generateArithmetic()
            SpeedPreset.PERCENTAGES -> generatePercentage()
            SpeedPreset.POWERS -> generatePowers()
            SpeedPreset.FRACTIONS -> generateFractions()
            SpeedPreset.PHYSICS_SVT -> generatePhysicsSvt(svtDifficulty)
        }
    }

    fun isCorrect(userInput: String, task: SpeedTask): Boolean {
        val clean = userInput.trim().replace(",", ".")
        if (clean.isEmpty()) return false
        if (clean == task.expectedAnswer) return true

        val target = task.numericAnswer ?: return false
        val userNumber = clean.toDoubleOrNull() ?: return false
        return abs(userNumber - target) <= task.tolerance
    }

    private fun generateArithmetic(): SpeedTask {
        return when (Random.nextInt(3)) {
            0 -> {
                val a = Random.nextInt(12, 99)
                val b = Random.nextInt(12, 99)
                SpeedTask("$a + $b", "${a + b}")
            }
            1 -> {
                val a = Random.nextInt(30, 120)
                val b = Random.nextInt(11, a)
                SpeedTask("$a − $b", "${a - b}")
            }
            else -> {
                val a = Random.nextInt(4, 16)
                val b = Random.nextInt(4, 15)
                SpeedTask("$a × $b", "${a * b}")
            }
        }
    }

    private fun generatePercentage(): SpeedTask {
        val percents = listOf(10, 15, 20, 25, 50, 75)
        val p = percents.random()
        val base = listOf(20, 40, 60, 80, 120, 160, 200, 400).random()
        val ans = (base * p) / 100
        return SpeedTask("$p% от $base", "$ans")
    }

    private fun generatePowers(): SpeedTask {
        return if (Random.nextBoolean()) {
            val base = Random.nextInt(2, 21)
            SpeedTask("$base²", "${base * base}")
        } else {
            val root = Random.nextInt(3, 18)
            SpeedTask("√${root * root}", "$root")
        }
    }

    private fun generateFractions(): SpeedTask {
        val cases = listOf(
            SpeedTask("1/2 + 1/4", "3/4"),
            SpeedTask("3/4 − 1/2", "1/4"),
            SpeedTask("1/3 + 1/6", "1/2"),
            SpeedTask("2/3 + 1/6", "5/6"),
            SpeedTask("3/5 + 1/5", "4/5"),
            SpeedTask("1/2 × 2/3", "1/3"),
            SpeedTask("3/4 ÷ 1/2", "3/2"),
            SpeedTask("2/5 + 3/10", "7/10")
        )
        return cases.random()
    }

    private fun generatePhysicsSvt(difficulty: SvtDifficulty): SpeedTask {
        return when (difficulty) {
            SvtDifficulty.EASY -> generateSvtEasy()
            SvtDifficulty.MEDIUM -> generateSvtMedium()
            SvtDifficulty.HARD -> generateSvtHard()
        }
    }

    private fun generateSvtEasy(): SpeedTask {
        return when (Random.nextInt(3)) {
            0 -> {
                val v = Random.nextInt(4, 25) * 5
                val t = Random.nextInt(2, 9)
                val s = v * t
                SpeedTask(
                    question = "Автомобиль едет со скоростью $v км/ч в течение $t ч. Какой путь S (в км) он проедет?",
                    expectedAnswer = "$s",
                    numericAnswer = s.toDouble(),
                    tolerance = 0.01
                )
            }
            1 -> {
                val v = Random.nextInt(3, 18) * 5
                val t = Random.nextInt(2, 8)
                val s = v * t
                SpeedTask(
                    question = "Велосипедист проехал $s км за $t ч. Найди его скорость V (в км/ч).",
                    expectedAnswer = "$v",
                    numericAnswer = v.toDouble(),
                    tolerance = 0.01
                )
            }
            else -> {
                val v = Random.nextInt(4, 20) * 5
                val t = Random.nextInt(2, 10)
                val s = v * t
                SpeedTask(
                    question = "Поезд прошёл $s км со скоростью $v км/ч. Сколько часов t занял путь?",
                    expectedAnswer = "$t",
                    numericAnswer = t.toDouble(),
                    tolerance = 0.01
                )
            }
        }
    }

    private fun generateSvtMedium(): SpeedTask {
        return when (Random.nextInt(3)) {
            0 -> {
                val vMs = Random.nextInt(5, 31)
                val vKmh = (vMs * 36) / 10.0
                val tSec = Random.nextInt(4, 25)
                val sMeters = vMs * tSec
                val vKmhFormatted = formatNumber(vKmh)
                SpeedTask(
                    question = "Машина едет со скоростью $vKmhFormatted км/ч. Какой путь S (в метрах) она проедет за $tSec с?",
                    expectedAnswer = "$sMeters",
                    numericAnswer = sMeters.toDouble(),
                    tolerance = 0.1
                )
            }
            1 -> {
                val minutesOptions = listOf(15, 20, 30, 45)
                val minutes = minutesOptions.random()
                val vKmh = listOf(36, 48, 60, 72, 80, 90, 120).random()
                val sKm = vKmh * (minutes / 60.0)
                SpeedTask(
                    question = "Мотоциклист движется со скоростью $vKmh км/ч. Сколько километров (S) он проедет за $minutes минут?",
                    expectedAnswer = formatNumber(sKm),
                    numericAnswer = sKm,
                    tolerance = 0.05
                )
            }
            else -> {
                val vMs = listOf(5, 10, 15, 20, 25, 30).random()
                val tSec = Random.nextInt(6, 20)
                val sMeters = vMs * tSec
                val vKmh = vMs * 3.6
                SpeedTask(
                    question = "Спортсмен преодолел $sMeters м за $tSec с. Найди его скорость V в км/ч.",
                    expectedAnswer = formatNumber(vKmh),
                    numericAnswer = vKmh,
                    tolerance = 0.05
                )
            }
        }
    }

    private fun generateSvtHard(): SpeedTask {
        return when (Random.nextInt(3)) {
            0 -> {
                val v1 = Random.nextInt(40, 90)
                val v2 = Random.nextInt(45, 95)
                val tHours = listOf(1.5, 2.0, 2.5, 3.0, 4.0).random()
                val s = (v1 + v2) * tHours
                SpeedTask(
                    question = "Два поезда выехали навстречу со скоростями $v1 км/ч и $v2 км/ч. Расстояние между ними ${formatNumber(s)} км. Через сколько часов t они встретятся?",
                    expectedAnswer = formatNumber(tHours),
                    numericAnswer = tHours,
                    tolerance = 0.05
                )
            }
            1 -> {
                val v1 = Random.nextInt(40, 75)
                val diff = listOf(15, 20, 25, 30, 40).random()
                val v2 = v1 + diff
                val tHours = listOf(1.5, 2.0, 2.5, 3.0).random()
                val gapKm = diff * tHours
                SpeedTask(
                    question = "Грузовик едет со скоростью $v1 км/ч. В ${formatNumber(gapKm)} км позади него вдогонку выехала машина со скоростью $v2 км/ч. Через сколько часов t она догонит грузовик?",
                    expectedAnswer = formatNumber(tHours),
                    numericAnswer = tHours,
                    tolerance = 0.05
                )
            }
            else -> {
                val t1 = Random.nextInt(1, 4)
                val t2 = Random.nextInt(1, 4)
                val v1 = Random.nextInt(4, 12) * 10
                val v2 = Random.nextInt(5, 13) * 10
                val avgV = (v1 * t1 + v2 * t2).toDouble() / (t1 + t2)
                SpeedTask(
                    question = "Первые $t1 ч автомобиль ехал со скоростью $v1 км/ч, а следующие $t2 ч — со скоростью $v2 км/ч. Найди среднюю скорость Vср (в км/ч) на всём пути.",
                    expectedAnswer = formatNumber(avgV),
                    numericAnswer = avgV,
                    tolerance = 0.1
                )
            }
        }
    }

    private fun formatNumber(value: Double): String {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }
}