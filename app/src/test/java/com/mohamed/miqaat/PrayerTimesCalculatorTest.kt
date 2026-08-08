package com.mohamed.miqaat

import com.mohamed.miqaat.domain.PrayerTimesCalculator
import com.mohamed.miqaat.domain.model.PrayerName
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerTimesCalculatorTest {

    private val calculator = PrayerTimesCalculator()
    private val zone = ZoneId.of("Africa/Algiers")
    private val skikdaLatitude = 36.8665
    private val skikdaLongitude = 6.9063
    private val date = LocalDate.of(2026, 7, 5)

    private fun skikdaTimes() =
        calculator.calculate(skikdaLatitude, skikdaLongitude, date, zone)

    @Test
    fun `horaires de Skikda du 5 juillet 2026 proches de la reference`() {
        val daily = skikdaTimes()

        // Référence : api.aladhan.com, méthode Muslim World League, mêmes coordonnées.
        val expected = mapOf(
            PrayerName.FAJR to LocalTime.of(3, 28),
            PrayerName.SUNRISE to LocalTime.of(5, 19),
            PrayerName.DHUHR to LocalTime.of(12, 37),
            PrayerName.ASR to LocalTime.of(16, 29),
            PrayerName.MAGHRIB to LocalTime.of(19, 55),
            PrayerName.ISHA to LocalTime.of(21, 38),
        )

        expected.forEach { (prayer, reference) ->
            val actual = daily.timeOf(prayer).toLocalTime()
            val deltaMinutes = abs(ChronoUnit.MINUTES.between(reference, actual))
            assertTrue(
                "$prayer : attendu ~$reference, obtenu $actual (écart $deltaMinutes min)",
                deltaMinutes <= 3,
            )
        }
    }

    @Test
    fun `les horaires du jour sont strictement croissants`() {
        val daily = skikdaTimes()
        val times = PrayerName.entries.map { daily.timeOf(it) }
        assertEquals(times.sorted(), times)
        assertEquals(times.distinct().size, times.size)
    }

    @Test
    fun `nextPrayer ignore le shuruq et retourne null apres Isha`() {
        val daily = skikdaTimes()

        // Juste après le Fajr, la prochaine prière est le Dhuhr (le shurūq n'est pas une prière)
        val afterFajr = daily.timeOf(PrayerName.FAJR).plusMinutes(1)
        assertEquals(PrayerName.DHUHR, daily.nextPrayer(afterFajr))

        // Après l'Isha, plus rien aujourd'hui : le ViewModel gérera le Fajr du lendemain
        val afterIsha = daily.timeOf(PrayerName.ISHA).plusMinutes(1)
        assertNull(daily.nextPrayer(afterIsha))
    }
}
