package com.mohamed.miqaat.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthGridTest {

    @Test
    fun `la grille est toujours un multiple de sept cases`() {
        // Douze mois consécutifs, et le cas limite du février de 28 jours
        // commençant pile au premier jour de la semaine.
        for (month in 1..12) {
            val cells = monthGridCells(YearMonth.of(2026, month), DayOfWeek.SATURDAY)
            assertEquals(0, cells.size % DAYS_PER_WEEK)
        }
    }

    @Test
    fun `les cases vides precedent le premier du mois`() {
        // 1er juillet 2026 = mercredi ; semaine commençant au samedi → 4 cases vides.
        val cells = monthGridCells(YearMonth.of(2026, 7), DayOfWeek.SATURDAY)

        repeat(4) { assertNull(cells[it]) }
        assertEquals(LocalDate.of(2026, 7, 1), cells[4])
    }

    @Test
    fun `le premier jour de la semaine change le decalage`() {
        // Le même mois, aligné sur le lundi : mercredi → 2 cases vides seulement.
        val cells = monthGridCells(YearMonth.of(2026, 7), DayOfWeek.MONDAY)

        repeat(2) { assertNull(cells[it]) }
        assertEquals(LocalDate.of(2026, 7, 1), cells[2])
    }

    @Test
    fun `tous les jours du mois sont presents, une seule fois, dans l'ordre`() {
        val month = YearMonth.of(2026, 2)
        val days = monthGridCells(month, DayOfWeek.SATURDAY).filterNotNull()

        assertEquals(month.lengthOfMonth(), days.size)
        assertEquals(month.atDay(1), days.first())
        assertEquals(month.atEndOfMonth(), days.last())
        assertEquals(days.sorted(), days)
    }

    @Test
    fun `un mois qui tombe pile ne comporte aucune case vide`() {
        // 1er février 2026 = dimanche ; semaine commençant au dimanche, 28 jours.
        val cells = monthGridCells(YearMonth.of(2026, 2), DayOfWeek.SUNDAY)

        assertEquals(28, cells.size)
        assertEquals(0, cells.count { it == null })
    }

    @Test
    fun `l'en-tete des colonnes part du premier jour de la semaine`() {
        assertEquals(
            listOf(
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
            ),
            weekdaysFrom(DayOfWeek.SATURDAY),
        )
    }
}
