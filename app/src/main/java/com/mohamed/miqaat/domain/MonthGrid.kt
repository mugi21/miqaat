package com.mohamed.miqaat.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Nombre de colonnes d'une grille de mois : une semaine. */
const val DAYS_PER_WEEK = 7

/**
 * Découpe un mois grégorien en cases de grille, alignées sur [firstDayOfWeek].
 *
 * Les cases précédant le 1er du mois et suivant le dernier jour valent `null` :
 * on préfère du vide aux jours des mois voisins, qui inviteraient à cliquer sur
 * une date qui sortirait de la grille affichée.
 *
 * La liste renvoyée a toujours un multiple de [DAYS_PER_WEEK] éléments.
 */
fun monthGridCells(month: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
    // DayOfWeek.value va de 1 (lundi) à 7 (dimanche) : le modulo positif donne
    // le décalage quel que soit le premier jour de la semaine de la locale.
    val leading = Math.floorMod(month.atDay(1).dayOfWeek.value - firstDayOfWeek.value, DAYS_PER_WEEK)
    val length = month.lengthOfMonth()
    val trailing = Math.floorMod(-(leading + length), DAYS_PER_WEEK)

    return buildList(leading + length + trailing) {
        repeat(leading) { add(null) }
        for (day in 1..length) add(month.atDay(day))
        repeat(trailing) { add(null) }
    }
}

/** Les sept jours de la semaine à partir de [firstDayOfWeek] : l'en-tête des colonnes. */
fun weekdaysFrom(firstDayOfWeek: DayOfWeek): List<DayOfWeek> =
    List(DAYS_PER_WEEK) { firstDayOfWeek.plus(it.toLong()) }
