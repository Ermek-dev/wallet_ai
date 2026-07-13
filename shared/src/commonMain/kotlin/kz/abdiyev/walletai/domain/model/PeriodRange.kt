package kz.abdiyev.walletai.domain.model

/**
 * Half-open interval [startInclusive, endExclusive) in epoch millis.
 * The last civil day is included via endExclusive at the start of the next local day.
 */
data class PeriodRange(
    val startInclusive: Long,
    val endExclusive: Long
)
