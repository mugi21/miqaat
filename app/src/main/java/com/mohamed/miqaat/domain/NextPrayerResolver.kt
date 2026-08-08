package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.DailyPrayerTimes
import com.mohamed.miqaat.domain.model.PrayerName
import java.time.ZonedDateTime

/**
 * La prochaine prière avec son instant exact, y compris quand elle tombe demain.
 * L'instant précis servira aussi à planifier les alarmes de notification.
 */
data class NextPrayerInstant(
    val prayer: PrayerName,
    val time: ZonedDateTime,
    val isTomorrow: Boolean,
)

/**
 * Résout la prochaine prière à partir des horaires du jour et du lendemain :
 * après l'Isha, la prochaine prière est le Fajr de demain (jamais null).
 */
class NextPrayerResolver {

    fun resolve(
        now: ZonedDateTime,
        today: DailyPrayerTimes,
        tomorrow: DailyPrayerTimes,
    ): NextPrayerInstant {
        val next = today.nextPrayer(now)
        return if (next != null) {
            NextPrayerInstant(next, today.timeOf(next), isTomorrow = false)
        } else {
            NextPrayerInstant(PrayerName.FAJR, tomorrow.timeOf(PrayerName.FAJR), isTomorrow = true)
        }
    }
}
