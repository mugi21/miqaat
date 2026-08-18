package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.update.AppVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun `le v du tag n'est pas une composante`() {
        assertEquals(listOf(1, 2, 1), AppVersion.parse("v1.2.1"))
        assertEquals(listOf(1, 2, 1), AppVersion.parse("1.2.1"))
        assertEquals(listOf(1, 2, 1), AppVersion.parse("  V1.2.1  "))
    }

    @Test
    fun `la meme version n'est pas plus recente`() {
        assertFalse(AppVersion.isNewer("v1.2.1", "1.2.1"))
        assertFalse(AppVersion.isNewer("1.2.1", "1.2.1"))
    }

    @Test
    fun `une version superieure est plus recente`() {
        assertTrue(AppVersion.isNewer("v1.3", "1.2.1"))
        assertTrue(AppVersion.isNewer("v1.2.2", "1.2.1"))
        assertTrue(AppVersion.isNewer("v2.0", "1.9.9"))
    }

    /** Les composantes manquantes valent zéro : `1.2` est bien antérieure à `1.2.1`. */
    @Test
    fun `les composantes manquantes valent zero`() {
        assertFalse(AppVersion.isNewer("v1.2", "1.2.1"))
        assertTrue(AppVersion.isNewer("v1.2.1", "1.2"))
        assertEquals(0, AppVersion.compare(listOf(1, 2), listOf(1, 2, 0)))
    }

    /** Le piège classique : en lexicographique, « 1.10 » passerait pour antérieure à « 1.9 ». */
    @Test
    fun `la comparaison est numerique et non lexicographique`() {
        assertTrue(AppVersion.isNewer("v1.10", "1.9"))
        assertTrue(AppVersion.isNewer("v1.2.10", "1.2.9"))
        assertFalse(AppVersion.isNewer("v1.9", "1.10"))
    }

    /**
     * Une pré-version ne doit jamais se faire passer pour la version finale :
     * `v1.3-rc1` est refusée en entier, elle n'est pas coupée au tiret.
     */
    @Test
    fun `une pre-version est refusee et non tronquee`() {
        assertNull(AppVersion.parse("v1.3-rc1"))
        assertNull(AppVersion.parse("v1.3+build7"))
        assertFalse(AppVersion.isNewer("v1.3-rc1", "1.2.1"))
    }

    @Test
    fun `un tag illisible ne propose rien`() {
        assertNull(AppVersion.parse("nightly"))
        assertNull(AppVersion.parse(""))
        assertNull(AppVersion.parse(null))
        assertNull(AppVersion.parse("v"))
        assertNull(AppVersion.parse("1." + "9".repeat(100)))
        assertNull(AppVersion.parse("1.2.3.4.5"))
        assertFalse(AppVersion.isNewer("nightly", "1.2.1"))
    }

    /** Repli fermé : version installée illisible, on ne propose rien non plus. */
    @Test
    fun `une version installee illisible ferme le repli`() {
        assertFalse(AppVersion.isNewer("v9.9.9", ""))
        assertFalse(AppVersion.isNewer("v9.9.9", null))
        assertFalse(AppVersion.isNewer("v9.9.9", "inconnue"))
    }
}
