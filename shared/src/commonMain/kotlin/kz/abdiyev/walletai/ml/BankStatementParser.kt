package kz.abdiyev.walletai.ml

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.ml.statement.FreedomStatementParser
import kz.abdiyev.walletai.ml.statement.GenericStatementParser
import kz.abdiyev.walletai.ml.statement.KaspiStatementParser
import kz.abdiyev.walletai.ml.statement.StatementCategorizer
import kz.abdiyev.walletai.ml.statement.StatementFormat
import kz.abdiyev.walletai.ml.statement.StatementFormatDetector
import kz.abdiyev.walletai.ml.statement.StatementParseContext

data class ParsedStatementLine(
    val id: Long,
    val money: kz.abdiyev.walletai.domain.model.Money,
    val type: kz.abdiyev.walletai.domain.model.TransactionType,
    val description: String,
    val predictedCategoryId: Long,
    val date: Long,
    val rawOperation: String = "",
    val isTransfer: Boolean = false
)

data class BankStatementParseResult(
    val format: StatementFormat,
    val lines: List<ParsedStatementLine>
)

class BankStatementParser(
    private val classifier: TransactionClassifier = TransactionClassifier(),
    private val detector: StatementFormatDetector = StatementFormatDetector(),
    private val kaspiParser: KaspiStatementParser = KaspiStatementParser(),
    private val freedomParser: FreedomStatementParser = FreedomStatementParser(),
    private val genericParser: GenericStatementParser = GenericStatementParser(),
    private val categorizer: StatementCategorizer = StatementCategorizer(classifier)
) {
    fun parse(
        text: String,
        categories: List<CategoryData>,
        trainedModel: TransactionClassifier.TrainedModel?,
        defaultCurrency: Currency = Currency.KZT,
        defaultDate: Long = System.currentTimeMillis()
    ): BankStatementParseResult {
        if (text.isBlank() || categories.isEmpty()) {
            return BankStatementParseResult(StatementFormat.GENERIC, emptyList())
        }

        val format = detector.detect(text)
        val ctx = StatementParseContext(defaultCurrency = defaultCurrency, defaultDate = defaultDate)
        val rawLines = when (format) {
            StatementFormat.KASPI -> kaspiParser.parse(text, ctx)
            StatementFormat.FREEDOM -> freedomParser.parse(text, ctx)
            StatementFormat.GENERIC -> genericParser.parse(text, ctx)
        }

        val transfersCategoryId = categories.find { it.name == "Переводы" }?.id
        var tempId = 1L
        val lines = rawLines.map { raw ->
            ParsedStatementLine(
                id = tempId++,
                money = raw.money,
                type = raw.type,
                description = raw.description,
                predictedCategoryId = categorizer.categoryIdFor(
                    raw = raw,
                    categories = categories,
                    trainedModel = trainedModel,
                    transfersCategoryId = transfersCategoryId
                ),
                date = raw.date,
                rawOperation = raw.rawOperation,
                isTransfer = raw.isTransfer
            )
        }
        return BankStatementParseResult(format, lines)
    }
}
