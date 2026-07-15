package kz.abdiyev.walletai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ui.components.PrimaryCta
import kz.abdiyev.walletai.ui.components.SegmentedToggle
import kz.abdiyev.walletai.ui.components.getCategoryIcon
import kz.abdiyev.walletai.ui.theme.PillShape
import kz.abdiyev.walletai.ui.theme.SmallShape
import kz.abdiyev.walletai.ui.theme.WalletAiTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDialog(
    categories: List<CategoryData>,
    currency: Currency,
    existing: TransactionData? = null,
    defaultCategoryId: Long = 0L,
    onDismiss: () -> Unit,
    onConfirm: (Money, TransactionType, Long, String, Long) -> Unit
) {
    val financialColors = WalletAiTheme.financialColors
    var amountStr by remember(existing) {
        mutableStateOf(existing?.money?.majorAmount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "")
    }
    var descStr by remember(existing) { mutableStateOf(existing?.description ?: "") }
    var type by remember(existing) { mutableStateOf(existing?.type ?: TransactionType.EXPENSE) }
    var selectedCategoryId by remember(existing, categories, type, defaultCategoryId) {
        mutableLongStateOf(
            existing?.categoryId
                ?: if (defaultCategoryId != 0L && categories.any { it.id == defaultCategoryId && it.type == type }) {
                    defaultCategoryId
                } else {
                    categories.firstOrNull { it.type == type }?.id
                        ?: categories.firstOrNull()?.id
                        ?: 0L
                }
        )
    }
    var selectedDate by remember(existing) { mutableLongStateOf(existing?.date ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale("ru")) }
    val filteredCategories = categories.filter { it.type == type }
    val isEdit = existing != null

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(if (isEdit) "Редактировать операцию" else "Новая операция") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SegmentedToggle(
                    options = listOf("Расход", "Доход"),
                    selectedIndex = if (type == TransactionType.EXPENSE) 0 else 1,
                    onSelect = { index ->
                        type = if (index == 0) TransactionType.EXPENSE else TransactionType.INCOME
                        selectedCategoryId = categories.firstOrNull { it.type == type }?.id
                            ?: selectedCategoryId
                    }
                )

                val amountInvalid = amountStr.isNotBlank() && Money.fromMajorString(amountStr, currency) == null
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Сумма (${currency.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    isError = amountInvalid,
                    supportingText = {
                        if (amountInvalid) {
                            Text("Введите сумму больше нуля, например 3400 или 3400,50")
                        }
                    },
                    shape = SmallShape
                )

                OutlinedTextField(
                    value = descStr,
                    onValueChange = { descStr = it },
                    label = { Text("Описание (необязательно)") },
                    placeholder = { Text("Например: Magnum, зарплата") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = SmallShape
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PillShape
                ) {
                    Text("Дата: ${dateFormatter.format(Date(selectedDate))}")
                }

                Text(
                    text = "Категория",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredCategories.forEach { category ->
                        val isSelected = selectedCategoryId == category.id
                        val color = parseColorHex(category.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryId = category.id },
                            label = { Text(category.name) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        getCategoryIcon(category.iconName),
                                        contentDescription = null,
                                        tint = color
                                    )
                                }
                            } else {
                                null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            val parsedMoney = Money.fromMajorString(amountStr, currency)
            PrimaryCta(
                text = if (isEdit) "Сохранить" else "Добавить",
                onClick = {
                    val money = parsedMoney ?: return@PrimaryCta
                    val description = descStr.trim().ifBlank {
                        categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Операция"
                    }
                    onConfirm(money, type, selectedCategoryId, description, selectedDate)
                },
                enabled = parsedMoney != null && selectedCategoryId != 0L
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

internal fun parseColorHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color.Gray
}
