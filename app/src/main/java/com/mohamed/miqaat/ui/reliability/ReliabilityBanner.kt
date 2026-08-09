package com.mohamed.miqaat.ui.reliability

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mohamed.miqaat.R
import com.mohamed.miqaat.data.reliability.ReliabilityInspector
import com.mohamed.miqaat.data.reliability.ReliabilityLog
import com.mohamed.miqaat.domain.reliability.ReliabilityVerdict

/**
 * L'avertissement d'accueil : « les notifications risquent de ne pas arriver ».
 *
 * Il n'apparaît que sur du **certain et du critique**
 * ([ReliabilityVerdict.shouldWarnOnHome]), jamais sur un état illisible : sans
 * cette règle, tout possesseur d'un appareil à surcouche verrait un
 * avertissement permanent qu'aucune action ne pourrait éteindre. Et « plus tard »
 * le fait taire deux semaines — l'écran de fiabilité reste accessible depuis les
 * réglages entre-temps.
 *
 * Composable autonome plutôt qu'un champ de `HomeUiState` : le diagnostic ne
 * regarde pas les horaires de prière, il n'a rien à faire dans leur ViewModel.
 */
@Composable
fun ReliabilityBanner(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }

    // Réévalué à chaque retour au premier plan : l'utilisateur revient peut-être
    // d'un écran système où il vient de corriger le problème.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                visible = ReliabilityVerdict.shouldWarnOnHome(
                    statuses = ReliabilityInspector.inspect(context),
                    dismissedUntil = ReliabilityLog.dismissedUntil(context),
                    now = System.currentTimeMillis(),
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!visible) return

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(R.string.home_reliability_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpen) {
                    Text(stringResource(R.string.reliability_action_fix))
                }
                TextButton(
                    onClick = {
                        ReliabilityLog.snoozeBanner(context)
                        visible = false
                    },
                ) {
                    Text(stringResource(R.string.action_later))
                }
            }
        }
    }
}
