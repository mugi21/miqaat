package com.mohamed.miqaat.ui.invocations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohamed.miqaat.R
import com.mohamed.miqaat.domain.model.Invocation
import com.mohamed.miqaat.domain.model.InvocationSchedule
import com.mohamed.miqaat.miqaatApp
import com.mohamed.miqaat.notifications.PrayerAlarmScheduler
import com.mohamed.miqaat.ui.displayBody
import com.mohamed.miqaat.ui.displayTitle
import com.mohamed.miqaat.ui.labelRes
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * L'écran des adhkār : la liste, la lecture d'une invocation, et son éditeur.
 *
 * Les trois vivent dans le même écran plutôt que dans une pile de navigation :
 * l'état appartient au ViewModel, donc aucun argument ne circule et D7 tient
 * toujours (même raisonnement qu'au calendrier, D21).
 */
@Composable
fun InvocationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** Invocation à ouvrir d'emblée, quand on arrive depuis sa notification. */
    openInvocationId: Long? = null,
    viewModel: InvocationsViewModel = invocationsViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(openInvocationId) {
        openInvocationId?.let(viewModel::open)
    }

    // Enregistré après celui de MainActivity, donc prioritaire : depuis la
    // lecture, « retour » ramène à la liste et non à l'accueil.
    BackHandler(enabled = state.reading != null) { viewModel.closeReading() }

    val reading = state.reading
    if (reading != null) {
        InvocationDetail(
            title = reading.displayTitle(context),
            body = reading.displayBody(context),
            scheduleSummary = scheduleSummary(reading),
            onEdit = {
                viewModel.startEditing(
                    reading,
                    title = reading.displayTitle(context),
                    body = reading.displayBody(context),
                )
            },
            onBack = viewModel::closeReading,
            modifier = modifier,
        )
    } else {
        InvocationsList(
            invocations = state.invocations,
            onOpen = { viewModel.open(it.id) },
            onToggle = viewModel::setEnabled,
            onAdd = viewModel::startCreating,
            onBack = onBack,
            modifier = modifier,
        )
    }

    state.editing?.let { draft ->
        InvocationEditor(
            draft = draft,
            onChange = viewModel::editDraft,
            onSave = viewModel::saveDraft,
            onDelete = { draft.original?.let(viewModel::delete) },
            onDismiss = viewModel::cancelEditing,
        )
    }
}

@Composable
private fun invocationsViewModel(): InvocationsViewModel {
    val app = LocalContext.current.miqaatApp
    return viewModel {
        InvocationsViewModel(
            repository = app.invocationRepository,
            onInvocationsChanged = { PrayerAlarmScheduler(app).scheduleNext() },
        )
    }
}

@Composable
private fun InvocationsList(
    invocations: List<Invocation>,
    onOpen: (Invocation) -> Unit,
    onToggle: (Invocation, Boolean) -> Unit,
    onAdd: () -> Unit,
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
    ) {
        ScreenHeader(title = stringResource(R.string.invocation_title), onBack = onBack)

        Spacer(Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                invocations.forEachIndexed { index, invocation ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                    InvocationRow(
                        invocation = invocation,
                        onOpen = { onOpen(invocation) },
                        onToggle = { onToggle(invocation, it) },
                    )
                }
                if (invocations.isEmpty()) {
                    Text(
                        text = stringResource(R.string.invocation_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.invocation_guard_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp),
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onAdd,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp),
        ) {
            Text(stringResource(R.string.invocation_add))
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InvocationRow(
    invocation: Invocation,
    onOpen: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = invocation.displayTitle(context),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (invocation.enabled) {
                    scheduleSummary(invocation)
                } else {
                    stringResource(R.string.invocation_schedule_disabled)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (invocation.enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = invocation.enabled, onCheckedChange = onToggle)
    }
}

/** En-tête commun aux deux vues de l'écran : flèche de retour + titre. */
@Composable
internal fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.action_back),
            )
        }
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
    }
}

/** « عند 07:00 », « بعد الفجر بـ30 دقيقة »… */
@Composable
internal fun scheduleSummary(invocation: Invocation): String =
    when (val schedule = invocation.schedule) {
        is InvocationSchedule.FixedTime -> stringResource(
            R.string.invocation_schedule_at,
            formatClock(schedule.hour, schedule.minute),
        )

        is InvocationSchedule.PrayerAnchor -> {
            val prayer = stringResource(schedule.prayer.labelRes)
            val minutes = schedule.offsetMinutes.absoluteValue
            when {
                schedule.offsetMinutes == 0 ->
                    stringResource(R.string.invocation_schedule_at_prayer, prayer)

                schedule.offsetMinutes > 0 -> stringResource(
                    R.string.invocation_schedule_after,
                    prayer,
                    pluralStringResource(R.plurals.duration_minutes, minutes, minutes),
                )

                else -> stringResource(
                    R.string.invocation_schedule_before,
                    prayer,
                    pluralStringResource(R.plurals.duration_minutes, minutes, minutes),
                )
            }
        }
    }

/** Chiffres occidentaux même en arabe, comme partout ailleurs dans l'app. */
internal fun formatClock(hour: Int, minute: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", hour, minute)
