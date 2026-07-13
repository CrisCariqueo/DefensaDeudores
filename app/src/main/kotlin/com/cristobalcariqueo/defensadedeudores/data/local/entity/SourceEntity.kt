package com.cristobalcariqueo.defensadedeudores.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cristobalcariqueo.defensadedeudores.domain.model.Source
import kotlinx.datetime.Instant

/** Local mirror of the Supabase `sources` table. See [PersonEntity] for conventions. */
@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

fun SourceEntity.toDomain() = Source(
    id = id,
    name = name,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    deletedAt = deletedAt?.let(Instant::fromEpochMilliseconds),
)
