package kz.abdiyev.walletai.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.ml.statement.StatementFormat
import kz.abdiyev.walletai.ui.viewmodel.ImportSummary
import kz.abdiyev.walletai.ui.viewmodel.ImportViewModel
import kz.abdiyev.walletai.ui.viewmodel.ParsedPreview
import kz.abdiyev.walletai.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val parsedPreviews by viewModel.parsedPreviews.collectAsStateWithLifecycle()
    val importSummary by viewModel.importSummary.collectAsStateWithLifecycle()
    val skipTransfers by viewModel.skipTransfers.collectAsStateWithLifecycle()
    val importMessage by viewModel.importMessage.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val currency = remember(settings.currencyCode) { Currency.fromCode(settings.currencyCode) }
    var statementInputText by remember { mutableStateOf("") }
    val committableCount = remember(parsedPreviews, skipTransfers) {
        viewModel.committablePreviews().size
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Импорт выписки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
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
                    expanded = true,
                    onToggle = {},
                    onImportPdf = {
                        pdfLauncher.launch(arrayOf("application/pdf"))
                    }
                )
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
                    ImportParsedPreviewsCard(
                        previews = parsedPreviews,
                        categories = categories,
                        currency = currency,
                        skipTransfers = skipTransfers,
                        committableCount = committableCount,
                        onUpdateCategory = viewModel::updatePreviewCategory,
                        onClear = viewModel::clearPreviews,
                        onCommit = viewModel::commitPreviews
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ImportParsedPreviewsCard(
    previews: List<ParsedPreview>,
    categories: List<kz.abdiyev.walletai.domain.model.CategoryData>,
    currency: Currency,
    skipTransfers: Boolean,
    committableCount: Int,
    onUpdateCategory: (Long, Long) -> Unit,
    onClear: () -> Unit,
    onCommit: () -> Unit
) {
    PreviewCardShell(
        title = "Предпросмотр (${previews.size})",
        onClose = onClear
    ) {
        previews.forEach { preview ->
            val skipped = preview.isDuplicate || (skipTransfers && preview.isTransfer)
            ImportPreviewItem(
                preview = preview,
                categories = categories,
                currency = currency,
                skipped = skipped,
                onUpdateCategory = { onUpdateCategory(preview.id, it) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onClear) { Text("Отмена") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onCommit,
                enabled = committableCount > 0,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Сохранить ($committableCount)")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImportPreviewItem(
    preview: ParsedPreview,
    categories: List<kz.abdiyev.walletai.domain.model.CategoryData>,
    currency: Currency,
    skipped: Boolean,
    onUpdateCategory: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
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
        PreviewItem(
            preview = preview,
            categories = categories,
            currency = currency,
            onUpdateCategory = onUpdateCategory
        )
    }
}
