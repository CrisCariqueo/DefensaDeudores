package com.cristobalcariqueo.defensadedeudores.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Quick-pick debtor shortcuts for a track's quick-create area -- NOT a
 * membership constraint; registries may reference any person. Mirrors the
 * Supabase `track_people` table.
 */
@Entity(
    tableName = "track_people",
    primaryKeys = ["track_id", "person_id"],
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["track_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["person_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("person_id")],
)
data class TrackPersonEntity(
    @ColumnInfo(name = "track_id") val trackId: String,
    @ColumnInfo(name = "person_id") val personId: String,
)
