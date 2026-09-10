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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@UnstableApi
class NarrationPlaybackService : MediaSessionService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val director = RuleBasedSceneDirector()
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var renderer: OfflineNeuralVoiceRenderer
    private lateinit var loader: DocumentTextLoader
    private lateinit var store: BookSessionStore
    private var loadJob: Job? = null
    private var queueJob: Job? = null
    private var progressJob: Job? = null
    private var generationToken = 0L
    private var currentBook: BookDocument? = null
    private var currentText: String? = null
    private var activeChunk: NarrationChunk? = null
    private var nextChunkOffset = 0
    private var autoContinue = false
    private val chunksByMediaId = mutableMapOf<String, NarrationChunk>()

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
        progressJob = serviceScope.launch {
            while (isActive) {
                delay(PROGRESS_UPDATE_MS)
                if (player.isPlaying) publishLiveProgress()
            }
        }
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
        if (currentBook?.uriString == uri && player.mediaItemCount > 0) {
            autoContinue = true
            player.play()
            ensureQueue()
            return
        }

        cancelPendingWork()
        generationToken += 1
        autoContinue = true
        player.stop()
        player.clearMediaItems()
        chunksByMediaId.clear()
        activeChunk = null
        currentBook = book
        currentText = null
        nextChunkOffset = requested.coerceAtLeast(0)
        store.saveLastBook(book)
        updateState(
            NarrationPlaybackPhase.PREPARING,
            nextChunkOffset,
            0,
            if (nextChunkOffset == 0) "Preparando voz neuronal local…" else "Recuperando tu narración…",
        )

        val token = generationToken
        loadJob = serviceScope.launch {
            try {
                val text = cleanNarratableText(loader.load(book))
                check(text.isNotBlank())
                if (token != generationToken || !autoContinue) return@launch
                currentText = text
                nextChunkOffset = requested.coerceIn(0, text.length).let {
                    if (it >= text.length) 0 else it
                }
                if (requested >= text.length) store.saveNarrationOffset(uri, 0)
                loadJob = null
                ensureQueue()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failNarration(error)
            }
        }
    }

    private fun ensureQueue() {
        val text = currentText ?: return
        if (!autoContinue || queueJob?.isActive == true || nextChunkOffset >= text.length) return
        val token = generationToken
        queueJob = serviceScope.launch {
            try {
                while (
                    autoContinue && token == generationToken && nextChunkOffset < text.length &&
                    preparedAheadCount() < PREFETCH_AHEAD_COUNT
                ) {
                    val chunk = nextNarrationChunk(text, nextChunkOffset)
                    if (chunk.text.isBlank()) break
                    if (player.mediaItemCount == 0) {
                        updateState(
                            NarrationPlaybackPhase.PREPARING,
                            chunk.start,
                            text.length,
                            "Preparando voz neuronal local…",
                        )
                    }
                    val plan = director.createPlan(chunk.text)
                    val segment = renderer.render(plan, plan.speakerId)
                    if (!autoContinue || token != generationToken) return@launch

                    val mediaId = "narration-${token}-${chunk.start}-${chunk.end}"
                    chunksByMediaId[mediaId] = chunk
                    nextChunkOffset = chunk.end
                    addPreparedItem(
                        MediaItem.Builder()
                            .setMediaId(mediaId)
                            .setUri(Uri.fromFile(File(segment.localUri)))
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(displayTitle(currentBook?.title.orEmpty()))
                                    .setArtist("BookFlow · ${plan.speakerLabel}")
                                    .build(),
                            )
                            .build(),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
                    failNarration(error)
                } else {
                    CrashReporter.recordNonFatal(DiagnosticArea.NARRATION, error)
                    updateState(
                        NarrationPlaybackPhase.PLAYING,
                        currentPlaybackOffset(),
                        text.length,
                        "Reproduciendo · reintentaremos preparar el próximo tramo",
                    )
                }
            } finally {
                queueJob = null
            }
        }
    }

    private fun addPreparedItem(item: MediaItem) {
        val wasEmpty = player.mediaItemCount == 0
        val hadEnded = player.playbackState == Player.STATE_ENDED
        player.addMediaItem(item)
        when {
            wasEmpty -> {
                player.prepare()
                if (autoContinue) player.play()
            }
            hadEnded -> {
                if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                player.prepare()
                if (autoContinue) player.play()
            }
        }
    }

    private fun preparedAheadCount(): Int {
        if (player.mediaItemCount == 0) return 0
        val current = player.currentMediaItemIndex.coerceAtLeast(0)
        return (player.mediaItemCount - current - 1).coerceAtLeast(0)
    }

    private fun stopNarration() {
        val offset = currentPlaybackOffset()
        cancelPendingWork()
        persistOffset(offset)
        player.stop()
        player.clearMediaItems()
        chunksByMediaId.clear()
        activeChunk = null
        updateState(
            NarrationPlaybackPhase.PAUSED,
            offset,
            currentText?.length ?: 0,
            "Narración detenida en ${progress(offset)}%.",
        )
        stopSelf()
    }

    private fun rewind(intent: Intent) {
        val uri = intent.getStringExtra(EXTRA_URI) ?: currentBook?.uriString ?: return
        val active = currentBook?.uriString == uri
        val total = if (active) currentText?.length ?: 0 else NarrationPlaybackStateHolder.state.value.totalChars
        val currentOffset = if (active) currentPlaybackOffset() else store.loadNarrationOffset(uri)
        val offset = rewindNarrationOffset(currentOffset)
        if (active) {
            cancelPendingWork()
            player.stop()
            player.clearMediaItems()
            chunksByMediaId.clear()
            activeChunk = null
        }
        nextChunkOffset = offset
        store.saveNarrationOffset(uri, offset)
        NarrationPlaybackStateHolder.update(
            NarrationPlaybackState(uri, offset, total, NarrationPlaybackPhase.PAUSED, "Retrocediste un tramo."),
        )
        if (active) stopSelf()
    }

    private fun currentPlaybackOffset(): Int {
        val chunk = activeChunk ?: return NarrationPlaybackStateHolder.state.value.offset
        val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        return narrationOffsetForPlayback(chunk.start, chunk.end, player.currentPosition, duration)
    }

    private fun publishLiveProgress() {
        val text = currentText ?: return
        updateState(
            NarrationPlaybackPhase.PLAYING,
            currentPlaybackOffset(),
            text.length,
            if (preparedAheadCount() > 0) {
                "Reproduciendo sin pausas · audio preparado por adelantado"
            } else {
                "Reproduciendo · preparando el próximo tramo"
            },
        )
    }

    private fun finishBook(total: Int) {
        autoContinue = false
        persistOffset(total)
        updateState(NarrationPlaybackPhase.COMPLETED, total, total, "Llegaste al final del libro.")
        player.clearMediaItems()
        chunksByMediaId.clear()
        activeChunk = null
        stopSelf()
    }

    private fun cancelPendingWork() {
        loadJob?.cancel()
        queueJob?.cancel()
        loadJob = null
        queueJob = null
        generationToken += 1
        autoContinue = false
    }

    private fun persistOffset(offset: Int) {
        currentBook?.let { store.saveNarrationOffset(it.uriString, offset) }
    }

    private fun failNarration(error: Exception) {
        autoContinue = false
        val offset = currentPlaybackOffset()
        persistOffset(offset)
        CrashReporter.recordNonFatal(DiagnosticArea.NARRATION, error)
        updateState(
            NarrationPlaybackPhase.ERROR,
            offset,
            currentText?.length ?: 0,
            "No pude iniciar la voz local. Inténtalo otra vez.",
        )
        player.clearMediaItems()
        chunksByMediaId.clear()
        activeChunk = null
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
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val previous = activeChunk
            val next = mediaItem?.mediaId?.let(chunksByMediaId::get) ?: return
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && previous != null) {
                persistOffset(previous.end)
            }
            activeChunk = next
            if (player.isPlaying) publishLiveProgress()
            ensureQueue()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val text = currentText ?: return
            if (isPlaying) {
                publishLiveProgress()
                ensureQueue()
            } else if (
                NarrationPlaybackStateHolder.state.value.phase == NarrationPlaybackPhase.PLAYING &&
                player.playbackState != Player.STATE_ENDED
            ) {
                val offset = currentPlaybackOffset()
                persistOffset(offset)
                updateState(
                    NarrationPlaybackPhase.PAUSED,
                    offset,
                    text.length,
                    "Pausado en ${progress(offset)}%.",
                )
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return
            val text = currentText ?: return
            val completed = activeChunk?.end ?: currentPlaybackOffset()
            persistOffset(completed)
            if (nextChunkOffset >= text.length && queueJob?.isActive != true) {
                finishBook(text.length)
            } else if (autoContinue) {
                updateState(
                    NarrationPlaybackPhase.PREPARING,
                    completed,
                    text.length,
                    "Terminando de preparar el próximo tramo…",
                )
                ensureQueue()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            autoContinue = false
            val offset = currentPlaybackOffset()
            persistOffset(offset)
            CrashReporter.recordNonFatal(DiagnosticArea.PLAYBACK, error)
            updateState(
                NarrationPlaybackPhase.ERROR,
                offset,
                currentText?.length ?: 0,
                "No pude reproducir este tramo. Inténtalo otra vez.",
            )
            player.clearMediaItems()
            chunksByMediaId.clear()
            activeChunk = null
            stopSelf()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        persistOffset(currentPlaybackOffset())
        loadJob?.cancel()
        queueJob?.cancel()
        progressJob?.cancel()
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
        private const val PREFETCH_AHEAD_COUNT = 3
        private const val PROGRESS_UPDATE_MS = 750L
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
