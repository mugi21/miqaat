package com.mohamed.miqaat.data.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le passage JSON → domaine, sur des extraits repris de la documentation
 * officielle de mp3quran (`https://www.mp3quran.net/ar/api`). Rien ici ne touche
 * au réseau : le parseur est une fonction pure prenant une chaîne.
 */
class Mp3QuranParserTest {

    @Test
    fun `un recitateur et son moshaf sont lus`() {
        val json = """
            {"reciters":[{"id":1,"name":"إبراهيم الأخضر","letter":"إ",
             "moshaf":[{"id":1,"name":"حفص عن عاصم - مرتل",
             "server":"https://server6.mp3quran.net/akdr/","surah_total":114,
             "surah_list":"1,2,3,18,114"}]}]}
        """.trimIndent()

        val reciters = Mp3QuranParser.parseReciters(json)
        assertEquals(1, reciters.size)
        val reciter = reciters.single()
        assertEquals("إبراهيم الأخضر", reciter.name)
        val moshaf = reciter.moshafs.single()
        assertEquals("https://server6.mp3quran.net/akdr/", moshaf.server)
        assertEquals(setOf(1, 2, 3, 18, 114), moshaf.surahIds)
        assertEquals(1, moshaf.reciterId)
    }

    /**
     * ⚠ La documentation officielle montre elle-même une `surah_list` à virgule
     * traînante (l'exemple de Ahmad Deban). Le jeton vide ne doit rien casser.
     */
    @Test
    fun `une surah_list a virgule trainante est toleree`() {
        val json = """
            {"reciters":[{"id":265,"name":"Ahmad Deban","letter":"A",
             "moshaf":[{"id":280,"name":"Rewayat Qalon","server":"https://s/x/",
             "surah_list":"1,2,3,"}]}]}
        """.trimIndent()

        val moshaf = Mp3QuranParser.parseReciters(json).single().moshafs.single()
        assertEquals(setOf(1, 2, 3), moshaf.surahIds)
    }

    @Test
    fun `les numeros hors des 114 sourates sont ecartes`() {
        val json = """
            {"reciters":[{"id":1,"name":"X","letter":"X",
             "moshaf":[{"id":1,"name":"m","server":"https://s/",
             "surah_list":"0,1,115,114,abc"}]}]}
        """.trimIndent()

        val moshaf = Mp3QuranParser.parseReciters(json).single().moshafs.single()
        assertEquals(setOf(1, 114), moshaf.surahIds)
    }

    /** Un moshaf n'a pas toujours les 114 : celui de Hazza Al-Balushi en compte 83. */
    @Test
    fun `un moshaf partiel garde exactement ce qu'il annonce`() {
        val json = """
            {"reciters":[{"id":231,"name":"Hazza Al-Balushi","letter":"H",
             "moshaf":[{"id":231,"name":"Murattal","server":"https://server11.mp3quran.net/hazza/",
             "surah_list":"1,6,13,18,67"}]}]}
        """.trimIndent()

        val moshaf = Mp3QuranParser.parseReciters(json).single().moshafs.single()
        assertEquals(5, moshaf.surahIds.size)
        assertTrue(moshaf.has(18))
        assertTrue(!moshaf.has(2))
    }

    @Test
    fun `un recitateur sans moshaf lisible est ecarte`() {
        val json = """
            {"reciters":[
             {"id":1,"name":"Sans serveur","letter":"S","moshaf":[{"id":1,"name":"m","surah_list":"1,2"}]},
             {"id":2,"name":"Sans sourate","letter":"S","moshaf":[{"id":2,"name":"m","server":"https://s/","surah_list":""}]},
             {"id":3,"name":"Correct","letter":"C","moshaf":[{"id":3,"name":"m","server":"https://s/","surah_list":"1"}]}
            ]}
        """.trimIndent()

        val reciters = Mp3QuranParser.parseReciters(json)
        assertEquals(listOf("Correct"), reciters.map { it.name })
    }

    @Test
    fun `les sourates sont lues avec leur origine`() {
        val json = """
            {"suwar":[
             {"id":1,"name":"الفاتحة ","start_page":1,"end_page":1,"makkia":1,"type":0},
             {"id":2,"name":"البقرة","start_page":2,"end_page":49,"makkia":0,"type":1}
            ]}
        """.trimIndent()

        val suwar = Mp3QuranParser.parseSuwar(json)
        assertEquals(2, suwar.size)
        // ⚠ Les noms de l'API portent souvent une espace finale.
        assertEquals("الفاتحة", suwar.first().name)
        assertTrue(suwar.first().makki)
        assertTrue(!suwar[1].makki)
    }

    @Test
    fun `un json tronque ne leve pas et rend une liste vide`() {
        assertTrue(Mp3QuranParser.parseReciters("""{"reciters":[{"id":1,""").isEmpty())
        assertTrue(Mp3QuranParser.parseSuwar("pas du json").isEmpty())
        assertTrue(Mp3QuranParser.parseReciters("").isEmpty())
    }

    @Test
    fun `une reponse d'une autre forme rend une liste vide`() {
        assertTrue(Mp3QuranParser.parseReciters("""{"error":"not found"}""").isEmpty())
    }

    /**
     * ⚠ Le piège relevé sur `/api/v3/languages` : l'anglais de mp3quran est
     * `eng`, pas `en`. Un code inconnu ne provoque aucune erreur côté API — elle
     * retombe sur l'arabe, et le catalogue serait en arabe dans une app en
     * anglais. C'est donc un test, pas un commentaire.
     */
    @Test
    fun `l'anglais de l'API est eng et non en`() {
        assertEquals("eng", Mp3QuranLanguage.forTag("en").code)
        assertEquals("fr", Mp3QuranLanguage.forTag("fr").code)
        assertEquals("ar", Mp3QuranLanguage.forTag("ar").code)
    }

    @Test
    fun `une langue inconnue ou absente retombe sur l'arabe`() {
        assertEquals(Mp3QuranLanguage.ARABIC, Mp3QuranLanguage.forTag(null))
        assertEquals(Mp3QuranLanguage.ARABIC, Mp3QuranLanguage.forTag("de"))
        assertEquals(Mp3QuranLanguage.ARABIC, Mp3QuranLanguage.forTag(""))
    }

    @Test
    fun `les etiquettes composees et la casse sont acceptees`() {
        assertEquals(Mp3QuranLanguage.ENGLISH, Mp3QuranLanguage.forTag("en-US"))
        assertEquals(Mp3QuranLanguage.FRENCH, Mp3QuranLanguage.forTag("FR"))
        assertNotNull(Mp3QuranLanguage.forTag("ar-DZ"))
        assertEquals(Mp3QuranLanguage.ARABIC, Mp3QuranLanguage.forTag("ar-DZ"))
    }
}
