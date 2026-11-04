package kz.abdiyev.walletai.data.seed

import kz.abdiyev.walletai.data.database.CategoryDao
import kz.abdiyev.walletai.data.database.TransactionDao
import kz.abdiyev.walletai.data.database.TransactionEntity
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.util.CalendarMillis
import kz.abdiyev.walletai.util.Clock
import kz.abdiyev.walletai.util.TimeUtils

object DemoDataSeeder {

    suspend fun loadDemoTransactions(
        categoryDao: CategoryDao,
        transactionDao: TransactionDao,
        clock: Clock
    ) {
        seedMissingCategories(categoryDao)
        transactionDao.deleteAllTransactions()
        val cats = categoryDao.getAllCategoriesList()
        val byName = cats.associateBy { it.name }
        val transactions = mutableListOf<TransactionEntity>()
        val now = clock.nowMillis()
        val offset = TimeUtils.localOffsetMillis(now)

        for (dayAgo in 1..90) {
            val dayStart = CalendarMillis.toLocalStartOfDay(now, offset) - dayAgo * 86_400_000L
            val dayOfMonth = CalendarMillis.fromEpochMillis(dayStart + offset).day

            if (dayOfMonth == 10) {
                transactions += tx(85000, "EXPENSE", byName["ЖКХ"]!!.id, "Kaspi оплата ЖКХ", dayStart)
            }
            if (dayOfMonth == 15) {
                transactions += tx(45000000, "INCOME", byName["Зарплата"]!!.id, "Зарплата Kaspi перевод", dayStart)
            }
            if (dayAgo % 3 == 0) {
                transactions += tx(1250000, "EXPENSE", byName["Продукты"]!!.id, "Magnum продукты", dayStart)
            }
            if (dayAgo % 5 == 0) {
                transactions += tx(350000, "EXPENSE", byName["Кофе и Рестораны"]!!.id, "Wolt доставка", dayStart)
            }
            if (dayAgo % 4 == 0) {
                transactions += tx(80000, "EXPENSE", byName["Транспорт"]!!.id, "Onay проезд", dayStart)
            }
        }
        transactionDao.insertTransactions(transactions)
    }

    private suspend fun seedMissingCategories(categoryDao: CategoryDao) {
        val existingNames = categoryDao.getAllCategoriesList().map { it.name }.toSet()
        val missing = DefaultCategories.entities.filter { it.name !in existingNames }
        if (missing.isNotEmpty()) {
            categoryDao.insertCategories(missing)
        }
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
