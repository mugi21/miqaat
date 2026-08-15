package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.MinuteRounding
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.domain.model.TimeCalibration
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * L'arrondi à la minute est désormais l'affaire de l'app et non d'Adhan : il doit
 * reproduire exactement l'ancien comportement par défaut, et rien d'autre.
 */
class TimeCalibrationTest {

    private val zone = ZoneId.of("Africa/Algiers")

    private fun at(hour: Int, minute: Int, second: Int): ZonedDateTime =
        ZonedDateTime.of(2026, 8, 15, hour, minute, second, 0, zone)

    @Test
    fun `au plus proche, la bascule est a trente secondes`() {
        // Le seuil d'Adhan : minute + round(seconde / 60), donc 30 s bascule vers le haut.
        assertEquals(at(12, 38, 0), MinuteRounding.NEAREST.apply(at(12, 38, 0)))
        assertEquals(at(12, 38, 0), MinuteRounding.NEAREST.apply(at(12, 38, 29)))
        assertEquals(at(12, 39, 0), MinuteRounding.NEAREST.apply(at(12, 38, 30)))
        assertEquals(at(12, 39, 0), MinuteRounding.NEAREST.apply(at(12, 38, 59)))
    }

    @Test
    fun `vers le bas, les secondes sont simplement coupees`() {
        assertEquals(at(12, 38, 0), MinuteRounding.DOWN.apply(at(12, 38, 0)))
        assertEquals(at(12, 38, 0), MinuteRounding.DOWN.apply(at(12, 38, 59)))
    }

    @Test
    fun `vers le haut, une seule seconde suffit a passer a la minute suivante`() {
        assertEquals(at(12, 38, 0), MinuteRounding.UP.apply(at(12, 38, 0)))
        assertEquals(at(12, 39, 0), MinuteRounding.UP.apply(at(12, 38, 1)))
        assertEquals(at(12, 39, 0), MinuteRounding.UP.apply(at(12, 38, 59)))
    }

    @Test
    fun `l'arrondi franchit l'heure et le jour sans deborder`() {
        assertEquals(at(13, 0, 0), MinuteRounding.NEAREST.apply(at(12, 59, 40)))
        assertEquals(
            ZonedDateTime.of(2026, 8, 16, 0, 0, 0, 0, zone),
            MinuteRounding.UP.apply(at(23, 59, 10)),
        )
    }

    @Test
    fun `le decalage est applique avant l'arrondi`() {
        // L'ordre compte : 12:38:50 + 20 s = 12:39:10, tronqué à 12:39. Arrondir
        // d'abord aurait donné 12:38, puis 12:38:20 — une minute d'écart.
        val calibration = TimeCalibration(
            rounding = MinuteRounding.DOWN,
            offsetSeconds = mapOf(PrayerName.DHUHR to 20),
        )

        assertEquals(at(12, 39, 0), calibration.apply(PrayerName.DHUHR, at(12, 38, 50)))
    }

    @Test
    fun `un moment sans decalage n'est pas touche par celui d'un autre`() {
        val calibration = TimeCalibration(
            rounding = MinuteRounding.DOWN,
            offsetSeconds = mapOf(PrayerName.MAGHRIB to 261),
        )

        assertEquals(0, calibration[PrayerName.FAJR])
        assertEquals(at(12, 38, 0), calibration.apply(PrayerName.FAJR, at(12, 38, 50)))
    }

    @Test
    fun `la calibration par defaut est celle d'Adhan`() {
        assertEquals(MinuteRounding.NEAREST, TimeCalibration.DEFAULT.rounding)
        for (prayer in PrayerName.entries) {
            assertEquals(prayer.name, 0, TimeCalibration.DEFAULT[prayer])
        }
    }
}
