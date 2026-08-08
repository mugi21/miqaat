package com.mohamed.miqaat.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Un fix de position ponctuel via le LocationManager du framework :
 * pas de Play Services, fonctionne sur tous les appareils, précision
 * « ville » largement suffisante pour les horaires de prière.
 */
class DeviceLocationDataSource(private val context: Context) {

    @SuppressLint("MissingPermission") // vérifiée par hasPermission()
    suspend fun currentFix(): Location? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val provider = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).firstOrNull(manager::isProviderEnabled) ?: return null

        // Un fix récent en cache suffit (la position ne change pas à l'échelle d'une ville).
        manager.getLastKnownLocation(provider)
            ?.takeIf { System.currentTimeMillis() - it.time < TimeUnit.MINUTES.toMillis(30) }
            ?.let { return it }

        return withTimeoutOrNull(FIX_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    manager.getCurrentLocation(
                        provider,
                        null,
                        ContextCompat.getMainExecutor(context),
                    ) { location -> continuation.resume(location) }
                } else {
                    @Suppress("DEPRECATION")
                    manager.requestSingleUpdate(
                        provider,
                        { location -> continuation.resume(location) },
                        context.mainLooper,
                    )
                }
            }
        }
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val FIX_TIMEOUT_MS = 15_000L
    }
}
