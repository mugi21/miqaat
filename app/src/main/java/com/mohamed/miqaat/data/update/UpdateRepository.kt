package com.mohamed.miqaat.data.update

import android.content.Context
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.mohamed.miqaat.domain.update.AppVersion
import com.mohamed.miqaat.domain.update.ReleaseInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Le nom et le code de version du paquet **réellement installé**. */
data class InstalledVersion(val name: String, val code: Long)

/**
 * Sait s'il existe une release plus récente que la version installée, et le
 * retient d'une ouverture à l'autre.
 *
 * La vérification ne part **que depuis l'activité** : jamais d'un receiver,
 * jamais de la chaîne d'alarmes, jamais d'un travail de fond. C'est ce qui
 * permet de continuer à écrire qu'aucune fonction cœur ne touche au réseau —
 * la mise à jour n'en est pas une, et elle se coupe (D44).
 */
class UpdateRepository(
    private val context: Context,
    private val api: GithubReleaseApi = GithubReleaseApi(),
) {

    // Amorcé depuis les préférences, donc correct dès la première composition :
    // la note d'accueil ne clignote pas en attendant une émission.
    private val _latest = MutableStateFlow(UpdateLog.cachedRelease(context))
    val latest: StateFlow<ReleaseInfo?> = _latest.asStateFlow()

    /** Un changement de langue recrée l'activité, qui rappellerait [refreshIfDue]. */
    @Volatile
    private var inFlight = false

    /**
     * Le paquet installé, et non `BuildConfig` : après un sideload, la question
     * est précisément « qu'est-ce qui tourne vraiment ? ».
     */
    fun installed(): InstalledVersion = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        InstalledVersion(
            name = info.versionName.orEmpty(),
            code = PackageInfoCompat.getLongVersionCode(info),
        )
    }.getOrDefault(InstalledVersion("", 0L))

    /**
     * Vérifie au plus une fois par jour, et seulement si l'utilisateur l'a laissé
     * faire.
     *
     * ⚠ `lastCheckAt` n'est écrit **qu'en cas de succès** : un échec ne consomme
     * pas le quota, et la fréquence de réessai reste naturellement bornée par le
     * nombre d'ouvertures de l'app.
     */
    suspend fun refreshIfDue(now: Long = System.currentTimeMillis()): Boolean {
        if (!UpdateLog.autoCheckEnabled(context)) return false
        if (now - UpdateLog.lastCheckAt(context) < UpdateLog.CHECK_INTERVAL_MS) return false
        return refresh(now)
    }

    /** La vérification demandée à la main : elle ignore le délai, pas l'opt-out. */
    suspend fun refreshNow(now: Long = System.currentTimeMillis()): Boolean = refresh(now)

    private suspend fun refresh(now: Long): Boolean {
        if (inFlight) return false
        inFlight = true
        return try {
            val release = api.latest() ?: return false
            UpdateLog.saveRelease(context, release, checkedAt = now)
            _latest.value = release
            true
        } finally {
            inFlight = false
        }
    }

    /**
     * Efface l'APK téléchargé une fois qu'il a servi — ou qu'il ne servira plus.
     *
     * Le nettoyage ne peut pas se faire après l'installation : le processus est
     * remplacé, plus rien de nous ne s'exécute. Il a donc lieu à l'ouverture
     * suivante, ce qui ramasse au passage les fichiers d'une installation
     * abandonnée. `RescheduleReceiver` n'est pas mis à contribution : il écoute
     * déjà `MY_PACKAGE_REPLACED` pour les alarmes et doit rendre la main vite.
     */
    fun cleanUpIfInstalled() {
        val tag = UpdateLog.downloadedTag(context) ?: return
        // Le tag téléchargé n'est plus en avance sur l'installé : il a servi, ou
        // il est devenu caduc. Dans les deux cas il n'a plus rien à faire là.
        if (AppVersion.isNewer(tag, installed().name)) return
        Log.i(TAG, "APK $tag devenu inutile, suppression")
        ApkInstaller.deleteDownloads(context)
        UpdateLog.clearDownload(context)
    }

    private companion object {
        const val TAG = "UpdateRepository"
    }
}
