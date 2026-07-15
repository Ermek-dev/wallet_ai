package kz.abdiyev.walletai.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun toStorageValue(): String = when (this) {
        SYSTEM -> "system"
        LIGHT -> "light"
        DARK -> "dark"
    }

    companion object {
        fun fromStorageValue(value: String): ThemeMode = when (value) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }
}
