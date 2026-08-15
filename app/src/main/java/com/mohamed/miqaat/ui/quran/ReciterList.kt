package com.mohamed.miqaat.ui.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mohamed.miqaat.R
import com.mohamed.miqaat.domain.model.Reciter

/**
 * Les ~130 récitateurs du catalogue, dans une liste paresseuse : les favoris
 * remontent en tête, et le champ de recherche filtre le tout.
 *
 * Écrit en `LazyListScope` plutôt qu'en composable autonome pour partager la
 * même liste défilante que l'en-tête et la carte de suggestion — deux zones
 * défilantes imbriquées seraient à la fois illégales et désagréables.
 */
fun LazyListScope.reciterList(
    reciters: List<Reciter>,
    favoriteIds: Set<Int>,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }

    if (reciters.isEmpty()) {
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

    items(reciters, key = { it.id }) { reciter ->
        ReciterRow(
            reciter = reciter,
            isFavorite = reciter.id in favoriteIds,
            onSelect = { onSelect(reciter) },
            onToggleFavorite = { onToggleFavorite(reciter) },
        )
    }
}

@Composable
private fun ReciterRow(
    reciter: Reciter,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = reciter.name, style = MaterialTheme.typography.titleMedium)
            // La rīwāya n'est affichée que si le choix se pose : un seul
            // enregistrement, et la préciser n'apprend rien.
            reciter.defaultMoshaf?.takeIf { reciter.moshafs.size > 1 || it.name.isNotBlank() }?.let { moshaf ->
                Text(
                    text = moshaf.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FavoriteButton(isFavorite = isFavorite, onClick = onToggleFavorite)
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
