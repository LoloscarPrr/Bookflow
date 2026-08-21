package app.bookflow.reader

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.bookflow.reader.core.data.DocxTextExtractor
import app.bookflow.reader.core.domain.BookDocument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BookFlowApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookFlowApp() {
    val context = LocalContext.current
    var importedBook by remember { mutableStateOf<BookDocument?>(null) }
    var openedBook by remember { mutableStateOf<BookDocument?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            val title = queryDisplayName(context, uri) ?: "Libro"
            val isTxt = mimeType == "text/plain" || title.endsWith(".txt", true)
            val isDocx = mimeType.contains("wordprocessingml", true) || title.endsWith(".docx", true)
            val text = when {
                isTxt -> runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
                isDocx -> runCatching { context.contentResolver.openInputStream(uri)?.use(DocxTextExtractor::extract) }.getOrNull()
                else -> null
            }
            importedBook = BookDocument(uri.toString(), title, mimeType, text)
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            openedBook?.let { book -> ReaderScreen(book, onBack = { openedBook = null }) }
                ?: Scaffold(topBar = { TopAppBar(title = { Text("BookFlow") }) }) { padding ->
                    LibraryScreen(
                        Modifier.padding(padding), importedBook,
                        onImport = { launcher.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain")) },
                        onOpen = { openedBook = it }
                    )
                }
        }
    }
}

@Composable
private fun LibraryScreen(modifier: Modifier, book: BookDocument?, onImport: () -> Unit, onOpen: (BookDocument) -> Unit) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Tu biblioteca", style = MaterialTheme.typography.headlineMedium)
        Text("Lectura · Narración · IA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Button(onClick = onImport) { Text("Importar libro") }
        if (book == null) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("Aún no hay libros. Importa tu primer PDF, DOCX o TXT.") }
        } else {
            Card(Modifier.fillMaxWidth().clickable { onOpen(book) }) {
                Column(Modifier.padding(18.dp)) {
                    Text(book.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(when {
                        book.isPdf -> "PDF · listo para leer"
                        book.isDocx && book.textContent != null -> "DOCX · listo para leer"
                        book.isDocx -> "DOCX · no pude extraer el texto"
                        book.isReadableText -> "TXT · listo para leer"
                        else -> "Documento importado"
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScreen(book: BookDocument, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = { Button(onClick = onBack, modifier = Modifier.padding(horizontal = 6.dp)) { Text("Volver") } }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                book.textContent != null -> TextReader(book.textContent)
                book.isPdf -> PdfBookReader(book.uriString)
                book.isDocx -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No pude extraer texto de este DOCX.", modifier = Modifier.padding(24.dp)) }
                else -> Text("Formato no compatible todavía.", modifier = Modifier.padding(20.dp))
            }
        }
    }
}

@Composable
private fun TextReader(text: String) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) { Text(text, style = MaterialTheme.typography.bodyLarge) }
}

@Composable
private fun PdfBookReader(uriString: String) {
    val context = LocalContext.current
    var renderer by remember(uriString) { mutableStateOf<PdfRenderer?>(null) }
    var descriptor by remember(uriString) { mutableStateOf<ParcelFileDescriptor?>(null) }
    var error by remember(uriString) { mutableStateOf<String?>(null) }
    DisposableEffect(uriString) {
        try {
            descriptor = context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")
            renderer = descriptor?.let { PdfRenderer(it) }
        } catch (_: Exception) { error = "No pude abrir este PDF. Prueba importándolo nuevamente." }
        onDispose { runCatching { renderer?.close() }; runCatching { descriptor?.close() } }
    }
    if (error != null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error!!, Modifier.padding(24.dp)) }; return }
    val pdf = renderer ?: run { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Abriendo PDF…") }; return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (index in 0 until pdf.pageCount) {
            remember(uriString, index) { renderPdfPage(pdf, index) }?.let { bitmap ->
                Image(bitmap.asImageBitmap(), "Página ${index + 1}", Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
            }
        }
    }
}

private fun renderPdfPage(renderer: PdfRenderer, index: Int): Bitmap? = runCatching {
    renderer.openPage(index).use { page ->
        val scale = 1.5f
        Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888).also { bitmap ->
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }
    }
}.getOrNull()

private val BookDocument.isPdf get() = mimeType.contains("pdf", true) || title.endsWith(".pdf", true)
private val BookDocument.isDocx get() = mimeType.contains("wordprocessingml", true) || title.endsWith(".docx", true)

private fun queryDisplayName(context: Context, uri: Uri): String? {
    var cursor: Cursor? = null
    return try {
        cursor = context.contentResolver.query(uri, null, null, null, null)
        val c = cursor ?: return null
        val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (c.moveToFirst() && i >= 0) c.getString(i) else null
    } finally { cursor?.close() }
}
