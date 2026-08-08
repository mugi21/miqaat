package com.mohamed.miqaat.ui.calendar

import com.mohamed.miqaat.ui.home.PrayerRowUi
import java.time.LocalDate

/**
 * État immuable de l'écran calendrier. Tout y est déjà formaté :
 * le composable ne connaît ni `LocalDate` ni fuseau, sauf pour renvoyer
 * la date cliquée au ViewModel.
 */
data class CalendarUiState(
    val hijriMonthLabel: String = "",
    val gregorianMonthLabel: String = "",
    /** En-tête des sept colonnes, dans l'ordre de la locale (l'arabe commence au samedi). */
    val weekdayLabels: List<String> = emptyList(),
    /** Multiple de 7 ; `null` = case vide avant le 1er ou après le dernier jour. */
    val cells: List<CalendarDayUi?> = emptyList(),
    val selectedDay: SelectedDayUi? = null,
    /** Faux quand l'utilisateur s'est éloigné d'aujourd'hui : le bouton de retour apparaît. */
    val isTodaySelected: Boolean = true,
)

/** Une case de la grille. */
data class CalendarDayUi(
    val date: LocalDate,
    val gregorianDay: String,
    val hijriDay: String,
    val isSelected: Boolean,
    val isToday: Boolean,
    val isRamadan: Boolean,
)

/** Le jour ouvert sous la grille : ses dates, ses six moments, et le jeûne s'il y a lieu. */
data class SelectedDayUi(
    val hijriDate: String,
    val gregorianDate: String,
    val prayers: List<PrayerRowUi>,
    /** Non nul uniquement si le jour tombe en Ramadan. */
    val ramadan: RamadanUi? = null,
)

/**
 * Les bornes du jeûne. La durée (de l'imsāk à l'iftār) reste en heures et minutes
 * brutes : son unité est un texte traduit, donc résolu côté composable.
 */
data class RamadanUi(
    val imsak: String,
    val iftar: String,
    val fastingHours: Int,
    val fastingMinutes: Int,
)
