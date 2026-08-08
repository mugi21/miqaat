package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.domain.model.ReminderSettings
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerEventResolverTest {

    private val calculator = PrayerTimesCalculator()
    private val resolver = PrayerEventResolver()
    private val zone = ZoneId.of("Africa/Algiers")
    private val date = LocalDate.of(2026, 7, 5)

    private val today = calculator.calculate(36.8665, 6.9063, date, zone)
    private val tomorrow = calculator.calculate(36.8665, 6.9063, date.plusDays(1), zone)

    private val enabled = ReminderSettings(enabled = true, leadMinutes = 10)
    private val disabled = ReminderSettings(enabled = false)

    @Test
    fun `le rappel s'intercale avant l'adhan`() {
        val beforeAsr = today.timeOf(PrayerName.ASR).minusMinutes(30)
        val next = resolver.resolveNext(beforeAsr, today, tomorrow, enabled)

        assertEquals(PrayerName.ASR, next.prayer)
        assertEquals(PrayerEventKind.REMINDER, next.kind)
        assertEquals(today.timeOf(PrayerName.ASR).minusMinutes(10), next.time)
    }

    @Test
    fun `apres le rappel, le prochain evenement est l'adhan de la meme priere`() {
        val afterReminder = today.timeOf(PrayerName.ASR).minusMinutes(5)
        val next = resolver.resolveNext(afterReminder, today, tomorrow, enabled)

        assertEquals(PrayerName.ASR, next.prayer)
        assertEquals(PrayerEventKind.ADHAN, next.kind)
        assertEquals(today.timeOf(PrayerName.ASR), next.time)
    }

    @Test
    fun `un rappel deja depasse est ignore, seul l'adhan reste`() {
        // Réveil du téléphone entre le rappel manqué et l'adhan.
        val justBeforeAdhan = today.timeOf(PrayerName.MAGHRIB).minusSeconds(30)
        val next = resolver.resolveNext(justBeforeAdhan, today, tomorrow, enabled)

        assertEquals(PrayerEventKind.ADHAN, next.kind)
        assertEquals(PrayerName.MAGHRIB, next.prayer)
    }

    @Test
    fun `rappel desactive, la chaine ne contient que des adhans`() {
        val beforeAsr = today.timeOf(PrayerName.ASR).minusMinutes(30)
        val next = resolver.resolveNext(beforeAsr, today, tomorrow, disabled)

        assertEquals(PrayerEventKind.ADHAN, next.kind)
        assertEquals(today.timeOf(PrayerName.ASR), next.time)
    }

    @Test
    fun `apres l'Isha, on enchaine sur le rappel du Fajr de demain`() {
        val afterIsha = today.timeOf(PrayerName.ISHA).plusMinutes(1)
        val next = resolver.resolveNext(afterIsha, today, tomorrow, enabled)

        assertEquals(PrayerName.FAJR, next.prayer)
        assertEquals(PrayerEventKind.REMINDER, next.kind)
        assertEquals(tomorrow.timeOf(PrayerName.FAJR).minusMinutes(10), next.time)
    }

    @Test
    fun `le shuruq n'a ni rappel ni adhan`() {
        val beforeSunrise = today.timeOf(PrayerName.SUNRISE).minusMinutes(2)
        val next = resolver.resolveNext(beforeSunrise, today, tomorrow, enabled)

        assertEquals(PrayerName.DHUHR, next.prayer)
    }

    @Test
    fun `un delai plus long avance le rappel d'autant`() {
        val settings = ReminderSettings(enabled = true, leadMinutes = 45)
        val wellBefore = today.timeOf(PrayerName.DHUHR).minusHours(1)
        val next = resolver.resolveNext(wellBefore, today, tomorrow, settings)

        assertEquals(PrayerEventKind.REMINDER, next.kind)
        assertEquals(today.timeOf(PrayerName.DHUHR).minusMinutes(45), next.time)
    }
}
