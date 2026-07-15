package kz.abdiyev.walletai.domain.model

enum class Period(val dbValue: String, val labelRu: String) {
    CUSTOM("CUSTOM", "Период"),
    WEEK("WEEK", "Неделя"),
    MONTH("MONTH", "Месяц"),
    ALL("ALL", "Всё");

    companion object {
        fun fromDb(value: String): Period = when (value) {
            "THREE_MONTHS" -> MONTH
            else -> entries.find { it.dbValue == value } ?: MONTH
        }
    }
}
