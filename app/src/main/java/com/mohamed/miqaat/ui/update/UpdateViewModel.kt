package com.mohamed.miqaat.ui.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.miqaat.R
import com.mohamed.miqaat.data.update.ApkInstaller
import com.mohamed.miqaat.data.update.UpdateLog
import com.mohamed.miqaat.data.update.UpdateRepository
import com.mohamed.miqaat.domain.update.UpdateVerdict
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * L'écran de mise à jour : vérifier, télécharger, installer.
 *
 * ⚠ Le contexte est celui de l'**application**, jamais celui de l'activité : un
 * ViewModel survit aux rotations, une référence d'activité fuirait. Les intents
 * système partent donc avec `FLAG_ACTIVITY_NEW_TASK`, posé par [ApkInstaller].
 */
class UpdateViewModel(
    private val context: Context,
    private val repository: UpdateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    init {
        readStoredState()
        // Un téléchargement peut être en cours depuis une visite précédente : la
        // surcouche a pu tuer l'app pendant l'aller-retour vers les réglages.
        resumeDownloadIfAny()
    }

    /**
     * À chaque retour au premier plan : l'utilisateur revient peut-être de l'écran
     * des sources inconnues, qu'il vient d'accorder.
     */
    fun refreshOnResume() {
        _state.update { it.copy(canInstall = ApkInstaller.canInstall(context)) }
        resumeDownloadIfAny()
    }

    /** La vérification à la demande : elle ignore le délai de vingt-quatre heures. */
    fun checkNow() {
        if (_state.value.phase == UpdatePhase.CHECKING) return
        _state.update { it.copy(phase = UpdatePhase.CHECKING, errorRes = null) }
        viewModelScope.launch {
            val success = repository.refreshNow()
            readStoredState()
            _state.update {
                it.copy(
                    phase = UpdatePhase.IDLE,
                    errorRes = if (success) null else R.string.update_error_network,
                )
            }
        }
    }

    fun download() {
        val release = _state.value.release ?: return
        val id = ApkInstaller.enqueue(context, release)
        if (id == null) {
            _state.update { it.copy(errorRes = R.string.update_error_download) }
            return
        }
        UpdateLog.setDownload(context, id, release.tag)
        _state.update {
            it.copy(
                phase = UpdatePhase.DOWNLOADING,
                downloadedBytes = 0L,
                totalBytes = release.apkSizeBytes,
                errorRes = null,
            )
        }
        poll(id)
    }

    fun cancelDownload() {
        pollJob?.cancel()
        pollJob = null
        val id = UpdateLog.downloadId(context)
        if (id != UpdateLog.NO_DOWNLOAD) ApkInstaller.cancel(context, id)
        ApkInstaller.deleteDownloads(context)
        _state.update {
            it.copy(phase = UpdatePhase.IDLE, downloadedBytes = 0L, errorRes = null)
        }
    }

    /**
     * Deux marches : sans l'autorisation « sources inconnues », l'installateur ne
     * s'ouvrirait pas — autant y envoyer l'utilisateur plutôt que le laisser devant
     * un bouton sans effet.
     */
    fun install() {
        if (!ApkInstaller.canInstall(context)) {
            val opened = ApkInstaller.openUnknownSourcesSettings(context)
            _state.update { it.copy(manualHelpVisible = !opened) }
            return
        }
        val tag = _state.value.release?.tag ?: return
        val file = ApkInstaller.downloadedFile(context, tag)
        if (file == null) {
            _state.update { it.copy(phase = UpdatePhase.IDLE, errorRes = R.string.update_error_download) }
            return
        }
        val started = ApkInstaller.install(context, file)
        if (!started) {
            _state.update { it.copy(errorRes = R.string.update_error_install) }
        }
    }

    fun openUnknownSourcesSettings() {
        val opened = ApkInstaller.openUnknownSourcesSettings(context)
        _state.update { it.copy(manualHelpVisible = !opened) }
    }

    /** N'écarte que cette version-là : la suivante repassera d'elle-même. */
    fun skipThisVersion() {
        val tag = _state.value.release?.tag ?: return
        UpdateLog.skip(context, tag)
        _state.update { it.copy(updateAvailable = false) }
    }

    fun setAutoCheckEnabled(enabled: Boolean) {
        UpdateLog.setAutoCheckEnabled(context, enabled)
        _state.update { it.copy(autoCheckEnabled = enabled) }
    }

    fun openReleasePage() {
        val url = _state.value.release?.pageUrl ?: return
        ApkInstaller.openPage(context, url)
    }

    /**
     * Sondage du curseur de `DownloadManager` plutôt qu'un `BroadcastReceiver` :
     * `ACTION_DOWNLOAD_COMPLETE` ne donne que la fin, il faudrait sonder de toute
     * façon pour la progression — et un receiver enregistré à l'exécution devrait
     * déclarer son exposition depuis Android 14. Hors de cet écran, la notification
     * de `DownloadManager` fait le travail gratuitement.
     */
    private fun poll(id: Long) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                val progress = ApkInstaller.progress(context, id)
                if (progress == null) {
                    _state.update {
                        it.copy(phase = UpdatePhase.IDLE, errorRes = R.string.update_error_download)
                    }
                    return@launch
                }
                _state.update {
                    it.copy(
                        downloadedBytes = progress.downloadedBytes,
                        totalBytes = if (progress.totalBytes > 0) {
                            progress.totalBytes
                        } else {
                            it.totalBytes
                        },
                    )
                }
                when {
                    progress.failed -> {
                        UpdateLog.clearDownload(context)
                        _state.update {
                            it.copy(
                                phase = UpdatePhase.IDLE,
                                errorRes = R.string.update_error_download,
                            )
                        }
                        return@launch
                    }

                    progress.succeeded -> {
                        finishDownload()
                        return@launch
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /** Taille puis empreinte : un fichier qui ne correspond pas est effacé, pas proposé. */
    private suspend fun finishDownload() {
        val release = _state.value.release ?: return
        val file = ApkInstaller.downloadedFile(context, release.tag)
        if (file == null || !ApkInstaller.verify(file, release)) {
            ApkInstaller.deleteDownloads(context)
            UpdateLog.clearDownload(context)
            _state.update {
                it.copy(phase = UpdatePhase.IDLE, errorRes = R.string.update_error_checksum)
            }
            return
        }
        _state.update {
            it.copy(
                phase = UpdatePhase.READY,
                downloadedBytes = file.length(),
                totalBytes = file.length(),
                errorRes = null,
            )
        }
    }

    private fun resumeDownloadIfAny() {
        val id = UpdateLog.downloadId(context)
        if (id == UpdateLog.NO_DOWNLOAD) return
        val progress = ApkInstaller.progress(context, id) ?: return
        when {
            progress.running -> {
                _state.update { it.copy(phase = UpdatePhase.DOWNLOADING) }
                poll(id)
            }

            progress.succeeded -> viewModelScope.launch { finishDownload() }
        }
    }

    private fun readStoredState() {
        val installed = repository.installed()
        val release = UpdateLog.cachedRelease(context)
        _state.update {
            it.copy(
                installedName = installed.name,
                release = release,
                updateAvailable = release != null &&
                    release.tag != UpdateLog.skippedTag(context) &&
                    UpdateVerdict.isNewer(release, installed.name, installed.code),
                lastCheckAt = UpdateLog.lastCheckAt(context),
                autoCheckEnabled = UpdateLog.autoCheckEnabled(context),
                canInstall = ApkInstaller.canInstall(context),
            )
        }
    }

    private companion object {
        /** Assez fin pour que la barre bouge, assez lâche pour ne rien coûter. */
        const val POLL_INTERVAL_MS = 500L
    }
}
