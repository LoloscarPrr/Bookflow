package app.bookflow.reader

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.rememberCoroutineScope
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
import app.bookflow.reader.core.data.RuleBasedSceneDirector
import app.bookflow.reader.core.data.SupabaseVoiceRenderer
import app.bookflow.reader.core.domain.BookDocument
import kotlinx.coroutines.launch

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
            if (openedBook != null) ReaderScreen(openedBook!!, onBack = { openedBook = null })
            else Scaffold(topBar = { TopAppBar(title = { Text("BookFlow") }) }) { padding ->
                LibraryScreen(Modifier.padding(padding), importedBook, { launcher.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/plain")) }, { openedBook = it })
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
        if (book == null) Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("Importa un libro para leerlo o escucharlo con narración IA.") }
        else Card(Modifier.fillMaxWidth().clickable { onOpen(book) }) {
            Column(Modifier.padding(18.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text(when { book.isPdf -> "PDF · listo para leer"; book.isDocx && book.textContent != null -> "DOCX · listo para narrar"; book.isReadableText -> "TXT · listo para narrar"; else -> "Documento importado" })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScreen(book: BookDocument, onBack: () -> Unit) {
    val context = LocalContext.current
    val director = remember { RuleBasedSceneDirector() }
    val renderer = remember(context) { SupabaseVoiceRenderer(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var generating by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Listo para leer.") }
    DisposableEffect(Unit) { onDispose { runCatching { player?.release() } } }
    Scaffold(topBar = { TopAppBar(title = { Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = { Button(onClick = onBack, modifier = Modifier.padding(horizontal = 6.dp)) { Text("Volver") } }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (book.textContent != null) {
                Card(Modifier.fillMaxWidth().padding(12.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Narración IA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("BookFlow analizará automáticamente el texto del libro y dirigirá la voz.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(enabled = !generating, onClick = {
                                generating = true; status = "Analizando y generando narración…"
                                scope.launch {
                                    try {
                                        val passage = book.textContent.take(1200)
                                        val plan = director.createPlan(passage)
                                        val voiceId = if (plan.speakerId == "narrator_female") FEMALE_VOICE_ID else MALE_VOICE_ID
                                        val segment = renderer.render(plan, voiceId)
                                        runCatching { player?.release() }
                                        player = MediaPlayer().apply {
                                            setDataSource(segment.localUri)
                                            setOnPreparedListener { status = "Reproduciendo · ${plan.speakerLabel} · ${plan.mood}"; it.start() }
                                            setOnCompletionListener { status = "Narración terminada." }
                                            prepareAsync()
                                        }
                                    } catch (e: Exception) { status = "No pude narrar: ${e.message ?: "error desconocido"}" }
                                    finally { generating = false }
                                }
                            }) { Text(if (generating) "Generando…" else "Narrar desde aquí") }
                            if (player?.isPlaying == true) Button(onClick = { player?.pause(); status = "Pausado." }) { Text("Pausar") }
                        }
                        Text(status, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) { Text(book.textContent, style = MaterialTheme.typography.bodyLarge) }
            } else if (book.isPdf) {
                Card(Modifier.fillMaxWidth().padding(12.dp)) { Text("Narración de PDF: siguiente paso. Primero debemos extraer el texto del PDF; por ahora puedes leerlo visualmente.", Modifier.padding(14.dp)) }
                Box(Modifier.weight(1f)) { PdfBookReader(book.uriString) }
            } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Este formato todavía no puede narrarse.") }
        }
    }
}

@Composable
private fun PdfBookReader(uriString: String) {
    val context = LocalContext.current
    var renderer by remember(uriString) { mutableStateOf<PdfRenderer?>(null) }
    var descriptor by remember(uriString) { mutableStateOf<ParcelFileDescriptor?>(null) }
    DisposableEffect(uriString) {
        descriptor = runCatching { context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r") }.getOrNull()
        renderer = descriptor?.let { runCatching { PdfRenderer(it) }.getOrNull() }
        onDispose { runCatching { renderer?.close() }; runCatching { descriptor?.close() } }
    }
    val pdf = renderer ?: return Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Abriendo PDF…") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (index in 0 until pdf.pageCount) remember(uriString, index) { renderPdfPage(pdf, index) }?.let { Image(it.asImageBitmap(), "Página ${index + 1}", Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
    }
}

private fun renderPdfPage(renderer: PdfRenderer, index: Int): Bitmap? = runCatching {
    renderer.openPage(index).use { page -> Bitmap.createBitmap((page.width * 1.5f).toInt(), (page.height * 1.5f).toInt(), Bitmap.Config.ARGB_8888).also { bitmap -> bitmap.eraseColor(android.graphics.Color.WHITE); page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) } }
}.getOrNull()
private val BookDocument.isPdf get() = mimeType.contains("pdf", true) || title.endsWith(".pdf", true)
private val BookDocument.isDocx get() = mimeType.contains("wordprocessingml", true) || title.endsWith(".docx", true)
private fun queryDisplayName(context: Context, uri: Uri): String? { var cursor: Cursor? = null; return try { cursor = context.contentResolver.query(uri, null, null, null, null); val c = cursor ?: return null; val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (c.moveToFirst() && i >= 0) c.getString(i) else null } finally { cursor?.close() } }
private const val MALE_VOICE_ID = "pNInz6obpgDQGcFmaJgB"
private const val FEMALE_VOICE_ID = "EXAVITQu4vr4xnSDxMaL"
