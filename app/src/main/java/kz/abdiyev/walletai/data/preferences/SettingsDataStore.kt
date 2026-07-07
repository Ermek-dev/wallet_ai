package kz.abdiyev.walletai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kz.abdiyev.walletai.domain.model.Currency

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

    val currencyCode: Flow<String> = context.dataStore.data.map { it[Keys.CURRENCY] ?: Currency.KZT.code }
    val userName: Flow<String> = context.dataStore.data.map { it[Keys.USER_NAME] ?: "" }
    val themeMode: Flow<String> = context.dataStore.data.map { it[Keys.THEME_MODE] ?: "system" }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    val demoLoaded: Flow<Boolean> = context.dataStore.data.map { it[Keys.DEMO_LOADED] ?: false }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: false }
    val notificationHour: Flow<Int> = context.dataStore.data.map { it[Keys.NOTIFICATION_HOUR] ?: 21 }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.BIOMETRIC_ENABLED] ?: false }

    suspend fun setCurrency(code: String) = context.dataStore.edit { it[Keys.CURRENCY] = code }
    suspend fun setUserName(name: String) = context.dataStore.edit { it[Keys.USER_NAME] = name }
    suspend fun setThemeMode(mode: String) = context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    suspend fun setOnboardingDone(done: Boolean) = context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    suspend fun setDemoLoaded(loaded: Boolean) = context.dataStore.edit { it[Keys.DEMO_LOADED] = loaded }
    suspend fun setNotificationsEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    suspend fun setNotificationHour(hour: Int) = context.dataStore.edit { it[Keys.NOTIFICATION_HOUR] = hour }
    suspend fun setBiometricEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
}
