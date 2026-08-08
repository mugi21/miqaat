package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.PrayerName
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RamadanTimesTest {

    private val calculator = PrayerTimesCalculator()
    private val formatter = HijriFormatter()
    private val zone = ZoneId.of("Africa/Algiers")

    // Skikda, 1er Ramadan 1445 (Umm al-Qura).
    private val ramadanDay = LocalDate.of(2024, 3, 11)
    private val times = calculator.calculate(36.8665, 6.9063, ramadanDay, zone)

    @Test
    fun `l'imsak precede le Fajr de dix minutes`() {
        val ramadan = ramadanTimesOf(times)

        assertEquals(
            times.timeOf(PrayerName.FAJR).minusMinutes(IMSAK_MINUTES_BEFORE_FAJR),
            ramadan.imsak,
        )
    }

    @Test
    fun `l'iftar est l'heure exacte du Maghrib`() {
        assertEquals(times.timeOf(PrayerName.MAGHRIB), ramadanTimesOf(times).iftar)
    }

    @Test
    fun `la duree du jeune court de l'imsak a l'iftar`() {
        val ramadan = ramadanTimesOf(times)

        assertEquals(
            Duration.between(ramadan.imsak, ramadan.iftar),
            ramadan.fastingDuration,
        )
        // Un jour de Ramadan de mars à Skikda : entre 12 h et 14 h de jeûne.
        val hours = ramadan.fastingDuration.toHours()
        assertTrue("Durée inattendue : $hours h", hours in 12..14)
    }

    @Test
    fun `le mois de Ramadan est reconnu, ses voisins non`() {
        assertTrue(formatter.toHijri(ramadanDay).isRamadan)
        assertEquals(1, formatter.toHijri(ramadanDay).hijriDayOfMonth)
        // La veille appartient encore à Cha'ban, le mois précédent.
        assertFalse(formatter.toHijri(ramadanDay.minusDays(1)).isRamadan)
        assertEquals(RAMADAN_MONTH - 1, formatter.toHijri(ramadanDay.minusDays(1)).hijriMonth)
    }

    @Test
    fun `le decalage hijri deplace l'entree en Ramadan`() {
        // Avec un ajustement de +1 jour, la veille est déjà comptée en Ramadan.
        assertTrue(formatter.toHijri(ramadanDay.minusDays(1), offsetDays = 1).isRamadan)
    }

    @Test
    fun `le titre de mois hijri porte le nom du mois et l'annee`() {
        val label = formatter.formatMonthYear(ramadanDay)

        assertTrue("« $label » devrait contenir رمضان", label.contains("رمضان"))
        assertTrue("« $label » devrait contenir 1445", label.contains("1445"))
    }
}
