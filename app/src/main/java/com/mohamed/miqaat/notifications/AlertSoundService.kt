package com.mohamed.miqaat.notifications

import android.app.Notification
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
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.mohamed.miqaat.domain.AlertStream
import com.mohamed.miqaat.domain.PrayerEventKind
import com.mohamed.miqaat.domain.model.Invocation
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.miqaatApp

/**
 * Joue l'alerte — adhan, rappel ou invocation — **nous-mêmes**, au lieu de la
 * confier au son du canal de notification. Deux raisons, toutes deux constatées
 * sur appareil (voir D20) :
 *
 * 1. le lecteur de notifications du système ne demande jamais le focus audio —
 *    la musique en cours continuait donc par-dessus l'appel à la prière ;
 * 2. plusieurs surcouches (Android 10 en particulier) ignorent purement et
 *    simplement le son personnalisé d'un canal.
 *
 * Depuis D36, il ne décide plus **si** l'on joue : [PrayerAlarmReceiver] a déjà
 * tranché à partir du mode d'alerte et de l'état du téléphone, et ne le démarre
 * que s'il y a un son à sortir. Le service reçoit le flux à emprunter.
 *
 * Un service en avant-plan, et pas un `MediaPlayer` lancé depuis le receiver :
 * le processus d'un `BroadcastReceiver` peut être tué dès `onReceive` terminé,
 * ce qui couperait le son au bout de quelques secondes. La notification de
 * l'évènement sert de notification d'avant-plan, puis lui est rendue
 * (`STOP_FOREGROUND_DETACH`) : l'utilisateur n'en voit jamais qu'une.
 */
/**
 * `USAGE_NOTIFICATION_RINGTONE` sort sur le flux de la **sonnerie d'appel** : le
 * volume que l'utilisateur règle pour ses appels, et qui se tait avec le
 * téléphone. `USAGE_ALARM` sort sur le flux des alarmes, que le mode sonnerie ne
 * coupe pas — c'est ce qui rend possible « toujours sonner ».
 */
private fun AlertStream.usage(): Int = when (this) {
    AlertStream.RINGTONE -> AudioAttributes.USAGE_NOTIFICATION_RINGTONE
    AlertStream.ALARM -> AudioAttributes.USAGE_ALARM
}

class AlertSoundService : Service() {

    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val target = intent?.let(::resolveTarget)
        if (target == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Obligatoire dans les 5 s qui suivent startForegroundService, avant toute
        // autre chose : c'est ce qui autorise le service à vivre le temps du son.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Le type n'est exigé — et `shortService` n'existe — qu'à partir d'Android 14.
            startForeground(
                target.notificationId,
                target.notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
            )
        } else {
            startForeground(target.notificationId, target.notification)
        }

        stopPlayback()
        if (!startPlayback(target)) {
            // Son illisible : la notification reste, elle a déjà été posée.
            finish()
        }
        return START_NOT_STICKY
    }

    /**
     * Android 14+ : un `shortService` non arrêté au bout de ~3 minutes lève une
     * `ForegroundServiceDidNotStopInTimeException` fatale. L'adhan dure 31 s, la
     * marge est large — mais un `MediaPlayer` bloqué ne doit pas faire tomber l'app.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onTimeout(startId: Int) {
        Log.w(TAG, "Délai du service court dépassé, arrêt")
        finish()
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onTimeout(startId: Int, fgsType: Int) {
        onTimeout(startId)
    }

    /** @return false si aucun son n'a démarré — l'appelant doit alors terminer le service. */
    private fun startPlayback(target: Target): Boolean {
        val audioManager = getSystemService(AudioManager::class.java)

        val attributes = AudioAttributes.Builder()
            .setUsage(target.stream.usage())
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // GAIN_TRANSIENT (et non ..._MAY_DUCK) : les lecteurs en cours reçoivent
        // AUDIOFOCUS_LOSS_TRANSIENT, donc ils se mettent en pause au lieu de
        // baisser le volume — et reprennent d'eux-mêmes après l'adhan. Une
        // invocation, elle, ne dure que quelques secondes : la mettre en sourdine
        // suffit, l'interrompre serait disproportionné.
        val request = AudioFocusRequest.Builder(target.focusGain)
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
                setDataSource(this@AlertSoundService, target.sound)
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

    /** Ce qu'il faut pour rester en avant-plan et jouer : notification identique à celle du receiver, son, flux. */
    private class Target(
        val notificationId: Int,
        val notification: Notification,
        val sound: Uri,
        val stream: AlertStream,
        val focusGain: Int,
    )

    private fun resolveTarget(intent: Intent): Target? {
        val stream = intent.getStringExtra(EXTRA_STREAM)
            ?.let { name -> AlertStream.entries.firstOrNull { it.name == name } }
            ?: return null

        val invocationId = intent.getLongExtra(EXTRA_INVOCATION, NO_INVOCATION)
        if (invocationId != NO_INVOCATION) {
            val invocation = miqaatApp.invocationRepository.current()
                .firstOrNull { it.id == invocationId }
                ?: return null
            return Target(
                notificationId = InvocationNotifications.idOf(invocation),
                notification = InvocationNotifications.build(this, invocation),
                // Aucun enregistrement livré pour les adhkār : on garde le son de
                // notification du téléphone, celui que l'utilisateur connaît.
                sound = Settings.System.DEFAULT_NOTIFICATION_URI,
                stream = stream,
                focusGain = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
            )
        }

        val prayer = intent.getStringExtra(EXTRA_PRAYER)
            ?.let { name -> PrayerName.entries.firstOrNull { it.name == name } }
            ?: return null
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { name -> PrayerEventKind.entries.firstOrNull { it.name == name } }
            ?: return null

        return Target(
            notificationId = PrayerNotifications.idOf(prayer, kind),
            notification = PrayerNotifications.build(this, prayer, kind),
            sound = rawUri(PrayerNotifications.soundOf(kind)),
            stream = stream,
            focusGain = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
        )
    }

    private fun rawUri(resId: Int): Uri = "android.resource://$packageName/$resId".toUri()

    companion object {
        private const val TAG = "AlertSoundService"
        private const val EXTRA_PRAYER = "prayer"
        private const val EXTRA_KIND = "kind"
        private const val EXTRA_INVOCATION = "invocation"
        private const val EXTRA_STREAM = "stream"
        private const val NO_INVOCATION = -1L

        /**
         * Démarre la lecture. Ne lève jamais : si le système refuse le service
         * d'avant-plan, la notification a déjà été posée par le receiver et
         * l'utilisateur est prévenu — seul le son manque.
         */
        fun start(context: Context, prayer: PrayerName, kind: PrayerEventKind, stream: AlertStream) {
            context.launch(
                Intent(context, AlertSoundService::class.java)
                    .putExtra(EXTRA_PRAYER, prayer.name)
                    .putExtra(EXTRA_KIND, kind.name)
                    .putExtra(EXTRA_STREAM, stream.name),
            )
        }

        fun start(context: Context, invocation: Invocation, stream: AlertStream) {
            context.launch(
                Intent(context, AlertSoundService::class.java)
                    .putExtra(EXTRA_INVOCATION, invocation.id)
                    .putExtra(EXTRA_STREAM, stream.name),
            )
        }

        private fun Context.launch(intent: Intent) {
            runCatching { ContextCompat.startForegroundService(this, intent) }
                .onFailure { Log.w(TAG, "Service d'avant-plan refusé", it) }
        }
    }
}
