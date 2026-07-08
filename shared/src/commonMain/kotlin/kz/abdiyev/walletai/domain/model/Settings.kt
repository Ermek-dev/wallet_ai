package kz.abdiyev.walletai.domain.model

data class Settings(
    val currencyCode: String = Currency.KZT.code,
    val userName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingDone: Boolean = false,
    val demoLoaded: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val notificationHour: Int = 21,
    val biometricEnabled: Boolean = false
)
