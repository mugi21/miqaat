package com.mohamed.miqaat.ui.quran

import com.mohamed.miqaat.data.quran.QuranCatalog
import com.mohamed.miqaat.data.quran.QuranFavorites
import com.mohamed.miqaat.domain.QuranSuggestion
import com.mohamed.miqaat.domain.model.Moshaf
import com.mohamed.miqaat.domain.model.Reciter

/** La sourate du moment, telle que la carte l'affiche. */
data class SuggestionUi(
    val surahId: Int,
    val surahName: String,
    val reason: QuranSuggestion.Reason,
)

data class QuranUiState(
    val loading: Boolean = true,
    /** Le catalogue est vide **et** le réseau a échoué : c'est le seul cas bloquant. */
    val failedToLoad: Boolean = false,
    val catalog: QuranCatalog = QuranCatalog(),
    val favorites: QuranFavorites = QuranFavorites(),
    val query: String = "",
    val selectedReciterId: Int? = null,
    val selectedMoshafId: Int? = null,
    val suggestion: SuggestionUi? = null,
) {
    val selectedReciter: Reciter?
        get() = catalog.reciters.firstOrNull { it.id == selectedReciterId }

    val selectedMoshaf: Moshaf?
        get() = selectedReciter?.let { reciter ->
            reciter.moshafs.firstOrNull { it.id == selectedMoshafId } ?: reciter.defaultMoshaf
        }

    /**
     * Les récitateurs à afficher, **séparés en deux groupes** : les favoris
     * d'abord — sans quoi une liste de cent trente noms serait inutilisable —
     * puis le reste, chacun dans l'ordre du catalogue. La recherche filtre les
     * deux. Deux listes plutôt qu'une seule concaténée : c'est ce qui permet à
     * l'écran de poser un intertitre entre elles.
     */
    val favoriteReciters: List<Reciter> get() = matchingReciters.first

    val otherReciters: List<Reciter> get() = matchingReciters.second

    val hasNoMatch: Boolean
        get() = favoriteReciters.isEmpty() && otherReciters.isEmpty()

    private val matchingReciters: Pair<List<Reciter>, List<Reciter>>
        get() {
            val needle = query.trim()
            val matching = if (needle.isEmpty()) {
                catalog.reciters
            } else {
                catalog.reciters.filter { it.name.contains(needle, ignoreCase = true) }
            }
            return matching.partition { it.id in favorites.reciterIds }
        }
}
