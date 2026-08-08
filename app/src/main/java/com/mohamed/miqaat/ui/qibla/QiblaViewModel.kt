package com.mohamed.miqaat.ui.qibla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.miqaat.data.compass.CompassDataSource
import com.mohamed.miqaat.data.location.LocationRepository
import com.mohamed.miqaat.domain.QiblaCalculator
import com.mohamed.miqaat.domain.isAlignedWithQibla
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class QiblaViewModel(
    locationRepository: LocationRepository,
    private val compass: CompassDataSource,
) : ViewModel() {

    // La position ne change pas en cours d'écran : on la fige à la création,
    // et l'angle de la Qibla avec elle (calcul purement géométrique, hors ligne).
    private val location = locationRepository.currentLocation()

    private val baseState = QiblaUiState(
        cityName = location.cityName,
        qiblaBearing = QiblaCalculator.bearingDegrees(location.latitude, location.longitude),
        distanceKm = QiblaCalculator.distanceToKaabaKm(location.latitude, location.longitude),
        compassAvailable = compass.isAvailable,
    )

    /**
     * Redémarre la lecture des capteurs quand la rotation de l'écran change
     * (les axes à permuter en dépendent).
     */
    private val displayRotation = MutableStateFlow(compass.displayRotation)

    val uiState: StateFlow<QiblaUiState> = displayRotation
        .flatMapLatest { rotation ->
            compass.displayRotation = rotation
            compass.readings(location.latitude, location.longitude)
        }
        .map { reading ->
            baseState.copy(
                deviceHeading = reading.headingDegrees,
                accuracy = reading.accuracy,
                isAligned = isAlignedWithQibla(reading.headingDegrees, baseState.qiblaBearing),
            )
        }
        // WhileSubscribed : les capteurs sont libérés dès que l'écran disparaît.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(2_000), baseState)

    fun onDisplayRotationChanged(rotation: Int) {
        displayRotation.value = rotation
    }
}
