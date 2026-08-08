package com.mohamed.miqaat.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface LocationDao {

    @Query("SELECT * FROM cached_location WHERE id = ${CachedLocationEntity.SINGLETON_ID}")
    suspend fun get(): CachedLocationEntity?

    @Upsert
    suspend fun upsert(entity: CachedLocationEntity)
}
