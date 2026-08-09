package com.mohamed.miqaat.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.mohamed.miqaat.R
import com.mohamed.miqaat.data.settings.AppLocale

/**
 * Les canaux de notification de l'application : l'adhan, le rappel qui le
 * précède, et les invocations. Un canal par nature d'alerte, parce qu'ils n'ont
 * ni le même son ni le même usage — et parce qu'Android laisse alors
 * l'utilisateur couper l'un sans toucher aux autres.
 *
 * ⚠ **Les trois canaux sont muets et sans vibration.** L'application prend tout
 * en main : le son via [AlertSoundService] (D20 — seul moyen de mettre en pause
 * la musique en cours et de ne pas dépendre du lecteur de notifications du
 * système), la vibration via [AlertVibrator]. C'est ce qui rend possible le
 * réglage « mode d'alerte » (D36/D38) : tant que le canal décidait, Android
 * suivait le mode du téléphone et personne ne pouvait le forcer.
 *
 * ⚠ Android **fige** les réglages d'un canal à sa création : tout changement de
 * son ou de vibration impose un nouvel identifiant, et les anciens doivent
 * rejoindre [OLD_IDS] sous peine de laisser des canaux fantômes dans les
 * réglages système de l'utilisateur.
 */
object NotificationChannels {

    /** Entrée du temps de la prière. */
    const val PRAYER_ID = "prayer_times_v4"

    /** Quelques minutes avant l'adhan. */
    const val REMINDER_ID = "prayer_reminder_v3"

    /** Les adhkār. Muet lui aussi depuis D39, qui renverse D27. */
    const val INVOCATION_ID = "invocations_v2"

    private val OLD_IDS = listOf(
        "prayer_times_v1",
        "prayer_times_v2",
        "prayer_times_v3",
        "prayer_reminder_v1",
        "prayer_reminder_v2",
        "invocations_v1",
    )

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
            ),
        )
    }

    private fun Context.buildChannel(
        id: String,
        nameRes: Int,
        descriptionRes: Int,
        // HIGH même sans son : c'est ce qui fait apparaître la bannière par-dessus
        // l'écran en cours, et l'alerte est jouée à côté.
        importance: Int = NotificationManager.IMPORTANCE_HIGH,
    ): NotificationChannel = NotificationChannel(
        id,
        getString(nameRes),
        importance,
    ).apply {
        description = getString(descriptionRes)
        setSound(null, null)
        enableVibration(false)
    }
}
