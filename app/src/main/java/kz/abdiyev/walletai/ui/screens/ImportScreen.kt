package kz.abdiyev.walletai.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ml.statement.StatementFormat
import kz.abdiyev.walletai.ui.components.DarkActionButton
import kz.abdiyev.walletai.ui.components.OutlinedPill
import kz.abdiyev.walletai.ui.components.PrimaryCta
import kz.abdiyev.walletai.ui.components.WalletSurfaceCard
import kz.abdiyev.walletai.ui.theme.SmallShape
import kz.abdiyev.walletai.ui.theme.WalletAiTheme
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.ui.viewmodel.ImportSummary
import kz.abdiyev.walletai.ui.viewmodel.ImportUiState
import kz.abdiyev.walletai.ui.viewmodel.ImportViewModel
import kz.abdiyev.walletai.ui.viewmodel.ParsedPreview
import kz.abdiyev.walletai.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val financialColors = WalletAiTheme.financialColors

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val parsedPreviews by viewModel.parsedPreviews.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val skipTransfers by viewModel.skipTransfers.collectAsStateWithLifecycle()
    val importMessage by viewModel.importMessage.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val currency = remember(settings.currencyCode) { Currency.fromCode(settings.currencyCode) }
    var statementInputText by remember { mutableStateOf("") }
    var inputExpanded by remember { mutableStateOf(true) }
    var showSkipped by remember { mutableStateOf(false) }
    var editingPreview by remember { mutableStateOf<ParsedPreview?>(null) }
    val isParsing = uiState is ImportUiState.Parsing
    val isSaving = uiState is ImportUiState.Saving
    val importSummary = (uiState as? ImportUiState.Preview)?.summary
    val sourceLabel = when (val state = uiState) {
        is ImportUiState.Parsing -> state.sourceLabel
        is ImportUiState.Preview -> state.sourceLabel
        else -> null
    }

    val committableCount = remember(parsedPreviews, skipTransfers) {
        viewModel.committablePreviews().size
    }
    val skippedPreviews = remember(parsedPreviews, skipTransfers) {
        parsedPreviews.filter { it.isDuplicate || (skipTransfers && it.isTransfer) }
    }
    val committablePreviews = remember(parsedPreviews, skipTransfers) {
        parsedPreviews.filterNot { it.isDuplicate || (skipTransfers && it.isTransfer) }
    }
    val displayPreviews = remember(committablePreviews, skippedPreviews, showSkipped) {
        if (showSkipped) committablePreviews + skippedPreviews else committablePreviews
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.parsePdf(context, it) }
    }

    LaunchedEffect(importMessage) {
        importMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearImportMessage()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is ImportUiState.Error) {
            snackbarHostState.showSnackbar((uiState as ImportUiState.Error).message)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(parsedPreviews.isNotEmpty()) {
        if (parsedPreviews.isNotEmpty()) {
            inputExpanded = false
            showSkipped = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Импорт выписки", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (parsedPreviews.isNotEmpty() || isSaving) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 3.dp
                ) {
                    if (isSaving) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Сохранение…")
                        }
                    } else {
                        DarkActionButton(
                            text = "Добавить $committableCount операций",
                            onClick = viewModel::commitPreviews,
                            enabled = committableCount > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                bottom = if (parsedPreviews.isNotEmpty()) 88.dp else 24.dp,
                top = 8.dp
            )
        ) {
            item {
                StatementImportCard(
                    text = statementInputText,
                    onTextChange = { statementInputText = it },
                    onParse = {
                        viewModel.parseBankStatement(statementInputText)
                        statementInputText = ""
                        focusManager.clearFocus()
                    },
                    onClear = { statementInputText = "" },
                    expanded = inputExpanded,
                    onToggle = { inputExpanded = !inputExpanded },
                    onImportPdf = {
                        pdfLauncher.launch(arrayOf("application/pdf"))
                    },
                    isParsing = isParsing
                )
            }

            if (isParsing) {
                item {
                    WalletSurfaceCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Column {
                                Text("Разбор выписки…", fontWeight = FontWeight.Medium)
                                sourceLabel?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            importSummary?.let { summary ->
                item {
                    ImportSummaryCard(
                        summary = summary,
                        skipTransfers = skipTransfers,
                        onSkipTransfersChange = viewModel::setSkipTransfers
                    )
                }
            }

            if (parsedPreviews.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Распознано ${parsedPreviews.size} операций",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            viewModel.clearPreviews()
                            inputExpanded = true
                            showSkipped = false
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(displayPreviews, key = { it.id }) { preview ->
                    val skipped = preview.isDuplicate || (skipTransfers && preview.isTransfer)
                    ImportPreviewItem(
                        preview = preview,
                        categories = categories,
                        skipped = skipped,
                        incomeColor = financialColors.income,
                        expenseColor = financialColors.expense,
                        onUpdateCategory = { viewModel.updatePreviewCategory(preview.id, it) },
                        onEdit = { editingPreview = preview }
                    )
                }

                if (skippedPreviews.isNotEmpty() && !showSkipped) {
                    item {
                        TextButton(
                            onClick = { showSkipped = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Пропущено: ${skippedPreviews.size} — показать")
                        }
                    }
                }
            }
        }
    }

    editingPreview?.let { preview ->
        val tx = TransactionData(
            id = preview.id,
            money = preview.money,
            type = preview.type,
            categoryId = preview.predictedCategoryId,
            description = preview.description,
            date = preview.date
        )
        TransactionDialog(
            categories = categories,
            currency = currency,
            existing = tx,
            onDismiss = { editingPreview = null },
            onConfirm = { money, type, categoryId, description, date ->
                viewModel.updatePreviewEdited(preview.id, money, type, categoryId, description, date)
                editingPreview = null
            }
        )
    }
}

@Composable
private fun StatementImportCard(
    text: String,
    onTextChange: (String) -> Unit,
    onParse: () -> Unit,
    onClear: () -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
    onImportPdf: () -> Unit,
    isParsing: Boolean = false
) {
    WalletSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !expanded) { onToggle() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Импорт из выписки / SMS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть"
                )
            }

            if (expanded) {
                Text(
                    text = "Вставьте строки из банковской выписки или загрузите PDF-файл.",
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
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    shape = SmallShape,
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
                    OutlinedPill(text = "PDF-файл", onClick = onImportPdf)
                    Spacer(modifier = Modifier.width(8.dp))
                    PrimaryCta(
                        text = "Анализировать",
                        icon = Icons.Default.AutoAwesome,
                        onClick = onParse,
                        enabled = text.isNotBlank() && !isParsing,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportSummaryCard(
    summary: ImportSummary,
    skipTransfers: Boolean,
    onSkipTransfersChange: (Boolean) -> Unit
) {
    WalletSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Сводка разбора",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Формат: ${formatLabel(summary.format)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Распознано ${summary.totalRecognized} операций: " +
                    "${summary.expenseCount} расходов на ${MoneyFormatter.format(summary.expenseTotal)}, " +
                    "${summary.incomeCount} доходов на ${MoneyFormatter.format(summary.incomeTotal)}",
                style = MaterialTheme.typography.bodySmall
            )
            if (summary.transferCount > 0) {
                Text(
                    text = "Переводов: ${summary.transferCount}" +
                        if (skipTransfers) " (будут пропущены при сохранении)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (summary.duplicateCount > 0) {
                Text(
                    text = "Уже в приложении: ${summary.duplicateCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Пропускать переводы")
                Switch(checked = skipTransfers, onCheckedChange = onSkipTransfersChange)
            }
        }
    }
}

private fun formatLabel(format: StatementFormat): String = when (format) {
    StatementFormat.KASPI -> "Kaspi"
    StatementFormat.FREEDOM -> "Freedom"
    StatementFormat.GENERIC -> "Универсальный"
}

@Composable
private fun ImportPreviewItem(
    preview: ParsedPreview,
    categories: List<CategoryData>,
    skipped: Boolean,
    incomeColor: androidx.compose.ui.graphics.Color,
    expenseColor: androidx.compose.ui.graphics.Color,
    onUpdateCategory: (Long) -> Unit,
    onEdit: () -> Unit
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id == preview.predictedCategoryId }
    val amountColor = if (preview.type == TransactionType.INCOME) incomeColor else expenseColor
    val itemAlpha = if (skipped) 0.55f else 1f

    WalletSurfaceCard(modifier = Modifier.alpha(itemAlpha).clickable(onClick = onEdit)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(preview.description, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (preview.type == TransactionType.INCOME) "Доход" else "Расход",
                        style = MaterialTheme.typography.bodySmall,
                        color = amountColor
                    )
                }
                Text(
                    text = MoneyFormatter.format(preview.money, showSign = true, type = preview.type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (preview.isTransfer) {
                    AssistChip(onClick = {}, label = { Text("перевод") }, enabled = false)
                }
                if (preview.isDuplicate) {
                    AssistChip(onClick = {}, label = { Text("уже импортировано") }, enabled = false)
                }
                if (skipped && !preview.isDuplicate) {
                    AssistChip(onClick = {}, label = { Text("пропуск") }, enabled = false)
                }
            }

            if (!skipped) {
                Box {
                    AssistChip(
                        onClick = { categoryMenuExpanded = true },
                        label = {
                            Text(selectedCategory?.name ?: "Выбрать категорию")
                        }
                    )
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    onUpdateCategory(category.id)
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
