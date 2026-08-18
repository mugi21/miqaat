package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.update.ReleaseInfo
import com.mohamed.miqaat.domain.update.UpdateVerdict
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVerdictTest {

    private val now = 1_755_000_000_000L

    private fun release(tag: String = "v1.3", versionCode: Long? = null) = ReleaseInfo(
        tag = tag,
        name = "Miqaat 1.3",
        notes = "",
        pageUrl = "https://github.com/mugi21/miqaat/releases/tag/$tag",
        apkUrl = "https://github.com/mugi21/miqaat/releases/download/$tag/miqaat-1.3.apk",
        apkSizeBytes = 13_400_000L,
        sha256 = null,
        versionCode = versionCode,
    )

    private fun shows(
        release: ReleaseInfo? = release(),
        installedName: String = "1.2.1",
        installedCode: Long = 5L,
        skippedTag: String? = null,
        snoozedUntil: Long = 0L,
        enabled: Boolean = true,
    ) = UpdateVerdict.shouldShowOnHome(
        release = release,
        installedName = installedName,
        installedCode = installedCode,
        skippedTag = skippedTag,
        snoozedUntil = snoozedUntil,
        enabled = enabled,
        now = now,
    )

    @Test
    fun `une version plus recente s'annonce`() {
        assertTrue(shows())
    }

    /** L'opt-out coupe tout, y compris l'affichage de ce qui a déjà été trouvé. */
    @Test
    fun `la verification desactivee n'annonce rien`() {
        assertFalse(shows(enabled = false))
    }

    @Test
    fun `plus tard fait taire la note jusqu'a son echeance`() {
        assertFalse(shows(snoozedUntil = now + TimeUnit.DAYS.toMillis(1)))
        assertTrue(shows(snoozedUntil = now - 1))
    }

    /** « Ignorer cette version » ne vaut que pour elle : la suivante repasse. */
    @Test
    fun `une version ignoree ne bloque pas la suivante`() {
        assertFalse(shows(skippedTag = "v1.3"))
        assertTrue(shows(release = release(tag = "v1.4"), skippedTag = "v1.3"))
    }

    @Test
    fun `sans release en cache il n'y a rien a annoncer`() {
        assertFalse(shows(release = null))
    }

    @Test
    fun `la version installee ou anterieure ne s'annonce pas`() {
        assertFalse(shows(release = release(tag = "v1.2.1")))
        assertFalse(shows(release = release(tag = "v1.2")))
    }

    /**
     * Le veto de D44 : Android refuse un `versionCode` non croissant, et il le
     * refuse après le téléchargement. Le tag dit « 1.3 », le corps dit « 5 »,
     * et 5 est déjà installé — on ne propose rien.
     */
    @Test
    fun `le versionCode du corps a le dernier mot`() {
        assertFalse(shows(release = release(versionCode = 5L)))
        assertFalse(shows(release = release(versionCode = 4L)))
        assertTrue(shows(release = release(versionCode = 6L)))
    }

    /** Ligne absente : le tag décide seul, un oubli de rédaction n'éteint rien. */
    @Test
    fun `un versionCode absent laisse le tag decider`() {
        assertTrue(UpdateVerdict.isNewer(release(versionCode = null), "1.2.1", 5L))
    }
}
