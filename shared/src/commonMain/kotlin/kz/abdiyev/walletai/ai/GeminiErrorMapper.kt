package kz.abdiyev.walletai.ai

object GeminiErrorMapper {

    fun userMessage(error: Throwable): String = when (error) {
        is GeminiKeyMissingException ->
            "Ключ API Gemini не настроен. Укажите GEMINI_API_KEY в файле .env"
        is GeminiTimeoutException ->
            "Сервер ИИ не ответил вовремя, попробуйте ещё раз"
        is GeminiApiException ->
            "Ошибка API (${error.code})"
        is GeminiNetworkException ->
            error.message?.takeIf { it.isNotBlank() }
                ?: "Сетевая ошибка, показан локальный анализ"
        else ->
            "Сетевая ошибка, показан локальный анализ"
    }
}
