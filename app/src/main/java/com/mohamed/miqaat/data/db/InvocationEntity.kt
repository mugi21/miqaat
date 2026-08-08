package com.mohamed.miqaat.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Une invocation persistée. Table à plat plutôt qu'un champ sérialisé : le
 * moment du rappel a deux formes ([SCHEDULE_FIXED] ou [SCHEDULE_ANCHOR]) et
 * chacune garde ses colonnes, lisibles depuis `sqlite3` en cas de diagnostic.
 *
 * Les invocations livrées ([builtinKey] non null) n'ont ni titre ni texte : ils
 * viennent des ressources, donc traduisibles pour le titre et intacts pour le
 * texte arabe (voir D26).
 */
@Entity(tableName = "invocation")
data class InvocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Nom de l'entrée `BuiltinInvocation`, ou null si l'utilisateur l'a écrite. */
    val builtinKey: String? = null,
    val title: String? = null,
    val body: String? = null,
    val enabled: Boolean = true,
    /** [SCHEDULE_FIXED] ou [SCHEDULE_ANCHOR]. */
    val scheduleType: String = SCHEDULE_FIXED,
    val hour: Int = 0,
    val minute: Int = 0,
    /** Nom de l'entrée `PrayerName` quand le moment est ancré à une prière. */
    val anchorPrayer: String? = null,
    val offsetMinutes: Int = 0,
    val sortOrder: Int = 0,
) {
    companion object {
        const val SCHEDULE_FIXED = "FIXED"
        const val SCHEDULE_ANCHOR = "ANCHOR"
    }
}
