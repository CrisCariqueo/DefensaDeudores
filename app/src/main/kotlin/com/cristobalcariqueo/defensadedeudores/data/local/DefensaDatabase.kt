package com.cristobalcariqueo.defensadedeudores.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cristobalcariqueo.defensadedeudores.data.local.dao.PersonDao
import com.cristobalcariqueo.defensadedeudores.data.local.dao.RegistryDao
import com.cristobalcariqueo.defensadedeudores.data.local.dao.SettingsDao
import com.cristobalcariqueo.defensadedeudores.data.local.dao.SourceDao
import com.cristobalcariqueo.defensadedeudores.data.local.dao.TrackDao
import com.cristobalcariqueo.defensadedeudores.data.local.entity.PersonEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.RegistryEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SettingsEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SourceEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackEntity
import com.cristobalcariqueo.defensadedeudores.data.local.entity.TrackPersonEntity

/**
 * Offline-first source of truth. Mirrors the Supabase schema (DATA_MODEL.md);
 * the sync engine reconciles this DB with remote when it lands.
 */
@Database(
    entities = [
        TrackEntity::class,
        TrackPersonEntity::class,
        PersonEntity::class,
        SourceEntity::class,
        RegistryEntity::class,
        SettingsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class DefensaDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun sourceDao(): SourceDao
    abstract fun settingsDao(): SettingsDao
    abstract fun trackDao(): TrackDao
    abstract fun registryDao(): RegistryDao

    companion object {
        const val NAME = "defensa.db"
    }
}
