package com.mohamed.miqaat.domain

import com.batoulapps.adhan2.Madhab
import com.mohamed.miqaat.domain.model.MethodOption
import com.mohamed.miqaat.domain.model.PrayerName
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculationSettingsTest {

    private val calculator = PrayerTimesCalculator()
    private val zone = ZoneId.of("Africa/Algiers")
    private val date = LocalDate.of(2026, 7, 5)

    private fun skikda(method: MethodOption, madhab: Madhab) =
        calculator.calculate(36.8665, 6.9063, date, zone, method, madhab)

    @Test
    fun `le Asr hanafi est plus tardif que le Asr du jumhur`() {
        val shafi = skikda(MethodOption.MUSLIM_WORLD_LEAGUE, Madhab.SHAFI)
        val hanafi = skikda(MethodOption.MUSLIM_WORLD_LEAGUE, Madhab.HANAFI)

        assertTrue(
            hanafi.timeOf(PrayerName.ASR).isAfter(shafi.timeOf(PrayerName.ASR)),
        )
        // Le madhab n'influence que le Asr
        assertEquals(shafi.timeOf(PrayerName.FAJR), hanafi.timeOf(PrayerName.FAJR))
        assertEquals(shafi.timeOf(PrayerName.MAGHRIB), hanafi.timeOf(PrayerName.MAGHRIB))
    }

    @Test
    fun `changer de methode change le Fajr`() {
        val mwl = skikda(MethodOption.MUSLIM_WORLD_LEAGUE, Madhab.SHAFI)
        val egyptian = skikda(MethodOption.EGYPTIAN, Madhab.SHAFI)

        // MWL : angle Fajr 18° ; Égypte : 19,5° → Fajr plus tôt
        assertTrue(
            egyptian.timeOf(PrayerName.FAJR).isBefore(mwl.timeOf(PrayerName.FAJR)),
        )
    }

    @Test
    fun `le decalage hijri deplace la date d'exactement n jours`() {
        val formatter = HijriFormatter()
        val base = formatter.format(date)

        assertNotEquals(base, formatter.format(date, 1))
        assertEquals(formatter.format(date.plusDays(1)), formatter.format(date, 1))
        assertEquals(formatter.format(date.minusDays(2)), formatter.format(date, -2))
    }
}
