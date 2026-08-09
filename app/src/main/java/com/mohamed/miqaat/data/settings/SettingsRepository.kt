package com.mohamed.miqaat.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.batoulapps.adhan2.Madhab
import com.mohamed.miqaat.domain.model.CalculationSettings
import com.mohamed.miqaat.domain.model.MethodOption
import com.mohamed.miqaat.domain.model.NotificationMode
import com.mohamed.miqaat.domain.model.NotificationSettings
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.domain.model.PrayerTimeAdjustments
import com.mohamed.miqaat.domain.model.ReminderSettings
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

/**
 * Réglages persistés (DataStore). Même patron que la position : un instantané
 * mémoire pour les lecteurs synchrones (tick de l'écran, receivers d'alarme),
 * un Flow pour l'écran de réglages.
 */
class SettingsRepository(context: Context) {

    private val dataStore = context.settingsDataStore

    @Volatile
    private var memory: CalculationSettings? = null

    @Volatile
    private var reminderMemory: ReminderSettings? = null

    @Volatile
    private var notificationMemory: NotificationSettings? = null

    val settingsFlow: Flow<CalculationSettings> =
        dataStore.data.map { it.toSettings().also { s -> memory = s } }

    /** Le rappel avant l'adhan : un flux à part, il n'influence aucun horaire. */
    val reminderFlow: Flow<ReminderSettings> =
        dataStore.data.map { it.toReminder().also { r -> reminderMemory = r } }

    /** Le mode d'alerte : n'influence ni les horaires ni les évènements, seulement leur rendu. */
    val notificationFlow: Flow<NotificationSettings> =
        dataStore.data.map { it.toNotification().also { n -> notificationMemory = n } }

    fun current(): CalculationSettings =
        memory ?: runBlocking { dataStore.data.first().also(::cache).toSettings() }

    fun currentReminder(): ReminderSettings =
        reminderMemory ?: runBlocking { dataStore.data.first().also(::cache).toReminder() }

    fun currentNotification(): NotificationSettings =
        notificationMemory ?: runBlocking { dataStore.data.first().also(::cache).toNotification() }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_REMINDER_ENABLED] = enabled }.also(::cache)
    }

    suspend fun setReminderLeadMinutes(minutes: Int) {
        val valid = ReminderSettings.sanitizeLead(minutes)
        dataStore.edit { it[KEY_REMINDER_LEAD] = valid }.also(::cache)
    }

    suspend fun setNotificationMode(mode: NotificationMode) {
        dataStore.edit { it[KEY_NOTIFICATION_MODE] = mode.name }.also(::cache)
    }

    /** Choix manuel d'une méthode : désactive la sélection automatique, atomiquement. */
    suspend fun setMethod(method: MethodOption) {
        dataStore.edit {
            it[KEY_METHOD] = method.name
            it[KEY_METHOD_AUTO] = false
        }.also(::cache)
    }

    /** Réactive la sélection automatique (le dernier choix manuel est conservé). */
    suspend fun setMethodAuto() {
        dataStore.edit { it[KEY_METHOD_AUTO] = true }.also(::cache)
    }

    suspend fun setMadhab(madhab: Madhab) {
        dataStore.edit { it[KEY_MADHAB] = madhab.name }.also(::cache)
    }

    /** Ajustement manuel d'un moment ; 0 efface la clé plutôt que d'écrire un zéro. */
    suspend fun setPrayerAdjustment(prayer: PrayerName, minutes: Int) {
        val bounded = PrayerTimeAdjustments.sanitize(minutes)
        dataStore.edit { preferences ->
            if (bounded == 0) {
                preferences.remove(adjustmentKey(prayer))
            } else {
                preferences[adjustmentKey(prayer)] = bounded
            }
        }.also(::cache)
    }

    suspend fun clearPrayerAdjustments() {
        dataStore.edit { preferences ->
            PrayerName.entries.forEach { preferences.remove(adjustmentKey(it)) }
        }.also(::cache)
    }

    suspend fun setHijriOffset(days: Int) {
        val bounded = days.coerceIn(
            CalculationSettings.HIJRI_OFFSET_MIN,
            CalculationSettings.HIJRI_OFFSET_MAX,
        )
        dataStore.edit { it[KEY_HIJRI_OFFSET] = bounded }.also(::cache)
    }

    /**
     * Un seul jeu de préférences : chaque écriture rafraîchit **tous** les
     * instantanés. En oublier un le rendrait périmé pour les lecteurs synchrones
     * — c'est-à-dire, précisément, pour le receiver d'alarme, qui tourne souvent
     * dans un processus fraîchement démarré où aucun Flow n'a jamais émis.
     */
    private fun cache(preferences: Preferences) {
        memory = preferences.toSettings()
        reminderMemory = preferences.toReminder()
        notificationMemory = preferences.toNotification()
    }

    private fun Preferences.toSettings(): CalculationSettings {
        val defaults = CalculationSettings()
        return CalculationSettings(
            method = this[KEY_METHOD]
                ?.let { name -> MethodOption.entries.firstOrNull { it.name == name } }
                ?: defaults.method,
            methodAuto = this[KEY_METHOD_AUTO] ?: defaults.methodAuto,
            madhab = this[KEY_MADHAB]
                ?.let { name -> Madhab.entries.firstOrNull { it.name == name } }
                ?: defaults.madhab,
            hijriOffsetDays = this[KEY_HIJRI_OFFSET] ?: defaults.hijriOffsetDays,
            adjustments = PrayerTimeAdjustments.of(
                PrayerName.entries.associateWith { this[adjustmentKey(it)] ?: 0 },
            ),
        )
    }

    private fun Preferences.toReminder(): ReminderSettings {
        val defaults = ReminderSettings()
        return ReminderSettings(
            enabled = this[KEY_REMINDER_ENABLED] ?: defaults.enabled,
            leadMinutes = this[KEY_REMINDER_LEAD]
                ?.let(ReminderSettings::sanitizeLead)
                ?: defaults.leadMinutes,
        )
    }

    private fun Preferences.toNotification(): NotificationSettings =
        NotificationSettings(mode = NotificationMode.parse(this[KEY_NOTIFICATION_MODE]))

    private companion object {
        /** Une clé par moment : `adjust_fajr`, `adjust_sunrise`… */
        fun adjustmentKey(prayer: PrayerName) =
            intPreferencesKey("adjust_${prayer.name.lowercase(Locale.ROOT)}")

        val KEY_METHOD = stringPreferencesKey("calculation_method")
        val KEY_METHOD_AUTO = booleanPreferencesKey("method_auto")
        val KEY_MADHAB = stringPreferencesKey("madhab")
        val KEY_HIJRI_OFFSET = intPreferencesKey("hijri_offset_days")
        val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val KEY_REMINDER_LEAD = intPreferencesKey("reminder_lead_minutes")
        val KEY_NOTIFICATION_MODE = stringPreferencesKey("notification_mode")
    }
}
