package kz.abdiyev.walletai.ai

interface AdviceAiClient {
    suspend fun generateAdvice(prompt: String): String
}
