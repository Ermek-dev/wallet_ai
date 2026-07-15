package kz.abdiyev.walletai.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getCategoryIcon(iconName: String): ImageVector = when (iconName) {
    "shopping_cart" -> Icons.Default.ShoppingCart
    "home" -> Icons.Default.Home
    "restaurant" -> Icons.Default.Restaurant
    "commute" -> Icons.Default.DirectionsCar
    "movie" -> Icons.Default.Movie
    "healing" -> Icons.Default.MedicalServices
    "payments" -> Icons.Default.Payments
    "subscriptions" -> Icons.Default.Star
    "swap_horiz" -> Icons.Default.SwapHoriz
    else -> Icons.Default.Paid
}

val categoryIconOptions = listOf(
    "shopping_cart", "home", "restaurant", "commute", "movie", "healing", "payments", "subscriptions", "swap_horiz", "paid"
)

val categoryColorOptions = listOf(
    "#EF5350", "#42A5F5", "#FFA726", "#26A69A", "#AB47BC", "#66BB6A", "#78909C", "#9CCC65", "#757575"
)
