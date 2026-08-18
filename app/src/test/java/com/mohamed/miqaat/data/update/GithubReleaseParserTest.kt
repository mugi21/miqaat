package com.mohamed.miqaat.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le passage JSON → domaine, sur des extraits de la réponse réelle de
 * `https://api.github.com/repos/mugi21/miqaat/releases/latest`, élaguée de la
 * centaine de champs dont l'app n'a que faire. Rien ici ne touche au réseau : le
 * parseur est une fonction pure prenant une chaîne.
 */
class GithubReleaseParserTest {

    private fun json(
        tag: String = "v1.3",
        draft: Boolean = false,
        prerelease: Boolean = false,
        body: String = "Notes de version.",
        assets: String = """
            {"name":"miqaat-1.3.apk","size":13400000,
             "browser_download_url":"https://github.com/mugi21/miqaat/releases/download/v1.3/miqaat-1.3.apk"}
        """.trimIndent(),
    ) = """
        {"tag_name":"$tag","name":"Miqaat 1.3 — مِيقات","draft":$draft,"prerelease":$prerelease,
         "html_url":"https://github.com/mugi21/miqaat/releases/tag/$tag",
         "body":${org.json.JSONObject.quote(body)},
         "assets":[$assets]}
    """.trimIndent()

    @Test
    fun `une release nominale est lue en entier`() {
        val release = GithubReleaseParser.parseLatest(json())
        assertNotNull(release)
        requireNotNull(release)
        assertEquals("v1.3", release.tag)
        assertEquals("Miqaat 1.3 — مِيقات", release.name)
        assertEquals("Notes de version.", release.notes)
        assertEquals("https://github.com/mugi21/miqaat/releases/tag/v1.3", release.pageUrl)
        assertEquals(
            "https://github.com/mugi21/miqaat/releases/download/v1.3/miqaat-1.3.apk",
            release.apkUrl,
        )
        assertEquals(13_400_000L, release.apkSizeBytes)
    }

    /** Une release joint aussi les sources et parfois un fichier de sommes. */
    @Test
    fun `l'apk est choisi parmi les autres fichiers joints`() {
        val assets = """
            {"name":"checksums.txt","size":120,
             "browser_download_url":"https://github.com/x/y/releases/download/v1.3/checksums.txt"},
            {"name":"miqaat-1.3.apk","size":13400000,
             "browser_download_url":"https://github.com/x/y/releases/download/v1.3/miqaat-1.3.apk"}
        """.trimIndent()
        assertEquals(
            "https://github.com/x/y/releases/download/v1.3/miqaat-1.3.apk",
            GithubReleaseParser.parseLatest(json(assets = assets))?.apkUrl,
        )
    }

    @Test
    fun `entre deux apk celui de la convention de nommage gagne`() {
        val assets = """
            {"name":"debug.apk","size":15400000,
             "browser_download_url":"https://github.com/x/y/releases/download/v1.3/debug.apk"},
            {"name":"miqaat-1.3.apk","size":13400000,
             "browser_download_url":"https://github.com/x/y/releases/download/v1.3/miqaat-1.3.apk"}
        """.trimIndent()
        assertEquals(13_400_000L, GithubReleaseParser.parseLatest(json(assets = assets))?.apkSizeBytes)
    }

    /**
     * Aucun APK joint : la release existe quand même, et l'écran n'offrira que le
     * repli navigateur. Masquer la version serait pire que ne pas savoir l'installer.
     */
    @Test
    fun `une release sans apk reste annoncee`() {
        val release = GithubReleaseParser.parseLatest(json(assets = ""))
        assertNotNull(release)
        assertNull(release?.apkUrl)
        assertEquals(0L, release?.apkSizeBytes)
    }

    @Test
    fun `une url d'asset en clair est rejetee`() {
        val assets = """
            {"name":"miqaat-1.3.apk","size":13400000,
             "browser_download_url":"http://github.com/x/y/releases/download/v1.3/miqaat-1.3.apk"}
        """.trimIndent()
        assertNull(GithubReleaseParser.parseLatest(json(assets = assets))?.apkUrl)
    }

    @Test
    fun `un brouillon ou une pre-version n'est jamais proposee`() {
        assertNull(GithubReleaseParser.parseLatest(json(draft = true)))
        assertNull(GithubReleaseParser.parseLatest(json(prerelease = true)))
    }

    @Test
    fun `une reponse illisible ne leve jamais`() {
        assertNull(GithubReleaseParser.parseLatest(""))
        assertNull(GithubReleaseParser.parseLatest("{}"))
        assertNull(GithubReleaseParser.parseLatest("""{"tag_name":"v1.3","assets":["""))
        assertNull(GithubReleaseParser.parseLatest("""{"tag_name":"  "}"""))
    }

    /**
     * Le gabarit de `docs/release.md` publie l'empreinte de l'APK **puis** celle
     * du certificat. La première étiquetée est la bonne : la seconde est annoncée
     * par « SHA-256 du certificat », qui ne suit pas la forme `clé : valeur`.
     */
    @Test
    fun `l'empreinte etiquetee est lue avant celle du certificat`() {
        val body = """
            - Fichier : `miqaat-1.3.apk`
            - SHA-256 : `0CF16634505D16C98B282806E77D2221996F2DEA410916D32A82EA7500C85BE4`
            - Signature v2 + v3, certificat `CN=Mohamed Boughouas, O=Miqaat, C=DZ`
              (SHA-256 du certificat : `1af97066f2706edbc7d5704bace12929f74253dedaeeacf75743b04f3ba3510d`)
        """.trimIndent()
        assertEquals(
            "0cf16634505d16c98b282806e77d2221996f2dea410916d32a82ea7500c85be4",
            GithubReleaseParser.sha256In(body),
        )
    }

    @Test
    fun `une empreinte non etiquetee est acceptee si elle est unique`() {
        val unique = "a".repeat(64)
        assertEquals(unique, GithubReleaseParser.sha256In("Empreinte $unique"))
        // Deux jetons : ambiguïté, donc pas de vérification — mais jamais de blocage.
        assertNull(GithubReleaseParser.sha256In("$unique et ${"b".repeat(64)}"))
        assertNull(GithubReleaseParser.sha256In("Aucune empreinte ici."))
    }

    @Test
    fun `la ligne versionCode est lue quand elle existe`() {
        assertEquals(6L, GithubReleaseParser.versionCodeIn("Notes\nversionCode: 6\nFin"))
        assertEquals(6L, GithubReleaseParser.versionCodeIn("VERSIONCODE = 6"))
        assertNull(GithubReleaseParser.versionCodeIn("Notes sans rien"))
        assertNull(GithubReleaseParser.versionCodeIn("versionCode: abc"))
    }

    @Test
    fun `le corps de la release alimente empreinte et versionCode`() {
        val body = "SHA-256 : ${"f".repeat(64)}\nversionCode: 7"
        val release = GithubReleaseParser.parseLatest(json(body = body))
        assertEquals("f".repeat(64), release?.sha256)
        assertEquals(7L, release?.versionCode)
    }

    /** Sans `html_url` exploitable, on sait quand même reconstruire la page du tag. */
    @Test
    fun `une page de release manquante est reconstruite depuis le tag`() {
        val json = """{"tag_name":"v1.3","body":"","assets":[]}"""
        val release = GithubReleaseParser.parseLatest(json)
        assertTrue(release?.pageUrl?.endsWith("/releases/tag/v1.3") == true)
    }
}
