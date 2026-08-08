package com.mohamed.miqaat.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.mohamed.miqaat.domain.PrayerEventKind
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.miqaatApp

/**
 * Se déclenche à l'heure exacte d'un évènement de la chaîne — rappel avant
 * l'adhan, adhan, ou invocation — affiche la notification correspondante, lance
 * son son s'il y a lieu, puis programme l'évènement suivant (chaîne).
 *
 * ⚠ **Cette classe ne se renomme pas**, malgré un périmètre devenu plus large
 * que les seules prières : l'alarme posée par la version déjà installée pointe
 * sur ce nom de classe et ne survivrait pas à un changement.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            val invocationId = intent.getLongExtra(EXTRA_INVOCATION, NO_INVOCATION)
            if (invocationId != NO_INVOCATION) {
                context.notifyInvocation(invocationId)
            } else {
                context.notifyPrayer(intent)
            }
        }

        // Toujours replanifier, même sans permission de notification :
        // la chaîne ne doit jamais se rompre.
        PrayerAlarmScheduler(context).scheduleNext()
    }

    private fun Context.notifyPrayer(intent: Intent) {
        val prayer = intent.getStringExtra(EXTRA_PRAYER)
            ?.let { name -> PrayerName.entries.firstOrNull { it.name == name } }
            ?: return
        // Absent = intent d'une version antérieure, qui n'annonçait que des adhans.
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { name -> PrayerEventKind.entries.firstOrNull { it.name == name } }
            ?: PrayerEventKind.ADHAN

        // Poser la notification ici, avant le service : si le système refuse
        // le service d'avant-plan, l'utilisateur est prévenu quand même.
        NotificationManagerCompat.from(this).notify(
            PrayerNotifications.idOf(prayer, kind),
            PrayerNotifications.build(this, prayer, kind),
        )
        // Le son ne peut pas être joué ici : le processus d'un receiver peut
        // être tué dès `onReceive` terminé (voir PrayerSoundService).
        PrayerSoundService.start(this, prayer, kind)
    }

    /**
     * Pas de [PrayerSoundService] ici : le canal des invocations garde le son du
     * système, il n'y a donc rien à jouer nous-mêmes (D27).
     */
    private fun Context.notifyInvocation(id: Long) {
        // Supprimée entre la pose de l'alarme et son déclenchement : rien à annoncer.
        val invocation = miqaatApp.invocationRepository.current()
            .firstOrNull { it.id == id && it.enabled }
            ?: return

        NotificationManagerCompat.from(this).notify(
            InvocationNotifications.idOf(invocation),
            InvocationNotifications.build(this, invocation),
        )
    }

    companion object {
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_KIND = "kind"
        const val EXTRA_INVOCATION = "invocation"

        private const val NO_INVOCATION = -1L
    }
}
