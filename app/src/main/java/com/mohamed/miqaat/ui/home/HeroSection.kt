package com.mohamed.miqaat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamed.miqaat.R
import com.mohamed.miqaat.ui.labelRes

/**
 * En-tête de l'écran : ville, dates, et la prochaine prière en grand
 * avec son compte à rebours. Peint son dégradé derrière la barre de statut.
 */
@Composable
fun HeroSection(state: HomeUiState, modifier: Modifier = Modifier) {
    // En sombre, le dégradé primaryContainer serait trop saturé : on part
    // d'une surface à peine plus claire que le fond.
    val gradientTop = if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(gradientTop, MaterialTheme.colorScheme.background),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.cityName,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // La date Hijri d'abord : c'est elle qui compte dans une app de prière.
        Text(
            text = state.hijriDate,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = state.gregorianDate,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.next?.let { next ->
            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(
                    if (next.isTomorrowFajr) R.string.tomorrow_fajr else R.string.next_prayer_title,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = stringResource(next.prayer.labelRes),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = next.time,
                style = MaterialTheme.typography.headlineMedium.tabularNumbers(),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = next.countdown,
                style = MaterialTheme.typography.headlineLarge.tabularNumbers(),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
