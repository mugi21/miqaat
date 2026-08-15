package com.mohamed.miqaat.quran

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Ce que l'interface a besoin de savoir de la lecture en cours. */
data class QuranPlaybackUiState(
    val surahId: Int? = null,
    val surahName: String = "",
    val reciterName: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    /** Erreur de lecture — en pratique presque toujours le réseau. */
    val failed: Boolean = false,
) {
    val isActive: Boolean get() = surahId != null
}

/**
 * Le lien entre l'interface et [QuranPlaybackService].
 *
 * Un seul exemplaire pour toute l'app (porté par `MiqaatApp`), parce que le
 * mini-lecteur vit dans la barre du bas de `MainActivity` et doit survivre au
 * changement d'écran. L'interface ne touche jamais au service ni au lecteur :
 * elle lit ce `StateFlow` et appelle les quelques commandes ci-dessous.
 */
class QuranPlayerConnection(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var controller: MediaController? = null
    private var connecting = false
    private var ticker: Job? = null

    private val _state = MutableStateFlow(QuranPlaybackUiState())
    val state: StateFlow<QuranPlaybackUiState> = _state.asStateFlow()

    /**
     * La pochette à coller sur les éléments de la file. Exposée ici pour que le
     * ViewModel n'ait pas à connaître un `Context` : le rendu est calculé une
     * fois puis mémorisé.
     */
    val artwork: ByteArray? get() = QuranArtwork.pngBytes(context)

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(failed = true, isBuffering = false, isPlaying = false) }
        }
    }

    /**
     * Se lie au service. Idempotent : appelable à chaque composition de l'écran.
     * La liaison est asynchrone, l'interface reste utilisable en attendant.
     */
    fun connect() {
        if (controller != null || connecting) return
        connecting = true
        val token = SessionToken(context, ComponentName(context, QuranPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                connecting = false
                controller = runCatching { future.get() }.getOrNull()
                controller?.addListener(listener)
                publish()
                startTicker()
            },
            // Le contrôleur veut le thread principal, celui du lecteur.
            ContextCompat.getMainExecutor(context),
        )
    }

    /** Lance une file à partir de la sourate choisie. */
    fun play(items: List<MediaItem>, startIndex: Int, positionMs: Long = 0) {
        val controller = controller ?: return
        _state.update { it.copy(failed = false) }
        controller.setMediaItems(items, startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)), positionMs)
        controller.prepare()
        controller.play()
    }

    fun togglePlayPause() {
        val controller = controller ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun next() = controller?.takeIf { it.hasNextMediaItem() }?.seekToNextMediaItem()

    fun previous() = controller?.takeIf { it.hasPreviousMediaItem() }?.seekToPreviousMediaItem()

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0))
    }

    /** Décalage relatif, pour les boutons ±10 s. */
    fun seekBy(deltaMs: Long) {
        val controller = controller ?: return
        controller.seekTo((controller.currentPosition + deltaMs).coerceAtLeast(0))
    }

    fun stop() {
        controller?.run {
            pause()
            clearMediaItems()
        }
        _state.value = QuranPlaybackUiState()
    }

    /**
     * Le lecteur ne prévient pas de l'avancée de la tête de lecture : c'est à
     * nous de relire la position. Une fois par demi-seconde tant que ça joue —
     * assez pour une barre de progression fluide, et rien du tout à l'arrêt.
     */
    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                if (controller?.isPlaying == true) publish()
                delay(TICK_MS)
            }
        }
    }

    private fun publish() {
        val controller = controller ?: return
        val item = controller.currentMediaItem
        val surahId = QuranMediaItems.surahIdOf(item)
        _state.update { previous ->
            QuranPlaybackUiState(
                surahId = surahId,
                surahName = item?.mediaMetadata?.title?.toString().orEmpty(),
                reciterName = item?.mediaMetadata?.artist?.toString().orEmpty(),
                isPlaying = controller.isPlaying,
                isBuffering = controller.playbackState == Player.STATE_BUFFERING,
                positionMs = controller.currentPosition.coerceAtLeast(0),
                // Une durée inconnue vaut TIME_UNSET : la barre doit alors
                // rester indéterminée plutôt que d'afficher un négatif.
                durationMs = controller.duration.takeIf { it > 0 } ?: 0,
                hasPrevious = controller.hasPreviousMediaItem(),
                hasNext = controller.hasNextMediaItem(),
                failed = previous.failed && surahId == null,
            )
        }
    }

    private companion object {
        const val TICK_MS = 500L
    }
}
