package kz.abdiyev.walletai.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.adviceCacheStore: DataStore<Preferences> by preferencesDataStore(name = "walletai_advice_cache")

data class AdviceCacheEntry(
    val adviceText: String,
    val promptHash: String,
    val timestampMillis: Long
)

class AdviceCacheDataStore(private val context: Context) {
    private object Keys {
        val ADVICE_TEXT = stringPreferencesKey("advice_text")
        val PROMPT_HASH = stringPreferencesKey("advice_prompt_hash")
        val TIMESTAMP = longPreferencesKey("advice_timestamp")
    }

    suspend fun get(): AdviceCacheEntry? {
        val prefs = context.adviceCacheStore.data.first()
        val text = prefs[Keys.ADVICE_TEXT] ?: return null
        val hash = prefs[Keys.PROMPT_HASH] ?: return null
        val timestamp = prefs[Keys.TIMESTAMP] ?: return null
        return AdviceCacheEntry(text, hash, timestamp)
    }

    suspend fun save(entry: AdviceCacheEntry) {
        context.adviceCacheStore.edit { prefs ->
            prefs[Keys.ADVICE_TEXT] = entry.adviceText
            prefs[Keys.PROMPT_HASH] = entry.promptHash
            prefs[Keys.TIMESTAMP] = entry.timestampMillis
        }
    }

    suspend fun clear() {
        context.adviceCacheStore.edit { it.clear() }
    }
}
