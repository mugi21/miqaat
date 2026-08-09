package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.NotificationMode
import com.mohamed.miqaat.domain.model.NotificationSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationModeTest {

    @Test
    fun `le defaut suit le mode du telephone`() {
        assertEquals(NotificationMode.FOLLOW_PHONE, NotificationSettings().mode)
        assertEquals(NotificationMode.FOLLOW_PHONE, NotificationMode.DEFAULT)
    }

    @Test
    fun `une valeur absente ou inconnue retombe sur le defaut`() {
        assertEquals(NotificationMode.DEFAULT, NotificationMode.parse(null))
        assertEquals(NotificationMode.DEFAULT, NotificationMode.parse(""))
        assertEquals(NotificationMode.DEFAULT, NotificationMode.parse("MODE_D_UNE_AUTRE_VERSION"))
    }

    @Test
    fun `chaque mode fait l'aller-retour par son nom`() {
        NotificationMode.entries.forEach { mode ->
            assertEquals(mode, NotificationMode.parse(mode.name))
        }
    }
}
