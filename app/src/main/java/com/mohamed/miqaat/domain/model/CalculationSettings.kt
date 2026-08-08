package com.mohamed.miqaat.domain.model

import com.batoulapps.adhan2.Madhab

/**
 * Réglages qui influencent le calcul des horaires et l'affichage Hijri.
 * Valeurs par défaut : standard international + usage algérien.
 */
data class CalculationSettings(
    /** Dernier choix manuel de méthode ; ignoré tant que [methodAuto] est actif. */
    val method: MethodOption = MethodOption.MUSLIM_WORLD_LEAGUE,
    /** Sélection automatique de la méthode selon le pays de la position. */
    val methodAuto: Boolean = true,
    val madhab: Madhab = Madhab.SHAFI,
    /** Décalage manuel du calendrier Hijri, en jours (Umm al-Qura ≠ rúya locale). */
    val hijriOffsetDays: Int = 0,
    /** Minutes ajoutées manuellement à chaque moment, en plus de celles de la méthode. */
    val adjustments: PrayerTimeAdjustments = PrayerTimeAdjustments(),
) {
    companion object {
        const val HIJRI_OFFSET_MIN = -2
        const val HIJRI_OFFSET_MAX = 2
    }
}
