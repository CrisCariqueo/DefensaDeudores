package com.cristobalcariqueo.defensadedeudores.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import kotlinx.datetime.Instant

/**
 * Local mirror of the Supabase `people` table (see DATA_MODEL.md). Table and
 * column names match the remote schema so the future sync engine maps rows
 * 1:1. `user_id` is deliberately absent everywhere locally -- the device DB is
 * single-user; sync attaches the authed user's id when pushing.
 */
@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

fun PersonEntity.toDomain() = Person(
    id = id,
    name = name,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    deletedAt = deletedAt?.let(Instant::fromEpochMilliseconds),
)
