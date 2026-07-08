package kz.abdiyev.walletai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kz.abdiyev.walletai.domain.model.Settings
import kz.abdiyev.walletai.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "walletai_settings")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val CURRENCY = stringPreferencesKey("currency_code")
        val USER_NAME = stringPreferencesKey("user_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val DEMO_LOADED = booleanPreferencesKey("demo_loaded")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            currencyCode = prefs[Keys.CURRENCY] ?: Settings().currencyCode,
            userName = prefs[Keys.USER_NAME] ?: "",
            themeMode = ThemeMode.fromStorageValue(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.toStorageValue()),
            onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
            demoLoaded = prefs[Keys.DEMO_LOADED] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            notificationHour = prefs[Keys.NOTIFICATION_HOUR] ?: 21,
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false
        )
    }

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { prefs ->
            val current = Settings(
                currencyCode = prefs[Keys.CURRENCY] ?: Settings().currencyCode,
                userName = prefs[Keys.USER_NAME] ?: "",
                themeMode = ThemeMode.fromStorageValue(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.toStorageValue()),
                onboardingDone = prefs[Keys.ONBOARDING_DONE] ?: false,
                demoLoaded = prefs[Keys.DEMO_LOADED] ?: false,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
                notificationHour = prefs[Keys.NOTIFICATION_HOUR] ?: 21,
                biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false
            )
            val updated = transform(current)
            prefs[Keys.CURRENCY] = updated.currencyCode
            prefs[Keys.USER_NAME] = updated.userName
            prefs[Keys.THEME_MODE] = updated.themeMode.toStorageValue()
            prefs[Keys.ONBOARDING_DONE] = updated.onboardingDone
            prefs[Keys.DEMO_LOADED] = updated.demoLoaded
            prefs[Keys.NOTIFICATIONS_ENABLED] = updated.notificationsEnabled
            prefs[Keys.NOTIFICATION_HOUR] = updated.notificationHour
            prefs[Keys.BIOMETRIC_ENABLED] = updated.biometricEnabled
        }
    }
}
