package com.mohamed.miqaat.domain

import java.time.Duration
import java.time.Instant

/**
 * Une alerte en retard ne doit **jamais** s'afficher.
 *
 * Le cas qui a motivé cette garde : sur une surcouche qui gèle les applications
 * fermées (MIUI et consorts), l'alarme du rappel du Fajr n'est délivrée qu'au
 * moment où l'utilisateur relance l'app — parfois dix heures plus tard. Recevoir
 * « le Fajr approche » à 14h est pire qu'une notification manquée : elle est
 * fausse, et elle décrédibilise toutes les autres.
 *
 * La chaîne, elle, se replanifie dans tous les cas : c'est l'affichage qui est
 * filtré, pas le rendez-vous suivant.
 */
object AlarmFreshness {

    /** « C'est l'heure du Asr » reste utile un moment après l'heure exacte. */
    val ADHAN: Duration = Duration.ofMinutes(20)

    /**
     * Strictement inférieure au délai de rappel le plus court
     * ([com.mohamed.miqaat.domain.model.ReminderSettings.LEAD_CHOICES] commence à
     * 10 minutes) : un rappel périmé ne peut donc jamais s'afficher **après**
     * l'adhan qu'il annonce.
     */
    val REMINDER: Duration = Duration.ofMinutes(5)

    /** Un dhikr n'a pas d'heure au sens strict, il tolère un plus grand décalage. */
    val INVOCATION: Duration = Duration.ofMinutes(30)

    /**
     * @param scheduledAt l'heure à laquelle l'alarme *devait* se déclencher.
     *   `null` quand l'intent vient d'une version antérieure de l'app, qui ne la
     *   transmettait pas : dans le doute on affiche, comme avant.
     *
     * Un déclenchement **en avance** (horloge reculée) est frais : l'alerte n'est
     * pas périmée, elle est prématurée, et la chaîne la reposera de toute façon.
     */
    fun isFresh(scheduledAt: Instant?, now: Instant, tolerance: Duration): Boolean {
        if (scheduledAt == null) return true
        val lateness = Duration.between(scheduledAt, now)
        return lateness <= tolerance
    }

    /** La tolérance qui convient à un évènement de prière. */
    fun toleranceOf(kind: PrayerEventKind): Duration = when (kind) {
        PrayerEventKind.ADHAN -> ADHAN
        PrayerEventKind.REMINDER -> REMINDER
    }
}
