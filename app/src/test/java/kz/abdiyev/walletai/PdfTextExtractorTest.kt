package kz.abdiyev.walletai

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.TransactionType
import kz.abdiyev.walletai.ml.BankStatementParser
import kz.abdiyev.walletai.util.PdfTextExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class PdfTextExtractorTest {

    private val categories = listOf(
        CategoryData(1, "Продукты", TransactionType.EXPENSE, "#EF5350", "cart"),
        CategoryData(2, "Зарплата", TransactionType.INCOME, "#66BB6A", "wallet")
    )

    @Test
    fun extractText_thenParse_recognizesOperations() {
        PDFBoxResourceLoader.init(RuntimeEnvironment.getApplication())
        val lines = listOf(
            "Statement of account",
            "01.07.2026 -4500.00 KZT Purchase MAGNUM",
            "02.07.2026 +150000.00 KZT Salary deposit"
        )
        val pdfFile = File.createTempFile("statement", ".pdf").apply { deleteOnExit() }
        PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)
            PDPageContentStream(document, page).use { stream ->
                stream.beginText()
                stream.setFont(PDType1Font.HELVETICA, 12f)
                stream.newLineAtOffset(50f, 700f)
                lines.forEach { line ->
                    stream.showText(line)
                    stream.newLineAtOffset(0f, -20f)
                }
                stream.endText()
            }
            document.save(pdfFile)
        }

        val context = RuntimeEnvironment.getApplication()
        val text = PdfTextExtractor.extractTextFromFile(context, pdfFile)

        assertTrue("extracted text should contain MAGNUM line", text.contains("MAGNUM"))

        val result = BankStatementParser().parse(text, categories, null, Currency.KZT)
        val operations = result.lines.filter { it.money.amountMinor > 0 }
        assertTrue("expected at least 2 parsed operations, got ${operations.size}", operations.size >= 2)
        assertTrue(
            "expected an expense of 4500 KZT",
            operations.any { it.type == TransactionType.EXPENSE && it.money.amountMinor == 450000L }
        )
        assertTrue(
            "expected an income of 150000 KZT",
            operations.any { it.type == TransactionType.INCOME && it.money.amountMinor == 15000000L }
        )
    }
}
