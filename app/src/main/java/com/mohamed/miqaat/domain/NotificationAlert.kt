package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.NotificationMode

/**
 * L'état sonore du téléphone, réduit à ce qui nous concerne. Sa lecture (mode
 * sonnerie **et** filtre « Ne pas déranger ») reste côté Android ; ici on ne
 * manipule que le résultat, ce qui rend la décision testable en JVM pur.
 */
enum class RingerState { NORMAL, VIBRATE, SILENT }

/**
 * Le flux de sortie. Sa traduction en `AudioAttributes` reste côté Android.
 *
 * - [RINGTONE] : volume de la **sonnerie d'appel**, muet quand le téléphone l'est.
 * - [ALARM] : volume des alarmes, que le mode sonnerie **ne coupe pas** — le seul
 *   moyen de forcer un son sur un téléphone en vibreur ou en silencieux.
 */
enum class AlertStream { RINGTONE, ALARM }

/** Une impulsion, une série courte, ou rien. Jamais indexé sur la durée du son. */
enum class VibrationStyle { NONE, SINGLE, SUSTAINED }

/**
 * Ce qu'il faut faire pour une alerte donnée.
 *
 * [stream] est nullable plutôt que doublé d'un booléen : l'état incohérent
 * « je ne joue pas, mais voici mon flux » devient impossible à construire.
 */
data class AlertDecision(
    val stream: AlertStream?,
    val vibration: VibrationStyle,
) {
    val playsSound: Boolean get() = stream != null

    val isSilent: Boolean get() = stream == null && vibration == VibrationStyle.NONE
}

/**
 * Croise le réglage de l'utilisateur avec l'état du téléphone.
 *
 * | mode ＼ téléphone | NORMAL | VIBRATE | SILENT |
 * |---|---|---|---|
 * | FOLLOW_PHONE   | sonnerie + impulsion | — + série | — + rien |
 * | ALWAYS_SOUND   | sonnerie + impulsion | alarme + impulsion | alarme + impulsion |
 * | ALWAYS_VIBRATE | — + série | — + série | — + série |
 * | SILENT         | — + rien | — + rien | — + rien |
 *
 * Deux choix à assumer :
 * - en `FOLLOW_PHONE` sur un téléphone qui sonne, une **impulsion** accompagne le
 *   son plutôt qu'une série — c'est ce que fait un téléphone qui sonne, et c'est
 *   au plus près de ce que produisait la vibration du canal ;
 * - `ALWAYS_SOUND` n'emprunte le flux « alarme » que lorsqu'il le faut. Sur un
 *   téléphone en mode sonnerie, la sonnerie suffit et respecte le volume que
 *   l'utilisateur a réglé pour elle.
 */
object AlertResolver {

    fun resolve(mode: NotificationMode, ringer: RingerState): AlertDecision = when (mode) {
        NotificationMode.FOLLOW_PHONE -> when (ringer) {
            RingerState.NORMAL -> AlertDecision(AlertStream.RINGTONE, VibrationStyle.SINGLE)
            RingerState.VIBRATE -> AlertDecision(null, VibrationStyle.SUSTAINED)
            RingerState.SILENT -> AlertDecision(null, VibrationStyle.NONE)
        }

        NotificationMode.ALWAYS_SOUND -> when (ringer) {
            RingerState.NORMAL -> AlertDecision(AlertStream.RINGTONE, VibrationStyle.SINGLE)
            RingerState.VIBRATE, RingerState.SILENT ->
                AlertDecision(AlertStream.ALARM, VibrationStyle.SINGLE)
        }

        NotificationMode.ALWAYS_VIBRATE -> AlertDecision(null, VibrationStyle.SUSTAINED)

        NotificationMode.SILENT -> AlertDecision(null, VibrationStyle.NONE)
    }
}
