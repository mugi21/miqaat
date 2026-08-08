package com.mohamed.miqaat.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.miqaat.data.location.GeoLocation
import com.mohamed.miqaat.data.location.LocationRepository
import com.mohamed.miqaat.data.settings.SettingsRepository
import com.mohamed.miqaat.domain.HijriFormatter
import com.mohamed.miqaat.domain.NextPrayerResolver
import com.mohamed.miqaat.domain.PrayerTimesCalculator
import com.mohamed.miqaat.domain.effectiveMethod
import com.mohamed.miqaat.domain.formatCountdown
import com.mohamed.miqaat.domain.model.CalculationSettings
import com.mohamed.miqaat.domain.model.DailyPrayerTimes
import com.mohamed.miqaat.domain.model.PrayerName
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val calculator: PrayerTimesCalculator = PrayerTimesCalculator(),
    private val nextPrayerResolver: NextPrayerResolver = NextPrayerResolver(),
    private val hijriFormatter: HijriFormatter = HijriFormatter(),
) : ViewModel() {

    // Locale ar-DZ : noms de jours/mois en arabe avec chiffres occidentaux (usage algérien)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    private val dateFormatter =
        DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.forLanguageTag("ar-DZ"))

    // Horaires mémorisés du jour courant et du lendemain : le calcul astronomique
    // ne doit pas tourner à chaque tick, seulement si la date, la position ou
    // les réglages changent.
    private var cachedKey: Triple<LocalDate, GeoLocation, CalculationSettings>? = null
    private lateinit var today: DailyPrayerTimes
    private lateinit var tomorrow: DailyPrayerTimes

    /**
     * Ticker : une émission par seconde tant que l'écran est visible.
     * WhileSubscribed arrête la boucle quand l'app passe en arrière-plan
     * (aucun tick inutile) et la relance avec un état frais au retour.
     */
    val uiState: StateFlow<HomeUiState> =
        flow {
            while (true) {
                emit(buildUiState())
                // Aligné sur la seconde pour que l'affichage change au bon moment.
                delay(1_000L - System.currentTimeMillis() % 1_000L)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun buildUiState(): HomeUiState {
        val location = locationRepository.currentLocation()
        val settings = settingsRepository.current()
        val now = ZonedDateTime.now(location.zoneId)
        val date = now.toLocalDate()

        if (cachedKey != Triple(date, location, settings)) {
            // Méthode effective : auto (selon le pays de la position) ou choix manuel.
            val method = settings.effectiveMethod(location.countryCode)
            today = calculator.calculate(
                location.latitude, location.longitude, date, location.zoneId,
                method, settings.madhab, settings.adjustments,
            )
            tomorrow = calculator.calculate(
                location.latitude, location.longitude, date.plusDays(1), location.zoneId,
                method, settings.madhab, settings.adjustments,
            )
            cachedKey = Triple(date, location, settings)
        }

        val next = nextPrayerResolver.resolve(now, today, tomorrow)

        return HomeUiState(
            cityName = location.cityName,
            gregorianDate = dateFormatter.format(now),
            hijriDate = hijriFormatter.format(date, settings.hijriOffsetDays),
            prayers = PrayerName.entries.map { prayer ->
                val time = today.timeOf(prayer)
                PrayerRowUi(
                    prayer = prayer,
                    time = timeFormatter.format(time),
                    isNext = !next.isTomorrow && prayer == next.prayer,
                    isPast = !time.isAfter(now),
                )
            },
            next = NextPrayerUi(
                prayer = next.prayer,
                time = timeFormatter.format(next.time),
                countdown = formatCountdown(Duration.between(now, next.time)),
                isTomorrowFajr = next.isTomorrow,
            ),
        )
    }
}
