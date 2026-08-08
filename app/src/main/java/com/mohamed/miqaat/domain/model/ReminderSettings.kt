package com.mohamed.miqaat.domain.model

/**
 * Rappel envoyé un peu **avant** l'adhan, pour laisser le temps de se préparer.
 * Séparé de [CalculationSettings] : ce réglage ne change aucun horaire, il ne
 * fait qu'ajouter des évènements à la chaîne d'alarmes.
 */
data class ReminderSettings(
    val enabled: Boolean = true,
    /** Minutes avant l'adhan ; toujours l'une des [LEAD_CHOICES]. */
    val leadMinutes: Int = DEFAULT_LEAD_MINUTES,
) {
    companion object {
        const val DEFAULT_LEAD_MINUTES = 10

        /**
         * Choix proposés dans les réglages. Une liste fermée plutôt qu'un champ
         * libre : elle se présente comme les autres réglages (dialogue radio),
         * et elle borne la valeur aux deux bouts.
         *
         * ⚠ Le minimum est **10 minutes, pas moins** : en Doze, Android ne laisse
         * une application déclencher qu'une seule alarme `setExactAndAllowWhileIdle`
         * toutes les ~9 minutes. Un rappel plus rapproché ferait donc reporter
         * l'adhan qui le suit — exactement ce que l'app s'interdit.
         */
        val LEAD_CHOICES = listOf(10, 15, 20, 30, 45, 60)

        /** Ce qui est lu du stockage n'est pas forcément un choix valide (version antérieure, valeur corrompue). */
        fun sanitizeLead(minutes: Int): Int =
            LEAD_CHOICES.minByOrNull { kotlin.math.abs(it - minutes) } ?: DEFAULT_LEAD_MINUTES
    }
}
