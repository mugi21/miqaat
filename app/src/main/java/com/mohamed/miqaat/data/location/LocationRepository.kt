package com.mohamed.miqaat.data.location

import java.time.ZoneId

/**
 * Position géographique de l'utilisateur, avec son fuseau horaire.
 */
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val zoneId: ZoneId,
    /** Code pays ISO 3166-1 alpha-2 (sélection auto de la méthode) ; null si inconnu. */
    val countryCode: String? = null,
)

/**
 * Source de la position.
 *
 * [currentLocation] est synchrone et bon marché (cache mémoire) : elle est
 * appelée à chaque tick de l'écran et depuis les receivers d'alarme.
 * [refresh] tente un vrai fix appareil et met à jour le cache persistant.
 */
interface LocationRepository {
    fun currentLocation(): GeoLocation

    /** @return true si un fix a été obtenu (le cache a été mis à jour). */
    suspend fun refresh(): Boolean
}

/**
 * Position fixe de Skikda : valeur par défaut tant qu'aucun fix n'a jamais
 * été obtenu, et implémentation pratique pour les previews/tests.
 */
class FixedLocationRepository : LocationRepository {
    override fun currentLocation() = DEFAULT

    override suspend fun refresh() = false

    companion object {
        val DEFAULT = GeoLocation(
            latitude = 36.8665,
            longitude = 6.9063,
            cityName = "سكيكدة",
            zoneId = ZoneId.of("Africa/Algiers"),
            countryCode = "DZ",
        )
    }
}
