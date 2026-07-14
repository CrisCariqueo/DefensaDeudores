package com.cristobalcariqueo.defensadedeudores.data.repository

import com.cristobalcariqueo.defensadedeudores.data.local.dao.SourceDao
import com.cristobalcariqueo.defensadedeudores.data.local.dao.SyncDao
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SourceEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SyncDeleteEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.toDomain
import com.cristobalcariqueo.defensadedeudores.domain.model.Source
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class SourceRepositoryImpl(
    private val dao: SourceDao,
    private val syncDao: SyncDao,
) : SourceRepository {

    override fun observeSources(): Flow<List<Source>> =
        dao.observeActive().map { entities -> entities.map(SourceEntity::toDomain) }

    override suspend fun create(name: String): Source {
        val now = Clock.System.now().toEpochMilliseconds()
        val entity = SourceEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun update(sourceId: String, name: String) {
        dao.rename(sourceId, name, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun delete(sourceId: String) {
        if (dao.registryCount(sourceId) == 0) {
            if (dao.remoteUpdatedAt(sourceId) != null) {
                syncDao.queueDelete(SyncDeleteEntity(tableName = "sources", rowId = sourceId))
            }
            dao.hardDelete(sourceId)
        } else {
            dao.softDelete(sourceId, Clock.System.now().toEpochMilliseconds())
        }
    }
}
