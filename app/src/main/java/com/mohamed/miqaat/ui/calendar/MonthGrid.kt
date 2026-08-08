package com.mohamed.miqaat.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohamed.miqaat.domain.DAYS_PER_WEEK
import com.mohamed.miqaat.ui.home.tabularNumbers
import java.time.LocalDate

/**
 * La grille du mois : une ligne d'en-tête, puis les semaines.
 *
 * Pas de `LazyVerticalGrid` — la grille tient toujours en six lignes au plus et
 * vit dans une colonne déjà défilante, où un conteneur défilant imbriqué serait
 * à la fois inutile et source d'ennuis de mesure.
 */
@Composable
fun MonthGrid(
    weekdayLabels: List<String>,
    cells: List<CalendarDayUi?>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp),
                )
            }
        }
        cells.chunked(DAYS_PER_WEEK).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                    ) {
                        if (day != null) DayCell(day = day, onClick = { onDayClick(day.date) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: CalendarDayUi, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    // Le Ramadan se signale par un fond bleuté : il reste lisible sous la
    // sélection verte, et ne fait donc jamais concurrence à celle-ci.
    val background = when {
        day.isSelected -> colors.primary
        day.isRamadan -> colors.tertiaryContainer
        else -> Color.Transparent
    }
    val content = when {
        day.isSelected -> colors.onPrimary
        day.isRamadan -> colors.onTertiaryContainer
        else -> colors.onSurface
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            // Aujourd'hui garde un liseré même quand il est sélectionné ailleurs.
            .then(
                if (day.isToday && !day.isSelected) {
                    Modifier.border(1.5.dp, colors.primary, RoundedCornerShape(14.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = day.gregorianDay,
                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                fontWeight = if (day.isSelected || day.isToday) FontWeight.SemiBold else null,
                color = content,
            )
            Text(
                text = day.hijriDay,
                style = MaterialTheme.typography.labelSmall.tabularNumbers(),
                color = content.copy(alpha = 0.7f),
            )
        }
    }
}
