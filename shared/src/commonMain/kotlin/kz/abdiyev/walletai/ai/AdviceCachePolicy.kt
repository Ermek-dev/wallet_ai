package kz.abdiyev.walletai.ai

object AdviceCachePolicy {

    const val TTL_MILLIS: Long = 24 * 60 * 60 * 1000L

    fun promptHash(prompt: String): String {
        var hash = 2166136261L
        for (char in prompt) {
            hash = hash xor char.code.toLong()
            hash *= 16777619L
        }
        return hash.toULong().toString(16)
    }

    fun isValid(
        cachedPromptHash: String?,
        cachedTimestampMillis: Long,
        currentPromptHash: String,
        nowMillis: Long,
        forceRefresh: Boolean
    ): Boolean {
        if (forceRefresh) return false
        if (cachedPromptHash.isNullOrBlank() || cachedTimestampMillis <= 0L) return false
        if (cachedPromptHash != currentPromptHash) return false
        return (nowMillis - cachedTimestampMillis) < TTL_MILLIS
    }
}
