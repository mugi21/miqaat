package com.mohamed.miqaat.ui.invocations

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohamed.miqaat.R
import com.mohamed.miqaat.domain.model.InvocationSchedule
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.ui.labelRes
import kotlin.math.absoluteValue

/**
 * Création et modification d'une invocation, en un seul dialogue.
 *
 * Une invocation livrée n'y expose que son **moment** : son titre et son texte
 * viennent des ressources et ne s'éditent pas (D26).
 */
@Composable
fun InvocationEditor(
    draft: InvocationDraft,
    onChange: ((InvocationDraft) -> InvocationDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var pickingTime by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (draft.isNew) R.string.invocation_add else R.string.invocation_edit,
                ),
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (draft.isBuiltin) {
                    Text(text = draft.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.invocation_builtin_readonly),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedTextField(
                        value = draft.title,
                        onValueChange = { value -> onChange { it.copy(title = value) } },
                        label = { Text(stringResource(R.string.invocation_field_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = draft.body,
                        onValueChange = { value -> onChange { it.copy(body = value) } },
                        label = { Text(stringResource(R.string.invocation_field_body)) },
                        minLines = 3,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.invocation_schedule),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScheduleMode.entries.forEach { mode ->
                        FilterChip(
                            selected = draft.mode == mode,
                            onClick = { onChange { it.copy(mode = mode) } },
                            label = { Text(stringResource(mode.labelRes)) },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                when (draft.mode) {
                    ScheduleMode.FIXED -> OutlinedButton(onClick = { pickingTime = true }) {
                        Text(formatClock(draft.hour, draft.minute))
                    }

                    ScheduleMode.ANCHOR -> AnchorEditor(draft = draft, onChange = onChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = draft.canSave) {
                Text(stringResource(R.string.invocation_save))
            }
        },
        // Le bouton reste inconditionnel : un `if` autour du paramètre lui ferait
        // perdre son contexte @Composable (piège rencontré en session 8).
        dismissButton = {
            Row {
                if (!draft.isNew && !draft.isBuiltin) {
                    TextButton(onClick = onDelete) {
                        Text(
                            text = stringResource(R.string.invocation_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        },
    )

    if (pickingTime) {
        TimePickerDialog(
            initialHour = draft.hour,
            initialMinute = draft.minute,
            onConfirm = { hour, minute ->
                onChange { it.copy(hour = hour, minute = minute) }
                pickingTime = false
            },
            onDismiss = { pickingTime = false },
        )
    }
}

/** Choix de la prière repère, puis du décalage par pas de cinq minutes. */
@Composable
private fun AnchorEditor(
    draft: InvocationDraft,
    onChange: ((InvocationDraft) -> InvocationDraft) -> Unit,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            PrayerName.entries.filter { it.isPrayer }.forEach { prayer ->
                FilterChip(
                    selected = draft.anchorPrayer == prayer,
                    onClick = { onChange { it.copy(anchorPrayer = prayer) } },
                    label = { Text(stringResource(prayer.labelRes)) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OffsetButton(
                label = "−",
                contentDescription = stringResource(R.string.invocation_offset_decrease),
                enabled = draft.offsetMinutes > InvocationSchedule.OFFSET_MIN,
                onClick = {
                    onChange {
                        it.copy(
                            offsetMinutes = InvocationSchedule.sanitizeOffset(
                                it.offsetMinutes - InvocationSchedule.OFFSET_STEP,
                            ),
                        )
                    }
                },
            )
            Text(
                text = offsetLabel(draft.anchorPrayer, draft.offsetMinutes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            OffsetButton(
                label = "+",
                contentDescription = stringResource(R.string.invocation_offset_increase),
                enabled = draft.offsetMinutes < InvocationSchedule.OFFSET_MAX,
                onClick = {
                    onChange {
                        it.copy(
                            offsetMinutes = InvocationSchedule.sanitizeOffset(
                                it.offsetMinutes + InvocationSchedule.OFFSET_STEP,
                            ),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun offsetLabel(prayer: PrayerName, offsetMinutes: Int): String {
    val name = stringResource(prayer.labelRes)
    val minutes = offsetMinutes.absoluteValue
    return when {
        offsetMinutes == 0 -> stringResource(R.string.invocation_schedule_at_prayer, name)
        offsetMinutes > 0 -> stringResource(
            R.string.invocation_schedule_after,
            name,
            pluralStringResource(R.plurals.duration_minutes, minutes, minutes),
        )

        else -> stringResource(
            R.string.invocation_schedule_before,
            name,
            pluralStringResource(R.plurals.duration_minutes, minutes, minutes),
        )
    }
}

@Composable
private fun OffsetButton(
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .width(56.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Text(text = label, style = MaterialTheme.typography.titleLarge)
    }
}

/**
 * Le sélecteur d'heure de Material 3 dans un simple `AlertDialog` — la
 * librairie n'expose pas encore de dialogue tout fait pour lui.
 * `is24Hour = true` : l'app affiche partout des heures sur 24 h.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.invocation_pick_time)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(stringResource(R.string.invocation_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
            }
        },
    )
}

private val ScheduleMode.labelRes: Int
    get() = when (this) {
        ScheduleMode.FIXED -> R.string.invocation_mode_fixed
        ScheduleMode.ANCHOR -> R.string.invocation_mode_anchor
    }
