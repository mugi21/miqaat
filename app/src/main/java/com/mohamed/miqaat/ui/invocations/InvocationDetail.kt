package com.mohamed.miqaat.ui.invocations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamed.miqaat.R

/**
 * La lecture d'une invocation. Interligne large et taille confortable : un
 * texte arabe vocalisé demande de la place au-dessus et au-dessous de la ligne
 * pour ses diacritiques.
 */
@Composable
fun InvocationDetail(
    title: String,
    body: String,
    scheduleSummary: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding(),
    ) {
        ScreenHeader(title = title, onBack = onBack)

        Text(
            text = scheduleSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 28.dp),
        )

        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 19.sp,
                    lineHeight = 36.sp,
                ),
                textAlign = alignmentOf(body),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp),
        ) {
            Text(stringResource(R.string.invocation_edit))
        }

        Spacer(
            Modifier
                .height(24.dp)
                .navigationBarsPadding(),
        )
    }
}

/**
 * L'alignement suit le **texte**, pas la langue de l'interface : un dhikr arabe
 * doit rester calé à droite même quand l'app est en français ou en anglais.
 *
 * `TextAlign.Start` ne suffit pas — Compose le résout contre `LocalLayoutDirection`,
 * alors que le sens du paragraphe, lui, vient déjà du contenu. Sans cela, le
 * texte s'écrit bien de droite à gauche mais se colle au bord gauche, et la
 * ponctuation de fin de ligne paraît égarée.
 *
 * Même heuristique que le paragraphe : le **premier caractère fortement
 * directionnel** décide, les neutres (espaces, chiffres, ponctuation) sont ignorés.
 */
private fun alignmentOf(text: String): TextAlign {
    for (character in text) {
        when (Character.getDirectionality(character)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
            -> return TextAlign.Right

            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return TextAlign.Left
        }
    }
    return TextAlign.Start
}
