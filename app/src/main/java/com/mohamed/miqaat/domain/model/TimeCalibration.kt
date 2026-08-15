package com.mohamed.miqaat.domain.model

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Comment passer de l'horaire astronomique, calculé à la seconde, à la minute affichée.
 *
 * Adhan tranche lui-même (minute la plus proche par défaut) ; l'app lui demande les
 * secondes brutes et décide à sa place, parce qu'un calendrier de ministère n'arrondit
 * pas comme une librairie : il ajoute une marge de précaution (iḥtiyāṭ) puis tronque,
 * de sorte que l'heure annoncée ne tombe **jamais** avant l'heure calculée.
 */
enum class MinuteRounding {

    /** Minute la plus proche : le comportement d'Adhan, donc celui de toutes nos méthodes non mesurées. */
    NEAREST,

    /** Minute inférieure : ce que fait un calendrier officiel, la marge étant déjà dans le décalage. */
    DOWN,

    /** Minute supérieure. */
    UP,
    ;

    fun apply(time: ZonedDateTime): ZonedDateTime {
        val truncated = time.truncatedTo(ChronoUnit.MINUTES)
        return when (this) {
            DOWN -> truncated
            UP -> if (truncated == time) time else truncated.plusMinutes(1)
            NEAREST -> if (time.second >= 30) truncated.plusMinutes(1) else truncated
        }
    }
}

/**
 * Le décalage, **en secondes**, appliqué à chaque moment avant l'arrondi.
 *
 * Des secondes et non des minutes : l'écart entre un calcul astronomique et un calendrier
 * officiel n'est pas un nombre rond. Il mélange la marge de précaution du ministère et le
 * point de référence qu'il retient pour la ville — et seule une mesure sur un mois entier
 * permet de le trancher (voir `docs/prayer-times-accuracy.md`).
 *
 * À ne pas confondre avec [PrayerTimeAdjustments], qui est le réglage **de l'utilisateur**,
 * en minutes, et vient s'ajouter par-dessus.
 */
data class TimeCalibration(
    val rounding: MinuteRounding = MinuteRounding.NEAREST,
    private val offsetSeconds: Map<PrayerName, Int> = emptyMap(),
) {

    operator fun get(prayer: PrayerName): Int = offsetSeconds[prayer] ?: 0

    /** Applique décalage puis arrondi — l'ordre compte, l'arrondi doit voir la marge. */
    fun apply(prayer: PrayerName, time: ZonedDateTime): ZonedDateTime =
        rounding.apply(time.plusSeconds(this[prayer].toLong()))

    companion object {
        /** Aucune mesure disponible : on s'en tient à ce que fait Adhan. */
        val DEFAULT = TimeCalibration()
    }
}
