package kz.abdiyev.walletai.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kz.abdiyev.walletai.domain.model.PeriodRange
import kz.abdiyev.walletai.util.PeriodFilter
import kz.abdiyev.walletai.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (PeriodRange) -> Unit
) {
    val state = rememberDateRangePickerState()
    val startMillis = state.selectedStartDateMillis
    val endMillis = state.selectedEndDateMillis
    val canConfirm = startMillis != null
    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale("ru")) }

    val hintText = when {
        startMillis != null && endMillis != null -> "Нажмите «Сохранить», чтобы применить диапазон"
        startMillis != null -> "Выберите дату окончания или сохраните один день"
        else -> "Сначала выберите дату начала, затем дату окончания"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxHeight()) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(
                        text = "Выберите период",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = hintText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                RussianLocaleContainer {
                    DateRangePicker(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        showModeToggle = false,
                        title = {
                            Text(
                                text = "Даты",
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        },
                        headline = {
                            val headlineText = when {
                                startMillis != null && endMillis != null ->
                                    "${dateFormatter.format(Date(startMillis))} — ${dateFormatter.format(Date(endMillis))}"
                                startMillis != null -> "С ${dateFormatter.format(Date(startMillis))}"
                                else -> "Начало — конец"
                            }
                            Text(
                                text = headlineText,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    Button(
                        enabled = canConfirm,
                        onClick = {
                            val start = startMillis ?: return@Button
                            val end = endMillis ?: start
                            val offset = TimeUtils.localOffsetMillis()
                            onConfirm(PeriodFilter.customRangeFromLocalDays(start, end, offset))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

@Composable
private fun RussianLocaleContainer(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val russianConfiguration = Configuration(context.resources.configuration).apply {
        setLocale(Locale("ru", "RU"))
    }
    CompositionLocalProvider(LocalConfiguration provides russianConfiguration) {
        content()
    }
}
