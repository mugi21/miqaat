package com.mohamed.miqaat.notifications

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.mohamed.miqaat.domain.RingerState

/**
 * Seul point de l'application qui lit l'état sonore du téléphone, pour le réduire
 * au [RingerState] que [com.mohamed.miqaat.domain.AlertResolver] sait manipuler.
 *
 * Deux sources, parce qu'aucune ne suffit seule : le mode sonnerie
 * (`AudioManager`) ignore « Ne pas déranger », et le filtre d'interruption ignore
 * le bouton de volume.
 *
 * `getCurrentInterruptionFilter()` ne demande **aucune** permission, contrairement
 * à `getNotificationPolicy()` / `setInterruptionFilter()` qui exigent
 * `ACCESS_NOTIFICATION_POLICY` — permission lourde qu'on ne demande pas (D37).
 */
object RingerReader {

    fun read(context: Context): RingerState {
        val notifications = context.getSystemService(NotificationManager::class.java)
        val filter = runCatching { notifications.currentInterruptionFilter }.getOrNull()
        if (filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
            filter == NotificationManager.INTERRUPTION_FILTER_ALARMS
        ) {
            // « Silence total » et « alarmes seulement » coupent l'un et l'autre
            // le son des notifications : de notre point de vue, c'est silencieux.
            // Le mode ALWAYS_SOUND passera quand même sur le flux « alarme »,
            // sauf en silence total où le système gagne toujours.
            return RingerState.SILENT
        }

        return when (context.getSystemService(AudioManager::class.java)?.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> RingerState.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> RingerState.VIBRATE
            else -> RingerState.NORMAL
        }
    }
}
