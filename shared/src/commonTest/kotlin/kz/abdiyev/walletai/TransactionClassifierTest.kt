package kz.abdiyev.walletai

import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ml.TransactionClassifier
import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionClassifierTest {

    private val categories = listOf(
        CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "shopping_cart"),
        CategoryData(2, "Транспорт", TransactionType.EXPENSE, "#42A5F5", "directions_car"),
        CategoryData(8, "Зарплата", TransactionType.INCOME, "#9CCC65", "payments")
    )

    @Test
    fun classify_prefersMatchingCategoryFromTraining() {
        val classifier = TransactionClassifier()
        val training = listOf(
            tx("Magnum продукты", 1),
            tx("Magnum хлеб", 1),
            tx("Такси Yandex", 2)
        )
        val model = classifier.train(training, categories)
        val result = classifier.classify("Magnum молоко", model, categories)
        assertEquals(1L, result)
    }

    private fun tx(description: String, categoryId: Long) = TransactionData(
        money = Money.fromMajor(1000.0, Currency.KZT),
        type = TransactionType.EXPENSE,
        categoryId = categoryId,
        description = description,
        date = System.currentTimeMillis()
    )
}
