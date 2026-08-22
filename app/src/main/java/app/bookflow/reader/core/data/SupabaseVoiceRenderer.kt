package app.bookflow.reader.core.data

import android.content.Context
import app.bookflow.reader.core.domain.NarrationPlan
import app.bookflow.reader.core.domain.RenderedVoiceSegment
import app.bookflow.reader.core.domain.VoiceRenderer
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseVoiceRenderer(private val context: Context) : VoiceRenderer {
    override suspend fun render(plan: NarrationPlan, voiceId: String): RenderedVoiceSegment = withContext(Dispatchers.IO) {
        val castVoiceId = when (plan.speakerId) {
            "narrator_female" -> FEMALE_VOICE_ID
            "narrator_male" -> MALE_VOICE_ID
            else -> voiceId.ifBlank { MALE_VOICE_ID }
        }
        val connection = (URL(FUNCTION_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20_000; readTimeout = 90_000; doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
        }
        try {
            val payload = JSONObject().apply {
                put("text", plan.passage.take(MAX_CHARS)); put("voiceId", castVoiceId)
                put("modelId", "eleven_v3"); put("mood", plan.mood.name); put("pace", plan.pace.name)
                put("emotionalIntensity", plan.emotionalIntensity.toDouble()); put("pauseAfterSentencesMs", plan.pauseAfterSentencesMs)
            }
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status !in 200..299) {
                val details = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $status"
                error("Narration backend error: $details")
            }
            val file = File(context.cacheDir, "bookflow_narration_${System.currentTimeMillis()}.mp3")
            connection.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            RenderedVoiceSegment("${plan.speakerId}:${plan.mood}:${plan.pace}:${plan.passage.hashCode()}:$castVoiceId", file.absolutePath, 0L)
        } finally { connection.disconnect() }
    }

    private companion object {
        const val FUNCTION_URL = "https://fkgccemweaqkdrjozgml.supabase.co/functions/v1/bookflow-narration"
        const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZrZ2NjZW13ZWFxa2Ryam96Z21sIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY5MDE1NjIsImV4cCI6MjEwMjQ3NzU2Mn0.2jZaYypzjugDFupl7vKPrfNpb8CVzH_DHOxf1mWNWwI"
        const val MALE_VOICE_ID = "pNInz6obpgDQGcFmaJgB"
        const val FEMALE_VOICE_ID = "21m00Tcm4TlvDq8ikWAM"
        const val MAX_CHARS = 1200
    }
}
