package com.mohamed.miqaat.domain

import com.mohamed.miqaat.domain.model.Moshaf
import com.mohamed.miqaat.domain.model.Surah
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranAudioTest {

    private fun moshaf(server: String, surahIds: Set<Int> = (1..114).toSet()) = Moshaf(
        id = 1,
        reciterId = 1,
        name = "Rewayat Hafs A'n Assem - Murattal",
        server = server,
        surahIds = surahIds,
    )

    @Test
    fun `le numero de sourate est sur trois chiffres`() {
        assertEquals("001.mp3", QuranAudio.fileName(1))
        assertEquals("018.mp3", QuranAudio.fileName(18))
        assertEquals("114.mp3", QuranAudio.fileName(114))
    }

    @Test
    fun `l'url colle la racine et le fichier`() {
        val url = QuranAudio.audioUrl(moshaf("https://server6.mp3quran.net/akdr/"), 1)
        assertEquals("https://server6.mp3quran.net/akdr/001.mp3", url)
    }

    /** L'API met un `/` final, mais rien ne l'y oblige : on ne s'y fie pas. */
    @Test
    fun `un serveur sans barre finale donne la meme url`() {
        val url = QuranAudio.audioUrl(moshaf("https://server6.mp3quran.net/akdr"), 1)
        assertEquals("https://server6.mp3quran.net/akdr/001.mp3", url)
    }

    /**
     * Une sourate absente ne doit pas produire d'URL : la renvoyer quand même
     * donnerait un 404 quelques secondes après le début du chargement.
     */
    @Test
    fun `une sourate absente du moshaf n'a pas d'url`() {
        assertNull(QuranAudio.audioUrl(moshaf("https://x/", setOf(1, 2, 3)), 18))
    }

    @Test
    fun `la file part de la sourate choisie et va jusqu'a la fin`() {
        val queue = QuranAudio.queueFrom(moshaf("https://x/"), 112)
        assertEquals(listOf(112, 113, 114), queue)
    }

    @Test
    fun `la file saute les sourates que le recitateur n'a pas`() {
        val partiel = moshaf("https://x/", setOf(1, 18, 36, 55, 67))
        assertEquals(listOf(36, 55, 67), QuranAudio.queueFrom(partiel, 20))
    }

    @Test
    fun `la file de la premiere sourate couvre tout le moshaf complet`() {
        val queue = QuranAudio.queueFrom(moshaf("https://x/"), Surah.FIRST_ID)
        assertEquals(Surah.COUNT, queue.size)
        assertEquals(Surah.LAST_ID, queue.last())
    }

    @Test
    fun `la file est vide si rien ne suit`() {
        val partiel = moshaf("https://x/", setOf(1, 2))
        assertTrue(QuranAudio.queueFrom(partiel, 50).isEmpty())
    }
}
