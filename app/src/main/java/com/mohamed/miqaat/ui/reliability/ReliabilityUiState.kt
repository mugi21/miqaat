package com.mohamed.miqaat.ui.reliability

import com.mohamed.miqaat.domain.reliability.ReliabilityStatus

/**
 * L'état de l'écran de fiabilité. Volontairement brut : les heures ne sont pas
 * mises en forme ici, l'écran s'en charge — c'est lui qui connaît la langue.
 */
data class ReliabilityUiState(
    val statuses: List<ReliabilityStatus> = emptyList(),
    /** Heure de la prochaine alerte programmée, « HH:mm » — la preuve que la chaîne est armée. */
    val nextAlertTime: String? = null,
    /** Millisecondes epoch de la dernière alerte réellement délivrée ; `null` = jamais. */
    val lastDeliveredAt: Long? = null,
    /** Le fabricant a-t-il un écran de démarrage automatique connu ? */
    val hasOemScreen: Boolean = false,
    /** L'utilisateur a-t-il déclaré l'avoir réglé ? */
    val oemAcknowledged: Boolean = false,
)
