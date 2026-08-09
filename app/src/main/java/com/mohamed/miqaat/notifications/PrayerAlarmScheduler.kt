package com.mohamed.miqaat.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mohamed.miqaat.MainActivity
import com.mohamed.miqaat.data.invocations.InvocationRepository
import com.mohamed.miqaat.data.location.LocationRepository
import com.mohamed.miqaat.data.settings.SettingsRepository
import com.mohamed.miqaat.domain.AlarmEventResolver
import com.mohamed.miqaat.domain.PrayerTimesCalculator
import com.mohamed.miqaat.domain.ScheduledEvent
import com.mohamed.miqaat.domain.effectiveMethod
import com.mohamed.miqaat.miqaatApp
import com.mohamed.miqaat.widget.NextPrayerWidget
import java.time.ZonedDateTime

/**
 * Programme l'alarme exacte du prochain évènement — rappel avant l'adhan, adhan
 * lui-même ou invocation — une seule à la fois : chaque déclenchement replanifie
 * la suivante ([PrayerAlarmReceiver]), et [RescheduleReceiver] + l'ouverture de
 * l'app resynchronisent la chaîne après un reboot ou un changement d'heure.
 */
class PrayerAlarmScheduler(
    private val context: Context,
    private val locationRepository: LocationRepository = context.miqaatApp.locationRepository,
    private val settingsRepository: SettingsRepository = context.miqaatApp.settingsRepository,
    private val invocationRepository: InvocationRepository = context.miqaatApp.invocationRepository,
    private val calculator: PrayerTimesCalculator = PrayerTimesCalculator(),
    private val resolver: AlarmEventResolver = AlarmEventResolver(),
) {

    /**
     * Le prochain évènement de la chaîne, sans rien programmer. Extrait de
     * [scheduleNext] pour que l'écran de fiabilité puisse afficher l'heure de la
     * prochaine alerte sans dupliquer le calcul — ni la reposer au passage.
     */
    fun nextEvent(): ScheduledEvent {
        val location = locationRepository.currentLocation()
        val settings = settingsRepository.current()
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

        return resolver.resolveNext(
            now, today, tomorrow,
            settingsRepository.currentReminder(),
            invocationRepository.current(),
        )
    }

    fun scheduleNext() {
        val next = nextEvent()
        val triggerAtMillis = next.time.toInstant().toEpochMilli()

        val intent = Intent(context, PrayerAlarmReceiver::class.java)
            // Ce que le receiver comparera à l'heure réelle : une alerte délivrée
            // trop tard (surcouche qui gèle l'app) ne doit pas s'afficher (D31).
            .putExtra(PrayerAlarmReceiver.EXTRA_TRIGGER_AT, triggerAtMillis)
        when (next) {
            is ScheduledEvent.Prayer -> intent
                .putExtra(PrayerAlarmReceiver.EXTRA_PRAYER, next.prayer.name)
                .putExtra(PrayerAlarmReceiver.EXTRA_KIND, next.kind.name)

            is ScheduledEvent.Invocation -> intent
                .putExtra(PrayerAlarmReceiver.EXTRA_INVOCATION, next.invocationId)
        }

        val fireIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        // Une surcouche peut refuser l'alarme exacte alors même que la permission
        // est accordée. Mieux vaut une alarme inexacte que pas d'alarme du tout :
        // sans repli, l'exception remonterait jusqu'au receiver et romprait la
        // chaîne définitivement.
        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            ) {
                // Exact même en Doze. Deux évènements consécutifs sont séparés d'au
                // moins 10 min — le délai minimal du rappel (ReminderSettings.LEAD_CHOICES)
                // et la garde des invocations (AlarmEventResolver.GUARD_MINUTES) y
                // veillent — donc au-delà du quota d'une alarme par ~9 min : rien
                // n'est jamais reporté.
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, fireIntent,
                )
            } else {
                // Permission d'alarme exacte révoquée : setAlarmClock reste exact sans
                // permission (il affiche l'icône réveil dans la barre de statut).
                val showApp = PendingIntent.getActivity(
                    context,
                    REQUEST_CODE_SHOW_APP,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showApp), fireIntent,
                )
            }
        }.onFailure { error ->
            Log.w(TAG, "Alarme exacte refusée, repli sur une alarme inexacte", error)
            runCatching { alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, fireIntent) }
        }

        scheduleWatchdog(alarmManager)

        // Tout ce qui replanifie l'alarme rend aussi le widget obsolète (heure d'une
        // prière, reboot, changement d'heure, de position ou de réglages) : un seul
        // point de resynchronisation évite d'oublier un cas. L'alarme est déjà posée
        // à ce stade, la chaîne des notifications ne dépend donc pas du widget.
        NextPrayerWidget.refresh(context)
    }

    /**
     * Le filet de sécurité (D33). La chaîne ne tient qu'à une alarme à la fois :
     * une diffusion perdue, une exception imprévue ou un quota Doze la romprait
     * jusqu'à la prochaine ouverture de l'app. Une seconde alarme, inexacte et
     * bihoraire… bi-quotidienne, la répare toute seule.
     *
     * Inexacte volontairement : elle ne notifie rien, elle ne fait que rappeler
     * `scheduleNext()`. Lui donner l'exactitude consommerait le quota Doze de la
     * vraie alarme, ce que D18 et D25 s'interdisent.
     *
     * ⚠ Elle ne répare **pas** une surcouche qui gèle l'application : rien de ce
     * qu'on programme n'est délivré dans ce cas. C'est l'écran de fiabilité qui
     * s'en charge, en faisant régler l'appareil par l'utilisateur.
     */
    private fun scheduleWatchdog(alarmManager: AlarmManager) {
        val intent = Intent(context, RescheduleReceiver::class.java)
            .setAction(RescheduleReceiver.ACTION_WATCHDOG)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WATCHDOG,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + WATCHDOG_DELAY_MS,
                AlarmManager.INTERVAL_HALF_DAY,
                pendingIntent,
            )
        }
    }

    private companion object {
        const val TAG = "PrayerAlarmScheduler"
        const val REQUEST_CODE_ALARM = 1001
        const val REQUEST_CODE_SHOW_APP = 1002
        const val REQUEST_CODE_WATCHDOG = 1003

        /** Première vérification six heures après la pose, puis toutes les douze heures. */
        const val WATCHDOG_DELAY_MS = 6L * 60 * 60 * 1000
    }
}
