package kz.abdiyev.walletai.ai

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Shared Ktor timeouts and JSON for Gemini / Groq advice clients. */
object AdviceHttpConfig {
    const val REQUEST_TIMEOUT_MS = 30_000L
    const val CONNECT_TIMEOUT_MS = 15_000L
    const val SOCKET_TIMEOUT_MS = 30_000L
}

internal fun HttpClientConfig<*>.configureAdviceHttpClient() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = AdviceHttpConfig.REQUEST_TIMEOUT_MS
        connectTimeoutMillis = AdviceHttpConfig.CONNECT_TIMEOUT_MS
        socketTimeoutMillis = AdviceHttpConfig.SOCKET_TIMEOUT_MS
    }
}
