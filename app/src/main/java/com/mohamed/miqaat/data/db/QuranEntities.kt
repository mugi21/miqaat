package com.mohamed.miqaat.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Le catalogue mp3quran mis en cache, pour que parcourir les récitateurs marche
 * hors ligne. Une **seule langue à la fois** : les noms ne sont que des
 * translittérations, garder trois copies ne vaudrait pas la complexité — au
 * changement de langue, le catalogue est refait.
 */
@Entity(tableName = "quran_reciter")
data class QuranReciterEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val letter: String,
    /** Code de langue de l'API (`ar`, `eng`, `fr`) sous lequel ce nom a été obtenu. */
    val language: String,
)

/**
 * Un enregistrement complet d'un récitateur : sa rīwāya, son serveur, et les
 * sourates qu'il contient réellement.
 */
@Entity(tableName = "quran_moshaf")
data class QuranMoshafEntity(
    @PrimaryKey val id: Int,
    val reciterId: Int,
    val name: String,
    val server: String,
    /** Le `surah_list` de l'API, gardé brut : c'est le domaine qui le découpe. */
    val surahList: String,
)

@Entity(tableName = "quran_surah")
data class QuranSurahEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val makki: Boolean,
    val language: String,
)

/**
 * Un favori, récitateur ou sourate. Une seule table pour les deux natures : la
 * question posée est la même (« est-ce marqué ? »), et la clé composite
 * (type, refId) empêche par construction le doublon.
 */
@Entity(tableName = "quran_favorite", primaryKeys = ["type", "refId"])
data class QuranFavoriteEntity(
    /** [TYPE_RECITER] ou [TYPE_SURAH]. */
    val type: String,
    val refId: Int,
) {
    companion object {
        const val TYPE_RECITER = "RECITER"
        const val TYPE_SURAH = "SURAH"
    }
}
