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
            NarrationMood.ACTION -> 0.78f
            NarrationMood.TENSE -> 0.65f
            NarrationMood.MYSTERIOUS -> 0.48f
            NarrationMood.MELANCHOLIC -> 0.42f
            NarrationMood.HOPEFUL -> 0.38f
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

        // Voice identity is independent from emotion. These markers are deliberately
        // conservative: when the prose explicitly identifies a first-person female
        // narrator we cast a female voice; otherwise we keep the neutral/default cast.
        val femaleMarkers = listOf(
            "estaba enamorada", "me sentía sola", "me sentía segura", "me sentía nerviosa",
            "estaba cansada", "estaba asustada", "estaba emocionada", "estaba confundida",
            "soy una mujer", "era una chica", "era una niña", "como mujer", "mi novio",
            "mi esposo", "mi marido"
        )
        val maleMarkers = listOf(
            "estaba enamorado", "me sentía solo", "me sentía seguro", "me sentía nervioso",
            "estaba cansado", "estaba asustado", "estaba emocionado", "estaba confundido",
            "soy un hombre", "era un chico", "era un niño", "como hombre", "mi novia",
            "mi esposa"
        )
        val femaleScore = femaleMarkers.count(text::contains)
        val maleScore = maleMarkers.count(text::contains)
        val speakerId = if (femaleScore > maleScore && femaleScore > 0) "narrator_female" else "narrator_male"
        val speakerLabel = if (speakerId == "narrator_female") "Narradora" else "Narrador"

        return NarrationPlan(
            passage = passage,
            speakerId = speakerId,
            speakerLabel = speakerLabel,
            mood = mood,
            emotionalIntensity = intensity,
            pace = pace,
            pauseAfterSentencesMs = if (pace == NarrationPace.SLOW) 600 else 350,
            ambience = ambience,
            musicIntensity = (intensity * 0.7f).coerceIn(0f, 1f),
            spoilerBoundary = context.spoilerBoundary,
        )
    }
}
