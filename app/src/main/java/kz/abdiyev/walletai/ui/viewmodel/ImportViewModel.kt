package kz.abdiyev.walletai.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.Settings
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.MerchantRuleRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.ml.BankStatementParser
import kz.abdiyev.walletai.ml.TransactionClassifier
import kz.abdiyev.walletai.ml.statement.StatementFormat
import kz.abdiyev.walletai.util.PdfTextExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

data class ImportSummary(
    val totalRecognized: Int,
    val expenseCount: Int,
    val expenseTotal: Money,
    val incomeCount: Int,
    val incomeTotal: Money,
    val transferCount: Int,
    val duplicateCount: Int,
    val format: StatementFormat
)

class ImportViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val merchantRuleRepository: MerchantRuleRepository,
    private val classifier: TransactionClassifier,
    private val bankStatementParser: BankStatementParser
) : ViewModel() {

    val categories = categoryRepository.categories.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Lazily, Settings())

    private val _parsedPreviews = MutableStateFlow<List<ParsedPreview>>(emptyList())
    val parsedPreviews: StateFlow<List<ParsedPreview>> = _parsedPreviews.asStateFlow()

    private val _importSummary = MutableStateFlow<ImportSummary?>(null)
    val importSummary: StateFlow<ImportSummary?> = _importSummary.asStateFlow()

    private val _skipTransfers = MutableStateFlow(true)
    val skipTransfers: StateFlow<Boolean> = _skipTransfers.asStateFlow()

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage.asStateFlow()

    private var trainedModel: TransactionClassifier.TrainedModel? = null

    init {
        viewModelScope.launch { refreshModel() }
    }

    private suspend fun refreshModel() {
        val txs = transactionRepository.getAll()
        val cats = categoryRepository.getAll()
        if (txs.isNotEmpty() && cats.isNotEmpty()) {
            trainedModel = classifier.train(txs, cats)
        }
    }

    fun parseBankStatement(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { parseStatementText(text) }
    }

    fun parsePdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val text = PdfTextExtractor.extractText(context, uri)
                if (text.isBlank()) {
                    _importMessage.value = "PDF не содержит текста для разбора"
                } else {
                    parseStatementText(text)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse PDF")
                _importMessage.value = "Не удалось прочитать PDF"
            }
        }
    }

    private suspend fun parseStatementText(text: String) {
        try {
            if (trainedModel == null) refreshModel()
            val currency = Currency.fromCode(settings.value.currencyCode)
            val cats = categoryRepository.getAll()
            val parseResult = bankStatementParser.parse(text, cats, trainedModel, currency)
            if (parseResult.lines.isEmpty()) {
                _importMessage.value = "Не удалось распознать операции в тексте"
                _parsedPreviews.value = emptyList()
                _importSummary.value = null
                return
            }

            val existingKeys = transactionRepository.getAll()
                .map { dedupKey(it.date, it.money.amountMinor, it.description) }
                .toSet()

            val previews = parseResult.lines.map { line ->
                var categoryId = line.predictedCategoryId
                merchantRuleRepository.findCategoryId(line.description)?.let { categoryId = it }

                val duplicate = dedupKey(line.date, line.money.amountMinor, line.description) in existingKeys
                ParsedPreview(
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

            _parsedPreviews.value = previews
            _importSummary.value = buildSummary(previews, parseResult.format)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse bank statement")
            _importMessage.value = "Ошибка разбора выписки"
        }
    }

    private fun buildSummary(previews: List<ParsedPreview>, format: StatementFormat): ImportSummary {
        val expenses = previews.filter { it.type == TransactionType.EXPENSE && !it.isTransfer }
        val incomes = previews.filter { it.type == TransactionType.INCOME && !it.isTransfer }
        return ImportSummary(
            totalRecognized = previews.size,
            expenseCount = expenses.size,
            expenseTotal = expenses.fold(Money.fromMajor(0.0, Currency.KZT)) { acc, p -> acc + p.money },
            incomeCount = incomes.size,
            incomeTotal = incomes.fold(Money.fromMajor(0.0, Currency.KZT)) { acc, p -> acc + p.money },
            transferCount = previews.count { it.isTransfer },
            duplicateCount = previews.count { it.isDuplicate },
            format = format
        )
    }

    fun setSkipTransfers(enabled: Boolean) {
        _skipTransfers.value = enabled
    }

    fun updatePreviewCategory(previewId: Long, categoryId: Long) {
        _parsedPreviews.value = _parsedPreviews.value.map {
            if (it.id == previewId) it.copy(predictedCategoryId = categoryId) else it
        }
    }

    fun commitPreviews() {
        val toSave = committablePreviews()
        if (toSave.isEmpty()) return
        viewModelScope.launch {
            try {
                transactionRepository.insertAll(toSave.map {
                    TransactionData(
                        money = it.money,
                        type = it.type,
                        categoryId = it.predictedCategoryId,
                        description = it.description,
                        date = it.date,
                        isAutoCategorized = true
                    )
                })
                _parsedPreviews.value = emptyList()
                _importSummary.value = null
                refreshModel()
                _importMessage.value = "Сохранено ${toSave.size} операций"
            } catch (e: Exception) {
                Timber.e(e, "Failed to commit previews")
                _importMessage.value = "Не удалось сохранить операции"
            }
        }
    }

    fun committablePreviews(): List<ParsedPreview> {
        val skip = _skipTransfers.value
        return _parsedPreviews.value.filter { preview ->
            !preview.isDuplicate && (!skip || !preview.isTransfer)
        }
    }

    fun clearPreviews() {
        _parsedPreviews.value = emptyList()
        _importSummary.value = null
    }

    fun clearImportMessage() {
        _importMessage.value = null
    }

    private fun dedupKey(date: Long, amountMinor: Long, description: String): Triple<Long, Long, String> =
        Triple(date, amountMinor, description.trim())
}
