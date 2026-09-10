package app.bookflow.reader.core.data

import android.content.Context
import android.net.Uri
import app.bookflow.reader.core.domain.BookDocument
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DocumentTextLoader(private val context: Context) {
    suspend fun load(book: BookDocument): String = withContext(Dispatchers.IO) {
        book.textContent ?: when {
            book.isPdf -> loadPdf(book.uriString)
            book.isDocx -> open(book.uriString, DocxTextExtractor::extract)
            book.isReadableText -> open(book.uriString) { input ->
                input.bufferedReader().use { it.readText() }
            }
            else -> error("Formato de documento no compatible")
        }
    }

    private fun loadPdf(uriString: String): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        val bytes = open(uriString) { it.readBytes() }
        return PDDocument.load(bytes).use { document ->
            PDFTextStripper().apply { sortByPosition = true }
                .getText(document)
                .replace("\u0000", "")
                .trim()
        }
    }

    private fun <T> open(uriString: String, read: (InputStream) -> T): T {
        val input = context.contentResolver.openInputStream(Uri.parse(uriString))
            ?: error("No se pudo abrir el documento")
        return input.use(read)
    }
}
