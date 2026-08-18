package com.mohamed.miqaat.data.update

import android.util.Log
import com.mohamed.miqaat.domain.update.ReleaseInfo
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * La dernière release publiée du dépôt, par l'API publique de GitHub (sans clé
 * ni compte).
 *
 * Même patron que [Mp3QuranApi][com.mohamed.miqaat.data.quran.Mp3QuranApi] :
 * `HttpURLConnection` et `org.json` plutôt qu'une pile Retrofit/Moshi — un seul
 * appel GET par jour ne justifie pas une dépendance. Aucune méthode ne lève : un
 * échec réseau rend `null`, et l'appelant garde la release mise en cache.
 */
class GithubReleaseApi {

    suspend fun latest(): ReleaseInfo? = get()?.let(GithubReleaseParser::parseLatest)

    private suspend fun get(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(LATEST_URL).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                // C'est déjà le défaut, mais on ne laisse pas une surcouche en décider :
                // api.github.com redirige, et une redirection non suivie rendrait un
                // corps vide plutôt qu'une erreur.
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("X-GitHub-Api-Version", API_VERSION)
                // ⚠ Sans User-Agent, GitHub répond 403 — c'est la seule API du projet
                // qui l'exige vraiment.
                connection.setRequestProperty("User-Agent", USER_AGENT)
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    // 403 ou 429 = quota anonyme épuisé (60 requêtes/heure/IP, hors
                    // d'atteinte à une vérification par jour). Traité comme tout échec.
                    Log.w(TAG, "release : HTTP ${connection.responseCode}")
                    return@withContext null
                }
                // On ne lit pas un flux sans fin depuis un hôte qu'on ne contrôle pas.
                // ⚠ `read` rend « jusqu'à » ce qu'on demande, pas tout : un seul appel
                // tronquerait la réponse au premier segment TCP. D'où la boucle.
                connection.inputStream.bufferedReader().use { reader ->
                    val body = StringBuilder()
                    val buffer = CharArray(READ_CHUNK_CHARS)
                    while (body.length < MAX_BODY_CHARS) {
                        val read = reader.read(buffer)
                        if (read < 0) break
                        body.appendRange(buffer, 0, read)
                    }
                    body.toString().takeIf { it.isNotEmpty() }
                }
            } finally {
                connection.disconnect()
            }
        }.getOrElse { error ->
            Log.w(TAG, "release injoignable", error)
            null
        }
    }

    private companion object {
        const val TAG = "GithubReleaseApi"

        /**
         * `/releases/latest` : GitHub y exclut d'office les brouillons et les
         * pré-versions. On revérifie quand même côté parseur — une décision locale
         * ne repose pas sur un contrat distant.
         */
        const val LATEST_URL = "https://api.github.com/repos/mugi21/miqaat/releases/latest"
        const val API_VERSION = "2022-11-28"
        const val TIMEOUT_MS = 10_000
        const val USER_AGENT = "Miqaat (Android; github.com/mugi21)"
        const val MAX_BODY_CHARS = 256 * 1024
        const val READ_CHUNK_CHARS = 8 * 1024
    }
}

/**
 * Le passage JSON → domaine, isolé de tout appel réseau pour être testable sur
 * des fixtures. Rien ne lève : une réponse tronquée ou d'une forme inattendue
 * rend `null`, et la release mise en cache reste en place.
 */
object GithubReleaseParser {

    fun parseLatest(json: String): ReleaseInfo? = runCatching {
        val node = JSONObject(json)
        // Ceinture et bretelles : /releases/latest ne devrait jamais en renvoyer.
        if (node.optBoolean("draft", false) || node.optBoolean("prerelease", false)) return null
        val tag = node.optString("tag_name").trim().takeIf { it.isNotBlank() } ?: return null
        val body = node.optString("body").trim()
        val apk = pickApk(node.optJSONArray("assets"))
        ReleaseInfo(
            tag = tag,
            name = node.optString("name").trim().takeIf { it.isNotBlank() } ?: tag,
            notes = body,
            pageUrl = node.optString("html_url").trim()
                .takeIf { it.startsWith(HTTPS) } ?: fallbackPageUrl(tag),
            apkUrl = apk?.first,
            apkSizeBytes = apk?.second ?: 0L,
            sha256 = sha256In(body),
            versionCode = versionCodeIn(body),
        )
    }.getOrNull()

    /**
     * L'APK de la release, et sa taille. Un dépôt peut joindre plusieurs fichiers
     * (sources, sommes de contrôle) ; on prend le `.apk`, en préférant celui qui
     * suit la convention de nommage de [docs/release.md].
     *
     * ⚠ Toute URL qui n'est pas en `https://` est rejetée — même garde que le
     * `server` d'un moshaf. Aucun `.apk` acceptable : `null`, et l'écran n'offrira
     * que le repli navigateur, ce qui vaut mieux que masquer l'existence de la version.
     */
    internal fun pickApk(assets: JSONArray?): Pair<String, Long>? {
        val candidates = (0 until (assets?.length() ?: 0))
            .mapNotNull { assets?.optJSONObject(it) }
            .filter { it.optString("name").trim().endsWith(".apk", ignoreCase = true) }
            .filter { it.optString("browser_download_url").trim().startsWith(HTTPS) }
        val chosen = candidates.firstOrNull {
            it.optString("name").trim().startsWith(APK_PREFIX, ignoreCase = true)
        } ?: candidates.firstOrNull() ?: return null
        return chosen.optString("browser_download_url").trim() to chosen.optLong("size", 0L)
    }

    /**
     * L'empreinte publiée dans les notes de version — `docs/release.md` l'exige
     * déjà, on ne fait que la lire.
     *
     * ⚠ Deux empreintes dans le corps : `null`. Une ambiguïté ne doit jamais
     * bloquer une mise à jour, seulement priver de la vérification.
     */
    internal fun sha256In(body: String): String? {
        val labelled = LABELLED_SHA.find(body)?.groupValues?.getOrNull(1)
        if (labelled != null) return labelled.lowercase()
        val loose = LOOSE_SHA.findAll(body).map { it.value.lowercase() }.distinct().toList()
        return loose.singleOrNull()
    }

    /** La ligne `versionCode: 6` du corps, qui a le dernier mot sur le tag (D44). */
    internal fun versionCodeIn(body: String): Long? =
        VERSION_CODE.find(body)?.groupValues?.getOrNull(1)?.toLongOrNull()

    private fun fallbackPageUrl(tag: String) = "$RELEASES_URL/$tag"

    private const val HTTPS = "https://"
    private const val APK_PREFIX = "miqaat-"
    private const val RELEASES_URL = "https://github.com/mugi21/miqaat/releases/tag"

    private val LABELLED_SHA =
        Regex("""sha-?256\s*[:=]\s*`?([0-9a-fA-F]{64})`?""", RegexOption.IGNORE_CASE)
    private val LOOSE_SHA = Regex("""\b[0-9a-fA-F]{64}\b""")
    private val VERSION_CODE = Regex(
        """^\s*versionCode\s*[:=]\s*(\d{1,9})\s*$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
    )
}
