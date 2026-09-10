package app.bookflow.reader.narration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val NARRATION_CHUNK_SIZE = 1_150

enum class NarrationPlaybackPhase { IDLE, PREPARING, PLAYING, PAUSED, COMPLETED, ERROR }

data class NarrationPlaybackState(
    val bookUri: String? = null,
    val offset: Int = 0,
    val totalChars: Int = 0,
    val phase: NarrationPlaybackPhase = NarrationPlaybackPhase.IDLE,
    val status: String = "Listo para narrar sin internet ni créditos.",
)

object NarrationPlaybackStateHolder {
    private val mutableState = MutableStateFlow(NarrationPlaybackState())
    val state: StateFlow<NarrationPlaybackState> = mutableState.asStateFlow()

    fun update(value: NarrationPlaybackState) { mutableState.value = value }

    fun restoreIfInactive(bookUri: String, offset: Int, totalChars: Int, status: String) {
        val current = mutableState.value
        if (current.bookUri == bookUri && current.phase != NarrationPlaybackPhase.IDLE) return
        mutableState.value = NarrationPlaybackState(
            bookUri = bookUri,
            offset = offset.coerceIn(0, totalChars.coerceAtLeast(0)),
            totalChars = totalChars.coerceAtLeast(0),
            phase = if (offset >= totalChars && totalChars > 0) {
                NarrationPlaybackPhase.COMPLETED
            } else {
                NarrationPlaybackPhase.IDLE
            },
            status = status,
        )
    }
}

data class NarrationChunk(val text: String, val start: Int, val end: Int)

fun nextNarrationChunk(text: String, start: Int, max: Int = NARRATION_CHUNK_SIZE): NarrationChunk {
    require(max > 0)
    val safeStart = start.coerceIn(0, text.length)
    if (safeStart >= text.length) return NarrationChunk("", text.length, text.length)
    val hardEnd = (safeStart + max).coerceAtMost(text.length)
    if (hardEnd == text.length) return NarrationChunk(text.substring(safeStart).trim(), safeStart, hardEnd)
    val window = text.substring(safeStart, hardEnd)
    val sentenceCut = listOf(
        window.lastIndexOf(". "), window.lastIndexOf("! "),
        window.lastIndexOf("? "), window.lastIndexOf("\n\n"),
    ).filter { it > max / 2 }.maxOrNull()?.plus(1)
    val cut = sentenceCut ?: window.lastIndexOf(' ').takeIf { it > max / 2 } ?: window.length
    return NarrationChunk(window.substring(0, cut).trim(), safeStart, safeStart + cut)
}

fun rewindNarrationOffset(offset: Int, chunkSize: Int = NARRATION_CHUNK_SIZE): Int {
    require(chunkSize > 0)
    return (offset.coerceAtLeast(0) - chunkSize).coerceAtLeast(0)
}

fun narrationProgressPercent(offset: Int, totalChars: Int): Int {
    if (totalChars <= 0) return 0
    return ((offset.coerceIn(0, totalChars).toLong() * 100L) / totalChars).toInt()
}

fun cleanNarratableText(text: String): String = text
    .replace(Regex("(?m)^\\s*\\d+\\s*$"), "")
    .replace(Regex("[ \\t]+"), " ")
    .replace(Regex("\\n{3,}"), "\n\n")
    .trim()
