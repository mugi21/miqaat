package com.mohamed.miqaat.ui.quran

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.miqaat.data.location.LocationRepository
import com.mohamed.miqaat.data.quran.CatalogRefresh
import com.mohamed.miqaat.data.quran.Mp3QuranLanguage
import com.mohamed.miqaat.data.quran.QuranCatalog
import com.mohamed.miqaat.data.quran.QuranCatalogRepository
import com.mohamed.miqaat.data.quran.QuranPreferences
import com.mohamed.miqaat.data.settings.SettingsRepository
import com.mohamed.miqaat.domain.PrayerTimesCalculator
import com.mohamed.miqaat.domain.QuranAudio
import com.mohamed.miqaat.domain.QuranSuggestion
import com.mohamed.miqaat.domain.effectiveMethod
import com.mohamed.miqaat.domain.model.Moshaf
import com.mohamed.miqaat.quran.QuranMediaItems
import com.mohamed.miqaat.quran.QuranPlayerConnection
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuranViewModel(
    private val repository: QuranCatalogRepository,
    private val preferences: QuranPreferences,
    private val player: QuranPlayerConnection,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
    /** Code ISO-639-1 de la langue affichée, pour demander le catalogue traduit. */
    private val languageTag: String?,
    private val calculator: PrayerTimesCalculator = PrayerTimesCalculator(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuranUiState())
    val uiState: StateFlow<QuranUiState> = _uiState.asStateFlow()

    val playback = player.state

    init {
        player.connect()
        viewModelScope.launch {
            repository.catalogFlow.collect { catalog ->
                _uiState.update {
                    it.copy(catalog = catalog, suggestion = suggestionOf(catalog))
                }
            }
        }
        viewModelScope.launch {
            repository.favoritesFlow.collect { favorites ->
                _uiState.update { it.copy(favorites = favorites) }
            }
        }
        refresh()
    }

    /** @param force le bouton « réessayer » : ignore la fraîcheur du cache. */
    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, failedToLoad = false) }
            val result = repository.refreshIfNeeded(Mp3QuranLanguage.forTag(languageTag), force)
            _uiState.update {
                it.copy(
                    loading = false,
                    // Un échec ne bloque que si l'on n'a rien à montrer : avec un
                    // cache en place, l'écran reste utilisable hors ligne.
                    failedToLoad = result == CatalogRefresh.FAILED && it.catalog.isEmpty,
                )
            }
        }
    }

    fun setQuery(query: String) = _uiState.update { it.copy(query = query) }

    fun selectReciter(reciterId: Int) = _uiState.update { state ->
        val reciter = state.catalog.reciters.firstOrNull { it.id == reciterId }
        state.copy(
            selectedReciterId = reciterId,
            selectedMoshafId = reciter?.defaultMoshaf?.id,
        )
    }

    fun selectMoshaf(moshafId: Int) = _uiState.update { it.copy(selectedMoshafId = moshafId) }

    fun clearReciter() = _uiState.update { it.copy(selectedReciterId = null, selectedMoshafId = null) }

    fun toggleReciterFavorite(reciterId: Int) {
        val favorite = reciterId !in _uiState.value.favorites.reciterIds
        viewModelScope.launch { repository.toggleReciterFavorite(reciterId, favorite) }
    }

    fun toggleSurahFavorite(surahId: Int) {
        val favorite = surahId !in _uiState.value.favorites.surahIds
        viewModelScope.launch { repository.toggleSurahFavorite(surahId, favorite) }
    }

    /** Lance une sourate chez le récitateur ouvert. */
    fun play(surahId: Int) {
        val state = _uiState.value
        playFrom(state.selectedMoshaf ?: return, surahId, state.catalog)
    }

    /**
     * La carte « sourate du moment » : elle n'impose pas de choisir un récitateur
     * d'abord. On prend celui qui est ouvert, sinon le dernier écouté, sinon un
     * favori, sinon le premier du catalogue — le premier, dans cet ordre, qui
     * possède réellement cette sourate.
     */
    fun playSuggestion() {
        val state = _uiState.value
        val surahId = state.suggestion?.surahId ?: return
        val moshaf = preferredMoshafFor(surahId, state) ?: return
        playFrom(moshaf, surahId, state.catalog)
    }

    private fun preferredMoshafFor(surahId: Int, state: QuranUiState): Moshaf? {
        val candidates = sequence {
            yield(state.selectedMoshaf)
            yield(state.catalog.moshaf(preferences.current().moshafId))
            yieldAll(
                state.catalog.reciters
                    .filter { it.id in state.favorites.reciterIds }
                    .map { it.defaultMoshaf },
            )
            yieldAll(state.catalog.reciters.map { it.defaultMoshaf })
        }
        return candidates.filterNotNull().firstOrNull { it.has(surahId) }
    }

    private fun playFrom(moshaf: Moshaf, surahId: Int, catalog: QuranCatalog) {
        val reciter = catalog.reciterOf(moshaf.id) ?: return
        val queue = QuranAudio.queueFrom(moshaf, surahId)
        if (queue.isEmpty()) return
        player.play(
            items = QuranMediaItems.build(
                moshaf = moshaf,
                reciterName = reciter.name,
                surahIds = queue,
                surahNames = catalog.suwar.associate { it.id to it.name },
            ),
            startIndex = 0,
        )
        viewModelScope.launch { preferences.setNowPlaying(reciter.id, moshaf.id, surahId) }
    }

    fun togglePlayPause() = player.togglePlayPause()

    fun next() { player.next() }

    fun previous() { player.previous() }

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun seekBy(deltaMs: Long) = player.seekBy(deltaMs)

    fun stop() = player.stop()

    /**
     * La sourate du moment, à partir des horaires **réels** du jour et de la
     * position de l'utilisateur — c'est ce qui distingue la suggestion d'une
     * simple règle d'horloge. Recalculée à chaque arrivée du catalogue, ce qui
     * suffit : l'écran ne reste pas ouvert des heures.
     */
    private fun suggestionOf(catalog: QuranCatalog): SuggestionUi? {
        if (catalog.suwar.isEmpty()) return null
        val location = locationRepository.currentLocation()
        val settings = settingsRepository.current()
        val now = ZonedDateTime.now(location.zoneId)
        val today = calculator.calculate(
            location.latitude, location.longitude, now.toLocalDate(), location.zoneId,
            settings.effectiveMethod(location.countryCode), settings.madhab, settings.adjustments,
        )
        val suggestion = QuranSuggestion.suggest(now, today)
        return SuggestionUi(
            surahId = suggestion.surahId,
            surahName = catalog.surahName(suggestion.surahId) ?: return null,
            reason = suggestion.reason,
        )
    }
}
