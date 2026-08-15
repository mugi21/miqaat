package com.mohamed.miqaat.ui.quran

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohamed.miqaat.R
import com.mohamed.miqaat.quran.QuranPlaybackUiState
import com.mohamed.miqaat.ui.home.tabularNumbers

/** Le pas des deux boutons de recul et d'avance. */
private const val SEEK_STEP_MS = 10_000L

/**
 * Le lecteur complet, en tête de l'écran du Coran quand quelque chose joue.
 * Le mini-lecteur de la barre du bas en est la version repliée.
 */
@Composable
fun QuranPlayerCard(
    state: QuranPlaybackUiState,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = state.surahName,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                text = state.reciterName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))
            SeekBar(state = state, onSeekTo = onSeekTo)

            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = onPrevious, enabled = state.hasPrevious) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous),
                        contentDescription = stringResource(R.string.quran_player_previous),
                    )
                }
                IconButton(onClick = { onSeekBy(-SEEK_STEP_MS) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_replay_10),
                        contentDescription = stringResource(R.string.quran_player_rewind),
                    )
                }
                // Le bouton principal, plus grand et sur la couleur de marque :
                // c'est celui qu'on vise sans regarder.
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                ) {
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
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                IconButton(onClick = { onSeekBy(SEEK_STEP_MS) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_forward_10),
                        contentDescription = stringResource(R.string.quran_player_forward),
                    )
                }
                IconButton(onClick = onNext, enabled = state.hasNext) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = stringResource(R.string.quran_player_next),
                    )
                }
            }

            if (state.failed) {
                Text(
                    text = stringResource(R.string.quran_error_playback),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SeekBar(state: QuranPlaybackUiState, onSeekTo: (Long) -> Unit) {
    // Pendant le glissement, la barre suit le doigt et non le lecteur : sans ça
    // elle sauterait en arrière à chaque tick tant que le seek n'a pas abouti.
    var dragged by remember { mutableFloatStateOf(-1f) }
    val duration = state.durationMs.takeIf { it > 0 }

    Slider(
        value = when {
            dragged >= 0f -> dragged
            duration != null -> (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)
            else -> 0f
        },
        onValueChange = { dragged = it },
        onValueChangeFinished = {
            duration?.let { onSeekTo((dragged * it).toLong()) }
            dragged = -1f
        },
        // Durée encore inconnue (flux en cours d'ouverture) : rien à viser.
        enabled = duration != null,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = formatPlaybackTime(state.positionMs),
            style = MaterialTheme.typography.bodySmall.tabularNumbers(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatPlaybackTime(state.durationMs),
            style = MaterialTheme.typography.bodySmall.tabularNumbers(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
