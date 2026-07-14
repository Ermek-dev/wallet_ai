package kz.abdiyev.walletai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.ThemeMode
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.notification.ReminderScheduler
import kz.abdiyev.walletai.ui.components.CategoryBadge
import kz.abdiyev.walletai.ui.components.PrimaryCta
import kz.abdiyev.walletai.ui.components.ScreenTitle
import kz.abdiyev.walletai.ui.components.SegmentedToggle
import kz.abdiyev.walletai.ui.components.SettingsRow
import kz.abdiyev.walletai.ui.components.WalletSurfaceCard
import kz.abdiyev.walletai.ui.components.categoryColorOptions
import kz.abdiyev.walletai.ui.components.categoryIconOptions
import kz.abdiyev.walletai.ui.components.getCategoryIcon
import kz.abdiyev.walletai.ui.theme.SmallShape
import kz.abdiyev.walletai.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToImport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheduler = remember { ReminderScheduler(context) }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    val currencyCode = settings.currencyCode
    val userName = settings.userName
    val themeMode = settings.themeMode
    val notificationsEnabled = settings.notificationsEnabled
    val notificationHour = settings.notificationHour
    val notificationMinute = settings.notificationMinute
    val currency = remember(currencyCode) { Currency.fromCode(currencyCode) }

    var localUserName by remember(userName) { mutableStateOf(userName) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryData?>(null) }
    var showAddCategory by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val notificationTimeLabel = remember(notificationHour, notificationMinute) {
        "%02d:%02d".format(notificationHour, notificationMinute)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.setNotificationsEnabled(true, scheduler)
    }

    val initials = remember(userName) { userName.trim().take(2).ifBlank { "?" }.uppercase() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        state = rememberLazyListState(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 16.dp)
    ) {
        item { ScreenTitle("Профиль") }

        item {
            WalletSurfaceCard(
                modifier = Modifier.clickable { isEditingProfile = !isEditingProfile }
            ) {
                if (isEditingProfile) {
                    OutlinedTextField(
                        value = localUserName,
                        onValueChange = { localUserName = it },
                        label = { Text("Ваше имя") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = SmallShape
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Currency.entries.forEach { c ->
                            FilterChip(
                                selected = currencyCode == c.code,
                                onClick = { viewModel.setCurrency(c.code) },
                                label = { Text("${c.symbol} ${c.code}") }
                            )
                        }
                    }
                    PrimaryCta(
                        text = "Сохранить",
                        onClick = {
                            viewModel.setUserName(localUserName.trim())
                            isEditingProfile = false
                        },
                        enabled = localUserName.isNotBlank()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName.trim().ifBlank { "Пользователь" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${currency.code} · ${currency.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            WalletSurfaceCard {
                SettingsRow(
                    icon = Icons.Default.DarkMode,
                    title = "Тема",
                    subtitle = when (themeMode) {
                        ThemeMode.SYSTEM -> "Системная"
                        ThemeMode.LIGHT -> "Светлая"
                        ThemeMode.DARK -> "Тёмная"
                    }
                )
                SegmentedToggle(
                    options = listOf("Авто", "Свет", "Тёмн"),
                    selectedIndex = when (themeMode) {
                        ThemeMode.SYSTEM -> 0
                        ThemeMode.LIGHT -> 1
                        ThemeMode.DARK -> 2
                    },
                    onSelect = { index ->
                        viewModel.setThemeMode(
                            when (index) {
                                0 -> ThemeMode.SYSTEM
                                1 -> ThemeMode.LIGHT
                                else -> ThemeMode.DARK
                            }
                        )
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsRow(
                    icon = Icons.Default.Notifications,
                    title = "Вечернее напоминание",
                    subtitle = notificationTimeLabel,
                    onClick = { showTimePicker = true }
                ) {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        viewModel.setNotificationsEnabled(true, scheduler)
                                    } else {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    viewModel.setNotificationsEnabled(true, scheduler)
                                }
                            } else {
                                viewModel.setNotificationsEnabled(false, scheduler)
                            }
                        }
                    )
                }
                Text(
                    text = "Android может доставить напоминание немного позже выбранного времени.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsRow(
                    icon = Icons.Default.UploadFile,
                    title = "Импорт выписки",
                    showChevron = true,
                    onClick = onNavigateToImport
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Категории",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showAddCategory = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить категорию")
                }
            }
        }

        items(categories, key = { it.id }) { category ->
            val color = parseColorHex(category.colorHex)
            WalletSurfaceCard(
                modifier = Modifier.clickable { editingCategory = category }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryBadge(category = category, color = color, size = 40.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(category.name, fontWeight = FontWeight.Medium)
                        Text(
                            if (category.type == TransactionType.INCOME) "Доход" else "Расход",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!category.isSystem) {
                        IconButton(onClick = { viewModel.deleteCategory(category) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        item {
            WalletSurfaceCard {
                Text("Данные", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Сброс к демо-данным", fontWeight = FontWeight.Medium)
                        Text(
                            "Загрузить 3 месяца примеров",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.resetToDemoData() }) {
                        Icon(Icons.Default.Restore, contentDescription = "Демо", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Button(
                    onClick = { showDeleteAllDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Удалить все данные")
                }
            }
        }
    }

    if (showAddCategory || editingCategory != null) {
        CategoryEditDialog(
            existing = editingCategory,
            onDismiss = {
                showAddCategory = false
                editingCategory = null
            },
            onSave = { category ->
                viewModel.saveCategory(category)
                showAddCategory = false
                editingCategory = null
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Удалить все данные?") },
            text = { Text("Все транзакции будут удалены без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAllDialog = false
                    showDeleteConfirmDialog = true
                }) {
                    Text("Продолжить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Подтвердите удаление") },
            text = { Text("Это действие необратимо. Вы уверены?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllData()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить навсегда")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = notificationHour,
            initialMinute = notificationMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Время напоминания") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setNotificationTime(
                        timePickerState.hour,
                        timePickerState.minute,
                        scheduler
                    )
                    showTimePicker = false
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditDialog(
    existing: CategoryData?,
    onDismiss: () -> Unit,
    onSave: (CategoryData) -> Unit
) {
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var type by remember(existing) { mutableStateOf(existing?.type ?: TransactionType.EXPENSE) }
    var colorHex by remember(existing) { mutableStateOf(existing?.colorHex ?: categoryColorOptions.first()) }
    var iconName by remember(existing) { mutableStateOf(existing?.iconName ?: categoryIconOptions.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(if (existing == null) "Новая категория" else "Редактировать категорию") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = SmallShape
                )
                SegmentedToggle(
                    options = listOf("Расход", "Доход"),
                    selectedIndex = if (type == TransactionType.EXPENSE) 0 else 1,
                    onSelect = { index ->
                        type = if (index == 0) TransactionType.EXPENSE else TransactionType.INCOME
                    }
                )
                Text("Цвет", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryColorOptions.forEach { hex ->
                        val selected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseColorHex(hex))
                                .clickable { colorHex = hex }
                                .then(if (selected) Modifier.padding(2.dp) else Modifier)
                        )
                    }
                }
                Text("Иконка", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categoryIconOptions.forEach { icon ->
                        FilterChip(
                            selected = iconName == icon,
                            onClick = { iconName = icon },
                            label = { Icon(getCategoryIcon(icon), contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            PrimaryCta(
                text = "Сохранить",
                onClick = {
                    if (name.isBlank()) return@PrimaryCta
                    onSave(
                        CategoryData(
                            id = existing?.id ?: 0L,
                            name = name.trim(),
                            type = type,
                            colorHex = colorHex,
                            iconName = iconName,
                            isSystem = existing?.isSystem ?: false
                        )
                    )
                },
                enabled = name.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
