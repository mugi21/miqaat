package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSettingsTest {

    @Test
    fun `le delai par defaut est de 10 minutes, rappel actif`() {
        val defaults = ReminderSettings()

        assertTrue(defaults.enabled)
        assertEquals(10, defaults.leadMinutes)
    }

    @Test
    fun `aucun choix sous 10 minutes, sinon le quota Doze retarderait l'adhan`() {
        assertEquals(10, ReminderSettings.LEAD_CHOICES.min())
    }

    @Test
    fun `une valeur hors liste est ramenee au choix le plus proche`() {
        assertEquals(10, ReminderSettings.sanitizeLead(5))
        assertEquals(10, ReminderSettings.sanitizeLead(0))
        assertEquals(15, ReminderSettings.sanitizeLead(16))
        assertEquals(60, ReminderSettings.sanitizeLead(120))
    }

    @Test
    fun `une valeur de la liste est conservee telle quelle`() {
        ReminderSettings.LEAD_CHOICES.forEach {
            assertEquals(it, ReminderSettings.sanitizeLead(it))
        }
    }
}
