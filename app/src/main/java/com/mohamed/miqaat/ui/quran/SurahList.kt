package com.mohamed.miqaat.ui.quran

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
    val favorites = suwar.filter { it.id in favoriteIds }
    if (favorites.isNotEmpty()) {
        item(key = "surah-favorites-header") {
            ListSectionHeader(stringResource(R.string.quran_favorites), favorites.size)
        }
        items(favorites, key = { "fav-${it.id}" }) { surah ->
            SurahRow(surah, moshaf.has(surah.id), true, surah.id == playingSurahId,
                { onPlay(surah) }, { onToggleFavorite(surah) })
        }
        item(key = "surah-all-header") {
            ListSectionHeader(stringResource(R.string.quran_all_surahs), suwar.size)
        }
    }

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
            .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    ) {
        SurahNumber(number = surah.id, highlighted = isPlaying, dimmed = !available)
        Column(
            Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(
                text = surah.name,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (available) {
                    // « مكية · 110 آيات » : l'origine et la longueur, les deux
                    // choses qu'on veut savoir avant de lancer une récitation.
                    stringResource(
                        if (surah.makki) R.string.quran_surah_makki else R.string.quran_surah_madani,
                    ) + SEPARATOR + pluralStringResource(
                        R.plurals.quran_ayahs, surah.ayahCount, surah.ayahCount,
                    )
                } else {
                    stringResource(R.string.quran_surah_unavailable)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isPlaying) {
            Icon(
                painter = painterResource(R.drawable.ic_play),
                contentDescription = stringResource(R.string.quran_now_playing),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        if (available) {
            FavoriteButton(isFavorite = isFavorite, onClick = onToggleFavorite)
        }
    }
}

/** Le point médian arabe convient aux trois langues et ne se traduit pas. */
private const val SEPARATOR = " · "

/**
 * Le numéro dans une **rosace girih** — deux carrés dont l'un tourné de 45°,
 * l'étoile à huit branches « khātam ». C'est le motif déjà employé par la
 * mosaïque du widget : la liste porte ainsi la même identité que le reste de
 * l'app, sans dépendre d'une image.
 */
@Composable
private fun SurahNumber(number: Int, highlighted: Boolean, dimmed: Boolean) {
    val stroke = when {
        dimmed -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        highlighted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val label = when {
        dimmed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        highlighted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
        Canvas(Modifier.size(40.dp)) { drawKhatam(stroke) }
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelMedium.tabularNumbers(),
            color = label,
        )
    }
}

private fun DrawScope.drawKhatam(color: Color) {
    val side = size.minDimension * 0.62f
    val offset = Offset((size.width - side) / 2f, (size.height - side) / 2f)
    val square = Path().apply {
        addRect(
            androidx.compose.ui.geometry.Rect(offset, androidx.compose.ui.geometry.Size(side, side)),
        )
    }
    val line = Stroke(width = size.minDimension * 0.045f)
    drawPath(square, color, style = line)
    rotate(degrees = 45f) { drawPath(square, color, style = line) }
}
