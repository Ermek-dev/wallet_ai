package kz.abdiyev.walletai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.abdiyev.walletai.R
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ui.components.BalanceSummaryRow
import kz.abdiyev.walletai.ui.components.DayGroupCard
import kz.abdiyev.walletai.ui.components.HiddenByPeriodHint
import kz.abdiyev.walletai.ui.components.WalletEmptyState
import kz.abdiyev.walletai.ui.components.MonthHeader
import kz.abdiyev.walletai.ui.components.PeriodRangePickerDialog
import kz.abdiyev.walletai.ui.components.SegmentedPeriodSelector
import kz.abdiyev.walletai.ui.components.TransactionRow
import kz.abdiyev.walletai.ui.components.WalletSurfaceCard
import kz.abdiyev.walletai.ui.components.tiltGlow
import kz.abdiyev.walletai.ui.components.getCategoryIcon
import kz.abdiyev.walletai.ui.theme.FabDark
import kz.abdiyev.walletai.ui.theme.FabShape
import kz.abdiyev.walletai.ui.theme.CardShape
import kz.abdiyev.walletai.ui.theme.SmallShape
import kz.abdiyev.walletai.ui.theme.WalletAiTheme
import kz.abdiyev.walletai.util.groupTransactionsByDay
import kz.abdiyev.walletai.ui.viewmodel.DashboardViewModel
import kz.abdiyev.walletai.ui.viewmodel.ParsedPreview
import kz.abdiyev.walletai.ui.voice.VoiceRecognizerHelper
import kz.abdiyev.walletai.util.MoneyFormatter
import kz.abdiyev.walletai.util.PeriodLabels
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    userName: String = "",
    onLoadDemo: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onboardingReady by viewModel.onboardingReady.collectAsStateWithLifecycle()

    val categories = uiState.categories
    val filteredTransactions = uiState.filteredTransactions
    val hiddenByPeriodCount = uiState.hiddenByPeriodCount
    val voicePreview = uiState.voicePreview
    val selectedPeriod = uiState.selectedPeriod
    val customRange = uiState.customRange
    val currencyCode = uiState.currencyCode

    val currency = remember(currencyCode) { Currency.fromCode(currencyCode) }
    val financialColors = WalletAiTheme.financialColors
    val dayGroups = remember(filteredTransactions) { groupTransactionsByDay(filteredTransactions) }
    val periodTitle = remember(selectedPeriod, customRange) { PeriodLabels.title(selectedPeriod, customRange = customRange) }
    val periodRange = remember(selectedPeriod, customRange) { PeriodLabels.rangeLabel(selectedPeriod, customRange = customRange) }
    val initials = remember(userName) { userName.trim().take(2).ifBlank { "?" } }

    var showPeriodPicker by remember { mutableStateOf(false) }

    val expenses = filteredTransactions.filter { it.type == TransactionType.EXPENSE }
        .fold(Money(0, currency)) { acc, tx -> acc + tx.money }
    val incomes = filteredTransactions.filter { it.type == TransactionType.INCOME }
        .fold(Money(0, currency)) { acc, tx -> acc + tx.money }
    val totalBalance = incomes - expenses

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionData?>(null) }
    var editingVoicePreview by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var voiceTextFallback by remember { mutableStateOf("") }
    var showVoiceFallback by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale("ru")) }
    var pendingVoiceStart by remember { mutableStateOf(false) }
    val showOnboarding = onboardingReady == false

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingVoiceStart = true
    }

    var voiceHelper by remember { mutableStateOf<VoiceRecognizerHelper?>(null) }

    DisposableEffect(context) {
        onDispose {
            voiceHelper?.destroy()
            voiceHelper = null
        }
    }

    fun beginVoiceRecognition() {
        voiceHelper?.stopListening()
        voiceHelper = VoiceRecognizerHelper(
            context = context,
            onResult = { text ->
                viewModel.parseVoiceInput(text)
                showVoiceFallback = false
            },
            onError = { errorCode ->
                viewModel.showMessage(VoiceRecognizerHelper.userMessageForError(errorCode))
                showVoiceFallback = true
            },
            onListeningChanged = { listening -> isListening = listening }
        ).also { it.startListening() }
    }

    fun startVoiceInput() {
        if (!VoiceRecognizerHelper(context, {}, {}).isAvailable()) {
            showVoiceFallback = true
            return
        }
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        beginVoiceRecognition()
    }

    LaunchedEffect(pendingVoiceStart) {
        if (pendingVoiceStart) {
            pendingVoiceStart = false
            beginVoiceRecognition()
        }
    }

    if (showOnboarding) {
        AlertDialog(
            onDismissRequest = {
                viewModel.completeOnboarding(loadDemo = false)
            },
            title = { Text("Добро пожаловать в WalletAI") },
            text = {
                Text("Начните с чистого листа или загрузите демо-данные за 3 месяца для знакомства с приложением.")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.completeOnboarding(loadDemo = true)
                    onLoadDemo()
                }) {
                    Text("Демо-данные")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.completeOnboarding(loadDemo = false)
                }) {
                    Text("Чистый старт")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp)
        ) {
            item {
                MonthHeader(monthLabel = periodTitle, initials = initials)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.dashboard_balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MoneyFormatter.formatBalance(totalBalance),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalBalance.amountMinor >= 0) financialColors.income else financialColors.expense
                    )
                    BalanceSummaryRow(
                        incomesText = MoneyFormatter.format(incomes, showSign = false),
                        expensesText = MoneyFormatter.format(expenses, showSign = false),
                        incomeColor = financialColors.income,
                        expenseColor = financialColors.expense
                    )
                }
            }

            item {
                SegmentedPeriodSelector(
                    selected = selectedPeriod,
                    hasCustomRange = customRange != null,
                    onSelectPreset = viewModel::setPeriod,
                    onCustomClick = { showPeriodPicker = true }
                )
            }

            if (selectedPeriod != Period.ALL) {
                item {
                    Text(
                        text = periodRange,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isListening) {
                item {
                    WalletSurfaceCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Слушаю…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Пример: «потратил 1500 на обед»",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Системное распознавание речи может отправлять аудио в облако Google. Всегда можно ввести операцию вручную.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            voicePreview?.let { preview ->
                item {
                    VoicePreviewCard(
                        preview = preview,
                        categories = categories,
                        currency = currency,
                        onUpdateCategory = { viewModel.updateVoicePreviewCategory(it) },
                        onClear = { viewModel.clearVoicePreview() },
                        onEdit = { editingVoicePreview = true },
                        onCommit = viewModel::commitVoicePreview
                    )
                }
            }

            if (showVoiceFallback) {
                item {
                    VoiceFallbackCard(
                        text = voiceTextFallback,
                        onTextChange = { voiceTextFallback = it },
                        onSubmit = {
                            if (voiceTextFallback.isNotBlank()) {
                                viewModel.parseVoiceInput(voiceTextFallback)
                                voiceTextFallback = ""
                                showVoiceFallback = false
                            }
                        },
                        onRetry = {
                            showVoiceFallback = false
                            startVoiceInput()
                        },
                        onDismiss = { showVoiceFallback = false }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Операции",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Все",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onNavigateToHistory)
                    )
                }
            }

            if (filteredTransactions.isEmpty()) {
                item {
                    WalletEmptyState(
                        message = stringResource(R.string.dashboard_empty_period),
                        footer = if (hiddenByPeriodCount > 0) {
                            {
                                HiddenByPeriodHint(
                                    hiddenCount = hiddenByPeriodCount,
                                    onShowAll = { viewModel.setPeriod(Period.ALL) }
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            } else {
                item {
                    DayGroupCard(
                        dayGroups = dayGroups,
                        categories = categories,
                        incomeColor = financialColors.income,
                        expenseColor = financialColors.expense,
                        onTransactionClick = { editingTransaction = it },
                        onTransactionDelete = viewModel::deleteTransaction,
                        parseColorHex = ::parseColorHex
                    )
                }
            }
        }

        val isDark = isSystemInDarkTheme()
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFloatingActionButton(
                onClick = {
                    if (isListening) voiceHelper?.stopListening() else startVoiceInput()
                },
                modifier = Modifier.tiltGlow(shape = FabShape, intensity = 0.25f, maxShiftDp = 6.dp),
                containerColor = if (isListening) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Close else Icons.Default.Mic,
                    contentDescription = if (isListening) stringResource(R.string.voice_stop) else stringResource(R.string.voice_input)
                )
            }
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.tiltGlow(shape = FabShape, intensity = 0.3f),
                shape = FabShape,
                containerColor = if (isDark) MaterialTheme.colorScheme.primary else FabDark,
                contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить операцию")
            }
        }
    }

    if (showPeriodPicker) {
        PeriodRangePickerDialog(
            onDismiss = { showPeriodPicker = false },
            onConfirm = viewModel::applyCustomRange
        )
    }

    if (showAddDialog) {
        TransactionDialog(
            categories = categories,
            currency = currency,
            defaultCategoryId = viewModel.defaultCategoryId(TransactionType.EXPENSE, categories),
            onDismiss = { showAddDialog = false },
            onConfirm = { money, type, categoryId, desc, date ->
                viewModel.addTransaction(money, type, categoryId, desc, date)
                showAddDialog = false
            }
        )
    }

    editingTransaction?.let { tx ->
        TransactionDialog(
            categories = categories,
            currency = currency,
            existing = tx,
            onDismiss = { editingTransaction = null },
            onConfirm = { money, type, categoryId, desc, date ->
                viewModel.updateTransaction(
                    tx.copy(money = money, type = type, categoryId = categoryId, description = desc, date = date)
                )
                editingTransaction = null
            }
        )
    }

    if (editingVoicePreview) {
        voicePreview?.let { preview ->
            TransactionDialog(
                categories = categories,
                currency = currency,
                existing = TransactionData(
                    money = preview.money,
                    type = preview.type,
                    categoryId = preview.predictedCategoryId,
                    description = preview.description,
                    date = preview.date
                ),
                onDismiss = { editingVoicePreview = false },
                onConfirm = { money, type, categoryId, desc, date ->
                    viewModel.commitVoicePreviewEdited(money, type, categoryId, desc, date)
                    editingVoicePreview = false
                }
            )
        }
    }
}

@Composable
private fun BalanceCard(
    balance: Money,
    incomes: Money,
    expenses: Money,
    incomeColor: Color,
    expenseColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Текущий баланс",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = MoneyFormatter.formatBalance(balance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Доходы",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = MoneyFormatter.format(incomes, showSign = false),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = incomeColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Расходы",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = MoneyFormatter.format(expenses, showSign = false),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = expenseColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoicePreviewCard(
    preview: ParsedPreview,
    categories: List<CategoryData>,
    currency: Currency,
    onUpdateCategory: (Long) -> Unit,
    onClear: () -> Unit,
    onEdit: () -> Unit,
    onCommit: () -> Unit
) {
    PreviewCardShell(title = "Голосовой ввод", onClose = onClear) {
        PreviewItem(
            preview = preview,
            categories = categories,
            currency = currency,
            onUpdateCategory = onUpdateCategory
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Изменить")
            }
            Spacer(modifier = Modifier.width(8.dp))
            val canCommit = preview.money.amountMinor > 0
            Button(
                onClick = onCommit,
                enabled = canCommit,
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Подтвердить", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Сохранить")
            }
        }
        if (preview.money.amountMinor <= 0) {
            Text(
                text = "Не удалось определить сумму — отредактируйте операцию вручную.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
internal fun PreviewCardShell(
    title: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    WalletSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PreviewItem(
    preview: ParsedPreview,
    categories: List<CategoryData>,
    currency: Currency,
    onUpdateCategory: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(preview.description, fontWeight = FontWeight.Medium)
                Text(
                    text = if (preview.type == TransactionType.INCOME) "Доход" else "Расход",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (preview.type == TransactionType.INCOME) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
            Text(
                text = MoneyFormatter.format(preview.money, showSign = true, type = preview.type),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (preview.type == TransactionType.INCOME) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { category ->
                val isSelected = preview.predictedCategoryId == category.id
                val color = parseColorHex(category.colorHex)
                SuggestionChip(
                    onClick = { onUpdateCategory(category.id) },
                    label = { Text(category.name, fontSize = 10.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isSelected) color.copy(alpha = 0.25f) else Color.Transparent,
                        labelColor = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

@Composable
private fun VoiceFallbackCard(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Не удалось распознать голос", fontWeight = FontWeight.Bold)
            Text(
                text = "Повторите команду или введите фразу вручную. Распознавание может использовать облачный сервис Google.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("потратил 3400 в Magnum") }
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("Отмена") }
                TextButton(onClick = onRetry) { Text("Повторить") }
                Button(onClick = onSubmit, enabled = text.isNotBlank()) { Text("Разобрать") }
            }
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: TransactionData,
    category: CategoryData?,
    dateFormatter: SimpleDateFormat,
    incomeColor: Color = Color(0xFF2E9C6E),
    expenseColor: Color = Color(0xFFDA5C5C),
    onClick: () -> Unit = {},
    onDelete: () -> Unit = {},
    showDeleteButton: Boolean = true
) {
    val categoryColor = category?.let { parseColorHex(it.colorHex) } ?: Color.Gray
    TransactionRow(
        transaction = transaction,
        category = category,
        categoryColor = categoryColor,
        incomeColor = incomeColor,
        expenseColor = expenseColor,
        onClick = onClick,
        onDelete = onDelete,
        showDeleteButton = showDeleteButton
    )
}
