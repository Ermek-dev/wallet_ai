package kz.abdiyev.walletai.ai

class AiKeyMissingException : Exception("AI API key is not configured")

class AiApiException(val code: Int, message: String) : Exception(message)

class AiTimeoutException : Exception("AI request timed out")

class AiNetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
