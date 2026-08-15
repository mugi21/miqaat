package com.mohamed.miqaat

import android.app.Application
import android.content.Context
import com.mohamed.miqaat.data.db.MiqaatDatabase
import com.mohamed.miqaat.data.location.CachedLocationRepository
import com.mohamed.miqaat.data.location.CityNameResolver
import com.mohamed.miqaat.data.location.DeviceLocationDataSource
import com.mohamed.miqaat.data.invocations.InvocationRepository
import com.mohamed.miqaat.data.location.LocationRepository
import com.mohamed.miqaat.data.quran.QuranCatalogRepository
import com.mohamed.miqaat.data.quran.QuranPreferences
import com.mohamed.miqaat.data.settings.SettingsRepository
import com.mohamed.miqaat.notifications.NotificationChannels
import com.mohamed.miqaat.quran.QuranPlayerConnection

class MiqaatApp : Application() {

    // Singletons de l'app — pas de framework de DI : à cette échelle,
    // des lazy sur l'Application suffisent et restent lisibles.
    val database: MiqaatDatabase by lazy { MiqaatDatabase.build(this) }

    val locationRepository: LocationRepository by lazy {
        CachedLocationRepository(
            dao = database.locationDao(),
            deviceLocation = DeviceLocationDataSource(this),
            cityNameResolver = CityNameResolver(this),
        )
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    val invocationRepository: InvocationRepository by lazy {
        InvocationRepository(database.invocationDao())
    }

    val quranPreferences: QuranPreferences by lazy { QuranPreferences(this) }

    val quranCatalogRepository: QuranCatalogRepository by lazy {
        QuranCatalogRepository(
            dao = database.quranDao(),
            favoriteDao = database.quranFavoriteDao(),
            preferences = quranPreferences,
        )
    }

    /**
     * Un seul lien vers le service de lecture pour toute l'app : le mini-lecteur
     * vit dans la barre du bas de l'activité et doit survivre au changement
     * d'écran. `lazy` : rien n'est créé tant que l'utilisateur n'ouvre pas le
     * Coran, donc l'app ne paie rien si la fonctionnalité ne l'intéresse pas.
     */
    val quranPlayer: QuranPlayerConnection by lazy { QuranPlayerConnection(this) }

    override fun onCreate() {
        super.onCreate()
        // Les canaux doivent exister avant toute notification, y compris celles
        // déclenchées app fermée (receiver d'alarme).
        NotificationChannels.createAll(this)
    }
}

/** L'Application typée, accessible depuis n'importe quel Context (receivers compris). */
val Context.miqaatApp: MiqaatApp
    get() = applicationContext as MiqaatApp
