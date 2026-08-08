package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.DailyPrayerTimes
import com.mohamed.miqaat.domain.model.Invocation
import com.mohamed.miqaat.domain.model.InvocationSchedule
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.domain.model.ReminderSettings
import java.time.Duration
import java.time.ZonedDateTime
import kotlin.math.absoluteValue

/** Ce qu'une alarme de la chaîne doit annoncer : un moment de prière, ou une invocation. */
sealed interface ScheduledEvent {

    val time: ZonedDateTime

    data class Prayer(
        val prayer: PrayerName,
        val kind: PrayerEventKind,
        override val time: ZonedDateTime,
    ) : ScheduledEvent

    data class Invocation(
        val invocationId: Long,
        override val time: ZonedDateTime,
    ) : ScheduledEvent
}

/**
 * Le prochain évènement de la chaîne d'alarmes, prières **et** invocations
 * confondues. Deuxième généralisation après [PrayerEventResolver] (D17) : une
 * seule alarme à la fois, donc un seul point de rupture possible (D3).
 *
 * ### La garde (D25)
 *
 * En Doze, Android n'accorde à l'application qu'une alarme
 * `setExactAndAllowWhileIdle` toutes les ~9 minutes — le quota qui avait déjà
 * dicté le minimum de 10 minutes du rappel (D18). Il vaut pour **toute** l'app :
 * une invocation posée cinq minutes avant le Fajr ferait donc reporter l'adhan,
 * ce que l'application s'interdit.
 *
 * D'où la règle, appliquée ici plutôt que dans l'UI (elle est ainsi testable, et
 * l'utilisateur garde le droit de choisir l'heure qu'il veut) :
 *
 * > Les évènements de prière ne bougent **jamais**. Une invocation qui tombe à
 * > moins de [GUARD_MINUTES] d'un évènement déjà placé est repoussée à
 * > `celui-ci + garde`, et le contrôle reprend avec le suivant.
 *
 * Une invocation peut donc sonner plus tard que demandé ; un adhan, jamais.
 */
class AlarmEventResolver(
    private val prayerEvents: PrayerEventResolver = PrayerEventResolver(),
) {

    fun resolveNext(
        now: ZonedDateTime,
        today: DailyPrayerTimes,
        tomorrow: DailyPrayerTimes,
        reminder: ReminderSettings,
        invocations: List<Invocation>,
    ): ScheduledEvent {
        val days = listOf(today, tomorrow)

        val prayers = with(prayerEvents) {
            days.flatMap { day -> day.eventsOf(reminder).toList() }
        }.map { ScheduledEvent.Prayer(it.prayer, it.kind, it.time) }

        val guarded = applyGuard(
            prayerTimes = prayers.map { it.time },
            raw = days.flatMap { day -> invocationEventsOf(day, invocations) },
        )

        return (prayers + guarded)
            .filter { it.time.isAfter(now) }
            .minByOrNull { it.time }
            // Inatteignable : l'Isha de demain est postérieure à tout `now` d'aujourd'hui.
            ?: ScheduledEvent.Prayer(
                PrayerName.ISHA,
                PrayerEventKind.ADHAN,
                tomorrow.timeOf(PrayerName.ISHA),
            )
    }

    /**
     * Les invocations actives d'une journée, à leur heure **demandée** (avant la
     * garde). Le fuseau est celui des horaires du jour : tout reste cohérent si
     * l'utilisateur change de pays.
     */
    fun invocationEventsOf(
        day: DailyPrayerTimes,
        invocations: List<Invocation>,
    ): List<ScheduledEvent.Invocation> =
        invocations.filter { it.enabled }.map { invocation ->
            val time = when (val schedule = invocation.schedule) {
                is InvocationSchedule.FixedTime ->
                    day.date
                        .atTime(schedule.hour, schedule.minute)
                        .atZone(day.timeOf(PrayerName.FAJR).zone)

                is InvocationSchedule.PrayerAnchor ->
                    day.timeOf(schedule.prayer).plusMinutes(schedule.offsetMinutes.toLong())
            }
            ScheduledEvent.Invocation(invocation.id, time)
        }

    /**
     * Écarte les invocations des évènements déjà posés — prières d'abord,
     * puis les invocations traitées avant elles.
     *
     * Un seul passage suffit : les bloqueurs sont parcourus dans l'ordre
     * croissant et une invocation ne se déplace que vers l'avant, donc un
     * bloqueur déjà dépassé ne peut pas redevenir gênant.
     */
    private fun applyGuard(
        prayerTimes: List<ZonedDateTime>,
        raw: List<ScheduledEvent.Invocation>,
    ): List<ScheduledEvent.Invocation> {
        val guard = Duration.ofMinutes(GUARD_MINUTES)
        val placed = mutableListOf<ScheduledEvent.Invocation>()

        raw.sortedBy { it.time }.forEach { event ->
            var time = event.time
            (prayerTimes + placed.map { it.time }).sorted().forEach { blocker ->
                if (Duration.between(blocker, time).toMinutes().absoluteValue < GUARD_MINUTES) {
                    time = blocker.plus(guard)
                }
            }
            placed += event.copy(time = time)
        }
        return placed
    }

    companion object {
        /**
         * Marge minimale entre deux alarmes de l'application. Alignée sur le
         * quota Doze (~9 min) et sur [ReminderSettings.LEAD_CHOICES], dont le
         * premier choix vaut déjà 10 minutes.
         */
        const val GUARD_MINUTES = 10L
    }
}
