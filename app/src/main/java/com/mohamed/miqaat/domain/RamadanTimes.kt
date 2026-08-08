package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.DailyPrayerTimes
import com.mohamed.miqaat.domain.model.PrayerName
import java.time.Duration
import java.time.ZonedDateTime

/**
 * Marge de précaution entre l'imsāk et le Fajr, en minutes.
 *
 * Le jeûne commence à l'aube véritable (le Fajr) ; l'imsāk est l'usage répandu
 * de s'arrêter un peu avant, par précaution. Dix minutes est la valeur retenue
 * par Umm al-Qura et par la plupart des calendriers du Maghreb.
 */
const val IMSAK_MINUTES_BEFORE_FAJR = 10L

/**
 * Les deux bornes du jour de jeûne, dérivées des horaires du jour :
 * l'imsāk (avant le Fajr) et l'iftār (à l'appel du Maghrib).
 */
data class RamadanTimes(
    val imsak: ZonedDateTime,
    val iftar: ZonedDateTime,
) {
    /**
     * Durée d'abstention effective : de l'imsāk à l'iftār, donc exactement
     * l'intervalle entre les deux heures affichées à l'utilisateur.
     */
    val fastingDuration: Duration get() = Duration.between(imsak, iftar)
}

/** Aucun calcul astronomique supplémentaire : tout se déduit du Fajr et du Maghrib. */
fun ramadanTimesOf(day: DailyPrayerTimes): RamadanTimes = RamadanTimes(
    imsak = day.timeOf(PrayerName.FAJR).minusMinutes(IMSAK_MINUTES_BEFORE_FAJR),
    iftar = day.timeOf(PrayerName.MAGHRIB),
)
