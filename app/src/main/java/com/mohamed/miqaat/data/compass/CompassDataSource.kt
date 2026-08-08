package com.mohamed.miqaat.data.compass

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import com.mohamed.miqaat.domain.lerpDegrees
import com.mohamed.miqaat.domain.normalizeDegrees
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** Fiabilité du champ magnétique telle que rapportée par le capteur. */
enum class CompassAccuracy { UNRELIABLE, LOW, MEDIUM, HIGH }

/**
 * Cap de l'appareil (bord supérieur de l'écran) par rapport au **nord géographique**,
 * en degrés dans [0, 360[.
 */
data class CompassReading(
    val headingDegrees: Double,
    val accuracy: CompassAccuracy,
)

/**
 * Lecture de la boussole du téléphone, 100 % hors ligne.
 *
 * Trois choses s'enchaînent :
 * 1. le capteur donne une orientation par rapport au **nord magnétique** ;
 * 2. [GeomagneticField] (modèle WMM embarqué dans Android, aucun réseau) donne
 *    la déclinaison locale, qu'on ajoute pour obtenir le **nord géographique**
 *    — c'est celui-là qui sert à viser la Kaaba ;
 * 3. l'angle est lissé (interpolation circulaire) pour éviter l'aiguille qui tremble.
 *
 * Le capteur est référencé à l'écran « naturel » de l'appareil : si l'écran est
 * pivoté, il faut permuter les axes ([SensorManager.remapCoordinateSystem]).
 */
class CompassDataSource(context: Context) {

    private val sensorManager: SensorManager? =
        context.applicationContext.getSystemService(SensorManager::class.java)

    /**
     * Rotation courante de l'affichage ([Surface.ROTATION_0]…). Mise à jour par
     * l'UI, qui est la seule à connaître le Display de la fenêtre.
     */
    @Volatile
    var displayRotation: Int = Surface.ROTATION_0

    /** Un appareil sans magnétomètre ne peut pas faire de boussole. */
    val isAvailable: Boolean
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null

    /**
     * Flux des caps, actif seulement tant qu'il est collecté (les capteurs sont
     * enregistrés à l'abonnement et libérés à l'annulation).
     *
     * @param latitude/[longitude] position servant au calcul de la déclinaison.
     */
    fun readings(latitude: Double, longitude: Double): Flow<CompassReading> = callbackFlow {
        val manager = sensorManager
        if (manager == null) {
            close()
            return@callbackFlow
        }

        // Le vecteur de rotation fusionne accéléromètre + magnétomètre (+ gyroscope
        // quand il existe) : c'est le plus stable. Les deux replis couvrent les
        // appareils d'entrée de gamme.
        val rotationSensor = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: manager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationSensor == null && (accelerometer == null || magnetometer == null)) {
            close()
            return@callbackFlow
        }

        // Déclinaison magnétique locale : constante à l'échelle d'une session,
        // calculée une fois (le modèle est embarqué, donc valable hors ligne).
        val declination = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            0f,
            System.currentTimeMillis(),
        ).declination.toDouble()

        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val remappedMatrix = FloatArray(9)
            private val orientation = FloatArray(3)
            private val gravity = FloatArray(3)
            private val geomagnetic = FloatArray(3)
            private var hasGravity = false
            private var hasGeomagnetic = false
            private var smoothed: Double? = null
            private var accuracy = CompassAccuracy.UNRELIABLE

            override fun onSensorChanged(event: SensorEvent) {
                val matrixReady = when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR,
                    Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
                    -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        true
                    }

                    Sensor.TYPE_ACCELEROMETER -> {
                        event.values.copyInto(gravity, endIndex = 3)
                        hasGravity = true
                        hasGeomagnetic &&
                            SensorManager.getRotationMatrix(
                                rotationMatrix, null, gravity, geomagnetic,
                            )
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        event.values.copyInto(geomagnetic, endIndex = 3)
                        hasGeomagnetic = true
                        accuracy = event.accuracy.toCompassAccuracy()
                        hasGravity &&
                            SensorManager.getRotationMatrix(
                                rotationMatrix, null, gravity, geomagnetic,
                            )
                    }

                    else -> false
                }
                if (!matrixReady) return

                val (axisX, axisY) = remapAxes(displayRotation)
                SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedMatrix)
                SensorManager.getOrientation(remappedMatrix, orientation)

                val magneticHeading = Math.toDegrees(orientation[0].toDouble())
                val trueHeading = normalizeDegrees(magneticHeading + declination)
                // Lissage circulaire : ~15 % du chemin par événement, assez réactif
                // pour suivre la main sans osciller sur le bruit du capteur.
                val next = smoothed?.let { lerpDegrees(it, trueHeading, SMOOTHING) } ?: trueHeading
                smoothed = next

                trySend(CompassReading(next, accuracy))
            }

            override fun onAccuracyChanged(sensor: Sensor, value: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD ||
                    sensor.type == Sensor.TYPE_ROTATION_VECTOR ||
                    sensor.type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
                ) {
                    accuracy = value.toCompassAccuracy()
                }
            }
        }

        if (rotationSensor != null) {
            manager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            // Le magnétomètre seul sert à connaître la précision réelle du champ.
            magnetometer?.let {
                manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } else {
            manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            manager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose { manager.unregisterListener(listener) }
    }
        // Les capteurs émettent plus vite que l'écran ne se redessine : seule
        // la dernière valeur compte, on jette les intermédiaires.
        .conflate()

    private companion object {
        const val SMOOTHING = 0.15
    }
}

/**
 * Axes à donner à [SensorManager.remapCoordinateSystem] pour que « le haut de
 * l'écran » reste le haut de l'écran quel que soit le sens de l'appareil.
 */
private fun remapAxes(displayRotation: Int): Pair<Int, Int> = when (displayRotation) {
    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
}

private fun Int.toCompassAccuracy(): CompassAccuracy = when (this) {
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIUM
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW
    else -> CompassAccuracy.UNRELIABLE
}
