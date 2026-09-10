package app.bookflow.reader.core.domain

data class BookDocument(
    val uriString: String,
    val title: String,
    val mimeType: String,
    val textContent: String? = null,
) {
    val isReadableText: Boolean
        get() = mimeType == "text/plain" || title.endsWith(".txt", ignoreCase = true)

    val isPdf: Boolean
        get() = mimeType.contains("pdf", ignoreCase = true) || title.endsWith(".pdf", true)

    val isDocx: Boolean
        get() = mimeType.contains("wordprocessingml", ignoreCase = true) || title.endsWith(".docx", true)
}
