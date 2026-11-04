package kz.abdiyev.walletai.domain.usecase

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
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
        defaultDateMillis: Long,
        accountLabel: String = ""
    ): ImportStatementResult {
        val parseResult = parser.parse(
            text = text,
            categories = categories,
            trainedModel = trainedModel,
            defaultCurrency = currency,
            defaultDate = defaultDateMillis
        )
        if (parseResult.lines.isEmpty()) return ImportStatementResult.Empty

        val scope = "${parseResult.format.name}:${DescriptionNormalizer.normalize(accountLabel)}"
        val existing = transactions.getAll()
        val confirmedByMerchant = existing.filter { !it.isAutoCategorized && !it.needsReview }
            .groupBy { it.merchantKey }
        val existingKeys = existing.map { it.sourceKey ?: ImportIdentity.key(
            scope, it.date, it.money, it.type, it.description
        ) }.groupingBy { it }.eachCount().toMutableMap()
        val memories = parseResult.lines.distinctBy { ImportIdentity.merchantKey(scope, it.description) }
            .associate { line ->
                val key = ImportIdentity.merchantKey(scope, line.description)
                key to (merchantRules.findMemory(key)
                    ?: merchantRules.findMemory(DescriptionNormalizer.normalize(line.description)))
            }

        val previews = parseResult.lines.map { line ->
            var categoryId = line.predictedCategoryId
            val merchantKey = ImportIdentity.merchantKey(scope, line.description)
            val memory = memories[merchantKey]
                ?.takeIf { rule -> categories.any { it.id == rule.categoryId && it.type == line.type } }
            val confirmed = confirmedByMerchant[merchantKey].orEmpty().filter { it.type == line.type }
            val historyCategory = confirmed.groupingBy { it.categoryId }.eachCount()
                .maxByOrNull { it.value }
                ?.takeIf { it.value >= 3 && it.value * 100 >= confirmed.size * 90 }
                ?.key?.takeIf { id -> categories.any { it.id == id && it.type == line.type } }
            historyCategory?.let { categoryId = it }
            memory?.let { categoryId = it.categoryId }
            val key = ImportIdentity.key(scope, line.date, line.money, line.type, line.description)
            val remaining = existingKeys[key] ?: 0
            val duplicate = remaining > 0
            if (duplicate) existingKeys[key] = remaining - 1
            val knownMerchant = kz.abdiyev.walletai.ml.statement.MerchantDictionary
                .categoryNameFor(line.description, line.rawOperation)
                ?.let { name -> categories.any { it.name == name && it.type == line.type } } == true
            TransactionPreview(
                id = line.id,
                money = line.money,
                type = line.type,
                description = memory?.displayName ?: line.description,
                predictedCategoryId = categoryId,
                date = line.date,
                isTransfer = line.isTransfer,
                isDuplicate = duplicate,
                sourceKey = key,
                originalDescription = line.description,
                merchantKey = merchantKey,
                needsReview = memory == null && historyCategory == null && !knownMerchant && !line.isTransfer
            )
        }
        return ImportStatementResult.Success(previews, parseResult.format)
    }

}
