package com.mohamed.miqaat.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QiblaCalculatorTest {

    // Azimuts de référence (islamicfinder / praytimes, cap de grand cercle).
    @Test
    fun `azimut de la Qibla depuis Skikda`() {
        assertEquals(109.2, QiblaCalculator.bearingDegrees(36.8665, 6.9063), 0.5)
    }

    @Test
    fun `azimut de la Qibla depuis Paris`() {
        assertEquals(119.2, QiblaCalculator.bearingDegrees(48.8566, 2.3522), 0.5)
    }

    @Test
    fun `azimut de la Qibla depuis Jakarta — vers l'ouest-nord-ouest`() {
        assertEquals(295.2, QiblaCalculator.bearingDegrees(-6.2088, 106.8456), 0.5)
    }

    @Test
    fun `azimut de la Qibla depuis New York — vers le nord-est`() {
        assertEquals(58.5, QiblaCalculator.bearingDegrees(40.7128, -74.0060), 0.5)
    }

    @Test
    fun `depuis un point plein nord de la Kaaba, la Qibla est au sud`() {
        val bearing = QiblaCalculator.bearingDegrees(
            latitude = QiblaCalculator.KAABA_LATITUDE + 10,
            longitude = QiblaCalculator.KAABA_LONGITUDE,
        )
        assertEquals(180.0, bearing, 0.001)
    }

    @Test
    fun `l'azimut reste toujours dans zero-360`() {
        val bearings = listOf(
            QiblaCalculator.bearingDegrees(-33.8688, 151.2093), // Sydney
            QiblaCalculator.bearingDegrees(64.1466, -21.9426), // Reykjavik
            QiblaCalculator.bearingDegrees(1.3521, 103.8198), // Singapour
        )
        bearings.forEach { assertTrue("$it hors bornes", it >= 0.0 && it < 360.0) }
    }

    @Test
    fun `distance jusqu'a la Kaaba`() {
        // ~3 600 km entre Skikda et La Mecque.
        assertEquals(3603.0, QiblaCalculator.distanceToKaabaKm(36.8665, 6.9063), 30.0)
        assertEquals(0.0, QiblaCalculator.distanceToKaabaKm(21.4225241, 39.8261818), 0.01)
    }

    @Test
    fun `l'ecart d'angle prend le chemin le plus court`() {
        assertEquals(2.0, shortestAngleDelta(359.0, 1.0), 0.001)
        assertEquals(-2.0, shortestAngleDelta(1.0, 359.0), 0.001)
        assertEquals(180.0, shortestAngleDelta(0.0, 180.0), 0.001)
    }

    @Test
    fun `l'alignement tolere trois degres de part et d'autre`() {
        assertTrue(isAlignedWithQibla(headingDegrees = 112.0, qiblaDegrees = 110.0))
        assertTrue(isAlignedWithQibla(headingDegrees = 358.0, qiblaDegrees = 1.0))
        assertFalse(isAlignedWithQibla(headingDegrees = 120.0, qiblaDegrees = 110.0))
    }

    @Test
    fun `le lissage passe par le zero sans faire le tour`() {
        // De 350° vers 10° : on avance vers 360/0, pas vers 180.
        val smoothed = lerpDegrees(from = 350.0, target = 10.0, fraction = 0.5)
        assertEquals(0.0, smoothed, 0.001)
    }

    @Test
    fun `la normalisation ramene les angles negatifs`() {
        assertEquals(350.0, normalizeDegrees(-10.0), 0.001)
        assertEquals(0.0, normalizeDegrees(360.0), 0.001)
        assertEquals(90.0, normalizeDegrees(450.0), 0.001)
    }
}
