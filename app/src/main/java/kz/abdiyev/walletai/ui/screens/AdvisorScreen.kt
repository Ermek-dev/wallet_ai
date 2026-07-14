package kz.abdiyev.walletai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.abdiyev.walletai.ai.AdviceResult
import kz.abdiyev.walletai.ui.components.AdviceCard
import kz.abdiyev.walletai.ui.components.PrimaryCta
import kz.abdiyev.walletai.ui.components.ScreenTitle
import kz.abdiyev.walletai.ui.components.WalletSurfaceCard
import kz.abdiyev.walletai.ui.viewmodel.AdvisorUiState
import kz.abdiyev.walletai.ui.viewmodel.AdvisorViewModel
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
    val monthName = remember {
        SimpleDateFormat("LLLL", Locale("ru")).format(Date()).replaceFirstChar { it.titlecase(Locale("ru")) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ScreenTitle("ИИ-советник")
                Text(
                    text = "Groq · анализ за $monthName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

                val icons = listOf(
                    Icons.Default.AutoAwesome,
                    Icons.Default.Analytics,
                    Icons.Default.PlayArrow
                )

                if (state.result is AdviceResult.Ai && state.result.fromCache) {
                    item {
                        Text(
                            text = formatCacheAge(state.result.cachedAtMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                itemsIndexed(paragraphs) { index, paragraph ->
                    AdviceCard(
                        icon = icons.getOrElse(index) { Icons.Default.Lightbulb },
                        text = paragraph
                    )
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

                if (state.result is AdviceResult.Ai) {
                    item {
                        TextButton(
                            onClick = viewModel::refreshAiFinancialAdvice,
                            enabled = advisorState !is AdvisorUiState.Loading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "  Обновить анализ",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
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
                text = "Получить полный ИИ-анализ",
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
                    text = "Локальный отчёт без сети",
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
