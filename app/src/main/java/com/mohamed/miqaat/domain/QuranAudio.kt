package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.Moshaf
import com.mohamed.miqaat.domain.model.Surah
import java.util.Locale

/**
 * L'URL d'un enregistrement chez mp3quran : la racine du moshaf, puis le numéro
 * de sourate sur **trois chiffres**, puis `.mp3`.
 *
 *     https://server6.mp3quran.net/akdr/ + 001.mp3
 */
object QuranAudio {

    /**
     * @return null si le récitateur n'a pas cette sourate. Renvoyer une URL
     *   quand même donnerait un 404 quelques secondes plus tard, au moment le
     *   plus désagréable : après le début du chargement.
     */
    fun audioUrl(moshaf: Moshaf, surahId: Int): String? {
        if (!moshaf.has(surahId)) return null
        return moshaf.server.trimEnd('/') + "/" + fileName(surahId)
    }

    /** `1` → `001.mp3`, `114` → `114.mp3`. `Locale.ROOT` : jamais de chiffres arabes ici. */
    fun fileName(surahId: Int): String = String.format(Locale.ROOT, "%03d.mp3", surahId)

    /**
     * La file de lecture : la sourate choisie puis toutes les suivantes que ce
     * récitateur possède. C'est ce qui donne un sens à « suivant » et fait
     * enchaîner la lecture au lieu de s'arrêter à chaque fin de sourate.
     */
    fun queueFrom(moshaf: Moshaf, surahId: Int): List<Int> =
        (surahId..Surah.LAST_ID).filter(moshaf::has)
}
