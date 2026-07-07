package kz.abdiyev.walletai.service

import kz.abdiyev.walletai.BuildConfig
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.util.MoneyFormatter
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateFinancialAdvice(
        transactions: List<TransactionData>,
        categories: List<CategoryData>,
        currencyCode: String
    ): String {
        val apiKey = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return getFallbackAdvice(
                transactions, categories, currencyCode,
                "Ключ API Gemini не настроен. Укажите GEMINI_API_KEY в файле .env"
            )
        }

        val currency = Currency.fromCode(currencyCode)
        val categoryMap = categories.associateBy { it.id }
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val incomes = transactions.filter { it.type == TransactionType.INCOME }
        val totalExpense = expenses.sumOf { it.money.amountMinor }
        val totalIncome = incomes.sumOf { it.money.amountMinor }

        val expensesByCategory = expenses.groupBy { it.categoryId }
            .map { (catId, txs) ->
                val catName = categoryMap[catId]?.name ?: "Другое"
                val sum = Money(txs.sumOf { it.money.amountMinor }, currency)
                val pct = if (totalExpense > 0) sum.amountMinor.toDouble() / totalExpense * 100 else 0.0
                "- $catName: ${MoneyFormatter.format(sum)} (${"%.1f".format(pct)}%)"
            }.joinToString("\n")

        val prompt = """
            Ты — персональный финансовый ИИ-помощник WalletAI. Проанализируй сводку и дай рекомендации на русском языке.
            
            - Общий доход: ${MoneyFormatter.format(Money(totalIncome, currency))}
            - Общий расход: ${MoneyFormatter.format(Money(totalExpense, currency))}
            - Сальдо: ${MoneyFormatter.format(Money(totalIncome - totalExpense, currency))}
            
            Распределение расходов:
            $expensesByCategory
            
            Формат: 1) краткий анализ, 2) 3 совета, 3) мотивация.
        """.trimIndent()

        val jsonRequest = """
            {"contents":[{"parts":[{"text":${JSONObject.quote(prompt)}}]}]}
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
            .addHeader("x-goog-api-key", apiKey)
            .post(jsonRequest.toRequestBody(mediaType))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return getFallbackAdvice(transactions, categories, currencyCode, "Ошибка API (${response.code})")
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val text = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                text
            }
        } catch (e: Exception) {
            Timber.e(e, "Gemini call failed")
            getFallbackAdvice(transactions, categories, currencyCode, e.message ?: "Сетевая ошибка")
        }
    }

    fun getFallbackAdvice(
        transactions: List<TransactionData>,
        categories: List<CategoryData>,
        currencyCode: String,
        reason: String
    ): String {
        val currency = Currency.fromCode(currencyCode)
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val incomes = transactions.filter { it.type == TransactionType.INCOME }
        val totalExpense = expenses.sumOf { it.money.amountMinor }
        val totalIncome = incomes.sumOf { it.money.amountMinor }
        val categoryMap = categories.associateBy { it.id }
        val maxCat = expenses.groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.money.amountMinor } }
            .maxByOrNull { it.value }
        val maxName = maxCat?.let { categoryMap[it.key]?.name } ?: "Не определено"
        val maxAmount = maxCat?.value ?: 0L
        val maxPct = if (totalExpense > 0) maxAmount.toDouble() / totalExpense * 100 else 0.0

        return """
            📊 [WalletAI — локальный анализ] ($reason)
            
            • Доходы: ${MoneyFormatter.format(Money(totalIncome, currency))}
            • Расходы: ${MoneyFormatter.format(Money(totalExpense, currency))}
            • Баланс: ${MoneyFormatter.format(Money(totalIncome - totalExpense, currency))}
            • Главная статья: "$maxName" — ${MoneyFormatter.format(Money(maxAmount, currency))} (${"%.1f".format(maxPct)}%)
            
            💡 Сократите траты в "$maxName" на 10%, вносите расходы сразу, откладывайте 10% от дохода.
            
            Продолжайте вести учёт в WalletAI!
        """.trimIndent()
    }
}
