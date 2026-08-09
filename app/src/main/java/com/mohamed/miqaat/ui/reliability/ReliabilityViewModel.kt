package com.mohamed.miqaat.ui.reliability

import android.content.Context
import androidx.lifecycle.ViewModel
import com.mohamed.miqaat.data.reliability.OemAutostart
import com.mohamed.miqaat.data.reliability.ReliabilityInspector
import com.mohamed.miqaat.data.reliability.ReliabilityLog
import com.mohamed.miqaat.domain.PrayerEventKind
import com.mohamed.miqaat.domain.ScheduledEvent
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.domain.reliability.ReliabilityCheck
import com.mohamed.miqaat.notifications.AlertVibrator
import com.mohamed.miqaat.notifications.PrayerAlarmScheduler
import com.mohamed.miqaat.notifications.PrayerNotifications
import com.mohamed.miqaat.notifications.RingerReader
import com.mohamed.miqaat.domain.AlertResolver
import com.mohamed.miqaat.miqaatApp
import androidx.core.app.NotificationManagerCompat
import com.mohamed.miqaat.notifications.AlertSoundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Diagnostic des cinq verrous, et actions pour les lever.
 *
 * Le contexte est celui de l'application (jamais l'activité) : le ViewModel
 * survit aux rotations, une référence d'activité fuirait. Les intents système
 * partent donc avec `FLAG_ACTIVITY_NEW_TASK`, posé par l'inspecteur.
 */
class ReliabilityViewModel(private val context: Context) : ViewModel() {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    private val _state = MutableStateFlow(ReliabilityUiState())
    val state: StateFlow<ReliabilityUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /**
     * À rappeler au retour d'un écran système : l'utilisateur vient peut-être
     * d'accorder ce qui manquait, l'écran doit le refléter sans être quitté.
     */
    fun refresh() {
        _state.update {
            ReliabilityUiState(
                statuses = ReliabilityInspector.inspect(context),
                nextAlertTime = nextAlertTime(),
                lastDeliveredAt = ReliabilityLog.lastFiredAt(context),
                hasOemScreen = OemAutostart.hasKnownScreen(context),
                oemAcknowledged = ReliabilityLog.oemAcknowledged(context),
            )
        }
    }

    /** @return false si aucun écran système n'a pu être ouvert. */
    fun fix(check: ReliabilityCheck): Boolean = ReliabilityInspector.fix(context, check)

    fun setOemAcknowledged(acknowledged: Boolean) {
        ReliabilityLog.setOemAcknowledged(context, acknowledged)
        refresh()
    }

    /**
     * Déclenche une vraie alerte de rappel, tout de suite : c'est le seul moyen
     * d'éprouver le mode d'alerte (son, vibration, flux) sans attendre une prière.
     */
    fun sendTestNotification() {
        val prayer = PrayerName.FAJR
        val kind = PrayerEventKind.REMINDER
        NotificationManagerCompat.from(context).notify(
            PrayerNotifications.idOf(prayer, kind),
            PrayerNotifications.build(context, prayer, kind),
        )
        val decision = AlertResolver.resolve(
            context.miqaatApp.settingsRepository.currentNotification().mode,
            RingerReader.read(context),
        )
        AlertVibrator.vibrate(context, decision.vibration, long = false)
        decision.stream?.let { AlertSoundService.start(context, prayer, kind, it) }
    }

    private fun nextAlertTime(): String? = runCatching {
        val next: ScheduledEvent = PrayerAlarmScheduler(context).nextEvent()
        next.time.format(timeFormatter)
    }.getOrNull()
}
