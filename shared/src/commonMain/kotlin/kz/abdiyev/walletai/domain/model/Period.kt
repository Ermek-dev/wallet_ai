package kz.abdiyev.walletai.domain.model

enum class Period(val dbValue: String, val labelRu: String) {
    WEEK("WEEK", "Неделя"),
    MONTH("MONTH", "Месяц"),
    THREE_MONTHS("THREE_MONTHS", "3 Месяца"),
    ALL("ALL", "Все");

    companion object {
        fun fromDb(value: String): Period =
            entries.find { it.dbValue == value } ?: MONTH
    }
}
