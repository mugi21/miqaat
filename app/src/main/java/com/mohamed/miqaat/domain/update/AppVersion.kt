package com.mohamed.miqaat.domain.update

/**
 * Comparer deux numéros de version, sans rien connaître d'Android.
 *
 * L'app se met à jour depuis les releases GitHub (D44) : d'un côté le `tag_name`
 * d'une release (`v1.3`), de l'autre le `versionName` du paquet réellement
 * installé (`1.2.1`). Les deux sont des chaînes écrites à la main, donc faillibles.
 *
 * ⚠ **Repli fermé** : dès qu'un des deux côtés est illisible, [isNewer] rend
 * `false`. C'est la même règle que [ReliabilityVerdict][com.mohamed.miqaat.domain.reliability.ReliabilityVerdict],
 * qui n'alarme jamais sur un état inconnu — proposer une mise à jour sur une
 * comparaison douteuse, c'est envoyer l'utilisateur télécharger vingt mégaoctets
 * pour rien.
 */
object AppVersion {

    /**
     * `"v1.2.1"` ou `"1.2.1"` → `[1, 2, 1]`. `null` si la chaîne n'est pas une
     * version purement numérique.
     *
     * ⚠ Un `-` ou un `+` fait échouer la lecture **entière**, il n'est pas un
     * séparateur qu'on couperait : sans cette règle `v1.3-rc1` se ferait passer
     * pour `1.3` et une pré-version serait proposée à tout le monde.
     * `/releases/latest` exclut déjà brouillons et pré-versions, mais on ne fait
     * pas reposer une décision locale sur un contrat distant.
     */
    fun parse(raw: String?): List<Int>? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.length > MAX_LENGTH) return null
        val digits = trimmed.removePrefix("v").removePrefix("V")
        if (digits.isEmpty()) return null
        if (digits.any { it !in '0'..'9' && it != '.' }) return null
        val parts = digits.split('.')
        if (parts.size > MAX_PARTS) return null
        return parts.map { it.toIntOrNull() ?: return null }
    }

    /**
     * Comparaison composante par composante, les manquantes valant zéro : `1.2`
     * et `1.2.0` sont la même version, et `1.3` est plus récente que `1.2.1`.
     *
     * ⚠ Comparaison **numérique**, jamais lexicographique — sinon `1.10`
     * passerait pour plus ancienne que `1.9`.
     */
    fun compare(left: List<Int>, right: List<Int>): Int {
        val size = maxOf(left.size, right.size)
        for (index in 0 until size) {
            val difference = left.getOrElse(index) { 0 } - right.getOrElse(index) { 0 }
            if (difference != 0) return if (difference > 0) 1 else -1
        }
        return 0
    }

    /** `true` seulement si les deux versions se lisent **et** que la première gagne. */
    fun isNewer(candidate: String?, installed: String?): Boolean {
        val left = parse(candidate) ?: return false
        val right = parse(installed) ?: return false
        return compare(left, right) > 0
    }

    /** De quoi couper court à une chaîne fantaisiste avant même de la découper. */
    private const val MAX_LENGTH = 32
    private const val MAX_PARTS = 4
}
