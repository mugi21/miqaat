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
 * Le filet qui tient la calibration algérienne : le calendrier officiel de la wilaya
 * de Skikda pour Rabīʿ al-Awwal 1448, trente jours et cinq moments, tel qu'il est
 * imprimé (مديرية الشؤون الدينية والأوقاف — سكيكدة).
 *
 * Protocole de mesure et valeurs retenues : `docs/prayer-times-accuracy.md`.
 */
class AlgeriaOfficialCalendarTest {

    private val calculator = PrayerTimesCalculator()
    private val zone = ZoneId.of("Africa/Algiers")
    private val latitude = 36.8665
    private val longitude = 6.9063

    /** Fajr, Ẓuhr, ʿAṣr, Maghrib, ʿIshāʾ — le shurūq n'a pas de colonne officielle. */
    private val moments = listOf(
        PrayerName.FAJR, PrayerName.DHUHR, PrayerName.ASR,
        PrayerName.MAGHRIB, PrayerName.ISHA,
    )

    private val official = """
        04:13 12:38 16:23 19:29 20:56
        04:15 12:38 16:23 19:28 20:54
        04:16 12:38 16:22 19:26 20:52
        04:17 12:37 16:22 19:25 20:51
        04:18 12:37 16:21 19:24 20:49
        04:19 12:37 16:21 19:23 20:48
        04:20 12:37 16:20 19:21 20:46
        04:22 12:36 16:20 19:20 20:45
        04:23 12:36 16:19 19:19 20:43
        04:24 12:36 16:18 19:17 20:41
        04:25 12:36 16:18 19:16 20:40
        04:26 12:35 16:17 19:15 20:38
        04:27 12:35 16:16 19:13 20:36
        04:28 12:35 16:16 19:12 20:35
        04:29 12:35 16:15 19:11 20:33
        04:30 12:34 16:14 19:09 20:32
        04:32 12:34 16:14 19:08 20:30
        04:33 12:34 16:13 19:06 20:28
        04:34 12:33 16:12 19:05 20:27
        04:35 12:33 16:11 19:03 20:25
        04:36 12:33 16:10 19:02 20:23
        04:37 12:32 16:10 19:00 20:22
        04:38 12:32 16:09 18:59 20:20
        04:39 12:32 16:08 18:58 20:18
        04:40 12:31 16:07 18:56 20:17
        04:41 12:31 16:06 18:55 20:15
        04:42 12:31 16:05 18:53 20:13
        04:43 12:30 16:04 18:52 20:12
        04:44 12:30 16:04 18:50 20:10
        04:45 12:30 16:03 18:49 20:08
    """.trimIndent().lines().mapIndexed { index, line ->
        LocalDate.of(2026, 8, 14).plusDays(index.toLong()) to
            moments.zip(line.trim().split(" ").map(LocalTime::parse)).toMap()
    }

    private fun appTime(date: LocalDate, prayer: PrayerName): LocalTime =
        calculator.calculate(latitude, longitude, date, zone, MethodOption.ALGERIA)
            .timeOf(prayer)
            .toLocalTime()

    @Test
    fun `l'app n'annonce jamais un moment avant l'heure officielle`() {
        // L'invariant qui compte : la marge de précaution d'un ministère existe pour
        // que l'heure annoncée ne tombe jamais avant l'heure calculée. Une minute de
        // retard est sans conséquence, une minute d'avance invalide la prière.
        for ((date, expected) in official) {
            for (prayer in moments) {
                val actual = appTime(date, prayer)
                assertTrue(
                    "$prayer du $date : officiel ${expected.getValue(prayer)}, app $actual",
                    !actual.isBefore(expected.getValue(prayer)),
                )
            }
        }
    }

    @Test
    fun `l'app ne depasse jamais l'heure officielle d'une minute`() {
        for ((date, expected) in official) {
            for (prayer in moments) {
                val gap = Duration.between(expected.getValue(prayer), appTime(date, prayer))
                assertTrue(
                    "$prayer du $date : écart $gap",
                    gap <= Duration.ofMinutes(1),
                )
            }
        }
    }

    @Test
    fun `le Fajr, le Zuhr et le Maghrib tombent juste les trente jours`() {
        // Ces trois moments s'expliquent par un décalage constant, sans reste.
        for ((date, expected) in official) {
            for (prayer in listOf(PrayerName.FAJR, PrayerName.DHUHR, PrayerName.MAGHRIB)) {
                assertEquals(
                    "$prayer du $date",
                    expected.getValue(prayer),
                    appTime(date, prayer),
                )
            }
        }
    }

    @Test
    fun `l'Asr et l'Isha tombent juste sur la grande majorite du mois`() {
        // Ces deux moments dérivent d'une dizaine de secondes sur le mois : aucun
        // décalage constant ne les rend exacts partout. On garde le côté tardif, ce
        // qui laisse cinq jours à une minute près. Les chiffres sont figés ici pour
        // qu'une retouche de la calibration ne les dégrade pas en silence.
        val exact = mapOf(PrayerName.ASR to 25, PrayerName.ISHA to 28)

        for ((prayer, expectedCount) in exact) {
            val count = official.count { (date, expected) ->
                appTime(date, prayer) == expected.getValue(prayer)
            }
            assertEquals("$prayer exact sur 30 jours", expectedCount, count)
        }
    }

    @Test
    fun `le shuruq reste entre le Fajr et le Zuhr`() {
        // Il n'a pas de colonne officielle : on vérifie seulement qu'il garde sa place.
        for ((date, expected) in official) {
            val sunrise = appTime(date, PrayerName.SUNRISE)
            assertTrue(
                "shurūq du $date : $sunrise",
                sunrise.isAfter(expected.getValue(PrayerName.FAJR)) &&
                    sunrise.isBefore(expected.getValue(PrayerName.DHUHR)),
            )
        }
    }
}
