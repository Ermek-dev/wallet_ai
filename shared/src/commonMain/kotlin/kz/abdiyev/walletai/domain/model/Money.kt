package kz.abdiyev.walletai.domain.model

data class Money(
    val amountMinor: Long,
    val currency: Currency = Currency.KZT
) {
    val majorAmount: Double get() = amountMinor.toDouble() / currency.minorUnits

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies" }
        return copy(amountMinor = amountMinor + other.amountMinor)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Cannot subtract different currencies" }
        return copy(amountMinor = amountMinor - other.amountMinor)
    }

    companion object {
        fun fromMajor(major: Double, currency: Currency = Currency.KZT): Money =
            Money((major * currency.minorUnits).toLong(), currency)

        fun fromMajorString(value: String, currency: Currency = Currency.KZT): Money? {
            // Keyboards and voice input produce grouping spaces (incl. NBSP) and comma decimals
            val normalized = value
                .replace(",", ".")
                .replace(Regex("[\\s\u00A0\u202F]"), "")
            val major = normalized.toDoubleOrNull() ?: return null
            if (major <= 0) return null
            return fromMajor(major, currency)
        }
    }
}
