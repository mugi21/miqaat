package com.mohamed.miqaat.ui.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohamed.miqaat.R
import com.mohamed.miqaat.domain.model.Moshaf
import com.mohamed.miqaat.domain.model.Surah
import com.mohamed.miqaat.ui.home.tabularNumbers

/**
 * Les 114 sourates chez le récitateur ouvert.
 *
 * Celles que ce récitateur n'a pas enregistrées apparaissent **atténuées et non
 * cliquables** plutôt que masquées : la numérotation reste continue, et l'on
 * comprend que c'est le récitateur qui manque, pas l'application.
 */
fun LazyListScope.surahList(
    suwar: List<Surah>,
    moshaf: Moshaf,
    favoriteIds: Set<Int>,
    playingSurahId: Int?,
    onPlay: (Surah) -> Unit,
    onToggleFavorite: (Surah) -> Unit,
) {
    items(suwar, key = { it.id }) { surah ->
        SurahRow(
            surah = surah,
            available = moshaf.has(surah.id),
            isFavorite = surah.id in favoriteIds,
            isPlaying = surah.id == playingSurahId,
            onPlay = { onPlay(surah) },
            onToggleFavorite = { onToggleFavorite(surah) },
        )
    }
}

@Composable
private fun SurahRow(
    surah: Surah,
    available: Boolean,
    isFavorite: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val contentColor = when {
        !available -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        isPlaying -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (available) Modifier.clickable(onClick = onPlay) else Modifier)
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Text(
            text = surah.id.toString(),
            style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            // Une largeur fixe est ici sans danger : trois chiffres au plus, et
            // aucun texte traduit — la règle vise les mots, pas les numéros.
            modifier = Modifier.width(32.dp),
        )
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                text = surah.name,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
            Text(
                text = if (available) {
                    stringResource(
                        if (surah.makki) R.string.quran_surah_makki else R.string.quran_surah_madani,
                    )
                } else {
                    stringResource(R.string.quran_surah_unavailable)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (available) {
            FavoriteButton(isFavorite = isFavorite, onClick = onToggleFavorite)
        }
    }
}
