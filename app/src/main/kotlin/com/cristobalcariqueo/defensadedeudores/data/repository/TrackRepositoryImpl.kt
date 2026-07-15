package com.cristobalcariqueo.defensadedeudores.data.repository

import com.cristobalcariqueo.defensadedeudores.data.local.dao.TrackDao
import com.cristobalcariqueo.defensadedeudores.data.local.entity.PersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SourceEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackPersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.toDomain
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import com.cristobalcariqueo.defensadedeudores.domain.model.Source
import com.cristobalcariqueo.defensadedeudores.domain.model.Track
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class TrackRepositoryImpl(private val dao: TrackDao) : TrackRepository {

    override fun observeTracks(): Flow<List<Track>> =
        dao.observeActive().map { entities -> entities.map(TrackEntity::toDomain) }

    override fun observeTrack(trackId: String): Flow<Track?> =
        dao.observeById(trackId).map { it?.toDomain() }

    override fun observeShortcutPeople(trackId: String): Flow<List<Person>> =
        dao.observeShortcutPeople(trackId).map { entities -> entities.map(PersonEntity::toDomain) }

    override suspend fun create(name: String, shortcutPersonIds: List<String>): Track {
        require(shortcutPersonIds.isNotEmpty()) { "A track needs at least one debtor shortcut" }
        val now = Clock.System.now().toEpochMilliseconds()
        val entity = TrackEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            createdAt = now,
            updatedAt = now,
        )
        dao.insertWithShortcuts(
            track = entity,
            shortcuts = shortcutPersonIds.map { TrackPersonEntity(trackId = entity.id, personId = it) },
        )
        return entity.toDomain()
    }

    override suspend fun rename(trackId: String, name: String) {
        dao.rename(trackId, name, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun softDelete(trackId: String) {
        dao.softDelete(trackId, Clock.System.now().toEpochMilliseconds())
    }

    override fun observeRelatedSources(trackId: String): Flow<List<Source>> =
        dao.observeRelatedSources(trackId).map { entities -> entities.map(SourceEntity::toDomain) }

    override suspend fun addShortcut(trackId: String, personId: String) {
        dao.addShortcut(trackId, personId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun removeShortcut(trackId: String, personId: String) {
        dao.removeShortcut(trackId, personId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun addRelatedSource(trackId: String, sourceId: String) {
        dao.addRelatedSource(trackId, sourceId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun removeRelatedSource(trackId: String, sourceId: String) {
        dao.removeRelatedSource(trackId, sourceId, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun personUsedInTrack(trackId: String, personId: String): Boolean =
        dao.registryCountForPerson(trackId, personId) > 0

    override suspend fun sourceUsedInTrack(trackId: String, sourceId: String): Boolean =
        dao.registryCountForSource(trackId, sourceId) > 0

    override suspend fun shortcutCount(trackId: String): Int = dao.shortcutCount(trackId)
}
