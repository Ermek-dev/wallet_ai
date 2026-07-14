package kz.abdiyev.walletai.ai

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Multiplatform Gemini REST client (Ktor). Kept as fallback provider.
 */
class GeminiClient(
    private val apiKeyProvider: () -> String,
    engine: HttpClientEngine? = null
) : AdviceAiClient {
    private val client: HttpClient =
        engine?.let { HttpClient(it) { configure() } } ?: HttpClient { configure() }

    private fun HttpClientConfig<*>.configure() {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
        }
    }

    override suspend fun generateAdvice(prompt: String): String {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank() || apiKey == PLACEHOLDER_KEY) {
            throw AiKeyMissingException()
        }

        val response = try {
            client.post(GENERATE_URL) {
                header(API_KEY_HEADER, apiKey)
                contentType(ContentType.Application.Json)
                setBody(GeminiRequest.fromPrompt(prompt))
            }
        } catch (e: HttpRequestTimeoutException) {
            throw AiTimeoutException()
        } catch (e: AiApiException) {
            throw e
        } catch (e: AiKeyMissingException) {
            throw e
        } catch (e: AiTimeoutException) {
            throw e
        } catch (e: Exception) {
            throw AiNetworkException("Сетевая ошибка, показан локальный анализ", e)
        }

        if (!response.status.isSuccess()) {
            throw AiApiException(response.status.value, "Gemini API error ${response.status.value}")
        }
        return response.body<GeminiResponse>().extractText()
            ?: throw AiApiException(0, "Empty Gemini response")
    }

    companion object {
        private const val PLACEHOLDER_KEY = "MY_GEMINI_API_KEY"
        private const val API_KEY_HEADER = "x-goog-api-key"
        private const val GENERATE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    }
}
