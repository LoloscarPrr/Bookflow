package app.bookflow.reader.core.domain

enum class NarrationMood { CALM, MELANCHOLIC, MYSTERIOUS, TENSE, ACTION, HOPEFUL, NEUTRAL }
enum class NarrationPace { SLOW, MEDIUM, FAST }
enum class AmbienceClass { SILENCE, ROOM, NATURE, NIGHT, URBAN, TENSION, ACTION, MELANCHOLY }

data class NarrationPlan(
    val passage: String,
    val speakerId: String,
    val speakerLabel: String,
    val mood: NarrationMood,
    val emotionalIntensity: Float,
    val pace: NarrationPace,
    val pauseAfterSentencesMs: Int,
    val ambience: AmbienceClass,
    val musicIntensity: Float,
    val spoilerBoundary: String? = null,
)
