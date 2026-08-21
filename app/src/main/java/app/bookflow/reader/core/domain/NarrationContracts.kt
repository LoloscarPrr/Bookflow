package app.bookflow.reader.core.domain

interface SceneDirector {
    suspend fun createPlan(passage: String, context: NarrationContext = NarrationContext()): NarrationPlan
}

data class NarrationContext(
    val bookId: String? = null,
    val chapterId: String? = null,
    val knownCharacters: Set<String> = emptySet(),
    val spoilerBoundary: String? = null,
)

data class VoiceAssignment(
    val characterId: String,
    val characterLabel: String,
    val voiceId: String,
)

data class VoiceCast(
    val narratorVoiceId: String,
    val assignments: List<VoiceAssignment> = emptyList(),
) {
    fun voiceFor(characterId: String): String =
        assignments.firstOrNull { it.characterId == characterId }?.voiceId ?: narratorVoiceId
}

data class RenderedVoiceSegment(
    val cacheKey: String,
    val localUri: String,
    val durationMs: Long,
)

interface VoiceRenderer {
    suspend fun render(plan: NarrationPlan, voiceId: String): RenderedVoiceSegment
}

interface NarrationCache {
    suspend fun find(cacheKey: String): RenderedVoiceSegment?
    suspend fun save(segment: RenderedVoiceSegment)
}
