package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.PrayerName
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NextPrayerResolverTest {

    private val calculator = PrayerTimesCalculator()
    private val resolver = NextPrayerResolver()
    private val zone = ZoneId.of("Africa/Algiers")
    private val date = LocalDate.of(2026, 7, 5)

    private val today = calculator.calculate(36.8665, 6.9063, date, zone)
    private val tomorrow = calculator.calculate(36.8665, 6.9063, date.plusDays(1), zone)

    @Test
    fun `avant le Fajr, la prochaine priere est le Fajr du jour`() {
        val beforeFajr = today.timeOf(PrayerName.FAJR).minusMinutes(30)
        val next = resolver.resolve(beforeFajr, today, tomorrow)

        assertEquals(PrayerName.FAJR, next.prayer)
        assertEquals(today.timeOf(PrayerName.FAJR), next.time)
        assertFalse(next.isTomorrow)
    }

    @Test
    fun `entre Dhuhr et Asr, la prochaine priere est le Asr`() {
        val afterDhuhr = today.timeOf(PrayerName.DHUHR).plusMinutes(10)
        val next = resolver.resolve(afterDhuhr, today, tomorrow)

        assertEquals(PrayerName.ASR, next.prayer)
        assertFalse(next.isTomorrow)
    }

    @Test
    fun `apres Isha, la prochaine priere est le Fajr du lendemain`() {
        val afterIsha = today.timeOf(PrayerName.ISHA).plusMinutes(1)
        val next = resolver.resolve(afterIsha, today, tomorrow)

        assertEquals(PrayerName.FAJR, next.prayer)
        assertEquals(tomorrow.timeOf(PrayerName.FAJR), next.time)
        assertTrue(next.isTomorrow)
    }

    @Test
    fun `a l'instant exact d'une priere, on passe a la suivante`() {
        // nextPrayer utilise isAfter : à l'heure pile du Asr, le Asr est « atteint »,
        // la prochaine devient le Maghrib.
        val atAsr = today.timeOf(PrayerName.ASR)
        val next = resolver.resolve(atAsr, today, tomorrow)

        assertEquals(PrayerName.MAGHRIB, next.prayer)
        assertFalse(next.isTomorrow)
    }
}
