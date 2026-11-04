package kz.abdiyev.walletai.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.PeriodRange
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionPreview
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.domain.repository.CategoryRepository
import kz.abdiyev.walletai.domain.repository.SettingsRepository
import kz.abdiyev.walletai.domain.repository.TransactionRepository
import kz.abdiyev.walletai.domain.usecase.SaveTransactionRequest
import kz.abdiyev.walletai.domain.usecase.SaveTransactionResult
import kz.abdiyev.walletai.domain.usecase.SaveTransactionUseCase
import kz.abdiyev.walletai.ml.TransactionClassifier
import kz.abdiyev.walletai.ml.VoiceInputParser
import kz.abdiyev.walletai.util.Clock
import kz.abdiyev.walletai.util.PeriodFilter
import kz.abdiyev.walletai.util.SystemClock
import kz.abdiyev.walletai.util.TimeUtils
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

/** @see TransactionPreview */
typealias ParsedPreview = TransactionPreview

@Immutable
data class DashboardUiState(
    val transactions: List<TransactionData> = emptyList(),
    val categories: List<CategoryData> = emptyList(),
    val settings: kz.abdiyev.walletai.domain.model.Settings = kz.abdiyev.walletai.domain.model.Settings(),
    val filteredTransactions: List<TransactionData> = emptyList(),
    val hiddenByPeriodCount: Int = 0,
    val selectedPeriod: Period = Period.MONTH,
    val customRange: PeriodRange? = null,
    val currencyCode: String = Currency.KZT.code,
    val voicePreview: TransactionPreview? = null
)

sealed interface DashboardEvent {
    data class Deleted(val transaction: TransactionData) : DashboardEvent
    data object SaveError : DashboardEvent
    data class Info(val text: String) : DashboardEvent
    data object VoiceAmountMissing : DashboardEvent
}

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val classifier: TransactionClassifier,
    private val voiceInputParser: VoiceInputParser,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val clock: Clock = SystemClock
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(Period.MONTH)
    private val _customRange = MutableStateFlow<PeriodRange?>(null)
    private val _voicePreview = MutableStateFlow<TransactionPreview?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        combine(
            transactionRepository.transactions,
            categoryRepository.categories,
            settingsRepository.settings,
            ::Triple
        ),
        combine(_selectedPeriod, _customRange, _voicePreview, ::Triple)
    ) { (txs, cats, settings), (period, customRange, voice) ->
        val now = clock.nowMillis()
        val offset = TimeUtils.localOffsetMillis(now)
        val filtered = PeriodFilter.filter(
                txs,
                period,
                now,
                offset,
                customRange
            )
        DashboardUiState(
            transactions = txs,
            categories = cats,
            settings = settings,
            filteredTransactions = filtered,
            hiddenByPeriodCount = PeriodFilter.outsideCount(txs, period, now, offset, customRange),
            selectedPeriod = period,
            customRange = customRange,
            currencyCode = settings.currencyCode,
            voicePreview = voice
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private val _events = Channel<DashboardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _onboardingReady = MutableStateFlow<Boolean?>(null)
    val onboardingReady: StateFlow<Boolean?> = _onboardingReady.asStateFlow()

    private var trainedModel: TransactionClassifier.TrainedModel? = null
    private var lastUsedCategoryId: Long = 0L

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _onboardingReady.value = settings.onboardingDone
            }
        }
        observeAndTrainClassifier()
    }

    @OptIn(FlowPreview::class)
    private fun observeAndTrainClassifier() {
        viewModelScope.launch {
            combine(
                transactionRepository.transactions,
                categoryRepository.categories
            ) { txs, cats -> txs to cats }
                .debounce(2_000)
                .collect { (txs, cats) ->
                    if (txs.isNotEmpty() && cats.isNotEmpty()) {
                        trainedModel = withContext(Dispatchers.Default) { classifier.train(txs, cats) }
                    }
                }
        }
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
        if (period != Period.CUSTOM) {
            _customRange.value = null
        }
    }

    fun applyCustomRange(range: PeriodRange) {
        _customRange.value = range
        _selectedPeriod.value = Period.CUSTOM
    }

    fun addTransaction(money: Money, type: TransactionType, categoryId: Long, description: String, date: Long) {
        Timber.d("addTransaction: %s %s cat=%d", money, type, categoryId)
        lastUsedCategoryId = categoryId
        viewModelScope.launch {
            when (saveTransactionUseCase.execute(
                SaveTransactionRequest(
                    money = money,
                    type = type,
                    categoryId = categoryId,
                    description = description,
                    date = date
                )
            )) {
                is SaveTransactionResult.Success -> Unit
                SaveTransactionResult.InvalidAmount,
                is SaveTransactionResult.BatchSuccess -> _events.trySend(DashboardEvent.SaveError)
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
            val result = voiceInputParser.parse(text, currency, clock.nowMillis())
            val cats = categoryRepository.getAll()
            val catId = saveTransactionUseCase.resolveCategoryId(
                description = result.description,
                categories = cats,
                trainedModel = trainedModel,
                fallbackCategoryId = cats.firstOrNull()?.id ?: 0L
            )
            val parsedMoney = result.money
            if (parsedMoney == null || parsedMoney.amountMinor <= 0) {
                _events.trySend(DashboardEvent.VoiceAmountMissing)
                _voicePreview.value = TransactionPreview(
                    money = Money.fromMajor(0.0, currency),
                    type = result.type,
                    description = result.description.ifBlank { text.trim() },
                    predictedCategoryId = catId,
                    date = result.dateMillis ?: clock.nowMillis()
                )
                return@launch
            }
            _voicePreview.value = TransactionPreview(
                money = parsedMoney,
                type = result.type,
                description = result.description,
                predictedCategoryId = catId,
                date = result.dateMillis ?: clock.nowMillis()
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
            when (saveTransactionUseCase.execute(
                SaveTransactionRequest(
                    money = preview.money,
                    type = preview.type,
                    categoryId = preview.predictedCategoryId,
                    description = preview.description,
                    date = preview.date,
                    isAutoCategorized = true,
                    persistMerchantRule = true
                )
            )) {
                is SaveTransactionResult.Success -> _voicePreview.value = null
                SaveTransactionResult.InvalidAmount,
                is SaveTransactionResult.BatchSuccess -> _events.trySend(DashboardEvent.SaveError)
            }
        }
    }

    /**
     * Saves a voice preview after the user edited it in the transaction dialog.
     * The manual correction is the strongest signal, so the merchant rule is saved too.
     */
    fun commitVoicePreviewEdited(money: Money, type: TransactionType, categoryId: Long, description: String, date: Long) {
        viewModelScope.launch {
            when (saveTransactionUseCase.execute(
                SaveTransactionRequest(
                    money = money,
                    type = type,
                    categoryId = categoryId,
                    description = description,
                    date = date,
                    persistMerchantRule = true
                )
            )) {
                is SaveTransactionResult.Success -> _voicePreview.value = null
                SaveTransactionResult.InvalidAmount,
                is SaveTransactionResult.BatchSuccess -> _events.trySend(DashboardEvent.SaveError)
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
