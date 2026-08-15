package com.mohamed.miqaat.data.quran

import com.mohamed.miqaat.data.db.QuranDao
import com.mohamed.miqaat.data.db.QuranFavoriteDao
import com.mohamed.miqaat.data.db.QuranFavoriteEntity
import com.mohamed.miqaat.data.db.QuranMoshafEntity
import com.mohamed.miqaat.data.db.QuranReciterEntity
import com.mohamed.miqaat.data.db.QuranSurahEntity
import com.mohamed.miqaat.domain.model.Moshaf
import com.mohamed.miqaat.domain.model.Reciter
import com.mohamed.miqaat.domain.model.Surah
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Le catalogue tel que l'écran le consomme. Vide tant que rien n'a été chargé. */
data class QuranCatalog(
    val reciters: List<Reciter> = emptyList(),
    val suwar: List<Surah> = emptyList(),
) {
    val isEmpty: Boolean get() = reciters.isEmpty() || suwar.isEmpty()

    fun surahName(id: Int): String? = suwar.firstOrNull { it.id == id }?.name

    fun moshaf(id: Int): Moshaf? =
        reciters.firstNotNullOfOrNull { reciter -> reciter.moshafs.firstOrNull { it.id == id } }

    fun reciterOf(moshafId: Int): Reciter? =
        reciters.firstOrNull { reciter -> reciter.moshafs.any { it.id == moshafId } }
}

data class QuranFavorites(
    val reciterIds: Set<Int> = emptySet(),
    val surahIds: Set<Int> = emptySet(),
)

/** Ce qu'a donné une tentative de rafraîchissement, pour que l'écran sache quoi dire. */
enum class CatalogRefresh { UP_TO_DATE, REFRESHED, FAILED }

/**
 * Le catalogue mp3quran : un cache Room lu en `Flow`, rempli depuis le réseau
 * quand il est vide, périmé, ou dans une autre langue que celle affichée.
 *
 * Parcourir marche donc hors ligne dès le premier chargement réussi ; **écouter**,
 * lui, demande toujours le réseau — c'est du streaming, et l'écran doit le dire.
 */
class QuranCatalogRepository(
    private val dao: QuranDao,
    private val favoriteDao: QuranFavoriteDao,
    private val preferences: QuranPreferences,
    private val api: Mp3QuranApi = Mp3QuranApi(),
) {

    val catalogFlow: Flow<QuranCatalog> = combine(
        dao.observeReciters(),
        dao.observeMoshafs(),
        dao.observeSuwar(),
    ) { reciters, moshafs, suwar ->
        val byReciter = moshafs.groupBy { it.reciterId }
        QuranCatalog(
            reciters = reciters.map { reciter ->
                Reciter(
                    id = reciter.id,
                    name = reciter.name,
                    letter = reciter.letter,
                    moshafs = byReciter[reciter.id]
                        ?.map { it.toDomain() }
                        .orEmpty(),
                )
            }.filter { it.moshafs.isNotEmpty() },
            suwar = suwar.map { Surah(id = it.id, name = it.name, makki = it.makki) },
        )
    }

    val favoritesFlow: Flow<QuranFavorites> = favoriteDao.observeAll().map { rows ->
        QuranFavorites(
            reciterIds = rows.filter { it.type == QuranFavoriteEntity.TYPE_RECITER }
                .map { it.refId }.toSet(),
            surahIds = rows.filter { it.type == QuranFavoriteEntity.TYPE_SURAH }
                .map { it.refId }.toSet(),
        )
    }

    /**
     * Recharge le catalogue si nécessaire.
     *
     * Trois raisons de le refaire : rien en cache, cache d'une autre langue
     * (les noms viennent traduits de l'API), ou cache plus vieux que
     * [CACHE_MAX_AGE_MS] — mp3quran ajoute des récitateurs régulièrement, mais
     * pas au point de justifier un appel par ouverture.
     *
     * @param force ignore la fraîcheur : c'est le bouton « réessayer ».
     */
    suspend fun refreshIfNeeded(
        language: Mp3QuranLanguage,
        force: Boolean = false,
        now: Long = System.currentTimeMillis(),
    ): CatalogRefresh {
        if (!force && !needsRefresh(language, now)) return CatalogRefresh.UP_TO_DATE

        // Les deux appels doivent réussir ensemble : un catalogue de récitateurs
        // sans noms de sourates n'afficherait que des numéros.
        val reciters = api.reciters(language) ?: return CatalogRefresh.FAILED
        val suwar = api.suwar(language) ?: return CatalogRefresh.FAILED
        if (reciters.isEmpty() || suwar.isEmpty()) return CatalogRefresh.FAILED

        dao.replaceReciters(
            reciters = reciters.map { it.toEntity(language.code) },
            moshafs = reciters.flatMap { reciter -> reciter.moshafs.map { it.toEntity() } },
        )
        dao.replaceSuwar(suwar.map { it.toEntity(language.code) })
        preferences.setCatalogLoaded(language.code, now)
        return CatalogRefresh.REFRESHED
    }

    private suspend fun needsRefresh(language: Mp3QuranLanguage, now: Long): Boolean {
        if (dao.reciterCount() == 0 || dao.surahCount() == 0) return true
        val (cachedLanguage, loadedAt) = preferences.cachedCatalog()
        if (cachedLanguage != language.code) return true
        return now - loadedAt > CACHE_MAX_AGE_MS
    }

    suspend fun toggleReciterFavorite(reciterId: Int, favorite: Boolean) =
        toggleFavorite(QuranFavoriteEntity.TYPE_RECITER, reciterId, favorite)

    suspend fun toggleSurahFavorite(surahId: Int, favorite: Boolean) =
        toggleFavorite(QuranFavoriteEntity.TYPE_SURAH, surahId, favorite)

    private suspend fun toggleFavorite(type: String, refId: Int, favorite: Boolean) {
        if (favorite) {
            favoriteDao.add(QuranFavoriteEntity(type = type, refId = refId))
        } else {
            favoriteDao.remove(type, refId)
        }
    }

    /**
     * Lecture ponctuelle, pour le service de lecture : il tourne dans le même
     * processus mais sans écran, donc sans `Flow` collecté.
     */
    suspend fun moshafById(id: Int): Moshaf? = dao.moshafById(id)?.toDomain()

    suspend fun surahNames(): Map<Int, String> = dao.suwar().associate { it.id to it.name }

    private companion object {
        /** Une semaine : le catalogue bouge, mais lentement. */
        val CACHE_MAX_AGE_MS = TimeUnit.DAYS.toMillis(7)
    }
}

private fun Reciter.toEntity(languageCode: String) = QuranReciterEntity(
    id = id,
    name = name,
    letter = letter,
    language = languageCode,
)

private fun Moshaf.toEntity() = QuranMoshafEntity(
    id = id,
    reciterId = reciterId,
    name = name,
    server = server,
    // La liste est regardée brute : le découpage appartient au domaine, et la
    // garder telle quelle permet de la relire à l'œil dans `sqlite3`.
    surahList = surahIds.joinToString(","),
)

private fun Surah.toEntity(languageCode: String) = QuranSurahEntity(
    id = id,
    name = name,
    makki = makki,
    language = languageCode,
)

private fun QuranMoshafEntity.toDomain() = Moshaf(
    id = id,
    reciterId = reciterId,
    name = name,
    server = server,
    surahIds = Moshaf.parseSurahList(surahList),
)
