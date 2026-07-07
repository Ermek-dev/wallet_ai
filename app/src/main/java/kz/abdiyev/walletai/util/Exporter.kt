package kz.abdiyev.walletai.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import kz.abdiyev.walletai.domain.model.CategoryData
import kz.abdiyev.walletai.domain.model.Currency
import kz.abdiyev.walletai.domain.model.TransactionData
import kz.abdiyev.walletai.domain.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Exporter {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))
    private val dateOnlyFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))

    fun exportToCsv(
        context: Context,
        transactions: List<TransactionData>,
        categories: List<CategoryData>,
        currency: Currency
    ): File {
        val categoryMap = categories.associateBy { it.id }
        val fileName = "WalletAI_Report_${System.currentTimeMillis()}.csv"
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val file = File(directory, fileName)

        FileOutputStream(file).use { fos ->
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val writer = fos.writer(Charsets.UTF_8)
            writer.write("ID;Дата;Тип;Категория;Сумма;Описание;Авто-категоризация\n")
            for (tx in transactions) {
                val dateStr = dateOnlyFormat.format(Date(tx.date))
                val typeStr = if (tx.type == TransactionType.INCOME) "Доход" else "Расход"
                val catName = categoryMap[tx.categoryId]?.name ?: "Другое"
                val amountStr = MoneyFormatter.format(tx.money)
                val cleanDesc = tx.description.replace(";", " ").replace("\n", " ")
                val autoStr = if (tx.isAutoCategorized) "Да (ИИ)" else "Нет"
                writer.write("${tx.id};$dateStr;$typeStr;$catName;$amountStr;$cleanDesc;$autoStr\n")
            }
            writer.flush()
        }
        return file
    }

    fun exportToPdf(
        context: Context,
        transactions: List<TransactionData>,
        categories: List<CategoryData>
    ): File {
        val categoryMap = categories.associateBy { it.id }
        val fileName = "WalletAI_Report_${System.currentTimeMillis()}.pdf"
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val file = File(directory, fileName)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        val textPaint = Paint().apply { isAntiAlias = true; color = Color.BLACK }

        paint.color = Color.parseColor("#121212")
        canvas.drawRect(0f, 0f, 595f, 100f, paint)
        textPaint.color = Color.WHITE
        textPaint.textSize = 22f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("WALLETAI — ФИНАНСОВЫЙ ОТЧЁТ", 30f, 45f, textPaint)
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Сформирован: ${dateFormat.format(Date())}", 30f, 75f, textPaint)

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val incomes = transactions.filter { it.type == TransactionType.INCOME }
        val expenseSum = expenses.sumOf { it.money.amountMinor }
        val incomeSum = incomes.sumOf { it.money.amountMinor }
        val currency = transactions.firstOrNull()?.money?.currency ?: Currency.KZT

        paint.color = Color.parseColor("#F5F5F5")
        canvas.drawRect(30f, 120f, 565f, 200f, paint)
        textPaint.color = Color.BLACK
        textPaint.textSize = 12f
        canvas.drawText("Доходы: ${MoneyFormatter.format(kz.abdiyev.walletai.domain.model.Money(incomeSum, currency))}", 45f, 175f, textPaint)
        canvas.drawText("Расходы: ${MoneyFormatter.format(kz.abdiyev.walletai.domain.model.Money(expenseSum, currency))}", 220f, 175f, textPaint)
        canvas.drawText(
            "Баланс: ${MoneyFormatter.format(kz.abdiyev.walletai.domain.model.Money(incomeSum - expenseSum, currency))}",
            400f, 175f, textPaint
        )

        var currentY = 295f
        textPaint.textSize = 10f
        for (tx in transactions.take(15)) {
            val line = "${dateOnlyFormat.format(Date(tx.date))} | ${categoryMap[tx.categoryId]?.name ?: "Другое"} | ${MoneyFormatter.format(tx.money, showSign = true, type = tx.type)} | ${tx.description.take(20)}"
            canvas.drawText(line, 40f, currentY, textPaint)
            currentY += 24f
        }

        textPaint.color = Color.LTGRAY
        textPaint.textSize = 8f
        canvas.drawText("Сгенерировано в приложении WalletAI.", 30f, 800f, textPaint)
        pdfDocument.finishPage(page)
        FileOutputStream(file).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        return file
    }
}
