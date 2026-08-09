package com.mohamed.miqaat.ui.reliability

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohamed.miqaat.R
import com.mohamed.miqaat.domain.reliability.CheckState
import com.mohamed.miqaat.domain.reliability.ReliabilityCheck
import com.mohamed.miqaat.domain.reliability.ReliabilityStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * « Pourquoi l'adhan n'arrive-t-il pas ? »
 *
 * Cinq contrôles, un bouton par contrôle défaillant, et deux informations qui
 * disent la vérité mieux que n'importe quel message : l'heure de la prochaine
 * alerte programmée, et celle de la dernière réellement délivrée. Si la seconde
 * date de plusieurs jours alors que la première est toujours là, c'est que
 * quelque chose empêche l'application de s'exécuter.
 */
@Composable
fun ReliabilityScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val viewModel: ReliabilityViewModel = viewModel { ReliabilityViewModel(appContext) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Au retour d'un écran système, l'utilisateur vient peut-être d'accorder ce
    // qui manquait : le diagnostic doit se refaire sans quitter l'écran.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var manualHelpVisible by remember { mutableStateOf(false) }

    ReliabilityContent(
        state = state,
        manualHelpVisible = manualHelpVisible,
        onBack = onBack,
        onFix = { check -> manualHelpVisible = !viewModel.fix(check) },
        onOemAcknowledged = viewModel::setOemAcknowledged,
        onTest = viewModel::sendTestNotification,
        modifier = modifier,
    )
}

@Composable
private fun ReliabilityContent(
    state: ReliabilityUiState,
    manualHelpVisible: Boolean,
    onBack: () -> Unit,
    onFix: (ReliabilityCheck) -> Unit,
    onOemAcknowledged: (Boolean) -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Insets sur le conteneur, avant verticalScroll (D30).
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
                text = stringResource(R.string.settings_reliability),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        Text(
            text = stringResource(R.string.reliability_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                // Un contrôle sans objet sur cet appareil n'a pas de ligne : mieux
                // vaut une liste courte et vraie qu'exhaustive et rassurante à tort.
                val shown = state.statuses.filter { it.state != CheckState.NOT_APPLICABLE }
                shown.forEachIndexed { index, status ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                    CheckRow(status = status, onFix = { onFix(status.check) })
                }
            }
        }

        if (state.hasOemScreen) {
            Spacer(Modifier.height(16.dp))
            OemCard(
                acknowledged = state.oemAcknowledged,
                manualHelpVisible = manualHelpVisible,
                onOpen = { onFix(ReliabilityCheck.OEM_AUTOSTART) },
                onAcknowledged = onOemAcknowledged,
            )
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = state.nextAlertTime
                    ?.let { stringResource(R.string.reliability_next_alarm, it) }
                    .orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = state.lastDeliveredAt
                    ?.let { stringResource(R.string.reliability_last_delivered, formatMoment(it)) }
                    ?: stringResource(R.string.reliability_never_delivered),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onTest,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Text(stringResource(R.string.reliability_test))
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CheckRow(status: ReliabilityStatus, onFix: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        StateDot(status.state)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(status.check.labelRes),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(status.state.labelRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (status.state == CheckState.ACTION_NEEDED) {
            Spacer(Modifier.width(8.dp))
            Button(onClick = onFix) {
                Text(stringResource(R.string.reliability_action_fix))
            }
        }
    }
}

@Composable
private fun StateDot(state: CheckState) {
    val color: Color = when (state) {
        CheckState.OK -> MaterialTheme.colorScheme.primary
        CheckState.ACTION_NEEDED -> MaterialTheme.colorScheme.error
        CheckState.UNKNOWN, CheckState.NOT_APPLICABLE -> MaterialTheme.colorScheme.outline
    }
    Spacer(
        Modifier
            .size(10.dp)
            .background(color, CircleShape),
    )
}

/**
 * Le démarrage automatique a sa propre carte : il n'est ni lisible ni accordable
 * par une API, seulement atteignable. L'utilisateur déclare lui-même l'avoir
 * réglé — c'est la seule façon d'arrêter de le lui redemander.
 */
@Composable
private fun OemCard(
    acknowledged: Boolean,
    manualHelpVisible: Boolean,
    onOpen: () -> Unit,
    onAcknowledged: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.reliability_autostart),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.reliability_autostart_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Affiché seulement quand l'ouverture a échoué : sinon c'est du bruit.
            if (manualHelpVisible) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.reliability_autostart_manual),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpen) {
                    Text(stringResource(R.string.reliability_action_fix))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.reliability_autostart_done),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.width(8.dp))
                Switch(checked = acknowledged, onCheckedChange = onAcknowledged)
            }
        }
    }
}

/** Chiffres occidentaux, comme partout ailleurs dans l'app. */
private fun formatMoment(epochMillis: Long): String = MOMENT_FORMATTER.format(
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()),
)

private val MOMENT_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.ROOT)

private val ReliabilityCheck.labelRes: Int
    @StringRes get() = when (this) {
        ReliabilityCheck.NOTIFICATIONS -> R.string.reliability_notifications
        ReliabilityCheck.EXACT_ALARMS -> R.string.reliability_exact_alarms
        ReliabilityCheck.BATTERY -> R.string.reliability_battery
        ReliabilityCheck.OEM_AUTOSTART -> R.string.reliability_autostart
        ReliabilityCheck.DELIVERY -> R.string.reliability_delivery
    }

private val CheckState.labelRes: Int
    @StringRes get() = when (this) {
        CheckState.OK -> R.string.reliability_state_ok
        CheckState.ACTION_NEEDED -> R.string.reliability_state_action
        CheckState.UNKNOWN, CheckState.NOT_APPLICABLE -> R.string.reliability_state_unknown
    }
