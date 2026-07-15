package kz.abdiyev.walletai.util

import kz.abdiyev.walletai.data.database.CategoryDao
import kz.abdiyev.walletai.data.database.TransactionDao
import kz.abdiyev.walletai.data.database.TransactionEntity
import kz.abdiyev.walletai.data.seed.DefaultCategories
import kz.abdiyev.walletai.domain.model.Currency
import java.util.Calendar

object MockDataProvider {

    val defaultCategories = DefaultCategories.entities

    suspend fun seedDefaultCategories(categoryDao: CategoryDao) {
        val existingNames = categoryDao.getAllCategoriesList().map { it.name }.toSet()
        val missing = defaultCategories.filter { it.name !in existingNames }
        if (missing.isNotEmpty()) {
            categoryDao.insertCategories(missing)
        }
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
