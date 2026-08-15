package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.DailyPrayerTimes
import com.mohamed.miqaat.domain.model.PrayerName
import java.time.DayOfWeek
import java.time.ZonedDateTime

/**
 * La sourate du moment.
 *
 * Ce que les autres applications de récitation ne peuvent pas faire : les bornes
 * ne sont pas des heures d'horloge mais **les horaires de prière réels du jour**.
 * C'est ce qui rend la première règle juste — la nuit du vendredi commence au
 * Maghrib du jeudi, donc jeudi soir c'est al-Mulk qui l'emporte, et al-Kahf ne
 * prend le relais qu'au Fajr du vendredi.
 *
 * Objet sans état, sans dépendance Android : entièrement testable en JVM.
 */
object QuranSuggestion {

    const val AL_KAHF = 18
    const val YA_SIN = 36
    const val AR_RAHMAN = 55
    const val AL_WAQIA = 56
    const val AL_MULK = 67

    enum class Reason { FRIDAY, BEFORE_SLEEP, MORNING, EVENING, ANYTIME }

    data class SurahSuggestion(val surahId: Int, val reason: Reason)

    /**
     * @param today les horaires du jour de [now] — c'est l'appelant qui garantit
     *   cette correspondance, comme partout ailleurs dans l'app.
     */
    fun suggest(now: ZonedDateTime, today: DailyPrayerTimes): SurahSuggestion {
        val fajr = today.timeOf(PrayerName.FAJR)
        val sunrise = today.timeOf(PrayerName.SUNRISE)
        val maghrib = today.timeOf(PrayerName.MAGHRIB)
        val isha = today.timeOf(PrayerName.ISHA)

        // 1. Vendredi, du Fajr au Maghrib : la sourate de la journée.
        //    Prioritaire sur la règle du matin — al-Kahf est la sunna du jour.
        if (now.dayOfWeek == DayOfWeek.FRIDAY && now >= fajr && now < maghrib) {
            return SurahSuggestion(AL_KAHF, Reason.FRIDAY)
        }
        // 2. Après l'Isha, et jusqu'au Fajr. Le `now < fajr` couvre les heures
        //    d'après minuit : on est alors après l'Isha de la veille.
        if (now >= isha || now < fajr) {
            return SurahSuggestion(AL_MULK, Reason.BEFORE_SLEEP)
        }
        // 3. Entre le Fajr et le shurūq.
        if (now < sunrise) {
            return SurahSuggestion(YA_SIN, Reason.MORNING)
        }
        // 4. Entre le Maghrib et l'Isha.
        if (now >= maghrib) {
            return SurahSuggestion(AL_WAQIA, Reason.EVENING)
        }
        // 5. Le reste de la journée.
        return SurahSuggestion(AR_RAHMAN, Reason.ANYTIME)
    }
}
