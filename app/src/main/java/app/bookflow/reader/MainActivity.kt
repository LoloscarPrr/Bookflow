package app.bookflow.reader

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            val title = queryDisplayName(context, uri) ?: "Libro"
            val text = if (mimeType == "text/plain" || title.endsWith(".txt", true)) {
                runCatching {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                }.getOrNull()
            } else {
                null
            }

            importedBook = BookDocument(
                uri = uri,
                title = title,
                mimeType = mimeType,
                textContent = text
            )
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (openedBook != null) {
                ReaderScreen(
                    book = openedBook!!,
                    onBack = { openedBook = null }
                )
            } else {
                Scaffold(
                    topBar = { TopAppBar(title = { Text("BookFlow") }) }
                ) { padding ->
                    LibraryScreen(
                        modifier = Modifier.padding(padding),
                        book = importedBook,
                        onImport = {
                            launcher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "text/plain"
                                )
                            )
                        },
                        onOpen = { openedBook = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    modifier: Modifier = Modifier,
    book: BookDocument?,
    onImport: () -> Unit,
    onOpen: (BookDocument) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Tu biblioteca", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Lectura · Narración · IA",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Button(onClick = onImport) {
            Text("Importar libro")
        }

        if (book == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Aún no hay libros. Importa tu primer PDF, DOCX o TXT.")
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(book) }
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(book.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when {
                            book.isReadableText -> "TXT · listo para leer"
                            book.mimeType.contains("pdf", ignoreCase = true) -> "PDF · importado"
                            book.mimeType.contains("wordprocessingml", ignoreCase = true) -> "DOCX · importado"
                            else -> "Documento importado"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderScreen(book: BookDocument, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(book.title) },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Volver") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            if (book.textContent != null) {
                Text(
                    book.textContent,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    "El archivo ya está dentro de BookFlow. El parser de este formato se conectará en la siguiente iteración sin cambiar la biblioteca.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Lectura")
                    Text("Narración")
                    Text("IA")
                }
            }
        }
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    var cursor: Cursor? = null
    return try {
        cursor = context.contentResolver.query(uri, null, null, null, null)
        val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME) ?: -1
        if (cursor != null && cursor.moveToFirst() && nameIndex >= 0) {
            cursor.getString(nameIndex)
        } else {
            null
        }
    } finally {
        cursor?.close()
    }
}
