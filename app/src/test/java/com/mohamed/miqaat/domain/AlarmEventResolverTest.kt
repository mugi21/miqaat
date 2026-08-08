package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.Invocation
import com.mohamed.miqaat.domain.model.InvocationSchedule
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.domain.model.ReminderSettings
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmEventResolverTest {

    private val calculator = PrayerTimesCalculator()
    private val resolver = AlarmEventResolver()
    private val zone = ZoneId.of("Africa/Algiers")
    private val date = LocalDate.of(2026, 7, 5)

    private val today = calculator.calculate(36.8665, 6.9063, date, zone)
    private val tomorrow = calculator.calculate(36.8665, 6.9063, date.plusDays(1), zone)

    private val reminderOff = ReminderSettings(enabled = false)
    private val reminderOn = ReminderSettings(enabled = true, leadMinutes = 10)

    private fun invocation(
        id: Long = 1L,
        schedule: InvocationSchedule,
        enabled: Boolean = true,
    ) = Invocation(
        id = id,
        builtin = null,
        title = "دعاء",
        body = "نص",
        enabled = enabled,
        schedule = schedule,
        sortOrder = 0,
    )

    private fun invocationTime(vararg invocations: Invocation) =
        resolver.invocationEventsOf(today, invocations.toList()).map { it.time }

    @Test
    fun `une invocation a heure fixe tombe a l'heure demandee`() {
        val times = invocationTime(invocation(schedule = InvocationSchedule.FixedTime(22, 30)))

        assertEquals(1, times.size)
        assertEquals(date.atTime(22, 30).atZone(zone), times.first())
    }

    @Test
    fun `une invocation ancree suit la priere, avant comme apres`() {
        val after = invocationTime(
            invocation(schedule = InvocationSchedule.PrayerAnchor(PrayerName.FAJR, 30)),
        )
        val before = invocationTime(
            invocation(schedule = InvocationSchedule.PrayerAnchor(PrayerName.MAGHRIB, -20)),
        )

        assertEquals(today.timeOf(PrayerName.FAJR).plusMinutes(30), after.first())
        assertEquals(today.timeOf(PrayerName.MAGHRIB).minusMinutes(20), before.first())
    }

    @Test
    fun `une invocation desactivee ne produit aucun evenement`() {
        val times = invocationTime(
            invocation(schedule = InvocationSchedule.FixedTime(9, 0), enabled = false),
        )

        assertTrue(times.isEmpty())
    }

    @Test
    fun `loin de toute priere, l'invocation garde son heure`() {
        // Le Dhuhr d'été à Skikda est vers 12h37 : une heure avant, rien ne gêne.
        val wanted = today.timeOf(PrayerName.DHUHR).minusHours(2)
        val schedule = InvocationSchedule.PrayerAnchor(PrayerName.DHUHR, -120)

        val next = resolver.resolveNext(
            now = wanted.minusMinutes(1),
            today = today,
            tomorrow = tomorrow,
            reminder = reminderOff,
            invocations = listOf(invocation(schedule = schedule)),
        )

        assertEquals(wanted, next.time)
        assertTrue(next is ScheduledEvent.Invocation)
    }

    @Test
    fun `une invocation trop proche d'un adhan est repoussee apres lui`() {
        // Cinq minutes avant l'Asr : la poser là ferait reporter l'adhan (D25).
        val asr = today.timeOf(PrayerName.ASR)
        val invocations = listOf(
            invocation(schedule = InvocationSchedule.PrayerAnchor(PrayerName.ASR, -5)),
        )

        val next = resolver.resolveNext(
            now = asr.minusMinutes(30),
            today = today,
            tomorrow = tomorrow,
            reminder = reminderOff,
            invocations = invocations,
        )

        // L'adhan passe d'abord, intact…
        assertEquals(asr, next.time)
        assertTrue(next is ScheduledEvent.Prayer)

        // …et l'invocation le suit à la distance de garde.
        val afterAdhan = resolver.resolveNext(
            now = asr, today = today, tomorrow = tomorrow,
            reminder = reminderOff, invocations = invocations,
        )
        assertEquals(asr.plusMinutes(AlarmEventResolver.GUARD_MINUTES), afterAdhan.time)
        assertTrue(afterAdhan is ScheduledEvent.Invocation)
    }

    @Test
    fun `la poussee se repete jusqu'a degager le rappel et l'adhan`() {
        // Juste avant le rappel du Maghrib : l'invocation doit franchir les deux.
        val maghrib = today.timeOf(PrayerName.MAGHRIB)
        val invocations = listOf(
            invocation(schedule = InvocationSchedule.PrayerAnchor(PrayerName.MAGHRIB, -12)),
        )

        val next = resolver.resolveNext(
            now = maghrib.minusMinutes(20),
            today = today,
            tomorrow = tomorrow,
            reminder = reminderOn,
            invocations = invocations,
        )

        // Le rappel (−10 min) reste le prochain évènement, à sa place.
        assertEquals(maghrib.minusMinutes(10), next.time)

        val afterAdhan = resolver.resolveNext(
            now = maghrib, today = today, tomorrow = tomorrow,
            reminder = reminderOn, invocations = invocations,
        )
        assertEquals(maghrib.plusMinutes(AlarmEventResolver.GUARD_MINUTES), afterAdhan.time)
    }

    @Test
    fun `aucun evenement de priere n'est deplace par une invocation`() {
        val invocations = PrayerName.entries.filter { it.isPrayer }.mapIndexed { index, prayer ->
            invocation(
                id = index + 1L,
                schedule = InvocationSchedule.PrayerAnchor(prayer, -3),
            )
        }

        val prayerTimes = PrayerName.entries.filter { it.isPrayer }.map { today.timeOf(it) }
        var now = today.timeOf(PrayerName.FAJR).minusHours(1)
        val fired = mutableListOf<ZonedDateTime>()

        // On déroule la chaîne d'une journée entière comme le ferait le receiver.
        repeat(30) {
            val event = resolver.resolveNext(now, today, tomorrow, reminderOn, invocations)
            if (event is ScheduledEvent.Prayer && event.kind == PrayerEventKind.ADHAN) {
                fired += event.time
            }
            now = event.time
        }

        assertTrue(prayerTimes.all { it in fired })
    }

    @Test
    fun `deux evenements consecutifs restent separes de la garde`() {
        val invocations = listOf(
            invocation(id = 1L, schedule = InvocationSchedule.PrayerAnchor(PrayerName.ASR, -5)),
            invocation(id = 2L, schedule = InvocationSchedule.PrayerAnchor(PrayerName.ASR, 2)),
        )

        var now = today.timeOf(PrayerName.ASR).minusHours(1)
        var previous: ZonedDateTime? = null
        repeat(6) {
            val event = resolver.resolveNext(now, today, tomorrow, reminderOn, invocations)
            previous?.let {
                val gap = Duration.between(it, event.time).toMinutes()
                assertTrue("écart de $gap min", gap >= AlarmEventResolver.GUARD_MINUTES)
            }
            previous = event.time
            now = event.time
        }
    }

    @Test
    fun `apres la derniere invocation du jour, la chaine passe a demain`() {
        val invocations = listOf(
            invocation(schedule = InvocationSchedule.PrayerAnchor(PrayerName.FAJR, 30)),
        )

        val afterIsha = today.timeOf(PrayerName.ISHA).plusMinutes(1)
        val next = resolver.resolveNext(afterIsha, today, tomorrow, reminderOff, invocations)

        assertEquals(tomorrow.timeOf(PrayerName.FAJR), next.time)
        assertTrue(next is ScheduledEvent.Prayer)
    }
}
