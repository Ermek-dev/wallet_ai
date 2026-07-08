package kz.abdiyev.walletai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ui.components.CategoryShare
import kz.abdiyev.walletai.ui.components.DonutChart
import kz.abdiyev.walletai.ui.components.ProgressBarChart
import kz.abdiyev.walletai.ui.theme.WalletAiTheme
import kz.abdiyev.walletai.ui.viewmodel.AnalyticsChartMode
import kz.abdiyev.walletai.ui.viewmodel.AnalyticsViewModel
import kz.abdiyev.walletai.util.MoneyFormatter
import kz.abdiyev.walletai.util.ShareHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val chartMode by viewModel.chartMode.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val currency = remember(settings.currencyCode) { Currency.fromCode(settings.currencyCode) }
    val financialColors = WalletAiTheme.financialColors
    val expenses = filteredTransactions.filter { it.type == TransactionType.EXPENSE }
    val incomes = filteredTransactions.filter { it.type == TransactionType.INCOME }
    val totalExpenseMinor = expenses.sumOf { it.money.amountMinor }
    val totalIncomeMinor = incomes.sumOf { it.money.amountMinor }
    val balanceMinor = totalIncomeMinor - totalExpenseMinor
    val savingsRate = if (totalIncomeMinor > 0) {
        balanceMinor.toFloat() / totalIncomeMinor.toFloat() * 100f
    } else {
        0f
    }

    val activeType = when (chartMode) {
        AnalyticsChartMode.EXPENSE -> TransactionType.EXPENSE
        AnalyticsChartMode.INCOME -> TransactionType.INCOME
    }
    val activeTransactions = filteredTransactions.filter { it.type == activeType }
    val totalActiveMinor = activeTransactions.sumOf { it.money.amountMinor }

    val categoryShares = remember(activeTransactions, categories, currency, totalActiveMinor) {
        if (totalActiveMinor == 0L) emptyList()
        else {
            activeTransactions.groupBy { it.categoryId }.map { (catId, txs) ->
                val category = categories.find { it.id == catId }
                    ?: CategoryData(id = catId, name = "Другое", type = activeType, colorHex = "#757575", iconName = "")
                val sumMinor = txs.sumOf { it.money.amountMinor }
                val pct = (sumMinor.toFloat() / totalActiveMinor * 100f)
                CategoryShare(
                    category = category,
                    amountMinor = sumMinor,
                    percentage = pct,
                    color = parseColorHex(category.colorHex)
                )
            }.sortedByDescending { it.amountMinor }
        }
    }

    var exportSuccessFile by remember { mutableStateOf<File?>(null) }
    var exportType by remember { mutableStateOf("") }
    var exportMimeType by remember { mutableStateOf("text/csv") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Сводка за ${selectedPeriod.labelRu.lowercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Доходы", style = MaterialTheme.typography.labelMedium)
                            Text(
                                MoneyFormatter.format(Money(totalIncomeMinor, currency), showSign = false),
                                fontWeight = FontWeight.Bold,
                                color = financialColors.income
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Расходы", style = MaterialTheme.typography.labelMedium)
                            Text(
                                MoneyFormatter.format(Money(totalExpenseMinor, currency), showSign = false),
                                fontWeight = FontWeight.Bold,
                                color = financialColors.expense
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Сальдо", style = MaterialTheme.typography.labelMedium)
                            Text(
                                MoneyFormatter.formatBalance(Money(balanceMinor, currency)),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Норма сбережений", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = if (totalIncomeMinor > 0) "${"%.0f".format(savingsRate)}%" else "—",
                                fontWeight = FontWeight.Bold,
                                color = if (savingsRate >= 0) financialColors.income else financialColors.expense
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Период: ${selectedPeriod.labelRu}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Period.entries.forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { viewModel.setPeriod(period) },
                                label = { Text(period.labelRu, fontSize = 11.sp) }
                            )
                        }
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = chartMode == AnalyticsChartMode.EXPENSE,
                            onClick = { viewModel.setChartMode(AnalyticsChartMode.EXPENSE) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            label = { Text("Расходы") }
                        )
                        SegmentedButton(
                            selected = chartMode == AnalyticsChartMode.INCOME,
                            onClick = { viewModel.setChartMode(AnalyticsChartMode.INCOME) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            label = { Text("Доходы") }
                        )
                    }
                }
            }
        }

        if (totalActiveMinor == 0L) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (chartMode == AnalyticsChartMode.EXPENSE) {
                                "Недостаточно данных расходов для графиков."
                            } else {
                                "Недостаточно данных доходов для графиков."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (chartMode == AnalyticsChartMode.EXPENSE) {
                                "Структура расходов"
                            } else {
                                "Структура доходов"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        DonutChart(
                            shares = categoryShares,
                            totalText = MoneyFormatter.format(Money(totalActiveMinor, currency)),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Рейтинг по категориям",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        ProgressBarChart(shares = categoryShares, currency = currency)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Экспорт отчётов",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "CSV (Excel) и PDF с полной историей операций.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.exportToCsv(context) { file ->
                                    exportSuccessFile = file
                                    exportType = "CSV"
                                    exportMimeType = "text/csv"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CSV", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                viewModel.exportToPdf(context) { file ->
                                    exportSuccessFile = file
                                    exportType = "PDF"
                                    exportMimeType = "application/pdf"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    exportSuccessFile?.let { file ->
        AlertDialog(
            onDismissRequest = { exportSuccessFile = null },
            title = { Text("Экспорт завершён") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Отчёт $exportType создан: ${file.name}")
                    Text(
                        text = "Нажмите «Поделиться», чтобы отправить файл.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ShareHelper.shareFile(context, file, exportMimeType)
                        exportSuccessFile = null
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Поделиться")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportSuccessFile = null }) {
                    Text("Закрыть")
                }
            }
        )
    }
}
