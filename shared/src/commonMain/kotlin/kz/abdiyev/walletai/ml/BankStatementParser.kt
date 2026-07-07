package kz.abdiyev.walletai.ml

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType

data class ParsedStatementLine(
    val id: Long,
    val money: Money,
    val type: TransactionType,
    val description: String,
    val predictedCategoryId: Long,
    val date: Long
)

class BankStatementParser(
    private val classifier: TransactionClassifier = TransactionClassifier()
) {
    private val amountRegex = Regex(
        "(?<=^|\\s|[^0-9])([0-9]+[.,]?[0-9]{0,2})(?=\\s|р|руб|руб\\.|usd|eur|тг|тенге|₸|tg|kzt|\\s*\$|[^0-9])",
        RegexOption.IGNORE_CASE
    )

    fun parse(
        text: String,
        categories: List<CategoryData>,
        trainedModel: TransactionClassifier.TrainedModel?,
        defaultCurrency: Currency = Currency.KZT,
        defaultDate: Long = System.currentTimeMillis()
    ): List<ParsedStatementLine> {
        if (text.isBlank() || categories.isEmpty()) return emptyList()

        val previewList = mutableListOf<ParsedStatementLine>()
        var tempIdCounter = 1L

        for (line in text.split("\n")) {
            val cleanLine = line.trim()
            if (cleanLine.isBlank()) continue

            val amountMatch = amountRegex.findAll(cleanLine).lastOrNull()
            var majorAmount = 0.0
            if (amountMatch != null) {
                majorAmount = amountMatch.value.replace(",", ".").toDoubleOrNull() ?: 0.0
            }
            if (majorAmount == 0.0) {
                val simpleDigits = Regex("\\d+").find(cleanLine)
                majorAmount = simpleDigits?.value?.toDoubleOrNull() ?: 0.0
            }
            if (majorAmount == 0.0) continue

            var description = cleanLine
                .replace(amountMatch?.value ?: "", "")
                .replace(Regex("(?i)(руб|руб\\.|р\\.|р|usd|eur|тг|тенге|₸|tg|kzt|\\+|-|=)"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (description.isBlank()) description = "Транзакция по выписке"

            val isIncome = cleanLine.contains("+") ||
                cleanLine.lowercase().contains("зарплата") ||
                cleanLine.lowercase().contains("зачисление") ||
                cleanLine.lowercase().contains("аванс") ||
                cleanLine.lowercase().contains("пополнение") ||
                cleanLine.lowercase().contains("postupil") ||
                cleanLine.lowercase().contains("kaspi red")

            val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
            val predictedCatId = if (trainedModel != null) {
                classifier.classify(description, trainedModel, categories)
            } else {
                categories.first().id
            }

            previewList.add(
                ParsedStatementLine(
                    id = tempIdCounter++,
                    money = Money.fromMajor(majorAmount, defaultCurrency),
                    type = type,
                    description = description,
                    predictedCategoryId = predictedCatId,
                    date = defaultDate
                )
            )
        }
        return previewList
    }
}
