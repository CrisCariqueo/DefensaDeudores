package com.cristobalcariqueo.defensadedeudores.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cristobalcariqueo.defensadedeudores.domain.model.Registry
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Local mirror of the Supabase `registries` table. `type` uses the same
 * smallint codes as remote (0 normal / 1 return / 2 superseded); `date` is
 * stored as epoch days. Indexes mirror DATA_MODEL.md. No Room foreign keys to
 * tracks/people/sources -- referenced rows are soft-deleted, never removed,
 * and skipping FK enforcement keeps the future sync engine free to insert in
 * any order.
 */
@Entity(
    tableName = "registries",
    indices = [
        Index("track_id", "date"),
        Index("person_id", "type", "checked"),
        Index("track_id", "type"),
        Index("matched_retreg_id"),
    ],
)
data class RegistryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "track_id") val trackId: String,
    @ColumnInfo(name = "person_id") val personId: String,
    @ColumnInfo(name = "source_id") val sourceId: String?,
    val amount: Int,
    val type: Int,
    val checked: Boolean,
    val note: String?,
    val date: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "supersedes_id") val supersedesId: String? = null,
    @ColumnInfo(name = "matched_retreg_id") val matchedRetRegId: String? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
) {
    companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_RETURN = 1
        const val TYPE_SUPERSEDED = 2
    }
}

fun RegistryEntity.toDomain() = Registry(
    id = id,
    trackId = trackId,
    personId = personId,
    sourceId = sourceId,
    amount = amount,
    type = RegistryType.entries[type],
    checked = checked,
    note = note,
    date = LocalDate.fromEpochDays(date),
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    supersedesId = supersedesId,
    matchedRetRegId = matchedRetRegId,
    deletedAt = deletedAt?.let(Instant::fromEpochMilliseconds),
)
