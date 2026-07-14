package kz.abdiyev.walletai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kz.abdiyev.walletai.domain.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Color(0xFF0D1A12),
    primaryContainer = Color(0xFF1A3D2A),
    onPrimaryContainer = EmeraldPrimaryDark,
    secondary = EmeraldPrimaryDark,
    onSecondary = Color(0xFF0D1A12),
    tertiary = EmeraldPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF3A3F3C),
    error = ExpenseDark,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F0E4),
    onPrimaryContainer = Color(0xFF0D4A2E),
    secondary = EmeraldPrimary,
    onSecondary = Color.White,
    tertiary = EmeraldPrimary,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFD8DAD8),
    error = ExpenseLight,
    onError = Color.White
)

data class FinancialColors(
    val income: Color,
    val expense: Color
)

val LocalFinancialColors = staticCompositionLocalOf {
    FinancialColors(income = IncomeLight, expense = ExpenseLight)
}

@Composable
fun WalletAiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val financialColors = if (darkTheme) {
        FinancialColors(income = IncomeDark, expense = ExpenseDark)
    } else {
        FinancialColors(income = IncomeLight, expense = ExpenseLight)
    }

    CompositionLocalProvider(LocalFinancialColors provides financialColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = WalletShapes,
            content = content
        )
    }
}

object WalletAiTheme {
    val financialColors: FinancialColors
        @Composable
        get() = LocalFinancialColors.current
}
