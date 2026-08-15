package com.mohamed.miqaat.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.mohamed.miqaat.data.reliability.ReliabilityLog
import com.mohamed.miqaat.domain.AlarmFreshness
import com.mohamed.miqaat.domain.AlertDecision
import com.mohamed.miqaat.domain.AlertResolver
import com.mohamed.miqaat.domain.PrayerEventKind
import com.mohamed.miqaat.domain.model.Invocation
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.miqaatApp
import com.mohamed.miqaat.quran.QuranPlaybackService
import java.time.Duration
import java.time.Instant

/**
 * Se déclenche à l'heure exacte d'un évènement de la chaîne — rappel avant
 * l'adhan, adhan, ou invocation — affiche la notification correspondante, joue
 * l'alerte qui convient, puis programme l'évènement suivant (chaîne).
 *
 * ⚠ **Cette classe ne se renomme pas**, malgré un périmètre devenu plus large
 * que les seules prières : l'alarme posée par la version déjà installée pointe
 * sur ce nom de classe et ne survivrait pas à un changement.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                val invocationId = intent.getLongExtra(EXTRA_INVOCATION, NO_INVOCATION)
                if (invocationId != NO_INVOCATION) {
                    context.notifyInvocation(invocationId, intent.scheduledAt())
                } else {
                    context.notifyPrayer(intent)
                }
            }
            // Trace de délivrance : c'est elle qui permet à l'écran de fiabilité
            // de distinguer « la surcouche nous gèle » de « tout va bien ».
            ReliabilityLog.recordFired(context)
        } finally {
            // Toujours replanifier, quoi qu'il soit arrivé au-dessus — sans
            // permission de notification, sur un évènement périmé, ou après une
            // exception : la chaîne ne doit jamais se rompre.
            PrayerAlarmScheduler(context).scheduleNext()
        }
    }

    private fun Context.notifyPrayer(intent: Intent) {
        val prayer = intent.getStringExtra(EXTRA_PRAYER)
            ?.let { name -> PrayerName.entries.firstOrNull { it.name == name } }
            ?: return
        // Absent = intent d'une version antérieure, qui n'annonçait que des adhans.
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { name -> PrayerEventKind.entries.firstOrNull { it.name == name } }
            ?: PrayerEventKind.ADHAN

        if (!isFresh(intent.scheduledAt(), AlarmFreshness.toleranceOf(kind))) {
            Log.w(TAG, "Évènement périmé ignoré : $prayer / $kind")
            return
        }

        val decision = alertDecision()
        // Poser la notification avant tout le reste : si le système refuse le
        // service d'avant-plan, l'utilisateur est prévenu quand même.
        NotificationManagerCompat.from(this).notify(
            PrayerNotifications.idOf(prayer, kind),
            PrayerNotifications.build(this, prayer, kind),
        )
        yieldQuranPlayer(decision)
        // La vibration part d'ici et non du service : l'effet est confié au
        // service système, il survit donc à la mort de notre processus.
        AlertVibrator.vibrate(this, decision.vibration, long = kind == PrayerEventKind.ADHAN)
        // Le son, lui, ne peut pas être joué ici : le processus d'un receiver
        // peut être tué dès `onReceive` terminé (voir AlertSoundService).
        decision.stream?.let { AlertSoundService.start(this, prayer, kind, it) }
    }

    private fun Context.notifyInvocation(id: Long, scheduledAt: Instant?) {
        // Supprimée ou désactivée entre la pose de l'alarme et son déclenchement :
        // rien à annoncer.
        val invocation: Invocation = miqaatApp.invocationRepository.current()
            .firstOrNull { it.id == id && it.enabled }
            ?: return

        if (!isFresh(scheduledAt, AlarmFreshness.INVOCATION)) {
            Log.w(TAG, "Invocation périmée ignorée : $id")
            return
        }

        val decision = alertDecision()
        NotificationManagerCompat.from(this).notify(
            InvocationNotifications.idOf(invocation),
            InvocationNotifications.build(this, invocation),
        )
        AlertVibrator.vibrate(this, decision.vibration, long = false)
        decision.stream?.let { AlertSoundService.start(this, invocation, it) }
    }

    /**
     * D43 — la récitation en cours cède la place à l'appel à la prière.
     *
     * Uniquement quand l'alerte est **muette** : dès qu'il y a du son,
     * `AlertSoundService` demande `AUDIOFOCUS_GAIN_TRANSIENT` (D20) et ExoPlayer
     * se met en pause tout seul — puis **reprend** à la fin de l'adhan, ce qu'un
     * appel explicite ne saurait pas faire. En mode vibreur ou silencieux, en
     * revanche, plus aucun service sonore n'est démarré depuis D38 : personne ne
     * prend le focus, et sans cette ligne la récitation continuerait par-dessus
     * l'heure de la prière.
     */
    private fun yieldQuranPlayer(decision: AlertDecision) {
        if (decision.stream == null) QuranPlaybackService.pauseForPrayer()
    }

    /** Le réglage de l'utilisateur croisé avec l'état sonore du téléphone (D36). */
    private fun Context.alertDecision(): AlertDecision = AlertResolver.resolve(
        miqaatApp.settingsRepository.currentNotification().mode,
        RingerReader.read(this),
    )

    private fun isFresh(scheduledAt: Instant?, tolerance: Duration): Boolean =
        AlarmFreshness.isFresh(scheduledAt, Instant.now(), tolerance)

    /** `null` = alarme posée par une version antérieure, qui ne transmettait pas l'heure prévue. */
    private fun Intent.scheduledAt(): Instant? =
        getLongExtra(EXTRA_TRIGGER_AT, NO_TRIGGER_AT)
            .takeIf { it != NO_TRIGGER_AT }
            ?.let(Instant::ofEpochMilli)

    companion object {
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_KIND = "kind"
        const val EXTRA_INVOCATION = "invocation"

        /** L'heure à laquelle l'alarme devait se déclencher, pour la garde de fraîcheur (D31). */
        const val EXTRA_TRIGGER_AT = "trigger_at"

        private const val TAG = "PrayerAlarmReceiver"
        private const val NO_INVOCATION = -1L
        private const val NO_TRIGGER_AT = 0L
    }
}
