package com.mohamed.miqaat.domain.model

/**
 * Les invocations livrées avec l'application. Leur **contenu** (titre et texte)
 * vit dans les ressources et non en base : un dhikr authentique ne s'édite pas,
 * et son texte arabe ne se traduit pas (voir D26). Seul leur état — activée ou
 * non, à quel moment — est persisté.
 */
enum class BuiltinInvocation(val id: Long) {
    MORNING(id = 1L),
    EVENING(id = 2L),
    ;

    companion object {
        fun fromKey(key: String?): BuiltinInvocation? =
            key?.let { k -> entries.firstOrNull { it.name == k } }
    }
}

/** Quand une invocation doit rappeler d'elle-même. */
sealed interface InvocationSchedule {

    /** Une heure d'horloge, tous les jours (ex. دعاء النوم à 22:30). */
    data class FixedTime(val hour: Int, val minute: Int) : InvocationSchedule

    /**
     * Un décalage par rapport à une prière (ex. « 30 min après le Fajr »).
     * C'est le mode naturel des adhkār du matin et du soir : l'heure suit les
     * saisons toute seule, sans que l'utilisateur ait à la recaler.
     */
    data class PrayerAnchor(val prayer: PrayerName, val offsetMinutes: Int) : InvocationSchedule

    companion object {
        /** Deux heures avant la prière au plus tôt, quatre heures après au plus tard. */
        const val OFFSET_MIN = -120
        const val OFFSET_MAX = 240

        /** Pas du réglage : la minute près n'a aucun sens pour un dhikr. */
        const val OFFSET_STEP = 5

        fun sanitizeOffset(minutes: Int): Int = minutes.coerceIn(OFFSET_MIN, OFFSET_MAX)

        fun sanitizeHour(hour: Int): Int = hour.coerceIn(0, 23)

        fun sanitizeMinute(minute: Int): Int = minute.coerceIn(0, 59)
    }
}

/**
 * Une invocation : celles livrées avec l'app ([builtin] non null) comme celles
 * écrites par l'utilisateur ([title] et [body] renseignés).
 */
data class Invocation(
    val id: Long,
    val builtin: BuiltinInvocation?,
    /** Null pour une invocation livrée : son titre vient des ressources. */
    val title: String?,
    /** Null pour une invocation livrée : son texte vient des ressources. */
    val body: String?,
    val enabled: Boolean,
    val schedule: InvocationSchedule,
    val sortOrder: Int,
) {
    /** Une invocation livrée se désactive et se replanifie, mais ne se supprime ni ne se réécrit. */
    val isBuiltin: Boolean get() = builtin != null
}
