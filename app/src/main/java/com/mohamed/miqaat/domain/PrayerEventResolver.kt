package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.DailyPrayerTimes
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.domain.model.ReminderSettings
import java.time.ZonedDateTime

/** Ce qu'une alarme doit annoncer quand elle se déclenche. */
enum class PrayerEventKind {
    /** Le rappel, quelques minutes avant l'adhan. */
    REMINDER,

    /** L'entrée du temps de la prière. */
    ADHAN,
}

data class PrayerEvent(
    val prayer: PrayerName,
    val kind: PrayerEventKind,
    val time: ZonedDateTime,
)

/**
 * Généralise [NextPrayerResolver] à la chaîne d'alarmes : celle-ci ne réveille
 * plus seulement aux adhans, mais aussi aux rappels qui les précèdent.
 *
 * Le principe de D3 est conservé — **une seule alarme à la fois**. On construit
 * simplement la suite des évènements de deux jours, et on prend le premier à
 * venir : rappel ou adhan, la chaîne ne voit qu'un « prochain évènement ».
 * [NextPrayerResolver] reste en place pour l'affichage, qui ne connaît que les
 * prières.
 */
class PrayerEventResolver {

    /**
     * Le prochain évènement strictement après [now]. Jamais null : l'Isha de
     * [tomorrow] est toujours à venir.
     *
     * Un rappel déjà dépassé alors que son adhan approche (ouvrir l'app à
     * 12h55 pour un Dhuhr à 13h00) est simplement ignoré : seul l'adhan reste.
     */
    fun resolveNext(
        now: ZonedDateTime,
        today: DailyPrayerTimes,
        tomorrow: DailyPrayerTimes,
        reminder: ReminderSettings,
    ): PrayerEvent =
        sequenceOf(today, tomorrow)
            .flatMap { day -> day.eventsOf(reminder) }
            .filter { it.time.isAfter(now) }
            .minByOrNull { it.time }
            // Inatteignable : l'Isha de demain est postérieure à tout `now` d'aujourd'hui.
            ?: PrayerEvent(PrayerName.ISHA, PrayerEventKind.ADHAN, tomorrow.timeOf(PrayerName.ISHA))

    /**
     * Tous les évènements d'une journée, dans l'ordre. Public parce que
     * [AlarmEventResolver] en a besoin en entier — et non seulement du prochain —
     * pour tenir les invocations à distance des prières (voir D25).
     */
    fun DailyPrayerTimes.eventsOf(reminder: ReminderSettings): Sequence<PrayerEvent> =
        PrayerName.entries.asSequence()
            .filter { it.isPrayer }
            .flatMap { prayer ->
                val adhan = timeOf(prayer)
                sequence {
                    if (reminder.enabled) {
                        yield(
                            PrayerEvent(
                                prayer,
                                PrayerEventKind.REMINDER,
                                adhan.minusMinutes(reminder.leadMinutes.toLong()),
                            ),
                        )
                    }
                    yield(PrayerEvent(prayer, PrayerEventKind.ADHAN, adhan))
                }
            }
}
