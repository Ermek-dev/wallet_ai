package kz.abdiyev.walletai.ml

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionType

data class VoiceParseResult(
    val money: Money?,
    val type: TransactionType,
    val description: String,
    val dateMillis: Long? = null
)

class VoiceInputParser {

    private val expenseWords = setOf(
        "потратил", "потратила", "купил", "купила", "заплатил", "заплатила",
        "оплатил", "оплатила", "списание", "расход", "минус"
    )
    private val incomeWords = setOf(
        "получил", "получила", "пришло", "зарплата", "зарплату", "аванс", "доход", "плюс", "зачисление"
    )
    private val fillerWords = setOf(
        "в", "на", "за", "с", "из", "по", "и",
        "руб", "рубль", "рубля", "рублей", "тенге", "тг", "₸",
        "доллар", "доллара", "долларов", "евро"
    )

    private val unitWords = mapOf(
        "один" to 1L, "одна" to 1L, "одну" to 1L, "два" to 2L, "две" to 2L,
        "три" to 3L, "четыре" to 4L, "пять" to 5L, "шесть" to 6L,
        "семь" to 7L, "восемь" to 8L, "девять" to 9L
    )
    private val teenWords = mapOf(
        "десять" to 10L, "одиннадцать" to 11L, "двенадцать" to 12L, "тринадцать" to 13L,
        "четырнадцать" to 14L, "пятнадцать" to 15L, "шестнадцать" to 16L,
        "семнадцать" to 17L, "восемнадцать" to 18L, "девятнадцать" to 19L
    )
    private val tenWords = mapOf(
        "двадцать" to 20L, "тридцать" to 30L, "сорок" to 40L, "пятьдесят" to 50L,
        "шестьдесят" to 60L, "семьдесят" to 70L, "восемьдесят" to 80L, "девяносто" to 90L
    )
    private val hundredWords = mapOf(
        "сто" to 100L, "двести" to 200L, "триста" to 300L, "четыреста" to 400L,
        "пятьсот" to 500L, "шестьсот" to 600L, "семьсот" to 700L,
        "восемьсот" to 800L, "девятьсот" to 900L
    )
    private val thousandWords = setOf("тысяча", "тысячи", "тысяч", "тыща", "тыщи", "тыс")
    private val thousandSuffixes = setOf("k", "к", "тыс", "тысяч")

    fun parse(text: String, defaultCurrency: Currency = Currency.KZT, referenceMillis: Long = 0L): VoiceParseResult {
        val lower = text.lowercase().trim()
        val type = when {
            incomeWords.any { lower.contains(it) } -> TransactionType.INCOME
            expenseWords.any { lower.contains(it) } -> TransactionType.EXPENSE
            else -> TransactionType.EXPENSE
        }

        // Detach currency words glued to digits ("3400тг" -> "3400 тг")
        val normalized = lower.replace(Regex("(?<=[0-9])(тг|тенге|руб|₸)"), " $1")
        // The thousand suffix must be glued to the digits, otherwise "250 к обеду" would become 250000
        val amountRegex = Regex("([0-9]+(?:[.,][0-9]{1,2})?)(k|к|тыс|тысяч)?(?![а-яёa-z0-9])")
        val match = amountRegex.find(normalized)

        var numberWordsUsed: Set<String> = emptySet()
        val money = when {
            match != null -> {
                var value = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
                if (match.groupValues[2] in thousandSuffixes) value *= 1000
                Money.fromMajor(value, defaultCurrency)
            }
            else -> extractWordNumber(normalized)?.let { (value, used) ->
                numberWordsUsed = used
                Money.fromMajor(value.toDouble(), defaultCurrency)
            }
        }

        val description = buildDescription(normalized, match?.value, numberWordsUsed)
        val dateMillis = parseRelativeDate(lower, referenceMillis)

        return VoiceParseResult(
            money = money,
            type = type,
            description = if (description.isBlank()) text.trim() else description,
            dateMillis = dateMillis
        )
    }

    private fun parseRelativeDate(lower: String, referenceMillis: Long): Long? {
        if (referenceMillis <= 0L) return null
        val dayMs = 86_400_000L
        return when {
            lower.contains("вчера") -> referenceMillis - dayMs
            lower.contains("сегодня") -> referenceMillis
            else -> null
        }
    }

    /**
     * Parses Russian spelled-out numbers like "три тысячи четыреста" (3400)
     * or "сто пятьдесят тысяч" (150000). Returns the value and the tokens consumed.
     */
    private fun extractWordNumber(text: String): Pair<Long, Set<String>>? {
        val tokens = text.split(Regex("\\s+"))
        var total = 0L
        var current = 0L
        var found = false
        val used = mutableSetOf<String>()

        for (token in tokens) {
            val value = hundredWords[token] ?: tenWords[token] ?: teenWords[token] ?: unitWords[token]
            when {
                value != null -> {
                    current += value
                    found = true
                    used += token
                }
                token in thousandWords -> {
                    total += (if (current == 0L) 1L else current) * 1000L
                    current = 0L
                    found = true
                    used += token
                }
            }
        }
        total += current
        return if (found && total > 0) total to used else null
    }

    private fun buildDescription(lower: String, amountMatch: String?, numberWords: Set<String>): String {
        val amountTokens = amountMatch?.trim()?.split(Regex("\\s+"))?.toSet() ?: emptySet()
        return lower
            .split(Regex("\\s+"))
            .filter { token ->
                token.isNotBlank() &&
                    token !in expenseWords &&
                    token !in incomeWords &&
                    token !in fillerWords &&
                    token !in numberWords &&
                    token !in amountTokens &&
                    !token.matches(Regex("[0-9.,]+[kк]?"))
            }
            .joinToString(" ")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
