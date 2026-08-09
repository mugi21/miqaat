package com.mohamed.miqaat.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mohamed.miqaat.MainActivity
import com.mohamed.miqaat.R
import com.mohamed.miqaat.data.settings.AppLocale
import com.mohamed.miqaat.domain.model.Invocation
import com.mohamed.miqaat.ui.displayBody
import com.mohamed.miqaat.ui.displayTitle

/**
 * La notification d'une invocation. Volontairement séparée de
 * [PrayerNotifications] : elle porte le texte du dhikr et ouvre l'écran de
 * lecture, là où celle d'une prière annonce une heure.
 *
 * Depuis D39, elle suit le même mode d'alerte que les prières et son son est
 * joué par [AlertSoundService] — celui du système, faute d'enregistrement livré.
 *
 * Un appui ouvre l'invocation dans l'application, prête à être lue.
 */
object InvocationNotifications {

    /**
     * Les identifiants déjà pris : 0..5 les adhans, 100..105 les rappels.
     * Les invocations commencent donc bien plus loin.
     */
    private const val ID_OFFSET = 1_000

    private const val REQUEST_CODE_OFFSET = 3_000

    fun idOf(invocation: Invocation): Int = ID_OFFSET + invocation.id.toInt()

    fun build(base: Context, invocation: Invocation): Notification {
        // Hors activité : appliquer la langue choisie dans les réglages.
        val context = AppLocale.wrap(base)

        val openInvocation = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OFFSET + invocation.id.toInt(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_INVOCATION, invocation.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, NotificationChannels.INVOCATION_ID)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setContentTitle(
                context.getString(
                    R.string.notification_invocation_title,
                    invocation.displayTitle(context),
                ),
            )
            .setContentText(context.getString(R.string.notification_invocation_text))
            // Le texte du dhikr tient rarement sur une ligne : dépliée, la
            // notification en montre déjà le début sans ouvrir l'app.
            .setStyle(NotificationCompat.BigTextStyle().bigText(invocation.displayBody(context)))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openInvocation)
            .build()
    }
}
