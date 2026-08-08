package com.mohamed.miqaat.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import com.mohamed.miqaat.R
import com.mohamed.miqaat.data.settings.AppLocale

/**
 * Les canaux de notification de l'application : l'adhan, le rappel qui le
 * précède, et les invocations. Un canal par nature d'alerte, parce qu'ils n'ont
 * ni le même son ni le même usage — et parce qu'Android laisse alors
 * l'utilisateur régler (ou couper) l'un sans toucher aux autres.
 *
 * ⚠ **Les canaux de prière sont muets** : le son est joué par [PrayerSoundService], seul
 * moyen de mettre en pause la musique en cours et de ne pas dépendre du lecteur
 * de notifications du système (voir D20). Leur laisser un son ferait tout
 * entendre deux fois.
 *
 * La **vibration**, elle, reste au canal : Android suit alors tout seul le mode
 * du téléphone (sonnerie et vibreur → vibration, silencieux → rien), et le
 * service applique la même règle au son.
 *
 * ⚠ Android fige les réglages d'un canal à sa création : tout changement de son
 * impose un nouvel ID — d'où le passage en v3 le jour où le son est parti du canal.
 */
object NotificationChannels {

    /** Entrée du temps de la prière. */
    const val PRAYER_ID = "prayer_times_v3"

    /** Quelques minutes avant l'adhan. */
    const val REMINDER_ID = "prayer_reminder_v2"

    /**
     * Les adhkār. Seul canal à garder un **son** : celui du système, réglable
     * depuis les paramètres Android (voir D27). Une invocation n'a ni la durée
     * ni l'urgence d'un adhan, rien ne justifie de le jouer nous-mêmes.
     */
    const val INVOCATION_ID = "invocations_v1"

    private val OLD_IDS = listOf("prayer_times_v1", "prayer_times_v2", "prayer_reminder_v1")

    /**
     * Idempotent : recréer un canal avec le même ID ne fait qu'en rafraîchir le
     * nom et la description — c'est ainsi qu'un changement de langue se propage
     * jusqu'aux réglages système.
     */
    fun createAll(base: Context) {
        val context = AppLocale.wrap(base)
        val manager = context.getSystemService(NotificationManager::class.java)

        // Nettoyer les anciens canaux pour ne pas polluer les réglages de l'utilisateur.
        OLD_IDS.forEach(manager::deleteNotificationChannel)

        manager.createNotificationChannel(
            context.buildChannel(
                id = PRAYER_ID,
                nameRes = R.string.notification_channel_name,
                descriptionRes = R.string.notification_channel_description,
            ),
        )
        manager.createNotificationChannel(
            context.buildChannel(
                id = REMINDER_ID,
                nameRes = R.string.notification_reminder_channel_name,
                descriptionRes = R.string.notification_reminder_channel_description,
            ),
        )
        manager.createNotificationChannel(
            context.buildChannel(
                id = INVOCATION_ID,
                nameRes = R.string.notification_invocation_channel_name,
                descriptionRes = R.string.notification_invocation_channel_description,
                // DEFAULT et non HIGH : un rappel d'adhkār se signale, il
                // n'interrompt pas ce que l'utilisateur est en train de faire.
                importance = NotificationManager.IMPORTANCE_DEFAULT,
                sound = Settings.System.DEFAULT_NOTIFICATION_URI,
            ),
        )
    }

    private fun Context.buildChannel(
        id: String,
        nameRes: Int,
        descriptionRes: Int,
        // HIGH même sans son : c'est ce qui fait apparaître la bannière par-dessus
        // l'écran en cours, et le son est joué à côté.
        importance: Int = NotificationManager.IMPORTANCE_HIGH,
        /** Null = canal muet, le son est joué par [PrayerSoundService] (D20). */
        sound: Uri? = null,
    ): NotificationChannel = NotificationChannel(
        id,
        getString(nameRes),
        importance,
    ).apply {
        description = getString(descriptionRes)
        setSound(
            sound,
            sound?.let {
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            },
        )
        enableVibration(true)
    }
}
