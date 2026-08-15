package com.mohamed.miqaat.domain.model

/**
 * Une sourate du catalogue. Le nom vient de l'API dans la langue demandée : il
 * n'est donc **pas** dans `strings.xml` — trois copies de 114 noms n'auraient
 * aucun intérêt puisque la source les traduit déjà.
 */
data class Surah(
    val id: Int,
    val name: String,
    /** Révélée à La Mecque (`makkia = 1`) ou à Médine. */
    val makki: Boolean,
) {
    /** Le nombre de versets, connu localement — l'API ne le donne pas. */
    val ayahCount: Int get() = ayahCountOf(id)

    companion object {
        const val FIRST_ID = 1
        const val LAST_ID = 114
        const val COUNT = LAST_ID

        /** La somme des versets du Coran dans le décompte de Kūfa (celui de Ḥafṣ). */
        const val TOTAL_AYAHS = 6236

        /**
         * Le nombre de versets de chacune des 114 sourates, décompte de Kūfa —
         * celui qu'emploient les mushafs de Ḥafṣ ʿan ʿĀṣim, donc l'écrasante
         * majorité des enregistrements du catalogue.
         *
         * En dur, et non demandé à l'API : c'est une donnée immuable, elle n'a
         * pas à dépendre du réseau. Un test vérifie que la somme fait bien
         * [TOTAL_AYAHS] — c'est ce qui rend une faute de frappe détectable.
         */
        private val AYAH_COUNTS = intArrayOf(
            7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
            123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
            112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
            34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
            54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
            60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
            14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
            28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
            29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
            15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
            11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
            5, 4, 5, 6,
        )

        fun ayahCountOf(surahId: Int): Int =
            AYAH_COUNTS.getOrElse(surahId - FIRST_ID) { 0 }
    }
}
