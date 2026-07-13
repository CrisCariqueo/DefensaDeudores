package com.cristobalcariqueo.defensadedeudores.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cristobalcariqueo.defensadedeudores.domain.model.Track
import kotlinx.datetime.Instant

/** Local mirror of the Supabase `tracks` table. See [PersonEntity] for conventions. */
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

fun TrackEntity.toDomain() = Track(
    id = id,
    name = name,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    deletedAt = deletedAt?.let(Instant::fromEpochMilliseconds),
)
