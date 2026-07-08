package kz.abdiyev.walletai

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kz.abdiyev.walletai.ai.AdvicePrompt
import kz.abdiyev.walletai.ai.FallbackAdvice
import kz.abdiyev.walletai.ai.GeminiApiException
import kz.abdiyev.walletai.ai.GeminiClient
import kz.abdiyev.walletai.ai.GeminiErrorMapper
import kz.abdiyev.walletai.ai.GeminiKeyMissingException
import kz.abdiyev.walletai.ai.GeminiNetworkException
import kz.abdiyev.walletai.ai.GeminiResponse
import kz.abdiyev.walletai.ai.GeminiTimeoutException
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
        val e = assertFailsWith<GeminiApiException> { client.generateAdvice("prompt") }
        assertEquals(403, e.code)
    }

    @Test
    fun throwsOnEmptyCandidates() = runTest {
        val client = clientReturning("""{"candidates":[]}""")
        assertFailsWith<GeminiApiException> { client.generateAdvice("prompt") }
    }

    @Test
    fun throwsWhenApiKeyMissing() = runTest {
        val client = GeminiClient(apiKeyProvider = { "" })
        assertFailsWith<GeminiKeyMissingException> { client.generateAdvice("prompt") }
    }

    @Test
    fun throwsWhenApiKeyIsPlaceholder() = runTest {
        val client = GeminiClient(apiKeyProvider = { "MY_GEMINI_API_KEY" })
        assertFailsWith<GeminiKeyMissingException> { client.generateAdvice("prompt") }
    }
}

class GeminiErrorMapperTest {
    @Test
    fun mapsKnownExceptions() {
        assertTrue(GeminiErrorMapper.userMessage(GeminiKeyMissingException()).contains("GEMINI_API_KEY"))
        assertTrue(GeminiErrorMapper.userMessage(GeminiTimeoutException()).contains("вовремя"))
        assertEquals("Ошибка API (403)", GeminiErrorMapper.userMessage(GeminiApiException(403, "err")))
        assertTrue(GeminiErrorMapper.userMessage(GeminiNetworkException("custom")).contains("custom"))
    }
}
