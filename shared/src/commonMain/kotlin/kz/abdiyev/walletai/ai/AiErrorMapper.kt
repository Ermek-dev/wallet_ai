package kz.abdiyev.walletai.ai

object AiErrorMapper {

    fun userMessage(error: Throwable): String = when (error) {
        is AiKeyMissingException ->
            "Ключ API не настроен. Укажите GROQ_API_KEY в файле .env"
        is AiTimeoutException ->
            "Сервер ИИ не ответил вовремя, попробуйте ещё раз"
        is AiApiException -> when (error.code) {
            401, 403 ->
                "Доступ к API запрещён (${error.code}): проверьте ключ — " +
                    "он мог быть отозван, иметь ограничения или региональные блокировки"
            429 ->
                "Превышен лимит запросов (${error.code}): попробуйте позже"
            else ->
                "Ошибка API (${error.code})"
        }
        is AiNetworkException ->
            error.message?.takeIf { it.isNotBlank() }
                ?: "Сетевая ошибка, показан локальный анализ"
        else ->
            "Сетевая ошибка, показан локальный анализ"
    }
}
