package com.mohamed.miqaat.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohamed.miqaat.R
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.miqaatApp
import com.mohamed.miqaat.ui.reliability.ReliabilityBanner
import com.mohamed.miqaat.ui.theme.MiqaatTheme

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenInvocations: () -> Unit,
    onOpenQuran: () -> Unit,
    onOpenReliability: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = homeViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onOpenSettings = onOpenSettings,
        onOpenQibla = onOpenQibla,
        onOpenCalendar = onOpenCalendar,
        onOpenInvocations = onOpenInvocations,
        onOpenQuran = onOpenQuran,
        onOpenReliability = onOpenReliability,
        modifier = modifier,
    )
}

@Composable
private fun homeViewModel(): HomeViewModel {
    val app = LocalContext.current.miqaatApp
    return viewModel {
        HomeViewModel(
            locationRepository = app.locationRepository,
            settingsRepository = app.settingsRepository,
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onOpenSettings: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenInvocations: () -> Unit,
    onOpenQuran: () -> Unit = {},
    onOpenReliability: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Pas de statusBarsPadding ici : le héros peint volontairement son
        // dégradé derrière la barre de statut et gère lui-même sa marge.
        // L'inset du bas, lui, va avant verticalScroll — après, il défilerait.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            HeroSection(state = state)
            Spacer(Modifier.height(24.dp))
            // Ne s'affiche que si quelque chose de critique et de certain empêche
            // les notifications d'arriver — sinon la fonction ne pose rien.
            ReliabilityBanner(
                onOpen = onOpenReliability,
                // Marges portées par la bannière elle-même : invisible, elle ne
                // compose rien du tout et ne laisse donc aucun blanc.
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            )
            Text(
                text = stringResource(R.string.section_today_times),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp),
            )
            Spacer(Modifier.height(8.dp))
            PrayerList(
                prayers = state.prayers,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = stringResource(R.string.settings_open),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Les deux entrées secondaires vivent côte à côte : Row respecte le RTL,
        // la Qibla reste donc toujours la plus proche du bord de départ.
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(4.dp),
        ) {
            IconButton(onClick = onOpenQibla) {
                Icon(
                    painter = painterResource(R.drawable.ic_qibla),
                    contentDescription = stringResource(R.string.qibla_open),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenCalendar) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = stringResource(R.string.calendar_open),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenInvocations) {
                Icon(
                    painter = painterResource(R.drawable.ic_invocation),
                    contentDescription = stringResource(R.string.invocation_open),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenQuran) {
                Icon(
                    painter = painterResource(R.drawable.ic_quran),
                    contentDescription = stringResource(R.string.quran_open),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Chiffres tabulaires (largeur fixe) : évite que l'heure ou le compte à rebours « tremble ». */
internal fun TextStyle.tabularNumbers(): TextStyle = copy(fontFeatureSettings = "tnum")

private fun previewState(afterIsha: Boolean = false) = HomeUiState(
    cityName = "سكيكدة",
    gregorianDate = "الأحد 5 يوليو 2026",
    hijriDate = "20 محرم 1448",
    prayers = listOf(
        PrayerRowUi(PrayerName.FAJR, "03:42", isNext = false, isPast = true),
        PrayerRowUi(PrayerName.SUNRISE, "05:32", isNext = false, isPast = true),
        PrayerRowUi(PrayerName.DHUHR, "12:37", isNext = !afterIsha, isPast = false),
        PrayerRowUi(PrayerName.ASR, "16:28", isNext = false, isPast = false),
        PrayerRowUi(PrayerName.MAGHRIB, "19:42", isNext = false, isPast = afterIsha),
        PrayerRowUi(PrayerName.ISHA, "21:20", isNext = false, isPast = afterIsha),
    ),
    next = if (afterIsha) {
        NextPrayerUi(PrayerName.FAJR, "03:43", "05:12:44", isTomorrowFajr = true)
    } else {
        NextPrayerUi(PrayerName.DHUHR, "12:37", "02:13:05", isTomorrowFajr = false)
    },
)

@Preview(showBackground = true, locale = "ar")
@Composable
private fun HomeContentPreview() {
    MiqaatTheme { HomeContent(state = previewState(), onOpenSettings = {}, onOpenQibla = {}, onOpenCalendar = {}, onOpenInvocations = {}) }
}

@Preview(showBackground = true, locale = "ar", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeContentDarkPreview() {
    MiqaatTheme { HomeContent(state = previewState(), onOpenSettings = {}, onOpenQibla = {}, onOpenCalendar = {}, onOpenInvocations = {}) }
}

@Preview(showBackground = true, locale = "ar")
@Composable
private fun HomeContentTomorrowFajrPreview() {
    MiqaatTheme { HomeContent(state = previewState(afterIsha = true), onOpenSettings = {}, onOpenQibla = {}, onOpenCalendar = {}, onOpenInvocations = {}) }
}
