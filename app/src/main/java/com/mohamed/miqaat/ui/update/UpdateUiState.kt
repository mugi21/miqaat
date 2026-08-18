package com.mohamed.miqaat.ui.update

import androidx.annotation.StringRes
import com.mohamed.miqaat.domain.update.ReleaseInfo

/** Où en est l'écran de mise à jour. */
enum class UpdatePhase {
    /** Rien en cours : on affiche ce qu'on sait. */
    IDLE,

    /** Appel réseau en cours vers GitHub. */
    CHECKING,

    /** `DownloadManager` travaille. */
    DOWNLOADING,

    /** L'APK est sur le disque et son empreinte est bonne. */
    READY,
}

data class UpdateUiState(
    val installedName: String = "",
    val release: ReleaseInfo? = null,

    /** Vrai seulement si [release] est réellement plus récente que l'installée. */
    val updateAvailable: Boolean = false,

    /** `0` = aucune vérification n'a jamais abouti. */
    val lastCheckAt: Long = 0L,
    val autoCheckEnabled: Boolean = true,
    val phase: UpdatePhase = UpdatePhase.IDLE,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,

    /** L'autorisation « sources inconnues », relue à chaque retour au premier plan. */
    val canInstall: Boolean = false,

    /** Affiché seulement quand l'ouverture d'un écran système a échoué. */
    val manualHelpVisible: Boolean = false,
    @StringRes val errorRes: Int? = null,
) {
    val percent: Int
        get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

    /** Sans APK joint à la release, seul le repli navigateur reste offert. */
    val downloadable: Boolean get() = updateAvailable && release?.apkUrl != null
}
