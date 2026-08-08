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
    fun `l'Algerie a les memes angles que MWL, au Maghrib pres`() {
        val algeria = skikda(MethodOption.ALGERIA)
        val mwl = skikda(MethodOption.MUSLIM_WORLD_LEAGUE)

        // Mêmes angles (18°/17°) → mêmes horaires. Deux exceptions : le Dhuhr peut
        // différer d'une minute (la librairie Adhan ajoute +1 min de précaution à MWL,
        // absent de la spécification AlAdhan des méthodes nationales), et le Maghrib
        // porte la marge de 3 min du calendrier officiel algérien.
        val sameAsMwl = PrayerName.entries.filter {
            it != PrayerName.DHUHR && it != PrayerName.MAGHRIB
        }
        for (prayer in sameAsMwl) {
            assertEquals(prayer.name, mwl.timeOf(prayer), algeria.timeOf(prayer))
        }
        assertEquals(
            Duration.ofMinutes(3),
            Duration.between(mwl.timeOf(PrayerName.MAGHRIB), algeria.timeOf(PrayerName.MAGHRIB)),
        )
        val dhuhrGap = Duration.between(
            algeria.timeOf(PrayerName.DHUHR), mwl.timeOf(PrayerName.DHUHR),
        ).abs()
        assertTrue("écart Dhuhr: $dhuhrGap", dhuhrGap <= Duration.ofMinutes(1))
    }

    @Test
    fun `le Maghrib algerien colle au calendrier officiel sur deux saisons`() {
        // Relevés sur le calendrier officiel à Skikda, à quatre mois d'écart :
        // la marge de 3 min est constante, ce n'est pas un artefact d'arrondi.
        val zone = ZoneId.of("Africa/Algiers")
        val officialMaghrib = mapOf(
            LocalDate.of(2026, 8, 6) to LocalTime.of(19, 37),
            LocalDate.of(2026, 12, 15) to LocalTime.of(17, 20),
        )

        for ((day, expected) in officialMaghrib) {
            val times = calculator.calculate(36.8665, 6.9063, day, zone, MethodOption.ALGERIA)
            assertEquals(
                "Maghrib du $day",
                expected,
                times.timeOf(PrayerName.MAGHRIB).toLocalTime().withSecond(0).withNano(0),
            )
        }
    }

    @Test
    fun `la marge algerienne du Maghrib ne decale pas l'Isha`() {
        // L'Isha algérienne est calculée par angle (17°), pas par intervalle depuis
        // le coucher : contrairement au Portugal, elle n'hérite pas de l'ajustement.
        assertEquals(
            skikda(MethodOption.MUSLIM_WORLD_LEAGUE).timeOf(PrayerName.ISHA),
            skikda(MethodOption.ALGERIA).timeOf(PrayerName.ISHA),
        )
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
