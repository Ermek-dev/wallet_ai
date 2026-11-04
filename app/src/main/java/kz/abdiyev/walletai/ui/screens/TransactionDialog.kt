package kz.abdiyev.walletai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kz.abdiyev.walletai.R
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ml.VoiceInputParser
import kz.abdiyev.walletai.ui.components.PrimaryCta
import kz.abdiyev.walletai.ui.components.SegmentedToggle
import kz.abdiyev.walletai.ui.components.getCategoryIcon
import kz.abdiyev.walletai.ui.theme.SmallShape
import kz.abdiyev.walletai.ui.theme.WalletAiTheme
import kz.abdiyev.walletai.ui.voice.VoiceRecognizerHelper
import kz.abdiyev.walletai.util.MoneyFormatter
import java.util.Calendar

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
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val voiceParser = remember { VoiceInputParser() }

    var amountStr by remember(existing) {
        mutableStateOf(
            existing?.money?.majorAmount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
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
    var categoriesExpanded by remember { mutableStateOf(false) }
    var pendingVoiceStart by remember { mutableStateOf(false) }
    var voiceHelper by remember { mutableStateOf<VoiceRecognizerHelper?>(null) }

    val filteredCategories = categories.filter { it.type == type }
    val rootCategories = filteredCategories.filter { it.parentId == null }
    val categoriesById = remember(categories) { categories.associateBy { it.id } }

    var selectedRootId by remember(existing, type, categories) {
        mutableLongStateOf(
            existing?.let { tx ->
                categoriesById[tx.categoryId]?.parentId ?: tx.categoryId
            } ?: if (defaultCategoryId != 0L) {
                categoriesById[defaultCategoryId]?.let { cat ->
                    cat.parentId ?: cat.id
                } ?: rootCategories.firstOrNull()?.id ?: 0L
            } else {
                rootCategories.firstOrNull()?.id ?: 0L
            }
        )
    }

    val subcategories = filteredCategories.filter { it.parentId == selectedRootId }
    val resolvedCategoryId = when {
        subcategories.isEmpty() -> selectedRootId
        selectedCategoryId in subcategories.map { it.id } -> selectedCategoryId
        selectedCategoryId == selectedRootId -> selectedRootId
        else -> selectedCategoryId
    }

    val isEdit = existing != null
    val nowMillis = System.currentTimeMillis()
    val todayStart = startOfDay(nowMillis)
    val yesterdayStart = todayStart - DAY_MS
    val isTodaySelected = isSameDay(selectedDate, nowMillis)
    val isYesterdaySelected = isSameDay(selectedDate, yesterdayStart)
    val isCustomDateSelected = !isTodaySelected && !isYesterdaySelected

    val visibleRootCategories = if (categoriesExpanded || rootCategories.size <= 5) {
        rootCategories
    } else {
        rootCategories.take(5)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingVoiceStart = true
    }

    DisposableEffect(context) {
        onDispose {
            voiceHelper?.destroy()
            voiceHelper = null
        }
    }

    fun applyVoiceResult(text: String) {
        val result = voiceParser.parse(text, currency, nowMillis)
        result.money?.let { money ->
            val major = money.majorAmount
            amountStr = if (major % 1.0 == 0.0) major.toLong().toString() else major.toString()
        }
        type = result.type
        val roots = categories.filter { it.type == type && it.parentId == null }
        val matchedRoot = roots.firstOrNull { root ->
            result.description.contains(root.name, ignoreCase = true)
        } ?: roots.firstOrNull()
        matchedRoot?.let { root ->
            selectedRootId = root.id
            selectedCategoryId = root.id
        }
        if (result.description.isNotBlank()) {
            descStr = result.description
        }
        result.dateMillis?.let { selectedDate = it }
    }

    fun beginVoiceRecognition() {
        voiceHelper?.stopListening()
        voiceHelper = VoiceRecognizerHelper(
            context = context,
            onResult = { text -> applyVoiceResult(text) },
            onError = { }
        ).also { it.startListening() }
    }

    fun startVoiceInput() {
        if (!VoiceRecognizerHelper(context, {}, {}).isAvailable()) return
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        beginVoiceRecognition()
    }

    LaunchedEffect(pendingVoiceStart) {
        if (pendingVoiceStart) {
            pendingVoiceStart = false
            beginVoiceRecognition()
        }
    }

    val parsedMoney = Money.fromMajorString(amountStr, currency)
    val amountInvalid = amountStr.isNotBlank() && parsedMoney == null
    val formattedAmount = parsedMoney?.let { MoneyFormatter.format(it, showSign = false) } ?: "0 ${currency.symbol}"

    val ctaText = when {
        isEdit -> stringResource(R.string.add_op_cta_save, formattedAmount)
        type == TransactionType.INCOME -> stringResource(R.string.add_op_cta_income, formattedAmount)
        else -> stringResource(R.string.add_op_cta_expense, formattedAmount)
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SegmentedToggle(
                    options = listOf("Расход", "Доход"),
                    selectedIndex = if (type == TransactionType.EXPENSE) 0 else 1,
                    onSelect = { index ->
                        type = if (index == 0) TransactionType.EXPENSE else TransactionType.INCOME
                        val roots = categories.filter { it.type == type && it.parentId == null }
                        selectedRootId = roots.firstOrNull()?.id ?: 0L
                        selectedCategoryId = selectedRootId
                    }
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BasicTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            modifier = Modifier.weight(1f, fill = false),
                            textStyle = TextStyle(
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterEnd) {
                                    if (amountStr.isEmpty()) {
                                        Text(
                                            text = "0",
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currency.symbol,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = { startVoiceInput() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(financialColors.income.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = stringResource(R.string.voice_input),
                                tint = financialColors.income,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    if (amountInvalid) {
                        Text(
                            text = stringResource(R.string.add_op_amount_error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = stringResource(R.string.add_op_voice_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.add_op_date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isTodaySelected,
                            onClick = { selectedDate = nowMillis },
                            label = { Text(stringResource(R.string.add_op_today)) }
                        )
                        FilterChip(
                            selected = isYesterdaySelected,
                            onClick = { selectedDate = yesterdayStart },
                            label = { Text(stringResource(R.string.add_op_yesterday)) }
                        )
                        FilterChip(
                            selected = isCustomDateSelected,
                            onClick = { showDatePicker = true },
                            label = { Text(stringResource(R.string.add_op_pick_date)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.add_op_category),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visibleRootCategories.forEach { category ->
                            val isSelected = selectedRootId == category.id
                            val color = parseColorHex(category.colorHex)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedRootId = category.id
                                    selectedCategoryId = category.id
                                },
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
                                    selectedContainerColor = color.copy(alpha = 0.15f),
                                    selectedLabelColor = color
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    selectedBorderColor = financialColors.income
                                )
                            )
                        }
                        if (!categoriesExpanded && rootCategories.size > 5) {
                            FilterChip(
                                selected = false,
                                onClick = { categoriesExpanded = true },
                                label = { Text(stringResource(R.string.add_op_more)) }
                            )
                        }
                    }
                }

                if (subcategories.isNotEmpty()) {
                    Text(
                        text = "Уточнение",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategoryId == selectedRootId,
                            onClick = { selectedCategoryId = selectedRootId },
                            label = { Text("Без уточнения") }
                        )
                        subcategories.forEach { category ->
                            val isSelected = selectedCategoryId == category.id
                            val color = parseColorHex(category.colorHex)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryId = category.id },
                                label = { Text(category.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.2f),
                                    selectedLabelColor = color
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = descStr,
                    onValueChange = { descStr = it },
                    label = { Text(stringResource(R.string.add_op_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = SmallShape
                )
            }
        },
        confirmButton = {
            PrimaryCta(
                text = ctaText,
                onClick = {
                    val money = parsedMoney ?: return@PrimaryCta
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val description = descStr.trim().ifBlank {
                        categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Операция"
                    }
                    onConfirm(money, type, resolvedCategoryId, description, selectedDate)
                },
                enabled = parsedMoney != null && resolvedCategoryId != 0L
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

private const val DAY_MS = 86_400_000L

private fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun isSameDay(a: Long, b: Long): Boolean = startOfDay(a) == startOfDay(b)

internal fun parseColorHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color.Gray
}
