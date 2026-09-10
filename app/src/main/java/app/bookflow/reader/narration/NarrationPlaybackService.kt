package app.bookflow.reader.narration

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import app.bookflow.reader.core.data.BookSessionStore
import app.bookflow.reader.core.data.DocumentTextLoader
import app.bookflow.reader.core.data.OfflineNeuralVoiceRenderer
import app.bookflow.reader.core.data.RuleBasedSceneDirector
import app.bookflow.reader.core.diagnostics.CrashReporter
import app.bookflow.reader.core.diagnostics.DiagnosticArea
import app.bookflow.reader.core.domain.BookDocument
import java.io.File
import kotlinx.coroutines.*

@UnstableApi
class NarrationPlaybackService : MediaSessionService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val director = RuleBasedSceneDirector()
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var renderer: OfflineNeuralVoiceRenderer
    private lateinit var loader: DocumentTextLoader
    private lateinit var store: BookSessionStore
    private var generationJob: Job? = null
    private var generationToken = 0L
    private var currentBook: BookDocument? = null
    private var currentText: String? = null
    private var chunkStart = 0
    private var chunkEnd = 0
    private var autoContinue = false
    private var completedChunkHandled = false

    override fun onCreate() {
        super.onCreate()
        CrashReporter.initialize(applicationContext)
        renderer = OfflineNeuralVoiceRenderer(applicationContext)
        loader = DocumentTextLoader(applicationContext)
        store = BookSessionStore(applicationContext)
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
            addListener(playbackListener)
        }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> playBook(intent)
            ACTION_STOP -> stopNarration()
            ACTION_REWIND -> rewind(intent)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun playBook(intent: Intent) {
        val uri = intent.getStringExtra(EXTRA_URI)?.takeIf { it.isNotBlank() } ?: return
        val book = BookDocument(
            uri,
            intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Libro" },
            intent.getStringExtra(EXTRA_MIME).orEmpty(),
        )
        val requested = intent.getIntExtra(EXTRA_OFFSET, store.loadNarrationOffset(uri))
        if (currentBook?.uriString == uri && player.mediaItemCount > 0 && player.playbackState != Player.STATE_ENDED) {
            autoContinue = true
            player.play()
            return
        }

        generationJob?.cancel()
        generationToken += 1
        autoContinue = true
        completedChunkHandled = false
        player.stop()
        player.clearMediaItems()
        currentBook = book
        currentText = null
        chunkStart = requested.coerceAtLeast(0)
        chunkEnd = chunkStart
        store.saveLastBook(book)
        updateState(
            NarrationPlaybackPhase.PREPARING,
            chunkStart,
            0,
            if (chunkStart == 0) "Preparando voz neuronal local…" else "Recuperando tu narración…",
        )

        val loadToken = generationToken
        generationJob = serviceScope.launch {
            try {
                val text = cleanNarratableText(loader.load(book))
                check(text.isNotBlank())
                if (loadToken != generationToken || !autoContinue) return@launch
                currentText = text
                generationJob = null
                val offset = requested.coerceIn(0, text.length)
                if (offset >= text.length) {
                    store.saveNarrationOffset(uri, 0)
                    renderChunk(0)
                } else {
                    renderChunk(offset)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failNarration(error)
            }
        }
    }

    private fun renderChunk(start: Int) {
        val book = currentBook ?: return
        val text = currentText ?: return
        val chunk = nextNarrationChunk(text, start)
        if (chunk.text.isBlank()) {
            finishBook(text.length)
            return
        }

        generationJob?.cancel()
        val token = ++generationToken
        chunkStart = chunk.start
        chunkEnd = chunk.end
        completedChunkHandled = false
        updateState(
            NarrationPlaybackPhase.PREPARING,
            chunkStart,
            text.length,
            if (chunkStart == 0) "Preparando voz neuronal local…" else "Preparando el siguiente tramo…",
        )
        generationJob = serviceScope.launch {
            try {
                val plan = director.createPlan(chunk.text)
                val segment = renderer.render(plan, plan.speakerId)
                if (token != generationToken || !autoContinue) return@launch
                val item = MediaItem.Builder()
                    .setMediaId("bookflow-narration")
                    .setUri(Uri.fromFile(File(segment.localUri)))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(displayTitle(book.title))
                            .setArtist("BookFlow · ${plan.speakerLabel}")
                            .build(),
                    )
                    .build()
                player.setMediaItem(item)
                player.prepare()
                player.play()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failNarration(error)
            }
        }
    }

    private fun stopNarration() {
        cancelGeneration()
        persistOffset(chunkStart)
        player.stop()
        player.clearMediaItems()
        updateState(
            NarrationPlaybackPhase.PAUSED,
            chunkStart,
            currentText?.length ?: 0,
            "Narración detenida en ${progress(chunkStart)}%.",
        )
        stopSelf()
    }

    private fun rewind(intent: Intent) {
        val uri = intent.getStringExtra(EXTRA_URI) ?: currentBook?.uriString ?: return
        val active = currentBook?.uriString == uri
        val total = if (active) currentText?.length ?: 0 else NarrationPlaybackStateHolder.state.value.totalChars
        val offset = rewindNarrationOffset(if (active) chunkStart else store.loadNarrationOffset(uri))
        if (active) {
            cancelGeneration()
            player.stop()
            player.clearMediaItems()
        }
        chunkStart = offset
        chunkEnd = offset
        store.saveNarrationOffset(uri, offset)
        NarrationPlaybackStateHolder.update(
            NarrationPlaybackState(uri, offset, total, NarrationPlaybackPhase.PAUSED, "Retrocediste un tramo."),
        )
        if (active) stopSelf()
    }

    private fun completeChunk() {
        if (completedChunkHandled || chunkEnd <= chunkStart) return
        completedChunkHandled = true
        val text = currentText ?: return
        persistOffset(chunkEnd)
        if (autoContinue && chunkEnd < text.length) {
            player.stop()
            player.clearMediaItems()
            renderChunk(chunkEnd)
        } else if (chunkEnd >= text.length) {
            finishBook(text.length)
        }
    }

    private fun finishBook(total: Int) {
        autoContinue = false
        persistOffset(total)
        updateState(NarrationPlaybackPhase.COMPLETED, total, total, "Llegaste al final del libro.")
        player.clearMediaItems()
        stopSelf()
    }

    private fun cancelGeneration() {
        generationJob?.cancel()
        generationToken += 1
        autoContinue = false
    }

    private fun persistOffset(offset: Int) {
        currentBook?.let { store.saveNarrationOffset(it.uriString, offset) }
    }

    private fun failNarration(error: Exception) {
        autoContinue = false
        persistOffset(chunkStart)
        CrashReporter.recordNonFatal(DiagnosticArea.NARRATION, error)
        updateState(
            NarrationPlaybackPhase.ERROR,
            chunkStart,
            currentText?.length ?: 0,
            "No pude iniciar la voz local. Inténtalo otra vez.",
        )
        player.clearMediaItems()
        stopSelf()
    }

    private fun updateState(phase: NarrationPlaybackPhase, offset: Int, total: Int, status: String) {
        NarrationPlaybackStateHolder.update(
            NarrationPlaybackState(
                currentBook?.uriString,
                offset.coerceIn(0, total.coerceAtLeast(0)),
                total.coerceAtLeast(0),
                phase,
                status,
            ),
        )
    }

    private fun progress(offset: Int) = narrationProgressPercent(offset, currentText?.length ?: 0)

    private val playbackListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val text = currentText ?: return
            if (isPlaying) {
                updateState(
                    NarrationPlaybackPhase.PLAYING,
                    chunkStart,
                    text.length,
                    "Reproduciendo en segundo plano · sin créditos",
                )
            } else if (
                NarrationPlaybackStateHolder.state.value.phase == NarrationPlaybackPhase.PLAYING &&
                player.playbackState != Player.STATE_ENDED
            ) {
                persistOffset(chunkStart)
                updateState(
                    NarrationPlaybackPhase.PAUSED,
                    chunkStart,
                    text.length,
                    "Pausado en ${progress(chunkStart)}%.",
                )
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) completeChunk()
        }

        override fun onPlayerError(error: PlaybackException) {
            autoContinue = false
            persistOffset(chunkStart)
            CrashReporter.recordNonFatal(DiagnosticArea.PLAYBACK, error)
            updateState(
                NarrationPlaybackPhase.ERROR,
                chunkStart,
                currentText?.length ?: 0,
                "No pude reproducir este tramo. Inténtalo otra vez.",
            )
            player.clearMediaItems()
            stopSelf()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        persistOffset(chunkStart)
        generationJob?.cancel()
        serviceScope.cancel()
        mediaSession.release()
        player.removeListener(playbackListener)
        player.release()
        renderer.close()
        super.onDestroy()
    }

    private fun displayTitle(title: String) = title.substringBeforeLast('.')
        .replace('_', ' ').replace(Regex("\\s+"), " ").trim().ifBlank { "Libro" }

    companion object {
        private const val ACTION_PLAY = "app.bookflow.reader.action.PLAY_BOOK"
        private const val ACTION_STOP = "app.bookflow.reader.action.STOP_NARRATION"
        private const val ACTION_REWIND = "app.bookflow.reader.action.REWIND_NARRATION"
        private const val EXTRA_URI = "book_uri"
        private const val EXTRA_TITLE = "book_title"
        private const val EXTRA_MIME = "book_mime"
        private const val EXTRA_OFFSET = "book_offset"

        fun play(context: Context, book: BookDocument, offset: Int) {
            context.startService(
                Intent(context, NarrationPlaybackService::class.java)
                    .setAction(ACTION_PLAY)
                    .putExtra(EXTRA_URI, book.uriString)
                    .putExtra(EXTRA_TITLE, book.title)
                    .putExtra(EXTRA_MIME, book.mimeType)
                    .putExtra(EXTRA_OFFSET, offset.coerceAtLeast(0)),
            )
        }

        fun stop(context: Context) {
            context.startService(Intent(context, NarrationPlaybackService::class.java).setAction(ACTION_STOP))
        }

        fun rewind(context: Context, book: BookDocument) {
            context.startService(
                Intent(context, NarrationPlaybackService::class.java)
                    .setAction(ACTION_REWIND)
                    .putExtra(EXTRA_URI, book.uriString),
            )
        }
    }
}
