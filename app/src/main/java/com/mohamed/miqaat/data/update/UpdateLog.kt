package com.mohamed.miqaat.data.update

import android.content.Context
import androidx.core.content.edit
import com.mohamed.miqaat.domain.update.ReleaseInfo
import java.util.concurrent.TimeUnit

/**
 * Ce que l'app retient de sa dernière visite chez GitHub : la release trouvée,
 * quand elle l'a été, et ce que l'utilisateur en a décidé.
 *
 * `SharedPreferences` et non DataStore, pour trois raisons dans cet ordre :
 * la note d'accueil a besoin d'une **lecture synchrone** (le repository amorce
 * son état dans son constructeur, donc rien ne clignote à la première
 * composition) ; cinq scalaires écrits une fois par jour ne justifient pas un
 * troisième DataStore — l'argument qui a fait naître celui du Coran (« la
 * position de lecture s'écrit à chaque pause ») joue ici en sens inverse ; et
 * c'est exactement la même famille de données que
 * [ReliabilityLog][com.mohamed.miqaat.data.reliability.ReliabilityLog], dont ce
 * fichier est le calque.
 */
object UpdateLog {

    private const val PREFS = "update"

    private const val KEY_AUTO_CHECK = "auto_check"
    private const val KEY_LAST_CHECK_AT = "last_check_at"
    private const val KEY_TAG = "latest_tag"
    private const val KEY_NAME = "latest_name"
    private const val KEY_NOTES = "latest_notes"
    private const val KEY_PAGE_URL = "latest_page_url"
    private const val KEY_APK_URL = "latest_apk_url"
    private const val KEY_APK_SIZE = "latest_apk_size"
    private const val KEY_SHA256 = "latest_sha256"
    private const val KEY_VERSION_CODE = "latest_version_code"
    private const val KEY_SKIPPED_TAG = "skipped_tag"
    private const val KEY_SNOOZED_UNTIL = "snoozed_until"
    private const val KEY_DOWNLOAD_ID = "download_id"
    private const val KEY_DOWNLOADED_TAG = "downloaded_tag"

    /**
     * Combien de temps la note d'accueil se tait après un « plus tard ».
     * Plus court que les quatorze jours de la fiabilité : rater une version est
     * moins grave que rater un adhan.
     */
    const val SNOOZE_DAYS = 7L

    /** Deux appels réseau ne peuvent pas être plus rapprochés que ça. */
    val CHECK_INTERVAL_MS: Long = TimeUnit.HOURS.toMillis(24)

    const val NO_DOWNLOAD = -1L
    private const val NO_VERSION_CODE = -1L

    fun autoCheckEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_CHECK, true)

    fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_AUTO_CHECK, enabled) }
    }

    /** `0` = aucune vérification n'a jamais abouti. */
    fun lastCheckAt(context: Context): Long = prefs(context).getLong(KEY_LAST_CHECK_AT, 0L)

    /**
     * La release mise en cache, telle qu'elle sera affichée hors ligne — notes de
     * version comprises. `null` tant qu'aucune vérification n'a abouti.
     */
    fun cachedRelease(context: Context): ReleaseInfo? {
        val preferences = prefs(context)
        val tag = preferences.getString(KEY_TAG, null)?.takeIf { it.isNotBlank() } ?: return null
        return ReleaseInfo(
            tag = tag,
            name = preferences.getString(KEY_NAME, null).orEmpty().ifBlank { tag },
            notes = preferences.getString(KEY_NOTES, null).orEmpty(),
            pageUrl = preferences.getString(KEY_PAGE_URL, null).orEmpty(),
            apkUrl = preferences.getString(KEY_APK_URL, null)?.takeIf { it.isNotBlank() },
            apkSizeBytes = preferences.getLong(KEY_APK_SIZE, 0L),
            sha256 = preferences.getString(KEY_SHA256, null)?.takeIf { it.isNotBlank() },
            versionCode = preferences.getLong(KEY_VERSION_CODE, NO_VERSION_CODE)
                .takeIf { it != NO_VERSION_CODE },
        )
    }

    fun saveRelease(context: Context, release: ReleaseInfo, checkedAt: Long) {
        prefs(context).edit {
            putString(KEY_TAG, release.tag)
            putString(KEY_NAME, release.name)
            putString(KEY_NOTES, release.notes)
            putString(KEY_PAGE_URL, release.pageUrl)
            putString(KEY_APK_URL, release.apkUrl)
            putLong(KEY_APK_SIZE, release.apkSizeBytes)
            putString(KEY_SHA256, release.sha256)
            putLong(KEY_VERSION_CODE, release.versionCode ?: NO_VERSION_CODE)
            putLong(KEY_LAST_CHECK_AT, checkedAt)
        }
    }

    /** La version que l'utilisateur a explicitement écartée ; la suivante repassera. */
    fun skippedTag(context: Context): String? = prefs(context).getString(KEY_SKIPPED_TAG, null)

    fun skip(context: Context, tag: String) {
        prefs(context).edit { putString(KEY_SKIPPED_TAG, tag) }
    }

    fun snoozedUntil(context: Context): Long = prefs(context).getLong(KEY_SNOOZED_UNTIL, 0L)

    fun snooze(context: Context, days: Long = SNOOZE_DAYS) {
        val until = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days)
        prefs(context).edit { putLong(KEY_SNOOZED_UNTIL, until) }
    }

    /**
     * Le téléchargement en cours ou terminé. Persisté parce qu'il doit survivre
     * à un aller-retour vers l'écran des sources inconnues — certaines surcouches
     * tuent et relancent l'application au passage.
     */
    fun downloadId(context: Context): Long = prefs(context).getLong(KEY_DOWNLOAD_ID, NO_DOWNLOAD)

    fun setDownload(context: Context, id: Long, tag: String?) {
        prefs(context).edit {
            putLong(KEY_DOWNLOAD_ID, id)
            putString(KEY_DOWNLOADED_TAG, tag)
        }
    }

    /** Le tag de l'APK posé sur le disque, pour savoir plus tard s'il a servi. */
    fun downloadedTag(context: Context): String? = prefs(context).getString(KEY_DOWNLOADED_TAG, null)

    fun clearDownload(context: Context) {
        prefs(context).edit {
            remove(KEY_DOWNLOAD_ID)
            remove(KEY_DOWNLOADED_TAG)
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
