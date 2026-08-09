package com.mohamed.miqaat.notifications

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.mohamed.miqaat.domain.VibrationStyle

/**
 * La vibration des alertes, désormais émise par l'application et non plus par le
 * canal de notification (D38) — c'est la seule façon d'honorer un mode forcé.
 *
 * ⚠ **Appelé depuis le receiver d'alarme, jamais depuis le service sonore.** Un
 * `VibrationEffect` fini est confié au service système : il se poursuit même
 * quand notre processus meurt. Le déclencher depuis un service qu'on arrête
 * aussitôt le son terminé le couperait au milieu.
 *
 * Les motifs sont **finis et courts**, jamais indexés sur la durée du son : trente
 * secondes de vibration continue pendant l'adhan seraient agressives et videraient
 * la batterie pour rien.
 */
object AlertVibrator {

    private const val TAG = "AlertVibrator"

    /** Motifs `[attente, vibration, attente, vibration…]`, en millisecondes. */
    private val ADHAN_SINGLE = longArrayOf(0, 400)
    private val ADHAN_SUSTAINED = longArrayOf(0, 600, 400, 600, 400, 600)
    private val SHORT_SINGLE = longArrayOf(0, 250)
    private val SHORT_SUSTAINED = longArrayOf(0, 300, 200, 300)

    /**
     * @param long `true` pour l'adhan (motif plus ample), `false` pour un rappel
     *   ou une invocation.
     */
    fun vibrate(context: Context, style: VibrationStyle, long: Boolean) {
        if (style == VibrationStyle.NONE) return
        val vibrator = vibratorOf(context) ?: return
        if (!vibrator.hasVibrator()) return

        val pattern = when {
            long && style == VibrationStyle.SUSTAINED -> ADHAN_SUSTAINED
            long -> ADHAN_SINGLE
            style == VibrationStyle.SUSTAINED -> SHORT_SUSTAINED
            else -> SHORT_SINGLE
        }

        // -1 : on ne répète pas. Une répétition infinie survivrait à notre
        // processus exactement comme le motif fini — mais sans personne pour l'arrêter.
        val effect = VibrationEffect.createWaveform(pattern, -1)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // USAGE_ALARM : sans cet attribut, le système traite la vibration
                // comme celle d'une notification et la supprime dès que
                // l'utilisateur a coupé les vibrations de notification — ce qui
                // viderait de son sens le mode « toujours vibrer ».
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(
                    effect,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
        }.onFailure { Log.w(TAG, "Vibration impossible", it) }
    }

    private fun vibratorOf(context: Context): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }.getOrNull()
}
