package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.NotificationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La matrice complète du mode d'alerte : quatre réglages × trois états du
 * téléphone. C'est ce tableau qui décide si l'adhan s'entend, d'où une assertion
 * par case plutôt qu'un test « représentatif ».
 */
class AlertResolverTest {

    private fun resolve(mode: NotificationMode, ringer: RingerState) =
        AlertResolver.resolve(mode, ringer)

    // --- FOLLOW_PHONE : on reproduit ce que faisait le canal de notification ---

    @Test
    fun `suivre le telephone en mode sonnerie sonne sur le flux de la sonnerie`() {
        val decision = resolve(NotificationMode.FOLLOW_PHONE, RingerState.NORMAL)
        assertEquals(AlertStream.RINGTONE, decision.stream)
        assertEquals(VibrationStyle.SINGLE, decision.vibration)
    }

    @Test
    fun `suivre le telephone en vibreur ne joue rien et vibre`() {
        val decision = resolve(NotificationMode.FOLLOW_PHONE, RingerState.VIBRATE)
        assertNull(decision.stream)
        assertEquals(VibrationStyle.SUSTAINED, decision.vibration)
    }

    @Test
    fun `suivre le telephone en silencieux ne fait rien`() {
        val decision = resolve(NotificationMode.FOLLOW_PHONE, RingerState.SILENT)
        assertTrue(decision.isSilent)
    }

    // --- ALWAYS_SOUND : le flux « alarme » n'est emprunté que lorsqu'il le faut ---

    @Test
    fun `toujours sonner emprunte la sonnerie quand le telephone sonne`() {
        val decision = resolve(NotificationMode.ALWAYS_SOUND, RingerState.NORMAL)
        assertEquals(AlertStream.RINGTONE, decision.stream)
    }

    @Test
    fun `toujours sonner passe par le flux alarme en vibreur`() {
        val decision = resolve(NotificationMode.ALWAYS_SOUND, RingerState.VIBRATE)
        assertEquals(AlertStream.ALARM, decision.stream)
        assertEquals(VibrationStyle.SINGLE, decision.vibration)
    }

    @Test
    fun `toujours sonner passe par le flux alarme en silencieux`() {
        val decision = resolve(NotificationMode.ALWAYS_SOUND, RingerState.SILENT)
        assertEquals(AlertStream.ALARM, decision.stream)
    }

    // --- ALWAYS_VIBRATE et SILENT : insensibles à l'état du téléphone ---

    @Test
    fun `toujours vibrer ne joue jamais de son quel que soit l'etat du telephone`() {
        RingerState.entries.forEach { ringer ->
            val decision = resolve(NotificationMode.ALWAYS_VIBRATE, ringer)
            assertNull("Ringer $ringer", decision.stream)
            assertEquals("Ringer $ringer", VibrationStyle.SUSTAINED, decision.vibration)
        }
    }

    @Test
    fun `toujours silencieux ne fait rien quel que soit l'etat du telephone`() {
        RingerState.entries.forEach { ringer ->
            assertTrue("Ringer $ringer", resolve(NotificationMode.SILENT, ringer).isSilent)
        }
    }

    // --- Invariants de la décision elle-même ---

    @Test
    fun `seuls SILENT et FOLLOW_PHONE en silencieux sont totalement muets`() {
        val silentCases = NotificationMode.entries.flatMap { mode ->
            RingerState.entries.map { ringer -> Triple(mode, ringer, resolve(mode, ringer)) }
        }.filter { (_, _, decision) -> decision.isSilent }

        val expected = RingerState.entries.map { NotificationMode.SILENT to it } +
            listOf(NotificationMode.FOLLOW_PHONE to RingerState.SILENT)

        assertEquals(
            expected.toSet(),
            silentCases.map { (mode, ringer, _) -> mode to ringer }.toSet(),
        )
    }

    @Test
    fun `playsSound et stream ne peuvent pas se contredire`() {
        NotificationMode.entries.forEach { mode ->
            RingerState.entries.forEach { ringer ->
                val decision = resolve(mode, ringer)
                assertEquals(decision.stream != null, decision.playsSound)
            }
        }
    }

    @Test
    fun `une decision qui joue un son n'est jamais consideree comme muette`() {
        val decision = resolve(NotificationMode.ALWAYS_SOUND, RingerState.SILENT)
        assertFalse(decision.isSilent)
    }
}
