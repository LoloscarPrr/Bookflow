package app.bookflow.reader.narration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrationPlaybackStateTest {
    @Test fun chunkPrefersSentenceBoundary() {
        val chunk = nextNarrationChunk("a".repeat(60) + ". " + "b".repeat(80), 0, 100)
        assertEquals(61, chunk.end)
        assertTrue(chunk.text.endsWith("."))
    }

    @Test fun finalChunkEndsAtTextLength() {
        val text = "Un tramo final corto."
        val chunk = nextNarrationChunk(text, 3, 100)
        assertEquals(3, chunk.start)
        assertEquals(text.length, chunk.end)
        assertEquals(text.substring(3), chunk.text)
    }

    @Test fun rewindClampsAtBeginning() {
        assertEquals(0, rewindNarrationOffset(500))
        assertEquals(1_150, rewindNarrationOffset(2_300))
    }

    @Test fun progressIsClamped() {
        assertEquals(0, narrationProgressPercent(20, 0))
        assertEquals(25, narrationProgressPercent(250, 1_000))
        assertEquals(100, narrationProgressPercent(2_000, 1_000))
    }
}
