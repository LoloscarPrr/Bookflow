package app.bookflow.reader.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveLayoutTest {
    @Test
    fun `359 dp is compact`() {
        assertEquals(BookFlowLayoutClass.COMPACT, classifyBookFlowLayout(359))
    }

    @Test
    fun `360 dp is regular`() {
        assertEquals(BookFlowLayoutClass.REGULAR, classifyBookFlowLayout(360))
    }

    @Test
    fun `839 dp is regular`() {
        assertEquals(BookFlowLayoutClass.REGULAR, classifyBookFlowLayout(839))
    }

    @Test
    fun `840 dp is wide`() {
        assertEquals(BookFlowLayoutClass.WIDE, classifyBookFlowLayout(840))
    }
}
