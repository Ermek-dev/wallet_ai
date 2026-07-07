package kz.abdiyev.walletai.util

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionType

object MoneyFormatter {

    fun format(money: Money, showSign: Boolean = false, type: TransactionType? = null): String {
        val major = money.majorAmount
        val formatted = formatNumber(major)
        val sign = when {
            !showSign || type == null -> ""
            type == TransactionType.INCOME -> "+"
            else -> "-"
        }
        return "$sign$formatted ${money.currency.symbol}"
    }

    fun formatBalance(money: Money): String = format(money)

    private fun formatNumber(value: Double): String {
        val whole = value.toLong()
        val fraction = kotlin.math.round((value - whole) * 100).toInt()
        val wholeFormatted = whole.toString().reversed().chunked(3).joinToString(" ").reversed()
        return if (fraction == 0) wholeFormatted else "$wholeFormatted.${fraction.toString().padStart(2, '0')}"
    }

    fun currencyLabel(currency: Currency): String = "${currency.displayName} (${currency.symbol})"
}
