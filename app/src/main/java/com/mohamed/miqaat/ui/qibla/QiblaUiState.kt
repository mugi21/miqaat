package com.mohamed.miqaat.ui.qibla

import com.mohamed.miqaat.data.compass.CompassAccuracy

/**
 * État de l'écran Qibla.
 *
 * [qiblaBearing] est stable (il ne dépend que de la position) ; [deviceHeading]
 * suit le capteur. La rotation à appliquer à la marque de la Kaaba sur le
 * cadran vaut `qiblaBearing − deviceHeading`.
 */
data class QiblaUiState(
    val cityName: String = "",
    /** Azimut de la Qibla depuis le nord géographique, en degrés. */
    val qiblaBearing: Double = 0.0,
    /** Distance jusqu'à la Kaaba, en kilomètres. */
    val distanceKm: Double = 0.0,
    /** Cap courant de l'appareil ; null tant qu'aucune mesure n'est arrivée. */
    val deviceHeading: Double? = null,
    val accuracy: CompassAccuracy = CompassAccuracy.UNRELIABLE,
    /** Faux si l'appareil n'a pas de magnétomètre : on n'affiche que l'angle. */
    val compassAvailable: Boolean = true,
    val isAligned: Boolean = false,
)
