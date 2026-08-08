package com.mohamed.miqaat.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mohamed.miqaat.R
import com.mohamed.miqaat.ui.theme.SplashGradientBottom
import com.mohamed.miqaat.ui.theme.SplashGradientTop
import com.mohamed.miqaat.ui.theme.SplashOnBackground

/**
 * L'écran de démarrage : le logo, le nom de l'app et ce à quoi elle sert.
 *
 * Il prend le relais de l'écran système d'Android 12+ (values-v31/themes.xml),
 * qui affiche le même logo sur le même vert mais ne sait pas porter de texte —
 * et il tient lieu d'écran de démarrage tout court en dessous d'Android 12, où
 * il n'y en a pas. D'où le fond vert du `windowBackground` : les deux moitiés
 * s'enchaînent sans changement de couleur.
 *
 * Le logo réutilisé est l'avant-plan de l'icône adaptative, blanc et déjà marge
 * comprise : une seule source pour le dessin, quel que soit l'endroit où il sort.
 *
 * Purement décoratif et sans état : la durée d'affichage est décidée par
 * `MainActivity`, qui le superpose à l'accueil pendant que celui-ci se compose.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(SplashGradientTop, SplashGradientBottom)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                // Décoratif : le nom de l'app est juste en dessous, en toutes lettres.
                contentDescription = null,
                modifier = Modifier.size(LOGO_SIZE),
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = SplashOnBackground,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.titleSmall,
                color = SplashOnBackground.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 288dp : la taille exacte à laquelle Android 12+ dessine
 * `windowSplashScreenAnimatedIcon`. Le logo garde donc la même taille des deux
 * côtés du relais — sans quoi il rapetisse d'un tiers en passant de l'écran
 * système au nôtre.
 */
private val LOGO_SIZE = 288.dp
