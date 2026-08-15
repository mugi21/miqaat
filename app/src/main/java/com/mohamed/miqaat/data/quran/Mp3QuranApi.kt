package com.mohamed.miqaat.data.quran

import android.util.Log
import com.mohamed.miqaat.domain.model.Moshaf
import com.mohamed.miqaat.domain.model.Reciter
import com.mohamed.miqaat.domain.model.Surah
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Les codes de langue de l'API mp3quran, relevés sur `/api/v3/languages`.
 *
 * ⚠ Ce ne sont **pas** les codes d'Android : l'anglais est `eng`, pas `en`.
 * Envoyer un code inconnu ne provoque aucune erreur — l'API retombe
 * silencieusement sur l'arabe, et le catalogue serait en arabe dans une app en
 * anglais. D'où cette table explicite, et son test.
 */
enum class Mp3QuranLanguage(val code: String) {
    ARABIC("ar"),
    ENGLISH("eng"),
    FRENCH("fr"),
    ;

    companion object {
        /** @param tag code ISO-639-1 de la langue affichée (`ar`, `fr`, `en`…). */
        fun forTag(tag: String?): Mp3QuranLanguage = when (tag?.lowercase()?.take(2)) {
            "en" -> ENGLISH
            "fr" -> FRENCH
            // L'arabe est la langue par défaut de l'app comme celle de l'API :
            // c'est le repli le moins surprenant pour toute autre langue.
            else -> ARABIC
        }
    }
}

/**
 * Le catalogue mp3quran.net (API v3 publique, sans clé ni inscription).
 *
 * `HttpURLConnection` et `org.json` plutôt qu'une pile Retrofit/Moshi : deux
 * appels GET sans authentification ne justifient pas trois dépendances de plus.
 * Aucune méthode ne lève : un échec réseau rend `null`, et l'appelant retombe
 * sur le cache.
 */
class Mp3QuranApi {

    suspend fun reciters(language: Mp3QuranLanguage): List<Reciter>? =
        get("reciters", language)?.let(Mp3QuranParser::parseReciters)

    suspend fun suwar(language: Mp3QuranLanguage): List<Surah>? =
        get("suwar", language)?.let(Mp3QuranParser::parseSuwar)

    private suspend fun get(path: String, language: Mp3QuranLanguage): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL("$BASE_URL/$path?language=${language.code}")
                    .openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = TIMEOUT_MS
                    connection.readTimeout = TIMEOUT_MS
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("User-Agent", USER_AGENT)
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        Log.w(TAG, "$path : HTTP ${connection.responseCode}")
                        return@withContext null
                    }
                    connection.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    connection.disconnect()
                }
            }.getOrElse { error ->
                Log.w(TAG, "$path injoignable", error)
                null
            }
        }

    private companion object {
        const val TAG = "Mp3QuranApi"

        /**
         * ⚠ **Avec le `www.`**, qui n'est pas décoratif : `mp3quran.net/api/v3`
         * répond **301** vers `www.mp3quran.net/api/v3`. Suivre une redirection
         * à chaque appel, c'est un aller-retour de plus et une occasion de plus
         * de perdre le paramètre `language` en route. On vise l'hôte canonique.
         */
        const val BASE_URL = "https://www.mp3quran.net/api/v3"
        const val TIMEOUT_MS = 10_000
        const val USER_AGENT = "Miqaat (Android; github.com/mugi21)"
    }
}

/**
 * Le passage JSON → domaine, isolé de tout appel réseau pour être testable sur
 * des fixtures. Rien ne lève : une réponse tronquée ou d'une forme inattendue
 * rend une liste vide, et le cache existant reste en place.
 */
object Mp3QuranParser {

    fun parseReciters(json: String): List<Reciter> = runCatching {
        JSONObject(json).optJSONArray("reciters")
            ?.objects()
            ?.mapNotNull(::toReciter)
            ?.filter { it.moshafs.isNotEmpty() }
            .orEmpty()
    }.getOrDefault(emptyList())

    fun parseSuwar(json: String): List<Surah> = runCatching {
        JSONObject(json).optJSONArray("suwar")
            ?.objects()
            ?.mapNotNull(::toSurah)
            .orEmpty()
    }.getOrDefault(emptyList())

    private fun toReciter(node: JSONObject): Reciter? {
        val id = node.optInt("id", 0).takeIf { it > 0 } ?: return null
        val name = node.optString("name").takeIf { it.isNotBlank() } ?: return null
        return Reciter(
            id = id,
            name = name.trim(),
            letter = node.optString("letter").trim(),
            moshafs = node.optJSONArray("moshaf")
                ?.objects()
                ?.mapNotNull { toMoshaf(it, reciterId = id) }
                .orEmpty(),
        )
    }

    private fun toMoshaf(node: JSONObject, reciterId: Int): Moshaf? {
        val id = node.optInt("id", 0).takeIf { it > 0 } ?: return null
        val server = node.optString("server").takeIf { it.startsWith("http") } ?: return null
        val surahIds = Moshaf.parseSurahList(node.optString("surah_list"))
        // Un moshaf sans aucune sourate lisible n'a rien à faire dans la liste :
        // il n'offrirait que des lignes grisées.
        if (surahIds.isEmpty()) return null
        return Moshaf(
            id = id,
            reciterId = reciterId,
            name = node.optString("name").trim(),
            server = server.trim(),
            surahIds = surahIds,
        )
    }

    private fun toSurah(node: JSONObject): Surah? {
        val id = node.optInt("id", 0).takeIf { it in Surah.FIRST_ID..Surah.LAST_ID } ?: return null
        val name = node.optString("name").takeIf { it.isNotBlank() } ?: return null
        // ⚠ Les noms de l'API portent souvent une espace finale (« Al-Fatihah »).
        return Surah(id = id, name = name.trim(), makki = node.optInt("makkia", 0) == 1)
    }

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).mapNotNull(::optJSONObject)
}
