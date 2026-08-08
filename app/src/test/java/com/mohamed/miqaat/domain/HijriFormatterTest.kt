package com.mohamed.miqaat.domain

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HijriFormatterTest {

    private val formatter = HijriFormatter()

    @Test
    fun `le 11 mars 2024 correspond au 1 Ramadan 1445`() {
        // Correspondance Umm al-Qura documentée : début du Ramadan 1445.
        val hijrah = HijrahDate.from(LocalDate.of(2024, 3, 11))
        assertEquals(1, hijrah.get(ChronoField.DAY_OF_MONTH))
        assertEquals(9, hijrah.get(ChronoField.MONTH_OF_YEAR))
        assertEquals(1445, hijrah.get(ChronoField.YEAR))
    }

    @Test
    fun `le format contient le mois arabe et l'annee en chiffres occidentaux`() {
        val formatted = formatter.format(LocalDate.of(2024, 3, 11))

        assertTrue("« $formatted » devrait contenir رمضان", formatted.contains("رمضان"))
        assertTrue("« $formatted » devrait contenir 1445", formatted.contains("1445"))
        assertTrue("« $formatted » devrait commencer par le jour 1", formatted.contains("1 "))
    }
}
