package com.mohamed.miqaat.data.location

import com.mohamed.miqaat.data.db.CachedLocationEntity
import com.mohamed.miqaat.data.db.LocationDao
import java.time.ZoneId
import kotlinx.coroutines.runBlocking

/**
 * Position réelle avec cache : mémoire → Room → défaut Skikda.
 * Après le premier fix, l'app est 100 % hors ligne : les horaires se
 * calculent sur la dernière position connue.
 */
class CachedLocationRepository(
    private val dao: LocationDao,
    private val deviceLocation: DeviceLocationDataSource,
    private val cityNameResolver: CityNameResolver,
) : LocationRepository {

    @Volatile
    private var memory: GeoLocation? = null

    override fun currentLocation(): GeoLocation =
        memory ?: runBlocking { loadFromDb() } // une seule ligne lue, une seule fois par process

    private suspend fun loadFromDb(): GeoLocation {
        val location = dao.get()?.toGeoLocation() ?: FixedLocationRepository.DEFAULT
        memory = location
        return location
    }

    override suspend fun refresh(): Boolean {
        val fix = deviceLocation.currentFix() ?: return false
        // Géocodage best-effort : hors ligne, on garde les dernières valeurs
        // connues (ou celles par défaut si on n'a jamais rien su de mieux).
        val place = cityNameResolver.resolve(fix.latitude, fix.longitude)
        val zone = ZoneId.systemDefault()

        val entity = CachedLocationEntity(
            latitude = fix.latitude,
            longitude = fix.longitude,
            cityName = place?.cityName ?: currentLocation().cityName,
            zoneId = zone.id,
            updatedAtEpochMs = System.currentTimeMillis(),
            countryCode = place?.countryCode
                ?: currentLocation().countryCode
                ?: countryFromZone(zone),
        )
        dao.upsert(entity)
        memory = entity.toGeoLocation()
        return true
    }

    private fun CachedLocationEntity.toGeoLocation() = GeoLocation(
        latitude = latitude,
        longitude = longitude,
        cityName = cityName,
        zoneId = runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault()),
        countryCode = countryCode,
    )

    /**
     * Repli 100 % hors ligne quand le géocodeur échoue : la table ICU des
     * fuseaux donne le pays (Africa/Algiers → DZ). "001" = zone sans pays.
     */
    private fun countryFromZone(zone: ZoneId): String? = runCatching {
        android.icu.util.TimeZone.getRegion(zone.id)
            .takeUnless { it == "001" || it == "ZZ" }
    }.getOrNull()
}
