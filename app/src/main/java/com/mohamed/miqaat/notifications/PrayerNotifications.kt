package com.mohamed.miqaat.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RawRes
import androidx.core.app.NotificationCompat
import com.mohamed.miqaat.MainActivity
import com.mohamed.miqaat.R
import com.mohamed.miqaat.data.settings.AppLocale
import com.mohamed.miqaat.domain.PrayerEventKind
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.miqaatApp
import com.mohamed.miqaat.ui.labelRes

/**
 * Tout ce qui décrit la notification d'un évènement, au même endroit : son
 * identifiant, son canal, son son, et son contenu déjà traduit.
 *
 * [PrayerAlarmReceiver] la pose et [PrayerSoundService] la reprend à son compte
 * pour rester en avant-plan le temps du son — les deux doivent construire
 * exactement la même, d'où ce point unique.
 */
object PrayerNotifications {

    /** Les adhans occupent les ordinaux de [PrayerName] ; les rappels sont décalés pour ne pas les écraser. */
    private const val REMINDER_ID_OFFSET = 100

    private const val REQUEST_CODE_OPEN_APP = 2001

    fun idOf(prayer: PrayerName, kind: PrayerEventKind): Int = when (kind) {
        PrayerEventKind.ADHAN -> prayer.ordinal
        PrayerEventKind.REMINDER -> REMINDER_ID_OFFSET + prayer.ordinal
    }

    fun channelOf(kind: PrayerEventKind): String = when (kind) {
        PrayerEventKind.ADHAN -> NotificationChannels.PRAYER_ID
        PrayerEventKind.REMINDER -> NotificationChannels.REMINDER_ID
    }

    @RawRes
    fun soundOf(kind: PrayerEventKind): Int = when (kind) {
        PrayerEventKind.ADHAN -> R.raw.prayer_notification
        PrayerEventKind.REMINDER -> R.raw.prayer_reminder
    }

    fun build(base: Context, prayer: PrayerName, kind: PrayerEventKind): Notification {
        // Hors activité : appliquer la langue choisie dans les réglages.
        val context = AppLocale.wrap(base)
        val name = context.getString(prayer.labelRes)

        val title: String
        val text: String
        when (kind) {
            PrayerEventKind.ADHAN -> {
                title = context.getString(R.string.notification_prayer_time, name)
                text = context.getString(R.string.notification_call)
            }

            PrayerEventKind.REMINDER -> {
                // Le délai vient des réglages et non de l'intent : c'est celui qui a
                // servi à poser l'alarme, et il ne peut pas avoir changé entre-temps
                // sans que la chaîne ne soit replanifiée.
                val minutes = context.miqaatApp.settingsRepository.currentReminder().leadMinutes
                title = context.getString(R.string.notification_reminder_title, name)
                text = context.getString(
                    R.string.notification_reminder_text,
                    context.resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes),
                )
            }
        }

        val openApp = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_APP,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, channelOf(kind))
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()
    }
}
