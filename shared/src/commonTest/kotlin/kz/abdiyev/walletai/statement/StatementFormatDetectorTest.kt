package kz.abdiyev.walletai.statement

import kz.abdiyev.walletai.ml.statement.StatementFormat
import kz.abdiyev.walletai.ml.statement.StatementFormatDetector
import kotlin.test.Test
import kotlin.test.assertEquals

class StatementFormatDetectorTest {
    private val detector = StatementFormatDetector()

    @Test
    fun detectsKaspi() {
        assertEquals(StatementFormat.KASPI, detector.detect(StatementFixtures.KASPI_RAW))
    }

    @Test
    fun detectsFreedom() {
        assertEquals(StatementFormat.FREEDOM, detector.detect(StatementFixtures.FREEDOM_RAW))
    }

    @Test
    fun unknownTextIsGeneric() {
        assertEquals(StatementFormat.GENERIC, detector.detect("Magnum 3400 тг"))
        assertEquals(StatementFormat.GENERIC, detector.detect(""))
    }
}
