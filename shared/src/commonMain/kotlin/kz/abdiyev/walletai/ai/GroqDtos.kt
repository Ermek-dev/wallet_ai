package kz.abdiyev.walletai.ai

import kotlinx.serialization.Serializable

@Serializable
data class GroqMessage(val role: String, val content: String)

@Serializable
data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>
) {
    companion object {
        fun fromPrompt(prompt: String, model: String = DEFAULT_MODEL): GroqRequest =
            GroqRequest(
                model = model,
                messages = listOf(GroqMessage(role = "user", content = prompt))
            )

        const val DEFAULT_MODEL = "llama-3.3-70b-versatile"
    }
}

@Serializable
data class GroqChoice(val message: GroqMessage? = null)

@Serializable
data class GroqResponse(val choices: List<GroqChoice> = emptyList()) {
    fun extractText(): String? = choices.firstOrNull()?.message?.content
}
