package kz.abdiyev.walletai.ml.statement

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionType
class GenericStatementParser : StatementParser {
    private val amountRegex = Regex(
        "(?<=^|\\s|[^0-9])([0-9]+[.,]?[0-9]{0,2})(?=\\s|р|руб|руб\\.|usd|eur|тг|тенге|₸|tg|kzt|\\s*\$|[^0-9])",
        RegexOption.IGNORE_CASE
    )

    override fun parse(text: String, ctx: StatementParseContext): List<RawStatementLine> {
        if (text.isBlank()) return emptyList()

        val result = mutableListOf<RawStatementLine>()
        for (line in text.split("\n")) {
            val cleanLine = line.trim()
            if (cleanLine.isBlank()) continue

            val amountMatch = amountRegex.findAll(cleanLine).lastOrNull()
            var majorAmount = amountMatch?.value?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
            if (majorAmount == 0.0) {
                majorAmount = Regex("\\d+").find(cleanLine)?.value?.toDoubleOrNull() ?: 0.0
            }
            if (majorAmount == 0.0) continue

            var description = cleanLine
                .replace(amountMatch?.value ?: "", "")
                .replace(Regex("(?i)(руб|руб\\.|р\\.|р|usd|eur|тг|тенге|₸|tg|kzt|\\+|-|=)"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (description.isBlank()) description = "Транзакция по выписке"

            val isIncome = cleanLine.contains("+") ||
                description.lowercase().contains("зарплата") ||
                description.lowercase().contains("зачисление") ||
                description.lowercase().contains("аванс") ||
                description.lowercase().contains("пополнение")

            val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
            result += RawStatementLine(
                money = Money.fromMajor(majorAmount, ctx.defaultCurrency),
                type = type,
                description = StatementDescriptionSanitizer.clean(description),
                rawOperation = if (type == TransactionType.INCOME) "Пополнение" else "Покупка",
                date = ctx.defaultDate,
                isTransfer = false
            )
        }
        return result
    }
}
