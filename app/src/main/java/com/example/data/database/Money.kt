package com.example.data.database

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Money helper for local UI/prototype accounting.
 *
 * Storage rule:
 * - All authoritative ledger/transaction calculations use minor units (1 Riyal = 100 minor units).
 * - Double fields remain only for old UI compatibility and human-readable cached display.
 */
object Money {
    const val SCALE: Int = 2
    const val FACTOR: Long = 100L

    fun toMinor(amount: Double): Long {
        return BigDecimal.valueOf(amount)
            .setScale(SCALE, RoundingMode.HALF_UP)
            .movePointRight(SCALE)
            .longValueExact()
    }

    fun toMajor(minor: Long): Double {
        return BigDecimal.valueOf(minor)
            .movePointLeft(SCALE)
            .setScale(SCALE, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun roundMajor(amount: Double): Double = toMajor(toMinor(amount))

    fun formatMajor(amount: Double): String = formatMinor(toMinor(amount))

    fun formatMinor(minor: Long): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = DecimalFormat("#,##0.00", symbols)
        return formatter.format(toMajor(minor))
    }
}
