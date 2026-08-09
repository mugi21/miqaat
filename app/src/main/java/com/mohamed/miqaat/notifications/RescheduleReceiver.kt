package com.mohamed.miqaat.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Resynchronise la chaîne d'alarmes chaque fois que ce sur quoi elle repose a pu
 * changer sans qu'elle en soit avertie :
 *
 * - **redémarrage** : les alarmes ne survivent pas à l'extinction ;
 * - **heure ou fuseau** : l'instant visé n'est plus le bon ;
 * - **mise à jour de l'application** : Android annule les alarmes du paquet
 *   remplacé. Sans ce cas, une mise à jour tuait la chaîne silencieusement
 *   jusqu'à la prochaine ouverture de l'app ;
 * - **permission d'alarme exacte** modifiée (Android 12/12L) ;
 * - **chien de garde** : l'alarme inexacte semi-quotidienne posée par
 *   [PrayerAlarmScheduler], seul filet si une diffusion s'est perdue (D33).
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val handled = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_WATCHDOG,
            -> true

            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED ->
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

            else -> false
        }
        if (!handled) return

        // Une exception ici laisserait la chaîne désarmée jusqu'à la prochaine
        // ouverture de l'app : on la journalise plutôt que de la propager.
        runCatching { PrayerAlarmScheduler(context).scheduleNext() }
            .onFailure { Log.w(TAG, "Replanification impossible (${intent.action})", it) }
    }

    companion object {
        /** Action maison du chien de garde ; jamais diffusée par le système. */
        const val ACTION_WATCHDOG = "com.mohamed.miqaat.action.WATCHDOG"

        private const val TAG = "RescheduleReceiver"
    }
}
