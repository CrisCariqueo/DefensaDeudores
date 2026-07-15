package com.cristobalcariqueo.defensadedeudores.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Optional source relation for a track's quick-create area: when a track has
 * no rows here, ALL sources are offered; once at least one exists, only the
 * related ones show. Not a constraint on registries. Mirrors the Supabase
 * `track_sources` table.
 */
@Entity(
    tableName = "track_sources",
    primaryKeys = ["track_id", "source_id"],
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["track_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("source_id")],
)
data class TrackSourceEntity(
    @ColumnInfo(name = "track_id") val trackId: String,
    @ColumnInfo(name = "source_id") val sourceId: String,
)
