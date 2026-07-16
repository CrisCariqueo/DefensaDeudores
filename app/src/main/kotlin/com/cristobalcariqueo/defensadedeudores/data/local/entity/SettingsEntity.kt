package com.cristobalcariqueo.defensadedeudores.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cristobalcariqueo.defensadedeudores.domain.model.Settings

/**
 * Single-row local mirror of the Supabase `settings` table (keyed by user_id
 * remotely; locally a fixed [SINGLETON_ID] since the device DB is single-user).
 * Defaults match supabase/schema.sql.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val font: String = "default",
    val language: String = "es-CL",
    val theme: String = "system",
    @ColumnInfo(name = "return_bg_color") val returnBgColor: String = "#FFF3CD",
    @ColumnInfo(name = "recent_table_size") val recentTableSize: Int = 50,
    @ColumnInfo(name = "historical_table_size") val historicalTableSize: Int = 100,
    val onboarded: Boolean = false,
    @ColumnInfo(name = "dirty") val dirty: Boolean = true,
    @ColumnInfo(name = "remote_updated_at") val remoteUpdatedAt: Long? = null,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

fun SettingsEntity.toDomain() = Settings(
    font = font,
    language = language,
    theme = theme,
    returnBgColor = returnBgColor,
    recentTableSize = recentTableSize,
    historicalTableSize = historicalTableSize,
    onboarded = onboarded,
)

fun Settings.toEntity() = SettingsEntity(
    font = font,
    language = language,
    theme = theme,
    returnBgColor = returnBgColor,
    recentTableSize = recentTableSize,
    historicalTableSize = historicalTableSize,
    onboarded = onboarded,
)
