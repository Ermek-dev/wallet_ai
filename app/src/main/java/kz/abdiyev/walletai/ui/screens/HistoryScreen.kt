package kz.abdiyev.walletai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.ui.theme.WalletAiTheme
import kz.abdiyev.walletai.ui.viewmodel.HistoryViewModel
import kz.abdiyev.walletai.util.groupTransactionsByDay
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val historyQuery by viewModel.historyQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val currency = remember(settings.currencyCode) { Currency.fromCode(settings.currencyCode) }
    val financialColors = WalletAiTheme.financialColors
    val groupedHistory = remember(searchResults) { groupTransactionsByDay(searchResults) }
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale("ru")) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("История операций") },
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = historyQuery,
                    onValueChange = viewModel::setHistoryQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Поиск по описанию") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }

            if (groupedHistory.isEmpty()) {
                item {
                    Text(
                        text = "Нет операций",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                groupedHistory.forEach { (dayLabel, txs) ->
                    item(key = "header-$dayLabel") {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(txs, key = { "history-${it.id}" }) { tx ->
                        val category = categories.find { it.id == tx.categoryId }
                        TransactionListItem(
                            transaction = tx,
                            category = category,
                            dateFormatter = dateFormatter,
                            incomeColor = financialColors.income,
                            expenseColor = financialColors.expense,
                            onClick = {},
                            onDelete = {}
                        )
                    }
                }
            }
        }
    }
}
