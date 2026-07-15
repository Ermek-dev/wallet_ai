package kz.abdiyev.walletai.statement

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ml.statement.FreedomStatementParser
import kz.abdiyev.walletai.ml.statement.StatementParseContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FreedomStatementParserTest {
    private val parser = FreedomStatementParser()

    @Test
    fun parsesOperationsFromRealText() {
        val lines = parser.parse(StatementFixtures.FREEDOM_RAW, StatementParseContext(Currency.KZT))
        assertEquals(7, lines.size)
    }

    @Test
    fun parsesFreedomAmountWithThousandsComma() {
        val lines = parser.parse(StatementFixtures.FREEDOM_RAW, StatementParseContext(Currency.KZT))
        val fourThousand = lines.first { it.money.majorAmount == 4000.0 }
        assertEquals(TransactionType.EXPENSE, fourThousand.type)
    }

    @Test
    fun parsesIncomeWithPlusSignInAmount() {
        val lines = parser.parse(StatementFixtures.FREEDOM_RAW, StatementParseContext(Currency.KZT))
        val income = lines.first { it.money.majorAmount == 19_840.0 }
        assertEquals(TransactionType.INCOME, income.type)
        assertTrue(income.isTransfer)
    }

    @Test
    fun skipsUsdOperations() {
        val lines = parser.parse(StatementFixtures.FREEDOM_RAW, StatementParseContext(Currency.KZT))
        assertFalse(lines.any { it.description.contains("Freedom на счет") })
    }

    @Test
    fun mergesMultilineOnayPayment() {
        val lines = parser.parse(StatementFixtures.FREEDOM_RAW, StatementParseContext(Currency.KZT))
        val onay = lines.first { it.description == "ОНАЙ" }
        assertEquals(240.0, onay.money.majorAmount)
    }

    @Test
    fun recognizesMerchantsInDescription() {
        val lines = parser.parse(StatementFixtures.FREEDOM_RAW, StatementParseContext(Currency.KZT))
        assertTrue(lines.any { it.description.contains("MAGNUM", ignoreCase = true) })
        assertTrue(lines.any { it.description.contains("KFC", ignoreCase = true) })
        assertTrue(lines.any { it.description.contains("SELI POYELI", ignoreCase = true) })
    }
}
