package com.mohamed.miqaat.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    @Query("SELECT * FROM quran_reciter ORDER BY id")
    fun observeReciters(): Flow<List<QuranReciterEntity>>

    @Query("SELECT * FROM quran_moshaf ORDER BY reciterId, id")
    fun observeMoshafs(): Flow<List<QuranMoshafEntity>>

    @Query("SELECT * FROM quran_surah ORDER BY id")
    fun observeSuwar(): Flow<List<QuranSurahEntity>>

    @Query("SELECT * FROM quran_moshaf WHERE id = :id")
    suspend fun moshafById(id: Int): QuranMoshafEntity?

    @Query("SELECT * FROM quran_surah ORDER BY id")
    suspend fun suwar(): List<QuranSurahEntity>

    /** La langue du cache : sert à savoir s'il faut le refaire après un changement. */
    @Query("SELECT language FROM quran_reciter LIMIT 1")
    suspend fun cachedLanguage(): String?

    @Query("SELECT COUNT(*) FROM quran_reciter")
    suspend fun reciterCount(): Int

    @Query("SELECT COUNT(*) FROM quran_surah")
    suspend fun surahCount(): Int

    /**
     * Remplacement complet : le catalogue n'existe que dans une langue à la fois,
     * et une réponse partielle ne doit jamais se mélanger à l'ancienne.
     */
    @Transaction
    suspend fun replaceReciters(reciters: List<QuranReciterEntity>, moshafs: List<QuranMoshafEntity>) {
        clearMoshafs()
        clearReciters()
        insertReciters(reciters)
        insertMoshafs(moshafs)
    }

    @Transaction
    suspend fun replaceSuwar(suwar: List<QuranSurahEntity>) {
        clearSuwar()
        insertSuwar(suwar)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReciters(entities: List<QuranReciterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoshafs(entities: List<QuranMoshafEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuwar(entities: List<QuranSurahEntity>)

    @Query("DELETE FROM quran_reciter")
    suspend fun clearReciters()

    @Query("DELETE FROM quran_moshaf")
    suspend fun clearMoshafs()

    @Query("DELETE FROM quran_surah")
    suspend fun clearSuwar()
}

@Dao
interface QuranFavoriteDao {

    @Query("SELECT * FROM quran_favorite")
    fun observeAll(): Flow<List<QuranFavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(entity: QuranFavoriteEntity)

    @Query("DELETE FROM quran_favorite WHERE type = :type AND refId = :refId")
    suspend fun remove(type: String, refId: Int)
}
