package com.mohamed.miqaat.domain

import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.mohamed.miqaat.domain.model.DailyPrayerTimes
import com.mohamed.miqaat.domain.model.MethodOption
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.domain.model.PrayerTimeAdjustments
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Calcule les horaires de prière d'un jour donné, entièrement hors ligne,
 * via la librairie Adhan (calcul astronomique local, aucun appel réseau).
 *
 * Classe pure JVM : aucune dépendance Android, donc testable en test unitaire rapide.
 */
class PrayerTimesCalculator {

    fun calculate(
        latitude: Double,
        longitude: Double,
        date: LocalDate,
        zone: ZoneId,
        method: MethodOption = MethodOption.MUSLIM_WORLD_LEAGUE,
        madhab: Madhab = Madhab.SHAFI,
        adjustments: PrayerTimeAdjustments = PrayerTimeAdjustments(),
    ): DailyPrayerTimes {
        val coordinates = Coordinates(latitude, longitude)
        val day = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        // `prayerAdjustments` est le réglage de l'utilisateur, distinct du
        // `methodAdjustments` de la méthode : Adhan additionne les deux, donc un
        // ajustement manuel se superpose à la marge officielle sans l'effacer.
        val parameters = method.parameters.copy(
            madhab = madhab,
            prayerAdjustments = adjustments.toAdhan(),
        )
        val prayerTimes = PrayerTimes(coordinates, day, parameters)

        // Adhan renvoie des Instant kotlinx-datetime (UTC) ; on les convertit
        // en ZonedDateTime java.time dans le fuseau local demandé.
        fun kotlinx.datetime.Instant.atLocalZone(): ZonedDateTime =
            Instant.ofEpochMilli(toEpochMilliseconds()).atZone(zone)

        return DailyPrayerTimes(
            date = date,
            times = mapOf(
                PrayerName.FAJR to prayerTimes.fajr.atLocalZone(),
                PrayerName.SUNRISE to prayerTimes.sunrise.atLocalZone(),
                PrayerName.DHUHR to prayerTimes.dhuhr.atLocalZone(),
                PrayerName.ASR to prayerTimes.asr.atLocalZone(),
                PrayerName.MAGHRIB to prayerTimes.maghrib.atLocalZone(),
                PrayerName.ISHA to prayerTimes.isha.atLocalZone(),
            ),
        )
    }
}

/** Les six moments d'Adhan sont exactement les nôtres, dans le même ordre. */
private fun PrayerTimeAdjustments.toAdhan(): PrayerAdjustments = PrayerAdjustments(
    fajr = this[PrayerName.FAJR],
    sunrise = this[PrayerName.SUNRISE],
    dhuhr = this[PrayerName.DHUHR],
    asr = this[PrayerName.ASR],
    maghrib = this[PrayerName.MAGHRIB],
    isha = this[PrayerName.ISHA],
)
