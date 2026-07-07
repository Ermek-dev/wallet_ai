package kz.abdiyev.walletai.util

import kz.abdiyev.walletai.data.database.CategoryDao
import kz.abdiyev.walletai.data.database.CategoryEntity
import kz.abdiyev.walletai.data.database.TransactionDao
import kz.abdiyev.walletai.data.database.TransactionEntity
import kz.abdiyev.walletai.domain.model.Currency
import java.util.Calendar

object MockDataProvider {

    val defaultCategories = listOf(
        CategoryEntity(name = "Продукты", type = "EXPENSE", colorHex = "#EF5350", iconName = "shopping_cart"),
        CategoryEntity(name = "ЖКХ", type = "EXPENSE", colorHex = "#42A5F5", iconName = "home"),
        CategoryEntity(name = "Кофе и Рестораны", type = "EXPENSE", colorHex = "#FFA726", iconName = "restaurant"),
        CategoryEntity(name = "Транспорт", type = "EXPENSE", colorHex = "#26A69A", iconName = "commute"),
        CategoryEntity(name = "Развлечения", type = "EXPENSE", colorHex = "#AB47BC", iconName = "movie"),
        CategoryEntity(name = "Здоровье", type = "EXPENSE", colorHex = "#66BB6A", iconName = "healing"),
        CategoryEntity(name = "Подписки", type = "EXPENSE", colorHex = "#78909C", iconName = "subscriptions"),
        CategoryEntity(name = "Зарплата", type = "INCOME", colorHex = "#9CCC65", iconName = "payments"),
        CategoryEntity(name = "Другое", type = "EXPENSE", colorHex = "#757575", iconName = "paid", isSystem = true)
    )

    suspend fun seedDefaultCategories(categoryDao: CategoryDao) {
        if (categoryDao.getAllCategoriesList().isNotEmpty()) return
        categoryDao.insertCategories(defaultCategories)
    }

    suspend fun loadDemoTransactions(categoryDao: CategoryDao, transactionDao: TransactionDao) {
        seedDefaultCategories(categoryDao)
        transactionDao.deleteAllTransactions()
        val cats = categoryDao.getAllCategoriesList()
        val byName = cats.associateBy { it.name }
        val transactions = mutableListOf<TransactionEntity>()
        val calendar = Calendar.getInstance()

        for (dayAgo in 1..90) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -dayAgo)
            val ts = calendar.timeInMillis
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

            if (dayOfMonth == 10) {
                transactions += tx(85000, "EXPENSE", byName["ЖКХ"]!!.id, "Kaspi оплата ЖКХ", ts)
            }
            if (dayOfMonth == 15) {
                transactions += tx(45000000, "INCOME", byName["Зарплата"]!!.id, "Зарплата Kaspi перевод", ts)
            }
            if (dayAgo % 3 == 0) {
                transactions += tx(1250000, "EXPENSE", byName["Продукты"]!!.id, "Magnum продукты", ts)
            }
            if (dayAgo % 5 == 0) {
                transactions += tx(350000, "EXPENSE", byName["Кофе и Рестораны"]!!.id, "Wolt доставка", ts)
            }
            if (dayAgo % 4 == 0) {
                transactions += tx(80000, "EXPENSE", byName["Транспорт"]!!.id, "Onay проезд", ts)
            }
        }
        transactionDao.insertTransactions(transactions)
    }

    private fun tx(minor: Long, type: String, categoryId: Long, desc: String, date: Long) =
        TransactionEntity(
            amountMinor = minor,
            currencyCode = Currency.KZT.code,
            type = type,
            categoryId = categoryId,
            description = desc,
            date = date
        )
}
