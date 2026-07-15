package kz.abdiyev.walletai.ai

import kotlinx.serialization.Serializable

@Serializable
data class GeminiPart(val text: String)

@Serializable
data class GeminiContent(val parts: List<GeminiPart> = emptyList())

@Serializable
data class GeminiRequest(val contents: List<GeminiContent>) {
    companion object {
        fun fromPrompt(prompt: String): GeminiRequest =
            GeminiRequest(contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))))
    }
}

@Serializable
data class GeminiCandidate(val content: GeminiContent? = null)

@Serializable
data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList()) {
    fun extractText(): String? = candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
}
