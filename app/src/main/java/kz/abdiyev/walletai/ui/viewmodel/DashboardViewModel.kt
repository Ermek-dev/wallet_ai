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
import kz.abdiyev.walletai.ml.TransactionClassifier
import kz.abdiyev.walletai.ml.VoiceInputParser
import kz.abdiyev.walletai.util.Clock
import kz.abdiyev.walletai.util.PeriodFilter
import kz.abdiyev.walletai.util.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

data class ParsedPreview(
    val id: Long = 0,
    val money: Money,
    val type: TransactionType,
    val description: String,
    val predictedCategoryId: Long,
    val date: Long,
    val isTransfer: Boolean = false,
    val isDuplicate: Boolean = false
)

data class DashboardUiState(
    val transactions: List<TransactionData> = emptyList(),
    val categories: List<CategoryData> = emptyList(),
    val currencyCode: String = Currency.KZT.code,
    val selectedPeriod: Period = Period.MONTH,
    val filteredTransactions: List<TransactionData> = emptyList(),
    val voicePreview: ParsedPreview? = null
)

sealed interface DashboardEvent {
    data class Deleted(val transaction: TransactionData) : DashboardEvent
    data object SaveError : DashboardEvent
    data class Info(val text: String) : DashboardEvent
}

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val merchantRuleRepository: MerchantRuleRepository,
    private val classifier: TransactionClassifier,
    private val voiceInputParser: VoiceInputParser,
    private val clock: Clock = SystemClock
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(Period.MONTH)
    private val _voicePreview = MutableStateFlow<ParsedPreview?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.transactions,
        categoryRepository.categories,
        settingsRepository.settings,
        _selectedPeriod,
        _voicePreview
    ) { txs, cats, settings, period, voice ->
        DashboardUiState(
            transactions = txs,
            categories = cats,
            currencyCode = settings.currencyCode,
            selectedPeriod = period,
            filteredTransactions = PeriodFilter.filter(txs, period, clock.nowMillis()),
            voicePreview = voice
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private val _onboardingReady = MutableStateFlow<Boolean?>(null)
    val onboardingReady: StateFlow<Boolean?> = _onboardingReady.asStateFlow()

    private val _events = Channel<DashboardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var trainedModel: TransactionClassifier.TrainedModel? = null

    private var lastUsedCategoryId: Long = 0L

    @OptIn(FlowPreview::class)
    private fun observeAndTrainClassifier() {
        // Training follows the data stream with a debounce instead of manual retrain
        // calls after every insert; CPU work stays off the main dispatcher.
        viewModelScope.launch(Dispatchers.Default) {
            combine(transactionRepository.transactions, categoryRepository.categories, ::Pair)
                .debounce(2_000)
                .collect { (txs, cats) ->
                    if (txs.isNotEmpty() && cats.isNotEmpty()) {
                        trainedModel = classifier.train(txs, cats)
                    }
                }
        }
    }

    init {
        viewModelScope.launch {
            categoryRepository.ensureDefaultCategories()
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _onboardingReady.value = settings.onboardingDone
            }
        }
        observeAndTrainClassifier()
    }

    /** Trains immediately when a classification is requested before the debounced stream fires. */
    private suspend fun ensureModelTrained() {
        if (trainedModel != null) return
        val txs = transactionRepository.getAll()
        val cats = categoryRepository.getAll()
        if (txs.isNotEmpty() && cats.isNotEmpty()) {
            trainedModel = withContext(Dispatchers.Default) { classifier.train(txs, cats) }
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
            } catch (e: Exception) {
                Timber.e(e, "Failed to add transaction")
                _events.trySend(DashboardEvent.SaveError)
            }
        }
    }

    fun updateTransaction(transaction: TransactionData) {
        Timber.d("updateTransaction: id=%d", transaction.id)
        lastUsedCategoryId = transaction.categoryId
        viewModelScope.launch {
            try {
                transactionRepository.update(transaction)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update transaction")
                _events.trySend(DashboardEvent.SaveError)
            }
        }
    }

    fun deleteTransaction(transaction: TransactionData) {
        Timber.d("deleteTransaction: id=%d", transaction.id)
        viewModelScope.launch {
            try {
                transactionRepository.delete(transaction)
                _events.trySend(DashboardEvent.Deleted(transaction))
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete transaction")
            }
        }
    }

    fun undoDelete(transaction: TransactionData) {
        viewModelScope.launch {
            try {
                transactionRepository.insert(transaction.copy(id = 0))
            } catch (e: Exception) {
                Timber.e(e, "Failed to undo delete")
            }
        }
    }

    fun showMessage(text: String) {
        _events.trySend(DashboardEvent.Info(text))
    }

    fun parseVoiceInput(text: String) {
        viewModelScope.launch {
            ensureModelTrained()
            val currency = Currency.fromCode(uiState.value.currencyCode)
            val result = voiceInputParser.parse(text, currency)
            val cats = categoryRepository.getAll()
            val ruleCat = merchantRuleRepository.findCategoryId(result.description)
            val catId = ruleCat ?: result.money?.let {
                trainedModel?.let { model -> classifier.classify(result.description, model, cats) }
            } ?: cats.firstOrNull()?.id ?: 0L
            _voicePreview.value = ParsedPreview(
                money = result.money ?: Money.fromMajor(0.0, currency),
                type = result.type,
                description = result.description,
                predictedCategoryId = catId,
                date = clock.nowMillis()
            )
        }
    }

    fun updateVoicePreviewCategory(categoryId: Long) {
        _voicePreview.value = _voicePreview.value?.copy(predictedCategoryId = categoryId)
    }

    fun commitVoicePreview() {
        val preview = _voicePreview.value ?: return
        if (preview.money.amountMinor <= 0) return
        viewModelScope.launch {
            try {
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
            } catch (e: Exception) {
                Timber.e(e, "Failed to commit voice preview")
                _events.trySend(DashboardEvent.SaveError)
            }
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
            } catch (e: Exception) {
                Timber.e(e, "Failed to commit edited voice preview")
                _events.trySend(DashboardEvent.SaveError)
            }
        }
    }

    fun clearVoicePreview() {
        _voicePreview.value = null
    }

    fun completeOnboarding(loadDemo: Boolean) {
        Timber.d("completeOnboarding: loadDemo=%s", loadDemo)
        viewModelScope.launch {
            try {
                settingsRepository.update { current ->
                    current.copy(onboardingDone = true, demoLoaded = current.demoLoaded || loadDemo)
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
}
