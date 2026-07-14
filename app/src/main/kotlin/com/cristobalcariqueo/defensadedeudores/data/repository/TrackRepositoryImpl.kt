package com.cristobalcariqueo.defensadedeudores.data.repository

import com.cristobalcariqueo.defensadedeudores.data.local.dao.TrackDao
import com.cristobalcariqueo.defensadedeudores.data.local.entity.PersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackPersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.toDomain
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
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
}
