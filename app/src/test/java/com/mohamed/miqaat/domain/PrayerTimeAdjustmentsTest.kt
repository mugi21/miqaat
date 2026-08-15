package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.MethodOption
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.domain.model.PrayerTimeAdjustments
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerTimeAdjustmentsTest {

    private val calculator = PrayerTimesCalculator()
    private val zone = ZoneId.of("Africa/Algiers")
    private val date = LocalDate.of(2026, 8, 6)

    private fun skikda(adjustments: PrayerTimeAdjustments) = calculator.calculate(
        36.8665, 6.9063, date, zone, MethodOption.MUSLIM_WORLD_LEAGUE,
        adjustments = adjustments,
    )

    @Test
    fun `un ajustement decale le moment vise et lui seul`() {
        val plain = skikda(PrayerTimeAdjustments())
        val tuned = skikda(PrayerTimeAdjustments().with(PrayerName.ASR, 4))

        assertEquals(
            Duration.ofMinutes(4),
            Duration.between(plain.timeOf(PrayerName.ASR), tuned.timeOf(PrayerName.ASR)),
        )
        for (prayer in PrayerName.entries.filter { it != PrayerName.ASR }) {
            assertEquals(prayer.name, plain.timeOf(prayer), tuned.timeOf(prayer))
        }
    }

    @Test
    fun `un ajustement negatif avance le moment`() {
        val plain = skikda(PrayerTimeAdjustments())
        val tuned = skikda(PrayerTimeAdjustments().with(PrayerName.ISHA, -7))

        assertEquals(
            Duration.ofMinutes(-7),
            Duration.between(plain.timeOf(PrayerName.ISHA), tuned.timeOf(PrayerName.ISHA)),
        )
    }

    @Test
    fun `l'ajustement manuel s'ajoute a la calibration de la methode, sans l'effacer`() {
        // L'Algérie porte déjà sa marge officielle sur le Maghrib : +2 manuel doit
        // décaler de 2 minutes de plus, pas ramener le moment au calcul brut.
        val plain = calculator.calculate(36.8665, 6.9063, date, zone, MethodOption.ALGERIA)
        val tuned = calculator.calculate(
            36.8665, 6.9063, date, zone, MethodOption.ALGERIA,
            adjustments = PrayerTimeAdjustments().with(PrayerName.MAGHRIB, 2),
        )

        assertEquals(
            Duration.ofMinutes(2),
            Duration.between(plain.timeOf(PrayerName.MAGHRIB), tuned.timeOf(PrayerName.MAGHRIB)),
        )
    }

    @Test
    fun `les valeurs sont bornees`() {
        val tooHigh = PrayerTimeAdjustments().with(PrayerName.FAJR, 500)
        val tooLow = PrayerTimeAdjustments().with(PrayerName.FAJR, -500)

        assertEquals(PrayerTimeAdjustments.MAX_MINUTES, tooHigh[PrayerName.FAJR])
        assertEquals(PrayerTimeAdjustments.MIN_MINUTES, tooLow[PrayerName.FAJR])
    }

    @Test
    fun `revenir a zero efface l'entree, donc deux reglages equivalents sont egaux`() {
        // C'est ce qui permet au cache de l'accueil de reposer sur l'égalité des réglages.
        val roundTrip = PrayerTimeAdjustments().with(PrayerName.DHUHR, 3).with(PrayerName.DHUHR, 0)

        assertEquals(PrayerTimeAdjustments(), roundTrip)
        assertTrue(roundTrip.isEmpty)
    }

    @Test
    fun `le resume ne liste que les moments reellement decales, dans l'ordre du jour`() {
        val adjustments = PrayerTimeAdjustments()
            .with(PrayerName.MAGHRIB, 3)
            .with(PrayerName.FAJR, -2)
            .with(PrayerName.ASR, 0)

        assertEquals(listOf(PrayerName.FAJR, PrayerName.MAGHRIB), adjustments.adjustedPrayers)
        assertFalse(adjustments.isEmpty)
    }

    @Test
    fun `la construction depuis le stockage ecarte zeros et valeurs aberrantes`() {
        val fromStorage = PrayerTimeAdjustments.of(
            mapOf(
                PrayerName.FAJR to 0,
                PrayerName.DHUHR to 999,
                PrayerName.ISHA to -4,
            ),
        )

        assertEquals(listOf(PrayerName.DHUHR, PrayerName.ISHA), fromStorage.adjustedPrayers)
        assertEquals(PrayerTimeAdjustments.MAX_MINUTES, fromStorage[PrayerName.DHUHR])
        assertEquals(-4, fromStorage[PrayerName.ISHA])
    }
}
