package app.bookflow.reader.core.data

import app.bookflow.reader.core.domain.AmbienceClass
import app.bookflow.reader.core.domain.NarrationContext
import app.bookflow.reader.core.domain.NarrationMood
import app.bookflow.reader.core.domain.NarrationPace
import app.bookflow.reader.core.domain.NarrationPlan
import app.bookflow.reader.core.domain.SceneDirector

class RuleBasedSceneDirector : SceneDirector {
    override suspend fun createPlan(passage: String, context: NarrationContext): NarrationPlan {
        val text = passage.lowercase()
        val mood = when {
            listOf("miedo", "terror", "sangre", "huir", "peligro", "amenaza").any(text::contains) -> NarrationMood.TENSE
            listOf("triste", "soledad", "perdí", "extraño", "recuerdo", "melancol").any(text::contains) -> NarrationMood.MELANCHOLIC
            listOf("oscur", "silencio", "misterio", "sombra", "desconocido").any(text::contains) -> NarrationMood.MYSTERIOUS
            listOf("correr", "golpe", "ataque", "disparo", "explos").any(text::contains) -> NarrationMood.ACTION
            listOf("esperanza", "sonre", "alivio", "amanecer").any(text::contains) -> NarrationMood.HOPEFUL
            else -> NarrationMood.CALM
        }
        val intensity = when (mood) {
            NarrationMood.ACTION -> 0.9f
            NarrationMood.TENSE -> 0.75f
            NarrationMood.MYSTERIOUS -> 0.55f
            NarrationMood.MELANCHOLIC -> 0.45f
            NarrationMood.HOPEFUL -> 0.4f
            NarrationMood.CALM -> 0.25f
            NarrationMood.NEUTRAL -> 0.2f
        }
        val pace = when (mood) {
            NarrationMood.ACTION -> NarrationPace.FAST
            NarrationMood.MELANCHOLIC, NarrationMood.MYSTERIOUS -> NarrationPace.SLOW
            else -> NarrationPace.MEDIUM
        }
        val ambience = when (mood) {
            NarrationMood.ACTION -> AmbienceClass.ACTION
            NarrationMood.TENSE -> AmbienceClass.TENSION
            NarrationMood.MELANCHOLIC -> AmbienceClass.MELANCHOLY
            NarrationMood.MYSTERIOUS -> AmbienceClass.NIGHT
            else -> AmbienceClass.ROOM
        }
        return NarrationPlan(
            passage = passage,
            speakerId = "narrator",
            speakerLabel = "Narrador",
            mood = mood,
            emotionalIntensity = intensity,
            pace = pace,
            pauseAfterSentencesMs = if (pace == NarrationPace.SLOW) 650 else 350,
            ambience = ambience,
            musicIntensity = (intensity * 0.7f).coerceIn(0f, 1f),
            spoilerBoundary = context.spoilerBoundary,
        )
    }
}
