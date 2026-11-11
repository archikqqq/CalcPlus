package io.github.calcplus.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

data class FractionNumber(
    val numerator: BigInteger,
    val denominator: BigInteger
) {
    init {
        require(denominator != BigInteger.ZERO) { "Знаменатель не может быть равен 0" }
    }

    fun simplify(): FractionNumber {
        if (numerator == BigInteger.ZERO) return FractionNumber(BigInteger.ZERO, BigInteger.ONE)
        val sign = if (denominator < BigInteger.ZERO) BigInteger.valueOf(-1) else BigInteger.ONE
        val num = numerator.multiply(sign)
        val den = denominator.multiply(sign)
        val common = num.abs().gcd(den)
        return FractionNumber(num.divide(common), den.divide(common))
    }

    operator fun plus(other: FractionNumber): FractionNumber {
        val num = this.numerator * other.denominator + other.numerator * this.denominator
        val den = this.denominator * other.denominator
        return FractionNumber(num, den).simplify()
    }

    operator fun minus(other: FractionNumber): FractionNumber {
        val num = this.numerator * other.denominator - other.numerator * this.denominator
        val den = this.denominator * other.denominator
        return FractionNumber(num, den).simplify()
    }

    operator fun times(other: FractionNumber): FractionNumber {
        val num = this.numerator * other.numerator
        val den = this.denominator * other.denominator
        return FractionNumber(num, den).simplify()
    }

    operator fun div(other: FractionNumber): FractionNumber {
        require(other.numerator != BigInteger.ZERO) { "Деление на нулевую дробь" }
        val num = this.numerator * other.denominator
        val den = this.denominator * other.numerator
        return FractionNumber(num, den).simplify()
    }

    fun toImproperString(): String {
        val s = simplify()
        return if (s.denominator == BigInteger.ONE) "${s.numerator}" else "${s.numerator}/${s.denominator}"
    }

    fun toMixedString(): String {
        val s = simplify()
        if (s.denominator == BigInteger.ONE) return "${s.numerator}"
        val whole = s.numerator.divide(s.denominator)
        val rem = s.numerator.remainder(s.denominator).abs()
        return if (whole == BigInteger.ZERO) {
            "${s.numerator}/${s.denominator}"
        } else if (rem == BigInteger.ZERO) {
            "$whole"
        } else {
            "$whole  $rem/${s.denominator}"
        }
    }

    fun toDecimalString(scale: Int = 8): String {
        val s = simplify()
        return BigDecimal(s.numerator)
            .divide(BigDecimal(s.denominator), scale, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    companion object {
        fun fromMixed(wholeStr: String, numStr: String, denStr: String): Result<FractionNumber> {
            return runCatching {
                val whole = if (wholeStr.isBlank() || wholeStr == "-") BigInteger.ZERO else BigInteger(wholeStr.trim())
                val num = if (numStr.isBlank()) BigInteger.ZERO else BigInteger(numStr.trim())
                val den = if (denStr.isBlank()) BigInteger.ONE else BigInteger(denStr.trim())
                require(den != BigInteger.ZERO) { "Знаменатель = 0" }

                val isNegative = wholeStr.trim().startsWith("-") || num < BigInteger.ZERO
                val absWhole = whole.abs()
                val absNum = num.abs()
                val totalNum = absWhole.multiply(den.abs()).add(absNum)
                val signedNum = if (isNegative) totalNum.negate() else totalNum
                FractionNumber(signedNum, den.abs()).simplify()
            }
        }

        fun fromDecimal(decimalStr: String): Result<FractionNumber> {
            return runCatching {
                val clean = decimalStr.trim().replace(",", ".")
                require(clean.isNotEmpty()) { "Пустое число" }
                val bd = BigDecimal(clean).stripTrailingZeros()
                val scale = bd.scale()
                if (scale <= 0) {
                    FractionNumber(bd.toBigInteger(), BigInteger.ONE)
                } else {
                    val unscaled = bd.unscaledValue()
                    val den = BigInteger.TEN.pow(scale)
                    FractionNumber(unscaled, den).simplify()
                }
            }
        }
    }
}