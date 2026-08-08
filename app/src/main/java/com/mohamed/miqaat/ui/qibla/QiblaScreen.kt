package com.mohamed.miqaat.ui.qibla

import android.content.res.Configuration
import android.view.Surface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as M3Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohamed.miqaat.R
import com.mohamed.miqaat.data.compass.CompassAccuracy
import com.mohamed.miqaat.data.compass.CompassDataSource
import com.mohamed.miqaat.miqaatApp
import com.mohamed.miqaat.ui.home.tabularNumbers
import com.mohamed.miqaat.ui.theme.MiqaatTheme
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun QiblaScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QiblaViewModel = qiblaViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Les axes du capteur dépendent du sens de l'écran : on tient le ViewModel
    // informé (la vue est la seule à connaître le Display de la fenêtre).
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    LaunchedEffect(view, configuration) {
        viewModel.onDisplayRotationChanged(view.display?.rotation ?: Surface.ROTATION_0)
    }

    // Petite vibration au moment précis où l'on tombe sur la Qibla :
    // on peut prier sans regarder l'écran.
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.isAligned) {
        if (state.isAligned) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    QiblaContent(state = state, onBack = onBack, modifier = modifier)
}

@Composable
private fun qiblaViewModel(): QiblaViewModel {
    val context = LocalContext.current
    val app = context.miqaatApp
    return viewModel {
        QiblaViewModel(
            locationRepository = app.locationRepository,
            compass = CompassDataSource(app),
        )
    }
}

@Composable
private fun QiblaContent(
    state: QiblaUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Insets avant verticalScroll : posés après, ils défileraient avec le contenu.
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(
                text = stringResource(R.string.qibla_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        Text(
            text = state.cityName,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        QiblaCompass(
            qiblaBearing = state.qiblaBearing,
            deviceHeading = state.deviceHeading,
            isAligned = state.isAligned,
            modifier = Modifier.padding(horizontal = 40.dp),
        )

        Spacer(Modifier.height(20.dp))

        StatusMessage(state)

        Spacer(Modifier.height(20.dp))

        M3Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                InfoRow(
                    label = stringResource(R.string.qibla_bearing_label),
                    value = "${state.qiblaBearing.roundToInt()}°",
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                InfoRow(
                    label = stringResource(R.string.qibla_distance_label),
                    value = stringResource(
                        R.string.qibla_distance_value,
                        String.format(Locale.ROOT, "%,d", state.distanceKm.roundToInt()),
                    ),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** Une seule ligne d'aide à la fois : panne capteur > calibration > visée. */
@Composable
private fun StatusMessage(state: QiblaUiState) {
    val (text, color) = when {
        !state.compassAvailable ->
            stringResource(R.string.qibla_no_compass) to MaterialTheme.colorScheme.error

        state.accuracy == CompassAccuracy.UNRELIABLE || state.accuracy == CompassAccuracy.LOW ->
            stringResource(R.string.qibla_accuracy_warning) to MaterialTheme.colorScheme.tertiary

        state.isAligned ->
            stringResource(R.string.qibla_aligned) to MaterialTheme.colorScheme.primary

        else ->
            stringResource(R.string.qibla_turn_hint) to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (state.isAligned) FontWeight.SemiBold else null,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp),
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.tabularNumbers(),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun previewState(
    aligned: Boolean = false,
    accuracy: CompassAccuracy = CompassAccuracy.HIGH,
) = QiblaUiState(
    cityName = "سكيكدة",
    qiblaBearing = 109.2,
    distanceKm = 3603.0,
    deviceHeading = if (aligned) 109.0 else 25.0,
    accuracy = accuracy,
    isAligned = aligned,
)

@Preview(showBackground = true, locale = "ar")
@Composable
private fun QiblaPreview() {
    MiqaatTheme { QiblaContent(state = previewState(), onBack = {}) }
}

@Preview(showBackground = true, locale = "ar", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QiblaAlignedDarkPreview() {
    MiqaatTheme { QiblaContent(state = previewState(aligned = true), onBack = {}) }
}

@Preview(showBackground = true, locale = "fr")
@Composable
private fun QiblaCalibrationPreview() {
    MiqaatTheme {
        QiblaContent(
            state = previewState(accuracy = CompassAccuracy.LOW),
            onBack = {},
        )
    }
}
