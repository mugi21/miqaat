package com.mohamed.miqaat.domain.model

/**
 * Un récitateur du catalogue mp3quran, et ses enregistrements.
 *
 * Un même récitateur peut avoir plusieurs [Moshaf] : une rīwāya différente
 * (Ḥafṣ, Warsh, Qālūn…) ou un style différent (murattal, muʿallim). C'est le
 * moshaf, et non le récitateur, qui porte le serveur et la liste des sourates.
 */
data class Reciter(
    val id: Int,
    val name: String,
    /** Première lettre du nom, telle que fournie par l'API : sert au regroupement. */
    val letter: String,
    val moshafs: List<Moshaf>,
) {
    /** Le seul, ou celui qui a le plus de sourates : le choix par défaut à l'ouverture. */
    val defaultMoshaf: Moshaf? = moshafs.maxByOrNull { it.surahIds.size }
}

data class Moshaf(
    val id: Int,
    val reciterId: Int,
    /** Nom de la rīwāya tel que rendu par l'API, déjà traduit dans la langue demandée. */
    val name: String,
    /** Racine des fichiers, p. ex. `https://server6.mp3quran.net/akdr/`. */
    val server: String,
    /**
     * Les sourates réellement disponibles chez ce récitateur : **un moshaf n'a
     * pas toujours les 114**. Celles qui manquent doivent apparaître comme
     * indisponibles, jamais donner une URL qui répondra 404.
     */
    val surahIds: Set<Int>,
) {
    fun has(surahId: Int): Boolean = surahId in surahIds

    companion object {
        /**
         * Découpe le champ `surah_list` de l'API — `"1,2,3,…"`.
         *
         * ⚠ Tolérant à dessein : la documentation officielle montre elle-même une
         * valeur à virgule traînante (`"…,39,40,"`), et rien ne garantit
         * l'absence d'espaces. Les jetons vides ou illisibles sont ignorés, et
         * les numéros hors des 114 sourates écartés.
         */
        fun parseSurahList(raw: String): Set<Int> =
            raw.split(',')
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in Surah.FIRST_ID..Surah.LAST_ID }
                .toSortedSet()
    }
}
