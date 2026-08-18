package com.mohamed.miqaat.ui.update

import android.text.format.Formatter
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mohamed.miqaat.miqaatApp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * « Y a-t-il une nouvelle version, et comment l'installer ? »
 *
 * Écran plutôt que dialogue (D44) : les notes de version tiennent plusieurs
 * paragraphes, le téléchargement doit survivre à un aller-retour vers un écran
 * système, et il faut une destination même quand tout est à jour.
 */
@Composable
fun UpdateScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val repository = context.miqaatApp.updateRepository
    val viewModel: UpdateViewModel = viewModel { UpdateViewModel(appContext, repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    // L'autorisation « sources inconnues » s'accorde sur un écran système : au
    // retour, elle a pu changer sans que rien ici ne l'ait su.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshOnResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    UpdateContent(
        state = state,
        onBack = onBack,
        onCheck = viewModel::checkNow,
        onDownload = viewModel::download,
        onCancel = viewModel::cancelDownload,
        onInstall = viewModel::install,
        onAllowUnknownSources = viewModel::openUnknownSourcesSettings,
        onSkip = viewModel::skipThisVersion,
        onOpenPage = viewModel::openReleasePage,
        onAutoCheckChanged = viewModel::setAutoCheckEnabled,
        modifier = modifier,
    )
}

@Composable
private fun UpdateContent(
    state: UpdateUiState,
    onBack: () -> Unit,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onAllowUnknownSources: () -> Unit,
    onSkip: () -> Unit,
    onOpenPage: () -> Unit,
    onAutoCheckChanged: (Boolean) -> Unit,
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
                text = stringResource(R.string.update_settings_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        VersionCard(state = state, onCheck = onCheck)

        if (state.updateAvailable) {
            Spacer(Modifier.height(16.dp))
            ReleaseNotesCard(state = state)
            Spacer(Modifier.height(16.dp))
            ActionsCard(
                state = state,
                onDownload = onDownload,
                onCancel = onCancel,
                onInstall = onInstall,
                onAllowUnknownSources = onAllowUnknownSources,
                onSkip = onSkip,
                onOpenPage = onOpenPage,
            )
        }

        state.errorRes?.let { error ->
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        AutoCheckCard(enabled = state.autoCheckEnabled, onChanged = onAutoCheckChanged)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun VersionCard(state: UpdateUiState, onCheck: () -> Unit) {
    Card {
        Text(
            text = stringResource(R.string.update_installed_version, state.installedName),
            style = MaterialTheme.typography.titleMedium,
        )
        if (state.updateAvailable && state.release != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.update_new_version, state.release.tag),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.update_up_to_date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = if (state.lastCheckAt > 0) {
                stringResource(R.string.update_last_checked, formatMoment(state.lastCheckAt))
            } else {
                stringResource(R.string.update_never_checked)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onCheck,
            enabled = state.phase != UpdatePhase.CHECKING,
        ) {
            Text(
                stringResource(
                    if (state.phase == UpdatePhase.CHECKING) {
                        R.string.update_checking
                    } else {
                        R.string.update_action_check
                    },
                ),
            )
        }
    }
}

@Composable
private fun ReleaseNotesCard(state: UpdateUiState) {
    Card {
        Text(
            text = stringResource(R.string.update_notes_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        // Markdown brut : le rendre demanderait une dépendance pour quelques
        // puces et deux titres. Le texte reste lisible tel quel.
        Text(
            text = state.release?.notes?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.update_no_notes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionsCard(
    state: UpdateUiState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onAllowUnknownSources: () -> Unit,
    onSkip: () -> Unit,
    onOpenPage: () -> Unit,
) {
    val context = LocalContext.current
    Card {
        when (state.phase) {
            UpdatePhase.DOWNLOADING -> {
                Text(
                    text = stringResource(R.string.update_downloading, state.percent),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.percent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.update_action_cancel))
                }
            }

            UpdatePhase.READY -> {
                // L'autorisation manque : on l'explique avant de proposer le bouton,
                // sinon l'utilisateur tape dans le vide.
                if (!state.canInstall) {
                    Text(
                        text = stringResource(R.string.update_unknown_sources_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.update_unknown_sources_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.manualHelpVisible) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.update_unknown_sources_manual),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onAllowUnknownSources) {
                        Text(stringResource(R.string.update_unknown_sources_title))
                    }
                } else {
                    Button(onClick = onInstall) {
                        Text(stringResource(R.string.update_action_install))
                    }
                }
            }

            UpdatePhase.IDLE, UpdatePhase.CHECKING -> {
                if (state.downloadable) {
                    Button(onClick = onDownload, enabled = state.phase == UpdatePhase.IDLE) {
                        Text(
                            stringResource(
                                R.string.update_action_download,
                                Formatter.formatShortFileSize(
                                    context,
                                    state.release?.apkSizeBytes ?: 0L,
                                ),
                            ),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Toujours offert : sur certaines surcouches, c'est le seul chemin qui marche.
            TextButton(onClick = onOpenPage) {
                Text(stringResource(R.string.update_action_browser))
            }
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.update_action_skip))
            }
        }
    }
}

/**
 * L'interrupteur vit ici et non dans les réglages : couper la vérification veut
 * dire « l'app ne contactera plus github.com », phrase qui ne tient pas dans le
 * sous-titre d'une ligne de réglages. Même raison que le témoin de démarrage
 * automatique, qui vit sur l'écran de fiabilité.
 */
@Composable
private fun AutoCheckCard(enabled: Boolean, onChanged: (Boolean) -> Unit) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.update_auto_check),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.update_auto_check_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = enabled, onCheckedChange = onChanged)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.update_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** La surface arrondie commune à toutes les cartes de l'écran. */
@Composable
private fun Card(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.Top,
            content = content,
        )
    }
}

/** Chiffres occidentaux, comme partout ailleurs dans l'app. */
private fun formatMoment(epochMillis: Long): String = MOMENT_FORMATTER.format(
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()),
)

private val MOMENT_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.ROOT)
