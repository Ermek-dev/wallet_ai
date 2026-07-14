package kz.abdiyev.walletai

import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.ml.BankStatementParser
import kz.abdiyev.walletai.util.PdfTextExtractor
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class PdfImportGoldenTest {

    private val categories = listOf(
        kz.abdiyev.walletai.domain.model.CategoryData(1, "Продукты", kz.abdiyev.walletai.domain.model.TransactionType.EXPENSE, "#EF5350", "cart"),
        kz.abdiyev.walletai.domain.model.CategoryData(10, "Переводы", kz.abdiyev.walletai.domain.model.TransactionType.EXPENSE, "#B0BEC5", "swap", isSystem = true)
    )

    @Test
    fun kaspiPdf_parsesOperationsWithoutHeaderPii() {
        val file = File("..\\bank_statement\\kaspi.pdf")
        assumeTrue("kaspi.pdf missing", file.exists())
        val context = RuntimeEnvironment.getApplication()
        val text = PdfTextExtractor.extractTextFromFile(context, file)
        val result = BankStatementParser().parse(text, categories, null, Currency.KZT)

        assertTrue(result.lines.isNotEmpty())
        assertTrue(result.lines.size >= 5)
        assertTrue(result.lines.none { it.description.contains("ИИН", ignoreCase = true) })
        assertTrue(result.lines.none { it.description.contains("www.kaspi", ignoreCase = true) })
    }

    @Test
    fun freedomPdf_parsesOperations() {
        val file = File("..\\bank_statement\\freedom-.pdf")
        assumeTrue("freedom-.pdf missing", file.exists())
        val context = RuntimeEnvironment.getApplication()
        val text = PdfTextExtractor.extractTextFromFile(context, file)
        val result = BankStatementParser().parse(text, categories, null, Currency.KZT)

        assertTrue(result.lines.isNotEmpty())
    }
}
