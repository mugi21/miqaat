package com.mohamed.miqaat.ui.quran

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mohamed.miqaat.R

/**
 * « La sourate du moment ».
 *
 * Ce que Miqaat peut faire et pas une application de récitation seule : la
 * suggestion sort des **horaires réels du jour** — al-Kahf pendant la journée du
 * vendredi, al-Mulk après l'Isha, Yā-Sīn entre le Fajr et le shurūq. Le ton reste
 * celui d'une proposition : une carte qu'on peut ignorer, jamais une modale.
 *
 * Teinte `tertiary` et non `primary` : la même règle que les jours de Ramadan
 * dans le calendrier — se distinguer du vert de la marque sans lui disputer
 * l'attention.
 */
@Composable
fun SuggestionCard(
    suggestion: SuggestionUi,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier.fillMaxWidth().clickable(onClick = onPlay),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.quran_suggestion_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = suggestion.surahName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = stringResource(suggestion.reason.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_play),
                contentDescription = stringResource(R.string.quran_player_play),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
