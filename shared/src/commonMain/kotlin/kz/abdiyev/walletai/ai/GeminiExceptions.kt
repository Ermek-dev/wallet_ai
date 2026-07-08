package kz.abdiyev.walletai.ai

class GeminiKeyMissingException : Exception("Gemini API key is not configured")

class GeminiNetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
