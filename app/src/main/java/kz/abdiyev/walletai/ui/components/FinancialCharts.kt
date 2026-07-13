package kz.abdiyev.walletai.ui.components



import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.animation.core.tween

import androidx.compose.foundation.Canvas

import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableFloatStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

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

import kz.abdiyev.walletai.domain.model.Currency

import kz.abdiyev.walletai.domain.model.Money

import kz.abdiyev.walletai.util.MoneyFormatter



data class CategoryShare(

    val category: CategoryData,

    val amountMinor: Long,

    val percentage: Float,

    val color: Color

)



@Composable

fun DonutChart(

    shares: List<CategoryShare>,

    centerLabel: String,

    totalText: String,

    ringColor: Color,

    modifier: Modifier = Modifier

) {

    val sharesKey = remember(shares) { shares.map { "${it.category.id}-${it.percentage}" }.joinToString() }

    var animationTarget by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(sharesKey) {

        animationTarget = 0f

        animationTarget = 1f

    }

    val animatePercent by animateFloatAsState(

        targetValue = animationTarget,

        animationSpec = tween(800),

        label = "donut"

    )



    Box(modifier = modifier, contentAlignment = Alignment.Center) {

        Canvas(modifier = Modifier.size(180.dp).padding(8.dp)) {

            val strokeWidth = 18.dp.toPx()

            val radius = (size.width - strokeWidth) / 2

            val center = Offset(size.width / 2, size.height / 2)

            drawCircle(

                color = ringColor.copy(alpha = 0.25f),

                radius = radius,

                center = center,

                style = Stroke(strokeWidth)

            )

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

            Text(

                text = centerLabel,

                style = MaterialTheme.typography.labelSmall,

                color = MaterialTheme.colorScheme.onSurfaceVariant

            )

            Text(

                text = totalText,

                style = MaterialTheme.typography.titleMedium,

                fontWeight = FontWeight.Bold,

                color = MaterialTheme.colorScheme.onSurface

            )

        }

    }

}



@Composable

fun CategoryRankingList(

    shares: List<CategoryShare>,

    currency: Currency,

    amountColor: Color,

    modifier: Modifier = Modifier

) {

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {

        shares.forEach { share ->

            Row(

                modifier = Modifier.fillMaxWidth(),

                verticalAlignment = Alignment.CenterVertically

            ) {

                CategoryBadge(
                    category = share.category,
                    color = share.color,
                    size = 32.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(

                    text = share.category.name,

                    style = MaterialTheme.typography.bodyMedium,

                    modifier = Modifier.weight(1f),

                    color = MaterialTheme.colorScheme.onSurface

                )

                Text(

                    text = "${share.percentage.toInt()}%",

                    style = MaterialTheme.typography.bodySmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant

                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(

                    text = MoneyFormatter.format(Money(share.amountMinor, currency), showSign = false),

                    style = MaterialTheme.typography.bodyMedium,

                    fontWeight = FontWeight.SemiBold,

                    color = amountColor

                )

            }

        }

    }

}



@Composable

fun ProgressBarChart(

    shares: List<CategoryShare>,

    currency: Currency,

    modifier: Modifier = Modifier

) {

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {

        shares.sortedByDescending { it.amountMinor }.forEach { share ->

            WalletSurfaceCard {

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement = Arrangement.SpaceBetween,

                        verticalAlignment = Alignment.CenterVertically

                    ) {

                        Text(

                            text = share.category.name,

                            style = MaterialTheme.typography.bodyMedium,

                            fontWeight = FontWeight.Medium,

                            color = MaterialTheme.colorScheme.onSurface

                        )

                        Text(

                            text = "${MoneyFormatter.format(Money(share.amountMinor, currency))} · ${share.percentage.toInt()}%",

                            style = MaterialTheme.typography.bodySmall,

                            color = MaterialTheme.colorScheme.onSurfaceVariant

                        )

                    }

                    Box(

                        modifier = Modifier

                            .fillMaxWidth()

                            .height(6.dp)

                            .clip(CircleShape)

                            .background(MaterialTheme.colorScheme.surfaceVariant)

                    ) {

                        Box(

                            modifier = Modifier

                                .fillMaxWidth(share.percentage / 100f)

                                .height(6.dp)

                                .clip(CircleShape)

                                .background(share.color)

                        )

                    }

                }

            }

        }

    }

}

