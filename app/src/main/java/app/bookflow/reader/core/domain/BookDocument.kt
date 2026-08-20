package app.bookflow.reader.core.domain

data class BookDocument(
    val uriString: String,
    val title: String,
    val mimeType: String,
    val textContent: String? = null
) {
    val isReadableText: Boolean
        get() = mimeType == "text/plain" || title.endsWith(".txt", ignoreCase = true)
}
