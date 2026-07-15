package kz.abdiyev.walletai.ml.statement

object MerchantDictionary {
    private val keywordToCategory = listOf(
        "MAGNUM" to "Продукты",
        "SUPERMARKET" to "Продукты",
        "ЖИВАЯ ВОДА" to "Продукты",
        "SELI POYELI" to "Кофе и Рестораны",
        "KFC" to "Кофе и Рестораны",
        "STARBUCKS" to "Кофе и Рестораны",
        "UMMA FOOD" to "Кофе и Рестораны",
        "BAURSAK" to "Кофе и Рестораны",
        "KAZIKHAN" to "Кофе и Рестораны",
        "GLOVO" to "Кофе и Рестораны",
        "WHOOSH" to "Транспорт",
        "ОНАЙ" to "Транспорт",
        "ONAY" to "Транспорт",
        "BEELINE" to "Подписки",
        "ALTEL" to "Подписки",
        "TELE2" to "Подписки",
        "ТЕЛЕ 2" to "Подписки",
        "ЗАРПЛАТА" to "Зарплата"
    )

    fun categoryNameFor(description: String, rawOperation: String): String? {
        val haystack = "$description $rawOperation".uppercase()
        return keywordToCategory.firstOrNull { (keyword, _) ->
            haystack.contains(keyword)
        }?.second
    }
}
