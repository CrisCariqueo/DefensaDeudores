package com.cristobalcariqueo.defensadedeudores.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = ${SettingsEntity.SINGLETON_ID}")
    fun observe(): Flow<SettingsEntity?>

    /** Seeds the singleton row with defaults; no-op if it already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seed(settings: SettingsEntity = SettingsEntity())

    /** Sync-engine write path -- carries dirty/remote_updated_at explicitly. */
    @Upsert
    suspend fun upsert(settings: SettingsEntity)

    /** Config-screen write path: touches the user fields only, preserving sync metadata. */
    @Query(
        """
        UPDATE settings SET font = :font, language = :language, dark_theme = :darkTheme,
            return_bg_color = :returnBgColor, recent_table_size = :recentTableSize,
            historical_table_size = :historicalTableSize, dirty = 1
        WHERE id = ${SettingsEntity.SINGLETON_ID}
        """,
    )
    suspend fun updateValues(
        font: String,
        language: String,
        darkTheme: Boolean,
        returnBgColor: String,
        recentTableSize: Int,
        historicalTableSize: Int,
    )

    @Query("UPDATE settings SET onboarded = 1, dirty = 1 WHERE id = ${SettingsEntity.SINGLETON_ID}")
    suspend fun markOnboarded()
}
