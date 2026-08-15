package com.mohamed.miqaat.quran

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mohamed.miqaat.MainActivity
import com.mohamed.miqaat.R
import com.mohamed.miqaat.data.settings.AppLocale
import com.mohamed.miqaat.miqaatApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * La lecture du Coran, hors de l'activité : elle continue quand l'app passe en
 * arrière-plan et se pilote depuis la notification et l'écran verrouillé.
 *
 * Media3 plutôt qu'un `MediaPlayer` maison (D42) : le buffering d'un flux HTTP
 * instable, le focus audio, la notification média et la reprise après coupure
 * réseau sont précisément ce qu'on ne réécrit pas correctement à la main.
 */
class QuranPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    /**
     * Comme toute surface hors activité (widget, notifications), le service doit
     * être habillé de la langue choisie dans l'app — sinon sa notification suit
     * celle du téléphone.
     */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        // Sans cela, la barre d'état porte la note de musique générique de
        // Media3 : rien ne dit que la récitation vient de Miqaat.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply {
                setSmallIcon(R.drawable.ic_quran_notification)
            },
        )

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                // La moitié gratuite de D43 : ExoPlayer se met en pause quand
                // l'adhan prend le focus audio (AUDIOFOCUS_GAIN_TRANSIENT, D20),
                // et reprend seul quand le focus lui revient.
                /* handleAudioFocus = */ true,
            )
            // Casque débranché = arrêt, comme n'importe quel lecteur.
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // À la pause : c'est le moment où la position vaut la peine
                // d'être retenue. En lecture, le ticker de l'écran s'en charge.
                if (!isPlaying) savePosition()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                savePosition()
            }
        })

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent())
            .build()
        instance = this
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * L'app est balayée du sélecteur d'applications récentes. Si rien ne joue,
     * le service n'a plus de raison d'être — sans cela une notification fantôme
     * survivrait à la fermeture.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.isPlaying) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        savePosition()
        instance = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        scope.cancel()
        super.onDestroy()
    }

    /** Appui sur la notification → l'app, sur l'écran du Coran. */
    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        REQUEST_OPEN,
        Intent(this, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_QURAN, true),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun savePosition() {
        val player = mediaSession?.player ?: return
        val surahId = QuranMediaItems.surahIdOf(player.currentMediaItem) ?: return
        val position = player.currentPosition
        scope.launch { miqaatApp.quranPreferences.setPosition(surahId, position) }
    }

    private fun pauseNow() {
        mediaSession?.player?.takeIf { it.isPlaying }?.pause()
    }

    companion object {
        private const val REQUEST_OPEN = 2001

        /**
         * Le service tourne dans le processus de l'application, tout comme le
         * receiver d'alarme : une référence directe suffit, et évite d'ouvrir un
         * `MediaController` asynchrone depuis un `onReceive` qui doit rendre la
         * main tout de suite.
         */
        @Volatile
        private var instance: QuranPlaybackService? = null

        /**
         * Le point d'entrée de D43, côté muet. En mode vibreur ou silencieux,
         * aucun service sonore n'est démarré (D38), donc personne ne prend le
         * focus audio : sans cet appel, la récitation continuerait par-dessus
         * l'heure de la prière. Quand l'alerte a du son, ne **pas** appeler ceci
         * — le focus audio fait déjà le travail, et il fait mieux : il rend la
         * lecture après l'adhan.
         *
         * Sans effet si rien ne joue, ce qui est le cas général.
         */
        fun pauseForPrayer() {
            // onReceive tourne sur le thread principal, celui du lecteur : appel direct.
            instance?.pauseNow()
        }
    }
}
