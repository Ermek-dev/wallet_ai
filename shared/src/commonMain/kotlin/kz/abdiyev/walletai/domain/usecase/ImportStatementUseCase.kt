package kz.abdiyev.walletai.domain.usecase

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionPreview
import kz.abdiyev.walletai.domain.port.MerchantRuleStore
import kz.abdiyev.walletai.domain.port.TransactionStore
import kz.abdiyev.walletai.ml.BankStatementParser
import kz.abdiyev.walletai.ml.TransactionClassifier
import kz.abdiyev.walletai.ml.statement.StatementFormat
import kz.abdiyev.walletai.util.DescriptionNormalizer

sealed interface ImportStatementResult {
    data class Success(
        val previews: List<TransactionPreview>,
        val format: StatementFormat
    ) : ImportStatementResult

    data object Empty : ImportStatementResult
}

class ImportStatementUseCase(
    private val transactions: TransactionStore,
    private val merchantRules: MerchantRuleStore,
    private val parser: BankStatementParser
) {
    suspend fun execute(
        text: String,
        categories: List<CategoryData>,
        currency: Currency,
        trainedModel: TransactionClassifier.TrainedModel?,
        defaultDateMillis: Long
    ): ImportStatementResult {
        val parseResult = parser.parse(
            text = text,
            categories = categories,
            trainedModel = trainedModel,
            defaultCurrency = currency,
            defaultDate = defaultDateMillis
        )
        if (parseResult.lines.isEmpty()) return ImportStatementResult.Empty

        val existingKeys = transactions.getAll()
            .map { dedupKey(it.date, it.money.amountMinor, it.description) }
            .toSet()

        val previews = parseResult.lines.map { line ->
            var categoryId = line.predictedCategoryId
            merchantRules.findCategoryId(line.description)?.let { categoryId = it }
            val duplicate = dedupKey(line.date, line.money.amountMinor, line.description) in existingKeys
            TransactionPreview(
                id = line.id,
                money = line.money,
                type = line.type,
                description = line.description,
                predictedCategoryId = categoryId,
                date = line.date,
                isTransfer = line.isTransfer,
                isDuplicate = duplicate
            )
        }
        return ImportStatementResult.Success(previews, parseResult.format)
    }

    private fun dedupKey(date: Long, amountMinor: Long, description: String): Triple<Long, Long, String> =
        Triple(date, amountMinor, DescriptionNormalizer.normalize(description))
}
