package kz.abdiyev.walletai.ml.statement

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionType

object StatementAmount {
    fun parseKaspi(sign: Char, amountPart: String, currency: Currency): Pair<TransactionType, Money> {
        val normalized = amountPart
            .replace(Regex("[\\s\u00A0\u202F]"), "")
            .replace(",", ".")
        val major = normalized.toDoubleOrNull()
            ?: throw IllegalArgumentException("Invalid Kaspi amount: $amountPart")
        val type = if (sign == '+') TransactionType.INCOME else TransactionType.EXPENSE
        return type to Money.fromMajor(major, currency)
    }

    fun parseFreedom(signedAmount: String, currency: Currency): Pair<TransactionType, Money> {
        val trimmed = signedAmount.trim()
        val isIncome = trimmed.startsWith("+")
        val numeric = trimmed.removePrefix("+").removePrefix("-").replace(",", "")
        val major = numeric.toDoubleOrNull()
            ?: throw IllegalArgumentException("Invalid Freedom amount: $signedAmount")
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
        return type to Money.fromMajor(major, currency)
    }
}
