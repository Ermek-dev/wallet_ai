package kz.abdiyev.walletai.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Groq OpenAI-compatible REST client (Ktor).
 */
class GroqClient(
    private val apiKeyProvider: () -> String,
    engine: HttpClientEngine? = null
) : AdviceAiClient {
    private val client: HttpClient =
        engine?.let { HttpClient(it) { configure() } } ?: HttpClient { configure() }

    private fun io.ktor.client.HttpClientConfig<*>.configure() {
        configureAdviceHttpClient()
    }

    override suspend fun generateAdvice(prompt: String): String {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank() || apiKey == PLACEHOLDER_KEY) {
            throw AiKeyMissingException()
        }

        val response = try {
            client.post(CHAT_COMPLETIONS_URL) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(GroqRequest.fromPrompt(prompt))
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
            throw AiApiException(response.status.value, "Groq API error ${response.status.value}")
        }
        return response.body<GroqResponse>().extractText()
            ?: throw AiApiException(0, "Empty Groq response")
    }

    companion object {
        private const val PLACEHOLDER_KEY = "MY_GROQ_API_KEY"
        private const val CHAT_COMPLETIONS_URL =
            "https://api.groq.com/openai/v1/chat/completions"
    }
}
