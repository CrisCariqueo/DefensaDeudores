package com.cristobalcariqueo.defensadedeudores.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Per-table pull watermark: the max server updated_at seen. */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey @ColumnInfo(name = "table_name") val tableName: String,
    /** ISO-8601 server timestamp of the newest row pulled; PostgREST filters use it directly. */
    @ColumnInfo(name = "pulled_until") val pulledUntil: String,
)

/**
 * A row whose local and remote versions diverged since the last sync
 * (DATA_MODEL.md). Both versions kept as JSON for the resolution screen,
 * which offers local / remote / field-by-field.
 */
@Entity(tableName = "sync_conflicts")
data class SyncConflictEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "table_name") val tableName: String,
    @ColumnInfo(name = "row_id") val rowId: String,
    @ColumnInfo(name = "local_json") val localJson: String,
    @ColumnInfo(name = "remote_json") val remoteJson: String,
    @ColumnInfo(name = "detected_at") val detectedAt: Long,
)

/** Hard deletes of already-synced rows, queued until the next push. */
@Entity(tableName = "sync_deletes", primaryKeys = ["table_name", "row_id"])
data class SyncDeleteEntity(
    @ColumnInfo(name = "table_name") val tableName: String,
    @ColumnInfo(name = "row_id") val rowId: String,
)
