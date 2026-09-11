package app.bookflow.reader.core.domain

enum class OfflineNarratorModel { MIRO_HIGH, DANIELA_HIGH }

fun offlineNarratorModelFor(speakerId: String): OfflineNarratorModel =
    if (speakerId == "narrator_female") OfflineNarratorModel.DANIELA_HIGH
    else OfflineNarratorModel.MIRO_HIGH

