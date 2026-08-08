package com.mohamed.miqaat.domain

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Direction de la Qibla : le cap (azimut) à suivre depuis une position donnée
 * pour faire face à la Kaaba, en suivant le grand cercle terrestre.
 *
 * 100 % local et déterministe (aucune donnée réseau) : c'est de la trigonométrie
 * sphérique. Domaine pur → testable en JVM.
 */
object QiblaCalculator {

    /** Kaaba, La Mecque — coordonnées de référence usuelles. */
    const val KAABA_LATITUDE = 21.4225241
    const val KAABA_LONGITUDE = 39.8261818

    /** Rayon moyen de la Terre, en kilomètres (sphère de référence). */
    private const val EARTH_RADIUS_KM = 6371.0088

    /**
     * Azimut de la Qibla en degrés depuis le **nord géographique** (0 = nord,
     * 90 = est), dans [0, 360[.
     *
     * Formule du cap initial d'un grand cercle :
     * `atan2(sin Δλ, cos φ₁·tan φ₂ − sin φ₁·cos Δλ)`.
     */
    fun bearingDegrees(latitude: Double, longitude: Double): Double {
        val lat = Math.toRadians(latitude)
        val deltaLng = Math.toRadians(KAABA_LONGITUDE - longitude)
        val kaabaLat = Math.toRadians(KAABA_LATITUDE)

        val y = sin(deltaLng)
        val x = cos(lat) * tan(kaabaLat) - sin(lat) * cos(deltaLng)
        return normalizeDegrees(Math.toDegrees(atan2(y, x)))
    }

    /** Distance orthodromique jusqu'à la Kaaba, en kilomètres (haversine). */
    fun distanceToKaabaKm(latitude: Double, longitude: Double): Double {
        val lat = Math.toRadians(latitude)
        val kaabaLat = Math.toRadians(KAABA_LATITUDE)
        val dLat = kaabaLat - lat
        val dLng = Math.toRadians(KAABA_LONGITUDE - longitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat) * cos(kaabaLat) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(a).coerceAtMost(1.0))
    }
}

/** Ramène un angle en degrés dans [0, 360[. */
fun normalizeDegrees(degrees: Double): Double = ((degrees % 360) + 360) % 360

/**
 * Écart signé le plus court entre deux azimuts, dans ]−180, 180].
 * Positif = [target] est à droite de [from].
 */
fun shortestAngleDelta(from: Double, target: Double): Double {
    val diff = normalizeDegrees(target - from)
    return if (diff > 180.0) diff - 360.0 else diff
}

/** Vrai si l'appareil pointe la Qibla à [toleranceDegrees] près. */
fun isAlignedWithQibla(
    headingDegrees: Double,
    qiblaDegrees: Double,
    toleranceDegrees: Double = 3.0,
): Boolean = abs(shortestAngleDelta(headingDegrees, qiblaDegrees)) <= toleranceDegrees

/**
 * Interpolation circulaire entre deux angles (passe par le plus court chemin) :
 * utilisée pour lisser l'aiguille sans saut au passage 359° → 0°.
 */
fun lerpDegrees(from: Double, target: Double, fraction: Double): Double =
    normalizeDegrees(from + shortestAngleDelta(from, target) * fraction)
