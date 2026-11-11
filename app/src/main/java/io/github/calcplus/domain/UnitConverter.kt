package io.github.calcplus.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class ConverterCategory(val title: String) {
    LENGTH("Длина"),
    MASS("Масса"),
    TEMPERATURE("Температура"),
    VOLUME("Объём"),
    AREA("Площадь"),
    SPEED("Скорость"),
    TIME("Время"),
    DATA("Данные")
}

data class UnitItem(
    val name: String,
    val shortName: String,
    val toBaseFactor: Double = 1.0
)

object UnitConverter {

    fun getUnits(category: ConverterCategory): List<UnitItem> = when (category) {
        ConverterCategory.LENGTH -> listOf(
            UnitItem("Метры", "м", 1.0),
            UnitItem("Километры", "км", 1000.0),
            UnitItem("Сантиметры", "см", 0.01),
            UnitItem("Миллиметры", "мм", 0.001),
            UnitItem("Мили", "mi", 1609.344),
            UnitItem("Футы", "ft", 0.3048),
            UnitItem("Дюймы", "in", 0.0254)
        )
        ConverterCategory.MASS -> listOf(
            UnitItem("Килограммы", "кг", 1.0),
            UnitItem("Граммы", "г", 0.001),
            UnitItem("Миллиграммы", "мг", 1e-6),
            UnitItem("Тонны", "т", 1000.0),
            UnitItem("Фунты", "lb", 0.45359237),
            UnitItem("Унции", "oz", 0.02834952)
        )
        ConverterCategory.TEMPERATURE -> listOf(
            UnitItem("Цельсий", "°C"),
            UnitItem("Фаренгейт", "°F"),
            UnitItem("Кельвин", "K")
        )
        ConverterCategory.VOLUME -> listOf(
            UnitItem("Литры", "л", 1.0),
            UnitItem("Миллилитры", "мл", 0.001),
            UnitItem("Кубометры", "м³", 1000.0),
            UnitItem("Галлоны (US)", "gal", 3.78541178)
        )
        ConverterCategory.AREA -> listOf(
            UnitItem("Кв. метры", "м²", 1.0),
            UnitItem("Кв. километры", "км²", 1e6),
            UnitItem("Гектары", "га", 10000.0),
            UnitItem("Сотки (Ар)", "а", 100.0),
            UnitItem("Кв. футы", "ft²", 0.09290304)
        )
        ConverterCategory.SPEED -> listOf(
            UnitItem("Метры в секунду", "м/с", 1.0),
            UnitItem("Километры в час", "км/ч", 1.0 / 3.6),
            UnitItem("Мили в час", "mph", 0.44704),
            UnitItem("Узлы", "kn", 0.514444)
        )
        ConverterCategory.TIME -> listOf(
            UnitItem("Секунды", "с", 1.0),
            UnitItem("Миллисекунды", "мс", 0.001),
            UnitItem("Минуты", "мин", 60.0),
            UnitItem("Часы", "ч", 3600.0),
            UnitItem("Сутки", "дн", 86400.0)
        )
        ConverterCategory.DATA -> listOf(
            UnitItem("Байты", "B", 1.0),
            UnitItem("Килобайты", "KB", 1024.0),
            UnitItem("Мегабайты", "MB", 1048576.0),
            UnitItem("Гигабайты", "GB", 1073741824.0),
            UnitItem("Терабайты", "TB", 1099511627776.0)
        )
    }

    fun convert(
        valueStr: String,
        category: ConverterCategory,
        fromUnit: UnitItem,
        toUnit: UnitItem
    ): String {
        val clean = valueStr.trim().replace(",", ".")
        val value = clean.toDoubleOrNull() ?: return ""

        val result = if (category == ConverterCategory.TEMPERATURE) {
            convertTemperature(value, fromUnit.shortName, toUnit.shortName)
        } else {
            val inBase = value * fromUnit.toBaseFactor
            inBase / toUnit.toBaseFactor
        }

        if (result.isNaN() || result.isInfinite()) return "Ошибка"
        return BigDecimal.valueOf(result)
            .round(MathContext(10, RoundingMode.HALF_UP))
            .stripTrailingZeros()
            .toPlainString()
    }

    private fun convertTemperature(value: Double, from: String, to: String): Double {
        if (from == to) return value
        val celsius = when (from) {
            "°C" -> value
            "°F" -> (value - 32.0) * 5.0 / 9.0
            "K" -> value - 273.15
            else -> value
        }
        return when (to) {
            "°C" -> celsius
            "°F" -> celsius * 9.0 / 5.0 + 32.0
            "K" -> celsius + 273.15
            else -> celsius
        }
    }
}