package kz.abdiyev.walletai

import kz.abdiyev.walletai.data.database.CategoryEntity
import kz.abdiyev.walletai.data.database.TransactionEntity
import kz.abdiyev.walletai.data.mapper.toDomain
import kz.abdiyev.walletai.data.mapper.toEntity
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.Money
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappersTest {

    @Test
    fun categoryEntity_roundTripsToDomain() {
        val entity = CategoryEntity(
            id = 5,
            name = "Продукты",
            type = "EXPENSE",
            colorHex = "#EF5350",
            iconName = "shopping_cart",
            isSystem = true
        )
        val domain = entity.toDomain()
        assertEquals("Продукты", domain.name)
        assertEquals(TransactionType.EXPENSE, domain.type)
        assertEquals(true, domain.isSystem)
        assertEquals(entity.id, domain.toEntity().id)
    }

    @Test
    fun transactionEntity_roundTripsMoneyAndType() {
        val domain = TransactionData(
            id = 10,
            money = Money.fromMajor(4500.0, Currency.KZT),
            type = TransactionType.INCOME,
            categoryId = 8,
            description = "Зарплата",
            date = 1_700_000_000_000,
            isAutoCategorized = false
        )
        val entity: TransactionEntity = domain.toEntity()
        assertEquals(450_000L, entity.amountMinor)
        assertEquals("KZT", entity.currencyCode)
        assertEquals("INCOME", entity.type)
        assertEquals(domain.description, entity.toDomain().description)
    }
}
