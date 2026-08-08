package com.mohamed.miqaat.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RawRes
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.mohamed.miqaat.domain.PrayerEventKind
import com.mohamed.miqaat.domain.model.PrayerName

/**
 * Joue l'adhan ou le rappel **nous-mêmes**, au lieu de le confier au son du canal
 * de notification. Deux raisons, toutes deux constatées sur appareil (voir D20) :
 *
 * 1. le lecteur de notifications du système ne demande jamais le focus audio —
 *    la musique en cours continuait donc par-dessus l'appel à la prière ;
 * 2. plusieurs surcouches (Android 10 en particulier) ignorent purement et
 *    simplement le son personnalisé d'un canal.
 *
 * Un service en avant-plan, et pas un `MediaPlayer` lancé depuis le receiver :
 * le processus d'un `BroadcastReceiver` peut être tué dès `onReceive` terminé,
 * ce qui couperait le son au bout de quelques secondes. La notification de
 * l'évènement sert de notification d'avant-plan, puis lui est rendue
 * (`STOP_FOREGROUND_DETACH`) : l'utilisateur n'en voit jamais qu'une.
 */
class PrayerSoundService : Service() {

    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayer = intent?.getStringExtra(EXTRA_PRAYER)
            ?.let { name -> PrayerName.entries.firstOrNull { it.name == name } }
        val kind = intent?.getStringExtra(EXTRA_KIND)
            ?.let { name -> PrayerEventKind.entries.firstOrNull { it.name == name } }

        if (prayer == null || kind == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Obligatoire dans les 5 s qui suivent startForegroundService, avant toute
        // autre chose : c'est ce qui autorise le service à vivre le temps du son.
        val id = PrayerNotifications.idOf(prayer, kind)
        val notification = PrayerNotifications.build(this, prayer, kind)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Le type n'est exigé — et `shortService` n'existe — qu'à partir d'Android 14.
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        } else {
            startForeground(id, notification)
        }

        stopPlayback()
        if (!startPlayback(PrayerNotifications.soundOf(kind))) {
            // Rien à jouer (mode silencieux ou vibreur, ou son illisible) :
            // la notification reste, elle vibre déjà via son canal.
            finish()
        }
        return START_NOT_STICKY
    }

    /** @return false si aucun son n'a démarré — l'appelant doit alors terminer le service. */
    private fun startPlayback(@RawRes sound: Int): Boolean {
        val audioManager = getSystemService(AudioManager::class.java)
        // On garde le comportement d'origine : le son suit le mode du téléphone.
        // La vibration, elle, reste gérée par le canal (donc en mode vibreur aussi).
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return false

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // GAIN_TRANSIENT (et non ..._MAY_DUCK) : les lecteurs en cours reçoivent
        // AUDIOFOCUS_LOSS_TRANSIENT, donc ils se mettent en pause au lieu de
        // baisser le volume — et reprennent d'eux-mêmes après l'adhan.
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS) finish()
            }
            .build()
        focusRequest = request
        // Un refus n'empêche pas de jouer : l'appel à la prière prime.
        audioManager.requestAudioFocus(request)

        return runCatching {
            player = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(
                    this@PrayerSoundService,
                    Uri.parse("android.resource://$packageName/$sound"),
                )
                setOnCompletionListener { finish() }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "Lecture impossible (what=$what, extra=$extra)")
                    finish()
                    true
                }
                // Ressource locale : la préparation est immédiate, pas besoin d'async.
                prepare()
                start()
            }
            true
        }.getOrElse { error ->
            Log.w(TAG, "Son injouable", error)
            false
        }
    }

    /** Rend la notification à l'utilisateur (elle survit au service) et s'arrête. */
    private fun finish() {
        stopPlayback()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun stopPlayback() {
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        focusRequest?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PrayerSoundService"
        private const val EXTRA_PRAYER = "prayer"
        private const val EXTRA_KIND = "kind"

        /**
         * Démarre la lecture. Ne lève jamais : si le système refuse le service
         * d'avant-plan, la notification a déjà été posée par le receiver et
         * l'utilisateur est prévenu — seul le son manque.
         */
        fun start(context: Context, prayer: PrayerName, kind: PrayerEventKind) {
            val intent = Intent(context, PrayerSoundService::class.java)
                .putExtra(EXTRA_PRAYER, prayer.name)
                .putExtra(EXTRA_KIND, kind.name)
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { Log.w(TAG, "Service d'avant-plan refusé", it) }
        }
    }
}
