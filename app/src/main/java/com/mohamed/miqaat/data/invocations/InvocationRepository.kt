package com.mohamed.miqaat.data.invocations

import com.mohamed.miqaat.data.db.InvocationDao
import com.mohamed.miqaat.data.db.InvocationEntity
import com.mohamed.miqaat.domain.model.BuiltinInvocation
import com.mohamed.miqaat.domain.model.Invocation
import com.mohamed.miqaat.domain.model.InvocationSchedule
import com.mohamed.miqaat.domain.model.PrayerName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking

/**
 * Les invocations et leur rappel. Même patron que `SettingsRepository` : un
 * instantané mémoire pour les lecteurs synchrones (le planificateur d'alarme
 * tourne dans un receiver, app fermée) et un `Flow` pour l'écran.
 *
 * Le repli `runBlocking` ne sert que lorsque le processus vient de démarrer —
 * exactement le cas où le receiver est seul en vie ; c'est le compromis déjà
 * retenu pour les réglages.
 */
class InvocationRepository(private val dao: InvocationDao) {

    @Volatile
    private var memory: List<Invocation>? = null

    val invocationsFlow: Flow<List<Invocation>> = dao.observeAll()
        .onStart { ensureSeeded() }
        .map { entities -> entities.map(InvocationEntity::toDomain).also { memory = it } }

    fun current(): List<Invocation> = memory ?: runBlocking { loadFromDatabase() }

    /**
     * Pose les invocations livrées si elles manquent. Idempotent grâce aux **ids
     * fixes** de [BuiltinInvocation] : une désactivation par l'utilisateur
     * survit, et la clause `IGNORE` ne réécrit jamais un moment déjà réglé.
     */
    suspend fun ensureSeeded() {
        dao.insertIfAbsent(BuiltinInvocation.entries.map(BuiltinInvocation::seedEntity))
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(enabled = enabled))
        refreshMemory()
    }

    /** Enregistre un moment, et — pour une invocation de l'utilisateur — son contenu. */
    suspend fun save(invocation: Invocation) {
        val entity = dao.getById(invocation.id) ?: return
        dao.update(
            entity.copy(
                title = if (invocation.isBuiltin) null else invocation.title,
                body = if (invocation.isBuiltin) null else invocation.body,
                enabled = invocation.enabled,
            ).withSchedule(invocation.schedule),
        )
        refreshMemory()
    }

    suspend fun add(title: String, body: String, schedule: InvocationSchedule): Long {
        val id = dao.insert(
            InvocationEntity(
                title = title,
                body = body,
                enabled = true,
                sortOrder = dao.maxSortOrder() + 1,
            ).withSchedule(schedule),
        )
        refreshMemory()
        return id
    }

    /** Sans effet sur une invocation livrée : elle se désactive, mais ne se supprime pas. */
    suspend fun delete(id: Long) {
        dao.deleteUserInvocation(id)
        refreshMemory()
    }

    private suspend fun refreshMemory() {
        memory = dao.getAll().map(InvocationEntity::toDomain)
    }

    private suspend fun loadFromDatabase(): List<Invocation> {
        ensureSeeded()
        return dao.getAll().map(InvocationEntity::toDomain).also { memory = it }
    }
}

/** Défauts des invocations livrées : ancrées à une prière, l'heure suit donc les saisons. */
private fun BuiltinInvocation.seedEntity(): InvocationEntity = when (this) {
    BuiltinInvocation.MORNING -> InvocationEntity(
        id = id,
        builtinKey = name,
        scheduleType = InvocationEntity.SCHEDULE_ANCHOR,
        anchorPrayer = PrayerName.FAJR.name,
        offsetMinutes = 30,
        sortOrder = 1,
    )

    BuiltinInvocation.EVENING -> InvocationEntity(
        id = id,
        builtinKey = name,
        scheduleType = InvocationEntity.SCHEDULE_ANCHOR,
        anchorPrayer = PrayerName.ASR.name,
        offsetMinutes = 30,
        sortOrder = 2,
    )
}

private fun InvocationEntity.toDomain(): Invocation = Invocation(
    id = id,
    builtin = BuiltinInvocation.fromKey(builtinKey),
    title = title,
    body = body,
    enabled = enabled,
    schedule = toSchedule(),
    sortOrder = sortOrder,
)

private fun InvocationEntity.toSchedule(): InvocationSchedule =
    if (scheduleType == InvocationEntity.SCHEDULE_ANCHOR) {
        InvocationSchedule.PrayerAnchor(
            // Valeur illisible (base modifiée à la main, entrée renommée) : le
            // Fajr est le repli le moins surprenant pour un dhikr.
            prayer = PrayerName.entries.firstOrNull { it.name == anchorPrayer } ?: PrayerName.FAJR,
            offsetMinutes = InvocationSchedule.sanitizeOffset(offsetMinutes),
        )
    } else {
        InvocationSchedule.FixedTime(
            hour = InvocationSchedule.sanitizeHour(hour),
            minute = InvocationSchedule.sanitizeMinute(minute),
        )
    }

private fun InvocationEntity.withSchedule(schedule: InvocationSchedule): InvocationEntity =
    when (schedule) {
        is InvocationSchedule.FixedTime -> copy(
            scheduleType = InvocationEntity.SCHEDULE_FIXED,
            hour = InvocationSchedule.sanitizeHour(schedule.hour),
            minute = InvocationSchedule.sanitizeMinute(schedule.minute),
        )

        is InvocationSchedule.PrayerAnchor -> copy(
            scheduleType = InvocationEntity.SCHEDULE_ANCHOR,
            anchorPrayer = schedule.prayer.name,
            offsetMinutes = InvocationSchedule.sanitizeOffset(schedule.offsetMinutes),
        )
    }
