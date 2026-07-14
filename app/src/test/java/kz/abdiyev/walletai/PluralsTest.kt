package kz.abdiyev.walletai

import kz.abdiyev.walletai.util.Plurals
import org.junit.Assert.assertEquals
import org.junit.Test

class PluralsTest {

    @Test
    fun operations_one() {
        assertEquals("1 операция", Plurals.operations(1))
    }

    @Test
    fun operations_few() {
        assertEquals("2 операции", Plurals.operations(2))
        assertEquals("4 операции", Plurals.operations(4))
    }

    @Test
    fun operations_many() {
        assertEquals("5 операций", Plurals.operations(5))
        assertEquals("11 операций", Plurals.operations(11))
        assertEquals("21 операция", Plurals.operations(21))
    }

    @Test
    fun hiddenOutsidePeriodHint() {
        assertEquals("Ещё 3 операции вне периода", Plurals.hiddenOutsidePeriodHint(3))
    }

    @Test
    fun formatSavingsRatePercent_positiveAndNegative() {
        assertEquals("25%", Plurals.formatSavingsRatePercent(25.4f))
        assertEquals("−15%", Plurals.formatSavingsRatePercent(-15.3f))
        assertEquals("0%", Plurals.formatSavingsRatePercent(0f))
    }
}
