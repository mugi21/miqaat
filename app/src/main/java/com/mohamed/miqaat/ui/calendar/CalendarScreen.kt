package com.mohamed.miqaat.ui.calendar

import android.content.res.Configuration
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohamed.miqaat.R
import com.mohamed.miqaat.domain.IMSAK_MINUTES_BEFORE_FAJR
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.miqaatApp
import com.mohamed.miqaat.ui.home.PrayerList
import com.mohamed.miqaat.ui.home.PrayerRowUi
import com.mohamed.miqaat.ui.home.tabularNumbers
import com.mohamed.miqaat.ui.theme.MiqaatTheme
import java.time.LocalDate

@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = calendarViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarContent(
        state = state,
        onBack = onBack,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
        onDayClick = viewModel::selectDate,
        onBackToToday = viewModel::backToToday,
        modifier = modifier,
    )
}

@Composable
private fun calendarViewModel(): CalendarViewModel {
    val app = LocalContext.current.miqaatApp
    return viewModel {
        CalendarViewModel(
            locationRepository = app.locationRepository,
            settingsRepository = app.settingsRepository,
        )
    }
}

@Composable
private fun CalendarContent(
    state: CalendarUiState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onBackToToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Insets avant verticalScroll : posés après, ils défileraient avec le contenu.
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(
                text = stringResource(R.string.calendar_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            // Le retour à aujourd'hui ne s'affiche que si l'on s'en est éloigné.
            if (!state.isTodaySelected) {
                TextButton(onClick = onBackToToday) {
                    Text(stringResource(R.string.calendar_today))
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                MonthHeader(
                    hijriMonthLabel = state.hijriMonthLabel,
                    gregorianMonthLabel = state.gregorianMonthLabel,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                )
                Spacer(Modifier.height(4.dp))
                MonthGrid(
                    weekdayLabels = state.weekdayLabels,
                    cells = state.cells,
                    onDayClick = onDayClick,
                )
            }
        }

        state.selectedDay?.let { day ->
            Spacer(Modifier.height(24.dp))
            SelectedDayHeader(day = day)
            Spacer(Modifier.height(8.dp))
            PrayerList(
                prayers = day.prayers,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )
            day.ramadan?.let { ramadan ->
                Spacer(Modifier.height(16.dp))
                RamadanCard(
                    ramadan = ramadan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MonthHeader(
    hijriMonthLabel: String,
    gregorianMonthLabel: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        // Les deux flèches sont autoMirrored : en RTL, « précédent » pointe bien à droite.
        IconButton(onClick = onPreviousMonth) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.calendar_previous_month),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = hijriMonthLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = gregorianMonthLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        IconButton(onClick = onNextMonth) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = stringResource(R.string.calendar_next_month),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectedDayHeader(day: SelectedDayUi) {
    Column(modifier = Modifier.padding(horizontal = 28.dp)) {
        Text(
            text = day.hijriDate,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = day.gregorianDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * L'encart du jeûne, en tons bleutés (tertiaire) pour se distinguer du vert
 * des prières : il ne s'affiche que si le jour tombe en Ramadan.
 */
@Composable
private fun RamadanCard(ramadan: RamadanUi, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Column(Modifier.padding(vertical = 14.dp)) {
            Text(
                text = stringResource(R.string.ramadan_title),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(4.dp))
            RamadanRow(
                label = stringResource(R.string.ramadan_imsak),
                value = ramadan.imsak,
                hint = stringResource(
                    R.string.ramadan_imsak_hint,
                    pluralStringResource(
                        R.plurals.duration_minutes,
                        IMSAK_MINUTES_BEFORE_FAJR.toInt(),
                        IMSAK_MINUTES_BEFORE_FAJR.toInt(),
                    ),
                ),
            )
            RamadanDivider()
            RamadanRow(
                label = stringResource(R.string.ramadan_iftar),
                value = ramadan.iftar,
                hint = stringResource(R.string.ramadan_iftar_hint),
            )
            RamadanDivider()
            RamadanRow(
                label = stringResource(R.string.ramadan_fasting_duration),
                value = stringResource(
                    R.string.duration_hours_minutes,
                    ramadan.fastingHours,
                    ramadan.fastingMinutes,
                ),
                hint = null,
            )
        }
    }
}

@Composable
private fun RamadanRow(label: String, value: String, hint: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.tabularNumbers(),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RamadanDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
    )
}

private fun previewState(ramadan: Boolean = true): CalendarUiState {
    val month = LocalDate.of(2027, 2, 1)
    val cells = buildList<CalendarDayUi?> {
        repeat(2) { add(null) }
        for (day in 1..28) {
            val date = month.withDayOfMonth(day)
            add(
                CalendarDayUi(
                    date = date,
                    gregorianDay = day.toString(),
                    hijriDay = ((day + 3) % 30 + 1).toString(),
                    isSelected = day == 12,
                    isToday = day == 8,
                    isRamadan = day >= 8,
                ),
            )
        }
        repeat(5) { add(null) }
    }
    return CalendarUiState(
        hijriMonthLabel = "شعبان – رمضان 1448",
        gregorianMonthLabel = "فيفري 2027",
        weekdayLabels = listOf("سبت", "أحد", "إثن", "ثلا", "أرب", "خمي", "جمع"),
        cells = cells,
        selectedDay = SelectedDayUi(
            hijriDate = "5 رمضان 1448",
            gregorianDate = "الجمعة 12 فيفري 2027",
            prayers = listOf(
                PrayerRowUi(PrayerName.FAJR, "06:12", isNext = false, isPast = false),
                PrayerRowUi(PrayerName.SUNRISE, "07:41", isNext = false, isPast = false),
                PrayerRowUi(PrayerName.DHUHR, "12:52", isNext = false, isPast = false),
                PrayerRowUi(PrayerName.ASR, "15:47", isNext = false, isPast = false),
                PrayerRowUi(PrayerName.MAGHRIB, "18:04", isNext = false, isPast = false),
                PrayerRowUi(PrayerName.ISHA, "19:26", isNext = false, isPast = false),
            ),
            ramadan = if (ramadan) {
                RamadanUi(imsak = "06:02", iftar = "18:04", fastingHours = 12, fastingMinutes = 2)
            } else {
                null
            },
        ),
        isTodaySelected = false,
    )
}

@Preview(showBackground = true, locale = "ar", heightDp = 1100)
@Composable
private fun CalendarContentPreview() {
    MiqaatTheme {
        CalendarContent(previewState(), {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, locale = "ar", heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CalendarContentDarkPreview() {
    MiqaatTheme {
        CalendarContent(previewState(), {}, {}, {}, {}, {})
    }
}
