package com.mohamed.miqaat.ui.home

import com.mohamed.miqaat.domain.model.PrayerName

/**
 * État immuable consommé par HomeScreen. Le ViewModel est le seul à le produire.
 * [next] est null uniquement avant la première émission du ticker.
 */
data class HomeUiState(
    val cityName: String = "",
    val gregorianDate: String = "",
    val hijriDate: String = "",
    val prayers: List<PrayerRowUi> = emptyList(),
    val next: NextPrayerUi? = null,
)

/** La prochaine prière mise en avant dans le héros, avec son compte à rebours. */
data class NextPrayerUi(
    val prayer: PrayerName,
    val time: String,
    val countdown: String,
    val isTomorrowFajr: Boolean,
)

/** Une ligne de la liste : un moment de la journée et son heure déjà formatée. */
data class PrayerRowUi(
    val prayer: PrayerName,
    val time: String,
    val isNext: Boolean,
    val isPast: Boolean,
)
