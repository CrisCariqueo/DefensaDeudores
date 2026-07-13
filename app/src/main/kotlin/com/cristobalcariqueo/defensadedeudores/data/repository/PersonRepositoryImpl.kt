package com.cristobalcariqueo.defensadedeudores.data.repository

import com.cristobalcariqueo.defensadedeudores.data.local.dao.PersonDao
import com.cristobalcariqueo.defensadedeudores.data.local.entity.PersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.toDomain
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class PersonRepositoryImpl(private val dao: PersonDao) : PersonRepository {

    override fun observePeople(): Flow<List<Person>> =
        dao.observeActive().map { entities -> entities.map(PersonEntity::toDomain) }

    override suspend fun create(name: String): Person {
        val now = Clock.System.now().toEpochMilliseconds()
        val entity = PersonEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun update(personId: String, name: String) {
        dao.rename(personId, name, Clock.System.now().toEpochMilliseconds())
    }

    override suspend fun delete(personId: String) {
        if (dao.registryCount(personId) == 0) {
            dao.hardDelete(personId)
        } else {
            dao.softDelete(personId, Clock.System.now().toEpochMilliseconds())
        }
    }
}
