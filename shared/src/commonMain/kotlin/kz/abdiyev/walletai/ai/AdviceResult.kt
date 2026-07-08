package kz.abdiyev.walletai.ai

sealed interface AdviceResult {
    val text: String

    data class Ai(override val text: String) : AdviceResult

    data class Fallback(override val text: String, val reason: String) : AdviceResult
}
