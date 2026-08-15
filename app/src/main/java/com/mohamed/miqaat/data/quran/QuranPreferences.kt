package com.mohamed.miqaat.data.quran

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.quranDataStore: DataStore<Preferences> by preferencesDataStore("quran")

/** Où l'on en était : de quoi rouvrir l'app sur la sourate qu'on écoutait. */
data class QuranPlaybackState(
    val moshafId: Int = NONE,
    val reciterId: Int = NONE,
    val surahId: Int = NONE,
    val positionMs: Long = 0,
) {
    val hasSomething: Boolean get() = moshafId != NONE && surahId != NONE

    companion object {
        const val NONE = 0
    }
}

/**
 * Les réglages du lecteur, dans un DataStore **à part** de `settings`.
 *
 * Deux magasins et non un seul parce que les rythmes d'écriture n'ont rien à
 * voir : les réglages de prière changent quelques fois dans la vie de l'app, la
 * position de lecture à chaque pause. Les mêler ferait réémettre tous les Flow
 * de réglages — et donc replanifier l'alarme — à chaque fois qu'on met la
 * récitation en pause.
 */
class QuranPreferences(context: Context) {

    private val dataStore = context.quranDataStore

    @Volatile
    private var memory: QuranPlaybackState? = null

    val playbackFlow: Flow<QuranPlaybackState> =
        dataStore.data.map { it.toPlayback().also { state -> memory = state } }

    fun current(): QuranPlaybackState =
        memory ?: runBlocking { dataStore.data.first().toPlayback().also { memory = it } }

    suspend fun setNowPlaying(reciterId: Int, moshafId: Int, surahId: Int) {
        dataStore.edit {
            it[KEY_RECITER] = reciterId
            it[KEY_MOSHAF] = moshafId
            it[KEY_SURAH] = surahId
            it[KEY_POSITION] = 0
        }.also { memory = it.toPlayback() }
    }

    suspend fun setPosition(surahId: Int, positionMs: Long) {
        dataStore.edit {
            it[KEY_SURAH] = surahId
            it[KEY_POSITION] = positionMs.coerceAtLeast(0)
        }.also { memory = it.toPlayback() }
    }

    /** Langue et date du dernier catalogue chargé : leur couple décide du rafraîchissement. */
    suspend fun cachedCatalog(): Pair<String?, Long> {
        val preferences = dataStore.data.first()
        return preferences[KEY_CATALOG_LANGUAGE] to (preferences[KEY_CATALOG_AT] ?: 0L)
    }

    suspend fun setCatalogLoaded(languageCode: String, atMillis: Long) {
        dataStore.edit {
            it[KEY_CATALOG_LANGUAGE] = languageCode
            it[KEY_CATALOG_AT] = atMillis
        }
    }

    private fun Preferences.toPlayback() = QuranPlaybackState(
        moshafId = this[KEY_MOSHAF] ?: QuranPlaybackState.NONE,
        reciterId = this[KEY_RECITER] ?: QuranPlaybackState.NONE,
        surahId = this[KEY_SURAH] ?: QuranPlaybackState.NONE,
        positionMs = this[KEY_POSITION] ?: 0L,
    )

    private companion object {
        val KEY_RECITER = intPreferencesKey("last_reciter_id")
        val KEY_MOSHAF = intPreferencesKey("last_moshaf_id")
        val KEY_SURAH = intPreferencesKey("last_surah_id")
        val KEY_POSITION = longPreferencesKey("last_position_ms")
        val KEY_CATALOG_LANGUAGE = stringPreferencesKey("catalog_language")
        val KEY_CATALOG_AT = longPreferencesKey("catalog_loaded_at")
    }
}
