package com.mohamed.miqaat.domain.update

/**
 * Une release GitHub, réduite à ce dont l'app a besoin.
 *
 * Tout est persisté au moment de la vérification : après **une** vérification
 * réussie, la note d'accueil et l'écran entier — notes de version comprises —
 * s'affichent hors ligne. Seul le téléchargement demande le réseau.
 */
data class ReleaseInfo(
    /** `tag_name`, tel quel : `v1.3`. */
    val tag: String,

    /** Titre de la release, ou le tag si elle n'en porte pas. */
    val name: String,

    /** Corps de la release, en markdown brut, affiché tel quel. */
    val notes: String,

    /** `html_url` : le repli navigateur, toujours disponible même sans APK joint. */
    val pageUrl: String,

    /** `null` = aucun APK joint à la release ; seul le repli navigateur est offert. */
    val apkUrl: String?,

    val apkSizeBytes: Long,

    /** Empreinte publiée dans les notes ; `null` = pas de vérification possible. */
    val sha256: String?,

    /** Ligne `versionCode: N` du corps de la release ; `null` si elle manque. */
    val versionCode: Long?,
)

/**
 * Faut-il proposer cette release, et faut-il le dire sur l'accueil ?
 *
 * Fonction pure, comme [ReliabilityVerdict][com.mohamed.miqaat.domain.reliability.ReliabilityVerdict] :
 * la lecture de l'état (préférences, paquet installé) est du ressort d'Android,
 * ici on ne raisonne que sur le verdict.
 */
object UpdateVerdict {

    /**
     * ⚠ Le `versionCode` du corps de la release fait **autorité sur le tag**.
     * Android refuse d'installer un paquet dont le `versionCode` ne croît pas, et
     * il le refuse *après* le téléchargement, par un « Application non installée »
     * que rien n'explique. Un veto ici épargne vingt mégaoctets et une énigme.
     *
     * Ligne absente : le tag décide seul — un oubli de rédaction ne doit pas
     * éteindre la détection.
     */
    fun isNewer(release: ReleaseInfo, installedName: String, installedCode: Long): Boolean {
        release.versionCode?.let { if (it <= installedCode) return false }
        return AppVersion.isNewer(release.tag, installedName)
    }

    /**
     * Les cinq portes de la note d'accueil, dans l'ordre : la vérification est-elle
     * seulement autorisée, l'utilisateur a-t-il demandé « plus tard », a-t-on
     * quelque chose à annoncer, a-t-il explicitement écarté cette version-là, et
     * est-elle vraiment plus récente.
     */
    fun shouldShowOnHome(
        release: ReleaseInfo?,
        installedName: String,
        installedCode: Long,
        skippedTag: String?,
        snoozedUntil: Long,
        enabled: Boolean,
        now: Long,
    ): Boolean {
        if (!enabled) return false
        if (now < snoozedUntil) return false
        val candidate = release ?: return false
        if (candidate.tag == skippedTag) return false
        return isNewer(candidate, installedName, installedCode)
    }
}
