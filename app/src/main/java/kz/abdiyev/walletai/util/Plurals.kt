package kz.abdiyev.walletai.util

import kotlin.math.abs
import kotlin.math.roundToInt

object Plurals {
    fun operations(count: Int): String {
        val mod10 = count % 10
        val mod100 = count % 100
        val word = when {
            mod100 in 11..14 -> "операций"
            mod10 == 1 -> "операция"
            mod10 in 2..4 -> "операции"
            else -> "операций"
        }
        return "$count $word"
    }

    fun hiddenOutsidePeriodHint(count: Int): String =
        "Ещё ${operations(count)} вне периода"

    fun formatSavingsRatePercent(rate: Float): String {
        val rounded = rate.roundToInt()
        return if (rounded < 0) "−${abs(rounded)}%" else "$rounded%"
    }
}
