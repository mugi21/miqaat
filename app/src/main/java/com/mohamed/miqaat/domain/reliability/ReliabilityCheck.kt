package com.mohamed.miqaat.domain.reliability

/**
 * Les cinq points dont dépend l'arrivée d'une notification, application fermée.
 * Leur lecture est du ressort d'Android ([com.mohamed.miqaat.data.reliability.ReliabilityInspector]) ;
 * ici on ne raisonne que sur le verdict.
 */
enum class ReliabilityCheck {
    /** Les notifications de l'app sont-elles autorisées ? */
    NOTIFICATIONS,

    /** L'app a-t-elle le droit de poser des alarmes exactes ? */
    EXACT_ALARMS,

    /** Est-elle exclue de l'optimisation de batterie ? */
    BATTERY,

    /** Le démarrage automatique de la surcouche (MIUI et consorts) est-il accordé ? */
    OEM_AUTOSTART,

    /** Une alerte a-t-elle réellement été délivrée récemment ? */
    DELIVERY,
}

enum class CheckState {
    OK,

    /** Certain et corrigeable : on peut proposer un bouton. */
    ACTION_NEEDED,

    /** Illisible depuis une application tierce — on informe, on n'alarme pas. */
    UNKNOWN,

    /** Sans objet sur cette version d'Android ou cet appareil. */
    NOT_APPLICABLE,
}

data class ReliabilityStatus(
    val check: ReliabilityCheck,
    val state: CheckState,
)

object ReliabilityVerdict {

    /**
     * Ce qui rend l'application inutile si ce n'est pas réglé — par opposition à
     * ce qui la rend seulement moins fiable.
     */
    fun isCritical(check: ReliabilityCheck): Boolean = when (check) {
        ReliabilityCheck.NOTIFICATIONS,
        ReliabilityCheck.EXACT_ALARMS,
        ReliabilityCheck.DELIVERY,
        -> true

        ReliabilityCheck.BATTERY,
        ReliabilityCheck.OEM_AUTOSTART,
        -> false
    }

    /**
     * Faut-il avertir sur l'accueil ?
     *
     * ⚠ **[CheckState.UNKNOWN] ne déclenche jamais la bannière.** L'état du
     * démarrage automatique d'une surcouche n'est pas lisible : s'en servir
     * afficherait à tout possesseur de Xiaomi un avertissement permanent qu'aucune
     * action ne pourrait éteindre. On n'alarme que sur du certain et du critique.
     */
    fun shouldWarnOnHome(
        statuses: List<ReliabilityStatus>,
        dismissedUntil: Long,
        now: Long,
    ): Boolean {
        if (now < dismissedUntil) return false
        return statuses.any { it.state == CheckState.ACTION_NEEDED && isCritical(it.check) }
    }
}
