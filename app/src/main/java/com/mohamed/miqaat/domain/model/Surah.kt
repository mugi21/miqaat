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
    companion object {
        const val FIRST_ID = 1
        const val LAST_ID = 114
        const val COUNT = LAST_ID
    }
}
