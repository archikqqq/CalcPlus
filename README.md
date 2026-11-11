# CalcPlus

<p align="center">
  <img src="https://img.shields.io/github/actions/workflow/status/archikqqq/CalcPlus/build.yml?branch=main&style=for-the-badge&logo=githubactions&logoColor=white&label=Build" alt="Build Status" />
  <img src="https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Android%20SDK-36-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android SDK 36" />
  <img src="https://img.shields.io/badge/Material%20Design-3%20Expressive-6750A4?style=for-the-badge&logo=materialdesign&logoColor=white" alt="Material Design 3 Expressive" />
  <img src="https://img.shields.io/github/license/archikqqq/CalcPlus?style=for-the-badge&color=blue" alt="License" />
</p>

---

Современный калькулятор для Android с расширенными математическими возможностями и интерфейсом на базе **Material Design 3 Expressive**.

## Возможности

- **Обычный калькулятор** — базовые арифметические вычисления с мгновенным отображением промежуточного результата.
- **Инженерный калькулятор** — расширенные тригонометрические, логарифмические и степенные операции.
- **Работа с дробями** — точные расчёты обыкновенных дробей с автоматическим сокращением и приведением к общему знаменателю.
- **Конвертер единиц** — быстрый перевод различных систем измерений.
- **Математический тренажёр** — интерактивные упражнения для тренировки устного счёта и логики.
- **ИИ-режим** — специализированный режим для решения задач с контекстными подсказками.

## Стек технологий

- **Язык**: Kotlin
- **UI Toolkit**: Jetpack Compose
- **Дизайн-система**: Material Design 3 Expressive, Material You (Dynamic Colors)
- **Целевая платформа**: Android (minSdk 36, targetSdk 36)
- **Сборщик**: Gradle 9.5.0
- **Java**: JDK 17

## Сборка и запуск

Для сборки проекта на локальной машине требуется установленный **JDK 17** и **Android SDK 36**:

```bash
# Клонирование репозитория
git clone [https://github.com/archikqqq/CalcPlus.git](https://github.com/archikqqq/CalcPlus.git)

# Переход в каталог проекта
cd CalcPlus

# Сборка Debug APK
./gradlew assembleDebug