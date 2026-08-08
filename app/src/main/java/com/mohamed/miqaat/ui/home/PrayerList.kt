package com.mohamed.miqaat.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamed.miqaat.domain.model.PrayerName
import com.mohamed.miqaat.ui.labelRes

/**
 * Les six moments du jour dans une seule surface arrondie, sans ombre :
 * la hiérarchie vient des tons (prochaine en primaryContainer, passées atténuées).
 */
@Composable
fun PrayerList(prayers: List<PrayerRowUi>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            prayers.forEachIndexed { index, row ->
                if (row.isNext) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    ) {
                        PrayerRow(row = row)
                    }
                } else {
                    PrayerRow(row = row)
                }
                // Pas de trait autour de la ligne mise en évidence : sa carte suffit.
                val nextRow = prayers.getOrNull(index + 1)
                if (nextRow != null && !row.isNext && !nextRow.isNext) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrayerRow(row: PrayerRowUi, modifier: Modifier = Modifier) {
    val contentColor: Color = when {
        row.isNext -> MaterialTheme.colorScheme.onPrimaryContainer
        // Le shurūq n'est pas une prière : toujours discret.
        row.prayer == PrayerName.SUNRISE || row.isPast ->
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(row.prayer.labelRes),
            style = MaterialTheme.typography.titleMedium,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = row.time,
            style = MaterialTheme.typography.titleMedium.tabularNumbers(),
            fontWeight = if (row.isNext) FontWeight.SemiBold else null,
            color = contentColor,
        )
    }
}
