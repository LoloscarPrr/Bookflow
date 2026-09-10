package app.bookflow.reader

import android.content.ComponentName
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
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.bookflow.reader.core.data.BookSessionStore
import app.bookflow.reader.core.data.DocumentTextLoader
import app.bookflow.reader.core.diagnostics.CrashReporter
import app.bookflow.reader.core.diagnostics.DiagnosticArea
import app.bookflow.reader.core.domain.BookDocument
import app.bookflow.reader.narration.*
import app.bookflow.reader.presentation.BookFlowLayout
import app.bookflow.reader.presentation.WIDE_STAGE_MAX_WIDTH_DP
import app.bookflow.reader.presentation.rememberBookFlowLayout
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CancellationException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReporter.initialize(applicationContext)
        PDFBoxResourceLoader.init(applicationContext)
        setContent { BookFlowApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookFlowApp() {
    val context = LocalContext.current
    val layout = rememberBookFlowLayout()
    val store = remember(context.applicationContext) { BookSessionStore(context.applicationContext) }
    var importedBook by remember { mutableStateOf(store.loadLastBook()) }
    var openedBook by remember { mutableStateOf<BookDocument?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }.onFailure { CrashReporter.recordNonFatal(DiagnosticArea.URI_PERMISSION, it) }
            val book = BookDocument(
                uri.toString(),
                queryDisplayName(context, uri) ?: "Libro",
                context.contentResolver.getType(uri).orEmpty(),
            )
            store.saveLastBook(book)
            importedBook = book
        }
    }

    LaunchedEffect(layout.layoutClass) {
        CrashReporter.setLayoutClass(layout.layoutClass.name.lowercase())
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(
                    Modifier.fillMaxHeight().then(
                        if (layout.isWide) {
                            Modifier.widthIn(max = WIDE_STAGE_MAX_WIDTH_DP.dp).fillMaxWidth()
                        } else Modifier.fillMaxWidth(),
                    ),
                ) {
                    if (openedBook != null) {
                        ReaderScreen(openedBook!!, layout) { openedBook = null }
                    } else {
                        Scaffold(topBar = { TopAppBar(title = { Text("BookFlow") }) }) { padding ->
                            LibraryScreen(
                                Modifier.padding(padding),
                                importedBook,
                                layout,
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
    }
}

@Composable
private fun LibraryScreen(
    modifier: Modifier,
    book: BookDocument?,
    layout: BookFlowLayout,
    onImport: () -> Unit,
    onOpen: (BookDocument) -> Unit,
) {
    Column(
        modifier.fillMaxSize().padding(horizontal = layout.horizontalPadding, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Tu biblioteca", style = MaterialTheme.typography.headlineMedium)
        Text("Lectura · Narración local · IA", fontWeight = FontWeight.SemiBold)
        Button(onClick = onImport, modifier = if (layout.isCompact) Modifier.fillMaxWidth() else Modifier) {
            Text("Importar libro")
        }
        if (book == null) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Importa un libro para leerlo o escucharlo sin gastar créditos.")
            }
        } else {
            Card(Modifier.fillMaxWidth().clickable { onOpen(book) }) {
                if (layout.isCompact) {
                    Column(
                        Modifier.padding(layout.cardPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (book.isPdf) PdfCover(book.uriString, Modifier.width(105.dp).height(150.dp))
                        BookSummary(book, Modifier.fillMaxWidth())
                    }
                } else {
                    Row(
                        Modifier.padding(layout.cardPadding),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (book.isPdf) PdfCover(book.uriString, Modifier.width(105.dp).height(150.dp))
                        BookSummary(book, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BookSummary(book: BookDocument, modifier: Modifier) {
    Column(modifier) {
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
                book.isDocx -> "DOCX · listo para leer y narrar"
                book.isReadableText -> "TXT · listo para leer y narrar"
                else -> "Documento importado"
            },
        )
    }
}

@Composable
private fun PdfCover(uriString: String, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap = remember(uriString) { renderPdfPageFromUri(context, uriString, 0, .55f) }
    if (bitmap != null) Image(bitmap.asImageBitmap(), "Portada", modifier, contentScale = ContentScale.Crop)
    else Card(modifier) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("PDF") } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScreen(book: BookDocument, layout: BookFlowLayout, onBack: () -> Unit) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val loader = remember(appContext) { DocumentTextLoader(appContext) }
    val store = remember(appContext) { BookSessionStore(appContext) }
    val controller = rememberNarrationController()
    val playback by NarrationPlaybackStateHolder.state.collectAsState()
    var loadedText by remember(book.uriString) { mutableStateOf(book.textContent) }
    var loadStatus by remember(book.uriString) { mutableStateOf("Preparando libro…") }

    LaunchedEffect(book.uriString) {
        try {
            val text = loader.load(book)
            val narratable = cleanNarratableText(text).takeIf { it.isNotBlank() }
            loadedText = text.takeIf { it.isNotBlank() }
            if (narratable == null) {
                loadStatus = "Este documento no contiene texto extraíble."
            } else {
                val saved = store.loadNarrationOffset(book.uriString).coerceIn(0, narratable.length)
                loadStatus = "Listo · voz neuronal local sin internet ni créditos."
                NarrationPlaybackStateHolder.restoreIfInactive(book.uriString, saved, narratable.length, loadStatus)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            CrashReporter.recordNonFatal(importArea(book), error)
            loadedText = null
            loadStatus = "No pude abrir el texto de este documento."
        }
    }

    val narratable = loadedText?.let(::cleanNarratableText)?.takeIf { it.isNotBlank() }
    val isCurrent = playback.bookUri == book.uriString
    val phase = if (isCurrent) playback.phase else NarrationPlaybackPhase.IDLE
    val offset = if (isCurrent) playback.offset else store.loadNarrationOffset(book.uriString)
    val total = if (isCurrent) playback.totalChars else narratable?.length ?: 0
    val progress = narrationProgressPercent(offset, total)
    val hasSegment = isCurrent && (controller?.mediaItemCount ?: 0) > 0
    val preparing = phase == NarrationPlaybackPhase.PREPARING
    val canNarrate = narratable != null && controller != null && !preparing
    val showStop = isCurrent && (preparing || phase == NarrationPlaybackPhase.PLAYING || hasSegment)

    fun primaryAction() {
        when {
            phase == NarrationPlaybackPhase.PLAYING -> controller?.pause()
            phase == NarrationPlaybackPhase.PAUSED && hasSegment -> controller?.play()
            else -> {
                val requested = if (phase == NarrationPlaybackPhase.COMPLETED) 0
                else store.loadNarrationOffset(book.uriString)
                runCatching { NarrationPlaybackService.play(appContext, book, requested) }
                    .onFailure { CrashReporter.recordNonFatal(DiagnosticArea.PLAYBACK, it) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cleanBookTitle(book.title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Card(
                Modifier.fillMaxWidth()
                    .padding(horizontal = if (layout.isCompact) 8.dp else 12.dp, vertical = 6.dp),
            ) {
                Column(
                    Modifier.padding(horizontal = layout.cardPadding, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Narración local", fontWeight = FontWeight.SemiBold)
                        Text("$progress%")
                    }
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            enabled = canNarrate,
                            onClick = ::primaryAction,
                            modifier = if (layout.isCompact) Modifier.weight(1f) else Modifier,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                        ) { Text(primaryButtonLabel(phase, offset)) }
                        if (showStop) {
                            OutlinedButton(
                                onClick = {
                                    runCatching { NarrationPlaybackService.stop(appContext) }
                                        .onFailure { CrashReporter.recordNonFatal(DiagnosticArea.PLAYBACK, it) }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                            ) { Text("Detener") }
                        }
                        if (isCurrent && offset > 0 && !preparing) {
                            TextButton(
                                onClick = {
                                    runCatching { NarrationPlaybackService.rewind(appContext, book) }
                                        .onFailure { CrashReporter.recordNonFatal(DiagnosticArea.PLAYBACK, it) }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 9.dp),
                            ) { Text("Atrás") }
                        }
                    }
                    Text(if (isCurrent) playback.status else loadStatus, style = MaterialTheme.typography.bodySmall)
                }
            }

            when {
                book.isPdf -> Box(Modifier.weight(1f)) { PdfBookReader(book.uriString, layout) }
                loadedText != null -> Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = layout.horizontalPadding, vertical = 20.dp),
                ) { Text(loadedText!!, style = MaterialTheme.typography.bodyLarge) }
                loadStatus == "Preparando libro…" -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center,
                ) { Text("Abriendo documento…") }
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(loadStatus) }
            }
        }
    }
}

private fun primaryButtonLabel(phase: NarrationPlaybackPhase, offset: Int) = when (phase) {
    NarrationPlaybackPhase.PREPARING -> "Preparando…"
    NarrationPlaybackPhase.PLAYING -> "Pausar"
    NarrationPlaybackPhase.PAUSED -> "Reanudar"
    NarrationPlaybackPhase.COMPLETED -> "Desde el inicio"
    NarrationPlaybackPhase.ERROR -> "Reintentar"
    NarrationPlaybackPhase.IDLE -> if (offset == 0) "Narrar" else "Continuar"
}

private fun importArea(book: BookDocument) = when {
    book.isPdf -> DiagnosticArea.PDF_EXTRACTION
    book.isDocx -> DiagnosticArea.DOCX_IMPORT
    else -> DiagnosticArea.TEXT_IMPORT
}

@Composable
private fun rememberNarrationController(): MediaController? {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    DisposableEffect(context.applicationContext) {
        val token = SessionToken(
            context.applicationContext,
            ComponentName(context.applicationContext, NarrationPlaybackService::class.java),
        )
        val future = MediaController.Builder(context.applicationContext, token).buildAsync()
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { controller = it }
                    .onFailure { CrashReporter.recordNonFatal(DiagnosticArea.PLAYBACK, it) }
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            controller = null
            MediaController.releaseFuture(future)
        }
    }
    return controller
}

@Composable
private fun PdfBookReader(uriString: String, layout: BookFlowLayout) {
    val context = LocalContext.current
    var renderer by remember(uriString) { mutableStateOf<PdfRenderer?>(null) }
    var descriptor by remember(uriString) { mutableStateOf<ParcelFileDescriptor?>(null) }
    DisposableEffect(uriString) {
        descriptor = runCatching {
            context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")
        }.onFailure { CrashReporter.recordNonFatal(DiagnosticArea.PDF_OPEN, it) }.getOrNull()
        renderer = descriptor?.let {
            runCatching { PdfRenderer(it) }
                .onFailure { error -> CrashReporter.recordNonFatal(DiagnosticArea.PDF_OPEN, error) }
                .getOrNull()
        }
        onDispose {
            runCatching { renderer?.close() }
            runCatching { descriptor?.close() }
        }
    }
    val pdf = renderer ?: return Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Abriendo PDF…")
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(if (layout.isCompact) 4.dp else 8.dp),
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
}.onFailure { CrashReporter.recordNonFatal(DiagnosticArea.PDF_RENDER, it) }.getOrNull()

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
}.onFailure { CrashReporter.recordNonFatal(DiagnosticArea.PDF_RENDER, it) }.getOrNull()

private fun cleanBookTitle(title: String) = title.substringBeforeLast(".")
    .replace('_', ' ').replace(Regex("\\s+"), " ").trim()

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
