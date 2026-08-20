package app.bookflow.reader.core.domain

import android.net.Uri

data class BookDocument(
    val uri: Uri,
    val title: String,
    val mimeType: String,
    val textContent: String? = null
) {
    val isReadableText: Boolean
        get() = mimeType == "text/plain" || title.endsWith(".txt", ignoreCase = true)
}
