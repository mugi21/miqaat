package com.mohamed.miqaat.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Dernière position connue de l'utilisateur — une seule ligne (id = 1).
 * C'est elle qui rend l'app 100 % hors ligne après le premier fix.
 */
@Entity(tableName = "cached_location")
data class CachedLocationEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val zoneId: String,
    val updatedAtEpochMs: Long,
    /** Code pays ISO alpha-2, null si jamais géocodé (colonne ajoutée en v2). */
    val countryCode: String? = null,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
