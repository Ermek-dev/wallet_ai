package kz.abdiyev.walletai

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ml.VoiceInputParser
import kz.abdiyev.walletai.util.StatementDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VoiceInputParserTest {
    private val parser = VoiceInputParser()
    private val reference = StatementDate.toEpochMillis(2026, 7, 13) + 12 * 3_600_000L

    @Test
    fun parse_todayKeyword_usesReferenceDate() {
        val result = parser.parse("сегодня потратил 1500 на обед", Currency.KZT, reference)
        assertEquals(reference, result.dateMillis)
        assertEquals(1500.0, result.money?.majorAmount)
    }

    @Test
    fun parse_yesterdayKeyword_shiftsDateBackOneDay() {
        val result = parser.parse("вчера купил кофе за 800", Currency.KZT, reference)
        assertEquals(reference - 86_400_000L, result.dateMillis)
    }

    @Test
    fun parse_transferPhrase_extractsAmountAndDescription() {
        val result = parser.parse("перевод 5000 другу", Currency.KZT, reference)
        assertEquals(5000.0, result.money?.majorAmount)
        assertTrue(result.description.contains("другу", ignoreCase = true))
    }

    @Test
    fun parse_incomePhrase_detectsIncomeType() {
        val result = parser.parse("получил зарплату 250000", Currency.KZT, reference)
        assertEquals(TransactionType.INCOME, result.type)
        assertNotNull(result.money)
    }
}
