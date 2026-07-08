package kz.abdiyev.walletai.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

object PdfTextExtractor {

    fun extractText(context: Context, uri: Uri): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        return context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open PDF stream" }
            PDDocument.load(input).use { document ->
                PDFTextStripper().getText(document)
            }
        }
    }
}
