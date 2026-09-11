package app.bookflow.reader.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineNarratorModelTest {
    @Test fun maleNarratorUsesMiroHigh() {
        assertEquals(OfflineNarratorModel.MIRO_HIGH, offlineNarratorModelFor("narrator_male"))
    }

    @Test fun femaleNarratorUsesDanielaHigh() {
        assertEquals(OfflineNarratorModel.DANIELA_HIGH, offlineNarratorModelFor("narrator_female"))
    }

    @Test fun unknownSpeakerFallsBackToMiroHigh() {
        assertEquals(OfflineNarratorModel.MIRO_HIGH, offlineNarratorModelFor("unknown"))
    }
}

