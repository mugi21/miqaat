package com.mohamed.miqaat.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context

/**
 * Widget d'écran d'accueil : la prochaine prière, son heure, le temps restant
 * et les cinq horaires du jour — sans ouvrir l'app.
 *
 * Il ne se réveille jamais de lui-même : [refresh] est appelé par
 * `PrayerAlarmScheduler.scheduleNext()`, c'est-à-dire exactement aux moments où
 * les horaires peuvent avoir changé (heure d'une prière, reboot, changement
 * d'heure, de position ou de réglages). Entre deux, le `Chronometer` de la mise
 * en page fait défiler le décompte côté lanceur, sans réveil ni batterie.
 */
class NextPrayerWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val views = NextPrayerWidgetViews.build(context)
        appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, views) }
    }

    companion object {
        /** Redessine toutes les instances posées sur l'écran d'accueil, s'il y en a. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(
                ComponentName(context.applicationContext, NextPrayerWidget::class.java),
            )
            if (ids.isEmpty()) return
            manager.updateAppWidget(ids, NextPrayerWidgetViews.build(context))
        }
    }
}
