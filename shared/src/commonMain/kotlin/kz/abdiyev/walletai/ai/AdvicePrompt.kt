package kz.abdiyev.walletai.ai

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.util.MoneyFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

/** String.format is JVM-only, so percentages are formatted manually for commonMain. */
internal fun formatPercent(value: Double): String {
    val tenths = (value * 10).roundToInt()
    return "${tenths / 10}.${abs(tenths % 10)}"
}

object AdvicePrompt {

    fun build(
        transactions: List<TransactionData>,
        categories: List<CategoryData>,
        currencyCode: String
    ): String {
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
                "- $catName: ${MoneyFormatter.format(sum)} (${formatPercent(pct)}%)"
            }.joinToString("\n")

        return """
            Ты — персональный финансовый ИИ-помощник WalletAI. Проанализируй сводку и дай рекомендации на русском языке.

            - Общий доход: ${MoneyFormatter.format(Money(totalIncome, currency))}
            - Общий расход: ${MoneyFormatter.format(Money(totalExpense, currency))}
            - Сальдо: ${MoneyFormatter.format(Money(totalIncome - totalExpense, currency))}

            Распределение расходов:
            $expensesByCategory

            Формат: 1) краткий анализ, 2) 3 совета, 3) мотивация.
        """.trimIndent()
    }
}

object FallbackAdvice {

    fun build(
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
            • Главная статья: "$maxName" — ${MoneyFormatter.format(Money(maxAmount, currency))} (${formatPercent(maxPct)}%)

            💡 Сократите траты в "$maxName" на 10%, вносите расходы сразу, откладывайте 10% от дохода.

            Продолжайте вести учёт в WalletAI!
        """.trimIndent()
    }
}
