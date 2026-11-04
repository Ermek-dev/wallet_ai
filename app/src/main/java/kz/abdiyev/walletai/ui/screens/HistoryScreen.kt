package kz.abdiyev.walletai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.abdiyev.walletai.R
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.ui.components.DayGroupCard
import kz.abdiyev.walletai.ui.components.WalletEmptyState
import kz.abdiyev.walletai.ui.theme.SmallShape
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
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<kz.abdiyev.walletai.domain.model.TransactionData?>(null) }
    var onlyReview by remember { mutableStateOf(false) }

    val financialColors = WalletAiTheme.financialColors
    val groupedHistory = remember(searchResults, onlyReview) {
        groupTransactionsByDay(if (onlyReview) searchResults.filter { it.needsReview } else searchResults)
    }
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale("ru")) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("История операций", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.history_back))
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                androidx.compose.material3.FilterChip(
                    selected = onlyReview, onClick = { onlyReview = !onlyReview },
                    label = { Text("Нужно проверить (${searchResults.count { it.needsReview }})") }
                )
                if (saveError) Text("Не удалось сохранить исправления. Повторите попытку.", color = MaterialTheme.colorScheme.error)
            }
            item {
                OutlinedTextField(
                    value = historyQuery,
                    onValueChange = viewModel::setHistoryQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.history_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.history_search)) },
                    singleLine = true,
                    shape = SmallShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            if (groupedHistory.isEmpty()) {
                item {
                    WalletEmptyState(message = stringResource(R.string.history_empty))
                }
            } else {
                item {
                    DayGroupCard(
                        dayGroups = groupedHistory,
                        categories = categories,
                        incomeColor = financialColors.income,
                        expenseColor = financialColors.expense,
                        onTransactionClick = { editing = it },
                        onTransactionDelete = {},
                        parseColorHex = ::parseColorHex,
                        showDeleteButton = false
                    )
                }
            }
        }
    }
    editing?.let { tx ->
        TransactionDialog(
            categories = categories, currency = Currency.fromCode(settings.currencyCode), existing = tx,
            onDismiss = { editing = null },
            onConfirm = { money, type, categoryId, description, date ->
                viewModel.updateTransaction(tx.copy(money = money, type = type, categoryId = categoryId,
                    description = description, date = date))
                editing = null
            }
        )
    }
}
