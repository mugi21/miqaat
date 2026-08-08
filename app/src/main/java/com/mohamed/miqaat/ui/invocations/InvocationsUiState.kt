package com.mohamed.miqaat.ui.invocations

import com.mohamed.miqaat.domain.model.Invocation
import com.mohamed.miqaat.domain.model.PrayerName

/**
 * L'écran des invocations garde son propre état : la liste, l'invocation
 * ouverte en lecture, et celle en cours d'édition. Aucun argument ne traverse
 * donc la navigation — c'est ce qui laisse D7 (pas de librairie de navigation)
 * en place, comme pour le calendrier (D21).
 */
data class InvocationsUiState(
    val invocations: List<Invocation> = emptyList(),
    /** Invocation ouverte en lecture, ou null si on est sur la liste. */
    val reading: Invocation? = null,
    /** Invocation en cours d'édition (dialogue), ou null. */
    val editing: InvocationDraft? = null,
)

/**
 * Ce que l'éditeur manipule : une invocation existante, ou une nouvelle (id nul).
 * Un brouillon plutôt que l'[Invocation] elle-même, pour que le dialogue puisse
 * être abandonné sans rien avoir écrit.
 */
data class InvocationDraft(
    val original: Invocation?,
    val title: String,
    val body: String,
    val mode: ScheduleMode,
    val hour: Int,
    val minute: Int,
    val anchorPrayer: PrayerName,
    val offsetMinutes: Int,
) {
    val isNew: Boolean get() = original == null
    val isBuiltin: Boolean get() = original?.isBuiltin == true

    /** Une invocation sans titre n'a rien à afficher dans la liste ni en notification. */
    val canSave: Boolean get() = isBuiltin || title.isNotBlank()
}

enum class ScheduleMode { FIXED, ANCHOR }
