package com.mohamed.miqaat.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.mohamed.miqaat.R
import com.mohamed.miqaat.domain.update.ReleaseInfo
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** L'état d'un téléchargement, tel que le curseur de `DownloadManager` le rend. */
data class DownloadProgress(
    val status: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
) {
    val running: Boolean
        get() = status != DownloadManager.STATUS_SUCCESSFUL && status != DownloadManager.STATUS_FAILED

    val succeeded: Boolean get() = status == DownloadManager.STATUS_SUCCESSFUL
    val failed: Boolean get() = status == DownloadManager.STATUS_FAILED
}

/**
 * Télécharger l'APK d'une release, en vérifier l'empreinte, et le confier à
 * l'installateur du système (D44).
 *
 * Tout est enveloppé de `runCatching` : `com.android.providers.downloads` peut
 * être désactivé (cas réel sur MIUI), le stockage externe peut être démonté,
 * l'écran système visé peut ne pas exister. Chaque échec ramène au repli
 * navigateur, jamais à un plantage.
 */
object ApkInstaller {

    private const val TAG = "ApkInstaller"
    private const val MIME = "application/vnd.android.package-archive"
    private const val AUTHORITY_SUFFIX = ".updates"

    /**
     * `DownloadManager` écrit dans le dossier privé de l'app : **aucune permission
     * de stockage à aucun niveau d'API**, hors du champ du scoped storage, et le
     * fichier disparaît à la désinstallation.
     *
     * @return l'identifiant du téléchargement, ou `null` si rien n'a pu être lancé.
     */
    fun enqueue(context: Context, release: ReleaseInfo): Long? = runCatching {
        val url = release.apkUrl ?: return null
        val manager = context.getSystemService(DownloadManager::class.java) ?: return null
        deleteDownloads(context)
        val request = DownloadManager.Request(url.toUri())
            .setTitle(context.getString(R.string.app_name))
            .setDescription(release.tag)
            // Certaines surcouches n'ouvrent l'installateur depuis leur notification
            // « téléchargement terminé » que si le type est déclaré.
            .setMimeType(MIME)
            // La tape de l'utilisateur *est* le consentement : on ne lui refuse pas
            // sa mise à jour parce qu'il est en données mobiles.
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
            )
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                fileNameFor(release.tag),
            )
        manager.enqueue(request)
    }.getOrElse { error ->
        Log.w(TAG, "Téléchargement impossible", error)
        null
    }

    fun progress(context: Context, id: Long): DownloadProgress? = runCatching {
        val manager = context.getSystemService(DownloadManager::class.java) ?: return null
        manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (cursor == null || !cursor.moveToFirst()) return null
            DownloadProgress(
                status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                downloadedBytes = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                ),
                totalBytes = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                ),
            )
        }
    }.getOrNull()

    fun cancel(context: Context, id: Long) {
        runCatching { context.getSystemService(DownloadManager::class.java)?.remove(id) }
        UpdateLog.clearDownload(context)
    }

    /** Le fichier attendu pour ce tag, s'il existe et n'est pas vide. */
    fun downloadedFile(context: Context, tag: String): File? = runCatching {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return null
        File(directory, fileNameFor(tag)).takeIf { it.isFile && it.length() > 0 }
    }.getOrNull()

    /**
     * Taille puis empreinte, dans cet ordre : la première est gratuite et attrape
     * une troncature, la seconde coûte une centaine de millisecondes.
     *
     * ⚠ Une empreinte absente des notes de version **n'invalide rien** : elle est
     * publiée par convention (`docs/release.md`), pas garantie. Et il faut être
     * honnête sur ce qu'elle prouve — arrivant par le même canal TLS que l'APK,
     * elle protège de la corruption, pas d'un dépôt compromis. La vraie protection
     * d'authenticité est ailleurs et gratuite : Android refuse d'installer par-dessus
     * l'existant un APK qui ne porte pas notre signature.
     */
    suspend fun verify(file: File, release: ReleaseInfo): Boolean = withContext(Dispatchers.IO) {
        if (release.apkSizeBytes > 0 && file.length() != release.apkSizeBytes) {
            Log.w(TAG, "Taille inattendue : ${file.length()} au lieu de ${release.apkSizeBytes}")
            return@withContext false
        }
        val expected = release.sha256 ?: return@withContext true
        val actual = runCatching { sha256Of(file) }.getOrNull() ?: return@withContext false
        (actual == expected.lowercase()).also {
            if (!it) Log.w(TAG, "Empreinte inattendue : $actual")
        }
    }

    /**
     * Passe le fichier à l'installateur du système.
     *
     * ⚠ `FLAG_GRANT_READ_URI_PERMISSION` n'est pas facultatif : sans lui,
     * l'installateur ne peut pas lire le `content://` et affiche « Analyse
     * impossible ». Et `ACTION_INSTALL_PACKAGE` est déprécié depuis l'API 29 :
     * c'est bien `ACTION_VIEW` avec ce type MIME, le chemin le plus banal, donc
     * celui que les surcouches reconnaissent.
     */
    fun install(context: Context, file: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + AUTHORITY_SUFFIX,
            file,
        )
        context.launch(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, MIME)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }.getOrElse { error ->
        Log.w(TAG, "Installateur injoignable", error)
        false
    }

    /**
     * Existe depuis l'API 26, donc aucune garde de version (`minSdk 26`). En
     * revanche le résultat **périme** : l'utilisateur accorde l'autorisation sur
     * un écran système et revient, d'où la relecture à chaque `ON_RESUME`.
     */
    fun canInstall(context: Context): Boolean =
        runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)

    /** Cascade de replis, comme pour l'optimisation de batterie : du plus précis au plus large. */
    fun openUnknownSourcesSettings(context: Context): Boolean =
        context.launch(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData(Uri.fromParts("package", context.packageName, null)),
        ) ||
            context.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)) ||
            context.launch(Intent(Settings.ACTION_SECURITY_SETTINGS))

    /**
     * Le repli universel : la page de la release dans le navigateur. Intent
     * implicite `ACTION_VIEW https`, donc hors du filtrage de visibilité
     * d'Android 11 — c'est `resolveActivity` qui est filtré, et on n'en utilise pas.
     */
    fun openPage(context: Context, url: String): Boolean =
        url.startsWith("https://") && context.launch(Intent(Intent.ACTION_VIEW, url.toUri()))

    /** Un seul APK à la fois sur le disque : le précédent n'a plus rien à y faire. */
    fun deleteDownloads(context: Context) {
        runCatching {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?.listFiles { file: File -> file.extension.equals("apk", ignoreCase = true) }
                ?.forEach { it.delete() }
        }
    }

    /**
     * ⚠ Construit depuis **notre** tag, jamais depuis le `name` de l'asset : un nom
     * de fichier venu d'un JSON distant est exactement le genre de détail qui
     * finit en CVE.
     */
    private fun fileNameFor(tag: String): String =
        "miqaat-" + tag.filter { it.isLetterOrDigit() || it == '.' || it == '-' } + ".apk"

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun Context.launch(intent: Intent): Boolean =
        runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Log.w(TAG, "Écran système injoignable : ${intent.action}", it) }
            .isSuccess
}
