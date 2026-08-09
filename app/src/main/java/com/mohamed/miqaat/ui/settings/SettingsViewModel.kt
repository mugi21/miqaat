package com.mohamed.miqaat.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan2.Madhab
import com.mohamed.miqaat.data.location.LocationRepository
import com.mohamed.miqaat.data.settings.SettingsRepository
import com.mohamed.miqaat.domain.AutoMethodResolver
import com.mohamed.miqaat.domain.model.CalculationSettings
import com.mohamed.miqaat.domain.model.MethodOption
import com.mohamed.miqaat.domain.model.NotificationMode
import com.mohamed.miqaat.domain.model.NotificationSettings
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.domain.model.ReminderSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    locationRepository: LocationRepository,
    /**
     * Un réglage modifié change soit les horaires, soit les évènements de la
     * chaîne (le rappel) : dans les deux cas, il faut replanifier l'alarme.
     */
    private val onSettingsChanged: () -> Unit,
) : ViewModel() {

    val settings: StateFlow<CalculationSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.current())

    val reminder: StateFlow<ReminderSettings> = repository.reminderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.currentReminder())

    val notification: StateFlow<NotificationSettings> = repository.notificationFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            repository.currentNotification(),
        )

    /**
     * Méthode que le mode automatique appliquerait ici — pour l'afficher
     * (« تلقائي — الجزائر »). Figée à la création du ViewModel : la position
     * ne bouge pas pendant qu'on est sur l'écran de réglages.
     */
    val autoMethod: MethodOption =
        AutoMethodResolver.resolve(locationRepository.currentLocation().countryCode)

    fun setMethod(method: MethodOption) = update { repository.setMethod(method) }

    fun setMethodAuto() = update { repository.setMethodAuto() }

    fun setMadhab(madhab: Madhab) = update { repository.setMadhab(madhab) }

    fun setHijriOffset(days: Int) = update { repository.setHijriOffset(days) }

    fun setPrayerAdjustment(prayer: PrayerName, minutes: Int) =
        update { repository.setPrayerAdjustment(prayer, minutes) }

    fun clearPrayerAdjustments() = update { repository.clearPrayerAdjustments() }

    fun setReminderEnabled(enabled: Boolean) = update { repository.setReminderEnabled(enabled) }

    fun setReminderLead(minutes: Int) = update { repository.setReminderLeadMinutes(minutes) }

    /**
     * Seul réglage qui ne change aucun horaire — il passe quand même par [update],
     * donc par une replanification inutile mais inoffensive : un chemin unique
     * vaut mieux qu'un cas particulier de plus.
     */
    fun setNotificationMode(mode: NotificationMode) =
        update { repository.setNotificationMode(mode) }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            onSettingsChanged()
        }
    }
}
