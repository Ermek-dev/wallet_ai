package kz.abdiyev.walletai.ui.screens



import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Description

import androidx.compose.material.icons.filled.Download

import androidx.compose.material.icons.filled.Share

import androidx.compose.material3.AlertDialog

import androidx.compose.material3.Button

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.Icon

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

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import kz.abdiyev.walletai.domain.model.CategoryData

import kz.abdiyev.walletai.domain.model.Currency

import kz.abdiyev.walletai.domain.model.Money

import kz.abdiyev.walletai.domain.model.Period

import kz.abdiyev.walletai.domain.model.TransactionType

import kz.abdiyev.walletai.ui.components.CategoryRankingList

import kz.abdiyev.walletai.ui.components.CategoryShare

import kz.abdiyev.walletai.ui.components.DonutChart

import kz.abdiyev.walletai.ui.components.HiddenByPeriodHint
import kz.abdiyev.walletai.ui.components.InfoChip

import kz.abdiyev.walletai.ui.components.OutlinedPill

import kz.abdiyev.walletai.ui.components.ScreenTitle

import kz.abdiyev.walletai.ui.components.PeriodRangePickerDialog
import kz.abdiyev.walletai.ui.components.SegmentedPeriodSelector

import kz.abdiyev.walletai.ui.components.SegmentedToggle

import kz.abdiyev.walletai.ui.components.WalletSurfaceCard

import kz.abdiyev.walletai.ui.theme.WalletAiTheme

import kz.abdiyev.walletai.ui.viewmodel.AnalyticsChartMode

import kz.abdiyev.walletai.ui.viewmodel.AnalyticsViewModel

import kz.abdiyev.walletai.util.MoneyFormatter
import kz.abdiyev.walletai.util.Plurals
import kz.abdiyev.walletai.util.ShareHelper

import java.io.File



@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

@Composable

fun AnalyticsScreen(

    viewModel: AnalyticsViewModel,

    modifier: Modifier = Modifier

) {

    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()



    val currency = remember(uiState.currencyCode) { Currency.fromCode(uiState.currencyCode) }

    val financialColors = WalletAiTheme.financialColors

    val isExpenseMode = uiState.chartMode == AnalyticsChartMode.EXPENSE

    val mainAmountColor = if (isExpenseMode) financialColors.expense else financialColors.income

    val savingsColor = if (uiState.balanceMinor >= 0) financialColors.income else financialColors.expense



    val categoryShares = remember(uiState.categoryShares) {

        uiState.categoryShares.map { share ->

            CategoryShare(

                category = CategoryData(

                    id = share.categoryId,

                    name = share.name,

                    type = if (isExpenseMode) TransactionType.EXPENSE else TransactionType.INCOME,

                    colorHex = share.colorHex,

                    iconName = share.iconName

                ),

                amountMinor = share.amountMinor,

                percentage = share.percentage,

                color = parseColorHex(share.colorHex)

            )

        }

    }



    var exportSuccessFile by remember { mutableStateOf<File?>(null) }

    var exportType by remember { mutableStateOf("") }

    var exportMimeType by remember { mutableStateOf("text/csv") }

    var showPeriodPicker by remember { mutableStateOf(false) }

    val exportPeriodLabel = remember(uiState.period, uiState.customRange) { viewModel.exportPeriodLabel() }



    LazyColumn(

        modifier = modifier

            .fillMaxSize()

            .padding(horizontal = 16.dp),

        verticalArrangement = Arrangement.spacedBy(16.dp),

        contentPadding = PaddingValues(bottom = 24.dp, top = 16.dp)

    ) {

        item { ScreenTitle("Аналитика") }



        item {

            SegmentedPeriodSelector(

                selected = uiState.period,

                hasCustomRange = uiState.customRange != null,

                onSelectPreset = viewModel::setPeriod,

                onCustomClick = { showPeriodPicker = true }

            )

        }



        item {

            Text(

                text = uiState.periodRangeLabel,

                style = MaterialTheme.typography.labelMedium,

                color = MaterialTheme.colorScheme.onSurfaceVariant

            )

        }



        item {

            SegmentedToggle(

                options = listOf("Расходы", "Доходы"),

                selectedIndex = if (isExpenseMode) 0 else 1,

                onSelect = { index ->

                    viewModel.setChartMode(

                        if (index == 0) AnalyticsChartMode.EXPENSE else AnalyticsChartMode.INCOME

                    )

                }

            )

        }



        item {

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                Text(

                    text = uiState.heading,

                    style = MaterialTheme.typography.bodyMedium,

                    color = MaterialTheme.colorScheme.onSurfaceVariant

                )

                Text(

                    text = MoneyFormatter.format(Money(uiState.activeTotalMinor, currency)),

                    style = MaterialTheme.typography.headlineMedium,

                    fontWeight = FontWeight.Bold,

                    color = mainAmountColor

                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    InfoChip(

                        text = "↗ Доходы ${MoneyFormatter.format(Money(uiState.totalIncomeMinor, currency), showSign = false)}",

                        color = financialColors.income

                    )

                    InfoChip(

                        text = "↘ Расходы ${MoneyFormatter.format(Money(uiState.totalExpenseMinor, currency), showSign = false)}",

                        color = financialColors.expense

                    )

                    if (uiState.totalIncomeMinor > 0) {

                        InfoChip(

                            text = "Сбережения ${Plurals.formatSavingsRatePercent(uiState.savingsRate)}",

                            color = savingsColor

                        )

                    }

                }

            }

        }



        if (uiState.activeTotalMinor == 0L) {

            item {

                WalletSurfaceCard {

                    Column(

                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),

                        horizontalAlignment = Alignment.CenterHorizontally,

                        verticalArrangement = Arrangement.spacedBy(12.dp)

                    ) {

                        Text(

                            text = if (isExpenseMode) {

                                "Недостаточно данных расходов."

                            } else {

                                "Недостаточно данных доходов."

                            },

                            style = MaterialTheme.typography.bodyMedium,

                            color = MaterialTheme.colorScheme.onSurfaceVariant

                        )

                        if (uiState.hiddenByPeriodCount > 0) {

                            HiddenByPeriodHint(

                                hiddenCount = uiState.hiddenByPeriodCount,

                                onShowAll = { viewModel.setPeriod(Period.ALL) }

                            )

                        }

                    }

                }

            }

        } else {

            item {

                WalletSurfaceCard {

                    Column(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalAlignment = Alignment.CenterHorizontally,

                        verticalArrangement = Arrangement.spacedBy(16.dp)

                    ) {

                        DonutChart(

                            shares = categoryShares,

                            centerLabel = if (isExpenseMode) "Расходы" else "Доходы",

                            totalText = MoneyFormatter.format(Money(uiState.activeTotalMinor, currency), showSign = false),

                            ringColor = mainAmountColor

                        )

                        CategoryRankingList(

                            shares = categoryShares,

                            currency = currency,

                            amountColor = mainAmountColor

                        )

                    }

                }

            }

        }



        item {

            Text(

                text = "Экспорт за период: $exportPeriodLabel",

                style = MaterialTheme.typography.labelMedium,

                color = MaterialTheme.colorScheme.onSurfaceVariant

            )

        }



        item {

            Row(

                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement = Arrangement.spacedBy(12.dp)

            ) {

                OutlinedPill(

                    text = "Экспорт CSV",

                    icon = Icons.Default.Download,

                    onClick = {

                        viewModel.exportToCsv(context) { file ->

                            exportSuccessFile = file

                            exportType = "CSV"

                            exportMimeType = "text/csv"

                        }

                    },

                    modifier = Modifier.weight(1f)

                )

                OutlinedPill(

                    text = "Экспорт PDF",

                    icon = Icons.Default.Description,

                    onClick = {

                        viewModel.exportToPdf(context) { file ->

                            exportSuccessFile = file

                            exportType = "PDF"

                            exportMimeType = "application/pdf"

                        }

                    },

                    modifier = Modifier.weight(1f)

                )

            }

        }

    }



    if (showPeriodPicker) {
        PeriodRangePickerDialog(
            onDismiss = { showPeriodPicker = false },
            onConfirm = viewModel::applyCustomRange
        )
    }

    exportSuccessFile?.let { file ->

        AlertDialog(

            onDismissRequest = { exportSuccessFile = null },

            containerColor = MaterialTheme.colorScheme.surface,

            title = { Text("Экспорт завершён") },

            text = {

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Text("Отчёт $exportType ($exportPeriodLabel) создан: ${file.name}")

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

