package kz.abdiyev.walletai.statement

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ml.statement.KaspiStatementParser
import kz.abdiyev.walletai.ml.statement.StatementParseContext
import kz.abdiyev.walletai.util.StatementDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KaspiStatementParserTest {
    private val parser = KaspiStatementParser()

    @Test
    fun parsesOperationsFromRealText() {
        val lines = parser.parse(StatementFixtures.KASPI_RAW, StatementParseContext(Currency.KZT))
        assertEquals(11, lines.size)
    }

    @Test
    fun parsesKaspiAmountWithGroupingSpaces() {
        val lines = parser.parse(StatementFixtures.KASPI_RAW, StatementParseContext(Currency.KZT))
        val twoThousand = lines.first { it.description.contains("Банкомат") }
        assertEquals(2000.0, twoThousand.money.majorAmount)
    }

    @Test
    fun parsesSalaryAsIncomeNotTransfer() {
        val lines = parser.parse(StatementFixtures.KASPI_RAW, StatementParseContext(Currency.KZT))
        val salary = lines.first { it.description.contains("Зарплата", ignoreCase = true) }
        assertEquals(TransactionType.INCOME, salary.type)
        assertEquals(550_000.0, salary.money.majorAmount)
        assertFalse(salary.isTransfer)
    }

    @Test
    fun mergesContinuationLineIntoSelfTransfer() {
        val lines = parser.parse(StatementFixtures.KASPI_RAW, StatementParseContext(Currency.KZT))
        val selfTransfer = lines.first { it.rawOperation.contains("Перевод на свой счет") }
        assertTrue(selfTransfer.isTransfer)
        assertEquals(TransactionType.EXPENSE, selfTransfer.type)
    }

    @Test
    fun cardTopUpMarkedAsTransfer() {
        val lines = parser.parse(StatementFixtures.KASPI_RAW, StatementParseContext(Currency.KZT))
        val topUp = lines.first { it.description.contains("с карты другого банка", ignoreCase = true) }
        assertTrue(topUp.isTransfer)
    }

    @Test
    fun commissionIsNotTransfer() {
        val lines = parser.parse(StatementFixtures.KASPI_RAW, StatementParseContext(Currency.KZT))
        val fee = lines.first { it.description.contains("Комиссия") }
        assertFalse(fee.isTransfer)
        assertEquals(TransactionType.EXPENSE, fee.type)
    }

    @Test
    fun parsesDecimalAmount() {
        val lines = parser.parse(StatementFixtures.KASPI_RAW, StatementParseContext(Currency.KZT))
        val glovo = lines.first { it.description.contains("Glovo") }
        assertEquals(16074.75, glovo.money.majorAmount)
    }

    @Test
    fun dateParsedToEpoch() {
        val lines = parser.parse(StatementFixtures.KASPI_RAW, StatementParseContext(Currency.KZT))
        val first = lines.first()
        assertEquals(StatementDate.kaspiDate(30, 6, 26), first.date)
    }
}
