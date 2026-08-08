package com.mohamed.miqaat.domain.model

/**
 * Ajustement manuel de chacun des six moments, en minutes.
 *
 * Sert à coller au calendrier d'une mosquée ou d'un ministère dont la marge de
 * précaution n'est pas celle de la méthode retenue — comme les 3 minutes que
 * l'Algérie ajoute au Maghrib (voir D23), mais pour les pays dont la valeur n'a
 * pas été mesurée. Ces minutes **s'ajoutent** à celles de la méthode : elles ne
 * les remplacent pas.
 *
 * Les entrées nulles ne sont jamais stockées, pour que deux réglages équivalents
 * soient égaux — c'est ce qui permet au cache de l'accueil de reposer sur
 * l'égalité de [CalculationSettings].
 */
data class PrayerTimeAdjustments(
    private val minutesByPrayer: Map<PrayerName, Int> = emptyMap(),
) {

    operator fun get(prayer: PrayerName): Int = minutesByPrayer[prayer] ?: 0

    /** Renvoie un nouvel ajustement, la valeur étant bornée puis normalisée. */
    fun with(prayer: PrayerName, minutes: Int): PrayerTimeAdjustments {
        val bounded = sanitize(minutes)
        val updated = if (bounded == 0) {
            minutesByPrayer - prayer
        } else {
            minutesByPrayer + (prayer to bounded)
        }
        return PrayerTimeAdjustments(updated)
    }

    val isEmpty: Boolean get() = minutesByPrayer.isEmpty()

    /** Les moments réellement décalés, dans l'ordre du jour : de quoi résumer à l'écran. */
    val adjustedPrayers: List<PrayerName>
        get() = PrayerName.entries.filter { this[it] != 0 }

    companion object {
        /** Bornes larges : certains calendriers locaux s'écartent de plus d'un quart d'heure. */
        const val MIN_MINUTES = -30
        const val MAX_MINUTES = 30

        fun sanitize(minutes: Int): Int = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)

        /** Construit depuis un stockage brut, en écartant zéros et valeurs hors bornes. */
        fun of(minutesByPrayer: Map<PrayerName, Int>): PrayerTimeAdjustments =
            PrayerTimeAdjustments(
                minutesByPrayer
                    .mapValues { (_, minutes) -> sanitize(minutes) }
                    .filterValues { it != 0 },
            )
    }
}
