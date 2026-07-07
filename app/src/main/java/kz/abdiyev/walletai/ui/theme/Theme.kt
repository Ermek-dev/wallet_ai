package kz.abdiyev.walletai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WalletTealLight,
    onPrimary = Color.Black,
    primaryContainer = WalletTealDark,
    onPrimaryContainer = Color.White,
    secondary = WalletTealLight,
    tertiary = WalletTealLight
)

private val LightColorScheme = lightColorScheme(
    primary = WalletTeal,
    onPrimary = Color.White,
    primaryContainer = WalletTealLight.copy(alpha = 0.3f),
    onPrimaryContainer = WalletTealDark,
    secondary = WalletTeal,
    tertiary = WalletTeal
)

data class FinancialColors(
    val income: Color,
    val expense: Color
)

val LocalFinancialColors = staticCompositionLocalOf {
    FinancialColors(income = IncomeGreen, expense = ExpenseRed)
}

@Composable
fun WalletAiTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val financialColors = if (darkTheme) {
        FinancialColors(income = IncomeGreenDark, expense = ExpenseRedDark)
    } else {
        FinancialColors(income = IncomeGreen, expense = ExpenseRed)
    }

    CompositionLocalProvider(LocalFinancialColors provides financialColors) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}

object WalletAiTheme {
    val financialColors: FinancialColors
        @Composable
        get() = LocalFinancialColors.current
}
