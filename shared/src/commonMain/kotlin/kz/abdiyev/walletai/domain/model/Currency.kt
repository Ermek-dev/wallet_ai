package kz.abdiyev.walletai.domain.model

enum class Currency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val minorUnits: Int = 100
) {
    KZT("KZT", "₸", "Тенге"),
    RUB("RUB", "₽", "Рубль"),
    USD("USD", "$", "Доллар"),
    EUR("EUR", "€", "Евро"),
    KGS("KGS", "с", "Сом"),
    UZS("UZS", "so'm", "Сум");

    companion object {
        fun fromCode(code: String): Currency =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: KZT
    }
}
