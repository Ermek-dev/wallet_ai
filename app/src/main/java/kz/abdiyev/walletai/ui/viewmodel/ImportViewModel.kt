package kz.abdiyev.walletai.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.Settings
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionPreview
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.MerchantRuleRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.domain.usecase.ImportStatementResult
import kz.abdiyev.walletai.domain.usecase.ImportStatementUseCase
import kz.abdiyev.walletai.domain.usecase.SaveTransactionResult
import kz.abdiyev.walletai.domain.usecase.SaveTransactionUseCase
import kz.abdiyev.walletai.ml.TransactionClassifier
import kz.abdiyev.walletai.ml.statement.StatementFormat
import kz.abdiyev.walletai.util.CalendarMillis
import kz.abdiyev.walletai.util.Clock
import kz.abdiyev.walletai.util.PdfTextExtractor
import kz.abdiyev.walletai.util.SystemClock
import kz.abdiyev.walletai.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data class Parsing(val sourceLabel: String) : ImportUiState
    data class Preview(val summary: ImportSummary, val sourceLabel: String) : ImportUiState
    data object Saving : ImportUiState
    data class Success(val savedCount: Int) : ImportUiState
    data class Error(val message: String) : ImportUiState
}

class ImportViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val merchantRuleRepository: MerchantRuleRepository,
    private val classifier: TransactionClassifier,
    private val importStatementUseCase: ImportStatementUseCase,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val clock: Clock = SystemClock
) : ViewModel() {

    val categories = categoryRepository.categories.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Lazily, Settings())

    private val _parsedPreviews = MutableStateFlow<List<TransactionPreview>>(emptyList())
    val parsedPreviews: StateFlow<List<TransactionPreview>> = _parsedPreviews.asStateFlow()

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val _skipTransfers = MutableStateFlow(true)
    val skipTransfers: StateFlow<Boolean> = _skipTransfers.asStateFlow()

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
        viewModelScope.launch {
            _uiState.value = ImportUiState.Parsing("Текст выписки")
            parseStatementText(text, "Текст выписки")
        }
    }

    fun parsePdf(context: Context, uri: Uri, fileName: String? = null) {
        viewModelScope.launch {
            val label = fileName ?: uri.lastPathSegment ?: "PDF-файл"
            _uiState.value = ImportUiState.Parsing(label)
            try {
                val text = withContext(Dispatchers.IO) {
                    PdfTextExtractor.extractText(context, uri)
                }
                when {
                    text.isBlank() -> {
                        _parsedPreviews.value = emptyList()
                        _uiState.value = ImportUiState.Error(
                            "PDF не содержит текстового слоя. Сканы без OCR пока не поддерживаются."
                        )
                    }
                    else -> parseStatementText(text, label)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse PDF")
                _parsedPreviews.value = emptyList()
                _uiState.value = ImportUiState.Error("Не удалось прочитать PDF")
            }
        }
    }

    private suspend fun parseStatementText(text: String, sourceLabel: String) {
        try {
            if (trainedModel == null) refreshModel()
            val currency = Currency.fromCode(settings.value.currencyCode)
            val cats = categoryRepository.getAll()
            when (
                val result = importStatementUseCase.execute(
                    text = text,
                    categories = cats,
                    currency = currency,
                    trainedModel = trainedModel,
                    defaultDateMillis = clock.nowMillis()
                )
            ) {
                ImportStatementResult.Empty -> {
                    _parsedPreviews.value = emptyList()
                    _uiState.value = ImportUiState.Error("Операций не найдено")
                }
                is ImportStatementResult.Success -> {
                    _parsedPreviews.value = result.previews
                    val summary = buildSummary(result.previews, result.format)
                    _uiState.value = ImportUiState.Preview(summary, sourceLabel)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse bank statement")
            _parsedPreviews.value = emptyList()
            _uiState.value = ImportUiState.Error("Ошибка разбора выписки")
        }
    }

    private fun buildSummary(previews: List<TransactionPreview>, format: StatementFormat): ImportSummary {
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
        val preview = _parsedPreviews.value.find { it.id == previewId } ?: return
        _parsedPreviews.value = _parsedPreviews.value.map {
            if (it.id == previewId) it.copy(predictedCategoryId = categoryId) else it
        }
        viewModelScope.launch {
            merchantRuleRepository.saveRule(preview.description, categoryId)
        }
    }

    fun updatePreviewEdited(
        previewId: Long,
        money: Money,
        type: TransactionType,
        categoryId: Long,
        description: String,
        date: Long
    ) {
        _parsedPreviews.value = _parsedPreviews.value.map {
            if (it.id == previewId) {
                it.copy(
                    money = money,
                    type = type,
                    predictedCategoryId = categoryId,
                    description = description,
                    date = date
                )
            } else {
                it
            }
        }
        viewModelScope.launch {
            merchantRuleRepository.saveRule(description, categoryId)
        }
        val current = _uiState.value
        if (current is ImportUiState.Preview) {
            _uiState.value = ImportUiState.Preview(
                summary = buildSummary(_parsedPreviews.value, current.summary.format),
                sourceLabel = current.sourceLabel
            )
        }
    }

    fun commitPreviews() {
        val toSave = committablePreviews()
        if (toSave.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = ImportUiState.Saving
            try {
                val entities = toSave.map {
                    val offset = TimeUtils.localOffsetMillis(it.date)
                    TransactionData(
                        money = it.money,
                        type = it.type,
                        categoryId = it.predictedCategoryId,
                        description = it.description,
                        date = CalendarMillis.toLocalStartOfDay(it.date, offset),
                        isAutoCategorized = true
                    )
                }
                when (val result = saveTransactionUseCase.executeBatch(entities)) {
                    is SaveTransactionResult.BatchSuccess -> {
                        _parsedPreviews.value = emptyList()
                        refreshModel()
                        _uiState.value = ImportUiState.Success(result.count)
                    }
                    SaveTransactionResult.InvalidAmount,
                    is SaveTransactionResult.Success -> {
                        _uiState.value = ImportUiState.Error("Не удалось сохранить операции")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to commit previews")
                _uiState.value = ImportUiState.Error("Не удалось сохранить операции")
            }
        }
    }

    fun committablePreviews(): List<TransactionPreview> {
        val skip = _skipTransfers.value
        return _parsedPreviews.value.filter { preview ->
            !preview.isDuplicate && (!skip || !preview.isTransfer)
        }
    }

    fun clearPreviews() {
        _parsedPreviews.value = emptyList()
        _uiState.value = ImportUiState.Idle
    }

    fun acknowledgeSuccess() {
        if (_uiState.value is ImportUiState.Success) {
            _uiState.value = ImportUiState.Idle
        }
    }

    fun dismissError() {
        if (_uiState.value is ImportUiState.Error) {
            _uiState.value = ImportUiState.Idle
        }
    }
}
