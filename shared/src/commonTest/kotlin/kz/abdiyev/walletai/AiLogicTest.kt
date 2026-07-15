package kz.abdiyev.walletai

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kz.abdiyev.walletai.ai.AdviceCachePolicy
import kz.abdiyev.walletai.ai.AdvicePrompt
import kz.abdiyev.walletai.ai.AiApiException
import kz.abdiyev.walletai.ai.AiErrorMapper
import kz.abdiyev.walletai.ai.AiKeyMissingException
import kz.abdiyev.walletai.ai.AiNetworkException
import kz.abdiyev.walletai.ai.AiTimeoutException
import kz.abdiyev.walletai.ai.FallbackAdvice
import kz.abdiyev.walletai.ai.GeminiClient
import kz.abdiyev.walletai.ai.GeminiResponse
import kz.abdiyev.walletai.ai.GroqClient
import kz.abdiyev.walletai.ai.GroqResponse
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val testCategories = listOf(
    CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "shopping_cart"),
    CategoryData(8, "Зарплата", TransactionType.INCOME, "#9CCC65", "payments")
)

private val testTransactions = listOf(
    TransactionData(
        id = 1, money = Money.fromMajor(3400.0, Currency.KZT), type = TransactionType.EXPENSE,
        categoryId = 1, description = "Magnum", date = 0L
    ),
    TransactionData(
        id = 2, money = Money.fromMajor(450000.0, Currency.KZT), type = TransactionType.INCOME,
        categoryId = 8, description = "Зарплата", date = 0L
    )
)

class GeminiDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesRealResponseShape() {
        val fixture = """
            {"candidates":[{"content":{"parts":[{"text":"Совет: откладывайте 10%"}],"role":"model"},
            "finishReason":"STOP","index":0}],"usageMetadata":{"promptTokenCount":42}}
        """.trimIndent()
        val response = json.decodeFromString<GeminiResponse>(fixture)
        assertEquals("Совет: откладывайте 10%", response.extractText())
    }

    @Test
    fun extractTextReturnsNullOnEmptyCandidates() {
        val response = json.decodeFromString<GeminiResponse>("""{"candidates":[]}""")
        assertEquals(null, response.extractText())
    }
}

class GroqDtoTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesRealResponseShape() {
        val fixture = """
            {"choices":[{"message":{"role":"assistant","content":"Совет: откладывайте 10%"}}]}
        """.trimIndent()
        val response = json.decodeFromString<GroqResponse>(fixture)
        assertEquals("Совет: откладывайте 10%", response.extractText())
    }

    @Test
    fun extractTextReturnsNullOnEmptyChoices() {
        val response = json.decodeFromString<GroqResponse>("""{"choices":[]}""")
        assertEquals(null, response.extractText())
    }
}

class AdvicePromptTest {
    @Test
    fun promptContainsTotalsAndCategories() {
        val prompt = AdvicePrompt.build(testTransactions, testCategories, "KZT")
        assertTrue(prompt.contains("Продукты"), "prompt should mention expense category")
        assertTrue(prompt.contains("₸"), "prompt should use currency symbol")
        assertTrue(prompt.contains("100.0%"), "single expense category is 100%")
    }
}

class FallbackAdviceTest {
    @Test
    fun fallbackContainsReasonAndTopCategory() {
        val advice = FallbackAdvice.build(testTransactions, testCategories, "KZT", "Нет сети")
        assertTrue(advice.contains("Нет сети"))
        assertTrue(advice.contains("Продукты"))
    }
}

class AdviceCachePolicyTest {
    @Test
    fun promptHashIsStable() {
        val hash1 = AdviceCachePolicy.promptHash("same prompt")
        val hash2 = AdviceCachePolicy.promptHash("same prompt")
        val hash3 = AdviceCachePolicy.promptHash("different")
        assertEquals(hash1, hash2)
        assertTrue(hash1 != hash3)
    }

    @Test
    fun cacheValidWithinTtlWhenHashMatches() {
        val now = 1_000_000L
        assertTrue(
            AdviceCachePolicy.isValid(
                cachedPromptHash = "abc",
                cachedTimestampMillis = now - 1_000L,
                currentPromptHash = "abc",
                nowMillis = now,
                forceRefresh = false
            )
        )
    }

    @Test
    fun cacheInvalidWhenForceRefresh() {
        assertFalse(
            AdviceCachePolicy.isValid(
                cachedPromptHash = "abc",
                cachedTimestampMillis = 1_000L,
                currentPromptHash = "abc",
                nowMillis = 2_000L,
                forceRefresh = true
            )
        )
    }

    @Test
    fun cacheInvalidWhenHashDiffers() {
        assertFalse(
            AdviceCachePolicy.isValid(
                cachedPromptHash = "abc",
                cachedTimestampMillis = 1_000L,
                currentPromptHash = "xyz",
                nowMillis = 2_000L,
                forceRefresh = false
            )
        )
    }

    @Test
    fun cacheInvalidAfterTtl() {
        assertFalse(
            AdviceCachePolicy.isValid(
                cachedPromptHash = "abc",
                cachedTimestampMillis = 0L,
                currentPromptHash = "abc",
                nowMillis = AdviceCachePolicy.TTL_MILLIS + 1L,
                forceRefresh = false
            )
        )
    }
}

class GeminiClientTest {
    private fun clientReturning(body: String, status: HttpStatusCode = HttpStatusCode.OK): GeminiClient {
        val engine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        return GeminiClient(apiKeyProvider = { "test-key" }, engine = engine)
    }

    @Test
    fun returnsTextFromSuccessfulResponse() = runTest {
        val client = clientReturning(
            """{"candidates":[{"content":{"parts":[{"text":"Ответ ИИ"}]}}]}"""
        )
        assertEquals("Ответ ИИ", client.generateAdvice("prompt"))
    }

    @Test
    fun throwsOnHttpError() = runTest {
        val client = clientReturning("""{"error":{"code":403}}""", HttpStatusCode.Forbidden)
        val e = assertFailsWith<AiApiException> { client.generateAdvice("prompt") }
        assertEquals(403, e.code)
    }

    @Test
    fun throwsOnEmptyCandidates() = runTest {
        val client = clientReturning("""{"candidates":[]}""")
        assertFailsWith<AiApiException> { client.generateAdvice("prompt") }
    }

    @Test
    fun throwsWhenApiKeyMissing() = runTest {
        val client = GeminiClient(apiKeyProvider = { "" })
        assertFailsWith<AiKeyMissingException> { client.generateAdvice("prompt") }
    }

    @Test
    fun throwsWhenApiKeyIsPlaceholder() = runTest {
        val client = GeminiClient(apiKeyProvider = { "MY_GEMINI_API_KEY" })
        assertFailsWith<AiKeyMissingException> { client.generateAdvice("prompt") }
    }
}

class GroqClientTest {
    private fun clientReturning(body: String, status: HttpStatusCode = HttpStatusCode.OK): GroqClient {
        val engine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        return GroqClient(apiKeyProvider = { "test-key" }, engine = engine)
    }

    @Test
    fun returnsTextFromSuccessfulResponse() = runTest {
        val client = clientReturning(
            """{"choices":[{"message":{"role":"assistant","content":"Ответ Groq"}}]}"""
        )
        assertEquals("Ответ Groq", client.generateAdvice("prompt"))
    }

    @Test
    fun throwsOnHttpError() = runTest {
        val client = clientReturning("""{"error":"forbidden"}""", HttpStatusCode.Forbidden)
        val e = assertFailsWith<AiApiException> { client.generateAdvice("prompt") }
        assertEquals(403, e.code)
    }

    @Test
    fun throwsOnEmptyChoices() = runTest {
        val client = clientReturning("""{"choices":[]}""")
        assertFailsWith<AiApiException> { client.generateAdvice("prompt") }
    }

    @Test
    fun throwsWhenApiKeyMissing() = runTest {
        val client = GroqClient(apiKeyProvider = { "" })
        assertFailsWith<AiKeyMissingException> { client.generateAdvice("prompt") }
    }

    @Test
    fun throwsWhenApiKeyIsPlaceholder() = runTest {
        val client = GroqClient(apiKeyProvider = { "MY_GROQ_API_KEY" })
        assertFailsWith<AiKeyMissingException> { client.generateAdvice("prompt") }
    }
}

class AiErrorMapperTest {
    @Test
    fun mapsKnownExceptions() {
        assertTrue(AiErrorMapper.userMessage(AiKeyMissingException()).contains("GROQ_API_KEY"))
        assertTrue(AiErrorMapper.userMessage(AiTimeoutException()).contains("вовремя"))
        val forbiddenMessage = AiErrorMapper.userMessage(AiApiException(403, "err"))
        assertTrue(forbiddenMessage.contains("403"))
        assertTrue(forbiddenMessage.contains("ключ"))
        val unauthorizedMessage = AiErrorMapper.userMessage(AiApiException(401, "err"))
        assertTrue(unauthorizedMessage.contains("401"))
        val rateLimitMessage = AiErrorMapper.userMessage(AiApiException(429, "err"))
        assertTrue(rateLimitMessage.contains("429"))
        assertTrue(rateLimitMessage.contains("позже"))
        assertEquals("Ошибка API (500)", AiErrorMapper.userMessage(AiApiException(500, "err")))
        assertTrue(AiErrorMapper.userMessage(AiNetworkException("custom")).contains("custom"))
    }
}
