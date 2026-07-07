package kz.abdiyev.walletai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.MerchantRuleRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.ml.BankStatementParser
import kz.abdiyev.walletai.ml.ParsedStatementLine
import kz.abdiyev.walletai.ml.TransactionClassifier
import kz.abdiyev.walletai.ml.VoiceInputParser
import kz.abdiyev.walletai.util.MockDataProvider
import kz.abdiyev.walletai.util.PeriodFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

data class ParsedPreview(
    val id: Long,
    val money: Money,
    val type: TransactionType,
    val description: String,
    val predictedCategoryId: Long,
    val date: Long
)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val merchantRuleRepository: MerchantRuleRepository,
    private val classifier: TransactionClassifier,
    private val bankStatementParser: BankStatementParser,
    private val voiceInputParser: VoiceInputParser
) : ViewModel() {

    val categories = categoryRepository.categories.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val transactions = transactionRepository.transactions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val currencyCode = settingsRepository.currencyCode.stateIn(viewModelScope, SharingStarted.Lazily, Currency.KZT.code)
    val onboardingDone = settingsRepository.onboardingDone.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _selectedPeriod = MutableStateFlow(Period.MONTH)
    val selectedPeriod: StateFlow<Period> = _selectedPeriod.asStateFlow()

    val filteredTransactions: StateFlow<List<TransactionData>> = combine(transactions, selectedPeriod) { txs, period ->
        PeriodFilter.filter(txs, period, System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _parsedPreviews = MutableStateFlow<List<ParsedPreview>>(emptyList())
    val parsedPreviews: StateFlow<List<ParsedPreview>> = _parsedPreviews.asStateFlow()

    private val _voicePreview = MutableStateFlow<ParsedPreview?>(null)
    val voicePreview: StateFlow<ParsedPreview?> = _voicePreview.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _lastDeleted = MutableStateFlow<TransactionData?>(null)
    val lastDeleted: StateFlow<TransactionData?> = _lastDeleted.asStateFlow()

    private var trainedModel: TransactionClassifier.TrainedModel? = null

    private var lastUsedCategoryId: Long = 0L

    init {
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategories()
            trainModel()
        }
    }

    private suspend fun trainModel() {
        val txs = transactionRepository.getAll()
        val cats = categoryRepository.getAll()
        if (txs.isNotEmpty() && cats.isNotEmpty()) {
            trainedModel = classifier.train(txs, cats)
        }
    }

    fun setPeriod(period: Period) {
        _selectedPeriod.value = period
    }

    fun addTransaction(money: Money, type: TransactionType, categoryId: Long, description: String, date: Long) {
        Timber.d("addTransaction: %s %s cat=%d", money, type, categoryId)
        lastUsedCategoryId = categoryId
        viewModelScope.launch {
            try {
                transactionRepository.insert(
                    TransactionData(money = money, type = type, categoryId = categoryId, description = description, date = date)
                )
                trainModel()
            } catch (e: Exception) {
                Timber.e(e, "Failed to add transaction")
                _snackbarMessage.value = SNACKBAR_SAVE_ERROR
            }
        }
    }

    fun updateTransaction(transaction: TransactionData) {
        Timber.d("updateTransaction: id=%d", transaction.id)
        lastUsedCategoryId = transaction.categoryId
        viewModelScope.launch {
            try {
                transactionRepository.update(transaction)
                trainModel()
            } catch (e: Exception) {
                Timber.e(e, "Failed to update transaction")
                _snackbarMessage.value = SNACKBAR_SAVE_ERROR
            }
        }
    }

    fun deleteTransaction(transaction: TransactionData) {
        Timber.d("deleteTransaction: id=%d", transaction.id)
        viewModelScope.launch {
            try {
                _lastDeleted.value = transaction
                transactionRepository.delete(transaction)
                trainModel()
                _snackbarMessage.value = SNACKBAR_DELETED
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete transaction")
            }
        }
    }

    fun undoDelete() {
        val tx = _lastDeleted.value ?: return
        viewModelScope.launch {
            try {
                transactionRepository.insert(tx.copy(id = 0))
                _lastDeleted.value = null
                trainModel()
                _snackbarMessage.value = null
            } catch (e: Exception) {
                Timber.e(e, "Failed to undo delete")
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun parseBankStatement(text: String) {
        viewModelScope.launch {
            if (trainedModel == null) trainModel()
            val currency = Currency.fromCode(currencyCode.value)
            val cats = categoryRepository.getAll()
            val parsed = bankStatementParser.parse(text, cats, trainedModel, currency)
            _parsedPreviews.value = parsed.map {
                ParsedPreview(it.id, it.money, it.type, it.description, it.predictedCategoryId, it.date)
            }
        }
    }

    fun parseVoiceInput(text: String) {
        viewModelScope.launch {
            if (trainedModel == null) trainModel()
            val currency = Currency.fromCode(currencyCode.value)
            val result = voiceInputParser.parse(text, currency)
            val cats = categoryRepository.getAll()
            val ruleCat = merchantRuleRepository.findCategoryId(result.description)
            val catId = ruleCat ?: result.money?.let {
                trainedModel?.let { model -> classifier.classify(result.description, model, cats) }
            } ?: cats.firstOrNull()?.id ?: 0L
            _voicePreview.value = ParsedPreview(
                id = 1,
                money = result.money ?: Money.fromMajor(0.0, currency),
                type = result.type,
                description = result.description,
                predictedCategoryId = catId,
                date = System.currentTimeMillis()
            )
        }
    }

    fun updatePreviewCategory(previewId: Long, categoryId: Long) {
        _parsedPreviews.value = _parsedPreviews.value.map {
            if (it.id == previewId) it.copy(predictedCategoryId = categoryId) else it
        }
        _voicePreview.value = _voicePreview.value?.copy(predictedCategoryId = categoryId)
    }

    fun commitPreviews() {
        val previews = _parsedPreviews.value
        if (previews.isEmpty()) return
        viewModelScope.launch {
            transactionRepository.insertAll(previews.map {
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
            trainModel()
        }
    }

    fun commitVoicePreview() {
        val preview = _voicePreview.value ?: return
        if (preview.money.amountMinor <= 0) return
        viewModelScope.launch {
            transactionRepository.insert(
                TransactionData(
                    money = preview.money,
                    type = preview.type,
                    categoryId = preview.predictedCategoryId,
                    description = preview.description,
                    date = preview.date,
                    isAutoCategorized = true
                )
            )
            merchantRuleRepository.saveRule(preview.description, preview.predictedCategoryId)
            _voicePreview.value = null
            trainModel()
        }
    }

    /**
     * Saves a voice preview after the user edited it in the transaction dialog.
     * The manual correction is the strongest signal, so the merchant rule is saved too.
     */
    fun commitVoicePreviewEdited(money: Money, type: TransactionType, categoryId: Long, description: String, date: Long) {
        viewModelScope.launch {
            try {
                transactionRepository.insert(
                    TransactionData(money = money, type = type, categoryId = categoryId, description = description, date = date)
                )
                merchantRuleRepository.saveRule(description, categoryId)
                _voicePreview.value = null
                trainModel()
            } catch (e: Exception) {
                Timber.e(e, "Failed to commit edited voice preview")
                _snackbarMessage.value = SNACKBAR_SAVE_ERROR
            }
        }
    }

    fun clearPreviews() {
        _parsedPreviews.value = emptyList()
        _voicePreview.value = null
    }

    fun completeOnboarding(loadDemo: Boolean) {
        Timber.d("completeOnboarding: loadDemo=%s", loadDemo)
        viewModelScope.launch {
            try {
                settingsRepository.setOnboardingDone(true)
                if (loadDemo) {
                    settingsRepository.setDemoLoaded(true)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to complete onboarding")
            }
        }
    }

    fun defaultCategoryId(type: TransactionType, categories: List<CategoryData>): Long {
        if (lastUsedCategoryId != 0L && categories.any { it.id == lastUsedCategoryId && it.type == type }) {
            return lastUsedCategoryId
        }
        return categories.firstOrNull { it.type == type }?.id ?: categories.firstOrNull()?.id ?: 0L
    }

    suspend fun needsOnboarding(): Boolean = !settingsRepository.onboardingDone.first()

    companion object {
        const val SNACKBAR_DELETED = "deleted"
        const val SNACKBAR_SAVE_ERROR = "save_error"
    }
}
