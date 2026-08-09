package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.reliability.CheckState
import com.mohamed.miqaat.domain.reliability.ReliabilityCheck
import com.mohamed.miqaat.domain.reliability.ReliabilityStatus
import com.mohamed.miqaat.domain.reliability.ReliabilityVerdict
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReliabilityVerdictTest {

    private val now = 1_754_700_000_000L

    private fun status(check: ReliabilityCheck, state: CheckState) =
        ReliabilityStatus(check, state)

    private fun warns(vararg statuses: ReliabilityStatus, dismissedUntil: Long = 0L) =
        ReliabilityVerdict.shouldWarnOnHome(statuses.toList(), dismissedUntil, now)

    /**
     * La règle anti-harcèlement de D34 : l'état du démarrage automatique n'est pas
     * lisible, en faire une alerte condamnerait tout possesseur de Xiaomi à une
     * bannière permanente qu'aucune action ne pourrait éteindre.
     */
    @Test
    fun `un etat inconnu n'alarme jamais`() {
        assertFalse(warns(status(ReliabilityCheck.OEM_AUTOSTART, CheckState.UNKNOWN)))
        assertFalse(warns(status(ReliabilityCheck.DELIVERY, CheckState.UNKNOWN)))
    }

    @Test
    fun `un probleme non critique n'alarme pas sur l'accueil`() {
        assertFalse(warns(status(ReliabilityCheck.BATTERY, CheckState.ACTION_NEEDED)))
    }

    @Test
    fun `un probleme critique et certain alarme`() {
        assertTrue(warns(status(ReliabilityCheck.NOTIFICATIONS, CheckState.ACTION_NEEDED)))
        assertTrue(warns(status(ReliabilityCheck.EXACT_ALARMS, CheckState.ACTION_NEEDED)))
        assertTrue(warns(status(ReliabilityCheck.DELIVERY, CheckState.ACTION_NEEDED)))
    }

    @Test
    fun `tout en ordre n'alarme pas`() {
        assertFalse(
            warns(
                status(ReliabilityCheck.NOTIFICATIONS, CheckState.OK),
                status(ReliabilityCheck.EXACT_ALARMS, CheckState.NOT_APPLICABLE),
                status(ReliabilityCheck.BATTERY, CheckState.OK),
                status(ReliabilityCheck.OEM_AUTOSTART, CheckState.UNKNOWN),
                status(ReliabilityCheck.DELIVERY, CheckState.OK),
            ),
        )
    }

    @Test
    fun `un report en cours fait taire meme un probleme critique`() {
        assertFalse(
            warns(
                status(ReliabilityCheck.NOTIFICATIONS, CheckState.ACTION_NEEDED),
                dismissedUntil = now + TimeUnit.DAYS.toMillis(1),
            ),
        )
    }

    @Test
    fun `un report expire laisse la banniere revenir`() {
        assertTrue(
            warns(
                status(ReliabilityCheck.NOTIFICATIONS, CheckState.ACTION_NEEDED),
                dismissedUntil = now - 1,
            ),
        )
    }

    @Test
    fun `la criticite distingue ce qui rend l'app inutile de ce qui la fragilise`() {
        assertTrue(ReliabilityVerdict.isCritical(ReliabilityCheck.NOTIFICATIONS))
        assertTrue(ReliabilityVerdict.isCritical(ReliabilityCheck.EXACT_ALARMS))
        assertTrue(ReliabilityVerdict.isCritical(ReliabilityCheck.DELIVERY))
        assertFalse(ReliabilityVerdict.isCritical(ReliabilityCheck.BATTERY))
        assertFalse(ReliabilityVerdict.isCritical(ReliabilityCheck.OEM_AUTOSTART))
    }
}
