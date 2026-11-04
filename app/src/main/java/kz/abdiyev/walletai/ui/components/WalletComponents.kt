package kz.abdiyev.walletai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kz.abdiyev.walletai.domain.category.displayName
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Period
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ui.theme.CardShape
import kz.abdiyev.walletai.ui.theme.FabDark
import kz.abdiyev.walletai.ui.theme.PillShape
import kz.abdiyev.walletai.ui.theme.SmallShape
import kz.abdiyev.walletai.util.MoneyFormatter
import kz.abdiyev.walletai.util.Plurals
import java.text.SimpleDateFormat

@Composable
fun MonthHeader(
    monthLabel: String,
    initials: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.take(2).ifBlank { "?" }.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

@Composable
fun CategoryBadge(
    category: CategoryData?,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getCategoryIcon(category?.iconName ?: ""),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(size * 0.45f)
        )
    }
}

@Composable
fun SegmentedPeriodSelector(
    selected: Period,
    hasCustomRange: Boolean,
    onSelectPreset: (Period) -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = true
) {
    val options = listOf("Период", "Неделя", "Месяц", "Всё")
    val selectedIndex = when {
        selected == Period.CUSTOM && hasCustomRange -> 0
        selected == Period.WEEK -> 1
        selected == Period.MONTH -> 2
        selected == Period.ALL -> 3
        else -> 2
    }
    SegmentedToggle(
        options = options,
        selectedIndex = selectedIndex,
        onSelect = { index ->
            when (index) {
                0 -> onCustomClick()
                1 -> onSelectPreset(Period.WEEK)
                2 -> onSelectPreset(Period.MONTH)
                3 -> onSelectPreset(Period.ALL)
            }
        },
        modifier = modifier,
        compact = compact,
        style = SegmentedToggleStyle.Secondary
    )
}

enum class SegmentedToggleStyle {
    Primary,
    Secondary
}

@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    style: SegmentedToggleStyle = SegmentedToggleStyle.Primary
) {
    val haptic = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()
    val verticalPadding = if (compact) 6.dp else 10.dp
    val textStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val containerColor = when (style) {
        SegmentedToggleStyle.Primary -> MaterialTheme.colorScheme.surfaceVariant
        SegmentedToggleStyle.Secondary -> MaterialTheme.colorScheme.surface
    }
    val selectedColors = when (style) {
        SegmentedToggleStyle.Primary -> when {
            isDark -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
            else -> FabDark to Color.White
        }
        SegmentedToggleStyle.Secondary -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(containerColor)
            .then(
                if (style == SegmentedToggleStyle.Secondary) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), PillShape)
                } else {
                    Modifier
                }
            )
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(PillShape)
                        .background(
                            if (selected) selectedColors.first else Color.Transparent
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(index)
                        }
                        .padding(vertical = verticalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = textStyle,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) selectedColors.second else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRow(
    transaction: TransactionData,
    category: CategoryData?,
    categoryColor: Color,
    incomeColor: Color,
    expenseColor: Color,
    onClick: () -> Unit = {},
    onDelete: () -> Unit = {},
    showDeleteButton: Boolean = true,
    modifier: Modifier = Modifier,
    categoriesById: Map<Long, CategoryData> = emptyMap()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(category = category, color = categoryColor, size = 44.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = category?.displayName(categoriesById) ?: "Без категории",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = MoneyFormatter.format(transaction.money, showSign = true, type = transaction.type),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (transaction.type == TransactionType.INCOME) incomeColor else expenseColor,
            modifier = Modifier.semantics {
                contentDescription = when (transaction.type) {
                    TransactionType.INCOME -> "Доход ${MoneyFormatter.format(transaction.money, showSign = false)}"
                    TransactionType.EXPENSE -> "Расход ${MoneyFormatter.format(transaction.money, showSign = false)}"
                }
            }
        )
        if (showDeleteButton) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun DayGroupCard(
    dayGroups: List<Pair<String, List<TransactionData>>>,
    categories: List<CategoryData>,
    incomeColor: Color,
    expenseColor: Color,
    onTransactionClick: (TransactionData) -> Unit,
    onTransactionDelete: (TransactionData) -> Unit,
    parseColorHex: (String) -> Color,
    modifier: Modifier = Modifier,
    showDeleteButton: Boolean = true
) {
    val categoriesById = categories.associateBy { it.id }
    WalletSurfaceCard(modifier = modifier) {
        dayGroups.forEachIndexed { groupIndex, (dayLabel, transactions) ->
            if (groupIndex > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            transactions.forEach { tx ->
                val category = categories.find { it.id == tx.categoryId }
                val color = category?.let { parseColorHex(it.colorHex) } ?: Color.Gray
                TransactionRow(
                    transaction = tx,
                    category = category,
                    categoryColor = color,
                    incomeColor = incomeColor,
                    expenseColor = expenseColor,
                    onClick = { onTransactionClick(tx) },
                    onDelete = { onTransactionDelete(tx) },
                    showDeleteButton = showDeleteButton,
                    categoriesById = categoriesById
                )
            }
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    value: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = it,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        value?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        trailing?.invoke()
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
fun StatCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
fun NumberedAdviceCard(
    number: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    WalletSurfaceCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AdviceCard(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    WalletSurfaceCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PrimaryCta(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun OutlinedPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DarkActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(
                when {
                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    isDark -> MaterialTheme.colorScheme.surfaceVariant
                    else -> FabDark
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.White
        )
    }
}

@Composable
fun WalletSurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .tiltGlow(shape = CardShape),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun BalanceSummaryRow(
    incomesText: String,
    expensesText: String,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "↗ $incomesText",
            style = MaterialTheme.typography.bodySmall,
            color = incomeColor,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "↘ $expensesText",
            style = MaterialTheme.typography.bodySmall,
            color = expenseColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun HiddenByPeriodHint(
    hiddenCount: Int,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (hiddenCount <= 0) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = Plurals.hiddenOutsidePeriodHint(hiddenCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Показать всё",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onShowAll)
        )
    }
}

@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    color: Color? = null
) {
    val chipColor = color ?: if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = chipColor,
        modifier = modifier
            .tiltGlow(shape = SmallShape, intensity = 0.2f, maxShiftDp = 4.dp)
            .clip(SmallShape)
            .background(
                if (color != null) color.copy(alpha = 0.12f)
                else if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
