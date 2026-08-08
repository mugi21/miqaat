package com.mohamed.miqaat.ui.invocations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamed.miqaat.data.invocations.InvocationRepository
import com.mohamed.miqaat.domain.model.Invocation
import com.mohamed.miqaat.domain.model.InvocationSchedule
import com.mohamed.miqaat.domain.model.PrayerName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InvocationsViewModel(
    private val repository: InvocationRepository,
    /**
     * Ajouter, supprimer, activer ou déplacer une invocation change les
     * évènements de la chaîne : il faut replanifier l'alarme, comme pour les
     * réglages.
     */
    private val onInvocationsChanged: () -> Unit,
) : ViewModel() {

    private val readingId = MutableStateFlow<Long?>(null)
    private val editing = MutableStateFlow<InvocationDraft?>(null)

    val uiState: StateFlow<InvocationsUiState> =
        combine(repository.invocationsFlow, readingId, editing) { invocations, reading, draft ->
            InvocationsUiState(
                invocations = invocations,
                reading = invocations.firstOrNull { it.id == reading },
                editing = draft,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            InvocationsUiState(invocations = repository.current()),
        )

    fun open(id: Long) { readingId.value = id }

    fun closeReading() { readingId.value = null }

    fun setEnabled(invocation: Invocation, enabled: Boolean) =
        update { repository.setEnabled(invocation.id, enabled) }

    // — Édition ————————————————————————————————————————————————

    fun startCreating() {
        editing.value = InvocationDraft(
            original = null,
            title = "",
            body = "",
            // Une invocation écrite par l'utilisateur se pense d'abord à une
            // heure ; l'ancrage reste à un choix d'onglet.
            mode = ScheduleMode.FIXED,
            hour = DEFAULT_HOUR,
            minute = 0,
            anchorPrayer = PrayerName.FAJR,
            offsetMinutes = DEFAULT_OFFSET,
        )
    }

    fun startEditing(invocation: Invocation, title: String, body: String) {
        val anchor = invocation.schedule as? InvocationSchedule.PrayerAnchor
        val fixed = invocation.schedule as? InvocationSchedule.FixedTime
        editing.value = InvocationDraft(
            original = invocation,
            // Le contenu d'une invocation livrée vient des ressources : on le
            // montre, mais les champs seront en lecture seule.
            title = title,
            body = body,
            mode = if (anchor != null) ScheduleMode.ANCHOR else ScheduleMode.FIXED,
            hour = fixed?.hour ?: DEFAULT_HOUR,
            minute = fixed?.minute ?: 0,
            anchorPrayer = anchor?.prayer ?: PrayerName.FAJR,
            offsetMinutes = anchor?.offsetMinutes ?: DEFAULT_OFFSET,
        )
    }

    fun editDraft(block: (InvocationDraft) -> InvocationDraft) = editing.update { it?.let(block) }

    fun cancelEditing() { editing.value = null }

    fun saveDraft() {
        val draft = editing.value ?: return
        if (!draft.canSave) return
        val schedule = draft.toSchedule()
        editing.value = null
        update {
            val original = draft.original
            if (original == null) {
                repository.add(draft.title.trim(), draft.body.trim(), schedule)
            } else {
                repository.save(
                    original.copy(
                        title = draft.title.trim(),
                        body = draft.body.trim(),
                        schedule = schedule,
                    ),
                )
            }
        }
    }

    fun delete(invocation: Invocation) {
        readingId.value = null
        editing.value = null
        update { repository.delete(invocation.id) }
    }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            onInvocationsChanged()
        }
    }

    private companion object {
        const val DEFAULT_HOUR = 7
        const val DEFAULT_OFFSET = 30
    }
}

private fun InvocationDraft.toSchedule(): InvocationSchedule = when (mode) {
    ScheduleMode.FIXED -> InvocationSchedule.FixedTime(hour, minute)
    ScheduleMode.ANCHOR -> InvocationSchedule.PrayerAnchor(anchorPrayer, offsetMinutes)
}
