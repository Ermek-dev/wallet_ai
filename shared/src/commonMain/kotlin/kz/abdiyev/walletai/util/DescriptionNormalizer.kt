package kz.abdiyev.walletai.util

object DescriptionNormalizer {
    private val punctuationRegex = Regex("[^\\p{L}\\p{N}\\s]+")

    fun normalize(description: String): String =
        description
            .lowercase()
            .replace(punctuationRegex, " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ")
}
