package app.bookflow.reader.core.data

import android.content.Context
import app.bookflow.reader.core.domain.BookDocument
import java.security.MessageDigest

class BookSessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveLastBook(book: BookDocument) {
        preferences.edit()
            .putString(LAST_URI, book.uriString)
            .putString(LAST_TITLE, book.title)
            .putString(LAST_MIME, book.mimeType)
            .apply()
    }

    fun loadLastBook(): BookDocument? {
        val uri = preferences.getString(LAST_URI, null)?.takeIf { it.isNotBlank() } ?: return null
        return BookDocument(
            uriString = uri,
            title = preferences.getString(LAST_TITLE, null).orEmpty().ifBlank { "Libro" },
            mimeType = preferences.getString(LAST_MIME, null).orEmpty(),
        )
    }

    fun saveNarrationOffset(uriString: String, offset: Int) {
        preferences.edit().putInt(offsetKey(uriString), offset.coerceAtLeast(0)).apply()
    }

    fun loadNarrationOffset(uriString: String): Int =
        preferences.getInt(offsetKey(uriString), 0).coerceAtLeast(0)

    private fun offsetKey(uriString: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(uriString.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return "narration_offset_$digest"
    }

    private companion object {
        const val PREFS = "bookflow_session"
        const val LAST_URI = "last_book_uri"
        const val LAST_TITLE = "last_book_title"
        const val LAST_MIME = "last_book_mime"
    }
}
