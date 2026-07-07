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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ui.components.getCategoryIcon
import kz.abdiyev.walletai.ui.theme.WalletAiTheme
import kz.abdiyev.walletai.ui.viewmodel.DashboardViewModel
import kz.abdiyev.walletai.ui.viewmodel.ParsedPreview
import kz.abdiyev.walletai.ui.voice.VoiceRecognizerHelper
import kz.abdiyev.walletai.util.MoneyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onLoadDemo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val parsedPreviews by viewModel.parsedPreviews.collectAsStateWithLifecycle()
    val voicePreview by viewModel.voicePreview.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val currencyCode by viewModel.currencyCode.collectAsStateWithLifecycle()
    val onboardingDone by viewModel.onboardingDone.collectAsStateWithLifecycle()

    val currency = remember(currencyCode) { Currency.fromCode(currencyCode) }
    val financialColors = WalletAiTheme.financialColors
    val recentTransactions = remember(filteredTransactions) { filteredTransactions.take(5) }

    val expenses = filteredTransactions.filter { it.type == TransactionType.EXPENSE }
        .fold(Money(0, currency)) { acc, tx -> acc + tx.money }
    val incomes = filteredTransactions.filter { it.type == TransactionType.INCOME }
        .fold(Money(0, currency)) { acc, tx -> acc + tx.money }
    val totalBalance = incomes - expenses

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionData?>(null) }
    var editingVoicePreview by remember { mutableStateOf(false) }
    var statementInputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var voiceTextFallback by remember { mutableStateOf("") }
    var showVoiceFallback by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale("ru")) }
    var pendingVoiceStart by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }
    var onboardingInitialized by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingVoiceStart = true
    }

    var voiceHelper by remember { mutableStateOf<VoiceRecognizerHelper?>(null) }

    DisposableEffect(context) {
        onDispose { voiceHelper?.stopListening() }
    }

    LaunchedEffect(onboardingDone) {
        if (!onboardingInitialized) {
            showOnboarding = !onboardingDone
            onboardingInitialized = true
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
            onError = { showVoiceFallback = true },
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
                showOnboarding = false
                viewModel.completeOnboarding(loadDemo = false)
            },
            title = { Text("Добро пожаловать в WalletAI") },
            text = {
                Text("Начните с чистого листа или загрузите демо-данные за 3 месяца для знакомства с приложением.")
            },
            confirmButton = {
                Button(onClick = {
                    showOnboarding = false
                    viewModel.completeOnboarding(loadDemo = true)
                    onLoadDemo()
                }) {
                    Text("Демо-данные")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOnboarding = false
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
            contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
        ) {
            item {
                BalanceCard(
                    balance = totalBalance,
                    incomes = incomes,
                    expenses = expenses,
                    incomeColor = financialColors.income,
                    expenseColor = financialColors.expense
                )
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Period.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { viewModel.setPeriod(period) },
                            label = { Text(period.labelRu) }
                        )
                    }
                }
            }

            item {
                StatementImportCard(
                    text = statementInputText,
                    onTextChange = { statementInputText = it },
                    onParse = {
                        viewModel.parseBankStatement(statementInputText)
                        statementInputText = ""
                        focusManager.clearFocus()
                    },
                    onClear = { statementInputText = "" }
                )
            }

            if (parsedPreviews.isNotEmpty()) {
                item {
                    ParsedPreviewsCard(
                        previews = parsedPreviews,
                        categories = categories,
                        currency = currency,
                        onUpdateCategory = viewModel::updatePreviewCategory,
                        onClear = viewModel::clearPreviews,
                        onCommit = viewModel::commitPreviews
                    )
                }
            }

            voicePreview?.let { preview ->
                item {
                    VoicePreviewCard(
                        preview = preview,
                        categories = categories,
                        currency = currency,
                        onUpdateCategory = { viewModel.updatePreviewCategory(preview.id, it) },
                        onClear = { viewModel.clearPreviews() },
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
                        onDismiss = { showVoiceFallback = false }
                    )
                }
            }

            item {
                Text(
                    text = "Последние операции",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (recentTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нет записей за выбранный период",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                items(recentTransactions, key = { it.id }) { tx ->
                    val category = categories.find { it.id == tx.categoryId }
                    TransactionListItem(
                        transaction = tx,
                        category = category,
                        dateFormatter = dateFormatter,
                        incomeColor = financialColors.income,
                        expenseColor = financialColors.expense,
                        onClick = { editingTransaction = tx },
                        onDelete = { viewModel.deleteTransaction(tx) }
                    )
                }
            }
        }

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
                containerColor = if (isListening) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Close else Icons.Default.Mic,
                    contentDescription = if (isListening) "Остановить запись" else "Голосовой ввод"
                )
            }
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить операцию")
            }
        }
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
private fun StatementImportCard(
    text: String,
    onTextChange: (String) -> Unit,
    onParse: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Импорт из выписки / SMS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Вставьте строки из банковской выписки. ML-модель определит категории автоматически.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "01.07 Magnum продукты 4500₸\nKaspi перевод +150000₸",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodySmall,
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (text.isNotBlank()) {
                    TextButton(onClick = onClear) { Text("Сбросить") }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Button(onClick = onParse, enabled = text.isNotBlank(), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Анализировать")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParsedPreviewsCard(
    previews: List<ParsedPreview>,
    categories: List<CategoryData>,
    currency: Currency,
    onUpdateCategory: (Long, Long) -> Unit,
    onClear: () -> Unit,
    onCommit: () -> Unit
) {
    PreviewCardShell(
        title = "Предпросмотр (${previews.size})",
        onClose = onClear
    ) {
        previews.forEach { preview ->
            PreviewItem(
                preview = preview,
                categories = categories,
                currency = currency,
                onUpdateCategory = { onUpdateCategory(preview.id, it) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onClear) { Text("Отмена") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onCommit, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Сохранить все (${previews.size})")
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
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Изменить")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onCommit, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Сохранить")
            }
        }
    }
}

@Composable
private fun PreviewCardShell(
    title: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = MaterialTheme.colorScheme.error)
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewItem(
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
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Голос недоступен — введите фразу вручную", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("потратил 3400 в Magnum") }
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss) { Text("Отмена") }
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
    incomeColor: Color = Color(0xFF2E7D32),
    expenseColor: Color = Color(0xFFC62828),
    onClick: () -> Unit = {},
    onDelete: () -> Unit
) {
    val categoryColor = category?.let { parseColorHex(it.colorHex) } ?: Color.Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getCategoryIcon(category?.iconName ?: ""),
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            transaction.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (transaction.isAutoCategorized) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Auto",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Text(
                        text = "${category?.name ?: "Другое"} • ${dateFormatter.format(Date(transaction.date))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = MoneyFormatter.format(transaction.money, showSign = true, type = transaction.type),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == TransactionType.INCOME) incomeColor else expenseColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
