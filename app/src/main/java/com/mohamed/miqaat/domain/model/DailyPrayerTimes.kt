package com.mohamed.miqaat.domain.model

import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Les horaires d'une journée, déjà convertis dans le fuseau local de l'utilisateur.
 */
data class DailyPrayerTimes(
    val date: LocalDate,
    val times: Map<PrayerName, ZonedDateTime>,
) {
    fun timeOf(prayer: PrayerName): ZonedDateTime = times.getValue(prayer)

    /**
     * La prochaine prière à venir après [now], ou null si toutes les prières
     * du jour sont passées (la prochaine sera alors le Fajr du lendemain).
     * Le shurūq est ignoré : ce n'est pas une prière.
     */
    fun nextPrayer(now: ZonedDateTime): PrayerName? =
        PrayerName.entries.firstOrNull { it.isPrayer && timeOf(it).isAfter(now) }
}
