package com.mohamed.miqaat.ui.update

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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamed.miqaat.R
import com.mohamed.miqaat.data.update.UpdateLog
import com.mohamed.miqaat.domain.update.ReleaseInfo
import com.mohamed.miqaat.domain.update.UpdateVerdict
import com.mohamed.miqaat.miqaatApp

/**
 * La note d'accueil : « une nouvelle version est disponible ».
 *
 * Même patron que [ReliabilityBanner][com.mohamed.miqaat.ui.reliability.ReliabilityBanner] —
 * composable autonome, aucun champ dans `HomeUiState` : la veille des releases ne
 * regarde pas les horaires de prière, elle n'a rien à faire dans leur ViewModel.
 * Une seule adaptation, et elle est nécessaire : la source est **asynchrone**, donc
 * l'état se recalcule à la fois sur `ON_RESUME` (report, version ignorée, opt-out
 * — des préférences, non réactives) et à chaque émission du repository (une
 * vérification qui aboutit pendant qu'on regarde l'accueil).
 *
 * Couleur `tertiaryContainer` et non `errorContainer` : une mise à jour disponible
 * n'est pas une erreur, et le ton tertiaire est déjà celui de l'informatif non
 * urgent dans cette app (suggestion du Coran, encart de Ramadan).
 */
@Composable
fun UpdateBanner(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = context.miqaatApp.updateRepository
    val release by repository.latest.collectAsStateWithLifecycle()
    var visible by remember { mutableStateOf(false) }

    fun evaluate(candidate: ReleaseInfo?) {
        val installed = repository.installed()
        visible = UpdateVerdict.shouldShowOnHome(
            release = candidate,
            installedName = installed.name,
            installedCode = installed.code,
            skippedTag = UpdateLog.skippedTag(context),
            snoozedUntil = UpdateLog.snoozedUntil(context),
            enabled = UpdateLog.autoCheckEnabled(context),
            now = System.currentTimeMillis(),
        )
    }

    LaunchedEffect(release) { evaluate(release) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, release) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) evaluate(release)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Invisible, la fonction ne compose rien : c'est ce qui permet à l'appelant de
    // porter les marges par `modifier` sans laisser de blanc.
    if (!visible) return
    val tag = release?.tag ?: return

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(R.string.update_home_banner, tag),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpen) {
                    Text(stringResource(R.string.update_action_view))
                }
                TextButton(
                    onClick = {
                        UpdateLog.snooze(context)
                        visible = false
                    },
                ) {
                    Text(stringResource(R.string.action_later))
                }
            }
        }
    }
}
