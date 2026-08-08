package com.mohamed.miqaat.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import com.mohamed.miqaat.MainActivity
import com.mohamed.miqaat.R
import com.mohamed.miqaat.data.settings.AppLocale
import com.mohamed.miqaat.domain.HijriFormatter
import com.mohamed.miqaat.domain.NextPrayerResolver
import com.mohamed.miqaat.domain.PrayerTimesCalculator
import com.mohamed.miqaat.domain.effectiveMethod
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.miqaatApp
import com.mohamed.miqaat.ui.labelRes
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Fabrique le contenu du widget : mêmes sources et mêmes calculs que l'écran
 * d'accueil (position en cache, réglages, Adhan), mais rendus en [RemoteViews].
 *
 * Règle du widget : le lanceur inflate cette vue **dans son propre processus**.
 * On ne lui transmet donc que du texte et des identifiants de ressources —
 * jamais une couleur résolue chez nous — pour que les modes clair et sombre
 * suivent le système comme dans le reste de l'app.
 */
internal object NextPrayerWidgetViews {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    private val calculator = PrayerTimesCalculator()
    private val nextPrayerResolver = NextPrayerResolver()
    private val hijriFormatter = HijriFormatter()

    /** Les cinq prières de la rangée du bas : le shurūq n'en est pas une. */
    private val displayedPrayers = PrayerName.entries.filter { it.isPrayer }

    private val slotIds = listOf(
        Slot(R.id.widget_slot_1, R.id.widget_slot_1_name, R.id.widget_slot_1_time),
        Slot(R.id.widget_slot_2, R.id.widget_slot_2_name, R.id.widget_slot_2_time),
        Slot(R.id.widget_slot_3, R.id.widget_slot_3_name, R.id.widget_slot_3_time),
        Slot(R.id.widget_slot_4, R.id.widget_slot_4_name, R.id.widget_slot_4_time),
        Slot(R.id.widget_slot_5, R.id.widget_slot_5_name, R.id.widget_slot_5_time),
    )

    private data class Slot(val container: Int, val name: Int, val time: Int)

    fun build(base: Context): RemoteViews {
        // Le widget vit hors activité : c'est ici qu'on applique la langue choisie
        // dans les réglages (les RemoteViews ne portent que du texte déjà résolu).
        val context = AppLocale.wrap(base)
        val app = context.miqaatApp
        val location = app.locationRepository.currentLocation()
        val settings = app.settingsRepository.current()
        val now = ZonedDateTime.now(location.zoneId)
        val date = now.toLocalDate()
        val method = settings.effectiveMethod(location.countryCode)

        val today = calculator.calculate(
            location.latitude, location.longitude, date, location.zoneId,
            method, settings.madhab, settings.adjustments,
        )
        val tomorrow = calculator.calculate(
            location.latitude, location.longitude, date.plusDays(1), location.zoneId,
            method, settings.madhab, settings.adjustments,
        )
        val next = nextPrayerResolver.resolve(now, today, tomorrow)

        val views = RemoteViews(context.packageName, R.layout.widget_next_prayer)

        views.setTextViewText(R.id.widget_city, location.cityName)
        views.setTextViewText(
            R.id.widget_hijri,
            hijriFormatter.format(date, settings.hijriOffsetDays),
        )
        views.setTextViewText(
            R.id.widget_next_label,
            context.getString(
                if (next.isTomorrow) R.string.tomorrow_fajr else R.string.next_prayer_title,
            ),
        )
        views.setTextViewText(R.id.widget_next_name, context.getString(next.prayer.labelRes))
        views.setTextViewText(R.id.widget_next_time, timeFormatter.format(next.time))

        // Le décompte tourne côté lanceur : on lui donne l'instant d'arrivée sur
        // l'horloge monotone (elapsedRealtime), immunisée aux changements d'heure.
        views.setChronometer(
            R.id.widget_countdown,
            SystemClock.elapsedRealtime() + Duration.between(now, next.time).toMillis(),
            null,
            true,
        )
        views.setChronometerCountDown(R.id.widget_countdown, true)

        displayedPrayers.forEachIndexed { index, prayer ->
            val slot = slotIds[index]
            val isNext = !next.isTomorrow && prayer == next.prayer
            views.setTextViewText(slot.name, context.getString(prayer.labelRes))
            views.setTextViewText(slot.time, timeFormatter.format(today.timeOf(prayer)))
            // Seul le fond change : les couleurs de texte du thème restent lisibles
            // sur la pastille comme sur la carte, en clair comme en sombre.
            views.setInt(
                slot.container,
                "setBackgroundResource",
                if (isNext) R.drawable.widget_slot_next else 0,
            )
        }

        views.setOnClickPendingIntent(R.id.widget_root, openApp(context))
        return views
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE_OPEN_APP,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private const val REQUEST_CODE_OPEN_APP = 3001
}
