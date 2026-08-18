package com.mohamed.miqaat.data.update

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Les notes de version ne sont plus seulement lues par des humains : depuis la
 * v1.3, l'application y cherche l'empreinte de l'APK et le `versionCode`.
 *
 * Une note mal formée ne casse rien de visible — elle prive silencieusement les
 * utilisateurs de la vérification d'empreinte, ou pire, laisse proposer une
 * version qu'Android refusera d'installer. Ce test lit les **vrais** fichiers de
 * `docs/`, ceux qu'on colle dans le formulaire GitHub, et vérifie qu'ils
 * respectent le contrat de [updates.md](../../docs/updates.md).
 *
 * ⚠ Le répertoire de travail des tests Gradle est le module (`app/`), d'où le `..`.
 */
class ReleaseNotesContractTest {

    private val notesDirectory = File("../docs")

    /** Les notes antérieures à la v1.3 sont d'avant le contrat : rien à leur demander. */
    private val contractual = setOf("release-notes-v1.3.md")

    @Test
    fun `les notes publiees livrent l'empreinte de l'APK et non celle du certificat`() {
        forEachContractualNotes { name, body ->
            val sha = GithubReleaseParser.sha256In(body)
            assertNotNull("$name : aucune empreinte lisible", sha)
            requireNotNull(sha)
            assertEquals("$name : empreinte de longueur inattendue", 64, sha.length)
            // Le gabarit publie deux empreintes ; celle du certificat ne doit
            // jamais gagner, sinon toute vérification échouerait après le
            // téléchargement et l'APK serait effacé sans explication.
            assertTrue(
                "$name : c'est l'empreinte du certificat qui a été lue",
                sha != CERTIFICATE_SHA256,
            )
        }
    }

    @Test
    fun `les notes publiees portent une ligne versionCode lisible`() {
        forEachContractualNotes { name, body ->
            val code = GithubReleaseParser.versionCodeIn(body)
            assertNotNull("$name : ligne versionCode absente ou mal formée", code)
            assertTrue("$name : versionCode non positif", (code ?: 0L) > 0L)
        }
    }

    private fun forEachContractualNotes(block: (String, String) -> Unit) {
        val files = notesDirectory.listFiles { file -> file.name in contractual }.orEmpty()
        // Si les fichiers ne sont pas là, c'est le test qui est cassé, pas les notes.
        assertEquals("Fichiers de notes introuvables dans ${notesDirectory.absolutePath}",
            contractual.size, files.size)
        files.forEach { block(it.name, it.readText()) }
    }

    private companion object {
        /** L'empreinte du certificat de signature, qui figure dans chaque note. */
        const val CERTIFICATE_SHA256 =
            "1af97066f2706edbc7d5704bace12929f74253dedaeeacf75743b04f3ba3510d"
    }
}
