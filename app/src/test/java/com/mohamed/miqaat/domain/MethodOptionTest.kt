package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.MethodOption
import com.mohamed.miqaat.domain.model.PrayerName
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Vérifie que les méthodes nationales produisent bien les horaires attendus
 * (angles et ajustements relevés sur l'API AlAdhan `v1/methods`).
 */
class MethodOptionTest {

    private val calculator = PrayerTimesCalculator()
    private val date = LocalDate.of(2026, 7, 5)

    private fun skikda(method: MethodOption) =
        calculator.calculate(36.8665, 6.9063, date, ZoneId.of("Africa/Algiers"), method)

    private fun lisbonne(method: MethodOption) =
        calculator.calculate(38.7223, -9.1393, date, ZoneId.of("Europe/Lisbon"), method)

    @Test
    fun `l'Algerie reprend les angles de MWL`() {
        // Le calendrier algérien ne se distingue pas par ses angles mais par sa
        // calibration (marge de précaution + point de référence) — voir
        // AlgeriaOfficialCalendarTest, qui la verrouille sur un mois entier.
        val mwl = MethodOption.MUSLIM_WORLD_LEAGUE.parameters
        val algeria = MethodOption.ALGERIA.parameters

        assertEquals(mwl.fajrAngle, algeria.fajrAngle, 0.0)
        assertEquals(mwl.ishaAngle, algeria.ishaAngle, 0.0)
    }

    @Test
    fun `la calibration algerienne retarde de moins de deux minutes, sauf le Maghrib`() {
        val algeria = skikda(MethodOption.ALGERIA)
        val mwl = skikda(MethodOption.MUSLIM_WORLD_LEAGUE)

        for (prayer in PrayerName.entries) {
            val gap = Duration.between(mwl.timeOf(prayer), algeria.timeOf(prayer))
            // Le Maghrib porte 3 minutes de plus que les autres moments (D23).
            val most = if (prayer == PrayerName.MAGHRIB) Duration.ofMinutes(5) else Duration.ofMinutes(2)
            assertTrue(
                "$prayer : écart $gap",
                gap >= Duration.ofMinutes(-1) && gap <= most,
            )
        }
    }

    @Test
    fun `la marge algerienne du Maghrib ne se propage pas a l'Isha`() {
        // L'Isha algérienne est calculée par angle (17°), pas par intervalle depuis
        // le coucher : contrairement au Portugal, elle n'hérite pas de la marge.
        assertEquals(0, MethodOption.ALGERIA.parameters.ishaInterval)

        val gap = Duration.between(
            skikda(MethodOption.MUSLIM_WORLD_LEAGUE).timeOf(PrayerName.ISHA),
            skikda(MethodOption.ALGERIA).timeOf(PrayerName.ISHA),
        )
        assertTrue("écart Isha: $gap", gap.abs() <= Duration.ofMinutes(1))
    }

    @Test
    fun `le Fajr marocain (19 degres) precede le Fajr MWL (18 degres)`() {
        val morocco = skikda(MethodOption.MOROCCO)
        val mwl = skikda(MethodOption.MUSLIM_WORLD_LEAGUE)

        assertTrue(morocco.timeOf(PrayerName.FAJR).isBefore(mwl.timeOf(PrayerName.FAJR)))
        // Même angle Isha (17°) que MWL
        assertEquals(mwl.timeOf(PrayerName.ISHA), morocco.timeOf(PrayerName.ISHA))
    }

    @Test
    fun `la Jordanie est la Tunisie avec un Maghrib retarde de 5 minutes`() {
        val jordan = skikda(MethodOption.JORDAN)
        val tunisia = skikda(MethodOption.TUNISIA)

        // Mêmes angles 18°/18°
        assertEquals(tunisia.timeOf(PrayerName.FAJR), jordan.timeOf(PrayerName.FAJR))
        assertEquals(tunisia.timeOf(PrayerName.ISHA), jordan.timeOf(PrayerName.ISHA))
        assertEquals(
            Duration.ofMinutes(5),
            Duration.between(tunisia.timeOf(PrayerName.MAGHRIB), jordan.timeOf(PrayerName.MAGHRIB)),
        )
    }

    @Test
    fun `dans le Golfe le Isha suit le Maghrib de 90 minutes`() {
        val gulf = skikda(MethodOption.GULF)

        assertEquals(
            Duration.ofMinutes(90),
            Duration.between(gulf.timeOf(PrayerName.MAGHRIB), gulf.timeOf(PrayerName.ISHA)),
        )
    }

    @Test
    fun `au Portugal le Isha suit le Maghrib de 77 minutes et le Maghrib est retarde de 3`() {
        val portugal = lisbonne(MethodOption.PORTUGAL)
        val mwl = lisbonne(MethodOption.MUSLIM_WORLD_LEAGUE)

        assertEquals(
            Duration.ofMinutes(77),
            Duration.between(portugal.timeOf(PrayerName.MAGHRIB), portugal.timeOf(PrayerName.ISHA)),
        )
        assertEquals(
            Duration.ofMinutes(3),
            Duration.between(mwl.timeOf(PrayerName.MAGHRIB), portugal.timeOf(PrayerName.MAGHRIB)),
        )
    }

    @Test
    fun `le Fajr francais (12 degres) suit le Fajr MWL (18 degres)`() {
        val france = skikda(MethodOption.FRANCE)
        val mwl = skikda(MethodOption.MUSLIM_WORLD_LEAGUE)

        assertTrue(france.timeOf(PrayerName.FAJR).isAfter(mwl.timeOf(PrayerName.FAJR)))
    }
}
