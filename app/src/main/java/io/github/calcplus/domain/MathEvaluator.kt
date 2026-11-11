package io.github.calcplus.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object MathEvaluator {

    fun evaluate(expression: String, isDegrees: Boolean = true, ansValue: String = "0"): Result<String> {
        return runCatching {
            val cleanExpr = preprocess(expression, ansValue)
            if (cleanExpr.isBlank()) return Result.success("")
            val parser = Parser(cleanExpr, isDegrees)
            val result = parser.parse()
            if (result.isNaN() || result.isInfinite()) {
                throw ArithmeticException("Неопределённость или деление на 0")
            }
            formatResult(result)
        }
    }

    private fun preprocess(expr: String, ansValue: String): String {
        val safeAns = if (ansValue.isBlank() || ansValue == "Ошибка") "0" else ansValue.replace(",", ".")
        var s = expr
            .replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(",", ".")
            .replace("Ans", "($safeAns)")
            .replace("π", "(${Math.PI})")
            .replace("√(", "sqrt(")
            .replace("√", "sqrt")

        s = Regex("(?<![a-zA-Z])e(?![a-zA-Z])").replace(s, "(${Math.E})")

        s = Regex("(\\d|\\)|!)(\\(|[a-zA-Z])").replace(s) {
            "${it.groupValues[1]}*${it.groupValues[2]}"
        }
        s = Regex("(\\))(\\d)").replace(s) {
            "${it.groupValues[1]}*${it.groupValues[2]}"
        }

        val openCount = s.count { it == '(' }
        val closeCount = s.count { it == ')' }
        if (openCount > closeCount) {
            s += ")".repeat(openCount - closeCount)
        }
        return s
    }

    private fun formatResult(value: Double): String {
        if (kotlin.math.abs(value) < 1e-12) return "0"
        val bd = BigDecimal.valueOf(value)
            .round(MathContext(12, RoundingMode.HALF_UP))
            .stripTrailingZeros()
        return bd.toPlainString()
    }

    private class Parser(private val input: String, private val isDegrees: Boolean) {
        private var pos = -1
        private var ch = 0

        init {
            nextChar()
        }

        private fun nextChar() {
            pos++
            ch = if (pos < input.length) input[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            val x = parseExpression()
            if (pos < input.length) throw IllegalArgumentException("Неожиданный символ: ${input[pos]}")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parsePostfix()
            while (true) {
                when {
                    eat('*'.code) -> x *= parsePostfix()
                    eat('/'.code) -> {
                        val divisor = parsePostfix()
                        if (kotlin.math.abs(divisor) < 1e-15) throw ArithmeticException("Деление на 0")
                        x /= divisor
                    }
                    else -> return x
                }
            }
        }

        private fun parsePostfix(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('!'.code) -> x = factorial(x)
                    eat('%'.code) -> x /= 100.0
                    eat('^'.code) -> x = x.pow(parsePostfix())
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos

            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = input.substring(startPos, pos).toDouble()
            } else if (ch in 'a'.code..'z'.code) {
                while (ch in 'a'.code..'z'.code) nextChar()
                val func = input.substring(startPos, pos)
                x = parseFactor()
                x = applyFunction(func, x)
            } else {
                throw IllegalArgumentException("Некорректный ввод")
            }

            return x
        }

        private fun applyFunction(func: String, arg: Double): Double {
            val angleInRad = if (isDegrees) Math.toRadians(arg) else arg
            return when (func) {
                "sin" -> sin(angleInRad)
                "cos" -> cos(angleInRad)
                "tan" -> tan(angleInRad)
                "asin" -> {
                    val res = asin(arg)
                    if (isDegrees) Math.toDegrees(res) else res
                }
                "acos" -> {
                    val res = acos(arg)
                    if (isDegrees) Math.toDegrees(res) else res
                }
                "atan" -> {
                    val res = atan(arg)
                    if (isDegrees) Math.toDegrees(res) else res
                }
                "ln" -> {
                    if (arg <= 0) throw ArithmeticException("ln от неположительного числа")
                    ln(arg)
                }
                "log" -> {
                    if (arg <= 0) throw ArithmeticException("log от неположительного числа")
                    log10(arg)
                }
                "sqrt" -> {
                    if (arg < 0) throw ArithmeticException("Корень из отрицательного числа")
                    sqrt(arg)
                }
                else -> throw IllegalArgumentException("Неизвестная функция: $func")
            }
        }

        private fun factorial(n: Double): Double {
            if (n < 0 || n != kotlin.math.floor(n)) {
                throw ArithmeticException("Факториал только для целых неотрицательных чисел")
            }
            if (n > 170) throw ArithmeticException("Переполнение факториала")
            var res = 1.0
            for (i in 2..n.toInt()) {
                res *= i.toDouble()
            }
            return res
        }
    }
}