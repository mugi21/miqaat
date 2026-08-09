package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.ReminderSettings
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmFreshnessTest {

    private val scheduled: Instant = Instant.parse("2026-08-09T04:12:00Z")

    @Test
    fun `une alerte a l'heure est fraiche`() {
        assertTrue(AlarmFreshness.isFresh(scheduled, scheduled, AlarmFreshness.ADHAN))
    }

    @Test
    fun `la borne de tolerance est inclusive`() {
        val now = scheduled.plus(AlarmFreshness.ADHAN)
        assertTrue(AlarmFreshness.isFresh(scheduled, now, AlarmFreshness.ADHAN))
    }

    @Test
    fun `une seconde apres la tolerance l'alerte est perimee`() {
        val now = scheduled.plus(AlarmFreshness.ADHAN).plusSeconds(1)
        assertFalse(AlarmFreshness.isFresh(scheduled, now, AlarmFreshness.ADHAN))
    }

    @Test
    fun `le symptome rapporte est bien filtre — le rappel du Fajr recu dix heures plus tard`() {
        val now = scheduled.plus(Duration.ofHours(10))
        assertFalse(AlarmFreshness.isFresh(scheduled, now, AlarmFreshness.REMINDER))
    }

    @Test
    fun `sans heure prevue l'alerte est affichee — intent d'une version anterieure`() {
        assertTrue(AlarmFreshness.isFresh(null, Instant.now(), AlarmFreshness.REMINDER))
    }

    @Test
    fun `un declenchement en avance reste frais`() {
        val now = scheduled.minusSeconds(30)
        assertTrue(AlarmFreshness.isFresh(scheduled, now, AlarmFreshness.REMINDER))
    }

    /**
     * Le verrou de D31 : la tolérance du rappel doit rester **strictement** sous
     * le délai de rappel le plus court, sinon un rappel périmé pourrait s'afficher
     * après l'adhan qu'il annonçait.
     */
    @Test
    fun `la tolerance du rappel est strictement inferieure au delai de rappel minimal`() {
        val shortestLead = Duration.ofMinutes(ReminderSettings.LEAD_CHOICES.min().toLong())
        assertTrue(AlarmFreshness.REMINDER < shortestLead)
    }

    @Test
    fun `chaque nature d'evenement de priere a sa tolerance`() {
        assertEquals(AlarmFreshness.ADHAN, AlarmFreshness.toleranceOf(PrayerEventKind.ADHAN))
        assertEquals(AlarmFreshness.REMINDER, AlarmFreshness.toleranceOf(PrayerEventKind.REMINDER))
    }
}
