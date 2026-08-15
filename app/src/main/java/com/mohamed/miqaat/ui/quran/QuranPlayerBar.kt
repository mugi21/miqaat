package com.mohamed.miqaat.ui.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohamed.miqaat.R
import com.mohamed.miqaat.quran.QuranPlaybackUiState

/**
 * Le mini-lecteur, posé en barre du bas de l'activité : il survit donc au
 * changement d'écran, et l'on peut revenir à l'accueil consulter les horaires
 * sans couper la récitation.
 *
 * **Il ne compose rien quand rien ne joue** — même patron que `ReliabilityBanner` :
 * l'appelant n'a aucune condition à écrire, et aucun blanc n'apparaît en bas des
 * écrans le reste du temps.
 */
@Composable
fun QuranPlayerBar(
    state: QuranPlaybackUiState,
    onOpen: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isActive) return

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            // Filet de progression sur toute la largeur : il informe sans prendre
            // de place, et se met en indéterminé pendant le chargement du flux.
            if (state.isBuffering) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else if (state.durationMs > 0) {
                LinearProgressIndicator(
                    progress = { (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    .padding(start = 20.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.surahName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (state.isBuffering) {
                            stringResource(R.string.quran_buffering)
                        } else {
                            state.reciterName
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (state.isBuffering) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = onTogglePlayPause) {
                        Icon(
                            painter = painterResource(
                                if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                            ),
                            contentDescription = stringResource(
                                if (state.isPlaying) {
                                    R.string.quran_player_pause
                                } else {
                                    R.string.quran_player_play
                                },
                            ),
                        )
                    }
                }
                IconButton(onClick = onStop) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.quran_player_stop),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
