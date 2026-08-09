package com.mohamed.miqaat.domain.model

/**
 * Réglages de **rendu** des alertes, séparés de [CalculationSettings] (qui décide
 * des horaires) et de [ReminderSettings] (qui décide des évènements à poser).
 *
 * Volontairement à part de [ReminderSettings] : celui-ci est un paramètre
 * d'entrée des resolvers de la chaîne d'alarmes. Y glisser un réglage qui ne
 * change aucun horaire obligerait toute la planification — et sa vingtaine de
 * tests — à transporter une donnée qu'elle n'utilise pas.
 */
data class NotificationSettings(
    val mode: NotificationMode = NotificationMode.DEFAULT,
)
