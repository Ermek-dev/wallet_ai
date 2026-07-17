package kz.abdiyev.walletai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.abdiyev.walletai.R
import kz.abdiyev.walletai.ai.AdviceResult
import kz.abdiyev.walletai.ui.components.AdviceCard
import kz.abdiyev.walletai.ui.components.NumberedAdviceCard
import kz.abdiyev.walletai.ui.components.OutlinedPill
import kz.abdiyev.walletai.ui.components.PrimaryCta
import kz.abdiyev.walletai.ui.components.ScreenTitle
import kz.abdiyev.walletai.ui.components.SectionLabel
import kz.abdiyev.walletai.ui.components.StatCard
import kz.abdiyev.walletai.ui.components.WalletSurfaceCard
import kz.abdiyev.walletai.ui.theme.WalletAiTheme
import kz.abdiyev.walletai.ui.viewmodel.AdvisorUiState
import kz.abdiyev.walletai.ui.viewmodel.AdvisorViewModel
import kz.abdiyev.walletai.util.MoneyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun AdvisorScreen(
    viewModel: AdvisorViewModel,
    modifier: Modifier = Modifier
) {
    val advisorState by viewModel.advisorState.collectAsStateWithLifecycle()
    val stats by viewModel.statsState.collectAsStateWithLifecycle()
    val financialColors = WalletAiTheme.financialColors
    val monthName = remember {
        SimpleDateFormat("LLLL", Locale("ru")).format(Date()).replaceFirstChar {
            it.titlecase(Locale("ru"))
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ScreenTitle(stringResource(R.string.advisor_title))
                    Text(
                        text = stringResource(R.string.advisor_subtitle, monthName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedPill(
                    text = stringResource(R.string.advisor_refresh),
                    icon = Icons.Default.Refresh,
                    onClick = {
                        when (advisorState) {
                            is AdvisorUiState.Success -> viewModel.refreshAiFinancialAdvice()
                            else -> viewModel.loadAiFinancialAdvice()
                        }
                    }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = stringResource(R.string.advisor_income),
                    value = MoneyFormatter.format(stats.income, showSign = false),
                    accentColor = financialColors.income,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.advisor_expense),
                    value = MoneyFormatter.format(stats.expense, showSign = false),
                    accentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.advisor_savings),
                    value = "${stats.savingsRatePercent}%",
                    accentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (val state = advisorState) {
            AdvisorUiState.Idle -> {
                item {
                    AdviceCard(
                        icon = Icons.Default.AutoAwesome,
                        text = "Нажмите кнопку ниже — ИИ изучит ваши доходы и расходы и составит рекомендации."
                    )
                }
            }

            AdvisorUiState.Loading -> {
                item {
                    WalletSurfaceCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Анализируем транзакции…",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Обычно это занимает 5–10 секунд.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            is AdvisorUiState.Success -> {
                val paragraphs = state.result.text
                    .split("\n")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                if (state.result is AdviceResult.Ai && state.result.fromCache) {
                    item {
                        Text(
                            text = formatCacheAge(state.result.cachedAtMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (paragraphs.isNotEmpty()) {
                    item {
                        SectionLabel(stringResource(R.string.advisor_section_analysis))
                    }
                    item {
                        WalletSurfaceCard {
                            Text(
                                text = paragraphs.first(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (paragraphs.size > 1) {
                    item {
                        SectionLabel(stringResource(R.string.advisor_section_tips))
                    }
                    itemsIndexed(paragraphs.drop(1)) { index, paragraph ->
                        NumberedAdviceCard(number = index + 1, text = paragraph)
                    }
                }

                if (state.result is AdviceResult.Fallback) {
                    item {
                        Text(
                            text = state.result.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is AdvisorUiState.Error -> {
                item {
                    WalletSurfaceCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Ошибка анализа",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        item {
            PrimaryCta(
                text = stringResource(R.string.advisor_get_analysis),
                icon = Icons.Default.AutoAwesome,
                onClick = { viewModel.loadAiFinancialAdvice() },
                enabled = advisorState !is AdvisorUiState.Loading
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = advisorState !is AdvisorUiState.Loading,
                        onClick = viewModel::loadLocalFinancialAdvice
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.advisor_local_report),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatCacheAge(cachedAtMillis: Long?): String {
    if (cachedAtMillis == null || cachedAtMillis <= 0L) {
        return "Совет из кэша"
    }
    val elapsedHours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - cachedAtMillis)
        .coerceAtLeast(1)
    return "Совет из кэша, $elapsedHours ч назад"
}
