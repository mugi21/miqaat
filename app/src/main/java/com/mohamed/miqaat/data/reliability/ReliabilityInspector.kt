package com.mohamed.miqaat.data.reliability

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.mohamed.miqaat.domain.reliability.CheckState
import com.mohamed.miqaat.domain.reliability.ReliabilityCheck
import com.mohamed.miqaat.domain.reliability.ReliabilityStatus
import java.util.concurrent.TimeUnit

/**
 * Répond à la question « pourquoi l'adhan n'arrive-t-il pas ? » en interrogeant
 * les cinq verrous possibles, et sait ouvrir l'écran système qui corrige chacun.
 *
 * Tout ce qui touche Android vit ici ; le verdict, lui, est du domaine
 * ([com.mohamed.miqaat.domain.reliability.ReliabilityVerdict]) et donc testable.
 */
object ReliabilityInspector {

    private const val TAG = "ReliabilityInspector"

    /** Au-delà, une chaîne muette n'est plus une coïncidence : cinq prières par jour. */
    private val DELIVERY_MAX_SILENCE_MS = TimeUnit.HOURS.toMillis(24)

    fun inspect(context: Context): List<ReliabilityStatus> = listOf(
        ReliabilityStatus(ReliabilityCheck.NOTIFICATIONS, notificationsState(context)),
        ReliabilityStatus(ReliabilityCheck.EXACT_ALARMS, exactAlarmsState(context)),
        ReliabilityStatus(ReliabilityCheck.BATTERY, batteryState(context)),
        ReliabilityStatus(ReliabilityCheck.OEM_AUTOSTART, autostartState(context)),
        ReliabilityStatus(ReliabilityCheck.DELIVERY, deliveryState(context)),
    )

    private fun notificationsState(context: Context): CheckState =
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            CheckState.OK
        } else {
            CheckState.ACTION_NEEDED
        }

    /**
     * Sans objet en dehors d'Android 12/12L : avant, aucune permission n'est
     * requise ; à partir d'Android 13, `USE_EXACT_ALARM` est accordée d'office
     * aux applications d'alarme et n'est pas révocable.
     */
    private fun exactAlarmsState(context: Context): CheckState = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> CheckState.NOT_APPLICABLE
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> CheckState.NOT_APPLICABLE
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms() -> CheckState.OK
        else -> CheckState.ACTION_NEEDED
    }

    private fun batteryState(context: Context): CheckState {
        val power = context.getSystemService(PowerManager::class.java)
            ?: return CheckState.UNKNOWN
        return if (power.isIgnoringBatteryOptimizations(context.packageName)) {
            CheckState.OK
        } else {
            CheckState.ACTION_NEEDED
        }
    }

    /**
     * Jamais [CheckState.OK] par nous-mêmes : aucune API ne dit si le démarrage
     * automatique est accordé. Soit l'utilisateur a déclaré s'en être occupé, soit
     * l'état reste [CheckState.UNKNOWN] — qui, par construction, n'alarme personne.
     */
    private fun autostartState(context: Context): CheckState = when {
        !OemAutostart.hasKnownScreen(context) -> CheckState.NOT_APPLICABLE
        ReliabilityLog.oemAcknowledged(context) -> CheckState.OK
        else -> CheckState.UNKNOWN
    }

    /**
     * Le seul détecteur automatique du gel : si l'application est installée depuis
     * plus d'un jour et qu'aucune alerte n'a jamais été délivrée, quelque chose
     * l'empêche de s'exécuter. La condition sur la date d'installation évite
     * d'accuser à tort une application posée il y a dix minutes.
     */
    private fun deliveryState(context: Context): CheckState {
        val installedSince = installedSinceMs(context) ?: return CheckState.UNKNOWN
        if (installedSince < DELIVERY_MAX_SILENCE_MS) return CheckState.UNKNOWN

        val lastFired = ReliabilityLog.lastFiredAt(context) ?: return CheckState.ACTION_NEEDED
        val silence = System.currentTimeMillis() - lastFired
        return if (silence <= DELIVERY_MAX_SILENCE_MS) CheckState.OK else CheckState.ACTION_NEEDED
    }

    private fun installedSinceMs(context: Context): Long? = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        System.currentTimeMillis() - info.firstInstallTime
    }.getOrNull()

    /**
     * Ouvre l'écran système qui corrige le contrôle donné.
     * @return false si rien n'a pu être ouvert — l'écran affiche alors ses
     *   instructions écrites.
     */
    fun fix(context: Context, check: ReliabilityCheck): Boolean = when (check) {
        ReliabilityCheck.NOTIFICATIONS -> context.launch(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )

        ReliabilityCheck.EXACT_ALARMS ->
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context.launch(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.fromParts("package", context.packageName, null)),
            )

        ReliabilityCheck.BATTERY -> context.launch(
            @Suppress("BatteryLife")
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.fromParts("package", context.packageName, null)),
        ) || context.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))

        // Les deux mènent au même endroit : le gel constaté vient de la surcouche.
        ReliabilityCheck.OEM_AUTOSTART,
        ReliabilityCheck.DELIVERY,
        -> OemAutostart.open(context)
    }

    private fun Context.launch(intent: Intent): Boolean =
        runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Log.w(TAG, "Écran système injoignable : ${intent.action}", it) }
            .isSuccess
}
