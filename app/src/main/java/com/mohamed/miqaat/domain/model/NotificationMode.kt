package com.mohamed.miqaat.domain.model

/**
 * Comment l'application alerte : en suivant le téléphone, ou en forçant un état.
 *
 * Le défaut reproduit ce que faisait Android quand le son et la vibration
 * étaient laissés au canal de notification. Les trois autres valeurs existent
 * parce qu'un appel à la prière n'est pas une notification comme les autres :
 * on peut vouloir l'entendre alors que le téléphone est en silencieux, ou ne
 * jamais l'entendre alors qu'il sonne.
 */
enum class NotificationMode {
    /** Sonnerie → son ; vibreur → vibration ; silencieux → rien. */
    FOLLOW_PHONE,

    /** Sonne quel que soit le mode du téléphone (flux « alarme »). */
    ALWAYS_SOUND,

    /** Vibre toujours, ne sonne jamais. */
    ALWAYS_VIBRATE,

    /** Ni son ni vibration ; la notification reste affichée. */
    SILENT,
    ;

    companion object {
        val DEFAULT = FOLLOW_PHONE

        /** Tolérant : une valeur inconnue (stockage d'une autre version) retombe sur le défaut. */
        fun parse(name: String?): NotificationMode =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
