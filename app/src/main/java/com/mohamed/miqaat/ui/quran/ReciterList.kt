package com.mohamed.miqaat.ui.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.mohamed.miqaat.domain.model.Reciter
import com.mohamed.miqaat.domain.model.Surah

/**
 * Les ~130 récitateurs du catalogue, dans une liste paresseuse : les favoris
 * remontent dans une section à part, et le champ de recherche filtre le tout.
 *
 * Écrit en `LazyListScope` plutôt qu'en composable autonome pour partager la
 * même liste défilante que l'en-tête et la carte de suggestion — deux zones
 * défilantes imbriquées seraient à la fois illégales et désagréables.
 */
fun LazyListScope.reciterList(
    favorites: List<Reciter>,
    others: List<Reciter>,
    hasNoMatch: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (Reciter) -> Unit,
    onToggleFavorite: (Reciter) -> Unit,
) {
    item(key = "search") {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            label = { Text(stringResource(R.string.quran_search_reciter)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }

    if (hasNoMatch) {
        item(key = "empty") {
            Text(
                text = stringResource(R.string.quran_no_result),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
            )
        }
        return
    }

    // Les deux intertitres ne s'affichent que s'il y a des favoris : sans eux,
    // une seule liste n'a pas besoin d'être annoncée.
    if (favorites.isNotEmpty()) {
        item(key = "favorites-header") {
            ListSectionHeader(stringResource(R.string.quran_favorites), favorites.size)
        }
        items(favorites, key = { "fav-${it.id}" }) { reciter ->
            ReciterRow(reciter, true, { onSelect(reciter) }, { onToggleFavorite(reciter) })
        }
        item(key = "others-header") {
            ListSectionHeader(stringResource(R.string.quran_all_reciters), others.size)
        }
    }
    items(others, key = { it.id }) { reciter ->
        ReciterRow(reciter, false, { onSelect(reciter) }, { onToggleFavorite(reciter) })
    }
}

@Composable
private fun ReciterRow(
    reciter: Reciter,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val moshaf = reciter.defaultMoshaf
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = 20.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    ) {
        InitialBadge(reciter.name)
        Column(
            Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(
                text = reciter.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            moshaf?.let {
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Ne le dire que si l'enregistrement est incomplet : « 114 sur
                // 114 » serait du bruit sur la grande majorité des lignes.
                if (it.surahIds.size < Surah.COUNT) {
                    Text(
                        text = stringResource(
                            R.string.quran_surah_available,
                            it.surahIds.size,
                            Surah.COUNT,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
        FavoriteButton(isFavorite = isFavorite, onClick = onToggleFavorite)
    }
}

/**
 * La première lettre du nom dans une pastille : de quoi accrocher l'œil dans une
 * liste de cent trente lignes autrement uniformément textuelles. Pas de photo —
 * l'API n'en fournit pas, et aller en chercher ailleurs voudrait dire contacter
 * un second hôte (D41).
 */
@Composable
private fun InitialBadge(name: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.trim().take(1),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** Intertitre d'une section de liste, avec le nombre d'entrées qu'elle contient. */
@Composable
internal fun ListSectionHeader(title: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** L'étoile de favori, partagée par les récitateurs et les sourates. */
@Composable
internal fun FavoriteButton(isFavorite: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(
                if (isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star,
            ),
            contentDescription = stringResource(
                if (isFavorite) R.string.quran_favorite_remove else R.string.quran_favorite_add,
            ),
            tint = if (isFavorite) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
