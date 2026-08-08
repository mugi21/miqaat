package com.mohamed.miqaat.data.location

import android.content.Context
import android.location.Geocoder
import java.util.Locale

/**
 * Nom de ville (en arabe) et code pays via le géocodeur du système. Peut
 * nécessiter le réseau : en cas d'échec on garde les anciennes valeurs
 * (le cache Room fait foi hors ligne).
 */
class CityNameResolver(private val context: Context) {

    data class ResolvedPlace(val cityName: String?, val countryCode: String?)

    fun resolve(latitude: Double, longitude: Double): ResolvedPlace? {
        if (!Geocoder.isPresent()) return null
        return try {
            @Suppress("DEPRECATION") // la variante synchrone reste le choix simple ici
            Geocoder(context, Locale("ar"))
                .getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()
                ?.run {
                    ResolvedPlace(
                        cityName = locality ?: subAdminArea ?: adminArea,
                        countryCode = countryCode?.uppercase(Locale.ROOT),
                    )
                }
        } catch (_: Exception) {
            null
        }
    }
}
