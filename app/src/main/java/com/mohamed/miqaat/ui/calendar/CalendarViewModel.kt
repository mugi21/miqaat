package com.mohamed.miqaat.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.miqaat.data.location.GeoLocation
import com.mohamed.miqaat.data.location.LocationRepository
import com.mohamed.miqaat.data.settings.SettingsRepository
import com.mohamed.miqaat.domain.HijriFormatter
import com.mohamed.miqaat.domain.PrayerTimesCalculator
import com.mohamed.miqaat.domain.effectiveMethod
import com.mohamed.miqaat.domain.hijriDayOfMonth
import com.mohamed.miqaat.domain.isRamadan
import com.mohamed.miqaat.domain.model.CalculationSettings
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.domain.monthGridCells
import com.mohamed.miqaat.domain.ramadanTimesOf
import com.mohamed.miqaat.domain.weekdaysFrom
import com.mohamed.miqaat.ui.home.PrayerRowUi
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Écran calendrier : une grille mensuelle, un jour sélectionné, ses horaires.
 *
 * Pas de ticker ici — contrairement à l'accueil, rien ne défile : l'état ne se
 * reconstruit qu'au changement de mois ou de jour sélectionné. Les horaires ne
 * sont calculés que pour le jour ouvert, jamais pour les 42 cases.
 */
class CalendarViewModel(
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val calculator: PrayerTimesCalculator = PrayerTimesCalculator(),
    private val hijriFormatter: HijriFormatter = HijriFormatter(),
) : ViewModel() {

    // Même locale que l'accueil : noms arabes, chiffres occidentaux (usage algérien).
    private val locale = Locale.forLanguageTag("ar-DZ")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    private val dayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", locale)
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    private val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek

    private val today: LocalDate get() = LocalDate.now(locationRepository.currentLocation().zoneId)

    private val selectedDate = MutableStateFlow(today)
    private val displayedMonth = MutableStateFlow(YearMonth.from(selectedDate.value))

    val uiState: StateFlow<CalendarUiState> =
        combine(selectedDate, displayedMonth, ::buildUiState)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                // Valeur initiale calculée : l'écran s'ouvre déjà rempli, sans clignotement.
                buildUiState(selectedDate.value, displayedMonth.value),
            )

    /** Un clic sur une case : le mois affiché suit, au cas où la date vienne d'ailleurs. */
    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        displayedMonth.value = YearMonth.from(date)
    }

    fun showPreviousMonth() = displayedMonth.update { it.minusMonths(1) }

    fun showNextMonth() = displayedMonth.update { it.plusMonths(1) }

    fun backToToday() = selectDate(today)

    private fun buildUiState(selected: LocalDate, month: YearMonth): CalendarUiState {
        val location = locationRepository.currentLocation()
        val settings = settingsRepository.current()
        val offset = settings.hijriOffsetDays
        val currentDay = today

        val cells = monthGridCells(month, firstDayOfWeek).map { date ->
            date?.let {
                val hijri = hijriFormatter.toHijri(it, offset)
                CalendarDayUi(
                    date = it,
                    gregorianDay = it.dayOfMonth.toString(),
                    hijriDay = hijri.hijriDayOfMonth.toString(),
                    isSelected = it == selected,
                    isToday = it == currentDay,
                    isRamadan = hijri.isRamadan,
                )
            }
        }

        // Un mois grégorien chevauche deux mois hégiriens : on nomme les deux
        // quand ils diffèrent, plutôt que d'en choisir un arbitrairement.
        val firstHijri = hijriFormatter.formatMonthYear(month.atDay(1), offset)
        val lastHijri = hijriFormatter.formatMonthYear(month.atEndOfMonth(), offset)

        return CalendarUiState(
            hijriMonthLabel = if (firstHijri == lastHijri) firstHijri else "$firstHijri – $lastHijri",
            gregorianMonthLabel = monthFormatter.format(month.atDay(1)),
            weekdayLabels = weekdaysFrom(firstDayOfWeek).map {
                it.getDisplayName(TextStyle.SHORT, locale)
            },
            cells = cells,
            selectedDay = buildSelectedDay(selected, settings, location, currentDay),
            isTodaySelected = selected == currentDay,
        )
    }

    private fun buildSelectedDay(
        selected: LocalDate,
        settings: CalculationSettings,
        location: GeoLocation,
        currentDay: LocalDate,
    ): SelectedDayUi {
        val times = calculator.calculate(
            location.latitude, location.longitude, selected, location.zoneId,
            settings.effectiveMethod(location.countryCode), settings.madhab, settings.adjustments,
        )
        // « Passé » n'a de sens que pour aujourd'hui : un autre jour s'affiche à plat.
        val now: ZonedDateTime? =
            if (selected == currentDay) ZonedDateTime.now(location.zoneId) else null

        val hijri = hijriFormatter.toHijri(selected, settings.hijriOffsetDays)
        return SelectedDayUi(
            hijriDate = hijriFormatter.format(selected, settings.hijriOffsetDays),
            gregorianDate = dayFormatter.format(selected),
            prayers = PrayerName.entries.map { prayer ->
                val time = times.timeOf(prayer)
                PrayerRowUi(
                    prayer = prayer,
                    time = timeFormatter.format(time),
                    isNext = false,
                    isPast = now != null && !time.isAfter(now),
                )
            },
            ramadan = if (hijri.isRamadan) {
                val ramadan = ramadanTimesOf(times)
                // toMinutesPart() n'existe qu'à partir de l'API 31 : on reste en modulo.
                val minutes = ramadan.fastingDuration.toMinutes()
                RamadanUi(
                    imsak = timeFormatter.format(ramadan.imsak),
                    iftar = timeFormatter.format(ramadan.iftar),
                    fastingHours = (minutes / 60).toInt(),
                    fastingMinutes = (minutes % 60).toInt(),
                )
            } else {
                null
            },
        )
    }
}
