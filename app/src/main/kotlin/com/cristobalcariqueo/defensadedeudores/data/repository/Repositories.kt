package com.cristobalcariqueo.defensadedeudores.data.repository

import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import com.cristobalcariqueo.defensadedeudores.domain.model.Registry
import com.cristobalcariqueo.defensadedeudores.domain.model.Source
import com.cristobalcariqueo.defensadedeudores.domain.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * Contracts only -- implementations land with their respective feature tasks
 * (People/Sources CRUD, Track screen, retReg matching). Kept here so the
 * data/domain/ui layering is visible from the scaffold onward.
 */
interface TrackRepository {
    fun observeTracks(): Flow<List<Track>>
    suspend fun create(name: String, shortcutPersonIds: List<String>): Track
    suspend fun rename(trackId: String, name: String)
    suspend fun softDelete(trackId: String)
}

interface PersonRepository {
    fun observePeople(): Flow<List<Person>>
    suspend fun create(name: String): Person
    suspend fun update(personId: String, name: String)
    /** Hard-deletes if unreferenced by any registry, otherwise soft-deletes. */
    suspend fun delete(personId: String)
}

interface SourceRepository {
    fun observeSources(): Flow<List<Source>>
    suspend fun create(name: String): Source
    suspend fun update(sourceId: String, name: String)
    /** Hard-deletes if unreferenced by any registry, otherwise soft-deletes. */
    suspend fun delete(sourceId: String)
}

interface RegistryRepository {
    fun observeRegistries(trackId: String, limit: Int): Flow<List<Registry>>
    suspend fun createNormal(
        trackId: String,
        personId: String,
        sourceId: String,
        amount: Int,
        note: String?,
    ): Registry

    suspend fun createReturn(trackId: String, personId: String, amount: Int, note: String?): Registry
    suspend fun editAmount(registryId: String, newAmount: Int)
}
