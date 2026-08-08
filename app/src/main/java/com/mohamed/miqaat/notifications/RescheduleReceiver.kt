package com.mohamed.miqaat.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Resynchronise la chaîne d'alarmes quand le système invalide les alarmes
 * programmées : reboot, changement d'heure ou de fuseau horaire.
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> PrayerAlarmScheduler(context).scheduleNext()
        }
    }
}
