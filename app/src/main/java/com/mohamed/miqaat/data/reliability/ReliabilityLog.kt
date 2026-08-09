package com.mohamed.miqaat.data.reliability

import android.content.Context
import androidx.core.content.edit
import java.util.concurrent.TimeUnit

/**
 * Le fil d'Ariane de la chaîne d'alarmes : quand une alerte a-t-elle été
 * réellement délivrée pour la dernière fois ?
 *
 * Sans cette trace, impossible de distinguer « la surcouche gèle l'application »
 * de « la chaîne s'est rompue » — le symptôme est le même, le remède non. C'est
 * elle qui alimente le contrôle `DELIVERY` de l'écran de fiabilité, seul
 * détecteur automatique du gel.
 *
 * `SharedPreferences` et non DataStore, pour la même raison qu'[AppLocale][com.mohamed.miqaat.data.settings.AppLocale] :
 * l'écriture a lieu dans un `BroadcastReceiver`, où l'on ne veut ni `runBlocking`
 * ni coroutine dont la portée mourrait avec `onReceive`.
 */
object ReliabilityLog {

    private const val PREFS = "reliability"
    private const val KEY_LAST_FIRED = "last_fired_at"
    private const val KEY_BANNER_DISMISSED_UNTIL = "banner_dismissed_until"
    private const val KEY_OEM_ACKNOWLEDGED = "oem_acknowledged"

    /** Combien de temps la bannière d'accueil se tait après un « plus tard ». */
    const val SNOOZE_DAYS = 14L

    /** Appelé à chaque déclenchement du receiver d'alarme, avant tout affichage. */
    fun recordFired(context: Context) {
        prefs(context).edit { putLong(KEY_LAST_FIRED, System.currentTimeMillis()) }
    }

    /** `null` = aucune alerte n'a jamais été délivrée depuis l'installation. */
    fun lastFiredAt(context: Context): Long? =
        prefs(context).getLong(KEY_LAST_FIRED, NEVER).takeIf { it != NEVER }

    fun dismissedUntil(context: Context): Long =
        prefs(context).getLong(KEY_BANNER_DISMISSED_UNTIL, 0L)

    fun snoozeBanner(context: Context, days: Long = SNOOZE_DAYS) {
        val until = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days)
        prefs(context).edit { putLong(KEY_BANNER_DISMISSED_UNTIL, until) }
    }

    /**
     * Le démarrage automatique d'une surcouche n'est **pas lisible** par une
     * application tierce : l'utilisateur déclare lui-même l'avoir réglé, et
     * l'écran cesse alors de le lui redemander.
     */
    fun oemAcknowledged(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OEM_ACKNOWLEDGED, false)

    fun setOemAcknowledged(context: Context, acknowledged: Boolean) {
        prefs(context).edit { putBoolean(KEY_OEM_ACKNOWLEDGED, acknowledged) }
    }

    private const val NEVER = 0L

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
