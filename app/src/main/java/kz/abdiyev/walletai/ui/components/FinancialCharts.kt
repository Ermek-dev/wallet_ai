package kz.abdiyev.walletai.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.util.MoneyFormatter

data class CategoryShare(
    val category: CategoryData,
    val amountMinor: Long,
    val percentage: Float,
    val color: Color
)

@Composable
fun DonutChart(shares: List<CategoryShare>, totalText: String, modifier: Modifier = Modifier) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatePercent by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(1000)
    )
    LaunchedEffect(Unit) { animationPlayed = true }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(160.dp).padding(8.dp)) {
            val strokeWidth = 16.dp.toPx()
            val radius = (size.width - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(color = Color.LightGray.copy(alpha = 0.2f), radius = radius, center = center, style = Stroke(strokeWidth))
            var startAngle = -90f
            for (share in shares) {
                val sweepAngle = share.percentage * 3.6f * animatePercent
                if (sweepAngle > 0.1f) {
                    drawArc(
                        color = share.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2)
                    )
                    startAngle += share.percentage * 3.6f
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Всего трат", style = MaterialTheme.typography.labelSmall)
            Text(totalText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProgressBarChart(shares: List<CategoryShare>, currency: kz.abdiyev.walletai.domain.model.Currency, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        shares.sortedByDescending { it.amountMinor }.forEach { share ->
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(share.color))
                        Text(share.category.name, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        "${MoneyFormatter.format(Money(share.amountMinor, currency))} (${"%.1f".format(share.percentage)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Box(
                        Modifier.fillMaxHeight().fillMaxWidth(share.percentage / 100f)
                            .clip(RoundedCornerShape(4.dp)).background(share.color)
                    )
                }
            }
        }
    }
}
