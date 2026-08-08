package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.CalculationSettings
import com.mohamed.miqaat.domain.model.MethodOption
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoMethodResolverTest {

    @Test
    fun `chaque pays obtient sa methode nationale`() {
        assertEquals(MethodOption.ALGERIA, AutoMethodResolver.resolve("DZ"))
        assertEquals(MethodOption.TUNISIA, AutoMethodResolver.resolve("TN"))
        assertEquals(MethodOption.MOROCCO, AutoMethodResolver.resolve("MA"))
        assertEquals(MethodOption.JORDAN, AutoMethodResolver.resolve("JO"))
        assertEquals(MethodOption.NORTH_AMERICA, AutoMethodResolver.resolve("US"))
    }

    @Test
    fun `les pays sans methode documentee sont rattaches au standard regional`() {
        // Libye → méthode égyptienne, Oman → règle du Golfe (décision projet)
        assertEquals(MethodOption.EGYPTIAN, AutoMethodResolver.resolve("LY"))
        assertEquals(MethodOption.GULF, AutoMethodResolver.resolve("OM"))
    }

    @Test
    fun `pays inconnu ou absent retombe sur MWL`() {
        assertEquals(MethodOption.MUSLIM_WORLD_LEAGUE, AutoMethodResolver.resolve(null))
        assertEquals(MethodOption.MUSLIM_WORLD_LEAGUE, AutoMethodResolver.resolve("XX"))
    }

    @Test
    fun `la casse du code pays est ignoree`() {
        assertEquals(MethodOption.ALGERIA, AutoMethodResolver.resolve("dz"))
    }

    @Test
    fun `le mode manuel ignore le pays`() {
        val manual = CalculationSettings(method = MethodOption.EGYPTIAN, methodAuto = false)
        assertEquals(MethodOption.EGYPTIAN, manual.effectiveMethod("DZ"))

        val auto = CalculationSettings(method = MethodOption.EGYPTIAN, methodAuto = true)
        assertEquals(MethodOption.ALGERIA, auto.effectiveMethod("DZ"))
    }
}
