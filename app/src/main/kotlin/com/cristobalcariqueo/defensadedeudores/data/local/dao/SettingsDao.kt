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

    @Upsert
    suspend fun upsert(settings: SettingsEntity)

    @Query("UPDATE settings SET onboarded = 1 WHERE id = ${SettingsEntity.SINGLETON_ID}")
    suspend fun markOnboarded()
}
