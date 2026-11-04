package kz.abdiyev.walletai.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.Settings
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionPreview
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.domain.repository.ImportRepository
import kz.abdiyev.walletai.domain.usecase.ImportStatementResult
import kz.abdiyev.walletai.domain.usecase.ImportStatementUseCase
import kz.abdiyev.walletai.ml.TransactionClassifier
import kz.abdiyev.walletai.ml.statement.StatementFormat
import kz.abdiyev.walletai.util.Clock
import kz.abdiyev.walletai.util.PdfTextExtractor
import kz.abdiyev.walletai.util.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
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
    val format: StatementFormat,
    val reviewCount: Int = 0
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
    private val classifier: TransactionClassifier,
    private val importStatementUseCase: ImportStatementUseCase,
    private val importRepository: ImportRepository,
    private val clock: Clock = SystemClock,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    val categories = categoryRepository.categories.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Lazily, Settings())
    val importHistory = importRepository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _batchTransactions = MutableStateFlow<List<TransactionData>>(emptyList())
    val batchTransactions = _batchTransactions.asStateFlow()
    private val _accountLabel = MutableStateFlow("")
    val accountLabel = _accountLabel.asStateFlow()
    fun setAccountLabel(value: String) {
        if (_uiState.value is ImportUiState.Idle) _accountLabel.value = value
    }

    private val _parsedPreviews = MutableStateFlow<List<TransactionPreview>>(emptyList())
    val parsedPreviews: StateFlow<List<TransactionPreview>> = _parsedPreviews.asStateFlow()

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val _skipTransfers = MutableStateFlow(true)
    val skipTransfers: StateFlow<Boolean> = _skipTransfers.asStateFlow()

    private var trainedModel: TransactionClassifier.TrainedModel? = null
    private var retryPreview: ImportUiState.Preview? = null

    init {
        viewModelScope.launch { refreshModel() }
    }

    private suspend fun refreshModel() {
        val txs = transactionRepository.getAll()
        val cats = categoryRepository.getAll()
        trainedModel = withContext(workerDispatcher) {
            classifier.train(txs, cats).takeIf { it.categoryPriors.isNotEmpty() }
        }
    }

    fun parseBankStatement(text: String) {
        if (text.isBlank()) return
        if (_uiState.value is ImportUiState.Saving || _uiState.value is ImportUiState.Parsing) return
        _parsedPreviews.value = emptyList()
        _uiState.value = ImportUiState.Parsing("Текст выписки")
        viewModelScope.launch {
            _uiState.value = ImportUiState.Parsing("Текст выписки")
            parseStatementText(text, "Текст выписки")
        }
    }

    fun parsePdf(context: Context, uri: Uri, fileName: String? = null) {
        if (_uiState.value is ImportUiState.Saving || _uiState.value is ImportUiState.Parsing) return
        _parsedPreviews.value = emptyList()
        _uiState.value = ImportUiState.Parsing(fileName ?: "PDF-файл")
        viewModelScope.launch {
            try {
                val label = fileName ?: withContext(Dispatchers.IO) {
                    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }
                } ?: "PDF-файл"
                _uiState.value = ImportUiState.Parsing(label)
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
            if (categoryRepository.getAll().isEmpty()) {
                categoryRepository.ensureDefaultCategories()
            }
            val cats = categoryRepository.getAll()
            if (cats.isEmpty()) {
                _parsedPreviews.value = emptyList()
                _uiState.value = ImportUiState.Error("Не удалось подготовить категории для импорта. Повторите попытку.")
                return
            }
            refreshModel()
            val currency = Currency.fromCode(settings.value.currencyCode)
            when (
                val result = withContext(workerDispatcher) { importStatementUseCase.execute(
                    text = text,
                    categories = cats,
                    currency = currency,
                    trainedModel = trainedModel,
                    defaultDateMillis = clock.nowMillis(),
                    accountLabel = _accountLabel.value
                ) }
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
            format = format,
            reviewCount = previews.count { it.needsReview && !it.isDuplicate }
        )
    }

    fun setSkipTransfers(enabled: Boolean) {
        _skipTransfers.value = enabled
    }

    fun updatePreviewCategory(previewId: Long, categoryId: Long) {
        if (_uiState.value !is ImportUiState.Preview) return
        if (_parsedPreviews.value.none { it.id == previewId }) return
        _parsedPreviews.value = _parsedPreviews.value.map {
            if (it.id == previewId) it.copy(predictedCategoryId = categoryId, categoryConfirmed = true,
                needsReview = false, rememberMerchant = false) else it
        }
        refreshSummary()
    }

    fun updatePreviewEdited(
        previewId: Long,
        money: Money,
        type: TransactionType,
        categoryId: Long,
        description: String,
        date: Long
    ) {
        if (_uiState.value !is ImportUiState.Preview) return
        _parsedPreviews.value = _parsedPreviews.value.map {
            if (it.id == previewId) {
                it.copy(
                    money = money,
                    type = type,
                    predictedCategoryId = categoryId,
                    description = description,
                    date = date,
                    categoryConfirmed = true,
                    needsReview = false,
                    rememberMerchant = false
                )
            } else {
                it
            }
        }
        refreshSummary()
    }

    private fun refreshSummary() {
        val current = _uiState.value
        if (current is ImportUiState.Preview) {
            _uiState.value = ImportUiState.Preview(
                summary = buildSummary(_parsedPreviews.value, current.summary.format),
                sourceLabel = current.sourceLabel
            )
        }
    }

    fun commitPreviews() {
        val state = _uiState.value as? ImportUiState.Preview ?: return
        val snapshot = _parsedPreviews.value
        val skip = _skipTransfers.value
        _uiState.value = ImportUiState.Saving
        viewModelScope.launch {
            try {
                val count = importRepository.save(
                    listOf(state.sourceLabel, _accountLabel.value).filter { it.isNotBlank() }.joinToString(" · "),
                    clock.nowMillis(), snapshot, skip
                )
                _parsedPreviews.value = emptyList()
                _uiState.value = ImportUiState.Success(count)
            } catch (e: Exception) {
                Timber.e(e, "Failed to commit previews")
                retryPreview = state
                _uiState.value = ImportUiState.Error("Не удалось сохранить операции")
            }
        }
    }

    fun committablePreviews(): List<TransactionPreview> {
        val skip = _skipTransfers.value
        return _parsedPreviews.value.filter { preview ->
            (!preview.isDuplicate || preview.forceImport) && (!skip || !preview.isTransfer)
        }
    }

    fun setForceImport(previewId: Long, enabled: Boolean) {
        if (_uiState.value !is ImportUiState.Preview) return
        _parsedPreviews.value = _parsedPreviews.value.map {
            if (it.id == previewId) it.copy(forceImport = enabled) else it
        }
    }

    fun rememberMerchant(previewId: Long) {
        if (_uiState.value !is ImportUiState.Preview) return
        val source = _parsedPreviews.value.find { it.id == previewId } ?: return
        _parsedPreviews.value = _parsedPreviews.value.map {
            if (it.merchantKey == source.merchantKey && it.type == source.type && !it.isDuplicate) it.copy(
                description = source.description, predictedCategoryId = source.predictedCategoryId,
                needsReview = false, categoryConfirmed = true, rememberMerchant = true
            ) else it
        }
        refreshSummary()
    }

    fun showBatch(batchId: Long) {
        _batchTransactions.value = emptyList()
        viewModelScope.launch {
            try {
                _batchTransactions.value = importRepository.transactions(batchId)
            } catch (e: Exception) {
                _uiState.value = ImportUiState.Error("Не удалось загрузить операции импорта")
            }
        }
    }

    fun undoImport(batchId: Long) {
        if (_uiState.value is ImportUiState.Saving || _uiState.value is ImportUiState.Parsing) return
        _uiState.value = ImportUiState.Saving
        viewModelScope.launch {
            try {
                importRepository.undo(batchId)
                _batchTransactions.value = emptyList()
                _uiState.value = ImportUiState.Idle
                clearPreviews()
            } catch (e: Exception) {
                _uiState.value = ImportUiState.Error("Не удалось отменить импорт")
            }
        }
    }

    fun clearPreviews() {
        if (_uiState.value is ImportUiState.Saving) return
        retryPreview = null
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
            val retry = retryPreview
            retryPreview = null
            if (retry == null) _parsedPreviews.value = emptyList()
            _uiState.value = retry ?: ImportUiState.Idle
        }
    }
}
