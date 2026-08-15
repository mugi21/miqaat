package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.QuranSuggestion.Reason
import com.mohamed.miqaat.domain.model.DailyPrayerTimes
import com.mohamed.miqaat.domain.model.PrayerName
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * La sourate du moment. Ce qui est vérifié ici n'est pas seulement « telle règle
 * rend telle sourate », mais que les bornes sont bien **les horaires du jour** et
 * non des heures d'horloge : c'est tout l'intérêt de la fonctionnalité.
 */
class QuranSuggestionTest {

    private val zone: ZoneId = ZoneId.of("Africa/Algiers")

    /** Un vendredi. Le 14 août 2026 en est un — même mois que la calibration de Skikda. */
    private val friday: LocalDate = LocalDate.of(2026, 8, 14)
    private val thursday: LocalDate = friday.minusDays(1)
    private val saturday: LocalDate = friday.plusDays(1)

    /** Horaires plausibles d'un mois d'août à Skikda, arrondis à la minute. */
    private fun timesOf(date: LocalDate) = DailyPrayerTimes(
        date = date,
        times = mapOf(
            PrayerName.FAJR to at(date, 4, 20),
            PrayerName.SUNRISE to at(date, 5, 55),
            PrayerName.DHUHR to at(date, 12, 55),
            PrayerName.ASR to at(date, 16, 35),
            PrayerName.MAGHRIB to at(date, 19, 40),
            PrayerName.ISHA to at(date, 21, 5),
        ),
    )

    private fun at(date: LocalDate, hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), zone)

    @Test
    fun `le vendredi en journee c'est al-Kahf`() {
        val suggestion = QuranSuggestion.suggest(at(friday, 10, 0), timesOf(friday))
        assertEquals(QuranSuggestion.AL_KAHF, suggestion.surahId)
        assertEquals(Reason.FRIDAY, suggestion.reason)
    }

    @Test
    fun `le vendredi al-Kahf l'emporte sur la regle du matin`() {
        // 5h00 : entre le Fajr et le shurūq, donc Yā-Sīn un autre jour.
        val suggestion = QuranSuggestion.suggest(at(friday, 5, 0), timesOf(friday))
        assertEquals(QuranSuggestion.AL_KAHF, suggestion.surahId)
    }

    @Test
    fun `apres l'Isha c'est al-Mulk`() {
        val suggestion = QuranSuggestion.suggest(at(saturday, 22, 30), timesOf(saturday))
        assertEquals(QuranSuggestion.AL_MULK, suggestion.surahId)
        assertEquals(Reason.BEFORE_SLEEP, suggestion.reason)
    }

    @Test
    fun `apres minuit et avant le Fajr c'est encore al-Mulk — on est apres l'Isha de la veille`() {
        val suggestion = QuranSuggestion.suggest(at(saturday, 2, 0), timesOf(saturday))
        assertEquals(QuranSuggestion.AL_MULK, suggestion.surahId)
        assertEquals(Reason.BEFORE_SLEEP, suggestion.reason)
    }

    /**
     * Le cas qui justifie de borner sur les horaires : la nuit du vendredi
     * commence au Maghrib du **jeudi**. À 23h le jeudi on est déjà dans la nuit
     * du vendredi, et pourtant c'est al-Mulk qui doit sortir — al-Kahf est la
     * sourate de la *journée*, elle prend le relais au Fajr.
     */
    @Test
    fun `le jeudi soir c'est al-Mulk, pas encore al-Kahf`() {
        val suggestion = QuranSuggestion.suggest(at(thursday, 23, 0), timesOf(thursday))
        assertEquals(QuranSuggestion.AL_MULK, suggestion.surahId)
    }

    @Test
    fun `le vendredi avant le Fajr c'est encore al-Mulk`() {
        val suggestion = QuranSuggestion.suggest(at(friday, 3, 0), timesOf(friday))
        assertEquals(QuranSuggestion.AL_MULK, suggestion.surahId)
        assertNotEquals(Reason.FRIDAY, suggestion.reason)
    }

    @Test
    fun `le vendredi apres le Maghrib al-Kahf a passe la main`() {
        val suggestion = QuranSuggestion.suggest(at(friday, 20, 0), timesOf(friday))
        assertEquals(QuranSuggestion.AL_WAQIA, suggestion.surahId)
        assertEquals(Reason.EVENING, suggestion.reason)
    }

    @Test
    fun `entre le Fajr et le shuruq c'est Ya-Sin`() {
        val suggestion = QuranSuggestion.suggest(at(saturday, 5, 0), timesOf(saturday))
        assertEquals(QuranSuggestion.YA_SIN, suggestion.surahId)
        assertEquals(Reason.MORNING, suggestion.reason)
    }

    @Test
    fun `entre le Maghrib et l'Isha c'est al-Waqia`() {
        val suggestion = QuranSuggestion.suggest(at(saturday, 20, 0), timesOf(saturday))
        assertEquals(QuranSuggestion.AL_WAQIA, suggestion.surahId)
    }

    @Test
    fun `le reste de la journee c'est ar-Rahman`() {
        val suggestion = QuranSuggestion.suggest(at(saturday, 14, 0), timesOf(saturday))
        assertEquals(QuranSuggestion.AR_RAHMAN, suggestion.surahId)
        assertEquals(Reason.ANYTIME, suggestion.reason)
    }

    /**
     * Les bornes suivent les horaires : en décalant le Maghrib d'une heure, la
     * même heure d'horloge change de réponse. Une règle indexée sur l'horloge
     * échouerait ici.
     */
    @Test
    fun `les bornes suivent les horaires et non l'horloge`() {
        val hiver = DailyPrayerTimes(
            date = saturday,
            times = mapOf(
                PrayerName.FAJR to at(saturday, 6, 15),
                PrayerName.SUNRISE to at(saturday, 7, 45),
                PrayerName.DHUHR to at(saturday, 12, 40),
                PrayerName.ASR to at(saturday, 15, 10),
                PrayerName.MAGHRIB to at(saturday, 17, 20),
                PrayerName.ISHA to at(saturday, 18, 45),
            ),
        )
        // 18h00 : en août c'est encore l'après-midi, en décembre c'est déjà le soir.
        assertEquals(QuranSuggestion.AR_RAHMAN, QuranSuggestion.suggest(at(saturday, 18, 0), timesOf(saturday)).surahId)
        assertEquals(QuranSuggestion.AL_WAQIA, QuranSuggestion.suggest(at(saturday, 18, 0), hiver).surahId)
    }

    @Test
    fun `chaque raison a sa sourate, et elles sont toutes distinctes`() {
        val surahs = setOf(
            QuranSuggestion.AL_KAHF,
            QuranSuggestion.AL_MULK,
            QuranSuggestion.YA_SIN,
            QuranSuggestion.AL_WAQIA,
            QuranSuggestion.AR_RAHMAN,
        )
        assertEquals(Reason.entries.size, surahs.size)
    }
}
