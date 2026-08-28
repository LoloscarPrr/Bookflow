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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.bookflow.reader.core.data.DocxTextExtractor
import app.bookflow.reader.core.data.OfflineNeuralVoiceRenderer
import app.bookflow.reader.core.data.RuleBasedSceneDirector
import app.bookflow.reader.core.domain.BookDocument
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        setContent { BookFlowApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookFlowApp() {
    val context = LocalContext.current
    val renderer = remember(context.applicationContext) {
        OfflineNeuralVoiceRenderer(context.applicationContext)
    }
    var importedBook by remember { mutableStateOf<BookDocument?>(null) }
    var openedBook by remember { mutableStateOf<BookDocument?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val mime = context.contentResolver.getType(uri).orEmpty()
            val title = queryDisplayName(context, uri) ?: "Libro"
            val text = when {
                mime == "text/plain" || title.endsWith(".txt", true) -> runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()

                mime.contains("wordprocessingml", true) || title.endsWith(".docx", true) -> runCatching {
                    context.contentResolver.openInputStream(uri)?.use(DocxTextExtractor::extract)
                }.getOrNull()

                else -> null
            }
            importedBook = BookDocument(uri.toString(), title, mime, text)
        }
    }

    DisposableEffect(renderer) {
        onDispose { renderer.close() }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            if (openedBook != null) {
                ReaderScreen(
                    book = openedBook!!,
                    renderer = renderer,
                    onBack = { openedBook = null },
                )
            } else {
                Scaffold(topBar = { TopAppBar(title = { Text("BookFlow") }) }) { padding ->
                    LibraryScreen(
                        modifier = Modifier.padding(padding),
                        book = importedBook,
                        onImport = {
                            launcher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "text/plain",
                                ),
                            )
                        },
                        onOpen = { openedBook = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    modifier: Modifier,
    book: BookDocument?,
    onImport: () -> Unit,
    onOpen: (BookDocument) -> Unit,
) {
    Column(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Tu biblioteca", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Lectura · Narración local · IA",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Button(onClick = onImport) { Text("Importar libro") }
        if (book == null) {
            Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text("Importa un libro para leerlo o escucharlo sin gastar créditos.")
            }
        } else {
            Card(Modifier.fillMaxWidth().clickable { onOpen(book) }) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (book.isPdf) {
                        PdfCover(book.uriString, Modifier.width(105.dp).height(150.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            cleanBookTitle(book.title),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            when {
                                book.isPdf -> "PDF · lectura + voz neuronal local"
                                book.textContent != null -> "Listo para narrar sin créditos"
                                else -> "Documento importado"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfCover(uriString: String, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap = remember(uriString) { renderPdfPageFromUri(context, uriString, 0, .55f) }
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), "Portada", modifier, contentScale = ContentScale.Crop)
    } else {
        Card(modifier) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("PDF") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScreen(
    book: BookDocument,
    renderer: OfflineNeuralVoiceRenderer,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val director = remember { RuleBasedSceneDirector() }
    val scope = rememberCoroutineScope()
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var generating by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Preparando libro…") }
    var pdfText by remember(book.uriString) { mutableStateOf<String?>(null) }
    var offset by remember(book.uriString) { mutableIntStateOf(0) }
    var autoContinue by remember(book.uriString) { mutableStateOf(false) }
    var generationToken by remember(book.uriString) { mutableIntStateOf(0) }

    LaunchedEffect(book.uriString) {
        if (book.isPdf) {
            pdfText = withContext(Dispatchers.IO) {
                runCatching { extractPdfText(context, book.uriString) }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
            }
            status = if (pdfText != null) {
                "PDF listo · voz neuronal local sin créditos."
            } else {
                "Este PDF no contiene texto extraíble."
            }
        } else {
            status = "Listo · voz neuronal local sin créditos."
        }
    }

    DisposableEffect(book.uriString) {
        onDispose {
            autoContinue = false
            generationToken += 1
            runCatching { player?.release() }
        }
    }

    val raw = book.textContent ?: pdfText
    val narratable = raw?.let(::cleanNarratableText)
    val progress = if (narratable.isNullOrEmpty()) {
        0
    } else {
        ((offset.toFloat() / narratable.length) * 100).toInt().coerceIn(0, 100)
    }

    var narrateAt: ((Int) -> Unit)? = null
    narrateAt = fun(start: Int) {
        val text = narratable ?: return
        val chunk = nextNarrationChunk(text, start)
        if (chunk.text.isBlank()) {
            autoContinue = false
            offset = text.length
            status = "Llegaste al final del libro."
            return
        }

        val requestToken = generationToken + 1
        generationToken = requestToken
        generating = true
        status = if (start == 0) {
            "Preparando voz neuronal local… La primera vez puede demorar un poco."
        } else {
            "Preparando el siguiente tramo en el teléfono…"
        }

        scope.launch {
            try {
                val plan = director.createPlan(chunk.text)
                val segment = renderer.render(plan, plan.speakerId)
                if (requestToken != generationToken || !autoContinue) return@launch

                runCatching { player?.release() }
                player = MediaPlayer().apply {
                    setDataSource(segment.localUri)
                    setOnPreparedListener {
                        if (requestToken != generationToken || !autoContinue) {
                            it.release()
                            return@setOnPreparedListener
                        }
                        offset = start
                        status = "Reproduciendo · ${plan.speakerLabel} · ${plan.mood} · sin créditos"
                        it.start()
                    }
                    setOnCompletionListener {
                        offset = chunk.end
                        if (autoContinue && chunk.end < text.length) {
                            status = "Continuando narración local…"
                            narrateAt?.invoke(chunk.end)
                        } else if (chunk.end >= text.length) {
                            autoContinue = false
                            status = "Llegaste al final del libro."
                        } else {
                            status = "Pausado en ${((chunk.end.toFloat() / text.length) * 100).toInt()}%"
                        }
                    }
                    setOnErrorListener { _, _, _ ->
                        autoContinue = false
                        status = "No pude reproducir este tramo. Inténtalo otra vez."
                        true
                    }
                    prepareAsync()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (requestToken == generationToken) {
                    autoContinue = false
                    status = "No pude iniciar la voz local: ${error.message ?: "error desconocido"}"
                }
            } finally {
                if (requestToken == generationToken) generating = false
            }
        }
    }

    fun startOrResume() {
        if (narratable == null) return
        autoContinue = true
        narrateAt?.invoke(offset)
    }

    fun stopNarration() {
        autoContinue = false
        generationToken += 1
        generating = false
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        status = "Narración detenida en $progress%."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        cleanBookTitle(book.title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    Button(onClick = onBack, modifier = Modifier.padding(horizontal = 6.dp)) {
                        Text("Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Card(Modifier.fillMaxWidth().padding(12.dp)) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Narración local",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text("Voz neuronal española · funciona sin internet ni créditos")
                    Text("Posición de narración: $progress%")
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = narratable != null && !generating && !autoContinue,
                            onClick = { startOrResume() },
                        ) {
                            Text(
                                when {
                                    generating -> "Preparando…"
                                    offset == 0 -> "Narrar"
                                    else -> "Reanudar"
                                },
                            )
                        }
                        if (autoContinue || player != null || generating) {
                            OutlinedButton(onClick = { stopNarration() }) { Text("Detener") }
                        }
                        if (offset > 0 && !autoContinue) {
                            OutlinedButton(
                                enabled = !generating,
                                onClick = {
                                    offset = (offset - NARRATION_CHUNK_SIZE).coerceAtLeast(0)
                                    status = "Retrocediste un tramo."
                                },
                            ) {
                                Text("Atrás")
                            }
                        }
                    }
                    Text(status, style = MaterialTheme.typography.bodySmall)
                }
            }

            when {
                book.isPdf -> Box(Modifier.weight(1f)) { PdfBookReader(book.uriString) }
                book.textContent != null -> Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                ) {
                    Text(book.textContent, style = MaterialTheme.typography.bodyLarge)
                }

                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Este formato todavía no puede narrarse.")
                }
            }
        }
    }
}

private data class NarrationChunk(val text: String, val end: Int)

private const val NARRATION_CHUNK_SIZE = 1_150

private fun nextNarrationChunk(
    text: String,
    start: Int,
    max: Int = NARRATION_CHUNK_SIZE,
): NarrationChunk {
    if (start >= text.length) return NarrationChunk("", text.length)
    val hard = (start + max).coerceAtMost(text.length)
    if (hard == text.length) return NarrationChunk(text.substring(start), hard)
    val window = text.substring(start, hard)
    val candidates = listOf(
        window.lastIndexOf(". "),
        window.lastIndexOf("! "),
        window.lastIndexOf("? "),
        window.lastIndexOf("\n\n"),
    ).filter { it > max / 2 }
    val cut = candidates.maxOrNull()?.plus(1)
        ?: window.lastIndexOf(' ').takeIf { it > max / 2 }
        ?: window.length
    return NarrationChunk(window.substring(0, cut).trim(), start + cut)
}

private fun cleanNarratableText(text: String) = text
    .replace(Regex("(?m)^\\s*\\d+\\s*$"), "")
    .replace(Regex("[ \\t]+"), " ")
    .replace(Regex("\\n{3,}"), "\n\n")
    .trim()

private fun extractPdfText(context: Context, uriString: String): String {
    val bytes = context.contentResolver.openInputStream(Uri.parse(uriString))
        ?.use { it.readBytes() }
        ?: error("No pude abrir el PDF")
    return PDDocument.load(bytes).use { document ->
        PDFTextStripper().apply { sortByPosition = true }
            .getText(document)
            .replace("\u0000", "")
            .trim()
    }
}

@Composable
private fun PdfBookReader(uriString: String) {
    val context = LocalContext.current
    var renderer by remember(uriString) { mutableStateOf<PdfRenderer?>(null) }
    var descriptor by remember(uriString) { mutableStateOf<ParcelFileDescriptor?>(null) }
    DisposableEffect(uriString) {
        descriptor = runCatching {
            context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")
        }.getOrNull()
        renderer = descriptor?.let { runCatching { PdfRenderer(it) }.getOrNull() }
        onDispose {
            runCatching { renderer?.close() }
            runCatching { descriptor?.close() }
        }
    }

    val pdf = renderer ?: return Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) { Text("Abriendo PDF…") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (index in 0 until pdf.pageCount) {
            remember(uriString, index) { renderPdfPage(pdf, index, 1.5f) }?.let {
                Image(
                    it.asImageBitmap(),
                    "Página ${index + 1}",
                    Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }
    }
}

private fun renderPdfPageFromUri(
    context: Context,
    uri: String,
    index: Int,
    scale: Float,
): Bitmap? = runCatching {
    context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { descriptor ->
        PdfRenderer(descriptor).use { renderer -> renderPdfPage(renderer, index, scale) }
    }
}.getOrNull()

private fun renderPdfPage(renderer: PdfRenderer, index: Int, scale: Float): Bitmap? = runCatching {
    renderer.openPage(index).use { page ->
        Bitmap.createBitmap(
            (page.width * scale).toInt(),
            (page.height * scale).toInt(),
            Bitmap.Config.ARGB_8888,
        ).also { bitmap ->
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        }
    }
}.getOrNull()

private val BookDocument.isPdf: Boolean
    get() = mimeType.contains("pdf", true) || title.endsWith(".pdf", true)

private fun cleanBookTitle(title: String) = title
    .substringBeforeLast(".")
    .replace('_', ' ')
    .replace(Regex("\\s+"), " ")
    .trim()

private fun queryDisplayName(context: Context, uri: Uri): String? {
    var cursor: Cursor? = null
    return try {
        cursor = context.contentResolver.query(uri, null, null, null, null)
        val current = cursor ?: return null
        val index = current.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (current.moveToFirst() && index >= 0) current.getString(index) else null
    } finally {
        cursor?.close()
    }
}
