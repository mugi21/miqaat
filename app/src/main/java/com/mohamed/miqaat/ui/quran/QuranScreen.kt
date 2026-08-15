package com.mohamed.miqaat.ui.quran

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohamed.miqaat.R
import com.mohamed.miqaat.miqaatApp
import com.mohamed.miqaat.ui.invocations.ScreenHeader

/**
 * L'écran d'écoute : la liste des récitateurs, puis celle des sourates de celui
 * qu'on a ouvert, et le lecteur au-dessus dès que quelque chose joue.
 *
 * Les deux vues vivent dans le même écran, l'état dans le ViewModel : aucun
 * argument ne traverse la navigation, donc D7 (pas de librairie de navigation)
 * tient toujours — même raisonnement qu'au calendrier (D21) et aux adhkār.
 */
@Composable
fun QuranScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuranViewModel = quranViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()

    // La langue **affichée**, relue à chaque composition : le ViewModel survit à
    // la recréation d'activité que provoque un changement de langue, il ne peut
    // donc pas la retenir lui-même.
    val languageTag = LocalConfiguration.current.locales[0].language
    LaunchedEffect(languageTag) { viewModel.setLanguage(languageTag) }

    // Enregistré après celui de MainActivity, donc prioritaire : depuis les
    // sourates, « retour » ramène aux récitateurs et non à l'accueil.
    BackHandler(enabled = state.selectedReciterId != null) { viewModel.clearReciter() }

    val reciter = state.selectedReciter
    val moshaf = state.selectedMoshaf

    Column(
        modifier = modifier
            .fillMaxSize()
            // Insets sur le conteneur, avant la liste défilante (D30).
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenHeader(
            title = reciter?.name ?: stringResource(R.string.quran_title),
            onBack = { if (reciter != null) viewModel.clearReciter() else onBack() },
        )

        when {
            state.failedToLoad -> CatalogError(onRetry = { viewModel.refresh(force = true) })

            state.catalog.isEmpty && state.loading -> CatalogLoading()

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                if (playback.isActive) {
                    item(key = "player") {
                        QuranPlayerCard(
                            state = playback,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onPrevious = viewModel::previous,
                            onNext = viewModel::next,
                            onSeekTo = viewModel::seekTo,
                            onSeekBy = viewModel::seekBy,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                }

                if (reciter == null || moshaf == null) {
                    state.suggestion?.let { suggestion ->
                        item(key = "suggestion") {
                            SuggestionCard(
                                suggestion = suggestion,
                                onPlay = viewModel::playSuggestion,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                    }
                    item(key = "section") {
                        SectionLabel(stringResource(R.string.quran_choose_reciter))
                    }
                    reciterList(
                        favorites = state.favoriteReciters,
                        others = state.otherReciters,
                        hasNoMatch = state.hasNoMatch,
                        query = state.query,
                        onQueryChange = viewModel::setQuery,
                        onSelect = { viewModel.selectReciter(it.id) },
                        onToggleFavorite = { viewModel.toggleReciterFavorite(it.id) },
                    )
                } else {
                    item(key = "section") {
                        SectionLabel(stringResource(R.string.quran_choose_surah))
                    }
                    surahList(
                        suwar = state.catalog.suwar,
                        moshaf = moshaf,
                        favoriteIds = state.favorites.surahIds,
                        playingSurahId = playback.surahId,
                        onPlay = { viewModel.play(it.id) },
                        onToggleFavorite = { viewModel.toggleSurahFavorite(it.id) },
                    )
                }

                item(key = "hint") {
                    Text(
                        text = stringResource(R.string.quran_streaming_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun quranViewModel(): QuranViewModel {
    val app = LocalContext.current.miqaatApp
    return viewModel {
        QuranViewModel(
            repository = app.quranCatalogRepository,
            preferences = app.quranPreferences,
            player = app.quranPlayer,
            locationRepository = app.locationRepository,
            settingsRepository = app.settingsRepository,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
    )
}

@Composable
private fun CatalogLoading() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp),
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.quran_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Le seul cas bloquant : rien en cache **et** le réseau a échoué. */
@Composable
private fun CatalogError(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp),
    ) {
        Text(
            text = stringResource(R.string.quran_offline_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.quran_offline_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.quran_retry)) }
    }
}
