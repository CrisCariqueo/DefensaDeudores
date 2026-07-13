package com.cristobalcariqueo.defensadedeudores.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cristobalcariqueo.defensadedeudores.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM people WHERE deleted_at IS NULL ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<PersonEntity>>

    @Insert
    suspend fun insert(person: PersonEntity)

    @Query("UPDATE people SET name = :name, updated_at = :now WHERE id = :id")
    suspend fun rename(id: String, name: String, now: Long)

    /** All rows count as a reference -- superseded and soft-deleted included. */
    @Query("SELECT COUNT(*) FROM registries WHERE person_id = :id")
    suspend fun registryCount(id: String): Int

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("UPDATE people SET deleted_at = :now, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)
}
