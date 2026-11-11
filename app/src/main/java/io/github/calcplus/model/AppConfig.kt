package io.github.calcplus.model

enum class CalcMode(val title: String) {
    BASIC("Обычный"),
    SCIENTIFIC("Инженерный"),
    FRACTIONS("Дроби"),
    SPEED("Скорость"),
    CONVERTER("Конвертер"),
    AI("ИИ")
}

enum class ThemeMode(val title: String) {
    SYSTEM("Система"),
    LIGHT("Светлая"),
    DARK("Тёмная"),
    AMOLED("AMOLED")
}

enum class HapticStrength(val title: String) {
    OFF("Выкл"),
    LIGHT("Лёгкий"),
    MEDIUM("Средний"),
    STRONG("Сильный")
}