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

/**
 * Le décompte des versets est en dur (l'API ne le donne pas). Une faute de
 * frappe dans 114 nombres serait invisible à l'œil : c'est la somme connue du
 * décompte de Kūfa qui la rend détectable.
 */
class SurahAyahCountTest {

    @Test
    fun `les 114 sourates ont un decompte`() {
        (Surah.FIRST_ID..Surah.LAST_ID).forEach { id ->
            assertTrue("sourate $id", Surah.ayahCountOf(id) > 0)
        }
    }

    @Test
    fun `la somme des versets vaut le total du decompte de Kufa`() {
        val total = (Surah.FIRST_ID..Surah.LAST_ID).sumOf(Surah::ayahCountOf)
        assertEquals(Surah.TOTAL_AYAHS, total)
    }

    @Test
    fun `quelques reperes connus`() {
        assertEquals(7, Surah.ayahCountOf(1))     // al-Fātiḥa
        assertEquals(286, Surah.ayahCountOf(2))   // al-Baqara, la plus longue
        assertEquals(110, Surah.ayahCountOf(18))  // al-Kahf
        assertEquals(83, Surah.ayahCountOf(36))   // Yā-Sīn
        assertEquals(78, Surah.ayahCountOf(55))   // ar-Raḥmān
        assertEquals(30, Surah.ayahCountOf(67))   // al-Mulk
        assertEquals(6, Surah.ayahCountOf(114))   // an-Nās
    }

    @Test
    fun `un numero hors bornes ne leve pas`() {
        assertEquals(0, Surah.ayahCountOf(0))
        assertEquals(0, Surah.ayahCountOf(115))
    }
}
